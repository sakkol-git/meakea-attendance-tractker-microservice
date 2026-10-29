package com.hrflow.attendance.repository;

import com.hrflow.attendance.entity.AttendanceCorrection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceCorrectionRepository extends JpaRepository<AttendanceCorrection, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM AttendanceCorrection c WHERE c.id=:id AND c.tenantId=:tenant")
    Optional<AttendanceCorrection> findForDecision(UUID id, UUID tenant);
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT c.* FROM attendance.attendance_correction c
            JOIN attendance.employee_projection e ON e.tenant_id=c.tenant_id AND e.employee_id=c.employee_id
            WHERE c.tenant_id=:tenant AND e.manager_known AND e.manager_id=:manager AND e.status='ACTIVE'
            ORDER BY c.created_at DESC,c.id
            """, nativeQuery = true)
    List<AttendanceCorrection> findCurrentDirectReports(UUID tenant, UUID manager);
    Optional<AttendanceCorrection> findByIdAndTenantId(UUID id, UUID tenantId);
    List<AttendanceCorrection> findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(UUID tenantId, UUID employeeId);
    List<AttendanceCorrection> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<AttendanceCorrection> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status);
}
