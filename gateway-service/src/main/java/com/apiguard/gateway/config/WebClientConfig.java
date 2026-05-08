package com.apiguard.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${management.service.url:http://localhost:8081}")
    private String managementServiceUrl;

    @Bean
    public WebClient managementWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(managementServiceUrl)
                .build();
    }
}
