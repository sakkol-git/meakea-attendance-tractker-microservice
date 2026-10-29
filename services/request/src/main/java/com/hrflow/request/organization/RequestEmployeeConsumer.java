package com.hrflow.request.organization;

import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.request.service.TenantSqlScope;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Only organization can establish the relationship used to resolve request approval. */
@Service
@RequiredArgsConstructor
public class RequestEmployeeConsumer {
    private final JdbcTemplate jdbc;
    private final TenantSqlScope scope;

    @Transactional
    public void apply(String key, OrganizationFact event) {
        if (event == null || event.getSchemaVersion() != 1 || event.getAggregateVersion() < 1
                || event.getType() == null || !Set.of("org.employee.created.v1", "org.employee.updated.v1", "org.employee.terminated.v1").contains(event.getType())
                || !(event.getPayload() instanceof EmployeeSnapshot employee)) {
            throw new IllegalArgumentException("Unsupported request employee reference");
        }
        String tenant = UUID.fromString(event.getTenantId()).toString(), id = UUID.fromString(event.getAggregateId()).toString();
        UUID receipt = UUID.fromString(event.getEventId());
        UUID.fromString(event.getCorrelationId());
        Instant.parse(event.getOccurredAt());
        if (!(tenant + ":" + id).equals(key) || !id.equals(employee.getEmployeeId()) || employee.getStatus() == null
                || !Set.of("ACTIVE", "INACTIVE", "TERMINATED").contains(employee.getStatus())) {
            throw new IllegalArgumentException("Employee reference routing or status mismatch");
        }
        String manager = employee.getManagerId() == null ? null : UUID.fromString(employee.getManagerId()).toString();
        scope.use(tenant);
        int inserted = jdbc.update("INSERT INTO request.organization_inbox(tenant_id,event_id) VALUES (?,?) ON CONFLICT DO NOTHING", tenant, receipt);
        if (inserted == 0) return;
        jdbc.update("""
                INSERT INTO request.employee_projection(tenant_id,employee_id,manager_id,active,revision)
                VALUES (?,?,?,?,?) ON CONFLICT(tenant_id,employee_id) DO UPDATE
                SET manager_id=EXCLUDED.manager_id,active=EXCLUDED.active,revision=EXCLUDED.revision
                WHERE request.employee_projection.revision<EXCLUDED.revision
                """, tenant, id, manager, "ACTIVE".equals(employee.getStatus()) && employee.getDeletedAt() == null, event.getAggregateVersion());
    }
}
