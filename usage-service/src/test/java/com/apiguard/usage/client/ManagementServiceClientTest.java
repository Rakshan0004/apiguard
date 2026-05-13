package com.apiguard.usage.client;

import com.apiguard.usage.config.QuotaEnforcementConfig;
import com.apiguard.usage.dto.ApiKeyDetailsDTO;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ManagementServiceClient.
 */
class ManagementServiceClientTest {

    private MockWebServer mockWebServer;
    private ManagementServiceClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        QuotaEnforcementConfig config = new QuotaEnforcementConfig();
        config.setManagementServiceUrl(mockWebServer.url("/").toString());
        config.setTimeoutMs(5000);

        client = new ManagementServiceClientImpl(config, WebClient.builder());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldDisableKeySuccessfully() throws ExecutionException, InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"message\":\"Key disabled\",\"wasAlreadyDisabled\":false}")
                .addHeader("Content-Type", "application/json"));

        // When
        CompletableFuture<Void> future = client.disableKey("test-key-id");
        future.get(); // Wait for completion

        // Then
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        assertThat(mockWebServer.takeRequest().getPath())
                .isEqualTo("/internal/keys/test-key-id/disable");
    }

    @Test
    void shouldHandleDisableKeyTimeout() throws InterruptedException {
        // Given: Server delays response beyond timeout
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBodyDelay(10, java.util.concurrent.TimeUnit.SECONDS));

        // When
        CompletableFuture<Void> future = client.disableKey("test-key-id");

        // Then: Should complete without throwing exception (errors are logged only)
        Thread.sleep(6000); // Wait for timeout
        assertThat(future).isCompleted();
    }

    @Test
    void shouldHandleDisableKey404() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"error\":\"Key not found\"}")
                .addHeader("Content-Type", "application/json"));

        // When
        CompletableFuture<Void> future = client.disableKey("non-existent-key");

        // Then: Should complete without throwing exception (errors are logged only)
        Thread.sleep(1000);
        assertThat(future).isCompleted();
    }

    @Test
    void shouldHandleDisableKey500Error() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\":\"Internal server error\"}")
                .addHeader("Content-Type", "application/json"));

        // When
        CompletableFuture<Void> future = client.disableKey("test-key-id");

        // Then: Should complete without throwing exception (errors are logged only)
        Thread.sleep(1000);
        assertThat(future).isCompleted();
    }

    @Test
    void shouldEnableKeySuccessfully() throws ExecutionException, InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"message\":\"Key enabled\",\"wasAlreadyEnabled\":false}")
                .addHeader("Content-Type", "application/json"));

        // When
        CompletableFuture<Void> future = client.enableKey("test-key-id");
        future.get(); // Wait for completion

        // Then
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        assertThat(mockWebServer.takeRequest().getPath())
                .isEqualTo("/internal/keys/test-key-id/enable");
    }

    @Test
    void shouldGetApiKeyDetailsSuccessfully() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "apiKeyId": "test-key-id",
                        "planName": "PRO",
                        "monthlyQuota": 50000,
                        "rateLimitRpm": 300,
                        "active": true,
                        "disabledReason": null
                    }
                    """)
                .addHeader("Content-Type", "application/json"));

        // When
        ApiKeyDetailsDTO details = client.getApiKeyDetails("test-key-id");

        // Then
        assertThat(details).isNotNull();
        assertThat(details.apiKeyId()).isEqualTo("test-key-id");
        assertThat(details.planName()).isEqualTo("PRO");
        assertThat(details.monthlyQuota()).isEqualTo(50000);
        assertThat(details.rateLimitRpm()).isEqualTo(300);
        assertThat(details.active()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenGetApiKeyDetailsFails() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"error\":\"Key not found\"}")
                .addHeader("Content-Type", "application/json"));

        // When/Then
        assertThatThrownBy(() -> client.getApiKeyDetails("non-existent-key"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to retrieve API key details");
    }

    @Test
    void shouldGetQuotaDisabledKeysSuccessfully() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[\"key-1\", \"key-2\", \"key-3\"]")
                .addHeader("Content-Type", "application/json"));

        // When
        List<String> keys = client.getQuotaDisabledKeys();

        // Then
        assertThat(keys).hasSize(3);
        assertThat(keys).containsExactly("key-1", "key-2", "key-3");
    }

    @Test
    void shouldReturnEmptyListWhenGetQuotaDisabledKeysFails() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\":\"Internal server error\"}")
                .addHeader("Content-Type", "application/json"));

        // When
        List<String> keys = client.getQuotaDisabledKeys();

        // Then
        assertThat(keys).isEmpty();
    }
}
