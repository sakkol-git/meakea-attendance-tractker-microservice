package com.hrflow.reporting.repository;

import com.hrflow.reporting.entity.AttendanceFactProjection;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceFactProjectionRepository extends JpaRepository<AttendanceFactProjection, UUID> {
    @Query("""
            SELECT a FROM AttendanceFactProjection a WHERE a.tenantId=:tenant
              AND (:manager IS NULL OR a.employeeId IN (SELECT e.id.employeeId FROM ReportingEmployee e
                WHERE e.id.tenantId=:tenant AND e.managerId=:manager AND e.active=true))
              AND (cast(:fromDate as LocalDate) IS NULL OR a.workDate>=:fromDate)
              AND (cast(:toDate as LocalDate) IS NULL OR a.workDate<=:toDate)
            """)
    java.util.List<AttendanceFactProjection> findScoped(UUID tenant, UUID manager, LocalDate fromDate, LocalDate toDate,
            org.springframework.data.domain.Sort sort);
    Optional<AttendanceFactProjection> findByTenantIdAndSessionId(UUID tenantId, UUID sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AttendanceFactProjection a WHERE a.tenantId = :tenantId AND a.sessionId = :sessionId")
    Optional<AttendanceFactProjection> findForUpdate(@Param("tenantId") UUID tenantId, @Param("sessionId") UUID sessionId);

    @Query("SELECT COUNT(a) FROM AttendanceFactProjection a WHERE a.tenantId = :tenantId AND a.workDate = :workDate AND a.status = 'PRESENT'")
    long countPresentToday(@Param("tenantId") UUID tenantId, @Param("workDate") LocalDate workDate);

    @Query("SELECT COUNT(a) FROM AttendanceFactProjection a WHERE a.tenantId = :tenantId AND a.workDate = :workDate AND a.status = 'LATE'")
    long countLateToday(@Param("tenantId") UUID tenantId, @Param("workDate") LocalDate workDate);

    @Query("SELECT COUNT(a) FROM AttendanceFactProjection a WHERE a.tenantId = :tenantId AND a.workDate = :workDate AND a.status = 'ABSENT'")
    long countAbsentToday(@Param("tenantId") UUID tenantId, @Param("workDate") LocalDate workDate);

    java.util.List<AttendanceFactProjection> findByTenantId(UUID tenantId, org.springframework.data.domain.Sort sort);
    java.util.List<AttendanceFactProjection> findByTenantIdAndWorkDateBetween(UUID tenantId, LocalDate start, LocalDate end, org.springframework.data.domain.Sort sort);
    java.util.List<AttendanceFactProjection> findByTenantIdAndWorkDateGreaterThanEqual(UUID tenantId, LocalDate start, org.springframework.data.domain.Sort sort);
    java.util.List<AttendanceFactProjection> findByTenantIdAndWorkDateLessThanEqual(UUID tenantId, LocalDate end, org.springframework.data.domain.Sort sort);
}
