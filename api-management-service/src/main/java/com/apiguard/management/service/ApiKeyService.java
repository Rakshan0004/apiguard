package com.apiguard.management.service;

import com.apiguard.management.entity.ApiKey;
import com.apiguard.management.entity.Plan;
import com.apiguard.management.entity.RegisteredApi;
import com.apiguard.management.repository.ApiKeyRepository;
import com.apiguard.management.repository.PlanRepository;
import com.apiguard.management.repository.RegisteredApiRepository;
import com.apiguard.management.util.KeyGeneratorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository repository;
    private final RegisteredApiRepository apiRepository;
    private final PlanRepository planRepository;

    @Transactional
    public String createApiKey(UUID apiId, String planName) {
        RegisteredApi api = apiRepository.findById(apiId)
                .orElseThrow(() -> new IllegalArgumentException("API not found: " + apiId));

        Plan plan = planRepository.findByName(planName)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planName));

        String rawKey = KeyGeneratorUtils.generateRawKey();
        String hash = KeyGeneratorUtils.hashKey(rawKey);

        ApiKey apiKey = ApiKey.builder()
                .keyHash(hash)
                .keyPrefix(rawKey.substring(0, 8))
                .registeredApi(api)
                .plan(plan)
                .build();

        repository.save(apiKey);
        return rawKey; // Return raw key only once
    }

    public ApiKey validateKey(String rawKey) {
        String hash = KeyGeneratorUtils.hashKey(rawKey);
        return repository.findByKeyHash(hash)
                .filter(ApiKey::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive API key"));
    }
}
