package com.hrflow.reporting.consumer;

import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.reporting.service.TenantSqlScope;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Minimal local relationship facts; no cross-owner SQL/HTTP lookup or authorization from UI roles. */
@Service
@RequiredArgsConstructor
public class ReportingEmployeeConsumer {
    private final EntityManager entityManager;
    private final TenantSqlScope scope;

    @Transactional
    public void apply(String key, OrganizationFact event) {
        if (event == null || event.getSchemaVersion() != 1 || event.getAggregateVersion() < 1
                || !Set.of("org.employee.created.v1", "org.employee.updated.v1", "org.employee.terminated.v1")
                        .contains(event.getType()) || !(event.getPayload() instanceof EmployeeSnapshot employee)) {
            throw new IllegalArgumentException("Unsupported employee relationship snapshot");
        }
        UUID tenant = UUID.fromString(event.getTenantId()), aggregate = UUID.fromString(event.getAggregateId());
        UUID eventId = UUID.fromString(event.getEventId());
        UUID.fromString(event.getCorrelationId());
        Instant.parse(event.getOccurredAt());
        if (!(tenant + ":" + aggregate).equals(key) || !aggregate.toString().equals(employee.getEmployeeId())
                || !Set.of("ACTIVE", "INACTIVE", "TERMINATED").contains(employee.getStatus())) {
            throw new IllegalArgumentException("Employee snapshot routing/status mismatch");
        }
        UUID manager = employee.getManagerId() == null ? null : UUID.fromString(employee.getManagerId());
        scope.use(tenant);
        int receipt = entityManager.createNativeQuery("""
                INSERT INTO reporting.reporting_inbox(consumer_name,tenant_id,event_id,topic,aggregate_id,aggregate_revision)
                VALUES ('reporting-employee-v1',:tenant,:event,'org.employee.v1',:employee,:revision)
                ON CONFLICT DO NOTHING
                """).setParameter("tenant", tenant).setParameter("event", eventId)
                .setParameter("employee", aggregate).setParameter("revision", event.getAggregateVersion()).executeUpdate();
        if (receipt == 0) return;
        entityManager.createNativeQuery("""
                INSERT INTO reporting.employee_projection(tenant_id,employee_id,manager_id,active,revision)
                VALUES (:tenant,:employee,cast(:manager as uuid),:active,:revision)
                ON CONFLICT (tenant_id,employee_id) DO UPDATE SET manager_id=EXCLUDED.manager_id,
                    active=EXCLUDED.active,revision=EXCLUDED.revision
                WHERE reporting.employee_projection.revision<EXCLUDED.revision
                """).setParameter("tenant", tenant).setParameter("employee", aggregate).setParameter("manager", manager)
                .setParameter("active", "ACTIVE".equals(employee.getStatus()) && employee.getDeletedAt() == null)
                .setParameter("revision", event.getAggregateVersion()).executeUpdate();
    }
}
