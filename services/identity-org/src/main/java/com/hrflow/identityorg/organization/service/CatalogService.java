package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.audit.IdentityAuditService;
import com.hrflow.identityorg.organization.dto.NamedCreateRequest;
import com.hrflow.identityorg.organization.dto.NamedRecord;
import com.hrflow.identityorg.organization.entity.Department;
import com.hrflow.identityorg.organization.entity.Position;
import com.hrflow.identityorg.organization.repository.DepartmentRepository;
import com.hrflow.identityorg.organization.repository.PositionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final DepartmentRepository departments;
    private final PositionRepository positions;
    private final CommandReceipts receipts;
    private final TenantSqlScope tenantSqlScope;
    private final IdentityAuditService audit;

    @Transactional(readOnly = true)
    public List<NamedRecord> departments(UUID tenantId) {
        tenantSqlScope.use(tenantId);
        return departments.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(row -> new NamedRecord(row.getId(), row.getName(), row.getCode(), row.getVersion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NamedRecord> positions(UUID tenantId) {
        tenantSqlScope.use(tenantId);
        return positions.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(row -> new NamedRecord(row.getId(), row.getName(), row.getCode(), row.getVersion()))
                .toList();
    }

    @Transactional
    public NamedRecord createDepartment(UUID tenantId, UUID actorId, String key, NamedCreateRequest request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/departments", key, request, NamedRecord.class, () -> {
            Department row = departments.saveAndFlush(new Department(tenantId, code(request), request.name().strip()));
            audit.record(tenantId, actorId, "DEPARTMENT_CREATED", "department", row.getId(),
                    Map.of("name", Map.of("after", row.getName()), "code", Map.of("after", row.getCode())));
            return new NamedRecord(row.getId(), row.getName(), row.getCode(), row.getVersion());
        });
    }

    @Transactional
    public NamedRecord createPosition(UUID tenantId, UUID actorId, String key, NamedCreateRequest request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/positions", key, request, NamedRecord.class, () -> {
            Position row = positions.saveAndFlush(new Position(tenantId, code(request), request.name().strip()));
            audit.record(tenantId, actorId, "POSITION_CREATED", "position", row.getId(),
                    Map.of("name", Map.of("after", row.getName()), "code", Map.of("after", row.getCode())));
            return new NamedRecord(row.getId(), row.getName(), row.getCode(), row.getVersion());
        });
    }

    private String code(NamedCreateRequest request) {
        return request.code() == null || request.code().isBlank()
                ? UUID.randomUUID().toString() : request.code().strip();
    }
}
