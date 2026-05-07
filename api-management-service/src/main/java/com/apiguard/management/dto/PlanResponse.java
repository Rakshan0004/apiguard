package com.apiguard.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {
    private UUID id;
    private String name;
    private int rateLimitRpm;
    private long monthlyQuota;
    private boolean webhookEnabled;
    private UUID apiId;
}
