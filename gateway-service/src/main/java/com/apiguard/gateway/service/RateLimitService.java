package com.apiguard.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final org.springframework.data.redis.core.ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<List> rateLimitScript;

    public record RateLimitResult(boolean allowed, long remaining, long resetTimeMs) {}

    /**
     * Check rate limit using sliding window Lua script.
     * @param key The Redis key for the user/API combination
     * @param limit The max requests allowed per minute
     * @return Mono of RateLimitResult
     */
    public Mono<RateLimitResult> checkLimit(String key, int limit) {
        String redisKey = "apiguard:ratelimit:" + key;
        long now = Instant.now().toEpochMilli();
        long windowMs = 60000; // 1 minute window

        return redisTemplate.execute(
                rateLimitScript,
                List.of(redisKey),
                List.of(String.valueOf(now), String.valueOf(windowMs), String.valueOf(limit))
        ).next().map(result -> {
            if (result == null || result.size() < 3) {
                log.error("Unexpected result from rate limit Lua script: {}", result);
                return new RateLimitResult(true, 0, 0); // Fail open if script fails
            }
            // Use Number to be safe with Integer vs Long from Redis
            boolean allowed = ((Number) result.get(0)).longValue() == 1L;
            long remaining = ((Number) result.get(1)).longValue();
            long resetTimeMs = ((Number) result.get(2)).longValue();

            return new RateLimitResult(allowed, remaining, resetTimeMs);
        }).doOnError(e -> log.error("Error executing rate limit Lua script: {}", e.getMessage()));
    }
}
