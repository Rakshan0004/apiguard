package com.apiguard.usage.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for webhook delivery history.
 * Contains information about a webhook delivery attempt.
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
) {
}
