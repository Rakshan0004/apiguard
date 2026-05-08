package com.apiguard.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.apiguard.common.dto.ApiConfigDTO;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, ApiConfigDTO> apiConfigRedisTemplate(
            ReactiveRedisConnectionFactory factory,
            ObjectMapper objectMapper) {

        Jackson2JsonRedisSerializer<ApiConfigDTO> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, ApiConfigDTO.class);

        RedisSerializationContext<String, ApiConfigDTO> context =
                RedisSerializationContext.<String, ApiConfigDTO>newSerializationContext()
                        .key(StringRedisSerializer.UTF_8)
                        .value(valueSerializer)
                        .hashKey(StringRedisSerializer.UTF_8)
                        .hashValue(valueSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
