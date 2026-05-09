package com.apiguard.gateway.service;

import com.apiguard.common.dto.ApiConfigDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiConfigCacheService {

    private static final String CACHE_PREFIX = "apiguard:apikey:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final WebClient managementWebClient;
    private final ReactiveRedisTemplate<String, ApiConfigDTO> redisTemplate;

    /**
     * Sprint 7: Look up API config for a given raw API key.
     * Flow: Redis (fast cache) → Management Service (fallback) → cache result in Redis
     */
    public Mono<ApiConfigDTO> getConfigByKeyHash(String keyHash) {
        String redisKey = CACHE_PREFIX + keyHash;

        return redisTemplate.opsForValue().get(redisKey)
                .doOnNext(cached -> log.debug("Cache HIT for key hash: {}", keyHash))
                .switchIfEmpty(
                    fetchFromManagementService(keyHash)
                        .flatMap(config -> redisTemplate.opsForValue()
                                .set(redisKey, config, CACHE_TTL)
                                .thenReturn(config))
                        .doOnNext(fetched -> log.debug("Cache MISS — fetched from Management Service for: {}", keyHash))
                );
    }

    private Mono<ApiConfigDTO> fetchFromManagementService(String keyHash) {
        return managementWebClient.get()
                .uri("/internal/configs/{keyHash}", keyHash)
                .retrieve()
                .bodyToMono(ApiConfigDTO.class)
                .onErrorResume(e -> {
                    log.error("Failed to fetch config from Management Service for keyHash={}: {}", keyHash, e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Evict a key from cache (e.g. when it is revoked or disabled)
     */
    public Mono<Boolean> evict(String keyHash) {
        return redisTemplate.delete(CACHE_PREFIX + keyHash).map(count -> count > 0);
    }
}
