package com.apiguard.management.repository;

import com.apiguard.management.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    Optional<Plan> findByName(String name);
}
