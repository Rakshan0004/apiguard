package com.apiguard.management.controller;

import com.apiguard.management.dto.ApiKeyDetailsDTO;
import com.apiguard.management.dto.DisableKeyRequest;
import com.apiguard.management.dto.DisableKeyResponse;
import com.apiguard.management.dto.EnableKeyResponse;
import com.apiguard.management.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Internal API controller for key management operations.
 * These endpoints are intended for inter-service communication only.
 */
@RestController
@RequestMapping("/internal/keys")
@RequiredArgsConstructor
@Slf4j
public class InternalKeyManagementController {

    private final ApiKeyService apiKeyService;

    /**
     * Disable an API key with a specific reason.
     * Idempotent: returns 200 even if key is already disabled.
     *
     * @param keyId The UUID of the API key to disable
     * @param request The disable request containing the reason
     * @return DisableKeyResponse with operation result
     */
    @PostMapping("/{keyId}/disable")
    public ResponseEntity<DisableKeyResponse> disableKey(
            @PathVariable UUID keyId,
            @RequestBody DisableKeyRequest request) {
        try {
            DisableKeyResponse response = apiKeyService.disableKey(keyId, request.reason());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Failed to disable key: keyId={}, error={}", keyId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Enable an API key (clear disabled status).
     * Idempotent: returns 200 even if key is already enabled.
     *
     * @param keyId The UUID of the API key to enable
     * @return EnableKeyResponse with operation result
     */
    @PostMapping("/{keyId}/enable")
    public ResponseEntity<EnableKeyResponse> enableKey(@PathVariable UUID keyId) {
        try {
            EnableKeyResponse response = apiKeyService.enableKey(keyId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Failed to enable key: keyId={}, error={}", keyId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get detailed information about an API key.
     *
     * @param keyId The UUID of the API key
     * @return ApiKeyDetailsDTO with key details
     */
    @GetMapping("/{keyId}")
    public ResponseEntity<ApiKeyDetailsDTO> getApiKeyDetails(@PathVariable UUID keyId) {
        try {
            ApiKeyDetailsDTO details = apiKeyService.getApiKeyDetails(keyId);
            return ResponseEntity.ok(details);
        } catch (IllegalArgumentException e) {
            log.error("Failed to get key details: keyId={}, error={}", keyId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Query API keys by disabled reason.
     *
     * @param disabledReason Optional filter for disabled reason (e.g., "QUOTA_EXCEEDED")
     * @return List of API key IDs as strings
     */
    @GetMapping
    public ResponseEntity<List<String>> getKeysByDisabledReason(
            @RequestParam(required = false) String disabledReason) {
        if (disabledReason == null || disabledReason.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        List<String> keyIds = apiKeyService.getKeysByDisabledReason(disabledReason);
        return ResponseEntity.ok(keyIds);
    }
}
