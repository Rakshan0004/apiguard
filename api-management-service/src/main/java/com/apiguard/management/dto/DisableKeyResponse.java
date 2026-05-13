package com.apiguard.management.dto;

/**
 * Response DTO for disabling an API key.
 * Indicates whether the operation was successful and if the key was already disabled.
 */
public record DisableKeyResponse(
    String message,
    boolean wasAlreadyDisabled
) {
}
