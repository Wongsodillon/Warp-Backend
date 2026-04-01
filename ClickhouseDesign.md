# ClickHouse Schema Documentation

This document describes all 5 tables in the Warp analytics pipeline. Data flows in one direction:
**Kafka → `click_events_kafka` → `click_events_consumer` (MV) → `click_events_raw` → `minute_analytics_mv` (MV) → `minute_analytics`**

---

## 1. `click_events_kafka`

**Type:** Kafka Engine Table  
**Purpose:** Entry point for raw click event ingestion. This is not a real storage table — it's a virtual table that reads from a Kafka topic. ClickHouse treats it as a stream source.

**Engine:** `Kafka('kafka:29092', 'url.click.event', 'clickhouse-consumer', 'JSONEachRow')`

| Column | Type | Description |
|--------|------|-------------|
| `eventId` | String | Unique event identifier |
| `urlId` | UInt64 | ID of the shortened URL that was clicked |
| `userId` | UInt64 | ID of the user who owns the URL |
| `shortUrl` | String | The short URL string (e.g. `warp-io.dev/abc123`) |
| `timestamp` | Float64 | Unix timestamp in seconds (as a float) |
| `countryCode` | String | ISO country code of the visitor |
| `deviceType` | String | Device category (e.g. `mobile`, `desktop`) |
| `browser` | String | Browser name (e.g. `Chrome`, `Safari`) |
| `referrer` | Nullable(String) | HTTP referrer URL, if present |
| `responseLatencyMs` | UInt64 | Time taken to serve the redirect, in milliseconds |

**Notes:**
- Uses camelCase column naming (matches the JSON payload from the Spring Boot backend).
- `timestamp` is a `Float64` because Kafka messages carry a Unix epoch float. It gets converted to `DateTime64` in the materialized view.
- No data is persisted here — rows are consumed and forwarded by `click_events_consumer`.

---

## 2. `click_events_consumer`

**Type:** Materialized View (MV)  
**Purpose:** Bridges `click_events_kafka` to `click_events_raw`. Reads from the Kafka engine table, transforms column names and data types, and writes into the raw storage table.

**Target table:** `click_events_raw`

**Transformations applied:**

| Kafka Column | Raw Column | Transformation |
|---|---|---|
| `eventId` | `event_id` | Rename only |
| `urlId` | `url_id` | Rename only |
| `userId` | `user_id` | Rename only |
| `shortUrl` | `short_url` | Rename only |
| `timestamp` | `timestamp` | `fromUnixTimestamp64Milli(toInt64(timestamp * 1000))` — converts Float64 seconds → DateTime64(3) |
| `countryCode` | `country_code` | Rename only |
| `deviceType` | `device_type` | Rename only |
| `browser` | `browser` | No change |
| `referrer` | `referrer` | No change |
| `responseLatencyMs` | `response_latency_ms` | Rename only |

**Notes:**
- The timestamp conversion (`* 1000` then `fromUnixTimestamp64Milli`) is the critical step — Kafka sends seconds as a float, but `click_events_raw` stores millisecond-precision UTC datetimes.
- This MV fires automatically on every Kafka batch consumed; you never write to it directly.

---

## 3. `click_events_raw`

**Type:** MergeTree Table (primary storage)  
**Purpose:** Persistent, queryable store of all individual click events. This is the source of truth for raw event data and the input for the analytics rollup MV.

**Engine:** `MergeTree`  
**Partition by:** `toYYYYMM(timestamp)` (monthly partitions)  
**Order by:** `(url_id, timestamp)`  
**TTL:** 30 days from `timestamp`

| Column | Type | Description |
|--------|------|-------------|
| `event_id` | String | Unique event identifier |
| `url_id` | UInt64 | ID of the shortened URL |
| `user_id` | UInt64 | ID of the URL owner |
| `short_url` | String | The short URL string |
| `timestamp` | DateTime64(3, 'UTC') | Millisecond-precision UTC timestamp |
| `country_code` | LowCardinality(String) | ISO country code |
| `device_type` | LowCardinality(String) | Device category |
| `browser` | LowCardinality(String) | Browser name |
| `referrer` | Nullable(String) | HTTP referrer, if present |
| `response_latency_ms` | UInt64 | Redirect latency in milliseconds |

**Notes:**
- `LowCardinality(String)` on `country_code`, `device_type`, and `browser` improves compression and query performance for low-cardinality columns (typically <10k distinct values).
- Monthly partitioning aligns with the TTL policy — ClickHouse can drop entire 30-day-old partitions efficiently.
- The `ORDER BY (url_id, timestamp)` primary key optimizes the most common query pattern: filtering by URL then ranging over time.
- `referrer` is not `LowCardinality` here because referrer URLs have high cardinality.

---

## 4. `minute_analytics`

**Type:** AggregatingMergeTree Table  
**Purpose:** Pre-aggregated 1-minute rollups of click events, broken down by URL, country, device, browser, and referrer. Powers the analytics dashboard with fast sub-second queries without scanning raw events.

**Engine:** `AggregatingMergeTree`  
**Partition by:** `toYYYYMM(minute)`  
**Order by:** `(url_id, minute)`

| Column | Type | Description |
|--------|------|-------------|
| `minute` | DateTime | Truncated to the start of the minute (UTC) |
| `url_id` | UInt64 | ID of the shortened URL |
| `country_code` | LowCardinality(String) | ISO country code |
| `device_type` | LowCardinality(String) | Device category |
| `browser` | LowCardinality(String) | Browser name |
| `referrer` | LowCardinality(Nullable(String)) | HTTP referrer |
| `clicks` | AggregateFunction(count) | Aggregate state for click count |
| `avg_latency` | AggregateFunction(avg, UInt32) | Aggregate state for average latency |

**Notes:**
- `AggregateFunction` columns store **intermediate aggregation state**, not final values. You must use `countMerge(clicks)` and `avgMerge(avg_latency)` in SELECT queries — not `clicks` directly.
- `AggregatingMergeTree` merges partial states in the background. This means at query time, multiple partial rows for the same `(url_id, minute, ...)` key may exist temporarily — always use `GROUP BY` with merge functions.
- `referrer` is `LowCardinality` here (unlike in `click_events_raw`) because aggregation already collapses cardinality to a manageable level per time bucket.
- Example query pattern:
  ```sql
  SELECT
      minute,
      countMerge(clicks) AS total_clicks,
      avgMerge(avg_latency) AS avg_latency_ms
  FROM minute_analytics
  WHERE url_id = 42
  GROUP BY minute
  ORDER BY minute;
  ```

---

## 5. `minute_analytics_mv`

**Type:** Materialized View (MV)  
**Purpose:** Continuously populates `minute_analytics` from `click_events_raw`. Runs automatically whenever new rows land in `click_events_raw`, computing the 1-minute rollup aggregations in real time.

**Target table:** `minute_analytics`

**Logic:**
```sql
SELECT
    toStartOfMinute(timestamp) AS minute,
    url_id,
    country_code,
    device_type,
    browser,
    referrer,
    countState()                        AS clicks,
    avgState(toUInt32(response_latency_ms)) AS avg_latency
FROM click_events_raw
GROUP BY minute, url_id, country_code, device_type, browser, referrer
```

**Notes:**
- `countState()` and `avgState()` produce intermediate aggregate states (not final values), which is what `AggregatingMergeTree` expects.
- `toUInt32(response_latency_ms)` casts from `UInt64` to match the `AggregateFunction(avg, UInt32)` signature declared in `minute_analytics`.
- This MV fires incrementally — it processes each new batch of rows inserted into `click_events_raw`, not the full table. This keeps it efficient at scale.
- The GROUP BY here produces one row per unique `(minute, url_id, country_code, device_type, browser, referrer)` combination per batch. `AggregatingMergeTree` merges these over time in the background.

---

## Pipeline Summary

```
Spring Boot Backend
       │
       │  publishes JSON to Kafka topic: url.click.event
       ▼
click_events_kafka          (Kafka Engine — stream, no storage)
       │
       │  click_events_consumer MV transforms + writes
       ▼
click_events_raw            (MergeTree — raw event storage, 30-day TTL)
       │
       │  minute_analytics_mv MV aggregates per minute
       ▼
minute_analytics            (AggregatingMergeTree — rollup, dashboard queries)
```