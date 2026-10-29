package com.hrflow.leave.repository;

import com.hrflow.leave.entity.LeaveRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, String> {

    List<LeaveRequest> findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(String tenantId, String employeeId);

    List<LeaveRequest> findByTenantIdAndStatusOrderByCreatedAtAsc(String tenantId, String status);

    Optional<LeaveRequest> findByTenantIdAndId(String tenantId, String id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT r FROM LeaveRequest r WHERE r.tenantId=:tenant AND r.id=:id")
    Optional<LeaveRequest> findForDecision(String tenant, String id);
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT r.* FROM leave.leave_request r WHERE r.tenant_id=:tenant AND (r.status='PENDING' OR (cast(:manager as varchar) IS NULL AND r.status='NEEDS_REVIEW'))
              AND (cast(:manager as varchar) IS NULL OR (r.employee_id<>:manager AND r.employee_id IN
                (SELECT e.employee_id FROM leave.employee_projection e
                 WHERE e.tenant_id=:tenant AND e.manager_id=:manager AND e.active=true)))
            ORDER BY r.created_at,r.id
            """, nativeQuery = true)
    List<LeaveRequest> findPendingScope(String tenant, String manager);
}
