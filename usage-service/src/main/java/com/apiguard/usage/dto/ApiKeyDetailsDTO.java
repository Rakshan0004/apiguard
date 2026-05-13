package com.apiguard.usage.dto;

/**
 * DTO containing detailed information about an API key.
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
