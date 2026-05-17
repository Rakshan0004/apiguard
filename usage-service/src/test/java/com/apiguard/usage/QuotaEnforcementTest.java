package com.apiguard.usage;

import com.apiguard.common.event.UsageEvent;
import com.apiguard.usage.client.ManagementServiceClient;
import com.apiguard.usage.config.QuotaEnforcementConfig;
import com.apiguard.usage.dto.ApiKeyDetailsDTO;
import com.apiguard.usage.entity.MonthlyUsageSummary;
import com.apiguard.usage.repository.MonthlyUsageSummaryRepository;
import com.apiguard.usage.repository.UsageLogRepository;
import com.apiguard.usage.service.QuotaEnforcementService;
import com.apiguard.usage.service.QuotaEnforcementServiceImpl;
import com.apiguard.usage.service.UsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for quota enforcement feature.
 */
@ExtendWith(MockitoExtension.class)
class QuotaEnforcementTest {

    @Mock
    private UsageLogRepository logRepository;

    @Mock
    private MonthlyUsageSummaryRepository summaryRepository;

    @Mock
    private ManagementServiceClient managementServiceClient;

    @Mock
    private com.apiguard.usage.service.WebhookTriggerService webhookTriggerService;

    private QuotaEnforcementService quotaEnforcementService;
    private UsageService usageService;
    private QuotaEnforcementConfig config;

    private String testApiKeyId;
    private String testApiId;
    private String currentYearMonth;

    @BeforeEach
    void setUp() {
        testApiKeyId = UUID.randomUUID().toString();
        testApiId = UUID.randomUUID().toString();
        currentYearMonth = YearMonth.now().toString();

        config = new QuotaEnforcementConfig();
        config.setEnabled(true);

        quotaEnforcementService = new QuotaEnforcementServiceImpl(managementServiceClient, webhookTriggerService);
        usageService = new UsageService(logRepository, summaryRepository, quotaEnforcementService, config);
    }

    @Test
    void shouldDisableKeyWhenQuotaExactlyReached() throws InterruptedException {
        // Given: API key with quota = 100
        long quota = 100L;
        when(managementServiceClient.getApiKeyDetails(testApiKeyId))
                .thenReturn(new ApiKeyDetailsDTO(
                        testApiKeyId,
                        "TEST_PLAN",
                        quota,
                        60,
                        true,
                        null,
                        null,
                        null
                ));
        when(managementServiceClient.disableKey(testApiKeyId))
                .thenReturn(CompletableFuture.completedFuture(null));

        MonthlyUsageSummary summary = new MonthlyUsageSummary();
        summary.setApiKeyId(testApiKeyId);
        summary.setYearMonth(currentYearMonth);
        summary.setTotalRequests(100L);
        summary.setSuccessfulRequests(100L);

        when(summaryRepository.findByApiKeyIdAndYearMonth(testApiKeyId, currentYearMonth))
                .thenReturn(Optional.of(summary));

        // When: Process usage event that reaches quota
        UsageEvent event = new UsageEvent(
                UUID.randomUUID().toString(),
                testApiKeyId,
                testApiId,
                "GET",
                "/test",
                200,
                50L,
                Instant.now()
        );
        usageService.processUsageEvent(event);

        // Give async operations time to complete
        Thread.sleep(500);

        // Then: Key should be disabled
        verify(managementServiceClient, times(1)).disableKey(testApiKeyId);
    }

    @Test
    void shouldNotDisableKeyWhenUsageBelowQuota() throws InterruptedException {
        // Given: API key with quota = 100
        long quota = 100L;
        when(managementServiceClient.getApiKeyDetails(testApiKeyId))
                .thenReturn(new ApiKeyDetailsDTO(
                        testApiKeyId,
                        "TEST_PLAN",
                        quota,
                        60,
                        true,
                        null,
                        null,
                        null
                ));

        MonthlyUsageSummary summary = new MonthlyUsageSummary();
        summary.setApiKeyId(testApiKeyId);
        summary.setYearMonth(currentYearMonth);
        summary.setTotalRequests(50L);
        summary.setSuccessfulRequests(50L);

        when(summaryRepository.findByApiKeyIdAndYearMonth(testApiKeyId, currentYearMonth))
                .thenReturn(Optional.of(summary));

        // When: Process usage event below quota
        UsageEvent event = new UsageEvent(
                UUID.randomUUID().toString(),
                testApiKeyId,
                testApiId,
                "GET",
                "/test",
                200,
                50L,
                Instant.now()
        );
        usageService.processUsageEvent(event);

        // Give async operations time to complete
        Thread.sleep(500);

        // Then: Key should NOT be disabled
        verify(managementServiceClient, never()).disableKey(anyString());
    }

    @Test
    void shouldNotDisableKeyWithUnlimitedQuota() throws InterruptedException {
        // Given: API key with unlimited quota (-1)
        when(managementServiceClient.getApiKeyDetails(testApiKeyId))
                .thenReturn(new ApiKeyDetailsDTO(
                        testApiKeyId,
                        "UNLIMITED_PLAN",
                        -1L,
                        60,
                        true,
                        null,
                        null,
                        null
                ));

        MonthlyUsageSummary summary = new MonthlyUsageSummary();
        summary.setApiKeyId(testApiKeyId);
        summary.setYearMonth(currentYearMonth);
        summary.setTotalRequests(1000L);
        summary.setSuccessfulRequests(1000L);

        when(summaryRepository.findByApiKeyIdAndYearMonth(testApiKeyId, currentYearMonth))
                .thenReturn(Optional.of(summary));

        // When: Process usage event
        UsageEvent event = new UsageEvent(
                UUID.randomUUID().toString(),
                testApiKeyId,
                testApiId,
                "GET",
                "/test",
                200,
                50L,
                Instant.now()
        );
        usageService.processUsageEvent(event);

        // Give async operations time to complete
        Thread.sleep(500);

        // Then: Key should NOT be disabled
        verify(managementServiceClient, never()).disableKey(anyString());
    }

    @Test
    void shouldNotDisableKeyWhenEnforcementDisabled() {
        // Given: Quota enforcement is disabled
        config.setEnabled(false);

        // When: Process usage event
        UsageEvent event = new UsageEvent(
                UUID.randomUUID().toString(),
                testApiKeyId,
                testApiId,
                "GET",
                "/test",
                200,
                50L,
                Instant.now()
        );
        usageService.processUsageEvent(event);

        // Then: Key should NOT be disabled (enforcement is disabled)
        verifyNoInteractions(managementServiceClient);
    }

    @Test
    void shouldHandleManagementServiceFailureGracefully() throws InterruptedException {
        // Given: Management Service is unavailable
        when(managementServiceClient.getApiKeyDetails(testApiKeyId))
                .thenThrow(new RuntimeException("Service unavailable"));

        MonthlyUsageSummary summary = new MonthlyUsageSummary();
        summary.setApiKeyId(testApiKeyId);
        summary.setYearMonth(currentYearMonth);
        summary.setTotalRequests(100L);
        summary.setSuccessfulRequests(100L);

        when(summaryRepository.findByApiKeyIdAndYearMonth(testApiKeyId, currentYearMonth))
                .thenReturn(Optional.of(summary));

        // When: Process usage event
        UsageEvent event = new UsageEvent(
                UUID.randomUUID().toString(),
                testApiKeyId,
                testApiId,
                "GET",
                "/test",
                200,
                50L,
                Instant.now()
        );

        // Then: Should not throw exception (graceful degradation)
        usageService.processUsageEvent(event);

        // Verify usage was still logged
        verify(logRepository, times(1)).save(any());
        verify(summaryRepository, times(1)).upsertUsage(anyString(), anyString(), anyInt());
    }
}
