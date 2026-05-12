# API Guard — Development Roadmap (15 Sprints)

This roadmap breaks down the construction of the API Guard platform into 15 logical sprints. Each sprint is designed to deliver a specific set of functional capabilities, moving from foundational infrastructure to a production-ready, feature-rich platform.

---

## Phase 1: Foundation & Core Management

### ✅ Sprint 1: Project Scaffolding & Infrastructure [DONE]
*   **Objective**: Establish the multi-module project structure and local development environment.
*   **Tasks**:
    *   Set up Gradle Multi-Project build (`common`, `gateway`, `management`, `usage`).
    *   Configure Root `build.gradle.kts` with shared dependencies and Java 21 toolchain.
    *   Set up `compose.yaml` with PostgreSQL, Redis, and RabbitMQ.
    *   Create the `common` module for shared DTOs and RabbitMQ constants.
*   **Deliverable**: A buildable project skeleton with all infrastructure running in Docker.

### ✅ Sprint 2: API Management - Core Registry [DONE]
*   **Objective**: Enable users to register their backend APIs.
*   **Tasks**:
    *   Implement `RegisteredApi` entity and PostgreSQL schema.
    *   Create CRUD REST controllers for API registration.
    *   Implement logic to generate unique `proxyPath` slugs.
*   **Deliverable**: REST API to register and list backend services.

### ✅ Sprint 3: API Key & Plan System [DONE]
*   **Objective**: Secure the registered APIs with hashed API keys and global plans (FREE, BASIC, PRO).
*   **Tasks**:
    *   Implement `ApiKey` and `Plan` entities.
    *   Create secure API key generation logic (random string + SHA-256 storage).
    *   Add internal endpoints for the Gateway to validate keys.
*   **Deliverable**: Ability to generate keys and link them to a global usage plan.

---

## Phase 2: Authentication, Multi-Tenant & Gateway

### ✅ Sprint 4: JWT Authentication for API Owners [DONE]
*   **Objective**: Build a registration/login system so API Owners can authenticate and manage their resources programmatically via JWT.
*   **Tasks**:
    *   Create `Owner` entity (email, hashed password) and Flyway migration.
    *   Implement `POST /api/v1/auth/register` and `POST /api/v1/auth/login` endpoints.
    *   Add `jjwt` dependency and build a `JwtService` (issue, validate, extract claims).
    *   Configure Spring Security with a `JwtAuthFilter` to protect all `/api/v1/**` endpoints.
    *   Refactor existing controllers to extract `ownerEmail` from the JWT `SecurityContext`.
*   **Deliverable**: API Owners can register, log in, receive a JWT, and use it to authenticate all Management API calls.

### ✅ Sprint 5: Multi-Tenant Refactor - Custom Tiers [DONE]
*   **Objective**: Pivot from global plans to user-defined custom tiers, secured behind JWT auth.
*   **Tasks**:
    *   Refactor `Plan` entity to link it to a specific `RegisteredApi`.
    *   Add REST endpoints for API Owners to create/manage their own custom Tiers (e.g. "Basic: 10 RPM", "Gold: 500 RPM").
    *   Update API Key generation to link keys to these owner-defined tiers.
    *   Add ownership validation — owners can only manage plans for their own APIs.
*   **Deliverable**: API Owners can define exactly what limits they offer to their consumers.

### ✅ Sprint 6: Gateway Service - Core Routing [DONE]
*   **Objective**: Implement the basic proxy functionality using Spring Cloud Gateway.
*   **Tasks**:
    *   Set up `gateway-service` with WebFlux and Netty.
    *   Implement dynamic route loading from the Management Service.
    *   Verify request forwarding to a test backend origin.
*   **Deliverable**: Requests to `http://gateway:8080/{proxyPath}` are forwarded correctly to the origin.

### ✅ Sprint 7: API Key Authentication in Gateway [DONE]
*   **Objective**: Intercept requests and validate `X-Api-Key` headers against multi-tenant configs.
*   **Tasks**:
    *   Create a Global `ApiKeyAuthFilter` in the Gateway.
    *   Implement Redis-based caching for API key metadata (linked to custom tiers).
    *   Handle unauthorized (401) and forbidden (403) responses.
*   **Deliverable**: Only requests with a valid, active API key are allowed through the gateway.

### ✅ Sprint 8: Dynamic Multi-Tenant Rate Limiting [DONE]
*   **Objective**: Enforce user-defined custom limits at the Gateway edge.
*   **Tasks**:
    *   Develop a Redis Lua script for high-performance sliding window rate limiting.
    *   Implement a `RateLimitFilter` that pulls the owner-defined RPM limit.
    *   Return `429 Too Many Requests` with rate limit headers.
*   **Deliverable**: Real-time enforcement of whatever custom limits the API owner has set.

---

## Phase 3: Usage Tracking & Async Processing

### Sprint 9: RabbitMQ Event Publishing
*   **Objective**: Capture usage data asynchronously without slowing down requests. [DONE]
*   **Tasks**:
    *   Implement `UsageLoggingFilter` in the Gateway (post-forwarding).
    *   Create a reactive `UsageEventPublisher`.
    *   Configure the RabbitMQ Topic Exchange and initial routing keys.
*   **Deliverable**: Every successful proxy request triggers a `UsageEvent` message in RabbitMQ.

### Sprint 10: Usage Service - Consumption & Metrics
*   **Objective**: Process events and maintain monthly usage counters. [DONE]
*   **Tasks**:
    *   Initialize `usage-service` with JPA and PostgreSQL.
    *   Implement the RabbitMQ `UsageEventConsumer`.
    *   Create the `MonthlyUsageSummary` logic (Year-Month partitioning).
*   **Deliverable**: Real-time aggregation of API usage stored persistently in PostgreSQL.

### Sprint 11: Quota Enforcement & Auto-Disable
*   **Objective**: Stop traffic when the monthly quota is reached.
*   **Tasks**:
    *   Implement a background check in Usage Service after each update.
    *   Create logic to call Management Service internal API to deactivate a key when quota is 100%.
*   **Deliverable**: Automated protection against over-usage of monthly credits.

---

## Phase 4: Advanced Features & Polish

### Sprint 12: Webhook Notification System
*   **Objective**: Notify users when they are reaching their limits.
*   **Tasks**:
    *   Add `webhookUrl` support to API Keys.
    *   Trigger `quota.warning` (80%) and `quota.exceeded` notifications.
*   **Deliverable**: Real-time alerts sent to user-defined endpoints.

### Sprint 13: Usage Analytics & Dashboard API
*   **Objective**: Provide data for a future frontend dashboard.
*   **Tasks**:
    *   Add analytics endpoints to Usage/Management services.
    *   Implement queries for status code distribution and latency percentiles.
*   **Deliverable**: Rich data sets available via REST for monitoring and reporting.

### Sprint 14: Reliability & Error Handling
*   **Objective**: Ensure zero data loss for usage events.
*   **Tasks**:
    *   Configure RabbitMQ Dead Letter Exchange (DLX) and Retry Queues.
    *   Implement exponential backoff and parking lot pattern.
*   **Deliverable**: A resilient messaging pipeline that handles transient failures gracefully.

---

## Phase 5: Production Readiness

### Sprint 15: Security Hardening, Observability & Deployment
*   **Objective**: Secure internal traffic, add monitoring, and ship.
*   **Tasks**:
    *   Add shared-secret header validation for inter-service communication.
    *   Configure Spring Boot Actuator with Prometheus/Grafana metrics.
    *   Create optimized Dockerfiles (multi-stage).
    *   Write comprehensive API documentation (Swagger/OpenAPI).
    *   Conduct a final end-to-end walkthrough.
*   **Deliverable**: A fully documented, containerized, and deployable API Guard platform.
