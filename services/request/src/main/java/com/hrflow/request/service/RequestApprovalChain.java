package com.hrflow.request.service;

import com.hrflow.request.entity.ApprovalStep;
import com.hrflow.request.entity.GeneralRequest;
import com.hrflow.request.organization.RequestRelationships;
import com.hrflow.request.repository.ApprovalStepRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** User-approved UAT policy: current manager, then independently authorized tenant HR_ADMIN. */
@Component
@RequiredArgsConstructor
public class RequestApprovalChain {
    private final RequestRelationships relationships;
    private final ApprovalStepRepository steps;

    @Transactional(propagation = Propagation.MANDATORY)
    public void capture(GeneralRequest request) {
        String tenant = request.getTenantId(), employee = request.getEmployeeId();
        var requester = relationships.current(tenant, employee);
        String manager = requester.manager();
        if (employee.equals(manager)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Independent manager required; self-manager relationship must be corrected");
        }
        var assignedManager = manager == null ? null : relationships.current(tenant, manager);
        Instant now = Instant.now();
        var managerStep = ApprovalStep.builder().id(UUID.randomUUID().toString()).tenantId(tenant).requestId(request.getId())
                .stepOrder(1).approverId(manager).approverRole("MANAGER").requesterRevision(requester.revision())
                .approverRevision(assignedManager == null ? null : assignedManager.revision())
                .status(manager == null ? "SKIPPED" : "PENDING").createdAt(now)
                .decidedAt(manager == null ? now : null).decisionReason(manager == null ? "NO_MANAGER_ASSIGNED" : null).build();
        var hrStep = ApprovalStep.builder().id(UUID.randomUUID().toString()).tenantId(tenant).requestId(request.getId())
                .stepOrder(2).approverRole("HR_ADMIN").status("PENDING").requesterRevision(requester.revision()).createdAt(now).build();
        // Discard old untrusted draft suggestions only after all reference checks pass.
        steps.deleteByTenantIdAndRequestId(tenant, request.getId());
        steps.flush();
        steps.saveAll(List.of(managerStep, hrStep));
    }
}
