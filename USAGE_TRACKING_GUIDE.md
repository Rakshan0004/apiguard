# 📊 API Guard - Usage Tracking Guide

## How to Track API Key Usage

Your system **automatically tracks every request** that goes through the gateway. Here's how to access and use that data.

---

## 🎯 Quick Start

### **Option 1: Run the Automated Script (Easiest)**

**Windows:**
```bash
check_usage.bat
```

**Linux/Mac:**
```bash
./check_usage.sh
```

This will show you:
- Monthly usage summary per API key
- Top API keys this month
- Recent activity (last 10 requests)
- Overall statistics
- Daily trend (last 7 days)

---

### **Option 2: Query the Database Directly**

**Connect to PostgreSQL:**
```bash
docker exec -it apiguard-postgres-1 psql -U apiguard -d apiguard
```

Then run any of the queries below.

---

## 📊 Common Usage Queries

### **1. Check Monthly Usage for All API Keys**

```sql
SELECT 
    api_key_id,
    year_month,
    total_requests,
    successful_requests,
    ROUND(successful_requests::numeric / total_requests * 100, 2) as success_rate
FROM monthly_usage_summaries
ORDER BY year_month DESC, total_requests DESC;
```

**What you get:**
- How many requests each API key made this month
- Success rate (% of successful requests)
- Historical data by month

---

### **2. Check Usage for a Specific API Key**

```sql
SELECT 
    year_month,
    total_requests,
    successful_requests,
    (total_requests - successful_requests) as failed_requests
FROM monthly_usage_summaries
WHERE api_key_id = 'your-key-id-here'
ORDER BY year_month DESC;
```

**Use case:** Billing a specific customer

---

### **3. Get Current Month Usage**

```sql
SELECT 
    api_key_id,
    total_requests,
    successful_requests
FROM monthly_usage_summaries
WHERE year_month = TO_CHAR(CURRENT_DATE, 'YYYY-MM')
ORDER BY total_requests DESC;
```

**Use case:** See who's using the most this month

---

### **4. Check if API Key is Near Quota**

```sql
SELECT 
    m.api_key_id,
    m.total_requests,
    p.monthly_quota,
    ROUND((m.total_requests::numeric / p.monthly_quota) * 100, 2) as usage_percent,
    (p.monthly_quota - m.total_requests) as remaining_requests
FROM monthly_usage_summaries m
JOIN api_keys ak ON ak.id = m.api_key_id
JOIN plans p ON p.id = ak.plan_id
WHERE m.year_month = TO_CHAR(CURRENT_DATE, 'YYYY-MM')
ORDER BY usage_percent DESC;
```

**Use case:** See who's close to their limit

---

### **5. View Detailed Request Logs**

```sql
SELECT 
    timestamp,
    api_key_id,
    method,
    path,
    status,
    latency_ms
FROM usage_logs
WHERE api_key_id = 'your-key-id-here'
ORDER BY timestamp DESC
LIMIT 50;
```

**Use case:** Debug issues for a specific customer

---

### **6. Get Daily Usage Trend**

```sql
SELECT 
    DATE(timestamp) as date,
    COUNT(*) as total_requests,
    COUNT(*) FILTER (WHERE status >= 200 AND status < 300) as successful,
    COUNT(*) FILTER (WHERE status >= 400) as errors,
    ROUND(AVG(latency_ms), 2) as avg_latency_ms
FROM usage_logs
WHERE api_key_id = 'your-key-id-here'
    AND timestamp >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(timestamp)
ORDER BY date DESC;
```

**Use case:** Show usage trends to customers

---

### **7. Find Most Popular Endpoints**

```sql
SELECT 
    path,
    COUNT(*) as request_count,
    ROUND(AVG(latency_ms), 2) as avg_latency_ms
FROM usage_logs
WHERE timestamp >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY path
ORDER BY request_count DESC
LIMIT 10;
```

**Use case:** Optimize most-used endpoints

---

### **8. Get Status Code Distribution**

```sql
SELECT 
    CASE 
        WHEN status >= 200 AND status < 300 THEN '2xx Success'
        WHEN status >= 300 AND status < 400 THEN '3xx Redirect'
        WHEN status >= 400 AND status < 500 THEN '4xx Client Error'
        WHEN status >= 500 THEN '5xx Server Error'
        ELSE 'Unknown'
    END as status_category,
    COUNT(*) as count,
    ROUND(COUNT(*)::numeric / SUM(COUNT(*)) OVER () * 100, 2) as percentage
FROM usage_logs
WHERE timestamp >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY status_category
ORDER BY count DESC;
```

**Use case:** Monitor API health

---

### **9. Find Slowest Requests**

```sql
SELECT 
    timestamp,
    api_key_id,
    method,
    path,
    status,
    latency_ms
FROM usage_logs
WHERE timestamp >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY latency_ms DESC
LIMIT 20;
```

**Use case:** Performance optimization

---

### **10. Calculate Revenue (if you have pricing)**

```sql
SELECT 
    m.api_key_id,
    p.name as plan_name,
    m.total_requests,
    -- Example: $0.001 per request
    ROUND(m.total_requests * 0.001, 2) as estimated_revenue
FROM monthly_usage_summaries m
JOIN api_keys ak ON ak.id = m.api_key_id
JOIN plans p ON p.id = ak.plan_id
WHERE m.year_month = TO_CHAR(CURRENT_DATE, 'YYYY-MM')
ORDER BY estimated_revenue DESC;
```

**Use case:** Revenue reporting

---

## 🔄 How the Tracking Works

### **Automatic Flow:**

```
1. Request comes to Gateway
   ↓
2. Gateway processes request
   ↓
3. UsageLoggingFilter (runs AFTER response)
   ↓
4. Publishes UsageEvent to RabbitMQ
   ↓
5. Usage Service consumes event
   ↓
6. Saves to two tables:
   - usage_logs (detailed log)
   - monthly_usage_summaries (aggregated)
```

**Key Points:**
- ✅ **Automatic** - No manual tracking needed
- ✅ **Real-time** - Events processed within seconds
- ✅ **Non-blocking** - Doesn't slow down API responses
- ✅ **Reliable** - Uses RabbitMQ for guaranteed delivery
- ✅ **Accurate** - Atomic database operations prevent race conditions

---

## 📊 Database Tables

### **1. `usage_logs` - Detailed Request Logs**

Stores every single request with full details.

**Columns:**
- `id` - Unique event ID (UUID)
- `api_key_id` - Which API key made the request
- `api_id` - Which registered API was called
- `method` - HTTP method (GET, POST, etc.)
- `path` - Request path
- `status` - HTTP status code (200, 404, etc.)
- `latency_ms` - Response time in milliseconds
- `timestamp` - When the request happened

**Use for:**
- Debugging specific issues
- Detailed analytics
- Audit trails

---

### **2. `monthly_usage_summaries` - Aggregated Monthly Stats**

Automatically aggregated counts per API key per month.

**Columns:**
- `id` - Auto-increment ID
- `api_key_id` - Which API key
- `year_month` - Month (format: "2026-05")
- `total_requests` - Total number of requests
- `successful_requests` - Requests with 2xx status

**Use for:**
- Billing
- Quota enforcement
- Monthly reports

**How it updates:**
- Uses PostgreSQL `ON CONFLICT DO UPDATE` (upsert)
- Atomic operation - no race conditions
- Automatically creates new row for new months

---

## 🎯 Real-World Examples

### **Example 1: Monthly Billing Report**

```sql
-- Generate invoice data for May 2026
SELECT 
    ak.id as api_key_id,
    o.email as customer_email,
    p.name as plan_name,
    m.total_requests,
    m.successful_requests,
    p.monthly_quota,
    CASE 
        WHEN m.total_requests > p.monthly_quota 
        THEN m.total_requests - p.monthly_quota 
        ELSE 0 
    END as overage_requests
FROM monthly_usage_summaries m
JOIN api_keys ak ON ak.id = m.api_key_id
JOIN plans p ON p.id = ak.plan_id
JOIN registered_apis ra ON ra.id = p.api_id
JOIN owners o ON o.email = ra.owner_email
WHERE m.year_month = '2026-05'
ORDER BY o.email, m.total_requests DESC;
```

---

### **Example 2: Send Warning Email at 80% Quota**

```sql
-- Find API keys that have used 80%+ of their quota
SELECT 
    ak.id as api_key_id,
    o.email as owner_email,
    m.total_requests,
    p.monthly_quota,
    ROUND((m.total_requests::numeric / p.monthly_quota) * 100, 2) as usage_percent
FROM monthly_usage_summaries m
JOIN api_keys ak ON ak.id = m.api_key_id
JOIN plans p ON p.id = ak.plan_id
JOIN registered_apis ra ON ra.id = p.api_id
JOIN owners o ON o.email = ra.owner_email
WHERE m.year_month = TO_CHAR(CURRENT_DATE, 'YYYY-MM')
    AND m.total_requests >= (p.monthly_quota * 0.8)
    AND m.total_requests < p.monthly_quota;
```

---

### **Example 3: Performance Report**

```sql
-- Average latency per API key (last 7 days)
SELECT 
    api_key_id,
    COUNT(*) as total_requests,
    ROUND(AVG(latency_ms), 2) as avg_latency_ms,
    ROUND(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY latency_ms), 2) as p50_latency,
    ROUND(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY latency_ms), 2) as p95_latency,
    ROUND(PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY latency_ms), 2) as p99_latency
FROM usage_logs
WHERE timestamp >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY api_key_id
ORDER BY avg_latency_ms DESC;
```

---

## 🔧 Maintenance Queries

### **Clean Up Old Logs (Optional)**

```sql
-- Delete detailed logs older than 90 days
-- (Keep monthly summaries forever)
DELETE FROM usage_logs
WHERE timestamp < CURRENT_DATE - INTERVAL '90 days';
```

### **Recalculate Monthly Summary (if needed)**

```sql
-- Manually recalculate for a specific month
INSERT INTO monthly_usage_summaries (api_key_id, year_month, total_requests, successful_requests)
SELECT 
    api_key_id,
    TO_CHAR(timestamp, 'YYYY-MM') as year_month,
    COUNT(*) as total_requests,
    COUNT(*) FILTER (WHERE status >= 200 AND status < 300) as successful_requests
FROM usage_logs
WHERE TO_CHAR(timestamp, 'YYYY-MM') = '2026-05'
GROUP BY api_key_id, TO_CHAR(timestamp, 'YYYY-MM')
ON CONFLICT (api_key_id, year_month) DO UPDATE SET
    total_requests = EXCLUDED.total_requests,
    successful_requests = EXCLUDED.successful_requests;
```

---

## 📈 Export Data

### **Export to CSV**

```bash
# Export monthly summary to CSV
docker exec -i apiguard-postgres-1 psql -U apiguard -d apiguard -c "\COPY (SELECT * FROM monthly_usage_summaries ORDER BY year_month DESC) TO STDOUT WITH CSV HEADER" > usage_report.csv
```

### **Export to JSON**

```bash
# Export recent logs to JSON
docker exec -i apiguard-postgres-1 psql -U apiguard -d apiguard -t -c "SELECT json_agg(row_to_json(t)) FROM (SELECT * FROM usage_logs ORDER BY timestamp DESC LIMIT 100) t" > usage_logs.json
```

---

## 🎯 Next Steps (Sprint 11)

In the next sprint, we'll add:
- **Automatic quota enforcement** - Disable keys at 100% usage
- **Quota checking API** - Check remaining quota programmatically
- **Usage alerts** - Automatic notifications at 80% and 100%

But for now, you can already:
- ✅ Track all usage in real-time
- ✅ Generate billing reports
- ✅ Monitor API health
- ✅ Analyze performance
- ✅ Debug customer issues

---

## 💡 Pro Tips

1. **Index Performance**: The tables already have indexes on `api_key_id` and `timestamp` for fast queries

2. **Monthly Partitioning**: Consider partitioning `usage_logs` by month if you expect millions of requests

3. **Caching**: For frequently accessed data (like current month totals), consider caching in Redis

4. **Scheduled Reports**: Set up a cron job to run `check_usage.bat` daily and email results

5. **Monitoring**: Set up alerts when usage spikes unexpectedly

---

## 🆘 Troubleshooting

### **No data showing up?**

1. Check if services are running:
   ```bash
   docker-compose ps
   ```

2. Check if RabbitMQ has messages:
   - Open http://localhost:15672
   - Login: apiguard / secret
   - Check "Queues" tab

3. Check Usage Service logs:
   ```bash
   docker logs -f apiguard-usage-service-1
   ```

4. Make a test request through the gateway:
   ```bash
   curl -H "X-Api-Key: your-key" http://localhost:8080/proxy/your-api/test
   ```

5. Wait a few seconds, then check the database

---

## ✅ Summary

**You can track usage by:**
1. Running `check_usage.bat` (easiest)
2. Querying `monthly_usage_summaries` table (for billing)
3. Querying `usage_logs` table (for detailed analysis)

**The system automatically:**
- ✅ Tracks every request
- ✅ Aggregates monthly totals
- ✅ Stores detailed logs
- ✅ Updates in real-time

**You're ready to:**
- Generate billing reports
- Monitor API health
- Analyze usage patterns
- Debug customer issues

🎉 **Your usage tracking is fully operational!**
