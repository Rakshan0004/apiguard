package com.apiguard.management.repository;

import com.apiguard.management.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findByRegisteredApiIdAndOwnerEmail(UUID apiId, String ownerEmail);
    List<Plan> findByOwnerEmail(String ownerEmail);
    Optional<Plan> findByIdAndOwnerEmail(UUID id, String ownerEmail);
}
