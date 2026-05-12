package com.apiguard.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Webhook payload sent to API owners when quota thresholds are reached.
 * Contains usage information and event details.
 */
public record WebhookPayload(
    @NotNull
    @Pattern(regexp = "quota\\.(warning|exceeded|test)", message = "Event type must be quota.warning, quota.exceeded, or quota.test")
    @JsonProperty("event_type")
    String eventType,
    
    @NotNull
    @JsonProperty("api_key_id")
    String apiKeyId,
    
    @JsonProperty("current_usage")
    long currentUsage,
    
    @JsonProperty("quota_limit")
    long quotaLimit,
    
    @JsonProperty("usage_percentage")
    double usagePercentage,
    
    @NotNull
    @JsonProperty("timestamp")
    String timestamp,  // ISO 8601 format
    
    @NotNull
    @JsonProperty("year_month")
    String yearMonth   // YYYY-MM format
) {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Serialize this payload to JSON string.
     * @return JSON representation of the payload
     * @throws JsonProcessingException if serialization fails
     */
    public String toJson() throws JsonProcessingException {
        return objectMapper.writeValueAsString(this);
    }
    
    /**
     * Deserialize JSON string to WebhookPayload object.
     * @param json JSON string to parse
     * @return WebhookPayload object
     * @throws JsonProcessingException if deserialization fails
     */
    public static WebhookPayload fromJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, WebhookPayload.class);
    }
}
