package com.apiguard.usage.client;

import com.apiguard.usage.config.QuotaEnforcementConfig;
import com.apiguard.usage.dto.ApiKeyDetailsDTO;
import com.apiguard.usage.dto.DisableKeyRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of ManagementServiceClient using WebClient.
 */
@Component
@Slf4j
public class ManagementServiceClientImpl implements ManagementServiceClient {

    private final WebClient webClient;
    private final int timeoutMs;

    public ManagementServiceClientImpl(QuotaEnforcementConfig config, WebClient.Builder webClientBuilder) {
        this.timeoutMs = config.getTimeoutMs();
        this.webClient = webClientBuilder
                .baseUrl(config.getManagementServiceUrl())
                .build();
    }

    @Override
    public CompletableFuture<Void> disableKey(String apiKeyId) {
        return CompletableFuture.runAsync(() -> {
            try {
                webClient.post()
                        .uri("/internal/keys/{keyId}/disable", apiKeyId)
                        .bodyValue(new DisableKeyRequest("QUOTA_EXCEEDED"))
                        .retrieve()
                        .toBodilessEntity()
                        .timeout(Duration.ofMillis(timeoutMs))
                        .block();
                log.info("Successfully disabled API key: {}", apiKeyId);
            } catch (Exception e) {
                log.error("Failed to disable API key: {}, error: {}", apiKeyId, e.getMessage());
                // Do not throw exception - failures are logged only
            }
        });
    }

    @Override
    public ApiKeyDetailsDTO getApiKeyDetails(String apiKeyId) {
        try {
            return webClient.get()
                    .uri("/internal/keys/{keyId}", apiKeyId)
                    .retrieve()
                    .bodyToMono(ApiKeyDetailsDTO.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();
        } catch (Exception e) {
            log.error("Failed to get API key details: {}, error: {}", apiKeyId, e.getMessage());
            throw new RuntimeException("Failed to retrieve API key details", e);
        }
    }

    @Override
    public CompletableFuture<Void> enableKey(String apiKeyId) {
        return CompletableFuture.runAsync(() -> {
            try {
                webClient.post()
                        .uri("/internal/keys/{keyId}/enable", apiKeyId)
                        .retrieve()
                        .toBodilessEntity()
                        .timeout(Duration.ofMillis(timeoutMs))
                        .block();
                log.info("Successfully enabled API key: {}", apiKeyId);
            } catch (Exception e) {
                log.error("Failed to enable API key: {}, error: {}", apiKeyId, e.getMessage());
                // Do not throw exception - failures are logged only
            }
        });
    }

    @Override
    public List<String> getQuotaDisabledKeys() {
        try {
            List<String> keys = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/keys")
                            .queryParam("disabledReason", "QUOTA_EXCEEDED")
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();
            return keys != null ? keys : List.of();
        } catch (Exception e) {
            log.error("Failed to get quota disabled keys, error: {}", e.getMessage());
            return List.of();
        }
    }
}
