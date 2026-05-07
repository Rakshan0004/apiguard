package com.apiguard.management.controller;

import com.apiguard.management.dto.PlanRequest;
import com.apiguard.management.dto.PlanResponse;
import com.apiguard.management.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/apis/{apiId}/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService service;

    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(
            @PathVariable UUID apiId,
            @RequestBody PlanRequest request,
            Authentication authentication) {
        String ownerEmail = authentication.getName();
        return ResponseEntity.ok(service.createPlan(apiId, request, ownerEmail));
    }

    @GetMapping
    public ResponseEntity<List<PlanResponse>> getPlans(
            @PathVariable UUID apiId,
            Authentication authentication) {
        String ownerEmail = authentication.getName();
        return ResponseEntity.ok(service.getPlansForApi(apiId, ownerEmail));
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deletePlan(
            @PathVariable UUID apiId,
            @PathVariable UUID planId,
            Authentication authentication) {
        String ownerEmail = authentication.getName();
        service.deletePlan(planId, ownerEmail);
        return ResponseEntity.noContent().build();
    }
}
