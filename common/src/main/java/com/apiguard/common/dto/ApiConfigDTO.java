package com.apiguard.common.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record ApiConfigDTO(
    String id,
    String name,
    String targetUrl,
    String proxyPath,
    boolean active,
    int rateLimitRpm,
    long monthlyQuota
) {}
