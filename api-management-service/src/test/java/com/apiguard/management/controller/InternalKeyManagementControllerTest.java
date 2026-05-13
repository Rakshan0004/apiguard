package com.apiguard.management.controller;

import com.apiguard.management.dto.ApiKeyDetailsDTO;
import com.apiguard.management.dto.DisableKeyRequest;
import com.apiguard.management.dto.DisableKeyResponse;
import com.apiguard.management.dto.EnableKeyResponse;
import com.apiguard.management.entity.ApiKey;
import com.apiguard.management.entity.Plan;
import com.apiguard.management.entity.RegisteredApi;
import com.apiguard.management.repository.ApiKeyRepository;
import com.apiguard.management.repository.PlanRepository;
import com.apiguard.management.repository.RegisteredApiRepository;
import com.apiguard.management.service.ApiKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for InternalKeyManagementController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class InternalKeyManagementControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private RegisteredApiRepository registeredApiRepository;

    @Autowired
    private PlanRepository planRepository;

    private ApiKey testApiKey;

    @BeforeEach
    void setUp() {
        apiKeyRepository.deleteAll();
        registeredApiRepository.deleteAll();
        planRepository.deleteAll();

        // Create test data
        RegisteredApi api = RegisteredApi.builder()
                .name("Test API")
                .targetUrl("http://localhost:9999")
                .proxyPath("/test")
                .active(true)
                .build();
        api = registeredApiRepository.save(api);

        Plan plan = Plan.builder()
                .name("TEST_PLAN")
                .rateLimitRpm(60)
                .monthlyQuota(1000L)
                .registeredApi(api)
                .build();
        plan = planRepository.save(plan);

        testApiKey = ApiKey.builder()
                .keyHash("test-hash-" + UUID.randomUUID())
                .keyPrefix("test1234")
                .registeredApi(api)
                .plan(plan)
                .active(true)
                .build();
        testApiKey = apiKeyRepository.save(testApiKey);
    }

    @Test
    @WithMockUser
    void shouldDisableKeySuccessfully() throws Exception {
        // Given
        DisableKeyRequest request = new DisableKeyRequest("QUOTA_EXCEEDED");

        // When/Then
        mockMvc.perform(post("/internal/keys/{keyId}/disable", testApiKey.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Key disabled successfully"))
                .andExpect(jsonPath("$.wasAlreadyDisabled").value(false));

        // Verify database state
        ApiKey updatedKey = apiKeyRepository.findById(testApiKey.getId()).orElseThrow();
        assertThat(updatedKey.isActive()).isFalse();
        assertThat(updatedKey.getDisabledReason()).isEqualTo("QUOTA_EXCEEDED");
    }

    @Test
    @WithMockUser
    void shouldReturnIdempotentResponseWhenDisablingAlreadyDisabledKey() throws Exception {
        // Given: Key is already disabled
        testApiKey.setActive(false);
        testApiKey.setDisabledReason("QUOTA_EXCEEDED");
        apiKeyRepository.save(testApiKey);

        DisableKeyRequest request = new DisableKeyRequest("QUOTA_EXCEEDED");

        // When/Then
        mockMvc.perform(post("/internal/keys/{keyId}/disable", testApiKey.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Key was already disabled"))
                .andExpect(jsonPath("$.wasAlreadyDisabled").value(true));
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenDisablingNonExistentKey() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        DisableKeyRequest request = new DisableKeyRequest("QUOTA_EXCEEDED");

        // When/Then
        mockMvc.perform(post("/internal/keys/{keyId}/disable", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldEnableKeySuccessfully() throws Exception {
        // Given: Key is disabled
        testApiKey.setActive(false);
        testApiKey.setDisabledReason("QUOTA_EXCEEDED");
        apiKeyRepository.save(testApiKey);

        // When/Then
        mockMvc.perform(post("/internal/keys/{keyId}/enable", testApiKey.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Key enabled successfully"))
                .andExpect(jsonPath("$.wasAlreadyEnabled").value(false));

        // Verify database state
        ApiKey updatedKey = apiKeyRepository.findById(testApiKey.getId()).orElseThrow();
        assertThat(updatedKey.isActive()).isTrue();
        assertThat(updatedKey.getDisabledReason()).isNull();
    }

    @Test
    @WithMockUser
    void shouldReturnIdempotentResponseWhenEnablingAlreadyEnabledKey() throws Exception {
        // Given: Key is already enabled
        assertThat(testApiKey.isActive()).isTrue();

        // When/Then
        mockMvc.perform(post("/internal/keys/{keyId}/enable", testApiKey.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Key was already enabled"))
                .andExpect(jsonPath("$.wasAlreadyEnabled").value(true));
    }

    @Test
    @WithMockUser
    void shouldGetApiKeyDetailsSuccessfully() throws Exception {
        // When/Then
        mockMvc.perform(get("/internal/keys/{keyId}", testApiKey.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKeyId").value(testApiKey.getId().toString()))
                .andExpect(jsonPath("$.planName").value("TEST_PLAN"))
                .andExpect(jsonPath("$.monthlyQuota").value(1000))
                .andExpect(jsonPath("$.rateLimitRpm").value(60))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.disabledReason").isEmpty());
    }

    @Test
    @WithMockUser
    void shouldGetKeysByDisabledReason() throws Exception {
        // Given: Create multiple disabled keys
        ApiKey disabledKey1 = ApiKey.builder()
                .keyHash("disabled-hash-1")
                .keyPrefix("dis11234")
                .registeredApi(testApiKey.getRegisteredApi())
                .plan(testApiKey.getPlan())
                .active(false)
                .disabledReason("QUOTA_EXCEEDED")
                .build();
        apiKeyRepository.save(disabledKey1);

        ApiKey disabledKey2 = ApiKey.builder()
                .keyHash("disabled-hash-2")
                .keyPrefix("dis21234")
                .registeredApi(testApiKey.getRegisteredApi())
                .plan(testApiKey.getPlan())
                .active(false)
                .disabledReason("QUOTA_EXCEEDED")
                .build();
        apiKeyRepository.save(disabledKey2);

        // When/Then
        mockMvc.perform(get("/internal/keys")
                        .param("disabledReason", "QUOTA_EXCEEDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyListWhenNoKeysMatchDisabledReason() throws Exception {
        // When/Then
        mockMvc.perform(get("/internal/keys")
                        .param("disabledReason", "QUOTA_EXCEEDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
