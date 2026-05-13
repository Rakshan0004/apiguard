package com.apiguard.management.dto;

/**
 * DTO containing detailed information about an API key.
 * Used for quota enforcement and key management operations.
 */
public record ApiKeyDetailsDTO(
    String apiKeyId,
    String planName,
    long monthlyQuota,
    int rateLimitRpm,
    boolean active,
    String disabledReason,
    String webhookUrl,
    String webhookSecret
) {
    /**
     * Get webhook URL.
     * @return Webhook URL or null if not configured
     */
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    /**
     * Get webhook secret.
     * @return Webhook secret or null if not configured
     */
    public String getWebhookSecret() {
        return webhookSecret;
    }
}
