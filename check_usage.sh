#!/bin/bash

# API Guard - Usage Tracking Query Script
# This script helps you check how much each API key is being used

echo "=========================================="
echo "   API Guard - Usage Tracking Report"
echo "=========================================="
echo ""

# Check if PostgreSQL container is running
if ! docker ps | grep -q apiguard-postgres; then
    echo "❌ Error: PostgreSQL container is not running"
    echo "   Run: docker-compose up -d"
    exit 1
fi

echo "📊 Generating usage report..."
echo ""

# Function to run SQL queries
run_query() {
    docker exec -i apiguard-postgres-1 psql -U apiguard -d apiguard -t -A -F"," -c "$1"
}

# 1. Monthly Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📅 MONTHLY USAGE SUMMARY"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

docker exec -i apiguard-postgres-1 psql -U apiguard -d apiguard << 'EOF'
SELECT 
    api_key_id as "API Key",
    year_month as "Month",
    total_requests as "Total Requests",
    successful_requests as "Successful",
    ROUND(successful_requests::numeric / NULLIF(total_requests, 0) * 100, 2) || '%' as "Success Rate"
FROM monthly_usage_summaries
ORDER BY year_month DESC, total_requests DESC;
EOF

echo ""
echo ""

# 2. Current Month Top Users
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🏆 TOP API KEYS (CURRENT MONTH)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

docker exec -i apiguard-postgres-1 psql -U apiguard -d apiguard << 'EOF'
SELECT 
    api_key_id as "API Key",
    total_requests as "Requests",
    successful_requests as "Successful",
    (total_requests - successful_requests) as "Failed"
FROM monthly_usage_summaries
WHERE year_month = TO_CHAR(CURRENT_DATE, 'YYYY-MM')
ORDER BY total_requests DESC
LIMIT 10;
EOF

echo ""
echo ""

# 3. Recent Activity
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🕐 RECENT ACTIVITY (LAST 10 REQUESTS)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

docker exec -i apiguard-postgres-1 psql -U apiguard -d apiguard << 'EOF'
SELECT 
    TO_CHAR(timestamp, 'YYYY-MM-DD HH24:MI:SS') as "Time",
    api_key_id as "API Key",
    method as "Method",
    LEFT(path, 40) as "Path",
    status as "Status",
    latency_ms || 'ms' as "Latency"
FROM usage_logs
ORDER BY timestamp DESC
LIMIT 10;
EOF

echo ""
echo ""

# 4. Statistics
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📈 OVERALL STATISTICS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

docker exec -i apiguard-postgres-1 psql -U apiguard -d apiguard << 'EOF'
SELECT 
    COUNT(DISTINCT api_key_id) as "Total API Keys",
    COUNT(*) as "Total Requests",
    COUNT(*) FILTER (WHERE status >= 200 AND status < 300) as "Successful Requests",
    COUNT(*) FILTER (WHERE status >= 400) as "Failed Requests",
    ROUND(AVG(latency_ms), 2) || 'ms' as "Avg Latency"
FROM usage_logs;
EOF

echo ""
echo ""

# 5. Daily Trend (Last 7 Days)
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 DAILY TREND (LAST 7 DAYS)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

docker exec -i apiguard-postgres-1 psql -U apiguard -d apiguard << 'EOF'
SELECT 
    DATE(timestamp) as "Date",
    COUNT(*) as "Requests",
    COUNT(*) FILTER (WHERE status >= 200 AND status < 300) as "Successful",
    ROUND(AVG(latency_ms), 2) || 'ms' as "Avg Latency"
FROM usage_logs
WHERE timestamp >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY DATE(timestamp)
ORDER BY DATE(timestamp) DESC;
EOF

echo ""
echo "=========================================="
echo "✅ Report Complete!"
echo "=========================================="
echo ""
echo "💡 Tips:"
echo "   - Run this script anytime: ./check_usage.sh"
echo "   - For specific API key: docker exec -i apiguard-postgres-1 psql -U apiguard -d apiguard"
echo "   - Then query: SELECT * FROM monthly_usage_summaries WHERE api_key_id = 'your-key-id';"
echo ""
