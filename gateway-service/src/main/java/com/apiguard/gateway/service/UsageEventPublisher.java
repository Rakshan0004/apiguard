package com.apiguard.gateway.service;

import com.apiguard.common.constant.RabbitConstants;
import com.apiguard.common.event.UsageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishEvent(UsageEvent event) {
        Mono.fromRunnable(() -> {
            try {
                log.info("Publishing usage event for key: {}", event.apiKeyId());
                rabbitTemplate.convertAndSend(
                        RabbitConstants.USAGE_EXCHANGE,
                        RabbitConstants.USAGE_ROUTING_KEY,
                        event
                );
            } catch (Exception e) {
                log.error("Failed to publish usage event", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }
}
