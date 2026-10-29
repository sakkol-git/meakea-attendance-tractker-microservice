package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.audit.IdentityAuditService;
import com.hrflow.identityorg.organization.dto.EmployeeCreateRequest;
import com.hrflow.identityorg.organization.dto.EmployeePageV2;
import com.hrflow.identityorg.organization.dto.EmployeePatchRequest;
import com.hrflow.identityorg.organization.dto.EmployeeRecordV2;
import com.hrflow.identityorg.organization.entity.Employee;
import com.hrflow.identityorg.organization.repository.DepartmentRepository;
import com.hrflow.identityorg.organization.repository.EmployeeRepository;
import com.hrflow.identityorg.organization.repository.PositionRepository;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final PositionRepository positions;
    private final CommandReceipts receipts;
    private final TenantSqlScope tenantSqlScope;
    private final OrganizationFacts facts;
    private final IdentityAuditService audit;

    @Transactional(readOnly = true)
    public EmployeePageV2 list(UUID tenantId, int page, int size, String search) {
        tenantSqlScope.use(tenantId);
        if (page < 0 || size < 1 || size > 100) throw problem(HttpStatus.UNPROCESSABLE_ENTITY);
        if (search != null && search.length() > 120) throw problem(HttpStatus.UNPROCESSABLE_ENTITY);
        String term = search == null || search.isBlank() ? null : search.strip().toLowerCase(Locale.ROOT);
        PageRequest paging = PageRequest.of(page, size);
        Page<Employee> rows = term == null ? employees.findByTenantId(tenantId, paging)
                : employees.search(tenantId, term, paging);
        var content = rows.getContent();
        Set<UUID> departmentIds = content.stream().map(Employee::getDepartmentId).collect(Collectors.toSet());
        Set<UUID> positionIds = content.stream().map(Employee::getPositionId).collect(Collectors.toSet());
        Map<UUID, String> departmentNames = departmentIds.isEmpty() ? Map.of()
                : departments.findByTenantIdAndIdIn(tenantId, departmentIds).stream()
                        .collect(Collectors.toMap(row -> row.getId(), row -> row.getName()));
        Map<UUID, String> positionNames = positionIds.isEmpty() ? Map.of()
                : positions.findByTenantIdAndIdIn(tenantId, positionIds).stream()
                        .collect(Collectors.toMap(row -> row.getId(), row -> row.getName()));
        return new EmployeePageV2(content.stream().map(row -> record(row,
                departmentNames.get(row.getDepartmentId()), positionNames.get(row.getPositionId())))
                .toList(), page, size, rows.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EmployeeRecordV2 get(UUID tenantId, UUID id) {
        tenantSqlScope.use(tenantId);
        return record(find(tenantId, id));
    }

    @Transactional
    public EmployeeRecordV2 create(UUID tenantId, UUID actorId, String key, EmployeeCreateRequest request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/employees", key, request,
                EmployeeRecordV2.class, () -> createNew(tenantId, actorId, request));
    }

    private EmployeeRecordV2 createNew(UUID tenantId, UUID actorId, EmployeeCreateRequest request) {
        validateReferences(tenantId, request.departmentId(), request.positionId(), request.managerId());
        Employee row = new Employee(tenantId, request.employeeNo().strip(), request.name().strip(),
                request.email().strip().toLowerCase(Locale.ROOT), request.departmentId(),
                request.positionId(), request.managerId(), request.hireDate());
        Employee saved = employees.saveAndFlush(row);
        facts.employee(saved, "org.employee.created.v1");
        audit.recordChanges(tenantId, actorId, "EMPLOYEE_CREATED", "employee", saved.getId(),
                Map.of(), auditFields(saved));
        return record(saved);
    }

    @Transactional
    public EmployeeRecordV2 patch(UUID tenantId, UUID actorId, UUID id, String ifMatch, EmployeePatchRequest request) {
        tenantSqlScope.use(tenantId);
        Employee row = find(tenantId, id);
        RevisionPreconditions.require(ifMatch, row.getVersion());
        Map<String, Object> before = auditFields(row);
        UUID departmentId = request.getDepartmentId() == null ? row.getDepartmentId() : request.getDepartmentId();
        UUID positionId = request.getPositionId() == null ? row.getPositionId() : request.getPositionId();
        UUID managerId = request.isManagerIdPresent() ? request.getManagerId() : row.getManagerId();
        validateReferences(tenantId, departmentId, positionId, managerId);
        row.update(strip(request.getName()), normalizeEmail(request.getEmail()), strip(request.getEmployeeNo()),
                request.getDepartmentId(), request.getPositionId(), managerId,
                request.isManagerIdPresent(), request.getHireDate());
        Employee saved = employees.saveAndFlush(row);
        facts.employee(saved, "org.employee.updated.v1");
        audit.recordChanges(tenantId, actorId, "EMPLOYEE_UPDATED", "employee", saved.getId(),
                before, auditFields(saved));
        return record(saved);
    }

    private Employee find(UUID tenantId, UUID id) {
        return employees.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> problem(HttpStatus.NOT_FOUND));
    }

    private Map<String, Object> auditFields(Employee employee) {
        // Privacy allowlist: audit approved master fields, never serialize arbitrary entities.
        var fields = new LinkedHashMap<String, Object>();
        fields.put("name", employee.getName());
        fields.put("email", employee.getEmail());
        fields.put("employeeNo", employee.getEmployeeCode());
        fields.put("departmentId", employee.getDepartmentId());
        fields.put("positionId", employee.getPositionId());
        fields.put("managerId", employee.getManagerId());
        fields.put("hireDate", employee.getHireDate() == null ? null : employee.getHireDate().toString());
        return fields;
    }

    private void validateReferences(UUID tenantId, UUID departmentId, UUID positionId, UUID managerId) {
        if (departments.findByTenantIdAndId(tenantId, departmentId).isEmpty()
                || positions.findByTenantIdAndId(tenantId, positionId).isEmpty()
                || managerId != null && employees.findByTenantIdAndId(tenantId, managerId).isEmpty()) {
            throw problem(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private EmployeeRecordV2 record(Employee row) {
        String department = departments.findByTenantIdAndId(row.getTenantId(), row.getDepartmentId())
                .orElseThrow(() -> problem(HttpStatus.NOT_FOUND)).getName();
        String position = positions.findByTenantIdAndId(row.getTenantId(), row.getPositionId())
                .orElseThrow(() -> problem(HttpStatus.NOT_FOUND)).getName();
        return record(row, department, position);
    }

    private EmployeeRecordV2 record(Employee row, String department, String position) {
        if (department == null || position == null) throw problem(HttpStatus.NOT_FOUND);
        return new EmployeeRecordV2(row.getId(), row.getName(), row.getEmail(), row.getEmployeeCode(),
                row.getDepartmentId(), row.getPositionId(), row.getManagerId(),
                department, position, row.getStatus(), row.getHireDate(), row.getVersion());
    }

    private String strip(String value) {
        return value == null ? null : value.strip();
    }

    private String normalizeEmail(String value) {
        return value == null ? null : value.strip().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException problem(HttpStatus status) {
        return new ResponseStatusException(status);
    }
}
