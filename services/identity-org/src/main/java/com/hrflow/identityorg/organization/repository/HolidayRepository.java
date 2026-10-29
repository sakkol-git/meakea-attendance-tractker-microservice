package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.Holiday;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, UUID> {
    List<Holiday> findByTenantIdAndDeletedAtIsNullOrderByDateAsc(UUID tenantId);
    Optional<Holiday> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);
}
