package com.apiguard.common.dto;

import java.util.UUID;

/**
 * Webhook configuration for an API key.
 * Contains webhook URL, shared secret, and enabled status.
 */
public record WebhookConfig(
    UUID apiKeyId,
    String webhookUrl,
    String webhookSecret,
    boolean enabled
) {
    /**
     * Check if webhook is enabled and properly configured.
     * @return true if webhook URL is not null/blank and enabled flag is true
     */
    public boolean isEnabled() {
        return enabled && webhookUrl != null && !webhookUrl.isBlank();
    }
    
    /**
     * Validate that webhook URL uses HTTPS protocol.
     * @return true if URL is null/blank or uses HTTPS
     */
    public boolean hasValidProtocol() {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return true; // null/blank URLs are allowed (disables webhooks)
        }
        return webhookUrl.toLowerCase().startsWith("https://");
    }
}
