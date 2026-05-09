package com.apiguard.gateway.filter;

import com.apiguard.common.dto.ApiConfigDTO;
import com.apiguard.gateway.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimitService rateLimitService;

    @Override
    public int getOrder() {
        return -1; // Runs after ApiKeyAuthFilter (-2)
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ApiConfigDTO config = exchange.getAttribute(ApiKeyAuthFilter.ATTR_API_CONFIG);
        String keyHash = exchange.getAttribute(ApiKeyAuthFilter.ATTR_KEY_HASH);

        if (config == null || keyHash == null) {
            // Should have been handled by ApiKeyAuthFilter, but safety first
            return chain.filter(exchange);
        }

        // Use a unique key per API + API Key combination for rate limiting
        String limitKey = config.id() + ":" + keyHash;

        return rateLimitService.checkLimit(limitKey, config.rateLimitRpm())
                .flatMap(result -> {
                    // Add rate limit headers
                    exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(config.rateLimitRpm()));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(result.remaining()));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Reset", String.valueOf(result.resetTimeMs()));

                    if (result.allowed()) {
                        return chain.filter(exchange);
                    } else {
                        log.warn("Rate limit exceeded for API: {} and Key Hash: {}", config.name(), keyHash);
                        return reject(exchange, "Rate limit exceeded. Try again in " + (result.resetTimeMs() - System.currentTimeMillis()) + "ms");
                    }
                });
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        var buffer = exchange.getResponse().bufferFactory()
                .wrap(("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
