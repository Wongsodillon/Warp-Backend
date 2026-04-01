# CLAUDE.md - Warp

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview
A high-performance URL shortening service built with Spring MVC, PostgreSQL, Redis, Kafka, and ClickHouse, designed to handle Millions of DAU and Short URLs. It supports custom short links, password protection, expiry, and a real-time analytics pipeline with sub-50ms redirect latency.

## Commands

```bash
# Run with local profile (uses Supabase + Clerk dev env)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Build JAR (skipping tests)
./mvnw -DskipTests package

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=WarpBackendApplicationTests

# Build Docker image
docker build -t warp-backend .
```

## Architecture

Warp is a URL shortener backend built with Spring MVC + PostgreSQL, designed for three separated planes:

**Control Plane** — URL management API (`/api/**`), requires Clerk JWT auth. Currently implemented: `POST /api/shorten` creates a short URL using Base62-encoded XOR-obfuscated DB sequence IDs.

**Data Plane** — Public redirect endpoint (`/{shortUrl}`), no auth required, returns `302 Found`. Redis caching and Kafka telemetry (fire-and-forget `LinkVisited` events) are planned but not yet implemented.

**Analytics Plane** — Kafka → ClickHouse pipeline. ClickHouse consumes from Kafka via its Kafka Engine; no Spring consumer is needed. Minute-level rollups feed dashboard/detail analytics APIs. Not yet implemented.

## Key Patterns

**Short URL generation:** `UrlServiceImpl` fetches the next Postgres sequence ID, XORs it with `application.short-url.secret`, and encodes with Base62 (`util/Base62.java`). This avoids sequential/guessable codes while being deterministic.

**Custom validation:** Parameters annotated with `@Validate` on controller methods trigger field-level validation via `RequestValidationAspect` (AOP). Constraint annotations (e.g., `@NotBlank`) are in `model/annotation/constraint/` with corresponding validators in `model/annotation/validator/`. Throws `ValidationException` on failure.

**Authentication:** `ClerkJwtAuthenticationConverter` validates Clerk JWTs, then lazily creates a local `User` row via `UserService.resolveOrCreateUser()` on first login. The authenticated `User` entity is stored as the Spring Security principal.

**Current user:** `CurrentUserService` reads the `User` entity from the Spring Security context. Controllers/services call this to get the current `user_id`.

**Exception handling:** `ControllerAdvice` maps `BaseException` subclasses (`NotFoundException`, `ValidationException`) to HTTP responses. `ErrorCode` enum holds error codes.

**Response envelope:** All API responses use `RestSingleResponse<T>` wrapping the payload. Controllers extend `BaseController` which provides `toResponseSingleResponse()`.

## Configuration

Required environment variables (set via profile or env):
| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `CLERK_JWKS_URI` | Clerk JWKS endpoint for JWT validation |
| `SHORT_URL_SECRET` | Long integer XOR secret for short code obfuscation |
| `APPLICATION_DOMAIN_URL` | Base URL used to format returned short URLs |

The `local` profile (`application-local.properties`) provides all of these pointing at a Supabase Postgres instance and Clerk dev environment.

## Database Migrations

Flyway is used. Migration files live in `src/main/resources/db/migration/` (versioned `V__*.sql`, run in all profiles). Local dev seed data is in `src/main/resources/db/dev/` (repeatable `R__*.sql`, only included when `spring.flyway.locations` includes `classpath:db/dev`, which the local profile does).

Schema: `users` table (keyed by Clerk user ID) and `urls` table (with soft-delete via `deleted_date`, expiry via `expiry_date`, optional password protection).

## Writing Tests

Integration tests live in `src/test/java/com/warp/warp_backend/integration/`.

**Rules:**
- Every integration test class must extend `BaseIntegrationContextTest`
- One test file per API endpoint (e.g. `ShortenUrlTest` for `POST /api/shorten`)
- Use constants from `model/constant/` — never hardcode strings like field names or paths
- Place new test files under `integration/{resource}/` (e.g. `integration/url/ShortenUrlTest`)

**Naming:** `methodName_condition_expectedResult` (e.g. `shorten_emptyDestinationUrl_returns400`)

## Coding Style

- Use `Objects.isNull(x)` / `Objects.nonNull(x)` instead of `x == null` / `x != null`
- Prefer framework or utility methods/constants over raw operators or literals when equivalents exist.
- Avoid manual checks or magic literals when a standard helper provides the same behavior.

## Analytics Plane (ClickHouse)

ClickHouse runs locally on port 8123 (HTTP) / 9000 (native).
Kafka events flow: Spring producer → Kafka topic `url.click.event` → ClickHouse Kafka Engine → `click_events_raw` (MergeTree) → `minute_analytics` (AggregatingMergeTree via MV).

### Tables

**click_events_raw** — raw click events, 30-day TTL
- Columns: event_id (String), url_id (UInt64), user_id (UInt64), short_url (String), timestamp (DateTime64(3, 'UTC')), country_code (LowCardinality(String)), device_type (LowCardinality(String)), browser (LowCardinality(String)), referrer (Nullable(String)), response_latency_ms (UInt64)
- ORDER BY (url_id, timestamp)

**minute_analytics** — 1-minute rollup, AggregatingMergeTree
- Columns: minute (DateTime), url_id (UInt64), country_code (LowCardinality(String)), device_type (LowCardinality(String)), browser (LowCardinality(String)), referrer (LowCardinality(Nullable(String))), clicks (AggregateFunction(count)), avg_latency (AggregateFunction(avg, UInt32))
- ORDER BY (url_id, minute)

### ClickHouse JDBC

Use clickhouse-jdbc driver. Connection: jdbc:clickhouse://localhost:8123/default
No auth required locally (default user, no password).