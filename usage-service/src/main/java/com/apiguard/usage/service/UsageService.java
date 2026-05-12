package com.apiguard.usage.service;

import com.apiguard.common.event.UsageEvent;
import com.apiguard.usage.entity.UsageLog;
import com.apiguard.usage.repository.MonthlyUsageSummaryRepository;
import com.apiguard.usage.repository.UsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageService {

    private final UsageLogRepository logRepository;
    private final MonthlyUsageSummaryRepository summaryRepository;
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Transactional
    public void processUsageEvent(UsageEvent event) {
        LocalDateTime ldt = LocalDateTime.ofInstant(event.timestamp(), ZoneId.systemDefault());
        
        // 1. Persist the raw log
        UsageLog logEntry = UsageLog.builder()
                .id(event.eventId())
                .apiKeyId(event.apiKeyId())
                .apiId(event.registeredApiId())
                .method(event.method())
                .path(event.path())
                .status(event.responseStatus())
                .latencyMs(event.latencyMs())
                .timestamp(ldt)
                .build();
        logRepository.save(logEntry);

        // 2. Update monthly summary
        String yearMonth = ldt.format(YEAR_MONTH_FORMATTER);
        int successIncrement = (event.responseStatus() >= 200 && event.responseStatus() < 300) ? 1 : 0;
        
        summaryRepository.upsertUsage(event.apiKeyId(), yearMonth, successIncrement);
        
        log.debug("Processed usage event for key: {} in period: {}", event.apiKeyId(), yearMonth);
    }
}
