package com.hrflow.request.organization;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class RequestRelationships {
    private final JdbcTemplate jdbc;
    private final ObjectProvider<RequestEmployeeBootstrap> bootstrap;

    public Employee current(String tenant, String employee) {
        var replay = bootstrap.getIfAvailable();
        if (replay == null || !replay.isReady()) throw unknown();
        List<Employee> rows = jdbc.query("SELECT manager_id,active,revision FROM request.employee_projection WHERE tenant_id=? AND employee_id=?",
                (row, index) -> new Employee(row.getString(1), row.getBoolean(2), row.getLong(3)), tenant, employee);
        if (rows.isEmpty()) throw unknown();
        if (!rows.getFirst().active()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active employee required");
        return rows.getFirst();
    }

    public record Employee(String manager, boolean active, long revision) {}

    private ErrorResponseException unknown() {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Current employee relationship is not available");
        problem.setProperty("code", "REFERENCE_NOT_READY");
        return new ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE, problem, null);
    }
}
