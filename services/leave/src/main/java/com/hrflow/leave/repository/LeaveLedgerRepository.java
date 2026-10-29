package com.hrflow.leave.repository;

import com.hrflow.leave.entity.LeaveLedger;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveLedgerRepository extends JpaRepository<LeaveLedger, String> {

    List<LeaveLedger> findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(String tenantId, String employeeId);

    List<LeaveLedger> findByTenantIdAndReferenceId(String tenantId, String referenceId);
}
