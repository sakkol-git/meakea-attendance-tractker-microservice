package com.hrflow.leave.repository;

import com.hrflow.leave.entity.LeaveDay;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveDayRepository extends JpaRepository<LeaveDay, String> {

    List<LeaveDay> findByTenantIdAndRequestId(String tenantId, String requestId);

    List<LeaveDay> findByTenantIdAndEmployeeIdAndDateBetween(
            String tenantId, String employeeId, LocalDate startDate, LocalDate endDate);

    void deleteByTenantIdAndRequestId(String tenantId, String requestId);
}
