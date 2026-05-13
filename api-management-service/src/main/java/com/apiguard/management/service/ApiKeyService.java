package com.apiguard.management.service;

import com.apiguard.management.dto.ApiKeyDetailsDTO;
import com.apiguard.management.dto.DisableKeyResponse;
import com.apiguard.management.dto.EnableKeyResponse;
import com.apiguard.management.entity.ApiKey;
import com.apiguard.management.entity.Plan;
import com.apiguard.management.entity.RegisteredApi;
import com.apiguard.management.repository.ApiKeyRepository;
import com.apiguard.management.repository.PlanRepository;
import com.apiguard.management.repository.RegisteredApiRepository;
import com.apiguard.management.util.KeyGeneratorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository repository;
    private final RegisteredApiRepository apiRepository;
    private final PlanRepository planRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public String createApiKey(UUID apiId, UUID planId, String ownerEmail) {
        RegisteredApi api = apiRepository.findByIdAndOwnerEmail(apiId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("API not found or access denied"));

        Plan plan = planRepository.findByIdAndOwnerEmail(planId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or access denied"));

        if (!plan.getRegisteredApi().getId().equals(apiId)) {
            throw new IllegalArgumentException("This plan does not belong to the specified API");
        }

        String rawKey = KeyGeneratorUtils.generateRawKey();
        String hash = KeyGeneratorUtils.hashKey(rawKey);

        ApiKey apiKey = ApiKey.builder()
                .keyHash(hash)
                .keyPrefix(rawKey.substring(0, 8))
                .registeredApi(api)
                .plan(plan)
                .build();

        repository.save(apiKey);
        return rawKey; // Return raw key only once
    }

    public ApiKey validateKey(String rawKey) {
        String hash = KeyGeneratorUtils.hashKey(rawKey);
        return repository.findByKeyHash(hash)
                .filter(ApiKey::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive API key"));
    }

    /**
     * Disable an API key with a specific reason.
     * Idempotent operation - returns success even if key is already disabled.
     *
     * @param keyId The UUID of the API key to disable
     * @param reason The reason for disabling the key (e.g., "QUOTA_EXCEEDED")
     * @return DisableKeyResponse indicating success and whether key was already disabled
     */
    @Transactional
    public DisableKeyResponse disableKey(UUID keyId, String reason) {
        ApiKey apiKey = repository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));

        boolean wasAlreadyDisabled = !apiKey.isActive();

        if (!wasAlreadyDisabled) {
            apiKey.setActive(false);
            apiKey.setDisabledReason(reason);
            repository.save(apiKey);

            // Invalidate cache
            invalidateCache(apiKey.getKeyHash());

            log.info("API key deactivated: keyId={}, reason={}, timestamp={}",
                    keyId, reason, Instant.now());
        } else {
            log.warn("Disable request for already-disabled key: keyId={}, existingReason={}",
                    keyId, apiKey.getDisabledReason());
        }

        return new DisableKeyResponse(
                wasAlreadyDisabled ? "Key was already disabled" : "Key disabled successfully",
                wasAlreadyDisabled
        );
    }

    /**
     * Enable an API key (clear disabled status).
     * Idempotent operation - returns success even if key is already enabled.
     *
     * @param keyId The UUID of the API key to enable
     * @return EnableKeyResponse indicating success and whether key was already enabled
     */
    @Transactional
    public EnableKeyResponse enableKey(UUID keyId) {
        ApiKey apiKey = repository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));

        boolean wasAlreadyEnabled = apiKey.isActive();

        if (!wasAlreadyEnabled) {
            apiKey.setActive(true);
            apiKey.setDisabledReason(null);
            repository.save(apiKey);

            // Invalidate cache
            invalidateCache(apiKey.getKeyHash());

            log.info("API key activated: keyId={}, timestamp={}", keyId, Instant.now());
        } else {
            log.warn("Enable request for already-enabled key: keyId={}", keyId);
        }

        return new EnableKeyResponse(
                wasAlreadyEnabled ? "Key was already enabled" : "Key enabled successfully",
                wasAlreadyEnabled
        );
    }

    /**
     * Get detailed information about an API key including plan and quota details.
     *
     * @param keyId The UUID of the API key
     * @return ApiKeyDetailsDTO containing key details
     */
    @Transactional(readOnly = true)
    public ApiKeyDetailsDTO getApiKeyDetails(UUID keyId) {
        ApiKey apiKey = repository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));

        Plan plan = apiKey.getPlan();

        return new ApiKeyDetailsDTO(
                apiKey.getId().toString(),
                plan.getName(),
                plan.getMonthlyQuota(),
                plan.getRateLimitRpm(),
                apiKey.isActive(),
                apiKey.getDisabledReason(),
                apiKey.getWebhookUrl(),
                apiKey.getWebhookSecret()
        );
    }

    /**
     * Query API keys by disabled reason.
     *
     * @param disabledReason The disabled reason to filter by (e.g., "QUOTA_EXCEEDED")
     * @return List of API key IDs as strings
     */
    @Transactional(readOnly = true)
    public List<String> getKeysByDisabledReason(String disabledReason) {
        List<ApiKey> keys = repository.findByDisabledReason(disabledReason);
        return keys.stream()
                .map(key -> key.getId().toString())
                .toList();
    }

    /**
     * Invalidate the Redis cache entry for an API key.
     *
     * @param keyHash The hash of the API key
     */
    private void invalidateCache(String keyHash) {
        String cacheKey = "api:config:" + keyHash;
        redisTemplate.delete(cacheKey);
        log.debug("Cache invalidated for key hash: {}", keyHash);
    }

    /**
     * Get all API keys for a specific owner.
     *
     * @param ownerEmail The email of the owner
     * @return List of key information maps
     */
    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getKeysForOwner(String ownerEmail) {
        List<ApiKey> keys = repository.findByRegisteredApi_OwnerEmail(ownerEmail);
        return keys.stream()
                .map(key -> {
                    java.util.Map<String, Object> keyInfo = new java.util.HashMap<>();
                    keyInfo.put("id", key.getId().toString());
                    keyInfo.put("keyPrefix", key.getKeyPrefix());
                    keyInfo.put("apiName", key.getRegisteredApi().getName());
                    keyInfo.put("planName", key.getPlan().getName());
                    keyInfo.put("active", key.isActive());
                    keyInfo.put("createdAt", key.getCreatedAt().toString());
                    return keyInfo;
                })
                .toList();
    }
}
