# Implementation Tasks: Quota Enforcement & Auto-Disable

## Phase 1: Management Service - Internal API Endpoints

### Task 1.1: Create DTOs for Key Management
- [ ] 1.1.1 Create `DisableKeyRequest` record with `reason` field
- [ ] 1.1.2 Create `DisableKeyResponse` record with `message` and `wasAlreadyDisabled` fields
- [ ] 1.1.3 Create `EnableKeyResponse` record with `message` and `wasAlreadyEnabled` fields
- [ ] 1.1.4 Create `ApiKeyDetailsDTO` record with `apiKeyId`, `planName`, `monthlyQuota`, `rateLimitRpm`, `active`, `disabledReason` fields

### Task 1.2: Implement InternalKeyManagementController
- [ ] 1.2.1 Create `InternalKeyManagementController` class with `/internal/keys` base path
- [ ] 1.2.2 Implement `POST /internal/keys/{keyId}/disable` endpoint
  - Accept `DisableKeyRequest` in request body
  - Set `active = false` and `disabled_reason` from request
  - Return `DisableKeyResponse` with idempotent behavior
  - Return 404 if key not found
  - Log all deactivation actions with timestamp, keyId, and reason
- [ ] 1.2.3 Implement `POST /internal/keys/{keyId}/enable` endpoint
  - Set `active = true` and clear `disabled_reason`
  - Return `EnableKeyResponse` with idempotent behavior
  - Return 404 if key not found
  - Log all activation actions
- [ ] 1.2.4 Implement `GET /internal/keys?disabledReason={reason}` endpoint
  - Query keys by `disabled_reason` filter
  - Return list of API key IDs as strings
  - Support optional query parameter

### Task 1.3: Add Repository Methods
- [ ] 1.3.1 Add `findByDisabledReason(String reason)` method to `ApiKeyRepository`
- [ ] 1.3.2 Add database index on `api_keys.disabled_reason` column (if not exists)

### Task 1.4: Implement Cache Invalidation
- [ ] 1.4.1 Inject `RedisTemplate` into key management service
- [ ] 1.4.2 Add cache invalidation logic when key is disabled
- [ ] 1.4.3 Add cache invalidation logic when key is enabled
- [ ] 1.4.4 Use key hash to construct Redis cache key: `api:config:{keyHash}`

### Task 1.5: Unit Tests for Internal API
- [ ] 1.5.1 Test disable endpoint with valid key ID
- [ ] 1.5.2 Test disable endpoint with non-existent key ID (404)
- [ ] 1.5.3 Test disable endpoint idempotency (already disabled key)
- [ ] 1.5.4 Test enable endpoint with valid key ID
- [ ] 1.5.5 Test enable endpoint idempotency (already enabled key)
- [ ] 1.5.6 Test query endpoint with disabled reason filter
- [ ] 1.5.7 Test query endpoint with no results

## Phase 2: Usage Service - REST Client

### Task 2.1: Create Configuration Properties
- [ ] 2.1.1 Create `QuotaEnforcementConfig` class with `@ConfigurationProperties`
- [ ] 2.1.2 Add properties: `enabled`, `managementServiceUrl`, `timeoutMs`, `connectionPoolSize`
- [ ] 2.1.3 Add configuration to `application.yaml` with default values
- [ ] 2.1.4 Add `@EnableConfigurationProperties` to main application class

### Task 2.2: Implement ManagementServiceClient
- [ ] 2.2.1 Create `ManagementServiceClient` interface
- [ ] 2.2.2 Create `ManagementServiceClientImpl` with `WebClient`
- [ ] 2.2.3 Configure `WebClient` bean with connection pooling and timeouts
- [ ] 2.2.4 Implement `disableKey(String apiKeyId)` method
  - Make POST request to `/internal/keys/{keyId}/disable`
  - Return `CompletableFuture<Void>`
  - Log errors but do not throw exceptions
  - Use 5-second timeout
- [ ] 2.2.5 Implement `enableKey(String apiKeyId)` method
  - Make POST request to `/internal/keys/{keyId}/enable`
  - Return `CompletableFuture<Void>`
  - Log errors but do not throw exceptions
- [ ] 2.2.6 Implement `getApiKeyDetails(String apiKeyId)` method
  - Make GET request to `/internal/keys/{keyId}`
  - Return `ApiKeyDetailsDTO`
  - Handle 404 responses gracefully
- [ ] 2.2.7 Implement `getQuotaDisabledKeys()` method
  - Make GET request to `/internal/keys?disabledReason=QUOTA_EXCEEDED`
  - Return `List<String>` of key IDs

### Task 2.3: Unit Tests for REST Client
- [ ] 2.3.1 Test `disableKey()` with successful response
- [ ] 2.3.2 Test `disableKey()` with network timeout
- [ ] 2.3.3 Test `disableKey()` with 404 response
- [ ] 2.3.4 Test `disableKey()` with 500 error
- [ ] 2.3.5 Test `enableKey()` with successful response
- [ ] 2.3.6 Test `getApiKeyDetails()` with valid key
- [ ] 2.3.7 Test `getQuotaDisabledKeys()` with results
- [ ] 2.3.8 Verify no exceptions are thrown on failures

## Phase 3: Usage Service - Quota Enforcement Logic

### Task 3.1: Implement QuotaEnforcementService
- [ ] 3.1.1 Create `QuotaEnforcementService` interface
- [ ] 3.1.2 Create `QuotaEnforcementServiceImpl` class
- [ ] 3.1.3 Implement `checkAndEnforceQuota(String apiKeyId, String yearMonth, long currentUsage)` method
  - Retrieve monthly quota from Management Service
  - Skip enforcement if quota is -1 (unlimited)
  - Skip enforcement if quota is 0 or negative
  - Compare currentUsage with quota
  - If currentUsage >= quota, call `disableKey()` asynchronously
  - Log INFO when quota exceeded
  - Log INFO when key disabled successfully
  - Log ERROR when disable call fails
- [ ] 3.1.4 Implement `getMonthlyQuota(String apiKeyId)` method
  - Call `getApiKeyDetails()` from REST client
  - Extract and return `monthlyQuota` from response
  - Cache quota values to reduce API calls (optional enhancement)

### Task 3.2: Add Database Locking for Quota Check
- [ ] 3.2.1 Create custom repository method with `SELECT FOR UPDATE`
- [ ] 3.2.2 Add `findByApiKeyIdAndYearMonthForUpdate(String apiKeyId, String yearMonth)` to repository
- [ ] 3.2.3 Use native query with `FOR UPDATE OF monthly_usage_summaries`
- [ ] 3.2.4 Ensure method is called within transaction

### Task 3.3: Integrate Quota Enforcement into UsageService
- [ ] 3.3.1 Inject `QuotaEnforcementService` into `UsageService`
- [ ] 3.3.2 Inject `QuotaEnforcementConfig` into `UsageService`
- [ ] 3.3.3 Add quota check after `upsertUsage()` call
- [ ] 3.3.4 Check if `quota.enforcement.enabled` is true before checking quota
- [ ] 3.3.5 Retrieve updated `MonthlyUsageSummary` after upsert
- [ ] 3.3.6 Call `checkAndEnforceQuota()` with current usage
- [ ] 3.3.7 Ensure transaction commits before async disable call
- [ ] 3.3.8 Add correlation ID to MDC for logging

### Task 3.4: Unit Tests for Quota Enforcement
- [ ] 3.4.1 Test quota check when usage equals quota (boundary condition)
- [ ] 3.4.2 Test quota check when usage exceeds quota by 1
- [ ] 3.4.3 Test quota check when usage is below quota
- [ ] 3.4.4 Test quota check with unlimited quota (-1)
- [ ] 3.4.5 Test quota check with zero quota
- [ ] 3.4.6 Test quota check when enforcement is disabled
- [ ] 3.4.7 Test that disable call is made asynchronously
- [ ] 3.4.8 Test error handling when Management Service is unavailable
- [ ] 3.4.9 Test that transaction commits even if disable call fails

## Phase 4: Monthly Reset Scheduler

### Task 4.1: Implement MonthlyResetScheduler
- [ ] 4.1.1 Create `MonthlyResetScheduler` class with `@Component`
- [ ] 4.1.2 Enable scheduling with `@EnableScheduling` on main application class
- [ ] 4.1.3 Implement `resetMonthlyQuotas()` method with `@Scheduled` annotation
- [ ] 4.1.4 Use cron expression: `0 0 0 1 * ?` (00:00 UTC on 1st of month)
- [ ] 4.1.5 Set timezone to UTC in annotation
- [ ] 4.1.6 Call `getQuotaDisabledKeys()` from REST client
- [ ] 4.1.7 Loop through disabled keys and call `enableKey()` for each
- [ ] 4.1.8 Log summary: total keys processed, successful, failed
- [ ] 4.1.9 Handle partial failures gracefully (continue processing remaining keys)
- [ ] 4.1.10 Add correlation ID for tracing

### Task 4.2: Unit Tests for Monthly Reset
- [ ] 4.2.1 Test scheduler execution with mocked REST client
- [ ] 4.2.2 Test with empty list of disabled keys
- [ ] 4.2.3 Test with multiple disabled keys
- [ ] 4.2.4 Test partial failure scenario (some keys fail to enable)
- [ ] 4.2.5 Test logging of summary statistics
- [ ] 4.2.6 Verify cron expression is correct

## Phase 5: Gateway Service - Enhanced Error Handling

### Task 5.1: Update ApiKeyAuthFilter
- [ ] 5.1.1 Modify `filter()` method to check `disabledReason` field
- [ ] 5.1.2 Include `disabledReason` in error response when key is disabled
- [ ] 5.1.3 Update error message format: "API key is disabled. Reason: {reason}"
- [ ] 5.1.4 Log disabled reason in warning message
- [ ] 5.1.5 Ensure HTTP 403 status is returned for disabled keys

### Task 5.2: Update ApiConfigDTO
- [ ] 5.2.1 Add `disabledReason` field to `ApiConfigDTO` record
- [ ] 5.2.2 Update Management Service's `InternalConfigController` to include `disabledReason` in response
- [ ] 5.2.3 Update Gateway's cache service to handle new field

### Task 5.3: Integration Tests for Gateway
- [ ] 5.3.1 Test request with disabled key returns 403
- [ ] 5.3.2 Test error response includes disabled reason
- [ ] 5.3.3 Test request with active key succeeds
- [ ] 5.3.4 Test cache invalidation after key is disabled

## Phase 6: Integration Testing

### Task 6.1: End-to-End Integration Tests
- [ ] 6.1.1 Set up Testcontainers for PostgreSQL, RabbitMQ, Redis
- [ ] 6.1.2 Test full flow: usage event → quota check → key deactivation
  - Create API key with quota = 100
  - Publish 100 usage events
  - Verify key is disabled with reason "QUOTA_EXCEEDED"
  - Verify Gateway rejects requests with 403
- [ ] 6.1.3 Test monthly reset flow
  - Create disabled key with reason "QUOTA_EXCEEDED"
  - Trigger monthly reset job
  - Verify key is re-enabled
  - Verify Gateway accepts requests
- [ ] 6.1.4 Test concurrent usage events for same key
  - Create API key at quota limit
  - Publish 10 concurrent events
  - Verify only one disable call is made
  - Verify all events are persisted
- [ ] 6.1.5 Test unlimited quota behavior
  - Create API key with quota = -1
  - Publish 1000 usage events
  - Verify key is NOT disabled
- [ ] 6.1.6 Test quota enforcement disabled via config
  - Set `quota.enforcement.enabled = false`
  - Publish events exceeding quota
  - Verify key is NOT disabled

### Task 6.2: Performance Tests
- [ ] 6.2.1 Measure quota check latency (target: <50ms for 95th percentile)
- [ ] 6.2.2 Test throughput with quota enforcement enabled vs disabled
- [ ] 6.2.3 Test concurrent event processing for different keys
- [ ] 6.2.4 Verify connection pooling is working correctly

### Task 6.3: Contract Tests
- [ ] 6.3.1 Create contract test for Management Service internal API
- [ ] 6.3.2 Verify disable endpoint contract
- [ ] 6.3.3 Verify enable endpoint contract
- [ ] 6.3.4 Verify query endpoint contract

## Phase 7: Observability and Monitoring

### Task 7.1: Add Prometheus Metrics
- [ ] 7.1.1 Create `QuotaEnforcementMetrics` component
- [ ] 7.1.2 Add counter: `quota_exceeded_total`
- [ ] 7.1.3 Add counter: `key_deactivation_success_total`
- [ ] 7.1.4 Add counter: `key_deactivation_failure_total`
- [ ] 7.1.5 Add timer: `quota_check_duration_seconds`
- [ ] 7.1.6 Add counter: `monthly_reset_keys_enabled_total`
- [ ] 7.1.7 Increment metrics in appropriate service methods

### Task 7.2: Enhanced Logging
- [ ] 7.2.1 Add correlation IDs to all log messages using MDC
- [ ] 7.2.2 Add structured logging for quota exceeded events
- [ ] 7.2.3 Add structured logging for deactivation success/failure
- [ ] 7.2.4 Add structured logging for monthly reset job
- [ ] 7.2.5 Ensure all log levels are appropriate (INFO, WARN, ERROR, DEBUG)

### Task 7.3: Health Checks
- [ ] 7.3.1 Add health indicator for Management Service connectivity
- [ ] 7.3.2 Add health indicator for quota enforcement status
- [ ] 7.3.3 Expose health endpoints via Spring Actuator

## Phase 8: Documentation and Deployment

### Task 8.1: Update API Documentation
- [ ] 8.1.1 Document internal API endpoints in OpenAPI/Swagger
- [ ] 8.1.2 Add examples for disable/enable requests and responses
- [ ] 8.1.3 Document error codes and responses

### Task 8.2: Update Configuration Documentation
- [ ] 8.2.1 Document all quota enforcement configuration properties
- [ ] 8.2.2 Provide example configurations for different environments
- [ ] 8.2.3 Document feature flag usage

### Task 8.3: Deployment Preparation
- [ ] 8.3.1 Update Docker Compose files with new configuration
- [ ] 8.3.2 Update Kubernetes manifests (if applicable)
- [ ] 8.3.3 Create database migration scripts (if needed)
- [ ] 8.3.4 Update environment variable documentation

### Task 8.4: Runbook and Operations Guide
- [ ] 8.4.1 Document how to enable/disable quota enforcement
- [ ] 8.4.2 Document how to manually re-enable a disabled key
- [ ] 8.4.3 Document troubleshooting steps for common issues
- [ ] 8.4.4 Document monitoring and alerting recommendations

## Phase 9: Testing and Validation

### Task 9.1: Manual Testing
- [ ] 9.1.1 Test quota enforcement in local environment
- [ ] 9.1.2 Test monthly reset job manually
- [ ] 9.1.3 Test Gateway rejection of disabled keys
- [ ] 9.1.4 Test cache invalidation behavior
- [ ] 9.1.5 Verify logs and metrics are working

### Task 9.2: Load Testing
- [ ] 9.2.1 Run load test with quota enforcement enabled
- [ ] 9.2.2 Verify system handles high throughput
- [ ] 9.2.3 Verify quota checks don't cause bottlenecks
- [ ] 9.2.4 Verify async disable calls don't block processing

### Task 9.3: Chaos Testing (Optional)
- [ ] 9.3.1 Test behavior when Management Service is down
- [ ] 9.3.2 Test behavior with network latency
- [ ] 9.3.3 Test behavior with database connection issues
- [ ] 9.3.4 Verify graceful degradation

---

## Task Summary

- **Phase 1**: 5 tasks (Management Service Internal API)
- **Phase 2**: 3 tasks (Usage Service REST Client)
- **Phase 3**: 4 tasks (Quota Enforcement Logic)
- **Phase 4**: 2 tasks (Monthly Reset Scheduler)
- **Phase 5**: 3 tasks (Gateway Enhanced Error Handling)
- **Phase 6**: 3 tasks (Integration Testing)
- **Phase 7**: 3 tasks (Observability and Monitoring)
- **Phase 8**: 4 tasks (Documentation and Deployment)
- **Phase 9**: 3 tasks (Testing and Validation)

**Total**: 30 main tasks with 100+ sub-tasks

## Dependencies

- Sprint 10 (Usage Tracking) must be complete
- Sprints 3-5 (API Key Management) must be complete
- RabbitMQ, PostgreSQL, Redis infrastructure must be available

## Estimated Effort

- **Phase 1-2**: 2-3 days (REST API and client)
- **Phase 3-4**: 3-4 days (Core quota enforcement logic)
- **Phase 5**: 1 day (Gateway integration)
- **Phase 6**: 2-3 days (Integration testing)
- **Phase 7**: 1-2 days (Observability)
- **Phase 8-9**: 2-3 days (Documentation and validation)

**Total Estimated Effort**: 11-16 days
