package com.apiguard.gateway;

import com.apiguard.common.dto.ApiConfigDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "management.service.url=http://127.0.0.1:8089",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@AutoConfigureWireMock(port = 8089)
public class GatewayIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private ReactiveRedisTemplate<String, ApiConfigDTO> redisTemplate;

    @Test
    void testForwardingAndSecurity() {
        // Key: "test-key", SHA-256: 7d6394336183377227d8905b7efb99e710d0d80c0615569503437171e89f8166
        String testKey = "test-key";
        String keyHash = "7d6394336183377227d8905b7efb99e710d0d80c0615569503437171e89f8166";

        stubFor(get(urlEqualTo("/internal/configs/" + keyHash))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"1\", \"name\":\"Test\", \"proxyPath\":\"test\", \"targetUrl\":\"http://127.0.0.1:8089/origin\", \"active\":true, \"rateLimitRpm\":100, \"monthlyQuota\":1000}")));

        stubFor(get(urlEqualTo("/origin/hello"))
            .willReturn(aResponse().withStatus(200).withBody("OK")));

        webClient.get()
            .uri("/proxy/test/hello")
            .header("X-Api-Key", testKey)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).isEqualTo("OK");
    }
}
