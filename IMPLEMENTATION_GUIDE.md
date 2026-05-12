# Implementation Guide - Sprints 11, 12, 13

## 🚀 Quick Start

You have complete specifications for the next 3 sprints. Here's how to implement them efficiently.

---

## 📋 Sprint Execution Order

### Recommended: Sequential Implementation

```
Sprint 11 (Quota Enforcement)
    ↓ (2-3 weeks)
Sprint 12 (Webhooks)
    ↓ (2-3 weeks)
Sprint 13 (Analytics)
    ↓ (1-2 weeks)
```

**Why this order?**
- Sprint 12 depends on Sprint 11 (webhooks need quota data)
- Sprint 13 depends on both (analytics needs usage and quota data)
- Each sprint builds on the previous one

---

## 🎯 Sprint 11: Quota Enforcement (Start Here!)

### Location
`.kiro/specs/quota-enforcement/`

### Files
- `requirements.md` - 10 requirements with acceptance criteria
- `design.md` - Architecture, components, data models
- `tasks.md` - 9 phases, 30 tasks, 100+ sub-tasks

### Quick Implementation Path

#### Week 1: Core Functionality
**Days 1-2: Management Service Internal API**
```bash
# Task 1: Create DTOs and Controller
- Create DisableKeyRequest, DisableKeyResponse DTOs
- Implement InternalKeyManagementController
- Add POST /internal/keys/{keyId}/disable endpoint
- Add POST /internal/keys/{keyId}/enable endpoint
- Write unit tests
```

**Days 3-4: Usage Service REST Client**
```bash
# Task 2: REST Client
- Create QuotaEnforcementConfig properties
- Implement ManagementServiceClient with WebClient
- Add disableKey() and enableKey() methods
- Configure timeouts and connection pooling
- Write unit tests with WireMock
```

**Day 5: Quota Enforcement Logic**
```bash
# Task 3: Core Logic
- Create QuotaEnforcementService
- Implement checkAndEnforceQuota() method
- Add database locking (SELECT FOR UPDATE)
- Integrate with UsageService
- Write unit tests
```

#### Week 2: Integration & Testing
**Days 6-7: Monthly Reset & Gateway**
```bash
# Task 4: Monthly Reset
- Create MonthlyResetScheduler
- Implement scheduled job (cron: 0 0 0 1 * ?)
- Test scheduler execution

# Task 5: Gateway Integration
- Update ApiKeyAuthFilter error messages
- Add disabled_reason to ApiConfigDTO
- Test Gateway rejection of disabled keys
```

**Days 8-10: Testing & Deployment**
```bash
# Task 6: Integration Tests
- End-to-end test: usage → quota check → key disable
- Test monthly reset flow
- Test concurrent requests
- Performance testing (<50ms quota check)

# Task 7: Deploy
- Run database migrations
- Deploy to staging
- Smoke tests
- Deploy to production
```

### Key Commands

```bash
# Run Sprint 11 tests
./gradlew :usage-service:test --tests "*Quota*"
./gradlew :api-management-service:test --tests "*Internal*"

# Run integration tests
./gradlew :usage-service:integrationTest

# Deploy
docker-compose up -d --build
```

---

## 🎯 Sprint 12: Webhook Notifications

### Location
`.kiro/specs/webhook-notifications/`

### Files
- `requirements.md` - 8 requirements, 18 correctness properties
- `design.md` - Architecture, HMAC security, retry logic
- `tasks.md` - 14 tasks, 80+ sub-tasks

### Quick Implementation Path

#### Week 1: Core Webhook Delivery
**Days 1-2: Database & DTOs**
```bash
# Task 1: Database Schema
- Create V5__add_webhook_configuration.sql migration
- Create V6__create_webhook_history.sql migration
- Run migrations

# Task 2: DTOs
- Create WebhookPayload record
- Create WebhookConfig record
- Create WebhookHistory entity
- Write serialization tests
```

**Days 3-4: Webhook Delivery Service**
```bash
# Task 5: Delivery Service
- Create WebhookDeliveryService
- Implement HTTP POST with 10s timeout
- Implement retry logic (1s, 2s, 4s backoff)
- Add HMAC signature generation
- Write unit tests with WireMock
```

**Day 5: Webhook Trigger**
```bash
# Task 6: Trigger Service
- Create WebhookTriggerService
- Implement threshold detection (80%, 100%)
- Add deduplication logic
- Integrate with quota enforcement
- Write unit tests
```

#### Week 2: Management API & Testing
**Days 6-7: Management API**
```bash
# Task 7: Webhook Controller
- Create WebhookController
- Implement POST /api/keys/{keyId}/webhook (configure)
- Implement GET /api/keys/{keyId}/webhook (get config)
- Implement GET /api/keys/{keyId}/webhook/history
- Implement POST /api/keys/{keyId}/webhook/test
- Write controller tests
```

**Days 8-10: Property-Based Tests & Deployment**
```bash
# Task 8: Property-Based Tests
- Setup JUnit QuickCheck
- Write 18 property tests (100 iterations each)
- Test HMAC signatures, URL validation, payload round-trip

# Task 9: Integration Tests
- End-to-end webhook delivery test
- Test retry scenarios
- Test signature verification

# Task 10: Deploy
- Deploy to staging
- Test with real webhook endpoint
- Deploy to production
```

### Key Commands

```bash
# Run Sprint 12 tests
./gradlew :usage-service:test --tests "*Webhook*"
./gradlew :api-management-service:test --tests "*Webhook*"

# Run property-based tests
./gradlew :usage-service:test --tests "*PropertiesTest"

# Test webhook delivery locally
curl -X POST http://localhost:8081/api/keys/{keyId}/webhook \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: application/json" \
  -d '{"webhookUrl": "https://webhook.site/your-unique-url"}'
```

---

## 🎯 Sprint 13: Usage Analytics (Future)

### When to Implement
- After Sprints 11 & 12 are in production
- When customers request analytics features
- When you have bandwidth for nice-to-have features

### Quick Spec Creation
When ready, create spec using:
```bash
# Use the spec workflow orchestrator
# It will guide you through requirements → design → tasks
```

### Estimated Scope
- 8-12 days implementation
- REST API endpoints for analytics
- Redis caching for performance
- CSV/JSON export capabilities

---

## 🧪 Testing Strategy

### Unit Tests
```bash
# Run all unit tests
./gradlew test

# Run specific module tests
./gradlew :usage-service:test
./gradlew :api-management-service:test
./gradlew :gateway-service:test
```

### Integration Tests
```bash
# Run integration tests with Testcontainers
./gradlew integrationTest

# This will start:
# - PostgreSQL container
# - Redis container
# - RabbitMQ container
```

### Property-Based Tests
```bash
# Run property tests (JUnit QuickCheck)
./gradlew test --tests "*PropertiesTest"

# Each property runs 100 iterations
# Look for: @Property(trials = 100)
```

### Manual Testing
```bash
# 1. Start all services
docker-compose up -d

# 2. Register API owner
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}'

# 3. Create API key with quota
# (Use Postman collection or curl commands)

# 4. Make requests until quota is reached
for i in {1..100}; do
  curl -H "X-Api-Key: your-key" http://localhost:8080/proxy/your-api/test
done

# 5. Verify key is disabled
# Check database: SELECT * FROM api_keys WHERE id = 'your-key-id';
# Should see: active = false, disabled_reason = 'QUOTA_EXCEEDED'

# 6. Test webhook (Sprint 12)
# Configure webhook URL
# Trigger quota threshold
# Check webhook.site for received webhook
```

---

## 📊 Monitoring & Observability

### Metrics to Track

**Sprint 11 (Quota Enforcement):**
```
quota_exceeded_total
key_deactivation_success_total
key_deactivation_failure_total
quota_check_duration_seconds
monthly_reset_keys_enabled_total
```

**Sprint 12 (Webhooks):**
```
webhook_delivery_success_total
webhook_delivery_failure_total
webhook_delivery_duration_seconds
webhook_retry_total
webhook_signature_generation_duration_seconds
```

### Logging

**Important Log Messages:**
```
INFO: Quota exceeded for key: {keyId}, usage: {usage}/{quota}
INFO: API key disabled successfully: keyId={keyId}
ERROR: Failed to disable API key: keyId={keyId}, error={error}
INFO: Webhook delivered successfully: keyId={keyId}, eventType={type}
ERROR: Webhook delivery failed: keyId={keyId}, error={error}
```

### Health Checks
```bash
# Check service health
curl http://localhost:8081/actuator/health
curl http://localhost:8080/actuator/health
curl http://localhost:8082/actuator/health

# Check metrics
curl http://localhost:8082/actuator/metrics/quota_exceeded_total
curl http://localhost:8082/actuator/metrics/webhook_delivery_success_total
```

---

## 🐛 Troubleshooting

### Sprint 11 Issues

**Issue: Keys not being disabled**
```bash
# Check logs
docker logs -f apiguard-usage-service-1 | grep "Quota exceeded"

# Check database
docker exec -it apiguard-postgres-1 psql -U apiguard -d apiguard
SELECT * FROM monthly_usage_summaries WHERE api_key_id = 'your-key-id';
SELECT * FROM api_keys WHERE id = 'your-key-id';

# Check configuration
# Verify quota.enforcement.enabled = true in application.yaml
```

**Issue: Monthly reset not working**
```bash
# Check scheduler logs
docker logs -f apiguard-usage-service-1 | grep "Monthly reset"

# Manually trigger reset (for testing)
# Call the scheduled method directly in code or via actuator
```

### Sprint 12 Issues

**Issue: Webhooks not being sent**
```bash
# Check webhook configuration
curl -H "Authorization: Bearer YOUR_JWT" \
  http://localhost:8081/api/keys/{keyId}/webhook

# Check webhook history
curl -H "Authorization: Bearer YOUR_JWT" \
  http://localhost:8081/api/keys/{keyId}/webhook/history

# Check logs
docker logs -f apiguard-usage-service-1 | grep "Webhook"
```

**Issue: Webhook signature verification failing**
```bash
# Verify signature generation
# Use the HmacSignatureGenerator utility
# Compare with expected signature

# Check timestamp
# Ensure timestamp is in milliseconds (not seconds)

# Check secret
# Verify webhook_secret in database matches what you're using
```

---

## 📦 Deployment Checklist

### Pre-Deployment

- [ ] All tests passing (unit, integration, property-based)
- [ ] Code review completed
- [ ] Database migrations tested
- [ ] Configuration updated (application.yaml)
- [ ] Documentation updated
- [ ] Monitoring and alerting configured

### Deployment Steps

1. **Backup Database**
   ```bash
   docker exec apiguard-postgres-1 pg_dump -U apiguard apiguard > backup.sql
   ```

2. **Run Migrations**
   ```bash
   # Migrations run automatically on service startup
   # Or run manually:
   ./gradlew flywayMigrate
   ```

3. **Deploy Services**
   ```bash
   docker-compose down
   docker-compose up -d --build
   ```

4. **Verify Deployment**
   ```bash
   # Check all services are running
   docker-compose ps
   
   # Check health endpoints
   curl http://localhost:8081/actuator/health
   curl http://localhost:8080/actuator/health
   curl http://localhost:8082/actuator/health
   ```

5. **Smoke Tests**
   ```bash
   # Test quota enforcement
   # Test webhook delivery
   # Check logs for errors
   ```

### Post-Deployment

- [ ] Monitor error rates
- [ ] Monitor performance metrics
- [ ] Check logs for unexpected errors
- [ ] Verify quota enforcement is working
- [ ] Verify webhooks are being delivered
- [ ] Collect feedback from users

---

## 💡 Pro Tips

### Development Workflow

1. **Read the spec first** - Don't skip the requirements and design docs
2. **Follow the task list** - Tasks are ordered for a reason
3. **Write tests first** - TDD makes debugging easier
4. **Test incrementally** - Don't wait until the end
5. **Use property-based tests** - They catch edge cases you didn't think of

### Code Quality

1. **Follow existing patterns** - Match the style of Sprints 1-10
2. **Add comprehensive logging** - You'll thank yourself later
3. **Handle errors gracefully** - Don't let exceptions crash the service
4. **Document as you go** - Future you will appreciate it
5. **Keep methods small** - Single responsibility principle

### Performance

1. **Use async where appropriate** - Don't block the main thread
2. **Add database indexes** - Check query performance with EXPLAIN
3. **Cache expensive operations** - Use Redis for frequently accessed data
4. **Monitor metrics** - Know your performance baseline
5. **Load test before production** - Find bottlenecks early

---

## 🎉 Success Criteria

### Sprint 11 Complete When:
- [ ] API keys automatically disabled at 100% quota
- [ ] Gateway rejects requests from disabled keys with 403
- [ ] Monthly reset job re-enables keys on 1st of month
- [ ] All tests passing (unit, integration)
- [ ] Deployed to production
- [ ] Monitoring shows quota enforcement working

### Sprint 12 Complete When:
- [ ] Webhooks sent at 80% and 100% thresholds
- [ ] HMAC signatures verify correctly
- [ ] Retry logic works (3 retries with exponential backoff)
- [ ] Webhook history tracks all deliveries
- [ ] Test webhook endpoint works
- [ ] All 18 property tests passing (100 iterations each)
- [ ] Deployed to production
- [ ] Monitoring shows webhooks being delivered

### Sprint 13 Complete When:
- [ ] Analytics endpoints return correct data
- [ ] Queries perform well (< 1 second for most queries)
- [ ] Redis caching reduces database load
- [ ] CSV/JSON export works
- [ ] All tests passing
- [ ] Deployed to production
- [ ] Users can view their usage analytics

---

## 📞 Need Help?

### Resources

- **Spec Documents**: `.kiro/specs/{sprint-name}/`
- **Task Lists**: `.kiro/specs/{sprint-name}/tasks.md`
- **Design Docs**: `.kiro/specs/{sprint-name}/design.md`
- **Requirements**: `.kiro/specs/{sprint-name}/requirements.md`

### Common Questions

**Q: Can I skip Sprint 11 and go straight to Sprint 12?**
A: No, Sprint 12 depends on Sprint 11. Webhooks need quota data.

**Q: Can I implement Sprint 13 before Sprint 12?**
A: Yes, Sprint 13 only depends on Sprint 10 (usage tracking).

**Q: How long will each sprint take?**
A: Sprint 11: 2-3 weeks, Sprint 12: 2-3 weeks, Sprint 13: 1-2 weeks

**Q: Do I need to implement all sub-tasks?**
A: Yes, for production quality. But you can skip optional tasks marked with asterisk (*).

**Q: Can I modify the design?**
A: Yes, but document changes and update the spec. The design is a guide, not a prison.

---

## ✅ Ready to Start!

You have everything you need:
- ✅ Complete specifications
- ✅ Detailed task lists
- ✅ Implementation guide
- ✅ Testing strategies
- ✅ Deployment checklists

**Start with Sprint 11, Task 1.1: Create DTOs for Key Management**

Good luck! 🚀
