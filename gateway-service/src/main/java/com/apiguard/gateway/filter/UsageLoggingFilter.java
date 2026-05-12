package com.apiguard.gateway.filter;

import com.apiguard.common.dto.ApiConfigDTO;
import com.apiguard.common.event.UsageEvent;
import com.apiguard.gateway.service.UsageEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsageLoggingFilter implements GlobalFilter, Ordered {

    private final UsageEventPublisher usageEventPublisher;

    @Override
    public int getOrder() {
        // High order to run after the proxying logic (Post filter)
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ApiConfigDTO config = exchange.getAttribute(ApiKeyAuthFilter.ATTR_API_CONFIG);
            if (config != null) {
                long latency = System.currentTimeMillis() - startTime;
                int statusCode = exchange.getResponse().getStatusCode() != null ?
                        exchange.getResponse().getStatusCode().value() : 0;

                UsageEvent event = UsageEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .apiKeyId(config.apiKeyId()) // We might need to ensure this is in the DTO or use another ID
                        .registeredApiId(config.apiId())
                        .method(exchange.getRequest().getMethod().name())
                        .path(exchange.getRequest().getPath().value())
                        .responseStatus(statusCode)
                        .latencyMs(latency)
                        .timestamp(Instant.now())
                        .build();

                usageEventPublisher.publishEvent(event);
            }
        }));
    }
}
