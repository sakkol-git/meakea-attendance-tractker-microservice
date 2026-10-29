package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.EmployeeShift;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeShiftRepository extends JpaRepository<EmployeeShift, UUID> {
    List<EmployeeShift> findByTenantIdAndEmployeeIdOrderByValidFromDesc(UUID tenantId, UUID employeeId);
    List<EmployeeShift> findByTenantIdOrderByValidFromDesc(UUID tenantId);
    Optional<EmployeeShift> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("""
            select row from EmployeeShift row
            where row.tenantId = :tenant and row.employeeId = :employee
              and row.validFrom <= :end and (row.validTo is null or row.validTo >= :start)
            """)
    List<EmployeeShift> overlaps(@Param("tenant") UUID tenant, @Param("employee") UUID employee,
            @Param("start") LocalDate start, @Param("end") LocalDate end);
}
