package com.apiguard.usage.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "monthly_usage_summaries", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"api_key_id", "year_month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyUsageSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key_id", nullable = false)
    private String apiKeyId;

    @Column(name = "year_month", nullable = false)
    private String yearMonth; // Format: YYYY-MM

    @Column(name = "total_requests", nullable = false)
    private Long totalRequests;

    @Column(name = "successful_requests", nullable = false)
    private Long successfulRequests; // 2xx status
}
