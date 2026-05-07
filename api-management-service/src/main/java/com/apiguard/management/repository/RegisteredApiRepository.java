package com.apiguard.management.repository;

import com.apiguard.management.entity.RegisteredApi;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface RegisteredApiRepository extends JpaRepository<RegisteredApi, UUID> {
    Optional<RegisteredApi> findByProxyPath(String proxyPath);
    boolean existsByProxyPath(String proxyPath);
    java.util.List<RegisteredApi> findByOwnerEmail(String ownerEmail);
}
