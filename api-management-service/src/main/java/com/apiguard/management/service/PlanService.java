package com.apiguard.management.service;

import com.apiguard.management.dto.PlanRequest;
import com.apiguard.management.dto.PlanResponse;
import com.apiguard.management.entity.Plan;
import com.apiguard.management.entity.RegisteredApi;
import com.apiguard.management.repository.PlanRepository;
import com.apiguard.management.repository.RegisteredApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final RegisteredApiRepository apiRepository;

    @Transactional
    public PlanResponse createPlan(UUID apiId, PlanRequest request, String ownerEmail) {
        RegisteredApi api = apiRepository.findById(apiId)
                .orElseThrow(() -> new IllegalArgumentException("API not found: " + apiId));

        if (!api.getOwnerEmail().equals(ownerEmail)) {
            throw new IllegalArgumentException("You do not own this API");
        }

        Plan plan = Plan.builder()
                .name(request.getName())
                .rateLimitRpm(request.getRateLimitRpm())
                .monthlyQuota(request.getMonthlyQuota())
                .webhookEnabled(request.isWebhookEnabled())
                .ownerEmail(ownerEmail)
                .registeredApi(api)
                .build();

        Plan saved = planRepository.save(plan);
        return mapToResponse(saved);
    }

    public List<PlanResponse> getPlansForApi(UUID apiId, String ownerEmail) {
        return planRepository.findByRegisteredApiIdAndOwnerEmail(apiId, ownerEmail)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePlan(UUID planId, String ownerEmail) {
        Plan plan = planRepository.findByIdAndOwnerEmail(planId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or access denied"));
        
        planRepository.delete(plan);
    }

    private PlanResponse mapToResponse(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .rateLimitRpm(plan.getRateLimitRpm())
                .monthlyQuota(plan.getMonthlyQuota())
                .webhookEnabled(plan.isWebhookEnabled())
                .apiId(plan.getRegisteredApi().getId())
                .build();
    }
}
