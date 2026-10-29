package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.audit.IdentityAuditService;
import com.hrflow.identityorg.organization.dto.ShiftAssignmentClose;
import com.hrflow.identityorg.organization.dto.ShiftAssignmentInput;
import com.hrflow.identityorg.organization.dto.ShiftAssignmentRecordV2;
import com.hrflow.identityorg.organization.entity.Employee;
import com.hrflow.identityorg.organization.entity.EmployeeShift;
import com.hrflow.identityorg.organization.entity.Shift;
import com.hrflow.identityorg.organization.entity.WorkSite;
import com.hrflow.identityorg.organization.repository.EmployeeRepository;
import com.hrflow.identityorg.organization.repository.EmployeeShiftRepository;
import com.hrflow.identityorg.organization.repository.ShiftRepository;
import com.hrflow.identityorg.organization.repository.WorkSiteRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ShiftAssignmentService {
    private final EmployeeRepository employees;
    private final ShiftRepository shifts;
    private final WorkSiteRepository sites;
    private final EmployeeShiftRepository assignments;
    private final TenantSqlScope tenantSqlScope;
    private final CommandReceipts receipts;
    private final OrganizationFacts facts;
    private final IdentityAuditService audit;

    @Transactional(readOnly = true)
    public List<ShiftAssignmentRecordV2> list(UUID tenantId) {
        tenantSqlScope.use(tenantId);
        return assignments.findByTenantIdOrderByValidFromDesc(tenantId).stream().map(this::record).toList();
    }

    @Transactional(readOnly = true)
    public List<ShiftAssignmentRecordV2> listForEmployee(UUID tenantId, UUID employeeId) {
        tenantSqlScope.use(tenantId);
        employees.findByTenantIdAndId(tenantId, employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return assignments.findByTenantIdAndEmployeeIdOrderByValidFromDesc(tenantId, employeeId)
                .stream().map(this::record).toList();
    }

    @Transactional
    public ShiftAssignmentRecordV2 assign(UUID tenantId, UUID actorId, UUID employeeId,
            String key, ShiftAssignmentInput request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/employees/" + employeeId + "/shifts", key,
                request, ShiftAssignmentRecordV2.class, () -> assignNew(tenantId, actorId, employeeId, request));
    }

    private ShiftAssignmentRecordV2 assignNew(UUID tenantId, UUID actorId, UUID employeeId, ShiftAssignmentInput request) {
        Employee employee = employees.lockByTenantIdAndId(tenantId, employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"ACTIVE".equals(employee.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee is not active");
        }
        if (request.validTo() != null && request.validTo().isBefore(request.validFrom())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid effective period");
        }
        Shift shift = shifts.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, request.shiftId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Active shift required"));
        WorkSite site = sites.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, shift.getSiteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Active work site required"));
        LocalDate end = request.validTo() == null ? LocalDate.of(9999, 12, 31) : request.validTo();
        if (!assignments.overlaps(tenantId, employeeId, request.validFrom(), end).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Shift assignment overlaps an existing period");
        }
        Map<String, Object> snapshot = Map.of(
                "name", shift.getName(), "startTime", shift.getStartTime().toString(),
                "endTime", shift.getEndTime().toString(), "graceMinutes", shift.getGraceMinutes(),
                "workingDays", shift.getWorkingDays(), "timezone", shift.getTimezone(),
                "site", Map.of("id", site.getId().toString(), "name", site.getName(),
                        "latitude", site.getLatitude(), "longitude", site.getLongitude(),
                        "radiusM", site.getRadiusM(), "timezone", site.getTimezone()));
        EmployeeShift saved = assignments.saveAndFlush(new EmployeeShift(tenantId, employeeId,
                shift.getId(), request.validFrom(), request.validTo(), snapshot));
        facts.employee(employee, "org.employee.updated.v1");
        audit.recordChanges(tenantId, actorId, "SHIFT_ASSIGNED", "employee_shift", saved.getId(),
                Map.of(), auditFields(saved));
        return record(saved);
    }

    @Transactional
    public ShiftAssignmentRecordV2 close(UUID tenantId, UUID actorId, UUID id, String ifMatch,
            ShiftAssignmentClose request) {
        tenantSqlScope.use(tenantId);
        EmployeeShift row = assignments.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Employee employee = employees.lockByTenantIdAndId(tenantId, row.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        RevisionPreconditions.require(ifMatch, row.getVersion());
        var before = auditFields(row);
        if (request.validTo().isBefore(row.getValidFrom())
                || row.getValidTo() != null && request.validTo().isAfter(row.getValidTo())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid close date");
        }
        row.close(request.validTo());
        EmployeeShift saved = assignments.saveAndFlush(row);
        facts.employee(employee, "org.employee.updated.v1");
        audit.recordChanges(tenantId, actorId, "SHIFT_ASSIGNMENT_CLOSED", "employee_shift", id,
                before, auditFields(saved));
        return record(saved);
    }

    private Map<String, Object> auditFields(EmployeeShift assignment) {
        var fields = new LinkedHashMap<String, Object>(Map.of("employeeId", assignment.getEmployeeId(),
                "shiftId", assignment.getShiftId(), "validFrom", assignment.getValidFrom().toString()));
        fields.put("validTo", assignment.getValidTo() == null ? null : assignment.getValidTo().toString());
        return fields;
    }

    private ShiftAssignmentRecordV2 record(EmployeeShift row) {
        return new ShiftAssignmentRecordV2(row.getId(), row.getEmployeeId(), row.getShiftId(),
                row.getValidFrom(), row.getValidTo(), row.getShiftSnapshot(), row.getVersion());
    }
}
