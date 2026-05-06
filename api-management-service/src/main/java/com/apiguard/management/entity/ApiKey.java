package com.apiguard.management.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false)
    private String keyPrefix; // First 8 chars for display

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_api_id", nullable = false)
    private RegisteredApi registeredApi;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "disabled_reason")
    private String disabledReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
