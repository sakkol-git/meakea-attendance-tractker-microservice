package com.hrflow.notification.organization;

import com.hrflow.notification.service.TenantSqlScope;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationReferences {
    private final TenantSqlScope scope;
    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final ObjectProvider<NotificationEmployeeBootstrap> bootstrap;

    public Employee employee(UUID tenant, UUID employee) {
        ready(); scope.use(tenant);
        var rows = jdbc.query("SELECT employee_id,account_id,manager_id,active,roles::text FROM notification.employee_ref WHERE tenant_id=? AND employee_id=?",
                (row, index) -> map(row), tenant, employee);
        if (rows.isEmpty() || rows.getFirst().account() == null || rows.getFirst().roles() == null) throw unavailable();
        return rows.getFirst();
    }

    public List<Employee> hr(UUID tenant, UUID requester) {
        ready(); scope.use(tenant);
        return jdbc.query("""
                SELECT employee_id,account_id,manager_id,active,roles::text FROM notification.employee_ref
                WHERE tenant_id=? AND employee_id<>? AND active=true AND account_id IS NOT NULL AND roles @> '["HR_ADMIN"]'::jsonb
                ORDER BY employee_id
                """, (row, index) -> map(row), tenant, requester);
    }

    private Employee map(java.sql.ResultSet row) throws java.sql.SQLException {
        String roles = row.getString(5);
        return new Employee(row.getObject(1, UUID.class), row.getObject(2, UUID.class), row.getObject(3, UUID.class), row.getBoolean(4),
                roles == null ? null : json.readValue(roles, new TypeReference<List<String>>() {}));
    }
    private void ready() {
        var replay = bootstrap.getIfAvailable();
        if (replay == null || !replay.isReady()) throw unavailable();
    }
    private ResponseStatusException unavailable() { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Notification recipient authority is not available"); }
    public record Employee(UUID employee, UUID account, UUID manager, boolean active, List<String> roles) {}
}
