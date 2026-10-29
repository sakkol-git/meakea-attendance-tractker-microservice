package com.hrflow.request.service;

import com.hrflow.request.dto.ApprovalStepDto;
import com.hrflow.request.dto.DecisionRequest;
import com.hrflow.request.dto.FulfillRequest;
import com.hrflow.request.dto.GeneralRequestResponse;
import com.hrflow.request.entity.ApprovalStep;
import com.hrflow.request.entity.GeneralRequest;
import com.hrflow.request.repository.ApprovalStepRepository;
import com.hrflow.request.repository.GeneralRequestRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@lombok.RequiredArgsConstructor
@Transactional
public class RequestWorkflowService {

    private final GeneralRequestRepository generalRequestRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final RequestOutboxWriter outboxWriter;
    private final TenantSqlScope tenantSqlScope;
    private final RequestParticipantPolicy participantPolicy;
    private final RequestApprovalChain chain;

    public void submitRequest(String tenantId, String employeeId, String requestId, List<ApprovalStepDto> overrideApprovers, String ifMatch) {
        submitRequest(tenantId, new RequestCaller(employeeId, employeeId, Set.of()), requestId, ifMatch);
    }

    public void submitRequest(String tenantId, RequestCaller caller, String requestId, String ifMatch) {
        String employeeId = caller.employee();
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findForUpdate(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!request.getEmployeeId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only requester can submit request");
        }
        RevisionPreconditions.require(ifMatch, request.getVersion());
        if (!"DRAFT".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is already submitted or closed");
        }

        chain.capture(request);

        request.setStatus("PENDING_APPROVAL");
        request.setVersion(request.getVersion() + 1);
        request.setUpdatedAt(Instant.now());
        generalRequestRepository.save(request);

        outboxWriter.snapshot(tenantId, requestId, "GeneralRequestSubmitted", caller.account());
    }

    public void approveRequestStep(String tenantId, String actorId, Set<String> actorRoles, String requestId, DecisionRequest decision, String ifMatch) {
        approveRequestStep(tenantId, new RequestCaller(actorId, actorId, actorRoles), requestId, decision, ifMatch);
    }

    public void approveRequestStep(String tenantId, RequestCaller caller, String requestId, DecisionRequest decision, String ifMatch) {
        String actorId = caller.employee();
        Set<String> actorRoles = caller.roles();
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findForUpdate(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        RevisionPreconditions.require(ifMatch, request.getVersion());

        if (!"PENDING_APPROVAL".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is not in PENDING_APPROVAL state: " + request.getStatus());
        }

        if (caller.owns(request.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requester cannot approve own request");
        }

        List<ApprovalStep> steps = approvalStepRepository.findByTenantIdAndRequestIdOrderByStepOrderAsc(tenantId, requestId);
        ApprovalStep activeStep = steps.stream()
                .filter(s -> "PENDING".equals(s.getStatus()))
                .min(Comparator.comparingInt(ApprovalStep::getStepOrder))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No pending approval step"));

        if (!participantPolicy.canDecide(activeStep, actorId, actorRoles)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Actor is not authorized for the current approval step");
        }

        activeStep.recordDecision("APPROVED", decision != null ? decision.effectiveReason() : null, caller.account(), Instant.now());
        approvalStepRepository.save(activeStep);

        boolean morePending = steps.stream().anyMatch(s -> s.getStepOrder() > activeStep.getStepOrder() && "PENDING".equals(s.getStatus()));
        if (!morePending) {
            request.setStatus("APPROVED");
        }
        request.setVersion(request.getVersion() + 1);
        request.setUpdatedAt(Instant.now());
        generalRequestRepository.save(request);

        outboxWriter.snapshot(tenantId, requestId, "GeneralRequestStepApproved", caller.account());
    }

    public void rejectRequestStep(String tenantId, String actorId, Set<String> actorRoles, String requestId, DecisionRequest decision, String ifMatch) {
        rejectRequestStep(tenantId, new RequestCaller(actorId, actorId, actorRoles), requestId, decision, ifMatch);
    }

    public void rejectRequestStep(String tenantId, RequestCaller caller, String requestId, DecisionRequest decision, String ifMatch) {
        String actorId = caller.employee();
        Set<String> actorRoles = caller.roles();
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findForUpdate(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        RevisionPreconditions.require(ifMatch, request.getVersion());

        if (!"PENDING_APPROVAL".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is not in PENDING_APPROVAL state: " + request.getStatus());
        }

        if (decision == null || decision.effectiveReason() == null || decision.effectiveReason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Rejection reason is mandatory");
        }

        if (caller.owns(request.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requester cannot reject own request");
        }

        List<ApprovalStep> steps = approvalStepRepository.findByTenantIdAndRequestIdOrderByStepOrderAsc(tenantId, requestId);
        ApprovalStep activeStep = steps.stream()
                .filter(s -> "PENDING".equals(s.getStatus()))
                .min(Comparator.comparingInt(ApprovalStep::getStepOrder))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No pending approval step"));

        if (!participantPolicy.canDecide(activeStep, actorId, actorRoles)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Actor is not authorized for the current approval step");
        }

        activeStep.recordDecision("REJECTED", decision.effectiveReason().trim(), caller.account(), Instant.now());
        approvalStepRepository.save(activeStep);

        request.setStatus("REJECTED");
        request.setVersion(request.getVersion() + 1);
        request.setUpdatedAt(Instant.now());
        generalRequestRepository.save(request);

        outboxWriter.snapshot(tenantId, requestId, "GeneralRequestRejected", caller.account());
    }

    public void cancelRequest(String tenantId, String actorId, Set<String> actorRoles, String requestId, String ifMatch) {
        cancelRequest(tenantId, new RequestCaller(actorId, actorId, actorRoles), requestId, ifMatch);
    }

    public void cancelRequest(String tenantId, RequestCaller caller, String requestId, String ifMatch) {
        String actorId = caller.employee();
        Set<String> actorRoles = caller.roles();
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findForUpdate(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        RevisionPreconditions.require(ifMatch, request.getVersion());

        if (!request.getEmployeeId().equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only requester can cancel their request");
        }

        if ("APPROVED".equals(request.getStatus()) || "REJECTED".equals(request.getStatus())
                || "CANCELLED".equals(request.getStatus()) || "FULFILLED".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel request in settled state: " + request.getStatus());
        }

        request.setStatus("CANCELLED");
        request.setVersion(request.getVersion() + 1);
        request.setUpdatedAt(Instant.now());
        generalRequestRepository.save(request);

        outboxWriter.snapshot(tenantId, requestId, "GeneralRequestCancelled", caller.account());
    }

    public void fulfillApprovedMaterial(String tenantId, String actorId, Set<String> actorRoles, String requestId, FulfillRequest fulfill, String ifMatch) {
        fulfillApprovedMaterial(tenantId, new RequestCaller(actorId, actorId, actorRoles), requestId, fulfill, ifMatch);
    }

    public void fulfillApprovedMaterial(String tenantId, RequestCaller caller, String requestId, FulfillRequest fulfill, String ifMatch) {
        String actorId = caller.employee();
        Set<String> actorRoles = caller.roles();
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findForUpdate(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        RevisionPreconditions.require(ifMatch, request.getVersion());

        boolean isHrOrAdmin = participantPolicy.isHr(actorRoles);
        if (!isHrOrAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only HR_ADMIN can fulfill material handover");
        }

        if (!"APPROVED".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only APPROVED requests can be fulfilled; current status: " + request.getStatus());
        }

        if (!"MATERIAL".equals(request.getType())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Only MATERIAL requests have a handover");
        }

        if (fulfill == null || fulfill.effectiveReason() == null || fulfill.effectiveReason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Handover notes/reason are required");
        }

        request.setHandoverNotes(fulfill.effectiveReason().trim());
        request.setStatus("FULFILLED");
        request.setVersion(request.getVersion() + 1);
        request.setUpdatedAt(Instant.now());
        generalRequestRepository.save(request);

        outboxWriter.snapshot(tenantId, requestId, "GeneralRequestFulfilled", caller.account());
    }
}
