package com.apiguard.management.dto;

/**
 * Response DTO for enabling an API key.
 * Indicates whether the operation was successful and if the key was already enabled.
 */
public record EnableKeyResponse(
    String message,
    boolean wasAlreadyEnabled
) {
}
