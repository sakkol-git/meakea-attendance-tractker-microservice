package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.Department;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findByTenantIdAndId(UUID tenantId, UUID id);
    List<Department> findByTenantIdAndIdIn(UUID tenantId, Set<UUID> ids);
    List<Department> findByTenantIdOrderByNameAsc(UUID tenantId);
}
