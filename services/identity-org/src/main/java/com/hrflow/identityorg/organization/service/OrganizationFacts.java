package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.organization.entity.Employee;
import com.hrflow.identityorg.organization.entity.Holiday;
import com.hrflow.identityorg.organization.entity.Shift;
import com.hrflow.identityorg.organization.entity.WorkSite;
import com.hrflow.identityorg.organization.repository.DepartmentRepository;
import com.hrflow.identityorg.organization.repository.EmployeeShiftRepository;
import com.hrflow.identityorg.organization.repository.PositionRepository;
import com.hrflow.identityorg.organization.repository.WorkSiteRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrganizationFacts {
    private final OrganizationOutbox outbox;
    private final DepartmentRepository departments;
    private final PositionRepository positions;
    private final EmployeeShiftRepository assignments;
    private final WorkSiteRepository sites;
    private final com.hrflow.identityorg.profile.repository.UserRepository users;

    @Transactional(propagation = Propagation.MANDATORY)
    public void employee(Employee employee, String type) {
        var department = departments.findByTenantIdAndId(employee.getTenantId(), employee.getDepartmentId())
                .orElseThrow();
        var position = positions.findByTenantIdAndId(employee.getTenantId(), employee.getPositionId())
                .orElseThrow();
        var currentAssignments = assignments.findByTenantIdAndEmployeeIdOrderByValidFromDesc(
                employee.getTenantId(), employee.getId()).stream()
                .map(assignment -> new OrganizationSnapshots.Assignment(
                        assignment.getId().toString(), assignment.getShiftId().toString(),
                        assignment.getValidFrom().toString(),
                        assignment.getValidTo() == null ? null : assignment.getValidTo().toString()))
                .toList();
        var account = users.findByTenantIdAndEmployeeIdAndEnabledTrue(employee.getTenantId(), employee.getId());
        String accountId = account.map(user -> user.getId().toString()).orElse(null);
        var accountRoles = account.map(user -> com.hrflow.identityorg.auth.service.record.AuthRoles.parse(user.getRoles())).orElse(null);
        var payload = new OrganizationSnapshots.Employee(employee.getId().toString(), accountId,
                employee.getEmployeeCode(), employee.getName(), employee.getEmail(),
                employee.getDepartmentId().toString(), department.getName(),
                employee.getPositionId().toString(), position.getName(),
                employee.getManagerId() == null ? null : employee.getManagerId().toString(),
                employee.getHireDate() == null ? null : employee.getHireDate().toString(),
                null, employee.getStatus(), null, null, currentAssignments, null, accountRoles);
        outbox.append(employee.getTenantId(), "org.employee.v1", employee.getId(), type, payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void shift(Shift shift, String type) {
        WorkSite site = sites.findByTenantIdAndIdAndDeletedAtIsNull(shift.getTenantId(), shift.getSiteId())
                .orElseThrow();
        var sitePayload = new OrganizationSnapshots.Site(site.getId().toString(), site.getName(),
                site.getLatitude(), site.getLongitude(), BigDecimal.valueOf(site.getRadiusM()),
                site.getTimezone(), null, null);
        var payload = new OrganizationSnapshots.Shift(shift.getId().toString(), shift.getName(),
                shift.getStartTime().toString(), shift.getEndTime().toString(), shift.getTimezone(),
                BigDecimal.valueOf(shift.getGraceMinutes()), BigDecimal.valueOf(shift.getWorkingDays()),
                sitePayload, shift.getDeletedAt() == null ? null : shift.getDeletedAt().toString());
        outbox.append(shift.getTenantId(), "org.shift.v1", shift.getId(), type, payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void holiday(Holiday holiday, String type) {
        var payload = new OrganizationSnapshots.Holiday(holiday.getId().toString(),
                holiday.getDate().toString(), holiday.getName(), holiday.getKind(),
                holiday.getDeletedAt() == null ? null : holiday.getDeletedAt().toString());
        outbox.append(holiday.getTenantId(), "org.holiday.v1", holiday.getId(), type, payload);
    }
}
