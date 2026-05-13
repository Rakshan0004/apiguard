package com.apiguard.usage.repository;

import com.apiguard.usage.entity.MonthlyUsageSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlyUsageSummaryRepository extends JpaRepository<MonthlyUsageSummary, Long> {
    
    Optional<MonthlyUsageSummary> findByApiKeyIdAndYearMonth(String apiKeyId, String yearMonth);

    @Query(value = """
        SELECT * FROM monthly_usage_summaries
        WHERE api_key_id = :apiKeyId AND year_month = :yearMonth
        FOR UPDATE
        """, nativeQuery = true)
    Optional<MonthlyUsageSummary> findByApiKeyIdAndYearMonthForUpdate(String apiKeyId, String yearMonth);

    @Modifying
    @Query(value = """
        INSERT INTO monthly_usage_summaries (api_key_id, year_month, total_requests, successful_requests)
        VALUES (:apiKeyId, :yearMonth, 1, :successIncrement)
        ON CONFLICT (api_key_id, year_month) DO UPDATE SET
            total_requests = monthly_usage_summaries.total_requests + 1,
            successful_requests = monthly_usage_summaries.successful_requests + :successIncrement
        """, nativeQuery = true)
    void upsertUsage(String apiKeyId, String yearMonth, int successIncrement);
}
