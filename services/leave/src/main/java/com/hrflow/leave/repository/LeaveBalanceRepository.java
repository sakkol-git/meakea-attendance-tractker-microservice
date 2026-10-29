package com.hrflow.leave.repository;

import com.hrflow.leave.entity.LeaveBalance;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, String> {

    List<LeaveBalance> findByTenantIdAndEmployeeIdAndYear(String tenantId, String employeeId, int year);

    List<LeaveBalance> findByTenantIdAndEmployeeId(String tenantId, String employeeId);

    org.springframework.data.domain.Page<LeaveBalance> findByTenantIdAndEmployeeId(
            String tenantId, String employeeId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<LeaveBalance> findByTenantIdAndEmployeeIdAndYear(
            String tenantId, String employeeId, int year, org.springframework.data.domain.Pageable pageable);

    Optional<LeaveBalance> findByTenantIdAndEmployeeIdAndLeaveTypeCodeAndYear(
            String tenantId, String employeeId, String leaveTypeCode, int year);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM LeaveBalance b WHERE b.tenantId = :tenantId AND b.employeeId = :employeeId AND b.leaveTypeCode = :leaveTypeCode AND b.year = :year")
    Optional<LeaveBalance> findForUpdate(
            @Param("tenantId") String tenantId,
            @Param("employeeId") String employeeId,
            @Param("leaveTypeCode") String leaveTypeCode,
            @Param("year") int year);
}
