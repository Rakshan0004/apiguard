package com.apiguard.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class RouteLocatorConfig {

    @Value("${management.service.url:http://localhost:8081}")
    private String managementServiceUrl;

    /**
     * Sprint 6: Builds dynamic routes from the Management Service at startup.
     *
     * Each RegisteredApi with proxyPath = "weather-api" and targetUrl = "http://api.weather.com"
     * becomes a route:
     *   /proxy/weather-api/** → http://api.weather.com/**
     */
    @Bean
    public RouteLocator dynamicRouteLocator(RouteLocatorBuilder builder, WebClient managementWebClient) {
        List<Map<String, Object>> routes = fetchRoutes(managementWebClient);

        RouteLocatorBuilder.Builder routesBuilder = builder.routes();

        if (routes.isEmpty()) {
            log.warn("No routes fetched from Management Service. Gateway has no routes configured.");
        }

        for (Map<String, Object> route : routes) {
            String proxyPath = (String) route.get("proxyPath");
            String targetUrl = (String) route.get("targetUrl");
            String routeId = "route-" + proxyPath;

            log.info("Registering route: /proxy/{}/** → {}", proxyPath, targetUrl);

            routesBuilder = routesBuilder.route(routeId, r -> r
                    .path("/proxy/" + proxyPath + "/**")
                    .filters(f -> f.rewritePath(
                            "/proxy/" + proxyPath + "/?(?<segment>.*)",
                            "/${segment}"
                    ))
                    .uri(targetUrl)
            );
        }

        return routesBuilder.build();
    }

    private List<Map<String, Object>> fetchRoutes(WebClient managementWebClient) {
        try {
            org.springframework.core.ParameterizedTypeReference<Map<String, Object>> typeRef = new org.springframework.core.ParameterizedTypeReference<>() {};
            List<Map<String, Object>> result = managementWebClient.get()
                    .uri("/internal/routes")
                    .retrieve()
                    .bodyToFlux(typeRef)
                    .collectList()
                    .block();
            return result != null ? result : List.of();
        } catch (Exception e) {
            log.error("Could not fetch routes from Management Service at startup: {}", e.getMessage());
            return List.of();
        }
    }
}
