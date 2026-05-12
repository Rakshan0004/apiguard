package com.apiguard.usage.scheduler;

import com.apiguard.usage.client.ManagementServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled job that runs on the first day of each month to re-enable quota-disabled keys.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyResetScheduler {

    private final ManagementServiceClient managementServiceClient;

    /**
     * Scheduled job that runs at 00:00 UTC on the 1st of each month.
     * Queries all keys disabled due to QUOTA_EXCEEDED and re-enables them.
     */
    @Scheduled(cron = "0 0 0 1 * ?", zone = "UTC")
    public void resetMonthlyQuotas() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        
        try {
            log.info("Starting monthly quota reset job at {}", Instant.now());
            
            // Get all keys disabled due to quota exceeded
            List<String> disabledKeys = managementServiceClient.getQuotaDisabledKeys();
            
            if (disabledKeys.isEmpty()) {
                log.info("No quota-disabled keys found. Monthly reset job completed.");
                return;
            }
            
            log.info("Found {} quota-disabled keys to re-enable", disabledKeys.size());
            
            // Track success and failure counts
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            
            // Re-enable each key
            List<CompletableFuture<Void>> futures = disabledKeys.stream()
                    .map(keyId -> managementServiceClient.enableKey(keyId)
                            .thenAccept(v -> {
                                successCount.incrementAndGet();
                                log.debug("Successfully re-enabled key: {}", keyId);
                            })
                            .exceptionally(e -> {
                                failureCount.incrementAndGet();
                                log.error("Failed to re-enable key: {}, error: {}", keyId, e.getMessage());
                                return null;
                            }))
                    .toList();
            
            // Wait for all operations to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // Log summary
            log.info("Monthly quota reset job completed. Total keys: {}, Successful: {}, Failed: {}",
                    disabledKeys.size(), successCount.get(), failureCount.get());
            
        } catch (Exception e) {
            log.error("Error during monthly quota reset job: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
}
