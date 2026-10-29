package com.hrflow.attendance.repository;

import com.hrflow.attendance.entity.AttendanceSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {
    boolean existsByTenantIdAndEmployeeIdAndWorkDateIsNull(UUID tenant, UUID employee);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM AttendanceSession s WHERE s.tenantId=:tenant AND s.employeeId=:employee AND (s.workDate=:date OR s.id=:target) ORDER BY s.clockIn,s.id")
    List<AttendanceSession> originalSessions(@org.springframework.data.repository.query.Param("tenant") UUID tenant,
            @org.springframework.data.repository.query.Param("employee") UUID employee,
            @org.springframework.data.repository.query.Param("date") java.time.LocalDate date,
            @org.springframework.data.repository.query.Param("target") UUID target);

    Optional<AttendanceSession> findByTenantIdAndEmployeeIdAndClockOutIsNull(UUID tenantId, UUID employeeId);

    Optional<AttendanceSession> findByTenantIdAndId(UUID tenantId, UUID id);

    List<AttendanceSession> findByTenantIdAndEmployeeIdOrderByClockInDesc(UUID tenantId, UUID employeeId);
}
