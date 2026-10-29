package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.Shift;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {
    List<Shift> findByTenantIdAndDeletedAtIsNullOrderByNameAsc(UUID tenantId);
    Optional<Shift> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);
    boolean existsByTenantIdAndSiteIdAndDeletedAtIsNull(UUID tenantId, UUID siteId);
    List<Shift> findByTenantIdAndSiteIdAndDeletedAtIsNull(UUID tenantId, UUID siteId);
}
