package com.apiguard.management.service;

import com.apiguard.management.dto.PlanRequest;
import com.apiguard.management.dto.PlanResponse;
import com.apiguard.management.entity.Plan;
import com.apiguard.management.entity.RegisteredApi;
import com.apiguard.management.repository.PlanRepository;
import com.apiguard.management.repository.RegisteredApiRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private RegisteredApiRepository apiRepository;

    @InjectMocks
    private PlanService planService;

    @Test
    void createPlan_ShouldReturnPlanResponse_WhenSuccessful() {
        // Arrange
        UUID apiId = UUID.randomUUID();
        String ownerEmail = "test@example.com";
        PlanRequest request = new PlanRequest();
        request.setName("Basic Plan");
        request.setRateLimitRpm(100);
        request.setMonthlyQuota(1000L);
        request.setWebhookEnabled(false);

        RegisteredApi api = RegisteredApi.builder()
                .id(apiId)
                .ownerEmail(ownerEmail)
                .build();

        Plan savedPlan = Plan.builder()
                .id(UUID.randomUUID())
                .name("Basic Plan")
                .rateLimitRpm(100)
                .monthlyQuota(1000L)
                .webhookEnabled(false)
                .ownerEmail(ownerEmail)
                .registeredApi(api)
                .build();

        when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));
        when(planRepository.save(any(Plan.class))).thenReturn(savedPlan);

        // Act
        PlanResponse response = planService.createPlan(apiId, request, ownerEmail);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Basic Plan");
        assertThat(response.getRateLimitRpm()).isEqualTo(100);
        assertThat(response.getMonthlyQuota()).isEqualTo(1000L);
        assertThat(response.getApiId()).isEqualTo(apiId);
        
        verify(apiRepository, times(1)).findById(apiId);
        verify(planRepository, times(1)).save(any(Plan.class));
    }

    @Test
    void createPlan_ShouldThrowException_WhenApiNotFound() {
        // Arrange
        UUID apiId = UUID.randomUUID();
        String ownerEmail = "test@example.com";
        PlanRequest request = new PlanRequest();

        when(apiRepository.findById(apiId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> planService.createPlan(apiId, request, ownerEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API not found");

        verify(apiRepository, times(1)).findById(apiId);
        verify(planRepository, never()).save(any(Plan.class));
    }

    @Test
    void createPlan_ShouldThrowException_WhenOwnerMismatch() {
        // Arrange
        UUID apiId = UUID.randomUUID();
        String ownerEmail = "test@example.com";
        String differentEmail = "other@example.com";
        PlanRequest request = new PlanRequest();

        RegisteredApi api = RegisteredApi.builder()
                .id(apiId)
                .ownerEmail(differentEmail)
                .build();

        when(apiRepository.findById(apiId)).thenReturn(Optional.of(api));

        // Act & Assert
        assertThatThrownBy(() -> planService.createPlan(apiId, request, ownerEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You do not own this API");

        verify(apiRepository, times(1)).findById(apiId);
        verify(planRepository, never()).save(any(Plan.class));
    }

    @Test
    void getPlansForApi_ShouldReturnListOfPlans() {
        // Arrange
        UUID apiId = UUID.randomUUID();
        String ownerEmail = "test@example.com";

        RegisteredApi api = RegisteredApi.builder().id(apiId).build();
        Plan plan = Plan.builder()
                .id(UUID.randomUUID())
                .name("Pro Plan")
                .registeredApi(api)
                .build();

        when(planRepository.findByRegisteredApiIdAndOwnerEmail(apiId, ownerEmail))
                .thenReturn(List.of(plan));

        // Act
        List<PlanResponse> responses = planService.getPlansForApi(apiId, ownerEmail);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Pro Plan");
        verify(planRepository, times(1)).findByRegisteredApiIdAndOwnerEmail(apiId, ownerEmail);
    }

    @Test
    void deletePlan_ShouldDelete_WhenPlanExistsAndOwnerMatches() {
        // Arrange
        UUID planId = UUID.randomUUID();
        String ownerEmail = "test@example.com";
        Plan plan = Plan.builder().id(planId).build();

        when(planRepository.findByIdAndOwnerEmail(planId, ownerEmail)).thenReturn(Optional.of(plan));

        // Act
        planService.deletePlan(planId, ownerEmail);

        // Assert
        verify(planRepository, times(1)).findByIdAndOwnerEmail(planId, ownerEmail);
        verify(planRepository, times(1)).delete(plan);
    }

    @Test
    void deletePlan_ShouldThrowException_WhenPlanNotFoundOrOwnerMismatch() {
        // Arrange
        UUID planId = UUID.randomUUID();
        String ownerEmail = "test@example.com";

        when(planRepository.findByIdAndOwnerEmail(planId, ownerEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> planService.deletePlan(planId, ownerEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plan not found or access denied");

        verify(planRepository, times(1)).findByIdAndOwnerEmail(planId, ownerEmail);
        verify(planRepository, never()).delete(any(Plan.class));
    }
}
