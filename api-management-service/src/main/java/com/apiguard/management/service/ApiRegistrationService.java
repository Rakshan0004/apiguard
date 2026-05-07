package com.apiguard.management.service;

import com.apiguard.management.dto.ApiRegistrationRequest;
import com.apiguard.management.entity.RegisteredApi;
import com.apiguard.management.repository.RegisteredApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiRegistrationService {

    private final RegisteredApiRepository repository;

    @Transactional
    public RegisteredApi registerApi(String ownerEmail, ApiRegistrationRequest request) {
        if (repository.existsByProxyPath(request.proxyPath())) {
            throw new IllegalArgumentException("Proxy path already exists: " + request.proxyPath());
        }

        RegisteredApi api = RegisteredApi.builder()
                .name(request.name())
                .ownerEmail(ownerEmail)
                .targetUrl(request.targetUrl())
                .proxyPath(request.proxyPath())
                .build();

        return repository.save(api);
    }

    public List<RegisteredApi> getUserApis(String ownerEmail) {
        return repository.findByOwnerEmail(ownerEmail);
    }
}
