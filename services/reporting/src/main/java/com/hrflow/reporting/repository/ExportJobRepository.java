package com.hrflow.reporting.repository;

import com.hrflow.reporting.entity.ExportJob;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, UUID> {
    Optional<ExportJob> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<ExportJob> findByIdAndTenantIdAndRequesterId(UUID id, UUID tenantId, UUID requesterId);
    java.util.List<ExportJob> findByTenantIdAndRequesterId(UUID tenantId, UUID requesterId);
    java.util.List<ExportJob> findByTenantId(UUID tenantId);
}
