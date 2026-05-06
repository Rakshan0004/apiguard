package com.apiguard.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // FREE, BASIC, PRO, ENTERPRISE

    @Column(name = "rate_limit_rpm", nullable = false)
    private int rateLimitRpm;

    @Column(name = "monthly_quota", nullable = false)
    private long monthlyQuota;

    @Column(name = "webhook_enabled", nullable = false)
    @Builder.Default
    private boolean webhookEnabled = false;
}
