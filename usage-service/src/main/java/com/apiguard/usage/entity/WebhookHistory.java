package com.apiguard.usage.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a webhook delivery attempt.
 * Tracks all webhook notifications sent to API owners with delivery status and error details.
 */
@Entity
@Table(name = "webhook_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class WebhookHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "api_key_id", nullable = false)
    private UUID apiKeyId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "threshold_percentage", nullable = false)
    private int thresholdPercentage;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "usage_count", nullable = false)
    private long usageCount;

    @Column(name = "quota_limit", nullable = false)
    private long quotaLimit;

    @Column(name = "usage_percentage", nullable = false)
    private double usagePercentage;

    @Column(name = "webhook_url", nullable = false, length = 2048)
    private String webhookUrl;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "delivery_status", nullable = false, length = 20)
    private String deliveryStatus; // SUCCESS, FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
