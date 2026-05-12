package com.apiguard.usage.service;

/**
 * Service for checking and enforcing quota limits.
 */
public interface QuotaEnforcementService {

    /**
     * Check if the API key has exceeded its monthly quota and trigger deactivation if needed.
     * This method is called after each usage event is processed.
     *
     * @param apiKeyId The UUID of the API key
     * @param yearMonth The year-month period (format: YYYY-MM)
     * @param currentUsage The current total request count
     */
    void checkAndEnforceQuota(String apiKeyId, String yearMonth, long currentUsage);

    /**
     * Retrieve the monthly quota for an API key from the Management Service.
     * Returns -1 for unlimited quotas.
     *
     * @param apiKeyId The UUID of the API key
     * @return The monthly quota limit, or -1 for unlimited
     */
    long getMonthlyQuota(String apiKeyId);
}
