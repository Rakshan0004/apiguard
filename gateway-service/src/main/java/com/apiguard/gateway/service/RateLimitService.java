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
            // Lua returns {allowed (0/1), remaining, resetTimeMs}
            boolean allowed = ((Long) result.get(0)) == 1L;
            long remaining = (Long) result.get(1);
            long resetTimeMs = (Long) result.get(2);

            return new RateLimitResult(allowed, remaining, resetTimeMs);
        }).doOnError(e -> log.error("Error executing rate limit Lua script: {}", e.getMessage()));
    }
}
