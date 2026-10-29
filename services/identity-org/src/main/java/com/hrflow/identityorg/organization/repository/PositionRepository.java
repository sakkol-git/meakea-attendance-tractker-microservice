package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.Position;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    Optional<Position> findByTenantIdAndId(UUID tenantId, UUID id);
    List<Position> findByTenantIdAndIdIn(UUID tenantId, Set<UUID> ids);
    List<Position> findByTenantIdOrderByNameAsc(UUID tenantId);
}
