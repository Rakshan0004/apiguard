package com.apiguard.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRequest {
    private String name;
    private int rateLimitRpm;
    private long monthlyQuota;
    private boolean webhookEnabled;
}
