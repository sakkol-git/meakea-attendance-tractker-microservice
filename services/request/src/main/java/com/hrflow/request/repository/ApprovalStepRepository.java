package com.hrflow.request.repository;
import com.hrflow.request.entity.ApprovalStep;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, String> {
    List<ApprovalStep> findByTenantIdAndRequestIdOrderByStepOrderAsc(String tenantId, String requestId);

    Optional<ApprovalStep> findByTenantIdAndRequestIdAndStepOrder(String tenantId, String requestId, int stepOrder);

    List<ApprovalStep> findByTenantIdAndApproverIdAndStatus(String tenantId, String approverId, String status);

    void deleteByTenantIdAndRequestId(String tenantId, String requestId);
}
