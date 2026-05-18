# PowerShell Test Script for Sprints 11 & 12
# Tests Quota Enforcement and Webhook Notifications

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Sprint 11 & 12 Testing Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$API_MGMT_URL = "http://localhost:8081"
$GATEWAY_URL = "http://localhost:8080"
$USAGE_URL = "http://localhost:8082"

# Test Results
$testResults = @()

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Uri,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [object]$Body = $null,
        [int]$ExpectedStatus = 200
    )
    
    Write-Host "Testing: $Name" -ForegroundColor Yellow
    Write-Host "  URI: $Method $Uri" -ForegroundColor Gray
    
    try {
        $params = @{
            Uri = $Uri
            Method = $Method
            Headers = $Headers
            UseBasicParsing = $true
            ErrorAction = "Stop"
        }
        
        if ($Body) {
            $params.Body = ($Body | ConvertTo-Json -Depth 10)
            $params.ContentType = "application/json"
        }
        
        $response = Invoke-WebRequest @params
        
        if ($response.StatusCode -eq $ExpectedStatus) {
            Write-Host "  [PASS] - Status: $($response.StatusCode)" -ForegroundColor Green
            $script:testResults += @{Name=$Name; Status="PASS"; Details="Status: $($response.StatusCode)"}
            return @{Success=$true; Response=$response; Content=($response.Content | ConvertFrom-Json -ErrorAction SilentlyContinue)}
        } else {
            Write-Host "  [FAIL] - Expected: $ExpectedStatus, Got: $($response.StatusCode)" -ForegroundColor Red
            $script:testResults += @{Name=$Name; Status="FAIL"; Details="Expected: $ExpectedStatus, Got: $($response.StatusCode)"}
            return @{Success=$false; Response=$response}
        }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq $ExpectedStatus) {
            Write-Host "  [PASS] - Status: $statusCode (Expected error)" -ForegroundColor Green
            $script:testResults += @{Name=$Name; Status="PASS"; Details="Status: $statusCode (Expected)"}
            return @{Success=$true; StatusCode=$statusCode}
        } else {
            Write-Host "  [FAIL] - Error: $($_.Exception.Message)" -ForegroundColor Red
            $script:testResults += @{Name=$Name; Status="FAIL"; Details=$_.Exception.Message}
            return @{Success=$false; Error=$_.Exception.Message}
        }
    }
    Write-Host ""
}

# ========================================
# Phase 1: Service Health Checks
# ========================================
Write-Host "`n=== Phase 1: Service Health Checks ===" -ForegroundColor Cyan

Test-Endpoint -Name "API Management Service Health" -Uri "$API_MGMT_URL/actuator/health"
Test-Endpoint -Name "Gateway Service Health" -Uri "$GATEWAY_URL/actuator/health"
Test-Endpoint -Name "Usage Service Health" -Uri "$USAGE_URL/actuator/health"

# ========================================
# Phase 2: Setup Test Data
# ========================================
Write-Host "`n=== Phase 2: Setup Test Data ===" -ForegroundColor Cyan

# Register test owner
Write-Host "`nRegistering test owner..." -ForegroundColor Yellow
$testEmail = "test-$(Get-Random)@example.com"
$testPassword = "Test123!@#"

$registerResult = Test-Endpoint `
    -Name "Register Test Owner" `
    -Uri "$API_MGMT_URL/api/v1/auth/register" `
    -Method "POST" `
    -Body @{
        email = $testEmail
        password = $testPassword
    } `
    -ExpectedStatus 200

if (-not $registerResult.Success) {
    Write-Host "Failed to register owner. Trying to login with existing account..." -ForegroundColor Yellow
    $testEmail = "test@example.com"
    $testPassword = "Test123!@#"
}

# Login to get JWT token
Write-Host "`nLogging in..." -ForegroundColor Yellow
$loginResult = Test-Endpoint `
    -Name "Login Test Owner" `
    -Uri "$API_MGMT_URL/api/v1/auth/login" `
    -Method "POST" `
    -Body @{
        email = $testEmail
        password = $testPassword
    } `
    -ExpectedStatus 200

if (-not $loginResult.Success) {
    Write-Host "Failed to login. Cannot continue with authenticated tests." -ForegroundColor Red
    exit 1
}

$JWT_TOKEN = $loginResult.Content.token
Write-Host "  JWT Token obtained: $($JWT_TOKEN.Substring(0, 20))..." -ForegroundColor Green

# Register test API
Write-Host "`nRegistering test API..." -ForegroundColor Yellow
$randomId = Get-Random
$apiResult = Test-Endpoint `
    -Name "Register Test API" `
    -Uri "$API_MGMT_URL/api/v1/apis" `
    -Method "POST" `
    -Headers @{Authorization = "Bearer $JWT_TOKEN"} `
    -Body @{
        name = "Test API $randomId"
        targetUrl = "https://jsonplaceholder.typicode.com"
        proxyPath = "test-api-$randomId"
    } `
    -ExpectedStatus 200

if (-not $apiResult.Success) {
    Write-Host "Failed to register API. Cannot continue." -ForegroundColor Red
    exit 1
}

$API_ID = $apiResult.Content.id
Write-Host "  API ID: $API_ID" -ForegroundColor Green

# Create test plan with quota
Write-Host "`nCreating test plan..." -ForegroundColor Yellow
$planResult = Test-Endpoint `
    -Name "Create Test Plan" `
    -Uri "$API_MGMT_URL/api/v1/apis/$API_ID/plans" `
    -Method "POST" `
    -Headers @{Authorization = "Bearer $JWT_TOKEN"} `
    -Body @{
        name = "Test Plan $(Get-Random)"
        description = "Plan for testing quota enforcement"
        monthlyQuota = 100
        rateLimitRpm = 60
    } `
    -ExpectedStatus 200

if (-not $planResult.Success) {
    Write-Host "Failed to create plan. Cannot continue." -ForegroundColor Red
    exit 1
}

$PLAN_ID = $planResult.Content.id
Write-Host "  Plan ID: $PLAN_ID" -ForegroundColor Green

# Create API key
Write-Host "`nCreating API key..." -ForegroundColor Yellow
$keyResult = Test-Endpoint `
    -Name "Create API Key" `
    -Uri "$API_MGMT_URL/api/v1/keys/generate?apiId=$API_ID&planId=$PLAN_ID" `
    -Method "POST" `
    -Headers @{Authorization = "Bearer $JWT_TOKEN"} `
    -ExpectedStatus 200

if (-not $keyResult.Success) {
    Write-Host "Failed to create API key. Cannot continue." -ForegroundColor Red
    exit 1
}

$API_KEY_VALUE = $keyResult.Content.apiKey
Write-Host "  API Key Value: $($API_KEY_VALUE.Substring(0, 20))..." -ForegroundColor Green

# List keys to get the key ID
Write-Host "`nListing API keys to get key ID..." -ForegroundColor Yellow
$listKeysResult = Test-Endpoint `
    -Name "List API Keys" `
    -Uri "$API_MGMT_URL/api/v1/keys" `
    -Method "GET" `
    -Headers @{Authorization = "Bearer $JWT_TOKEN"} `
    -ExpectedStatus 200

if ($listKeysResult.Success -and $listKeysResult.Content.Count -gt 0) {
    $API_KEY_ID = $listKeysResult.Content[0].id
    Write-Host "  API Key ID: $API_KEY_ID" -ForegroundColor Green
} else {
    Write-Host "Failed to get API key ID. Cannot continue." -ForegroundColor Red
    exit 1
}

# ========================================
# Phase 3: Sprint 11 - Quota Enforcement Tests
# ========================================
Write-Host "`n=== Phase 3: Sprint 11 - Quota Enforcement Tests ===" -ForegroundColor Cyan

# Test 1: Get API Key Details (Internal API)
Write-Host "`nTest 1: Get API Key Details via Internal API" -ForegroundColor Yellow
$detailsResult = Test-Endpoint `
    -Name "Get API Key Details" `
    -Uri "$API_MGMT_URL/internal/keys/$API_KEY_ID" `
    -Method "GET" `
    -ExpectedStatus 200

if ($detailsResult.Success) {
    Write-Host "  Key Details:" -ForegroundColor Gray
    Write-Host "    Active: $($detailsResult.Content.active)" -ForegroundColor Gray
    Write-Host "    Monthly Quota: $($detailsResult.Content.monthlyQuota)" -ForegroundColor Gray
    Write-Host "    Disabled Reason: $($detailsResult.Content.disabledReason)" -ForegroundColor Gray
}

# Test 2: Manual Key Disable
Write-Host "`nTest 2: Manual Key Disable" -ForegroundColor Yellow
$disableResult = Test-Endpoint `
    -Name "Disable API Key" `
    -Uri "$API_MGMT_URL/internal/keys/$API_KEY_ID/disable" `
    -Method "POST" `
    -Body @{reason = "MANUAL_TEST"} `
    -ExpectedStatus 200

# Test 3: Verify Key is Disabled
Write-Host "`nTest 3: Verify Key is Disabled" -ForegroundColor Yellow
$verifyDisabledResult = Test-Endpoint `
    -Name "Verify Key Disabled" `
    -Uri "$API_MGMT_URL/internal/keys/$API_KEY_ID" `
    -Method "GET" `
    -ExpectedStatus 200

if ($verifyDisabledResult.Success) {
    $isDisabled = -not $verifyDisabledResult.Content.active
    $hasReason = $verifyDisabledResult.Content.disabledReason -eq "MANUAL_TEST"
    
    if ($isDisabled -and $hasReason) {
        Write-Host "  [PASS] Key is correctly disabled with reason: MANUAL_TEST" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Key state incorrect - Active: $($verifyDisabledResult.Content.active), Reason: $($verifyDisabledResult.Content.disabledReason)" -ForegroundColor Red
    }
}

# Test 4: Gateway Rejects Disabled Key
Write-Host "`nTest 4: Gateway Rejects Disabled Key" -ForegroundColor Yellow
Start-Sleep -Seconds 2  # Wait for cache invalidation
Test-Endpoint `
    -Name "Gateway Rejects Disabled Key" `
    -Uri "$GATEWAY_URL/test-api/posts/1" `
    -Method "GET" `
    -Headers @{"X-API-Key" = $API_KEY_VALUE} `
    -ExpectedStatus 403

# Test 5: Manual Key Enable
Write-Host "`nTest 5: Manual Key Enable" -ForegroundColor Yellow
$enableResult = Test-Endpoint `
    -Name "Enable API Key" `
    -Uri "$API_MGMT_URL/internal/keys/$API_KEY_ID/enable" `
    -Method "POST" `
    -ExpectedStatus 200

# Test 6: Verify Key is Enabled
Write-Host "`nTest 6: Verify Key is Enabled" -ForegroundColor Yellow
$verifyEnabledResult = Test-Endpoint `
    -Name "Verify Key Enabled" `
    -Uri "$API_MGMT_URL/internal/keys/$API_KEY_ID" `
    -Method "GET" `
    -ExpectedStatus 200

if ($verifyEnabledResult.Success) {
    $isEnabled = $verifyEnabledResult.Content.active
    $noReason = $null -eq $verifyEnabledResult.Content.disabledReason -or $verifyEnabledResult.Content.disabledReason -eq ""
    
    if ($isEnabled -and $noReason) {
        Write-Host "  [PASS] Key is correctly enabled" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Key state incorrect - Active: $($verifyEnabledResult.Content.active), Reason: $($verifyEnabledResult.Content.disabledReason)" -ForegroundColor Red
    }
}

# Test 7: Gateway Accepts Enabled Key
Write-Host "`nTest 7: Gateway Accepts Enabled Key" -ForegroundColor Yellow
Start-Sleep -Seconds 2  # Wait for cache invalidation
Test-Endpoint `
    -Name "Gateway Accepts Enabled Key" `
    -Uri "$GATEWAY_URL/test-api/posts/1" `
    -Method "GET" `
    -Headers @{"X-API-Key" = $API_KEY_VALUE} `
    -ExpectedStatus 200

# Test 8: Query Disabled Keys by Reason
Write-Host "`nTest 8: Query Disabled Keys by Reason" -ForegroundColor Yellow
Test-Endpoint `
    -Name "Query Keys by Disabled Reason" `
    -Uri "$API_MGMT_URL/internal/keys?disabledReason=QUOTA_EXCEEDED" `
    -Method "GET" `
    -ExpectedStatus 200

# ========================================
# Phase 4: Sprint 12 - Webhook Notifications Tests
# ========================================
Write-Host "`n=== Phase 4: Sprint 12 - Webhook Notifications Tests ===" -ForegroundColor Cyan

# Test 9: Configure Webhook
Write-Host "`nTest 9: Configure Webhook" -ForegroundColor Yellow
$webhookUrl = "https://webhook.site/test-$(Get-Random)"
$configWebhookResult = Test-Endpoint `
    -Name "Configure Webhook" `
    -Uri "$API_MGMT_URL/api/keys/$API_KEY_ID/webhook" `
    -Method "POST" `
    -Headers @{Authorization = "Bearer $JWT_TOKEN"} `
    -Body @{webhookUrl = $webhookUrl} `
    -ExpectedStatus 200

# Test 10: Get Webhook Configuration
Write-Host "`nTest 10: Get Webhook Configuration" -ForegroundColor Yellow
$getWebhookResult = Test-Endpoint `
    -Name "Get Webhook Config" `
    -Uri "$API_MGMT_URL/api/keys/$API_KEY_ID/webhook" `
    -Method "GET" `
    -Headers @{Authorization = "Bearer $JWT_TOKEN"} `
    -ExpectedStatus 200

if ($getWebhookResult.Success) {
    Write-Host "  Webhook Details:" -ForegroundColor Gray
    Write-Host "    URL: $($getWebhookResult.Content.webhookUrl)" -ForegroundColor Gray
    Write-Host "    Secret: $($getWebhookResult.Content.webhookSecret.Substring(0, 20))..." -ForegroundColor Gray
    Write-Host "    Enabled: $($getWebhookResult.Content.enabled)" -ForegroundColor Gray
}

# Test 11: Update Webhook URL
Write-Host "`nTest 11: Update Webhook URL" -ForegroundColor Yellow
$newWebhookUrl = "https://webhook.site/updated-$(Get-Random)"
$updateWebhookResult = Test-Endpoint `
    -Name "Update Webhook" `
    -Uri "$API_MGMT_URL/api/keys/$API_KEY_ID/webhook" `
    -Method "PUT" `
    -Headers @{Authorization = "Bearer $JWT_TOKEN"} `
    -Body @{webhookUrl = $newWebhookUrl} `
    -ExpectedStatus 200

# Test 12: Test Webhook Endpoint
Write-Host "`nTest 12: Test Webhook Endpoint" -ForegroundColor Yellow
$testWebhookResult = Test-Endpoint `
    -Name "Test Webhook" `
    -Uri "$API_MGMT_URL/api/keys/$API_KEY_ID/webhook/test" `
    -Method "POST" `
    -Headers @{Authorization = "Bearer $JWT_TOKEN"} `
    -ExpectedStatus 200

# Test 13: Reject HTTP URLs
Write-Host "`nTest 13: Reject HTTP URLs" -ForegroundColor Yellow
Test-Endpoint `
    -Name "Reject HTTP URL" `
    -Uri "$API_MGMT_URL/api/keys/$API_KEY_ID/webhook" `
    -Method "POST" `
    -Headers @{Authorization = "Bearer $JWT_TOKEN"} `
    -Body @{webhookUrl = "http://insecure.example.com/webhook"} `
    -ExpectedStatus 400

# Test 14: Get Webhook History
Write-Host "`nTest 14: Get Webhook History" -ForegroundColor Yellow
Test-Endpoint `
    -Name "Get Webhook History" `
    -Uri "$API_MGMT_URL/api/keys/$API_KEY_ID/webhook/history?limit=10" `
    -Method "GET" `
    -Headers @{Authorization = "Bearer $JWT_TOKEN"} `
    -ExpectedStatus 200

# ========================================
# Test Summary
# ========================================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$passCount = ($testResults | Where-Object {$_.Status -eq "PASS"}).Count
$failCount = ($testResults | Where-Object {$_.Status -eq "FAIL"}).Count
$totalCount = $testResults.Count

Write-Host "`nTotal Tests: $totalCount" -ForegroundColor White
Write-Host "Passed: $passCount" -ForegroundColor Green
Write-Host "Failed: $failCount" -ForegroundColor Red
Write-Host "Success Rate: $([math]::Round(($passCount / $totalCount) * 100, 2))%" -ForegroundColor $(if ($failCount -eq 0) {"Green"} else {"Yellow"})

Write-Host "`nDetailed Results:" -ForegroundColor White
foreach ($result in $testResults) {
    $color = if ($result.Status -eq "PASS") {"Green"} else {"Red"}
    $symbol = if ($result.Status -eq "PASS") {"[PASS]"} else {"[FAIL]"}
    Write-Host "  $symbol $($result.Name) - $($result.Details)" -ForegroundColor $color
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Testing Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Save test results to file
$testResults | ConvertTo-Json -Depth 10 | Out-File "test-results-$(Get-Date -Format 'yyyyMMdd-HHmmss').json"
Write-Host "`nTest results saved to test-results-$(Get-Date -Format 'yyyyMMdd-HHmmss').json" -ForegroundColor Gray
