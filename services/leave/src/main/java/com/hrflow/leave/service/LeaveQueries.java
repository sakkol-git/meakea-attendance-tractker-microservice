package com.hrflow.leave.service;

import com.hrflow.leave.dto.LeaveBalanceResponse;
import com.hrflow.leave.dto.LeaveRecordResponse;
import com.hrflow.leave.dto.LeaveTypeResponse;
import com.hrflow.leave.dto.LeavePage;
import com.hrflow.leave.entity.LeaveBalance;
import com.hrflow.leave.entity.LeaveRequest;
import com.hrflow.leave.mapper.LeaveBalanceMapper;
import com.hrflow.leave.mapper.LeaveRequestMapper;
import com.hrflow.leave.mapper.LeaveTypeMapper;
import com.hrflow.leave.repository.LeaveBalanceRepository;
import com.hrflow.leave.repository.LeaveRequestRepository;
import com.hrflow.leave.repository.LeaveTypeRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@lombok.RequiredArgsConstructor
public class LeaveQueries {
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final TenantSqlScope tenantSqlScope;
    private final LeaveAccess access;
    private final LeaveTypeMapper leaveTypeMapper;
    private final LeaveBalanceMapper leaveBalanceMapper;
    private final LeaveRequestMapper leaveRequestMapper;

    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> listLeaveTypes(String tenantId) {
        tenantSqlScope.use(tenantId);
        return leaveTypeRepository.findByTenantId(tenantId)
                .stream()
                .map(leaveTypeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeavePage<LeaveTypeResponse> typePage(String tenant, int page, int size) {
        tenantSqlScope.use(tenant);
        return LeavePage.from(leaveTypeRepository.findByTenantId(tenant, LeavePage.request(page, size, "code", "id"))
                .map(leaveTypeMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public LeavePage<LeaveBalanceResponse> balancePage(String tenant, String employee, Integer year, int page, int size) {
        tenantSqlScope.use(tenant);
        var request = LeavePage.request(page, size, "year", "leaveTypeCode", "id");
        var balances = year == null
                ? leaveBalanceRepository.findByTenantIdAndEmployeeId(tenant, employee, request)
                : leaveBalanceRepository.findByTenantIdAndEmployeeIdAndYear(tenant, employee, year, request);
        return LeavePage.from(balances.map(leaveBalanceMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getMyBalances(String tenantId, String employeeId, Integer year) {
        tenantSqlScope.use(tenantId);
        List<LeaveBalance> balances;
        if (year != null) {
            balances = leaveBalanceRepository.findByTenantIdAndEmployeeIdAndYear(tenantId, employeeId, year);
        } else {
            balances = leaveBalanceRepository.findByTenantIdAndEmployeeId(tenantId, employeeId);
        }
        return balances.stream()
                .map(leaveBalanceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRecordResponse> getMyRequests(String tenantId, String employeeId) {
        tenantSqlScope.use(tenantId);
        return leaveRequestRepository.findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(tenantId, employeeId)
                .stream()
                .map(leaveRequestMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRecordResponse> getPendingRequests(String tenant, LeaveCaller caller) {
        tenantSqlScope.use(tenant);
        return leaveRequestRepository.findPendingScope(tenant, access.pendingScope(tenant, caller))
                .stream().map(leaveRequestMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LeaveRecordResponse getRequest(String tenant, LeaveCaller caller, String id) {
        tenantSqlScope.use(tenant);
        LeaveRequest request = leaveRequestRepository.findByTenantIdAndId(tenant, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
        access.requireRead(tenant, caller, request.getEmployeeId());
        return leaveRequestMapper.toResponse(request);
    }

}
