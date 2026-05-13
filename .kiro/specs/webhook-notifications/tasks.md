# Implementation Tasks: Webhook Notifications

## Task 1: Database Schema and Migrations

### 1.1 Create Webhook Configuration Migration
- [ ] Create Flyway migration file `V5__add_webhook_configuration.sql`
- [ ] Add `webhook_url` column to `api_keys` table (VARCHAR(2048), nullable)
- [ ] Add `webhook_secret` column to `api_keys` table (VARCHAR(64), nullable)
- [ ] Test migration on local database
- [ ] Verify rollback script works correctly

### 1.2 Create Webhook History Table Migration
- [ ] Create Flyway migration file `V6__create_webhook_history.sql`
- [ ] Create `webhook_history` table with all required columns
- [ ] Add foreign key constraint to `api_keys` table
- [ ] Create unique index on (api_key_id, event_type, year_month) for deduplication
- [ ] Create index on (api_key_id, sent_at DESC) for history retrieval
- [ ] Create index on (delivery_status, sent_at DESC) for monitoring
- [ ] Test migration on local database

### 1.3 Update ApiKey Entity
- [ ] Add `webhookUrl` field to ApiKey entity
- [ ] Add `webhookSecret` field to ApiKey entity
- [ ] Update ApiKey builder to include webhook fields
- [ ] Add getter/setter methods
- [ ] Update existing tests to handle new nullable fields

## Task 2: Common DTOs and Models

### 2.1 Create WebhookPayload DTO
- [ ] Create `WebhookPayload` record in `common` module
- [ ] Add fields: eventType, apiKeyId, currentUsage, quotaLimit, usagePercentage, timestamp, yearMonth
- [ ] Implement `toJson()` method using Jackson
- [ ] Implement `fromJson()` static method using Jackson
- [ ] Add validation annotations (@NotNull, @Pattern for eventType)
- [ ] Write unit tests for serialization/deserialization

### 2.2 Create WebhookConfig Value Object
- [ ] Create `WebhookConfig` record in `common` module
- [ ] Add fields: apiKeyId, webhookUrl, webhookSecret, enabled
- [ ] Implement `isEnabled()` method (checks url is not null/blank)
- [ ] Add validation for HTTPS protocol
- [ ] Write unit tests for validation logic

### 2.3 Create WebhookNotification DTO
- [ ] Create `WebhookNotification` record in `common` module
- [ ] Add fields: apiKeyId, eventType, webhookUrl, webhookSecret, payload
- [ ] Add builder pattern support
- [ ] Write unit tests

## Task 3: Webhook History Entity and Repository

### 3.1 Create WebhookHistory Entity
- [ ] Create `WebhookHistory` entity in `usage-service`
- [ ] Add all required fields matching database schema
- [ ] Add JPA annotations (@Entity, @Table, @Column)
- [ ] Add audit annotations (@CreatedDate)
- [ ] Configure entity listeners for auditing
- [ ] Write unit tests for entity creation

### 3.2 Create WebhookHistoryRepository
- [ ] Create `WebhookHistoryRepository` interface extending JpaRepository
- [ ] Add method: `existsByApiKeyIdAndEventTypeAndYearMonth`
- [ ] Add method: `findByApiKeyIdOrderBySentAtDesc` with Pageable
- [ ] Add method: `findByDeliveryStatusAndSentAtAfter` for monitoring
- [ ] Write repository integration tests using @DataJpaTest

## Task 4: Webhook Configuration Service (API Management)

### 4.1 Create WebhookConfigService
- [ ] Create `WebhookConfigService` class in `api-management-service`
- [ ] Inject ApiKeyRepository and OwnerRepository
- [ ] Implement `configureWebhook(UUID apiKeyId, String webhookUrl, String ownerEmail)` method
- [ ] Implement `updateWebhook(UUID apiKeyId, String webhookUrl, String ownerEmail)` method
- [ ] Implement `getWebhookConfig(UUID apiKeyId)` method
- [ ] Implement `getWebhookSecret(UUID apiKeyId, String ownerEmail)` method
- [ ] Add owner validation in all methods
- [ ] Write unit tests with mocked repositories

### 4.2 Implement URL Validation
- [ ] Create `WebhookUrlValidator` utility class
- [ ] Implement HTTPS protocol validation
- [ ] Implement URL format validation using Java URL class
- [ ] Implement length validation (max 2048 characters)
- [ ] Add optional validation for localhost/private IPs (configurable)
- [ ] Write comprehensive unit tests for all validation scenarios

### 4.3 Implement Secret Generation
- [ ] Create `WebhookSecretGenerator` utility class
- [ ] Implement secure random secret generation (32 bytes, Base64 encoded)
- [ ] Add method to generate secret when API key is created
- [ ] Update ApiKeyService to generate webhook secret on key creation
- [ ] Write unit tests for secret generation

## Task 5: Webhook Delivery Service (Usage Service)

### 5.1 Create WebhookDeliveryService
- [ ] Create `WebhookDeliveryService` class in `usage-service`
- [ ] Inject RestTemplate/WebClient for HTTP calls
- [ ] Inject WebhookHistoryRepository
- [ ] Implement `deliverWebhook(WebhookNotification notification)` method
- [ ] Implement `deliverWebhookAsync(WebhookNotification notification)` with @Async
- [ ] Configure async executor with thread pool
- [ ] Write unit tests with mocked HTTP client

### 5.2 Implement HMAC Signature Generation
- [ ] Create `HmacSignatureGenerator` utility class
- [ ] Implement `generateSignature(String timestamp, String payload, String secret)` method
- [ ] Use HmacSHA256 algorithm
- [ ] Return hex-encoded signature string
- [ ] Implement `verifySignature(String signature, String timestamp, String payload, String secret)` method
- [ ] Use constant-time comparison for security
- [ ] Write unit tests with known test vectors

### 5.3 Implement HTTP Delivery with Retry
- [ ] Implement `sendWithRetry(String url, String payload, Map<String, String> headers)` method
- [ ] Configure HTTP client with 10-second timeout
- [ ] Implement retry logic: max 3 retries with exponential backoff (1s, 2s, 4s)
- [ ] Handle connection timeouts, connection refused, DNS failures
- [ ] Return DeliveryResult with status, httpCode, retryCount, errorMessage
- [ ] Write unit tests with WireMock for various scenarios

### 5.4 Implement Delivery History Recording
- [ ] Implement `recordDeliveryAttempt(WebhookNotification notification, DeliveryResult result)` method
- [ ] Create WebhookHistory entity from notification and result
- [ ] Save to database
- [ ] Handle duplicate key violations gracefully (log and ignore)
- [ ] Write unit tests with mocked repository

## Task 6: Webhook Trigger Service (Usage Service)

### 6.1 Create WebhookTriggerService
- [ ] Create `WebhookTriggerService` class in `usage-service`
- [ ] Inject WebhookDeliveryService
- [ ] Inject WebhookHistoryRepository
- [ ] Inject ApiKeyRepository (or create internal API client)
- [ ] Write unit tests with mocked dependencies

### 6.2 Implement Threshold Detection
- [ ] Implement `checkAndTriggerWebhook(String apiKeyId, long currentUsage, long quotaLimit, String yearMonth)` method
- [ ] Calculate usage percentage: (currentUsage / quotaLimit) * 100
- [ ] Check if 100% threshold reached and not already sent
- [ ] Check if 80% threshold reached and not already sent
- [ ] Trigger appropriate webhook notification
- [ ] Write unit tests for all threshold scenarios

### 6.3 Implement Deduplication Check
- [ ] Implement `hasNotificationBeenSent(UUID apiKeyId, String eventType, String yearMonth)` method
- [ ] Query WebhookHistoryRepository for existing notification
- [ ] Return boolean result
- [ ] Write unit tests

### 6.4 Integrate with Quota Enforcement
- [ ] Locate quota enforcement logic in usage-service
- [ ] Add call to `checkAndTriggerWebhook` after usage is recorded
- [ ] Pass apiKeyId, currentUsage, quotaLimit, yearMonth
- [ ] Ensure integration doesn't block main request flow (async)
- [ ] Write integration tests

## Task 7: Webhook Management API (API Management)

### 7.1 Create WebhookController
- [ ] Create `WebhookController` class in `api-management-service`
- [ ] Add @RestController and @RequestMapping("/api/keys/{keyId}/webhook")
- [ ] Inject WebhookConfigService
- [ ] Inject AuthService for owner validation
- [ ] Add @PreAuthorize annotations for security

### 7.2 Implement Configure Webhook Endpoint
- [ ] Create POST endpoint for webhook configuration
- [ ] Create `WebhookConfigRequest` DTO with webhookUrl field
- [ ] Validate request body
- [ ] Call WebhookConfigService.configureWebhook
- [ ] Return 200 OK with success message
- [ ] Handle validation errors with 400 Bad Request
- [ ] Write controller tests with MockMvc

### 7.3 Implement Update Webhook Endpoint
- [ ] Create PUT endpoint for webhook update
- [ ] Reuse `WebhookConfigRequest` DTO
- [ ] Call WebhookConfigService.updateWebhook
- [ ] Return 200 OK with success message
- [ ] Write controller tests

### 7.4 Implement Get Webhook Config Endpoint
- [ ] Create GET endpoint for webhook configuration
- [ ] Create `WebhookConfigResponse` DTO
- [ ] Call WebhookConfigService.getWebhookConfig
- [ ] Include webhook URL and secret in response
- [ ] Return 200 OK with configuration
- [ ] Write controller tests

### 7.5 Implement Get Webhook History Endpoint
- [ ] Create GET endpoint for webhook history
- [ ] Add query parameter: limit (default 50, max 100)
- [ ] Create `WebhookHistoryResponse` DTO
- [ ] Query WebhookHistoryRepository (via service or internal API)
- [ ] Return list ordered by sentAt DESC
- [ ] Write controller tests

### 7.6 Implement Test Webhook Endpoint
- [ ] Create POST endpoint for test webhook
- [ ] Validate API key has webhook configured
- [ ] Create test webhook notification with eventType "quota.test"
- [ ] Call WebhookDeliveryService.deliverWebhook
- [ ] Return delivery result in response
- [ ] Write controller tests

## Task 8: Property-Based Tests

### 8.1 Setup JUnit QuickCheck
- [ ] Add JUnit QuickCheck dependency to usage-service
- [ ] Add JUnit QuickCheck dependency to api-management-service
- [ ] Configure test runners
- [ ] Create base test class with common generators

### 8.2 Webhook Validation Properties
- [ ] Create `WebhookValidationPropertiesTest` class
- [ ] Write property test for HTTPS protocol validation (Property 1)
- [ ] Write property test for URL format validation (Property 2)
- [ ] Write property test for webhook configuration round-trip (Property 3)
- [ ] Configure 100 iterations per test
- [ ] Add property tags and comments

### 8.3 Webhook Payload Properties
- [ ] Create `WebhookPayloadPropertiesTest` class
- [ ] Write property test for payload completeness (Property 7)
- [ ] Write property test for payload serialization round-trip (Property 8)
- [ ] Create custom generators for WebhookPayload
- [ ] Configure 100 iterations per test

### 8.4 Webhook Delivery Properties
- [ ] Create `WebhookDeliveryPropertiesTest` class
- [ ] Write property test for threshold detection (Property 4)
- [ ] Write property test for notification idempotence (Property 5)
- [ ] Write property test for usage percentage calculation (Property 6)
- [ ] Write property test for required HTTP headers (Property 9)
- [ ] Write property test for retry count limit (Property 10)
- [ ] Write property test for final delivery status (Property 11)
- [ ] Configure 100 iterations per test

### 8.5 Webhook Security Properties
- [ ] Create `WebhookSecurityPropertiesTest` class
- [ ] Write property test for HMAC signature determinism (Property 12)
- [ ] Write property test for HMAC signature verification (Property 13)
- [ ] Write property test for invalid signature detection (Property 14)
- [ ] Create custom generators for payloads, timestamps, secrets
- [ ] Configure 100 iterations per test

### 8.6 Webhook History Properties
- [ ] Create `WebhookHistoryPropertiesTest` class
- [ ] Write property test for history completeness (Property 15)
- [ ] Write property test for history ordering (Property 16)
- [ ] Write property test for owner-based access control (Property 17)
- [ ] Write property test for webhook parser error handling (Property 18)
- [ ] Configure 100 iterations per test

## Task 9: Integration Tests

### 9.1 Webhook Delivery Integration Tests
- [ ] Create `WebhookDeliveryIntegrationTest` class
- [ ] Setup WireMock server for webhook endpoint simulation
- [ ] Test successful delivery (200 OK)
- [ ] Test retry on 503 Service Unavailable
- [ ] Test retry on connection timeout
- [ ] Test final failure after all retries
- [ ] Verify HTTP headers in requests
- [ ] Verify payload structure

### 9.2 Webhook Configuration Integration Tests
- [ ] Create `WebhookConfigurationIntegrationTest` class
- [ ] Test full flow: configure → retrieve → update
- [ ] Test owner validation (access denied for non-owners)
- [ ] Test invalid URL rejection
- [ ] Test null URL handling
- [ ] Use @SpringBootTest with test database

### 9.3 End-to-End Webhook Tests
- [ ] Create `WebhookEndToEndTest` class
- [ ] Test complete flow: configure webhook → trigger quota threshold → verify delivery → check history
- [ ] Test 80% threshold warning
- [ ] Test 100% threshold exceeded
- [ ] Test deduplication (no duplicate notifications in same month)
- [ ] Test test webhook trigger
- [ ] Use @SpringBootTest with WireMock

## Task 10: Unit Tests

### 10.1 URL Validation Unit Tests
- [ ] Test HTTPS validation with http://, https://, ftp:// URLs
- [ ] Test malformed URLs
- [ ] Test null and empty URLs
- [ ] Test URL length limits
- [ ] Test localhost and private IP blocking (if enabled)

### 10.2 HMAC Signature Unit Tests
- [ ] Test signature generation with known test vectors
- [ ] Test signature verification with valid signatures
- [ ] Test signature verification with invalid signatures
- [ ] Test signature verification with modified payloads
- [ ] Test signature verification with modified timestamps
- [ ] Test constant-time comparison

### 10.3 Threshold Detection Unit Tests
- [ ] Test 79% usage (no notification)
- [ ] Test 80% usage (warning notification)
- [ ] Test 81% usage (warning notification)
- [ ] Test 99% usage (warning notification)
- [ ] Test 100% usage (exceeded notification)
- [ ] Test 101% usage (exceeded notification)
- [ ] Test edge cases: usage=0, quota=0, usage>quota

### 10.4 Retry Logic Unit Tests
- [ ] Test exponential backoff timing (1s, 2s, 4s)
- [ ] Test success on first attempt (no retries)
- [ ] Test success on second attempt (1 retry)
- [ ] Test success on third attempt (2 retries)
- [ ] Test success on fourth attempt (3 retries)
- [ ] Test failure after all retries
- [ ] Test retry count limit (max 3 retries)

### 10.5 History Recording Unit Tests
- [ ] Test all fields populated correctly
- [ ] Test error message recording
- [ ] Test retry count recording
- [ ] Test delivery status recording (SUCCESS/FAILED)
- [ ] Test duplicate prevention

## Task 11: Documentation and Configuration

### 11.1 API Documentation
- [ ] Add OpenAPI/Swagger annotations to WebhookController
- [ ] Document request/response schemas
- [ ] Document error responses
- [ ] Add example requests and responses
- [ ] Generate API documentation

### 11.2 Configuration Properties
- [ ] Add webhook configuration to application.yaml
- [ ] Configure HTTP client timeout (10 seconds)
- [ ] Configure retry settings (max attempts, backoff delays)
- [ ] Configure async executor thread pool
- [ ] Add feature flag for webhook functionality
- [ ] Document all configuration properties

### 11.3 Monitoring and Metrics
- [ ] Add Micrometer metrics for webhook delivery success rate
- [ ] Add metrics for delivery latency
- [ ] Add metrics for retry rate
- [ ] Add metrics for failure rate by error type
- [ ] Configure metric export to monitoring system
- [ ] Create Grafana dashboard (optional)

### 11.4 Developer Documentation
- [ ] Create webhook integration guide for API owners
- [ ] Document webhook payload structure
- [ ] Document HMAC signature verification process
- [ ] Provide code examples for signature verification
- [ ] Document retry behavior and best practices
- [ ] Add troubleshooting guide

## Task 12: Security and Production Readiness

### 12.1 Security Hardening
- [ ] Implement localhost blocking in URL validation
- [ ] Implement private IP range blocking (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
- [ ] Add rate limiting on webhook configuration endpoints
- [ ] Implement webhook secret rotation endpoint
- [ ] Add audit logging for webhook configuration changes
- [ ] Review and test authorization on all endpoints

### 12.2 Error Handling and Logging
- [ ] Add structured logging for webhook delivery attempts
- [ ] Add error logging for validation failures
- [ ] Add warning logging for retry attempts
- [ ] Implement log correlation IDs for tracing
- [ ] Configure log levels appropriately
- [ ] Test log output in various scenarios

### 12.3 Performance Optimization
- [ ] Add database indexes (verify with EXPLAIN)
- [ ] Configure connection pooling for HTTP client
- [ ] Optimize async executor thread pool size
- [ ] Add caching for webhook configuration (if needed)
- [ ] Test performance under load (100+ concurrent webhooks)
- [ ] Profile and optimize slow queries

### 12.4 Deployment Preparation
- [ ] Create deployment checklist
- [ ] Prepare database migration scripts for production
- [ ] Configure production application properties
- [ ] Setup monitoring and alerting
- [ ] Create rollback plan
- [ ] Document deployment steps

## Task 13: Testing and Quality Assurance

### 13.1 Code Coverage
- [ ] Run code coverage analysis (target: >80%)
- [ ] Identify untested code paths
- [ ] Add tests for uncovered code
- [ ] Review coverage report

### 13.2 Security Testing
- [ ] Test HMAC signature tampering scenarios
- [ ] Test URL validation bypass attempts
- [ ] Test authorization bypass attempts
- [ ] Test replay attack scenarios
- [ ] Run OWASP dependency check
- [ ] Review security scan results

### 13.3 Load Testing
- [ ] Create load test scenarios (100+ concurrent webhooks)
- [ ] Test database performance under load
- [ ] Test HTTP client performance under load
- [ ] Measure delivery latency percentiles (p50, p95, p99)
- [ ] Identify bottlenecks and optimize

### 13.4 Manual Testing
- [ ] Test webhook configuration via API
- [ ] Test webhook delivery with real endpoint
- [ ] Test signature verification with real webhook
- [ ] Test retry behavior with failing endpoint
- [ ] Test history retrieval
- [ ] Test test webhook trigger
- [ ] Verify error messages are user-friendly

## Task 14: Final Integration and Deployment

### 14.1 Integration with Quota Enforcement
- [ ] Verify integration point in usage service
- [ ] Test quota threshold triggers webhook
- [ ] Test 80% and 100% thresholds
- [ ] Test deduplication across billing periods
- [ ] Test with multiple API keys simultaneously

### 14.2 Staging Deployment
- [ ] Deploy to staging environment
- [ ] Run database migrations
- [ ] Verify all services start correctly
- [ ] Run smoke tests
- [ ] Test with staging webhook endpoints
- [ ] Monitor logs for errors

### 14.3 Production Deployment
- [ ] Review deployment checklist
- [ ] Schedule deployment window
- [ ] Deploy to production
- [ ] Run database migrations
- [ ] Verify all services start correctly
- [ ] Run smoke tests
- [ ] Monitor metrics and logs
- [ ] Verify webhook deliveries are working

### 14.4 Post-Deployment Validation
- [ ] Monitor webhook delivery success rate
- [ ] Monitor error rates
- [ ] Check database performance
- [ ] Review logs for unexpected errors
- [ ] Verify monitoring and alerting
- [ ] Collect feedback from early adopters
