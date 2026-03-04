# URL Shortener (Warp)

# 1. Requirements

## 1.1. Functional Requirements

- Create a short URL (e.g https://warp/dU02kX)
- Create a custom short URL (e.g https://warp/queenparfumgs)
    - Must be unique out of all users
- Redirect when my short URL is clicked (302 status)
- Manage my URLs
    - See all my short URLs (Paginations)
    - Delete a short URL (Soft delete)
    - Can’t update a URL (if want to change, create a new one)
- Protect my URLs with passwords at redirect-time
- Set shortened URL Expiry time
    - 410 if expired
- View analytics
    - See all my URLs on the dashboard page with total clicks (all time) and clicks from the last 7 days, so I don’t have to open each URL to see its metrics.
    - View detailed analytics and filter click data by:
        - Timestamp (15:23 UTC) → Always use UTC
        - Country (ISO Code)
        - Referrer (e.g twitter.com, google.com)
        - Device type (mobile / desktop / tablet)
        - Http Status (302, 200, 400, 404)
        - Response Latency (10ms, 20ms, >50ms)
    - Time windows: 1h / 4h / 1d / 3d / 7d / 30d

---

## 1.2. Non-functional Requirements

- Redirect latency: p99 ≤ 50ms
- Scale targets: 100M DAU, 1B URLs
- Ensure uniqueness of `shortCode`
- High availability + fault tolerance:
    - Redis down
    - Analytics down
    - Any instance down
- Eventual consistency (5-minute window)
    - Clicks may appear in dashboards **up to 5 minutes late**
    - Aggregates converge asynchronously
    - Users never block on analytics
- Data integrity
    - Durable event ingestion
    - Idempotent aggregation
    - Periodic reconciliation to detect drifts

## 1.3 Out of Scope

- Bot Filtering
- Global CDN / edge deployment
- Exactly-once analytics & real-time guarantees
- Advanced security & abuse prevention
- Strong privacy / compliance guarantees
- UI polish & product UX depth
- Billing, quotas, plans, rate limiting

---

# 2. The Tech Stack (Tier-1 Standard)

- **Language / Framework:** **Spring MVC.**
- **Primary DB (OLTP):** **PostgreSQL** — transactional source of truth for URL mappings.
- **Caching:** **Redis** — hot-path lookups for sub-millisecond redirects.
- **Message Broker:** **Apache Kafka** — durable buffer for asynchronous telemetry.
- **Analytics DB (OLAP):** **ClickHouse** — columnar storage + insert-time pre-aggregation.
- **Infrastructure:** Docker & Docker Compose — local parity and reproducible environments.

---

# 3. System Design & Plane Separation

### A. Control Plane (Management API)

- **Purpose:** Create/update/delete links, access control, link settings.
- **Flow:** User → **Control Service (Spring)** → Postgres (write)
- **Constraint:** Prioritizes **consistency** and correctness over raw speed.

### B. Data Plane (Hot Path / Redirect)

- **Purpose:** Serve redirects as fast as possible.
- **Flow:** User Click → **Redirect Service (Spring)** → Redis (GET) → `302 Found`
- **Telemetry:** On every redirect, emit a `LinkVisited` event **asynchronously** (fire-and-forget) to Kafka. Redirect response must not wait on Kafka.

### C. Analytics Plane (Streaming → OLAP)

- **Purpose:** Convert high-volume click events into query-friendly counters with **1-minute granularity**.
- **Flow (ClickHouse as Consumer):**
    
    **Kafka → ClickHouse Kafka Engine → Aggregation Pipeline → Cron Jobs → OLAP**
    

No Spring Consumer Needed

---

# 4. Schema Design

### A. PostgreSQL (OLTP) Schema

**Table: `urls`** (source of truth)

| Column | Type | Description |
| --- | --- | --- |
| `id` | `UUID` / `BIGINT` | Primary key. |
| `short_url` | `VARCHAR(10)` | **Unique index** (e.g., `6ghS2Z`). |
| `original_url` | `TEXT` | Destination URL. |
| `user_id` | `UUID` | Owner (FK to users, optional). |
| `created_at` | `TIMESTAMP` | Creation time. |
| `expires_at` | `TIMESTAMP` | Optional link expiration. |
| `deleted_at`  | `TIMESTAMP` |  |
| `is_protected` | `BOOLEAN` |  |
| `password` | `TEXT` |  |
| `disabled` | `BOOLEAN` |  |

Index on short_code and user_id

> Note: Any “total_clicks” counter in Postgres is optional and usually eventual (analytics-backed). For strict hot-path performance, do not update Postgres per click.
> 

---

## B. ClickHouse (OLAP) Schema

This is optimized for high-ingest and fast aggregation queries. ClickHouse is **denormalized** by design.

### Raw events table: `click_events_raw`

**Purpose**

- Durability + lineage
- Reprocessing (UA parsing changes)
- Reconciliation (detect missed aggregates)
- Debugging anomalies

**Design requirements**

- Store timestamps in **UTC**
- Include both **parsed fields** (for convenience) and **raw inputs** (for correctness/backfills)
- Support **idempotency / dedup** at aggregation stage via `event_id`
- TTL to clean up raw data after X days (e.g., 7–30)
- `user_id` in Rollup Table is denormalized to avoid `IN` queries to Postgres

| **timestamp** | event_id | **url_id** | **short_url** | https status | country_code | device_type |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-02-02 14:23:00 | UUID | 001 | xyz | 302 | US | Phone |
| 2026-02-02 14:23:00 | UUID | 002 | xyz | 404 | INA | Laptop |

### Rollup Table (1-Minute Analytics)

| **minute** | **url_id** | user_id | **country** | **browser** | **clicks** |
| --- | --- | --- | --- | --- | --- |
| 2026-02-02 14:23 | xyz | 123 | US | Chrome | 100 |
| 2026-02-02 14:23 | xyz | 123 | CA | Firefox | 90 |

---

# 5. System Design

![image.png](attachment:4f8dc33a-cbf8-4dc1-bd42-57c8aec216e3:image.png)

---

# 6. Failure Modes

| Scenario | Action | Consequence |
| --- | --- | --- |
| Redis GET Unavailable | L1 Caffeine + Circuit Breaker | Postgres fallback, p99 may breach SLO |
| Postgres down | HPA | Can be via redis if exists, or just error |
| Kafka down | HPA | Redirections Unaffected, Clicks may lost |
| ClickHouse down | Long kafka retention | Kafka buffers events, catches up on recovery |
| Duplicate Events | Handle Idepotency by adding `event_id` | Inflated click counts |
| Short URL Collision | Counter Based generation / Retry | Rare |
| Thundering Herd | Define threshold that a cache hit will increase redis key lifespan | Postgres Overload |

---

# 7. SLA / SLO (Service Level Objectives)

- **Availability:** 99.9% for redirection (data plane).
- **Redirection Latency (p99):** < 50ms (request → 302 response).
- **Analytics Freshness:** Updated every 5 minutes.
- **Throughput:** 50,000 RPS (validated via load testing).

---

# 8. The 7-Week Sprint Plan

| **Week** | **Phase** | **Deliverable** |
| --- | --- | --- |
| **1** | **Core API** | Spring+ Postgres: create short URL + basic CRUD. |
| **2** | **Hot Path** | Redis integration + redirect endpoint + baseline benchmarks. |
| **3** | **Kafka Pipe** | Telemetry event schema + producer in Redirect Service (fire-and-forget). |
| **4** | **OLAP Setup** | ClickHouse schema: Kafka Engine, `raw_clicks`, MV, `minute_analytics`. |
| **5** | **Aggregation & Integrity** | Validate minute rollups, late events behavior, retention TTL. |
| **6** | **Optimization** | Redis TTL policies, Postgres indexes, Kafka partitioning, CH insert tuning. |
| **7** | **Validation** | k6/Locust load test + SLO evidence + resume bullets + architecture doc. |

---

# 9. Periodic Reconciliation (Daily Correctness Backstop)

**Goal:** Mitigate ingestion outages or misconfigurations (Kafka down, ClickHouse down, consumer lag exceeding retention) by detecting drift between raw events and OLAP aggregates.

### A. Why reconciliation exists

The real-time pipeline is designed to be **high-throughput and decoupled**. During incidents, analytics may lag or temporarily stop. Reconciliation provides:

- **Detection** of missing or inconsistent aggregates
- Optional **repair** for closed time ranges (e.g., “yesterday”)

### B. Recommended cadence

- **Daily** (e.g., run at 02:00 local time)
- Reconcile **closed windows** (e.g., previous day) to avoid constant churn.

### C. What gets checked

For a given day `D`:

- Recompute expected aggregates from `raw_clicks`:
    - `GROUP BY toStartOfMinute(timestamp), url_id, country, browser`
- Compare to existing `minute_analytics` for the same minutes
- Output:
    - missing keys (minute/url_id/…)
    - deltas over threshold (e.g., > 0.1% drift)

### D. Repair strategy (optional)

Keep it simple and safe:

- Repair only “closed” partitions (yesterday)
- Write repaired results into a new table/partition and **swap** (or insert-delta cautiously)
- Never mutate live aggregates for the current minute/hour.

---

# 10. Analytics Query Semantics (Why 1-minute granularity)

At time `15:23`, “last 60 minutes” is naturally:

- query from `14:23` to `15:23`

Minute rollups support **accurate sliding windows** without partial-bucket approximation.

Example (conceptual):

- “last 60 minutes”: `WHERE minute >= now() - INTERVAL 60 MINUTE`
- “today”: `WHERE minute >= toStartOfDay(now())`

---