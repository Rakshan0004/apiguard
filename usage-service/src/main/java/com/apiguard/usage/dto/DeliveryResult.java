package com.apiguard.usage.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Result of a webhook delivery attempt.
 * Contains status, HTTP response code, retry count, and error details.
 */
@Data
@Builder
public class DeliveryResult {
    private boolean success;
    private Integer httpStatusCode;
    private int retryCount;
    private String errorMessage;
    
    /**
     * Get delivery status string for database storage.
     * @return "SUCCESS" or "FAILED"
     */
    public String getDeliveryStatus() {
        return success ? "SUCCESS" : "FAILED";
    }
}
