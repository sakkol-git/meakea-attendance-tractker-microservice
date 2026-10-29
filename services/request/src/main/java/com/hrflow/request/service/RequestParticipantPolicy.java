package com.hrflow.request.service;

import com.hrflow.request.entity.ApprovalStep;
import com.hrflow.request.entity.GeneralRequest;
import com.hrflow.request.repository.ApprovalStepRepository;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RequestParticipantPolicy {

    private final ApprovalStepRepository approvalStepRepository;
    private final com.hrflow.request.organization.RequestRelationships relationships;
    private final com.hrflow.request.repository.GeneralRequestRepository requests;

    public RequestParticipantPolicy(ApprovalStepRepository approvalStepRepository,
            com.hrflow.request.organization.RequestRelationships relationships,
            com.hrflow.request.repository.GeneralRequestRepository requests) {
        this.approvalStepRepository = approvalStepRepository;
        this.relationships = relationships;
        this.requests = requests;
    }

    public void requireParticipant(String tenantId, String actorId, Set<String> actorRoles, GeneralRequest request) {
        if (request.getEmployeeId().equals(actorId)) {
            return;
        }
        if (isHr(actorRoles)) {
            return;
        }
        List<ApprovalStep> steps = approvalStepRepository.findByTenantIdAndRequestIdOrderByStepOrderAsc(tenantId, request.getId());
        boolean isStepApprover = steps.stream().anyMatch(step -> canDecide(step, actorId, actorRoles));
        if (isStepApprover) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: actor is not a participant in this request");
    }

    public boolean canDecide(ApprovalStep step, String employee, Set<String> roles) {
        boolean holdsRole = roles != null && roles.stream().anyMatch(role ->
                role.equalsIgnoreCase(step.getApproverRole()) || role.equalsIgnoreCase("ROLE_" + step.getApproverRole()));
        if (!holdsRole) return false;
        // Only the accepted tenant HR stage is shared; manager/assigned stages are individual.
        if ("HR_ADMIN".equalsIgnoreCase(step.getApproverRole())) return true;
        if (!employee.equals(step.getApproverId())) return false;
        if ("MANAGER".equalsIgnoreCase(step.getApproverRole())) {
            relationships.current(step.getTenantId(), employee);
            var request = requests.findByTenantIdAndId(step.getTenantId(), step.getRequestId()).orElseThrow();
            return employee.equals(relationships.current(step.getTenantId(), request.getEmployeeId()).manager());
        }
        return true;
    }

    public boolean isHr(Set<String> roles) {
        return roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("HR_ADMIN") || r.equalsIgnoreCase("ROLE_HR_ADMIN"));
    }
}
