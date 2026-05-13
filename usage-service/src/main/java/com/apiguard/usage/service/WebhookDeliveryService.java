package com.apiguard.usage.service;

import com.apiguard.common.dto.WebhookNotification;
import com.apiguard.usage.dto.DeliveryResult;
import com.apiguard.usage.entity.WebhookHistory;
import com.apiguard.usage.repository.WebhookHistoryRepository;
import com.apiguard.usage.util.HmacSignatureGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for delivering webhook notifications to API owners.
 * Handles HTTP delivery with retry logic, HMAC signatures, and delivery tracking.
 */
@Service
@Slf4j
public class WebhookDeliveryService {

    private final RestTemplate restTemplate;
    private final WebhookHistoryRepository webhookHistoryRepository;

    public WebhookDeliveryService(
        @Qualifier("webhookRestTemplate") RestTemplate restTemplate,
        WebhookHistoryRepository webhookHistoryRepository
    ) {
        this.restTemplate = restTemplate;
        this.webhookHistoryRepository = webhookHistoryRepository;
    }

    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {1000, 2000, 4000}; // 1s, 2s, 4s
    private static final int REQUEST_TIMEOUT_MS = 10000; // 10 seconds

    /**
     * Deliver webhook notification synchronously.
     *
     * @param notification Webhook notification to deliver
     */
    public void deliverWebhook(WebhookNotification notification) {
        log.info("Delivering webhook for API key: {}, event: {}",
            notification.apiKeyId(), notification.eventType());

        try {
            // Serialize payload
            String payloadJson = notification.payload().toJson();

            // Generate timestamp and signature
            String timestamp = String.valueOf(System.currentTimeMillis());
            String signature = HmacSignatureGenerator.generateSignature(
                timestamp, payloadJson, notification.webhookSecret()
            );

            // Prepare headers
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            headers.put("X-Webhook-Signature", signature);
            headers.put("X-Webhook-Timestamp", timestamp);

            // Send with retry
            DeliveryResult result = sendWithRetry(notification.webhookUrl(), payloadJson, headers);

            // Record delivery attempt
            recordDeliveryAttempt(notification, result);

            if (result.isSuccess()) {
                log.info("Webhook delivered successfully for API key: {}", notification.apiKeyId());
            } else {
                log.warn("Webhook delivery failed for API key: {} after {} attempts. Error: {}",
                    notification.apiKeyId(), result.getRetryCount() + 1, result.getErrorMessage());
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook payload for API key: {}", notification.apiKeyId(), e);
            DeliveryResult result = DeliveryResult.builder()
                .success(false)
                .retryCount(0)
                .errorMessage("Payload serialization failed: " + e.getMessage())
                .build();
            recordDeliveryAttempt(notification, result);
        } catch (Exception e) {
            log.error("Unexpected error delivering webhook for API key: {}", notification.apiKeyId(), e);
            DeliveryResult result = DeliveryResult.builder()
                .success(false)
                .retryCount(0)
                .errorMessage("Unexpected error: " + e.getMessage())
                .build();
            recordDeliveryAttempt(notification, result);
        }
    }

    /**
     * Deliver webhook notification asynchronously.
     *
     * @param notification Webhook notification to deliver
     * @return CompletableFuture that completes when delivery is done
     */
    @Async
    public CompletableFuture<Void> deliverWebhookAsync(WebhookNotification notification) {
        deliverWebhook(notification);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Send HTTP POST request with retry logic.
     *
     * @param url Webhook URL
     * @param payload JSON payload
     * @param headers HTTP headers
     * @return Delivery result
     */
    private DeliveryResult sendWithRetry(String url, String payload, Map<String, String> headers) {
        int attempt = 0;
        String lastError = null;
        Integer lastStatusCode = null;

        while (attempt <= MAX_RETRIES) {
            try {
                // Prepare request
                HttpHeaders httpHeaders = new HttpHeaders();
                headers.forEach(httpHeaders::set);
                HttpEntity<String> request = new HttpEntity<>(payload, httpHeaders);

                // Send request
                ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
                );

                // Check if successful (2xx status)
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.debug("Webhook delivery successful on attempt {}", attempt + 1);
                    return DeliveryResult.builder()
                        .success(true)
                        .httpStatusCode(response.getStatusCode().value())
                        .retryCount(attempt)
                        .build();
                } else {
                    lastStatusCode = response.getStatusCode().value();
                    lastError = "HTTP " + lastStatusCode;
                    log.warn("Webhook delivery returned non-2xx status: {} on attempt {}",
                        lastStatusCode, attempt + 1);
                }
            } catch (HttpClientErrorException e) {
                // 4xx errors - don't retry (permanent failure)
                lastStatusCode = e.getStatusCode().value();
                lastError = "HTTP " + lastStatusCode + ": " + e.getMessage();
                log.warn("Webhook delivery failed with client error: {} on attempt {}",
                    lastStatusCode, attempt + 1);
                break; // Don't retry 4xx errors
            } catch (HttpServerErrorException e) {
                // 5xx errors - retry
                lastStatusCode = e.getStatusCode().value();
                lastError = "HTTP " + lastStatusCode + ": " + e.getMessage();
                log.warn("Webhook delivery failed with server error: {} on attempt {}",
                    lastStatusCode, attempt + 1);
            } catch (ResourceAccessException e) {
                // Timeout or connection errors - retry
                lastError = "Connection error: " + e.getMessage();
                log.warn("Webhook delivery failed with connection error on attempt {}: {}",
                    attempt + 1, e.getMessage());
            } catch (Exception e) {
                // Other errors - retry
                lastError = "Unexpected error: " + e.getMessage();
                log.warn("Webhook delivery failed with unexpected error on attempt {}: {}",
                    attempt + 1, e.getMessage());
            }

            // Wait before retry (if not last attempt)
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAYS_MS[attempt]);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Retry delay interrupted");
                    break;
                }
            }

            attempt++;
        }

        // All attempts failed
        return DeliveryResult.builder()
            .success(false)
            .httpStatusCode(lastStatusCode)
            .retryCount(attempt)
            .errorMessage(lastError)
            .build();
    }

    /**
     * Record webhook delivery attempt in history.
     *
     * @param notification Webhook notification
     * @param result Delivery result
     */
    private void recordDeliveryAttempt(WebhookNotification notification, DeliveryResult result) {
        try {
            // Determine threshold percentage from event type
            int thresholdPercentage = switch (notification.eventType()) {
                case "quota.warning" -> 80;
                case "quota.exceeded" -> 100;
                case "quota.test" -> 0;
                default -> 0;
            };

            WebhookHistory history = WebhookHistory.builder()
                .apiKeyId(notification.apiKeyId())
                .eventType(notification.eventType())
                .thresholdPercentage(thresholdPercentage)
                .yearMonth(notification.payload().yearMonth())
                .usageCount(notification.payload().currentUsage())
                .quotaLimit(notification.payload().quotaLimit())
                .usagePercentage(notification.payload().usagePercentage())
                .webhookUrl(notification.webhookUrl())
                .sentAt(Instant.now())
                .httpStatusCode(result.getHttpStatusCode())
                .retryCount(result.getRetryCount())
                .deliveryStatus(result.getDeliveryStatus())
                .errorMessage(result.getErrorMessage())
                .build();

            webhookHistoryRepository.save(history);
            log.debug("Recorded webhook delivery attempt for API key: {}", notification.apiKeyId());
        } catch (DataIntegrityViolationException e) {
            // Duplicate key violation - notification already sent (race condition)
            log.info("Webhook notification already recorded for API key: {} (duplicate prevented)",
                notification.apiKeyId());
        } catch (Exception e) {
            // Log error but don't fail delivery
            log.error("Failed to record webhook delivery attempt for API key: {}",
                notification.apiKeyId(), e);
        }
    }
}
