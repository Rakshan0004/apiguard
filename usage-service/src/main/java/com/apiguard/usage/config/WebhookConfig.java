package com.apiguard.usage.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Configuration for webhook delivery.
 * Configures RestTemplate with timeout and async executor for webhook delivery.
 */
@Configuration
@EnableAsync
public class WebhookConfig {

    private static final int WEBHOOK_TIMEOUT_MS = 10000; // 10 seconds
    private static final int ASYNC_CORE_POOL_SIZE = 5;
    private static final int ASYNC_MAX_POOL_SIZE = 10;
    private static final int ASYNC_QUEUE_CAPACITY = 100;

    /**
     * RestTemplate bean for webhook HTTP requests.
     * Configured with 10-second timeout.
     */
    @Bean(name = "webhookRestTemplate")
    public RestTemplate webhookRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofMillis(WEBHOOK_TIMEOUT_MS))
            .setReadTimeout(Duration.ofMillis(WEBHOOK_TIMEOUT_MS))
            .build();
    }

    /**
     * Async executor for webhook delivery.
     * Allows webhook delivery to run asynchronously without blocking main request flow.
     */
    @Bean(name = "webhookAsyncExecutor")
    public Executor webhookAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(ASYNC_CORE_POOL_SIZE);
        executor.setMaxPoolSize(ASYNC_MAX_POOL_SIZE);
        executor.setQueueCapacity(ASYNC_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("webhook-async-");
        executor.initialize();
        return executor;
    }
}
