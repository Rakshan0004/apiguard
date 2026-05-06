package com.apiguard.management.controller;

import com.apiguard.common.dto.ApiConfigDTO;
import com.apiguard.management.entity.ApiKey;
import com.apiguard.management.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalConfigController {

    private final ApiKeyRepository repository;

    @GetMapping("/configs/{keyHash}")
    public ResponseEntity<ApiConfigDTO> getApiConfig(@PathVariable String keyHash) {
        return repository.findByKeyHash(keyHash)
                .filter(ApiKey::isActive)
                .map(key -> {
                    var api = key.getRegisteredApi();
                    var plan = key.getPlan();
                    return ApiConfigDTO.builder()
                            .id(api.getId().toString())
                            .name(api.getName())
                            .targetUrl(api.getTargetUrl())
                            .proxyPath(api.getProxyPath())
                            .active(api.isActive())
                            .rateLimitRpm(plan.getRateLimitRpm())
                            .monthlyQuota(plan.getMonthlyQuota())
                            .build();
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
