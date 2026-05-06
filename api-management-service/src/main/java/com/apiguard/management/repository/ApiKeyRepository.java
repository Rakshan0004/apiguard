package com.apiguard.management.repository;

import com.apiguard.management.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByKeyHash(String keyHash);
    List<ApiKey> findByRegisteredApiId(UUID registeredApiId);
}
