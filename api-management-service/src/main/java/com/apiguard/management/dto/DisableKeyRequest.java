package com.apiguard.management.dto;

/**
 * Request DTO for disabling an API key.
 * Contains the reason for disabling the key.
 */
public record DisableKeyRequest(String reason) {
}
