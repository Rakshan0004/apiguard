package com.apiguard.usage.service;

import com.apiguard.usage.client.ManagementServiceClient;
import com.apiguard.usage.dto.ApiKeyDetailsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of QuotaEnforcementService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuotaEnforcementServiceImpl implements QuotaEnforcementService {

    private final ManagementServiceClient managementServiceClient;
    private final WebhookTriggerService webhookTriggerService;

    @Override
    public void checkAndEnforceQuota(String apiKeyId, String yearMonth, long currentUsage) {
        try {
            long monthlyQuota = getMonthlyQuota(apiKeyId);

            // Skip enforcement for unlimited quota
            if (monthlyQuota == -1) {
                log.debug("Skipping quota enforcement for unlimited quota: apiKeyId={}", apiKeyId);
                return;
            }

            // Skip enforcement for zero or negative quota (invalid configuration)
            if (monthlyQuota <= 0) {
                log.warn("Invalid quota configuration: apiKeyId={}, quota={}", apiKeyId, monthlyQuota);
                return;
            }

            // Trigger webhook notification if threshold reached (async, non-blocking)
            try {
                webhookTriggerService.checkAndTriggerWebhook(apiKeyId, currentUsage, monthlyQuota, yearMonth);
            } catch (Exception e) {
                log.error("Failed to trigger webhook for key: {}, error={}", apiKeyId, e.getMessage());
                // Continue with quota enforcement even if webhook fails
            }

            // Check if quota is exceeded
            if (currentUsage >= monthlyQuota) {
                log.info("Quota exceeded for key: {}, usage: {}/{}, triggering deactivation",
                        apiKeyId, currentUsage, monthlyQuota);

                // Disable key asynchronously
                managementServiceClient.disableKey(apiKeyId)
                        .thenAccept(v -> log.info("API key disabled successfully: keyId={}, reason=QUOTA_EXCEEDED", apiKeyId))
                        .exceptionally(e -> {
                            log.error("Failed to disable API key: keyId={}, error={}", apiKeyId, e.getMessage());
                            return null;
                        });
            } else {
                log.debug("Quota check passed for key: {}, usage: {}/{}", apiKeyId, currentUsage, monthlyQuota);
            }
        } catch (Exception e) {
            log.error("Error during quota enforcement: apiKeyId={}, error={}", apiKeyId, e.getMessage(), e);
            // Do not throw exception - quota enforcement failures should not block usage tracking
        }
    }

    @Override
    public long getMonthlyQuota(String apiKeyId) {
        try {
            ApiKeyDetailsDTO details = managementServiceClient.getApiKeyDetails(apiKeyId);
            return details.monthlyQuota();
        } catch (Exception e) {
            log.error("Failed to retrieve monthly quota for key: {}, error={}", apiKeyId, e.getMessage());
            // Return -1 (unlimited) as fallback to avoid blocking usage tracking
            return -1;
        }
    }
}
