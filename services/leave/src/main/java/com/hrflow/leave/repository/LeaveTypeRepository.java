package com.hrflow.leave.repository;

import com.hrflow.leave.entity.LeaveType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, String> {

    List<LeaveType> findByTenantId(String tenantId);

    org.springframework.data.domain.Page<LeaveType> findByTenantId(
            String tenantId, org.springframework.data.domain.Pageable pageable);

    Optional<LeaveType> findByTenantIdAndCode(String tenantId, String code);
}
