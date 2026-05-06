package com.apiguard.common.event;

import lombok.Builder;
import java.time.Instant;

@Builder
public record UsageEvent(
    String eventId,
    String apiKeyId,
    String registeredApiId,
    String method,
    String path,
    int responseStatus,
    long latencyMs,
    Instant timestamp
) {}
