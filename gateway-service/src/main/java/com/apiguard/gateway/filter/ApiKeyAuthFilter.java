package com.apiguard.gateway.filter;

import com.apiguard.common.dto.ApiConfigDTO;
import com.apiguard.gateway.service.ApiConfigCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter implements GlobalFilter, Ordered {

    // Exchange attribute keys — other filters can read these downstream
    public static final String ATTR_API_CONFIG = "apiConfig";
    public static final String ATTR_KEY_HASH   = "keyHash";

    private final ApiConfigCacheService cacheService;

    @Override
    public int getOrder() {
        return -2; // Runs first, before rate limiting
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String rawKey = exchange.getRequest().getHeaders().getFirst("X-Api-Key");

        if (rawKey == null || rawKey.isBlank()) {
            log.warn("Request rejected — missing X-Api-Key header: {}", exchange.getRequest().getPath());
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing X-Api-Key header");
        }

        String keyHash = hashKey(rawKey);

        return cacheService.getConfigByKeyHash(keyHash)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Request rejected — invalid API key. Hash: {}", keyHash);
                    return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid API key").cast(ApiConfigDTO.class);
                }))
                .flatMap(config -> {
                    if (!config.active()) {
                        String reason = config.disabledReason() != null 
                            ? config.disabledReason() 
                            : "Unknown";
                        log.warn("Request rejected — API key is disabled. Hash: {}, Reason: {}", keyHash, reason);
                        return reject(exchange, HttpStatus.FORBIDDEN, 
                            "API key is disabled. Reason: " + reason);
                    }

                    // Attach config to exchange so downstream filters can use it
                    exchange.getAttributes().put(ATTR_API_CONFIG, config);
                    exchange.getAttributes().put(ATTR_KEY_HASH, keyHash);

                    log.debug("API key validated for API: {}", config.name());
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        var buffer = exchange.getResponse().bufferFactory()
                .wrap(("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
