package com.apiguard.usage.service;

import com.apiguard.common.constant.RabbitConstants;
import com.apiguard.common.event.UsageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UsageEventConsumer {

    private final UsageService usageService;

    @RabbitListener(queues = RabbitConstants.USAGE_QUEUE)
    public void consumeUsageEvent(UsageEvent event) {
        log.info("Received usage event: eventId={}, apiKeyId={}, status={}", 
                event.eventId(), event.apiKeyId(), event.responseStatus());
        
        try {
            usageService.processUsageEvent(event);
        } catch (Exception e) {
            log.error("Error processing usage event: {}", event.eventId(), e);
            throw e; // RabbitMQ will handle retry/DLQ based on config
        }
    }
}
