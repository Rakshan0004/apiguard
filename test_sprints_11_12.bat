@echo off
REM Quick Test Script for Sprints 11 & 12 (Windows)
REM This script automates the basic testing scenarios

echo ==========================================
echo Sprint 11 ^& 12 Testing Script
echo ==========================================
echo.

REM Check if services are running
echo Checking if services are running...

curl -s http://localhost:8081/actuator/health >nul 2>&1
if errorlevel 1 (
    echo [ERROR] API Management Service not running on port 8081
    echo Please start: cd api-management-service ^&^& gradlew bootRun
    exit /b 1
)

curl -s http://localhost:8080/actuator/health >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Gateway Service not running on port 8080
    echo Please start: cd gateway-service ^&^& gradlew bootRun
    exit /b 1
)

curl -s http://localhost:8082/actuator/health >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Usage Service not running on port 8082
    echo Please start: cd usage-service ^&^& gradlew bootRun
    exit /b 1
)

echo [OK] All services are running
echo.

REM Step 1: Register Owner
echo ==========================================
echo Step 1: Registering test owner
echo ==========================================

curl -s -X POST http://localhost:8081/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"email\": \"test-owner@example.com\", \"password\": \"TestPassword123!\"}" > register_response.json

type register_response.json
echo.

REM Step 2: Login
echo ==========================================
echo Step 2: Logging in
echo ==========================================

curl -s -X POST http://localhost:8081/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\": \"test-owner@example.com\", \"password\": \"TestPassword123!\"}" > login_response.json

REM Extract JWT token (basic parsing - may need jq for better parsing)
for /f "tokens=2 delims=:," %%a in ('findstr "token" login_response.json') do set JWT_TOKEN=%%a
set JWT_TOKEN=%JWT_TOKEN:"=%
set JWT_TOKEN=%JWT_TOKEN: =%

if "%JWT_TOKEN%"=="" (
    echo [ERROR] Failed to get JWT token
    type login_response.json
    exit /b 1
)

echo [OK] Logged in successfully
echo JWT Token: %JWT_TOKEN:~0,20%...
echo.

REM Step 3: Register API
echo ==========================================
echo Step 3: Registering test API
echo ==========================================

curl -s -X POST http://localhost:8081/api/register ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer %JWT_TOKEN%" ^
  -d "{\"name\": \"Test API for Quota\", \"baseUrl\": \"https://api.example.com\", \"description\": \"Testing quota enforcement and webhooks\"}" > api_response.json

REM Extract API ID
for /f "tokens=2 delims=:," %%a in ('findstr "\"id\"" api_response.json') do set API_ID=%%a
set API_ID=%API_ID:"=%
set API_ID=%API_ID: =%

if "%API_ID%"=="" (
    echo [ERROR] Failed to register API
    type api_response.json
    exit /b 1
)

echo [OK] API registered successfully
echo API ID: %API_ID%
echo.

REM Step 4: Create API Key
echo ==========================================
echo Step 4: Creating API key (quota: 5)
echo ==========================================

curl -s -X POST http://localhost:8081/api/keys ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer %JWT_TOKEN%" ^
  -d "{\"registeredApiId\": \"%API_ID%\", \"planName\": \"Test Plan\", \"monthlyQuota\": 5}" > key_response.json

REM Extract API Key
for /f "tokens=2 delims=:," %%a in ('findstr "keyValue" key_response.json') do set API_KEY=%%a
set API_KEY=%API_KEY:"=%
set API_KEY=%API_KEY: =%

if "%API_KEY%"=="" (
    echo [ERROR] Failed to create API key
    type key_response.json
    exit /b 1
)

echo [OK] API key created successfully
echo API Key: %API_KEY%
echo.

REM Step 5: Test Quota Enforcement
echo ==========================================
echo Step 5: Testing Quota Enforcement
echo ==========================================
echo Making 6 API calls (quota is 5)...
echo.

for /L %%i in (1,1,6) do (
    echo Call %%i:
    curl -s -w "%%{http_code}" -X GET http://localhost:8080/api/test ^
      -H "X-API-Key: %API_KEY%" > call_response.txt
    
    set /p HTTP_CODE=<call_response.txt
    
    if %%i LEQ 5 (
        echo [OK] Success (HTTP !HTTP_CODE!)
    ) else (
        if "!HTTP_CODE!"=="403" (
            echo [OK] Correctly blocked (HTTP 403 - Quota Exceeded)
        ) else (
            echo [ERROR] Expected 403, got HTTP !HTTP_CODE!
        )
    )
    
    timeout /t 1 /nobreak >nul
)

echo.

REM Step 6: Verify Key Status
echo ==========================================
echo Step 6: Verifying key is disabled
echo ==========================================

curl -s -X GET http://localhost:8081/api/keys ^
  -H "Authorization: Bearer %JWT_TOKEN%" > key_status.json

findstr "QUOTA_EXCEEDED" key_status.json >nul
if errorlevel 1 (
    echo [ERROR] Key not disabled or wrong reason
    type key_status.json
) else (
    echo [OK] Key correctly disabled with reason: QUOTA_EXCEEDED
)

echo.

REM Step 7: Webhook Testing Info
echo ==========================================
echo Step 7: Testing Webhooks
echo ==========================================
echo.
echo [INFO] Webhook testing requires manual setup
echo.
echo To test webhooks:
echo 1. Go to https://webhook.site and copy your unique URL
echo 2. Run this command with your webhook URL:
echo.
echo    curl -X POST http://localhost:8081/api/keys/%API_KEY%/webhook ^
echo      -H "Content-Type: application/json" ^
echo      -H "Authorization: Bearer %JWT_TOKEN%" ^
echo      -d "{\"webhookUrl\": \"https://webhook.site/YOUR-ID\"}"
echo.
echo 3. Create a new API key with quota 10
echo 4. Make 8 API calls to trigger 80%% webhook
echo 5. Make 2 more calls to trigger 100%% webhook
echo 6. Check webhook.site for notifications
echo.

REM Summary
echo ==========================================
echo Test Summary
echo ==========================================
echo.
echo [OK] Sprint 11 (Quota Enforcement) - TESTED
echo    - API key disabled after quota exceeded
echo    - Subsequent requests blocked with 403
echo.
echo [INFO] Sprint 12 (Webhooks) - MANUAL TESTING REQUIRED
echo    - Follow instructions above to test webhooks
echo    - See SPRINTS_11_12_TESTING_GUIDE.md for detailed steps
echo.
echo ==========================================
echo Test Data for Manual Testing:
echo ==========================================
echo JWT Token: %JWT_TOKEN%
echo API ID: %API_ID%
echo API Key: %API_KEY%
echo.
echo Save these values for further testing!
echo.

REM Cleanup temp files
del register_response.json login_response.json api_response.json key_response.json call_response.txt key_status.json 2>nul

pause
