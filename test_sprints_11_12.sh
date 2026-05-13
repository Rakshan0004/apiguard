#!/bin/bash

# Quick Test Script for Sprints 11 & 12
# This script automates the basic testing scenarios

set -e

echo "=========================================="
echo "Sprint 11 & 12 Testing Script"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if services are running
echo "Checking if services are running..."
if ! curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo -e "${RED}❌ API Management Service not running on port 8081${NC}"
    echo "Please start: cd api-management-service && ./gradlew bootRun"
    exit 1
fi

if ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${RED}❌ Gateway Service not running on port 8080${NC}"
    echo "Please start: cd gateway-service && ./gradlew bootRun"
    exit 1
fi

if ! curl -s http://localhost:8082/actuator/health > /dev/null 2>&1; then
    echo -e "${RED}❌ Usage Service not running on port 8082${NC}"
    echo "Please start: cd usage-service && ./gradlew bootRun"
    exit 1
fi

echo -e "${GREEN}✅ All services are running${NC}"
echo ""

# Step 1: Register Owner
echo "=========================================="
echo "Step 1: Registering test owner"
echo "=========================================="

REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-owner@example.com",
    "password": "TestPassword123!"
  }')

echo "Register response: $REGISTER_RESPONSE"
echo ""

# Step 2: Login
echo "=========================================="
echo "Step 2: Logging in"
echo "=========================================="

LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-owner@example.com",
    "password": "TestPassword123!"
  }')

JWT_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$JWT_TOKEN" ]; then
    echo -e "${RED}❌ Failed to get JWT token${NC}"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✅ Logged in successfully${NC}"
echo "JWT Token: ${JWT_TOKEN:0:20}..."
echo ""

# Step 3: Register API
echo "=========================================="
echo "Step 3: Registering test API"
echo "=========================================="

API_RESPONSE=$(curl -s -X POST http://localhost:8081/api/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "Test API for Quota",
    "baseUrl": "https://api.example.com",
    "description": "Testing quota enforcement and webhooks"
  }')

API_ID=$(echo $API_RESPONSE | grep -o '"id":"[^"]*' | cut -d'"' -f4)

if [ -z "$API_ID" ]; then
    echo -e "${RED}❌ Failed to register API${NC}"
    echo "Response: $API_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✅ API registered successfully${NC}"
echo "API ID: $API_ID"
echo ""

# Step 4: Create API Key with Low Quota
echo "=========================================="
echo "Step 4: Creating API key (quota: 5)"
echo "=========================================="

KEY_RESPONSE=$(curl -s -X POST http://localhost:8081/api/keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "registeredApiId": "'$API_ID'",
    "planName": "Test Plan",
    "monthlyQuota": 5
  }')

API_KEY=$(echo $KEY_RESPONSE | grep -o '"keyValue":"[^"]*' | cut -d'"' -f4)

if [ -z "$API_KEY" ]; then
    echo -e "${RED}❌ Failed to create API key${NC}"
    echo "Response: $KEY_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✅ API key created successfully${NC}"
echo "API Key: $API_KEY"
echo ""

# Step 5: Test Quota Enforcement
echo "=========================================="
echo "Step 5: Testing Quota Enforcement"
echo "=========================================="
echo "Making 6 API calls (quota is 5)..."
echo ""

for i in {1..6}; do
    echo -n "Call $i: "
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET http://localhost:8080/api/test \
      -H "X-API-Key: $API_KEY")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ $i -le 5 ]; then
        if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "404" ]; then
            echo -e "${GREEN}✅ Success (HTTP $HTTP_CODE)${NC}"
        else
            echo -e "${YELLOW}⚠️  Unexpected status: HTTP $HTTP_CODE${NC}"
        fi
    else
        if [ "$HTTP_CODE" = "403" ]; then
            echo -e "${GREEN}✅ Correctly blocked (HTTP 403 - Quota Exceeded)${NC}"
        else
            echo -e "${RED}❌ Expected 403, got HTTP $HTTP_CODE${NC}"
        fi
    fi
    
    sleep 1
done

echo ""

# Step 6: Verify Key Status
echo "=========================================="
echo "Step 6: Verifying key is disabled"
echo "=========================================="

KEY_STATUS=$(curl -s -X GET http://localhost:8081/api/keys \
  -H "Authorization: Bearer $JWT_TOKEN")

if echo "$KEY_STATUS" | grep -q "QUOTA_EXCEEDED"; then
    echo -e "${GREEN}✅ Key correctly disabled with reason: QUOTA_EXCEEDED${NC}"
else
    echo -e "${RED}❌ Key not disabled or wrong reason${NC}"
    echo "Response: $KEY_STATUS"
fi

echo ""

# Step 7: Test Webhooks
echo "=========================================="
echo "Step 7: Testing Webhooks"
echo "=========================================="
echo ""
echo -e "${YELLOW}⚠️  Webhook testing requires manual setup${NC}"
echo ""
echo "To test webhooks:"
echo "1. Go to https://webhook.site and copy your unique URL"
echo "2. Run this command with your webhook URL:"
echo ""
echo "   curl -X POST http://localhost:8081/api/keys/\$API_KEY/webhook \\"
echo "     -H \"Content-Type: application/json\" \\"
echo "     -H \"Authorization: Bearer $JWT_TOKEN\" \\"
echo "     -d '{\"webhookUrl\": \"https://webhook.site/YOUR-ID\"}'"
echo ""
echo "3. Create a new API key with quota 10"
echo "4. Make 8 API calls to trigger 80% webhook"
echo "5. Make 2 more calls to trigger 100% webhook"
echo "6. Check webhook.site for notifications"
echo ""

# Summary
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo ""
echo -e "${GREEN}✅ Sprint 11 (Quota Enforcement) - TESTED${NC}"
echo "   - API key disabled after quota exceeded"
echo "   - Subsequent requests blocked with 403"
echo ""
echo -e "${YELLOW}⚠️  Sprint 12 (Webhooks) - MANUAL TESTING REQUIRED${NC}"
echo "   - Follow instructions above to test webhooks"
echo "   - See SPRINTS_11_12_TESTING_GUIDE.md for detailed steps"
echo ""
echo "=========================================="
echo "Test Data for Manual Testing:"
echo "=========================================="
echo "JWT Token: $JWT_TOKEN"
echo "API ID: $API_ID"
echo "API Key: $API_KEY"
echo ""
echo "Save these values for further testing!"
echo ""
