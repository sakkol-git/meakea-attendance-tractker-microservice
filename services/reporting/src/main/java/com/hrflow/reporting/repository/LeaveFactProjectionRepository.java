package com.hrflow.reporting.repository;

import com.hrflow.reporting.entity.LeaveFactProjection;
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
public interface LeaveFactProjectionRepository extends JpaRepository<LeaveFactProjection, UUID> {
    @Query("""
            SELECT count(l) FROM LeaveFactProjection l WHERE l.tenantId=:tenant AND l.status IN ('APPROVED','NEEDS_REVIEW')
              AND :today BETWEEN l.startDate AND l.endDate
              AND (:manager IS NULL OR l.employeeId IN (SELECT e.id.employeeId FROM ReportingEmployee e
                WHERE e.id.tenantId=:tenant AND e.managerId=:manager AND e.active=true))
            """)
    long countScopedActive(UUID tenant, UUID manager, LocalDate today);
    Optional<LeaveFactProjection> findByTenantIdAndLeaveId(UUID tenantId, UUID leaveId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LeaveFactProjection l WHERE l.tenantId = :tenantId AND l.leaveId = :leaveId")
    Optional<LeaveFactProjection> findForUpdate(@Param("tenantId") UUID tenantId, @Param("leaveId") UUID leaveId);

    @Query("SELECT COUNT(l) FROM LeaveFactProjection l WHERE l.tenantId = :tenantId AND l.status = 'APPROVED' AND :today BETWEEN l.startDate AND l.endDate")
    long countActiveLeavesToday(@Param("tenantId") UUID tenantId, @Param("today") LocalDate today);
}
