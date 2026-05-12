package com.apiguard.usage.client;

import com.apiguard.usage.dto.ApiKeyDetailsDTO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST client for communicating with Management Service internal APIs.
 */
public interface ManagementServiceClient {

    /**
     * Call Management Service to disable an API key.
     * This call is non-blocking and failures are logged but do not throw exceptions.
     *
     * @param apiKeyId The UUID of the API key to disable
     * @return CompletableFuture that completes when the call finishes
     */
    CompletableFuture<Void> disableKey(String apiKeyId);

    /**
     * Retrieve API key details including plan and quota information.
     *
     * @param apiKeyId The UUID of the API key
     * @return ApiKeyDetailsDTO containing plan and quota info
     */
    ApiKeyDetailsDTO getApiKeyDetails(String apiKeyId);

    /**
     * Call Management Service to re-enable an API key.
     * Used by the monthly reset job.
     *
     * @param apiKeyId The UUID of the API key to enable
     * @return CompletableFuture that completes when the call finishes
     */
    CompletableFuture<Void> enableKey(String apiKeyId);

    /**
     * Retrieve all API keys disabled due to quota exceeded.
     * Used by the monthly reset job.
     *
     * @return List of API key IDs
     */
    List<String> getQuotaDisabledKeys();
}
