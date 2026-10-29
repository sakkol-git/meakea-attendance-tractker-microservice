package com.hrflow.attendance.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Authenticated account identity is distinct from employee identity and current reporting lines. */
@Component
@RequiredArgsConstructor
public class CorrectionAccess {
    private final JdbcTemplate jdbc;

    public void requireDecision(UUID tenant, Caller caller, UUID employee) {
        if (caller.employee().equals(employee)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee cannot decide own correction");
        }
        if (caller.hrAdmin()) return;
        if (!caller.manager()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Correction decision denied");
        List<java.util.Map<String, Object>> rows = jdbc.queryForList("""
                SELECT manager_id,manager_known,status FROM attendance.employee_projection
                WHERE tenant_id=? AND employee_id=?
                """, tenant, employee);
        if (rows.size() != 1 || !Boolean.TRUE.equals(rows.getFirst().get("manager_known"))) {
            var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                    "Current reporting relationship is not available");
            problem.setProperty("code", "REFERENCE_NOT_READY");
            throw new ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE, problem, null);
        }
        var row = rows.getFirst();
        if (!"ACTIVE".equals(row.get("status")) || !caller.employee().equals(row.get("manager_id"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee is not a current direct report");
        }
    }

    public record Caller(UUID actor, UUID employee, boolean hrAdmin, boolean manager) {}
}
