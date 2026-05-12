package com.apiguard.usage.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageLog {
    @Id
    private String id; // Matches Event ID

    @Column(nullable = false)
    private String apiKeyId;

    @Column(nullable = false)
    private String apiId;

    private String method;
    private String path;
    private int status;
    private long latencyMs;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
}
