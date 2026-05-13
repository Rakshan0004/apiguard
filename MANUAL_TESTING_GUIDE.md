# Manual Testing Guide: Sprints 11 & 12

## Prerequisites

✅ All services running:
- API Management Service (port 8081)
- Gateway Service (port 8080)
- Usage Service (port 8082)

✅ Infrastructure running:
- PostgreSQL (port 5435)
- Redis (port 6379)
- RabbitMQ (ports 5672, 15672)

## Test Setup

### Step 1: Create Test Owner and Login

```bash
# Register a test owner
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!@#",
    "organizationName": "Test Org"
  }'

# Login to get JWT token
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!@#"
  }'
```

**Save the JWT token from the response for subsequent requests.**

### Step 2: Create Test Plan

```bash
# Create a plan with quota limit
curl -X POST http://localhost:8081/api/plans \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Plan",
    "description": "Plan for testing quota enforcement",
    "monthlyQuota": 100,
    "rateLimitRpm": 60
  }'
```

**Save the plan ID from the response.**

### Step 3: Register Test API

```bash
# Register an API
curl -X POST http://localhost:8081/api/apis \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test API",
    "baseUrl": "https://jsonplaceholder.typicode.com",
    "description": "Test API for quota enforcement"
  }'
```

**Save the API ID from the response.**

### Step 4: Create API Key

```bash
# Create an API key
curl -X POST http://localhost:8081/api/keys \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "apiId": "YOUR_API_ID",
    "planId": "YOUR_PLAN_ID",
    "name": "Test Key"
  }'
```

**Save the API key value and key ID from the response.**

---

## Sprint 11: Quota Enforcement Tests

### Test 1: Verify Internal API - Get Key Details

```bash
# Get API key details via internal API
curl -X GET http://localhost:8081/internal/keys/YOUR_KEY_ID
```

**Expected Response:**
```json
{
  "apiKeyId": "uuid",
  "planName": "Test Plan",
  "monthlyQuota": 100,
  "rateLimitRpm": 60,
  "active": true,
  "disabledReason": null
}
```

### Test 2: Manual Key Disable

```bash
# Disable the API key manually
curl -X POST http://localhost:8081/internal/keys/YOUR_KEY_ID/disable \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "MANUAL_TEST"
  }'
```

**Expected Response:**
```json
{
  "message": "API key disabled successfully",
  "wasAlreadyDisabled": false
}
```

**Verification:**
```bash
# Verify key is disabled
curl -X GET http://localhost:8081/internal/keys/YOUR_KEY_ID
```

Should show `"active": false` and `"disabledReason": "MANUAL_TEST"`

### Test 3: Gateway Rejects Disabled Key

```bash
# Try to use the disabled key via Gateway
curl -X GET http://localhost:8080/test-api/posts/1 \
  -H "X-API-Key: YOUR_API_KEY_VALUE"
```

**Expected Response:** HTTP 403 Forbidden
```json
{
  "error": "API key is disabled. Reason: MANUAL_TEST"
}
```

### Test 4: Manual Key Enable

```bash
# Re-enable the API key
curl -X POST http://localhost:8081/internal/keys/YOUR_KEY_ID/enable
```

**Expected Response:**
```json
{
  "message": "API key enabled successfully",
  "wasAlreadyEnabled": false
}
```

**Verification:**
```bash
# Verify key is enabled
curl -X GET http://localhost:8081/internal/keys/YOUR_KEY_ID
```

Should show `"active": true` and `"disabledReason": null`

### Test 5: Gateway Accepts Enabled Key

```bash
# Try to use the enabled key via Gateway
curl -X GET http://localhost:8080/test-api/posts/1 \
  -H "X-API-Key: YOUR_API_KEY_VALUE"
```

**Expected Response:** HTTP 200 OK with data from jsonplaceholder

### Test 6: Automatic Quota Enforcement

This test requires publishing usage events to trigger quota enforcement.

**Option A: Via RabbitMQ (if you have rabbitmqadmin)**
```bash
# Publish 100 usage events to exceed quota
for i in {1..100}; do
  curl -X POST http://localhost:15672/api/exchanges/%2F/usage.events/publish \
    -u guest:guest \
    -H "Content-Type: application/json" \
    -d "{
      \"properties\": {},
      \"routing_key\": \"usage.event\",
      \"payload\": \"{\\\"apiKeyId\\\":\\\"YOUR_KEY_ID\\\",\\\"timestamp\\\":\\\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\\\",\\\"endpoint\\\":\\\"/test\\\",\\\"statusCode\\\":200,\\\"responseTimeMs\\\":50}\",
      \"payload_encoding\": \"string\"
    }"
done
```

**Option B: Via Gateway (generates real usage)**
```bash
# Make 100 requests through the Gateway
for i in {1..100}; do
  curl -X GET http://localhost:8080/test-api/posts/1 \
    -H "X-API-Key: YOUR_API_KEY_VALUE"
  echo "Request $i completed"
  sleep 0.1
done
```

**Verification:**
```bash
# Check if key was automatically disabled
curl -X GET http://localhost:8081/internal/keys/YOUR_KEY_ID
```

Should show `"active": false` and `"disabledReason": "QUOTA_EXCEEDED"`

```bash
# Query disabled keys by reason
curl -X GET "http://localhost:8081/internal/keys?disabledReason=QUOTA_EXCEEDED"
```

Should return array containing your key ID.

### Test 7: Unlimited Quota Behavior

```bash
# Create a new plan with unlimited quota
curl -X POST http://localhost:8081/api/plans \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Unlimited Plan",
    "description": "Plan with unlimited quota",
    "monthlyQuota": -1,
    "rateLimitRpm": 60
  }'

# Create API key with unlimited plan
# ... (follow steps 3-4 with new plan ID)

# Make many requests (should never be disabled)
for i in {1..200}; do
  curl -X GET http://localhost:8080/test-api/posts/1 \
    -H "X-API-Key: YOUR_UNLIMITED_KEY"
done
```

**Verification:** Key should remain active after 200+ requests.

---

## Sprint 12: Webhook Notifications Tests

### Test 8: Setup Webhook Endpoint

**Option A: Use webhook.site**
1. Go to https://webhook.site
2. Copy your unique URL (e.g., https://webhook.site/unique-id)

**Option B: Use local webhook receiver**
```bash
# Install and run a simple webhook receiver
npm install -g webhook-receiver
webhook-receiver --port 3000
```

### Test 9: Configure Webhook

```bash
# Configure webhook for your API key
curl -X POST http://localhost:8081/api/keys/YOUR_KEY_ID/webhook \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "https://webhook.site/YOUR-UNIQUE-ID"
  }'
```

**Expected Response:**
```json
{
  "message": "Webhook configured successfully"
}
```

### Test 10: Get Webhook Configuration

```bash
# Retrieve webhook configuration
curl -X GET http://localhost:8081/api/keys/YOUR_KEY_ID/webhook \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
{
  "webhookUrl": "https://webhook.site/YOUR-UNIQUE-ID",
  "webhookSecret": "base64-encoded-secret",
  "enabled": true
}
```

**Save the webhook secret for signature verification.**

### Test 11: Test Webhook Endpoint

```bash
# Trigger a test webhook
curl -X POST http://localhost:8081/api/keys/YOUR_KEY_ID/webhook/test \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
{
  "message": "Test webhook sent",
  "deliveryStatus": "PENDING"
}
```

**Verification:** Check webhook.site - you should see a POST request with:
- Event type: `quota.test`
- Headers: `X-Webhook-Signature`, `X-Webhook-Timestamp`
- Payload with test data

### Test 12: Trigger 80% Threshold Webhook

```bash
# Create a fresh API key with quota=100 and webhook configured
# Make 80 requests to reach 80% threshold

for i in {1..80}; do
  curl -X GET http://localhost:8080/test-api/posts/1 \
    -H "X-API-Key: YOUR_API_KEY_VALUE"
  echo "Request $i completed"
  sleep 0.1
done
```

**Verification:** Check webhook.site for a POST request with:
```json
{
  "eventType": "quota.warning",
  "apiKeyId": "your-key-id",
  "currentUsage": 80,
  "quotaLimit": 100,
  "usagePercentage": 80.0,
  "timestamp": "2026-05-13T...",
  "yearMonth": "2026-05"
}
```

**Headers:**
- `X-Webhook-Signature`: HMAC-SHA256 signature
- `X-Webhook-Timestamp`: Unix timestamp
- `Content-Type`: application/json

### Test 13: Trigger 100% Threshold Webhook

```bash
# Continue from Test 12 - make 20 more requests

for i in {81..100}; do
  curl -X GET http://localhost:8080/test-api/posts/1 \
    -H "X-API-Key: YOUR_API_KEY_VALUE"
  echo "Request $i completed"
  sleep 0.1
done
```

**Verification:** Check webhook.site for a second POST request with:
```json
{
  "eventType": "quota.exceeded",
  "apiKeyId": "your-key-id",
  "currentUsage": 100,
  "quotaLimit": 100,
  "usagePercentage": 100.0,
  "timestamp": "2026-05-13T...",
  "yearMonth": "2026-05"
}
```

### Test 14: Verify Webhook Deduplication

```bash
# Make more requests in the same month
for i in {101..120}; do
  curl -X GET http://localhost:8080/test-api/posts/1 \
    -H "X-API-Key: YOUR_API_KEY_VALUE"
done
```

**Verification:** No additional webhooks should be sent (check webhook.site - should still only have 2 webhooks: 80% and 100%)

### Test 15: Verify HMAC Signature

Use this Python script to verify the webhook signature:

```python
import hmac
import hashlib
import base64

# From webhook.site, copy:
webhook_secret = "YOUR_WEBHOOK_SECRET"  # From Test 10
timestamp = "TIMESTAMP_FROM_HEADER"     # X-Webhook-Timestamp header
payload = "PAYLOAD_FROM_BODY"           # Raw JSON body
signature = "SIGNATURE_FROM_HEADER"     # X-Webhook-Signature header

# Compute expected signature
message = f"{timestamp}.{payload}"
expected_signature = hmac.new(
    base64.b64decode(webhook_secret),
    message.encode('utf-8'),
    hashlib.sha256
).hexdigest()

print(f"Expected: {expected_signature}")
print(f"Received: {signature}")
print(f"Valid: {hmac.compare_digest(expected_signature, signature)}")
```

**Expected Output:** `Valid: True`

### Test 16: Update Webhook URL

```bash
# Update webhook URL
curl -X PUT http://localhost:8081/api/keys/YOUR_KEY_ID/webhook \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "https://webhook.site/NEW-UNIQUE-ID"
  }'
```

**Expected Response:**
```json
{
  "message": "Webhook updated successfully"
}
```

**Verification:** Get webhook config again - should show new URL but same secret.

### Test 17: Webhook History

```bash
# Get webhook delivery history
curl -X GET "http://localhost:8081/api/keys/YOUR_KEY_ID/webhook/history?limit=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:** List of webhook delivery records (currently returns empty array - TODO in implementation)

### Test 18: Reject HTTP URLs

```bash
# Try to configure HTTP URL (should fail)
curl -X POST http://localhost:8081/api/keys/YOUR_KEY_ID/webhook \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "http://insecure.example.com/webhook"
  }'
```

**Expected Response:** HTTP 400 Bad Request
```json
{
  "error": "Webhook URL must use HTTPS protocol"
}
```

---

## Integration Tests

### Test 19: End-to-End Flow

1. Create API key with quota=100 and webhook configured
2. Make 80 requests → Verify 80% webhook sent
3. Make 20 more requests → Verify 100% webhook sent AND key disabled
4. Try to make another request → Verify Gateway returns 403
5. Check webhook history → Verify 2 records

### Test 20: Monthly Reset (Manual Trigger)

This requires triggering the scheduled job manually or waiting for the 1st of next month.

**Manual trigger via database:**
```sql
-- Connect to PostgreSQL
psql -h localhost -p 5435 -U apiguard -d apiguard

-- Check disabled keys
SELECT api_key_id, active, disabled_reason 
FROM api_keys 
WHERE disabled_reason = 'QUOTA_EXCEEDED';

-- Manually trigger reset (simulate scheduler)
-- This would be done by the MonthlyResetScheduler
```

---

## Troubleshooting

### Services Not Responding
```bash
# Check service logs
.\gradlew.bat :api-management-service:bootRun
.\gradlew.bat :gateway-service:bootRun
.\gradlew.bat :usage-service:bootRun
```

### Database Connection Issues
```bash
# Check PostgreSQL
docker ps | grep postgres

# Connect to database
psql -h localhost -p 5435 -U apiguard -d apiguard
```

### RabbitMQ Issues
```bash
# Check RabbitMQ
docker ps | grep rabbitmq

# Access management UI
# http://localhost:15672 (guest/guest)
```

### Redis Issues
```bash
# Check Redis
docker ps | grep redis

# Connect to Redis
docker exec -it <redis-container-id> redis-cli
```

---

## Success Criteria

### Sprint 11: Quota Enforcement ✅
- [ ] Internal API endpoints working (disable, enable, get details, query)
- [ ] Manual key disable/enable works
- [ ] Gateway rejects disabled keys with 403
- [ ] Automatic quota enforcement triggers at 100%
- [ ] Key is disabled with reason "QUOTA_EXCEEDED"
- [ ] Unlimited quota (-1) never triggers enforcement
- [ ] Cache invalidation works (Gateway sees updated status)

### Sprint 12: Webhook Notifications ✅
- [ ] Webhook configuration API works
- [ ] Webhook secret is generated
- [ ] HTTPS validation works (rejects HTTP)
- [ ] 80% threshold webhook sent
- [ ] 100% threshold webhook sent
- [ ] Webhooks include correct payload
- [ ] HMAC signature is valid
- [ ] Deduplication works (no duplicate webhooks in same month)
- [ ] Test webhook endpoint works
- [ ] Webhook update preserves secret

---

## Next Steps

1. Run all manual tests above
2. Document any failures or issues
3. Run automated unit tests: `.\gradlew.bat test`
4. Run integration tests: `.\gradlew.bat integrationTest`
5. Performance testing with load
6. Security audit
7. Deploy to staging

