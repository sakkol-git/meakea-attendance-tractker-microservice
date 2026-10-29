package com.hrflow.leave.organization;

import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.leave.service.TenantSqlScope;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Organization owns the relationship; leave stores only its authorization inputs. */
@Service
@RequiredArgsConstructor
public class LeaveEmployeeConsumer {
    private final JdbcTemplate jdbc;
    private final TenantSqlScope scope;
    private final LeaveAssignmentMapper mapper;
    private final tools.jackson.databind.json.JsonMapper json;

    @Transactional
    public void apply(String key, OrganizationFact event) {
        if (event == null || event.getSchemaVersion() != 1 || event.getAggregateVersion() < 1
                || event.getType() == null || !Set.of("org.employee.created.v1", "org.employee.updated.v1", "org.employee.terminated.v1").contains(event.getType())
                || !(event.getPayload() instanceof EmployeeSnapshot employee)) {
            throw new IllegalArgumentException("Unsupported leave employee reference");
        }
        String tenant = UUID.fromString(event.getTenantId()).toString(), id = UUID.fromString(event.getAggregateId()).toString();
        UUID receiptId = UUID.fromString(event.getEventId());
        UUID.fromString(event.getCorrelationId());
        Instant.parse(event.getOccurredAt());
        if (!(tenant + ":" + id).equals(key) || !id.equals(employee.getEmployeeId()) || employee.getStatus() == null
                || !Set.of("ACTIVE", "INACTIVE", "TERMINATED").contains(employee.getStatus())) {
            throw new IllegalArgumentException("Employee reference routing or status mismatch");
        }
        String manager = employee.getManagerId() == null ? null : UUID.fromString(employee.getManagerId()).toString();
        var assignments = employee.getAssignments().stream().map(mapper::snapshot).toList();
        if (assignments.stream().anyMatch(assignment -> assignment.assignmentId() == null || assignment.shiftId() == null
                || assignment.validFrom() == null || assignment.validTo() != null && assignment.validTo().isBefore(assignment.validFrom()))) {
            throw new IllegalArgumentException("Invalid employee assignment reference");
        }
        scope.use(tenant);
        int receipt = jdbc.update("INSERT INTO leave.organization_inbox(tenant_id,event_id) VALUES (?,?) ON CONFLICT DO NOTHING", tenant, receiptId);
        // V4 may restore an unknown assignment basis from the exact already-received full owner revision.
        // Known projections still ignore duplicates and stale versions.
        jdbc.update("""
                INSERT INTO leave.employee_projection(tenant_id,employee_id,manager_id,active,revision,assignments)
                VALUES (?,?,?,?,?,?::jsonb) ON CONFLICT(tenant_id,employee_id) DO UPDATE
                SET manager_id=EXCLUDED.manager_id,active=EXCLUDED.active,revision=EXCLUDED.revision,assignments=EXCLUDED.assignments
                WHERE leave.employee_projection.revision<EXCLUDED.revision
                   OR (leave.employee_projection.revision=EXCLUDED.revision AND leave.employee_projection.assignments IS NULL)
                """, tenant, id, manager, "ACTIVE".equals(employee.getStatus()) && employee.getDeletedAt() == null,
                event.getAggregateVersion(), json.writeValueAsString(assignments));
    }
}
