package com.hrflow.reporting.repository;

import com.hrflow.reporting.entity.RequestFactProjection;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestFactProjectionRepository extends JpaRepository<RequestFactProjection, UUID> {
    @Query("""
            SELECT count(r) FROM RequestFactProjection r WHERE r.tenantId=:tenant AND r.status IN ('SUBMITTED','PENDING','PENDING_APPROVAL')
              AND (:manager IS NULL OR r.requesterId IN (SELECT e.id.employeeId FROM ReportingEmployee e
                WHERE e.id.tenantId=:tenant AND e.managerId=:manager AND e.active=true))
            """)
    long countScopedPending(UUID tenant, UUID manager);
    Optional<RequestFactProjection> findByTenantIdAndRequestId(UUID tenantId, UUID requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RequestFactProjection r WHERE r.tenantId = :tenantId AND r.requestId = :requestId")
    Optional<RequestFactProjection> findForUpdate(@Param("tenantId") UUID tenantId, @Param("requestId") UUID requestId);

    @Query("SELECT COUNT(r) FROM RequestFactProjection r WHERE r.tenantId = :tenantId AND r.status IN ('SUBMITTED', 'PENDING', 'PENDING_APPROVAL')")
    long countPendingRequests(@Param("tenantId") UUID tenantId);
}
