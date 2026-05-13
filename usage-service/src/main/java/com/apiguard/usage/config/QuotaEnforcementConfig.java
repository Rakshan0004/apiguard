package com.apiguard.usage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for quota enforcement feature.
 */
@Configuration
@ConfigurationProperties(prefix = "quota.enforcement")
@Data
public class QuotaEnforcementConfig {

    /**
     * Whether quota enforcement is enabled.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Base URL of the Management Service for internal API calls.
     * Default: http://localhost:8081
     */
    private String managementServiceUrl = "http://localhost:8081";

    /**
     * Timeout in milliseconds for REST API calls.
     * Default: 5000ms (5 seconds)
     */
    private int timeoutMs = 5000;

    /**
     * Connection pool size for WebClient.
     * Default: 10
     */
    private int connectionPoolSize = 10;
}
