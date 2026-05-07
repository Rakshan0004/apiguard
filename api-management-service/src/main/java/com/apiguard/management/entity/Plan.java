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

    @Column(nullable = false)
    private String name; // e.g. "Basic", "Gold", "Enterprise"

    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id", nullable = false)
    private RegisteredApi registeredApi;

    @Column(name = "rate_limit_rpm", nullable = false)
    private int rateLimitRpm;

    @Column(name = "monthly_quota", nullable = false)
    private long monthlyQuota;

    @Column(name = "webhook_enabled", nullable = false)
    @Builder.Default
    private boolean webhookEnabled = false;
}
