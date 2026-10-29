package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.WorkSite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkSiteRepository extends JpaRepository<WorkSite, UUID> {
    List<WorkSite> findByTenantIdAndDeletedAtIsNullOrderByNameAsc(UUID tenantId);
    Optional<WorkSite> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);
}
