package com.apiguard.management.service;

import com.apiguard.common.dto.WebhookConfig;
import com.apiguard.management.entity.ApiKey;
import com.apiguard.management.repository.ApiKeyRepository;
import com.apiguard.management.util.WebhookSecretGenerator;
import com.apiguard.management.util.WebhookUrlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing webhook configuration for API keys.
 * Handles webhook URL validation, secret generation, and owner-based access control.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookConfigService {

    private final ApiKeyRepository apiKeyRepository;

    /**
     * Configure webhook URL for an API key.
     * Validates the URL and generates a shared secret if not already present.
     *
     * @param apiKeyId API key identifier
     * @param webhookUrl Webhook URL (HTTPS required, null to disable)
     * @param ownerEmail Email of the API key owner (for authorization)
     * @throws IllegalArgumentException if URL is invalid
     * @throws SecurityException if owner email doesn't match
     * @throws IllegalStateException if API key not found
     */
    @Transactional
    public void configureWebhook(UUID apiKeyId, String webhookUrl, String ownerEmail) {
        log.info("Configuring webhook for API key: {}", apiKeyId);

        // Validate URL
        WebhookUrlValidator.validate(webhookUrl);

        // Find API key and validate owner
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new IllegalStateException("API key not found: " + apiKeyId));

        validateOwner(apiKey, ownerEmail);

        // Set webhook URL
        apiKey.setWebhookUrl(webhookUrl);

        // Generate secret if not already present and URL is not null
        if (webhookUrl != null && !webhookUrl.isBlank() && apiKey.getWebhookSecret() == null) {
            String secret = WebhookSecretGenerator.generateSecret();
            apiKey.setWebhookSecret(secret);
            log.info("Generated webhook secret for API key: {}", apiKeyId);
        }

        apiKeyRepository.save(apiKey);
        log.info("Webhook configured successfully for API key: {}", apiKeyId);
    }

    /**
     * Update webhook URL for an API key.
     * Validates the URL but preserves the existing shared secret.
     *
     * @param apiKeyId API key identifier
     * @param webhookUrl New webhook URL (HTTPS required, null to disable)
     * @param ownerEmail Email of the API key owner (for authorization)
     * @throws IllegalArgumentException if URL is invalid
     * @throws SecurityException if owner email doesn't match
     * @throws IllegalStateException if API key not found
     */
    @Transactional
    public void updateWebhook(UUID apiKeyId, String webhookUrl, String ownerEmail) {
        log.info("Updating webhook for API key: {}", apiKeyId);

        // Validate URL
        WebhookUrlValidator.validate(webhookUrl);

        // Find API key and validate owner
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new IllegalStateException("API key not found: " + apiKeyId));

        validateOwner(apiKey, ownerEmail);

        // Update webhook URL (preserve existing secret)
        apiKey.setWebhookUrl(webhookUrl);

        // Generate secret if not present and URL is not null
        if (webhookUrl != null && !webhookUrl.isBlank() && apiKey.getWebhookSecret() == null) {
            String secret = WebhookSecretGenerator.generateSecret();
            apiKey.setWebhookSecret(secret);
            log.info("Generated webhook secret for API key: {}", apiKeyId);
        }

        apiKeyRepository.save(apiKey);
        log.info("Webhook updated successfully for API key: {}", apiKeyId);
    }

    /**
     * Get webhook configuration for an API key.
     *
     * @param apiKeyId API key identifier
     * @return Webhook configuration
     * @throws IllegalStateException if API key not found
     */
    @Transactional(readOnly = true)
    public WebhookConfig getWebhookConfig(UUID apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new IllegalStateException("API key not found: " + apiKeyId));

        boolean enabled = apiKey.getWebhookUrl() != null && !apiKey.getWebhookUrl().isBlank();

        return new WebhookConfig(
            apiKey.getId(),
            apiKey.getWebhookUrl(),
            apiKey.getWebhookSecret(),
            enabled
        );
    }

    /**
     * Get webhook secret for an API key.
     * Requires owner authorization.
     *
     * @param apiKeyId API key identifier
     * @param ownerEmail Email of the API key owner (for authorization)
     * @return Webhook secret
     * @throws SecurityException if owner email doesn't match
     * @throws IllegalStateException if API key not found
     */
    @Transactional(readOnly = true)
    public String getWebhookSecret(UUID apiKeyId, String ownerEmail) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new IllegalStateException("API key not found: " + apiKeyId));

        validateOwner(apiKey, ownerEmail);

        return apiKey.getWebhookSecret();
    }

    /**
     * Validate that the owner email matches the API key owner.
     *
     * @param apiKey API key entity
     * @param ownerEmail Email to validate
     * @throws SecurityException if owner email doesn't match
     */
    private void validateOwner(ApiKey apiKey, String ownerEmail) {
        String actualOwnerEmail = apiKey.getRegisteredApi().getOwnerEmail();
        if (!actualOwnerEmail.equals(ownerEmail)) {
            log.warn("Owner validation failed for API key: {}. Expected: {}, Got: {}",
                apiKey.getId(), actualOwnerEmail, ownerEmail);
            throw new SecurityException("Access denied: you do not own this API key");
        }
    }
}
