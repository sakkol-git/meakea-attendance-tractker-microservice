package com.hrflow.leave.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class LeaveAccess {
    private final JdbcTemplate jdbc;
    private final org.springframework.beans.factory.ObjectProvider<com.hrflow.leave.organization.LeaveEmployeeBootstrap> bootstrap;

    public void requireApprover(LeaveCaller caller) {
        if (!caller.hrAdmin() && !caller.manager()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Approver role required");
    }

    public void requireDecision(String tenant, LeaveCaller caller, String employee) {
        requireApprover(caller);
        if (employee.equals(caller.actor()) || employee.equals(caller.employee())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Self-approval is forbidden; independent approver required");
        }
        if (!caller.hrAdmin()) requireTeam(tenant, caller.employee(), employee);
    }

    public void requireRead(String tenant, LeaveCaller caller, String employee) {
        if (employee.equals(caller.employee()) || caller.hrAdmin()) return;
        if (caller.manager()) {
            try { requireTeam(tenant, caller.employee(), employee); return; }
            catch (ResponseStatusException denied) {
                if (denied.getStatusCode().value() != 403) throw denied;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found");
    }

    public String pendingScope(String tenant, LeaveCaller caller) {
        requireApprover(caller);
        if (caller.hrAdmin()) return null;
        requireActive(tenant, caller.employee());
        return caller.employee();
    }

    private void requireTeam(String tenant, String manager, String target) {
        requireActive(tenant, manager);
        var rows = jdbc.queryForList("SELECT manager_id,active FROM leave.employee_projection WHERE tenant_id=? AND employee_id=?", tenant, target);
        if (rows.isEmpty()) throw unknown();
        if (!Boolean.TRUE.equals(rows.getFirst().get("active")) || !manager.equals(rows.getFirst().get("manager_id"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current direct-report relationship required");
        }
    }

    private void requireActive(String tenant, String employee) {
        var replay = bootstrap.getIfAvailable();
        if (replay == null || !replay.isReady()) throw unknown();
        List<Boolean> rows = jdbc.queryForList("SELECT active FROM leave.employee_projection WHERE tenant_id=? AND employee_id=?", Boolean.class, tenant, employee);
        if (rows.isEmpty()) throw unknown();
        if (!rows.getFirst()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active manager required");
    }

    private ErrorResponseException unknown() {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Current employee relationship is not available");
        problem.setProperty("code", "REFERENCE_NOT_READY");
        return new ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE, problem, null);
    }
}
