package com.apiguard.common.dto;

import java.util.UUID;

/**
 * Internal notification object used to trigger webhook delivery.
 * Contains all information needed to deliver a webhook notification.
 */
public record WebhookNotification(
    UUID apiKeyId,
    String eventType,
    String webhookUrl,
    String webhookSecret,
    WebhookPayload payload
) {
    /**
     * Builder for WebhookNotification.
     */
    public static class Builder {
        private UUID apiKeyId;
        private String eventType;
        private String webhookUrl;
        private String webhookSecret;
        private WebhookPayload payload;
        
        public Builder apiKeyId(UUID apiKeyId) {
            this.apiKeyId = apiKeyId;
            return this;
        }
        
        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public Builder webhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
            return this;
        }
        
        public Builder webhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
            return this;
        }
        
        public Builder payload(WebhookPayload payload) {
            this.payload = payload;
            return this;
        }
        
        public WebhookNotification build() {
            return new WebhookNotification(apiKeyId, eventType, webhookUrl, webhookSecret, payload);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
