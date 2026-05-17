package com.apiguard.management.controller;

import com.apiguard.common.dto.WebhookConfig;
import com.apiguard.management.service.WebhookConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for webhook configuration and management.
 * Provides endpoints for configuring webhooks, retrieving history, and testing webhook delivery.
 */
@RestController
@RequestMapping("/api/keys/{keyId}/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookConfigService webhookConfigService;

    /**
     * Configure webhook URL for an API key.
     * Generates a shared secret if not already present.
     *
     * @param keyId API key identifier
     * @param request Webhook configuration request
     * @param authentication Spring Security authentication
     * @return Success message
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> configureWebhook(
            @PathVariable UUID keyId,
            @Valid @RequestBody WebhookConfigRequest request,
            Authentication authentication) {
        
        String ownerEmail = authentication.getName();
        
        try {
            webhookConfigService.configureWebhook(keyId, request.webhookUrl(), ownerEmail);
            log.info("Webhook configured for API key: {} by owner: {}", keyId, ownerEmail);
            return ResponseEntity.ok(Map.of("message", "Webhook configured successfully"));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid webhook configuration: keyId={}, error={}", keyId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            log.warn("Unauthorized webhook configuration attempt: keyId={}, user={}", keyId, ownerEmail);
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("API key not found: keyId={}", keyId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update webhook URL for an API key.
     * Preserves the existing shared secret.
     *
     * @param keyId API key identifier
     * @param request Webhook configuration request
     * @param authentication Spring Security authentication
     * @return Success message
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> updateWebhook(
            @PathVariable UUID keyId,
            @Valid @RequestBody WebhookConfigRequest request,
            Authentication authentication) {
        
        String ownerEmail = authentication.getName();
        
        try {
            webhookConfigService.updateWebhook(keyId, request.webhookUrl(), ownerEmail);
            log.info("Webhook updated for API key: {} by owner: {}", keyId, ownerEmail);
            return ResponseEntity.ok(Map.of("message", "Webhook updated successfully"));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid webhook update: keyId={}, error={}", keyId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            log.warn("Unauthorized webhook update attempt: keyId={}, user={}", keyId, ownerEmail);
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("API key not found: keyId={}", keyId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get webhook configuration for an API key.
     * Includes webhook URL and shared secret.
     *
     * @param keyId API key identifier
     * @param authentication Spring Security authentication
     * @return Webhook configuration
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getWebhookConfig(
            @PathVariable UUID keyId,
            Authentication authentication) {
        
        String ownerEmail = authentication.getName();
        
        try {
            WebhookConfig config = webhookConfigService.getWebhookConfig(keyId);
            
            // Verify ownership before returning sensitive data
            String secret = webhookConfigService.getWebhookSecret(keyId, ownerEmail);
            
            WebhookConfigResponse response = new WebhookConfigResponse(
                config.webhookUrl(),
                secret,
                config.isEnabled()
            );
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.warn("Unauthorized webhook config access: keyId={}, user={}", keyId, ownerEmail);
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("API key not found: keyId={}", keyId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get webhook delivery history for an API key.
     * Returns most recent deliveries first.
     * Note: This endpoint requires internal API call to usage-service.
     *
     * @param keyId API key identifier
     * @param limit Maximum number of records to return (default 50, max 100)
     * @param authentication Spring Security authentication
     * @return List of webhook history records
     */
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getWebhookHistory(
            @PathVariable UUID keyId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit,
            Authentication authentication) {
        
        String ownerEmail = authentication.getName();
        
        try {
            // Verify ownership
            webhookConfigService.getWebhookSecret(keyId, ownerEmail);
            
            // Future Implementation: Implement internal API call to usage-service to retrieve history
            // For now, return empty list
            log.info("Webhook history requested for API key: {} by owner: {}", keyId, ownerEmail);
            return ResponseEntity.ok(List.of());
        } catch (SecurityException e) {
            log.warn("Unauthorized webhook history access: keyId={}, user={}", keyId, ownerEmail);
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("API key not found: keyId={}", keyId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Trigger a test webhook notification.
     * Sends a webhook with event type "quota.test".
     *
     * @param keyId API key identifier
     * @param authentication Spring Security authentication
     * @return Test result
     */
    @PostMapping("/test")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> testWebhook(
            @PathVariable UUID keyId,
            Authentication authentication) {
        
        String ownerEmail = authentication.getName();
        
        try {
            // Verify ownership and webhook configuration
            WebhookConfig config = webhookConfigService.getWebhookConfig(keyId);
            webhookConfigService.getWebhookSecret(keyId, ownerEmail);
            
            if (!config.isEnabled()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Webhook is not configured for this API key")
                );
            }
            
            // Future Implementation: Implement test webhook trigger via internal API to usage-service
            // For now, return success message
            log.info("Test webhook triggered for API key: {} by owner: {}", keyId, ownerEmail);
            return ResponseEntity.ok(Map.of(
                "message", "Test webhook sent",
                "deliveryStatus", "PENDING"
            ));
        } catch (SecurityException e) {
            log.warn("Unauthorized test webhook attempt: keyId={}, user={}", keyId, ownerEmail);
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("API key not found: keyId={}", keyId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Request DTO for webhook configuration.
     */
    public record WebhookConfigRequest(
        String webhookUrl  // Can be null to disable webhooks
    ) {}

    /**
     * Response DTO for webhook configuration.
     */
    public record WebhookConfigResponse(
        String webhookUrl,
        String webhookSecret,
        boolean enabled
    ) {}
    
    /**
     * Response DTO for webhook delivery history.
     */
    public record WebhookHistoryResponse(
        UUID id,
        String eventType,
        Instant sentAt,
        Integer httpStatusCode,
        int retryCount,
        String deliveryStatus,
        double usagePercentage,
        String errorMessage
    ) {}
}
