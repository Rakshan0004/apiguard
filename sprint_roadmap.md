# API Guard — Development Roadmap (15 Sprints)

This roadmap breaks down the construction of the API Guard platform into 15 logical sprints. Each sprint is designed to deliver a specific set of functional capabilities, moving from foundational infrastructure to a production-ready, feature-rich platform.

---

## Phase 1: Foundation & Core Management

### Sprint 1: Project Scaffolding & Infrastructure
*   **Objective**: Establish the multi-module project structure and local development environment.
*   **Tasks**:
    *   Set up Gradle Multi-Project build (`common`, `gateway`, `management`, `usage`).
    *   Configure Root `build.gradle.kts` with shared dependencies and Java 21 toolchain.
    *   Set up `compose.yaml` with PostgreSQL, Redis, and RabbitMQ.
    *   Create the `common` module for shared DTOs and RabbitMQ constants.
*   **Deliverable**: A buildable project skeleton with all infrastructure running in Docker.

### Sprint 2: API Management - Core Registry
*   **Objective**: Enable users to register their backend APIs.
*   **Tasks**:
    *   Implement `RegisteredApi` entity and PostgreSQL schema.
    *   Create CRUD REST controllers for API registration.
    *   Implement logic to generate unique `proxyPath` slugs.
    *   Set up Flyway migrations for the Management service.
*   **Deliverable**: REST API to register and list backend services to be proxied.

### Sprint 3: API Key & Plan System
*   **Objective**: Secure the registered APIs with hashed API keys.
*   **Tasks**:
    *   Implement `ApiKey` and `Plan` (Free, Basic, Pro) entities.
    *   Create secure API key generation logic (random string + SHA-256 storage).
    *   Add internal endpoints for the Gateway to validate keys.
    *   Implement basic security on Management API itself (Admin/User roles).
*   **Deliverable**: Ability to generate keys for specific APIs and link them to a usage plan.

---

## Phase 2: Gateway & Proxying

### Sprint 4: Gateway Service - Core Routing
*   **Objective**: Implement the basic proxy functionality using Spring Cloud Gateway.
*   **Tasks**:
    *   Set up `gateway-service` with WebFlux and Netty.
    *   Implement dynamic route loading from the Management Service.
    *   Verify request forwarding to a test backend origin.
*   **Deliverable**: Requests to `http://gateway:8080/{proxyPath}` are forwarded correctly to the origin.

### Sprint 5: API Key Authentication in Gateway
*   **Objective**: Intercept requests and validate `X-Api-Key` headers.
*   **Tasks**:
    *   Create a Global `ApiKeyAuthFilter` in the Gateway.
    *   Implement Redis-based caching for API key metadata (hash -> config).
    *   Handle unauthorized (401) and forbidden (403) responses.
*   **Deliverable**: Only requests with a valid, active API key are allowed through the gateway.

### Sprint 6: Rate Limiting (Sliding Window)
*   **Objective**: Enforce requests-per-minute (RPM) limits at the edge.
*   **Tasks**:
    *   Develop a Redis Lua script for the Sliding Window Log algorithm.
    *   Implement a `RateLimitFilter` that executes the Lua script per request.
    *   Return `429 Too Many Requests` with `Retry-After` headers.
*   **Deliverable**: APIs are protected from bursts exceeding their per-minute limits.

---

## Phase 3: Usage Tracking & Async Processing

### Sprint 7: RabbitMQ Event Publishing
*   **Objective**: Capture usage data asynchronously without slowing down requests.
*   **Tasks**:
    *   Implement `UsageLoggingFilter` in the Gateway (post-forwarding).
    *   Create a reactive `UsageEventPublisher`.
    *   Configure the RabbitMQ Topic Exchange and initial routing keys.
*   **Deliverable**: Every successful proxy request triggers a `UsageEvent` message in RabbitMQ.

### Sprint 8: Usage Service - Consumption & Metrics
*   **Objective**: Process events and maintain monthly usage counters.
*   **Tasks**:
    *   Initialize `usage-service` with JPA and PostgreSQL.
    *   Implement the RabbitMQ `UsageEventConsumer`.
    *   Create the `MonthlyUsageSummary` logic (Year-Month partitioning).
    *   Ensure idempotent processing using the `eventId`.
*   **Deliverable**: Real-time aggregation of API usage stored persistently in PostgreSQL.

### Sprint 9: Quota Enforcement & Auto-Disable
*   **Objective**: Stop traffic when the monthly quota is reached.
*   **Tasks**:
    *   Implement a background check in Usage Service after each update.
    *   Create logic to call Management Service internal API to "Deactivate" a key when quota is 100%.
    *   Update Gateway cache to reflect the disabled state immediately.
*   **Deliverable**: Automated protection against over-usage of monthly credits.

---

## Phase 4: Advanced Features & Polish

### Sprint 10: Webhook Notification System
*   **Objective**: Notify users when they are reaching their limits.
*   **Tasks**:
    *   Add `webhookUrl` support to API Keys.
    *   Implement a `WebhookService` in Usage Service.
    *   Trigger `quota.warning` (80%) and `quota.exceeded` notifications.
*   **Deliverable**: Real-time alerts sent to user-defined endpoints.

### Sprint 11: Usage Analytics & Dashboard API
*   **Objective**: Provide data for a future frontend dashboard.
*   **Tasks**:
    *   Add analytics endpoints to Usage/Management services.
    *   Implement queries for status code distribution (2xx vs 4xx vs 5xx).
    *   Calculate latency percentiles (P50, P90, P99).
    *   Provide daily usage breakdown for the current month.
*   **Deliverable**: Rich data sets available via REST for monitoring and reporting.

### Sprint 12: Reliability & Error Handling
*   **Objective**: Ensure zero data loss for usage events.
*   **Tasks**:
    *   Configure RabbitMQ Dead Letter Exchange (DLX) and Dead Letter Queue (DLQ).
    *   Implement a Retry Queue with exponential backoff (30s, 5m, 15m).
    *   Add "Parking Lot" queue for messages failing after all retries.
*   **Deliverable**: A resilient messaging pipeline that handles transient failures gracefully.

---

## Phase 5: Production Readiness

### Sprint 13: Security Hardening & Observability
*   **Objective**: Secure internal traffic and add monitoring.
*   **Tasks**:
    *   Add shared-secret header validation for inter-service communication.
    *   Configure Spring Boot Actuator with Prometheus/Grafana metrics.
    *   Implement centralized logging (ELK or similar stack setup).
    *   Perform a security audit (sensitive data in logs, key rotation).
*   **Deliverable**: A monitored system ready for operational oversight.

### Sprint 14: Load Testing & Performance Tuning
*   **Objective**: Validate the platform under stress.
*   **Tasks**:
    *   Run high-concurrency tests using `k6` or `Gatling`.
    *   Optimize Redis Lua script and ZSET cleanup.
    *   Tune database indexes and connection pools.
    *   Verify the Gateway's reactive performance under load.
*   **Deliverable**: Benchmark reports and a tuned configuration for high throughput.

### Sprint 15: Final Packaging & Deployment
*   **Objective**: Ship the production-ready bundle.
*   **Tasks**:
    *   Create optimized Dockerfiles (multi-stage, Alpine/Distroless).
    *   Write a comprehensive `README.md` and API documentation (Swagger/OpenAPI).
    *   Set up CI/CD pipeline examples (GitHub Actions/GitLab CI).
    *   Conduct a final end-to-end walkthrough.
*   **Deliverable**: A fully documented, containerized, and deployable API Guard platform.
