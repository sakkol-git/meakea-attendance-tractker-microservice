package com.hrflow.notification.organization;

import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.notification.service.TenantSqlScope;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class NotificationEmployeeConsumer {
    private final JdbcTemplate jdbc;
    private final TenantSqlScope scope;
    private final JsonMapper json;

    @Transactional
    public void apply(String key, OrganizationFact event) {
        if (event == null || event.getSchemaVersion() != 1 || event.getAggregateVersion() < 1
                || !Set.of("org.employee.created.v1", "org.employee.updated.v1", "org.employee.terminated.v1").contains(event.getType())
                || !(event.getPayload() instanceof EmployeeSnapshot employee)) throw new IllegalArgumentException("Unsupported notification employee reference");
        UUID tenant = UUID.fromString(event.getTenantId()), id = UUID.fromString(event.getAggregateId()), receipt = UUID.fromString(event.getEventId());
        UUID.fromString(event.getCorrelationId()); Instant.parse(event.getOccurredAt());
        if (!(tenant + ":" + id).equals(key) || !id.toString().equals(employee.getEmployeeId())
                || !Set.of("ACTIVE", "INACTIVE", "TERMINATED").contains(employee.getStatus())) throw new IllegalArgumentException("Employee reference routing/status mismatch");
        UUID account = employee.getUserId() == null ? null : UUID.fromString(employee.getUserId());
        UUID manager = employee.getManagerId() == null ? null : UUID.fromString(employee.getManagerId());
        var roles = employee.getAccountRoles();
        if (roles != null && (account == null || !Set.of("EMPLOYEE", "MANAGER", "HR_ADMIN", "OWNER").containsAll(roles)))
            throw new IllegalArgumentException("Unsupported or unlinked employee account roles");
        scope.use(tenant);
        int inserted = jdbc.update("INSERT INTO notification.notification_inbox(consumer_name,tenant_id,event_id,topic,aggregate_id) VALUES ('notification-organization',?,?,'org.employee.v1',?) ON CONFLICT DO NOTHING", tenant, receipt, id);
        if (inserted == 0) return;
        jdbc.update("""
                INSERT INTO notification.employee_ref(tenant_id,employee_id,account_id,manager_id,active,roles,revision)
                VALUES (?,?,?,?,?,?::jsonb,?) ON CONFLICT(tenant_id,employee_id) DO UPDATE SET account_id=EXCLUDED.account_id,
                  manager_id=EXCLUDED.manager_id,active=EXCLUDED.active,roles=EXCLUDED.roles,revision=EXCLUDED.revision
                WHERE notification.employee_ref.revision<EXCLUDED.revision
                """, tenant, id, account, manager, "ACTIVE".equals(employee.getStatus()) && employee.getDeletedAt() == null,
                roles == null ? null : json.writeValueAsString(roles), event.getAggregateVersion());
    }
}
