package com.apiguard.management.controller;

import com.apiguard.common.dto.ApiConfigDTO;
import com.apiguard.management.entity.ApiKey;
import com.apiguard.management.repository.ApiKeyRepository;
import com.apiguard.management.repository.RegisteredApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalConfigController {

    private final ApiKeyRepository repository;
    private final RegisteredApiRepository registeredApiRepository;

    // Used by Gateway on startup to build its route table
    @GetMapping("/routes")
    public ResponseEntity<List<ApiConfigDTO>> getAllRoutes() {
        List<ApiConfigDTO> routes = registeredApiRepository.findAll().stream()
                .filter(api -> api.isActive())
                .map(api -> ApiConfigDTO.builder()
                        .id(api.getId().toString())
                        .name(api.getName())
                        .targetUrl(api.getTargetUrl())
                        .proxyPath(api.getProxyPath())
                        .active(api.isActive())
                        .rateLimitRpm(0)
                        .monthlyQuota(0)
                        .apiKeyId("")
                        .apiId(api.getId().toString())
                        .disabledReason(null)
                        .build())
                .toList();
        return ResponseEntity.ok(routes);
    }

    // Used by Gateway per-request to validate an API Key
    @GetMapping("/configs/{keyHash}")
    public ResponseEntity<ApiConfigDTO> getApiConfig(@PathVariable String keyHash) {
        return repository.findByKeyHash(keyHash)
                .map(key -> {
                    var api = key.getRegisteredApi();
                    var plan = key.getPlan();
                    return ApiConfigDTO.builder()
                            .id(api.getId().toString())
                            .name(api.getName())
                            .targetUrl(api.getTargetUrl())
                            .proxyPath(api.getProxyPath())
                            .active(key.isActive())
                            .rateLimitRpm(plan.getRateLimitRpm())
                            .monthlyQuota(plan.getMonthlyQuota())
                            .apiKeyId(key.getId().toString())
                            .apiId(api.getId().toString())
                            .disabledReason(key.getDisabledReason())
                            .build();
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Used by Usage Service to disable a key when quota is exceeded
    @PutMapping("/keys/{id}/disable")
    public ResponseEntity<Void> disableKey(@PathVariable UUID id) {
        repository.findById(id).ifPresent(key -> {
            key.setActive(false);
            repository.save(key);
        });
        return ResponseEntity.ok().build();
    }
}
