package com.hrflow.reporting.service;

import com.hrflow.reporting.entity.ReportingEmployee;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class ReportAccess {
    private final EntityManager entityManager;
    private final org.springframework.beans.factory.ObjectProvider<com.hrflow.reporting.consumer.ReportingEmployeeBootstrap> bootstrap;

    public void requireKnownManager(UUID tenant, UUID manager) {
        if (manager == null) return; // HR tenant scope was established from the authenticated caller.
        var replay = bootstrap.getIfAvailable();
        if (replay == null || !replay.isReady()) throw unavailable();
        var employee = entityManager.find(ReportingEmployee.class, new ReportingEmployee.Key(tenant, manager));
        if (employee == null) {
            throw unavailable();
        }
        if (!employee.isActive()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager is not active");
    }

    private static ErrorResponseException unavailable() {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Current team relationships are not available for reporting");
        problem.setProperty("code", "REFERENCE_NOT_READY");
        return new ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE, problem, null);
    }
}
