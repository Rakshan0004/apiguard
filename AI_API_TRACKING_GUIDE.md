# 🤖 AI API Usage Tracking - Implementation Guide

## The Problem You Identified

**Current system:**
- 1 API call = 1 request counted
- Works for regular APIs (weather, payments, etc.)
- **Doesn't work for AI APIs** where usage varies by complexity

**AI APIs need:**
- Track **tokens** (input + output)
- Track **model used** (GPT-4 vs GPT-3.5)
- Track **processing time**
- Calculate **cost** based on actual usage

---

## 🎯 Solution: Multi-Metric Usage Tracking

### **Architecture Change Needed:**

Instead of just counting requests, track **multiple usage metrics**:

```
Current:  1 request = 1 count
New:      1 request = {
            requests: 1,
            tokens: 7000,
            compute_units: 150,
            data_processed_mb: 2.5,
            cost: $0.21
          }
```

---

## 🔧 Implementation Steps

### **Step 1: Extend UsageEvent (Add Token Tracking)**

**New file:** `common/src/main/java/com/apiguard/common/event/UsageEvent.java`

```java
package com.apiguard.common.event;

import lombok.Builder;
import java.time.Instant;

@Builder
public record UsageEvent(
    // Existing fields
    String eventId,
    String apiKeyId,
    String registeredApiId,
    String method,
    String path,
    int responseStatus,
    long latencyMs,
    Instant timestamp,
    
    // NEW: AI/Token-based tracking
    Long inputTokens,        // Tokens in request
    Long outputTokens,       // Tokens in response
    Long totalTokens,        // inputTokens + outputTokens
    String modelUsed,        // "gpt-4", "gpt-3.5-turbo", etc.
    Double computeUnits,     // For custom compute tracking
    Long dataSizeBytes,      // Request + response size
    Double estimatedCost     // Calculated cost
) {}
```

---

### **Step 2: Extract Token Info from Response**

**Problem:** How do you know how many tokens were used?

**Solution:** Parse the response from the AI API.

#### **Example: OpenAI Response**

```json
{
  "id": "chatcmpl-123",
  "choices": [{
    "message": {
      "content": "Hello! How can I help you?"
    }
  }],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 20,
    "total_tokens": 30
  }
}
```

**You need to extract:** `usage.prompt_tokens`, `usage.completion_tokens`, `usage.total_tokens`

---

### **Step 3: Modify UsageLoggingFilter**

**File:** `gateway-service/src/main/java/com/apiguard/gateway/filter/UsageLoggingFilter.java`

```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    long startTime = System.currentTimeMillis();

    return chain.filter(exchange).then(Mono.fromRunnable(() -> {
        ApiConfigDTO config = exchange.getAttribute(ApiKeyAuthFilter.ATTR_API_CONFIG);
        long latency = System.currentTimeMillis() - startTime;
        int statusCode = exchange.getResponse().getStatusCode().value();

        // NEW: Extract token usage from response
        TokenUsage tokenUsage = extractTokenUsage(exchange);

        UsageEvent event = UsageEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .apiKeyId(config.apiKeyId())
            .registeredApiId(config.apiId())
            .method(exchange.getRequest().getMethod().name())
            .path(exchange.getRequest().getPath().value())
            .responseStatus(statusCode)
            .latencyMs(latency)
            .timestamp(Instant.now())
            // NEW: Token tracking
            .inputTokens(tokenUsage.inputTokens())
            .outputTokens(tokenUsage.outputTokens())
            .totalTokens(tokenUsage.totalTokens())
            .modelUsed(tokenUsage.model())
            .estimatedCost(calculateCost(tokenUsage))
            .build();

        usageEventPublisher.publishEvent(event);
    }));
}

// NEW: Extract token usage from response body
private TokenUsage extractTokenUsage(ServerWebExchange exchange) {
    // This is tricky - you need to read the response body
    // For AI APIs, parse the JSON response
    
    // Example for OpenAI:
    // Parse response JSON and extract "usage" field
    
    // For now, return default values
    return new TokenUsage(0L, 0L, 0L, "unknown");
}

private Double calculateCost(TokenUsage usage) {
    // Example pricing (OpenAI GPT-4)
    double inputCostPer1K = 0.03;   // $0.03 per 1K input tokens
    double outputCostPer1K = 0.06;  // $0.06 per 1K output tokens
    
    double inputCost = (usage.inputTokens() / 1000.0) * inputCostPer1K;
    double outputCost = (usage.outputTokens() / 1000.0) * outputCostPer1K;
    
    return inputCost + outputCost;
}

record TokenUsage(Long inputTokens, Long outputTokens, Long totalTokens, String model) {}
```

---

### **Step 4: Update Database Schema**

**New migration:** `usage-service/src/main/resources/db/migration/V3__add_token_tracking.sql`

```sql
-- Add token tracking columns to usage_logs
ALTER TABLE usage_logs
ADD COLUMN input_tokens BIGINT,
ADD COLUMN output_tokens BIGINT,
ADD COLUMN total_tokens BIGINT,
ADD COLUMN model_used VARCHAR(100),
ADD COLUMN compute_units DOUBLE PRECISION,
ADD COLUMN data_size_bytes BIGINT,
ADD COLUMN estimated_cost DOUBLE PRECISION;

-- Add token tracking to monthly summaries
ALTER TABLE monthly_usage_summaries
ADD COLUMN total_tokens BIGINT DEFAULT 0,
ADD COLUMN total_cost DOUBLE PRECISION DEFAULT 0.0;

-- Create index for token-based queries
CREATE INDEX idx_usage_logs_tokens ON usage_logs(total_tokens);
```

---

### **Step 5: Update MonthlyUsageSummary Entity**

```java
@Entity
@Table(name = "monthly_usage_summaries")
public class MonthlyUsageSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key_id", nullable = false)
    private String apiKeyId;

    @Column(name = "year_month", nullable = false)
    private String yearMonth;

    @Column(name = "total_requests", nullable = false)
    private Long totalRequests;

    @Column(name = "successful_requests", nullable = false)
    private Long successfulRequests;

    // NEW: Token tracking
    @Column(name = "total_tokens")
    private Long totalTokens;

    @Column(name = "total_cost")
    private Double totalCost;
}
```

---

### **Step 6: Update Repository Upsert**

```java
@Modifying
@Query(value = """
    INSERT INTO monthly_usage_summaries 
        (api_key_id, year_month, total_requests, successful_requests, total_tokens, total_cost)
    VALUES 
        (:apiKeyId, :yearMonth, 1, :successIncrement, :tokens, :cost)
    ON CONFLICT (api_key_id, year_month) DO UPDATE SET
        total_requests = monthly_usage_summaries.total_requests + 1,
        successful_requests = monthly_usage_summaries.successful_requests + :successIncrement,
        total_tokens = monthly_usage_summaries.total_tokens + :tokens,
        total_cost = monthly_usage_summaries.total_cost + :cost
    """, nativeQuery = true)
void upsertUsage(String apiKeyId, String yearMonth, int successIncrement, Long tokens, Double cost);
```

---

## 📊 Different Billing Models

### **Model 1: Request-Based (Current)**
```
Pricing: $0.01 per request
Usage: 1000 requests
Cost: $10
```
**Good for:** Regular APIs (weather, payments, etc.)

---

### **Model 2: Token-Based (AI APIs)**
```
Pricing: $0.03 per 1K tokens
Usage: 500,000 tokens
Cost: $15
```
**Good for:** OpenAI, Anthropic, Google Gemini

---

### **Model 3: Compute-Based**
```
Pricing: $0.10 per compute unit
Usage: 150 compute units
Cost: $15
```
**Good for:** Video processing, image generation, ML inference

---

### **Model 4: Data-Based**
```
Pricing: $0.05 per GB processed
Usage: 300 GB
Cost: $15
```
**Good for:** Data transformation, ETL, file processing

---

### **Model 5: Hybrid (Most Flexible)**
```
Pricing: 
  - $0.01 per request (base)
  - $0.03 per 1K tokens (usage)
  - $0.10 per compute unit (processing)

Usage:
  - 1000 requests
  - 500,000 tokens
  - 150 compute units

Cost: $10 + $15 + $15 = $40
```
**Good for:** Complex APIs with multiple cost factors

---

## 🎯 Real-World Examples

### **Example 1: OpenAI-like API**

**Request:**
```json
POST /v1/chat/completions
{
  "model": "gpt-4",
  "messages": [{"role": "user", "content": "Write a 1000-word essay"}]
}
```

**Response:**
```json
{
  "choices": [{
    "message": {"content": "Here is your essay..."}
  }],
  "usage": {
    "prompt_tokens": 15,
    "completion_tokens": 1500,
    "total_tokens": 1515
  }
}
```

**Tracking:**
```java
UsageEvent event = UsageEvent.builder()
    .apiKeyId("key-123")
    .inputTokens(15L)
    .outputTokens(1500L)
    .totalTokens(1515L)
    .modelUsed("gpt-4")
    .estimatedCost(0.0945)  // (15/1000 * $0.03) + (1500/1000 * $0.06)
    .build();
```

**Database:**
```sql
-- monthly_usage_summaries
api_key_id | year_month | total_requests | total_tokens | total_cost
-----------|------------|----------------|--------------|------------
key-123    | 2026-05    | 1              | 1515         | $0.0945
```

---

### **Example 2: Image Generation API**

**Request:**
```json
POST /v1/images/generate
{
  "prompt": "A sunset over mountains",
  "size": "1024x1024",
  "quality": "hd"
}
```

**Tracking:**
```java
UsageEvent event = UsageEvent.builder()
    .apiKeyId("key-456")
    .computeUnits(10.0)  // HD image = 10 compute units
    .estimatedCost(0.20)  // $0.02 per compute unit
    .build();
```

---

### **Example 3: Video Processing API**

**Request:**
```json
POST /v1/video/transcode
{
  "input_url": "video.mp4",
  "output_format": "1080p"
}
```

**Tracking:**
```java
UsageEvent event = UsageEvent.builder()
    .apiKeyId("key-789")
    .dataSizeBytes(500_000_000L)  // 500 MB video
    .latencyMs(45000L)  // 45 seconds processing
    .computeUnits(50.0)  // Video processing units
    .estimatedCost(2.50)  // $0.05 per compute unit
    .build();
```

---

## 🔍 Querying Token-Based Usage

### **Get token usage for billing:**
```sql
SELECT 
    api_key_id,
    year_month,
    total_requests,
    total_tokens,
    total_cost,
    ROUND(total_tokens::numeric / total_requests, 2) as avg_tokens_per_request
FROM monthly_usage_summaries
WHERE year_month = '2026-05'
ORDER BY total_cost DESC;
```

### **Find expensive requests:**
```sql
SELECT 
    api_key_id,
    timestamp,
    total_tokens,
    estimated_cost,
    model_used
FROM usage_logs
WHERE estimated_cost > 1.0  -- Requests costing more than $1
ORDER BY estimated_cost DESC
LIMIT 20;
```

### **Token usage by model:**
```sql
SELECT 
    model_used,
    COUNT(*) as request_count,
    SUM(total_tokens) as total_tokens,
    SUM(estimated_cost) as total_cost,
    ROUND(AVG(total_tokens), 2) as avg_tokens_per_request
FROM usage_logs
WHERE timestamp >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY model_used
ORDER BY total_cost DESC;
```

---

## 💡 Implementation Challenges

### **Challenge 1: Reading Response Body**

**Problem:** Spring Cloud Gateway is reactive - reading response body is tricky.

**Solution:** Use `ModifyResponseBodyGatewayFilterFactory` or response decorators.

```java
// Simplified example
ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        return super.writeWith(Flux.from(body).map(dataBuffer -> {
            // Read response body here
            byte[] content = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(content);
            
            // Parse JSON and extract token usage
            TokenUsage usage = parseTokenUsage(new String(content));
            
            // Store for later use
            exchange.getAttributes().put("tokenUsage", usage);
            
            return dataBuffer;
        }));
    }
};
```

---

### **Challenge 2: Different AI Providers**

**Problem:** Each AI provider has different response formats.

**OpenAI:**
```json
{"usage": {"prompt_tokens": 10, "completion_tokens": 20}}
```

**Anthropic:**
```json
{"usage": {"input_tokens": 10, "output_tokens": 20}}
```

**Google Gemini:**
```json
{"usageMetadata": {"promptTokenCount": 10, "candidatesTokenCount": 20}}
```

**Solution:** Create provider-specific parsers.

```java
interface TokenExtractor {
    TokenUsage extract(String responseBody);
}

class OpenAITokenExtractor implements TokenExtractor {
    public TokenUsage extract(String responseBody) {
        // Parse OpenAI format
    }
}

class AnthropicTokenExtractor implements TokenExtractor {
    public TokenUsage extract(String responseBody) {
        // Parse Anthropic format
    }
}
```

---

### **Challenge 3: Streaming Responses**

**Problem:** AI APIs often stream responses (Server-Sent Events).

**Example:**
```
data: {"choices":[{"delta":{"content":"Hello"}}]}
data: {"choices":[{"delta":{"content":" world"}}]}
data: [DONE]
```

**Solution:** Accumulate tokens as stream progresses, send final count at the end.

---

## 🎯 Recommended Approach

### **Phase 1: Keep Current System (Request-Based)**
- Works for 90% of APIs
- Simple to implement and understand
- Already done! ✅

### **Phase 2: Add Token Tracking (Optional)**
- Only if you target AI API providers
- Requires response body parsing
- More complex but more accurate

### **Phase 3: Multi-Metric Tracking (Advanced)**
- Support multiple billing models
- Let API providers choose: requests, tokens, compute, data, or hybrid
- Most flexible but most complex

---

## ✅ Summary

**Your observation is 100% correct!**

**For regular APIs:**
- 1 request = 1 count ✅ (what you have now)

**For AI APIs:**
- 1 request = variable tokens ✅ (needs enhancement)

**To add token tracking:**
1. Extend `UsageEvent` with token fields
2. Parse response body to extract token usage
3. Update database schema
4. Modify upsert logic to sum tokens
5. Create token-based billing queries

**Recommendation:**
- **Keep current system** for now (request-based)
- **Add token tracking** only if you target AI API providers
- **Start simple**, add complexity only when needed

**Your system is already production-ready for 90% of use cases!** 🎉

Token tracking is an **enhancement**, not a requirement.
