package com.apiguard.usage.service;

import com.apiguard.common.dto.WebhookNotification;
import com.apiguard.common.dto.WebhookPayload;
import com.apiguard.usage.client.ManagementServiceClient;
import com.apiguard.usage.dto.ApiKeyDetailsDTO;
import com.apiguard.usage.repository.WebhookHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for triggering webhook notifications based on quota thresholds.
 * Monitors usage and triggers webhooks at 80% and 100% thresholds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookTriggerService {

    private final WebhookDeliveryService webhookDeliveryService;
    private final WebhookHistoryRepository webhookHistoryRepository;
    private final ManagementServiceClient managementServiceClient;

    private static final double WARNING_THRESHOLD = 80.0;
    private static final double EXCEEDED_THRESHOLD = 100.0;

    /**
     * Check quota usage and trigger webhook if threshold is reached.
     * Triggers warning at 80% and exceeded at 100%.
     *
     * @param apiKeyId API key identifier
     * @param currentUsage Current usage count
     * @param quotaLimit Quota limit
     * @param yearMonth Billing period in YYYY-MM format
     */
    public void checkAndTriggerWebhook(String apiKeyId, long currentUsage, long quotaLimit, String yearMonth) {
        if (quotaLimit <= 0) {
            log.debug("Skipping webhook check for API key {} - quota limit is 0", apiKeyId);
            return;
        }

        // Calculate usage percentage
        double usagePercentage = ((double) currentUsage / quotaLimit) * 100.0;

        UUID apiKeyUuid = UUID.fromString(apiKeyId);

        // Check 100% threshold first (higher priority)
        if (usagePercentage >= EXCEEDED_THRESHOLD) {
            if (!hasNotificationBeenSent(apiKeyUuid, "quota.exceeded", yearMonth)) {
                log.info("Quota exceeded threshold reached for API key: {} ({}%)", apiKeyId, usagePercentage);
                triggerWebhook(apiKeyUuid, "quota.exceeded", currentUsage, quotaLimit, usagePercentage, yearMonth);
            }
        }
        // Check 80% threshold
        else if (usagePercentage >= WARNING_THRESHOLD) {
            if (!hasNotificationBeenSent(apiKeyUuid, "quota.warning", yearMonth)) {
                log.info("Quota warning threshold reached for API key: {} ({}%)", apiKeyId, usagePercentage);
                triggerWebhook(apiKeyUuid, "quota.warning", currentUsage, quotaLimit, usagePercentage, yearMonth);
            }
        }
    }

    /**
     * Trigger a webhook notification.
     *
     * @param apiKeyId API key identifier
     * @param eventType Event type (quota.warning, quota.exceeded, quota.test)
     * @param currentUsage Current usage count
     * @param quotaLimit Quota limit
     * @param usagePercentage Usage percentage
     * @param yearMonth Billing period in YYYY-MM format
     */
    public void triggerWebhook(UUID apiKeyId, String eventType, long currentUsage,
                               long quotaLimit, double usagePercentage, String yearMonth) {
        try {
            // Get API key details including webhook configuration
            ApiKeyDetailsDTO apiKeyDetails = managementServiceClient.getApiKeyDetails(apiKeyId.toString());

            // Check if webhook is configured
            if (apiKeyDetails.getWebhookUrl() == null || apiKeyDetails.getWebhookUrl().isBlank()) {
                log.debug("No webhook configured for API key: {}", apiKeyId);
                return;
            }

            if (apiKeyDetails.getWebhookSecret() == null || apiKeyDetails.getWebhookSecret().isBlank()) {
                log.warn("Webhook URL configured but no secret found for API key: {}", apiKeyId);
                return;
            }

            // Create webhook payload
            WebhookPayload payload = new WebhookPayload(
                eventType,
                apiKeyId.toString(),
                currentUsage,
                quotaLimit,
                usagePercentage,
                Instant.now().toString(), // ISO 8601 format
                yearMonth
            );

            // Create webhook notification
            WebhookNotification notification = WebhookNotification.builder()
                .apiKeyId(apiKeyId)
                .eventType(eventType)
                .webhookUrl(apiKeyDetails.getWebhookUrl())
                .webhookSecret(apiKeyDetails.getWebhookSecret())
                .payload(payload)
                .build();

            // Deliver webhook asynchronously
            webhookDeliveryService.deliverWebhookAsync(notification);

            log.info("Webhook triggered for API key: {}, event: {}", apiKeyId, eventType);
        } catch (Exception e) {
            log.error("Failed to trigger webhook for API key: {}, event: {}", apiKeyId, eventType, e);
        }
    }

    /**
     * Check if a notification has already been sent for a specific threshold and billing period.
     * Used for deduplication.
     *
     * @param apiKeyId API key identifier
     * @param eventType Event type
     * @param yearMonth Billing period in YYYY-MM format
     * @return true if notification already sent
     */
    private boolean hasNotificationBeenSent(UUID apiKeyId, String eventType, String yearMonth) {
        return webhookHistoryRepository.existsByApiKeyIdAndEventTypeAndYearMonth(apiKeyId, eventType, yearMonth);
    }
}
