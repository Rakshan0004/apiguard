package com.apiguard.usage.repository;

import com.apiguard.usage.entity.WebhookHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for webhook delivery history.
 * Provides methods for deduplication checks, history retrieval, and monitoring.
 */
@Repository
public interface WebhookHistoryRepository extends JpaRepository<WebhookHistory, UUID> {

    /**
     * Check if a notification has already been sent for a specific API key, event type, and billing period.
     * Used for deduplication to prevent duplicate notifications.
     *
     * @param apiKeyId API key identifier
     * @param eventType Event type (quota.warning, quota.exceeded, quota.test)
     * @param yearMonth Billing period in YYYY-MM format
     * @return true if notification already exists
     */
    boolean existsByApiKeyIdAndEventTypeAndYearMonth(UUID apiKeyId, String eventType, String yearMonth);

    /**
     * Find webhook history for an API key, ordered by most recent first.
     *
     * @param apiKeyId API key identifier
     * @param pageable Pagination parameters
     * @return List of webhook history records
     */
    List<WebhookHistory> findByApiKeyIdOrderBySentAtDesc(UUID apiKeyId, Pageable pageable);

    /**
     * Find webhook deliveries by status after a specific timestamp.
     * Used for monitoring and alerting on failed deliveries.
     *
     * @param deliveryStatus Delivery status (SUCCESS, FAILED)
     * @param after Timestamp to search after
     * @return List of webhook history records
     */
    List<WebhookHistory> findByDeliveryStatusAndSentAtAfter(String deliveryStatus, Instant after);
}
