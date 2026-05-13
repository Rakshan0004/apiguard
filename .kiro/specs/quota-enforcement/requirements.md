# Requirements Document: Quota Enforcement & Auto-Disable

## Introduction

This feature implements automatic enforcement of monthly quotas for API keys in the API Guard platform. When an API key reaches 100% of its allocated monthly quota, the system will automatically disable the key to prevent over-usage. This ensures fair resource allocation, protects backend APIs from abuse, and enables accurate billing based on predefined usage limits.

The quota enforcement system builds on the existing usage tracking infrastructure (Sprint 10) and integrates with the API key management system to provide real-time quota monitoring and automatic key deactivation.

## Glossary

- **API_Key**: A unique credential issued to consumers that grants access to registered APIs through the Gateway
- **Monthly_Quota**: The maximum number of requests an API key is allowed to make within a calendar month, defined in the associated Plan
- **Usage_Service**: The microservice responsible for consuming usage events, aggregating monthly statistics, and enforcing quota limits
- **Management_Service**: The microservice responsible for managing API keys, plans, and API registrations
- **Monthly_Usage_Summary**: A database record tracking the total and successful request counts for an API key within a specific year-month period
- **Gateway**: The proxy service that routes API requests and validates API keys before forwarding to backend services
- **Plan**: A configuration defining rate limits and monthly quotas for API keys
- **Quota_Enforcement**: The process of checking usage against limits and automatically disabling keys that exceed their monthly quota
- **Unlimited_Quota**: A special quota value (-1 or NULL) indicating no monthly limit applies to an API key

## Requirements

### Requirement 1: Quota Checking After Usage Updates

**User Story:** As a platform operator, I want the system to check quota limits after each usage event is processed, so that API keys are disabled as soon as they reach their monthly limit.

#### Acceptance Criteria

1. WHEN a usage event is processed and the Monthly_Usage_Summary is updated, THE Usage_Service SHALL check if the total_requests equals or exceeds the monthly_quota
2. THE Usage_Service SHALL retrieve the monthly_quota value from the API_Key's associated Plan
3. IF the API_Key has an Unlimited_Quota (monthly_quota = -1 or NULL), THEN THE Usage_Service SHALL skip quota enforcement for that key
4. WHEN total_requests >= monthly_quota AND monthly_quota > 0, THE Usage_Service SHALL initiate the key deactivation process
5. THE Usage_Service SHALL perform quota checking within the same transaction as the usage update to ensure atomicity

### Requirement 2: API Key Deactivation Endpoint

**User Story:** As the Usage Service, I want an internal API endpoint to deactivate API keys, so that I can disable keys that have exceeded their quota.

#### Acceptance Criteria

1. THE Management_Service SHALL provide an internal endpoint POST /internal/keys/{keyId}/disable
2. WHEN the disable endpoint is called with a valid keyId, THE Management_Service SHALL set api_keys.active = false in the database
3. WHEN the disable endpoint is called, THE Management_Service SHALL set api_keys.disabled_reason = "QUOTA_EXCEEDED" 
4. THE Management_Service SHALL return HTTP 200 with a success message when deactivation succeeds
5. IF the keyId does not exist, THEN THE Management_Service SHALL return HTTP 404 with an error message
6. THE Management_Service SHALL log all key deactivation actions with timestamp, keyId, and reason

### Requirement 3: REST Client for Inter-Service Communication

**User Story:** As the Usage Service, I want to call the Management Service's internal API, so that I can deactivate keys when quotas are exceeded.

#### Acceptance Criteria

1. THE Usage_Service SHALL implement a REST client to communicate with the Management_Service
2. THE Usage_Service SHALL use Spring's RestTemplate or WebClient for HTTP communication
3. THE Usage_Service SHALL read the Management_Service base URL from application configuration (application.yaml)
4. WHEN the REST client call fails due to network errors, THE Usage_Service SHALL log the error and continue processing
5. WHEN the REST client call fails, THE Usage_Service SHALL NOT throw an exception that would cause the usage event transaction to roll back

### Requirement 4: Gateway Integration with Disabled Keys

**User Story:** As a consumer, I want to receive a clear error message when my API key has been disabled, so that I understand why my requests are being rejected.

#### Acceptance Criteria

1. WHEN the Gateway receives a request with a disabled API_Key (active = false), THE Gateway SHALL return HTTP 403 Forbidden
2. THE Gateway SHALL include the disabled_reason in the error response body
3. THE Gateway SHALL invalidate the Redis cache entry for disabled API keys to ensure immediate enforcement
4. THE Gateway SHALL log all rejected requests from disabled keys with the keyId and disabled_reason

### Requirement 5: Monthly Quota Reset Mechanism

**User Story:** As a platform operator, I want API keys to be automatically re-enabled at the start of each new month, so that customers can continue using the service after their quota resets.

#### Acceptance Criteria

1. WHEN a new calendar month begins, THE system SHALL create new Monthly_Usage_Summary records with total_requests = 0
2. THE Usage_Service SHALL implement a scheduled job that runs at 00:00 UTC on the first day of each month
3. THE scheduled job SHALL identify all API keys with disabled_reason = "QUOTA_EXCEEDED"
4. FOR ALL API keys disabled due to quota, THE scheduled job SHALL call the Management_Service to re-enable them (set active = true, clear disabled_reason)
5. THE scheduled job SHALL log all re-enabled keys with timestamp and keyId

### Requirement 6: Race Condition Handling

**User Story:** As a platform operator, I want the system to handle concurrent requests gracefully, so that multiple requests hitting the quota simultaneously don't cause data inconsistencies.

#### Acceptance Criteria

1. THE Usage_Service SHALL use database-level locking (SELECT FOR UPDATE) when checking and updating quota status
2. WHEN multiple usage events for the same API_Key are processed concurrently, THE Usage_Service SHALL ensure only one deactivation call is made
3. THE Usage_Service SHALL use the @Transactional annotation with appropriate isolation level to prevent race conditions
4. IF an API_Key is already disabled when quota enforcement runs, THE Usage_Service SHALL skip the deactivation call
5. THE Usage_Service SHALL handle duplicate deactivation requests idempotently (calling disable on an already-disabled key succeeds without error)

### Requirement 7: Quota Enforcement Logging and Observability

**User Story:** As a platform operator, I want detailed logs of all quota enforcement actions, so that I can audit the system and troubleshoot issues.

#### Acceptance Criteria

1. WHEN an API_Key reaches its quota, THE Usage_Service SHALL log an INFO message with keyId, current usage, and quota limit
2. WHEN an API_Key is successfully disabled, THE Usage_Service SHALL log an INFO message with keyId and timestamp
3. WHEN a deactivation call fails, THE Usage_Service SHALL log an ERROR message with keyId, error details, and stack trace
4. WHEN the monthly reset job runs, THE Usage_Service SHALL log the number of keys re-enabled
5. THE Usage_Service SHALL include correlation IDs in logs to trace quota enforcement actions across services

### Requirement 8: Performance and Non-Blocking Behavior

**User Story:** As a platform operator, I want quota enforcement to have minimal performance impact, so that usage tracking remains fast and reliable.

#### Acceptance Criteria

1. THE Usage_Service SHALL complete quota checking within 50ms for 95% of requests
2. THE Usage_Service SHALL NOT block the usage event consumer thread while making REST calls to Management_Service
3. WHERE the Management_Service is unavailable, THE Usage_Service SHALL continue processing usage events without failing
4. THE Usage_Service SHALL use connection pooling for REST client calls to minimize overhead
5. THE Usage_Service SHALL implement a timeout of 5 seconds for deactivation API calls to prevent hanging

### Requirement 9: Configuration and Feature Flags

**User Story:** As a platform operator, I want to enable or disable quota enforcement via configuration, so that I can control the feature in different environments.

#### Acceptance Criteria

1. THE Usage_Service SHALL read a configuration property `quota.enforcement.enabled` from application.yaml
2. WHEN quota.enforcement.enabled = false, THE Usage_Service SHALL skip all quota checking logic
3. THE Usage_Service SHALL default quota.enforcement.enabled to true if not specified
4. THE Usage_Service SHALL log the quota enforcement status (enabled/disabled) at application startup
5. WHERE quota enforcement is disabled, THE Usage_Service SHALL still process and aggregate usage events normally

### Requirement 10: Idempotent Deactivation Operations

**User Story:** As a developer, I want deactivation operations to be idempotent, so that retries and duplicate calls don't cause errors or inconsistent state.

#### Acceptance Criteria

1. WHEN the Management_Service receives a disable request for an already-disabled API_Key, THE Management_Service SHALL return HTTP 200 success
2. THE Management_Service SHALL NOT overwrite the disabled_reason if the key is already disabled with a different reason
3. WHEN the Management_Service receives a disable request, THE Management_Service SHALL check the current active status before updating
4. THE Management_Service SHALL use optimistic locking or database constraints to prevent concurrent modification issues
5. THE Management_Service SHALL return a response indicating whether the key was newly disabled or was already disabled

---

## Notes

- **Parser/Serializer Requirements**: Not applicable for this feature
- **External Dependencies**: This feature depends on the existing usage tracking system (Sprint 10) and API key management system (Sprints 3-5)
- **Future Enhancements**: Sprint 12 will add webhook notifications at 80% and 100% quota thresholds
- **Testing Strategy**: Property-based testing should focus on quota calculation logic, boundary conditions (exactly at quota, one over quota), and concurrent request handling. Integration tests should verify the full flow from usage event to key deactivation.
