import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// ── Custom metrics ─────────────────────────────────────────────────────────────
const redirectOkRate    = new Rate('redirect_ok_rate');
const redirectLatency   = new Trend('redirect_latency_ms', true);
const likelyCacheHits   = new Counter('likely_cache_hits');
const likelyCacheMisses = new Counter('likely_cache_misses');

// ── Constants ──────────────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 50 active seeded URLs from db/dev/R__seed_data.sql
const SEEDED = Array.from({ length: 50 }, (_, i) =>
  `dev${String(i + 1).padStart(4, '0')}`
);

// Mark 302 and 404 as expected — only 5xx / network errors count as "failed"
http.setResponseCallback(
  http.expectedStatuses(302, 404, { min: 200, max: 299 })
);

// ── Options ────────────────────────────────────────────────────────────────────
export const options = {
  scenarios: {
    // 1. Warm-cache baseline — establish p50/p95 on pure Redis-served hits
    //    Watch: lettuce.command.completion, url_cache_hits_total
    warm_cache_baseline: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 100 },
        { duration: '60s', target: 100 },
        { duration: '20s', target: 0 },
      ],
      exec: 'warmCacheScenario',
      tags: { scenario: 'warm_cache' },
    },

    // 2. Thread-pool stress ramp — drive VUs past Tomcat default (200 threads)
    //    Watch: tomcat.threads.busy, p99 cliff when VUs > 200
    stress_ramp: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 100 },
        { duration: '20s', target: 200 },  // at thread-pool limit
        { duration: '20s', target: 300 },  // over the limit → queuing
        { duration: '40s', target: 300 },  // hold to confirm saturation
        { duration: '20s', target: 0 },
      ],
      exec: 'stressRampScenario',
      tags: { scenario: 'stress_ramp' },
      // startTime: '110s',
    },

    // 3. Cache-miss storm — all requests hit DB directly (run with Redis disabled)
    //    HikariCP default pool = 10; excess threads will wait → hikaricp.connections.pending
    //    Watch: hikaricp.connections.pending, hikaricp.connections.active
    cache_miss_storm: {
      executor: 'constant-vus',
      vus: 100,
      duration: '60s',
      exec: 'cacheMissScenario',
      tags: { scenario: 'cache_miss' },
      // startTime: '260s',
    },

    // 4. Mixed realistic workload — 80 % cache hits / 20 % misses
    //    Watch: divergence between hit/miss latency; hikaricp under steady mixed load
    mixed_workload: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      stages: [
        { duration: '30s', target: 100 },
        { duration: '60s', target: 200 },
        { duration: '30s', target: 100 },
      ],
      preAllocatedVUs: 60,
      maxVUs: 250,
      exec: 'mixedWorkloadScenario',
      tags: { scenario: 'mixed_workload' },
      // startTime: '330s',
    },

    // 5. Spike test — 40× burst to expose GC stop-the-world pauses
    //    Watch: irregular p99 spikes (jvm.gc.pause), hikaricp burst recovery
    spike_test: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '10s', target: 10  },
        { duration: '5s',  target: 400 },  // instant spike
        { duration: '30s', target: 400 },  // sustain
        { duration: '5s',  target: 10  },  // instant drop
        { duration: '20s', target: 10  },  // recovery window
      ],
      exec: 'spikeScenario',
      tags: { scenario: 'spike' },
      // startTime: '460s',
    },
  },

  thresholds: {
    // SLO: 95 % of redirects served under 50 ms on a warm cache
    'http_req_duration{scenario:warm_cache}':     ['p(95)<50',   'p(99)<100'],
    // Graceful degradation under thread-pool saturation
    'http_req_duration{scenario:stress_ramp}':    ['p(95)<500',  'p(99)<2000'],
    // DB-bound path is slower; allow up to 300 ms p95
    'http_req_duration{scenario:cache_miss}':     ['p(95)<300'],
    // Production mix must stay reasonable
    'http_req_duration{scenario:mixed_workload}': ['p(95)<100',  'p(99)<200'],
    // Spike: some breaches ok, but no total meltdown
    'http_req_duration{scenario:spike}':          ['p(99)<3000'],
    // 302 success rate on seeded URLs must be near-perfect
    'redirect_ok_rate':                           ['rate>0.99'],
    // 5xx / network errors must be negligible
    'http_req_failed':                            ['rate<0.02'],
  },
};

// ── Helpers ────────────────────────────────────────────────────────────────────
function pickSeeded() {
  return SEEDED[Math.floor(Math.random() * SEEDED.length)];
}

// Non-existent URLs are never cached, so every call is a guaranteed DB hit.
function pickNonExistent() {
  return 'nx' + Math.random().toString(36).substring(2, 9);
}

// redirects: 0 → k6 sees the raw 302 and measures it directly.
function makeParams(extraTags) {
  return { redirects: 0, timeout: '5s', tags: extraTags };
}

// ── Scenario functions ─────────────────────────────────────────────────────────

// Scenario 1 — warm cache baseline
// After the first ~50 requests, all 50 seeded URLs are in Redis.
// Every subsequent request is a pure cache hit:  Redis GET → 302.
// Bottleneck surface: Lettuce / Redis throughput, Tomcat serialization overhead.
export function warmCacheScenario() {
  const res = http.get(
    `${BASE_URL}/${pickSeeded()}`,
    makeParams({ url_type: 'seeded' })
  );
  check(res, {
    'status 302':          r => r.status === 302,
    'has Location header': r => Boolean(r.headers['Location']),
    'latency < 50ms':      r => r.timings.duration < 50,
  });
  redirectOkRate.add(res.status === 302);
  redirectLatency.add(res.timings.duration);
  // Heuristic: sub-10 ms response almost certainly came straight from Redis
  if (res.timings.duration < 10) likelyCacheHits.add(1);
  else likelyCacheMisses.add(1);
}

// Scenario 2 — thread-pool stress ramp
// Ramp VUs well past the Tomcat default thread count (200).
// If p99 latency has a step-change cliff near 200 concurrent VUs, the thread
// pool is the bottleneck (requests start queuing in the accept queue).
export function stressRampScenario() {
  const res = http.get(
    `${BASE_URL}/${pickSeeded()}`,
    makeParams({ url_type: 'seeded' })
  );
  check(res, {
    'status 302':       r => r.status === 302,
    'no server error':  r => r.status < 500,
    'latency < 2000ms': r => r.timings.duration < 2000,
  });
  redirectOkRate.add(res.status === 302);
  redirectLatency.add(res.timings.duration);
}

// Scenario 3 — cache-miss storm
// Every VU hits a seeded URL with Redis disabled → guaranteed cache MISS → DB query → 302.
// Run this scenario with Redis turned off so every request falls through to the DB.
// With 100 concurrent VUs and a HikariCP pool of 10, up to 90 threads will be
// blocked waiting for a connection.  hikaricp.connections.pending will be > 0.
export function cacheMissScenario() {
  const res = http.get(
    `${BASE_URL}/${pickSeeded()}`,
    makeParams({ url_type: 'seeded' })
  );
  check(res, {
    'status 302':      r => r.status === 302,
    'no server error': r => r.status < 500,
    'latency < 300ms': r => r.timings.duration < 300,
  });
  redirectOkRate.add(res.status === 302);
  redirectLatency.add(res.timings.duration);
  likelyCacheMisses.add(1);
}

// Scenario 4 — mixed realistic workload
// 80 % of requests are seeded URLs (cache hits after scenario 1 warmed the cache).
// 20 % are non-existent URLs (cache misses → DB queries).
// Mirrors a production traffic distribution.
export function mixedWorkloadScenario() {
  const isCacheable = Math.random() < 0.80;
  const shortUrl    = isCacheable ? pickSeeded() : pickNonExistent();
  const res = http.get(
    `${BASE_URL}/${shortUrl}`,
    makeParams({ url_type: isCacheable ? 'seeded' : 'nonexistent' })
  );
  check(res, {
    'valid response':  r => r.status === 302 || r.status === 404,
    'no server error': r => r.status < 500,
  });
  if (isCacheable) {
    redirectOkRate.add(res.status === 302);
    likelyCacheHits.add(1);
  } else {
    likelyCacheMisses.add(1);
  }
  redirectLatency.add(res.timings.duration);
}

// Scenario 5 — spike test
// A sudden 40× traffic burst (10 → 400 VUs in 5 s) then an instant drop.
// Surfaces GC stop-the-world pauses (irregular p99 spikes in jvm.gc.pause),
// HikariCP connection pool cold-start latency, and Tomcat accept-queue depth.
export function spikeScenario() {
  const res = http.get(
    `${BASE_URL}/${pickSeeded()}`,
    makeParams({ url_type: 'seeded' })
  );
  check(res, {
    'status 302':      r => r.status === 302,
    'no server error': r => r.status < 500,
    'no timeout':      r => r.timings.duration < 5000,
  });
  redirectOkRate.add(res.status === 302);
  redirectLatency.add(res.timings.duration);
}
