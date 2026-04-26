-- 1. Kafka Engine source table (entry point, no storage)
CREATE TABLE IF NOT EXISTS click_events_kafka
(
    eventId           String,
    urlId             UInt64,
    userId            UInt64,
    shortUrl          String,
    timestamp         Float64,
    countryCode       String,
    deviceType        String,
    browser           String,
    referrer          Nullable(String),
    responseLatencyMs UInt64
) ENGINE = Kafka(
    'pkc-921jm.us-east-2.aws.confluent.cloud:9092',
    'url.click.event',
    'clickhouse-consumer',
    'JSONEachRow'
);

-- 2. Raw event storage (MergeTree, 30-day TTL)
CREATE TABLE IF NOT EXISTS click_events_raw
(
    event_id            String,
    url_id              UInt64,
    user_id             UInt64,
    short_url           String,
    timestamp           DateTime64(3, 'UTC'),
    country_code        LowCardinality(String),
    device_type         LowCardinality(String),
    browser             LowCardinality(String),
    referrer            Nullable(String),
    response_latency_ms UInt64
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(timestamp)
ORDER BY (url_id, timestamp)
TTL timestamp + INTERVAL 30 DAY;

-- 3. MV: Kafka source -> raw storage (renames columns, converts timestamp Float64 -> DateTime64)
CREATE MATERIALIZED VIEW IF NOT EXISTS click_events_consumer
TO click_events_raw AS
SELECT
    eventId                                             AS event_id,
    urlId                                               AS url_id,
    userId                                              AS user_id,
    shortUrl                                            AS short_url,
    fromUnixTimestamp64Milli(toInt64(timestamp * 1000)) AS timestamp,
    countryCode                                         AS country_code,
    deviceType                                          AS device_type,
    browser,
    referrer,
    responseLatencyMs                                   AS response_latency_ms
FROM click_events_kafka;

-- 4. Pre-aggregated 1-minute rollup table
CREATE TABLE IF NOT EXISTS minute_analytics
(
    minute       DateTime,
    url_id       UInt64,
    country_code LowCardinality(String),
    device_type  LowCardinality(String),
    browser      LowCardinality(String),
    referrer     LowCardinality(Nullable(String)),
    clicks       AggregateFunction(count),
    avg_latency  AggregateFunction(avg, UInt32)
) ENGINE = AggregatingMergeTree()
PARTITION BY toYYYYMM(minute)
ORDER BY (url_id, minute);

-- 5. MV: raw -> minute rollup (fires incrementally on each raw insert batch)
CREATE MATERIALIZED VIEW IF NOT EXISTS minute_analytics_mv
TO minute_analytics AS
SELECT
    toStartOfMinute(timestamp)              AS minute,
    url_id,
    country_code,
    device_type,
    browser,
    referrer,
    countState()                            AS clicks,
    avgState(toUInt32(response_latency_ms)) AS avg_latency
FROM click_events_raw
GROUP BY minute, url_id, country_code, device_type, browser, referrer;
