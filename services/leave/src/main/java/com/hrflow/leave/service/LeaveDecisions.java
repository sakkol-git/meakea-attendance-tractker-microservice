package com.hrflow.leave.service;

import com.hrflow.leave.dto.DecisionRequest;
import com.hrflow.leave.dto.LeaveRecordResponse;
import com.hrflow.leave.entity.LeaveLedger;
import com.hrflow.leave.entity.LeaveRequest;
import com.hrflow.leave.mapper.LeaveRequestMapper;
import com.hrflow.leave.repository.LeaveDayRepository;
import com.hrflow.leave.repository.LeaveLedgerRepository;
import com.hrflow.leave.repository.LeaveRequestRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@lombok.RequiredArgsConstructor
public class LeaveDecisions {
    private final LeaveLedgerRepository leaveLedgerRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveDayRepository leaveDayRepository;
    private final LeaveOutboxWriter leaveOutboxWriter;
    private final TenantSqlScope tenantSqlScope;
    private final LeaveAccess access;
    private final LeaveRequestMapper leaveRequestMapper;
    private final LeaveWorkCalendar calendar;
    private final com.hrflow.leave.accounting.LeaveAccountingModes accounting;

    @Transactional
    public LeaveRecordResponse approveLeaveRequest(String tenantId, String approverId,
                                                   Collection<String> roles, String id,
                                                   String ifMatch, DecisionRequest decision) {
        return approveLeaveRequest(tenantId, new LeaveCaller(approverId, approverId, roles), id, ifMatch, decision);
    }

    @Transactional
    public LeaveRecordResponse approveLeaveRequest(String tenantId, LeaveCaller caller, String id, String ifMatch, DecisionRequest decision) {
        String approverId = caller.actor();
        tenantSqlScope.use(tenantId);
        access.requireApprover(caller);

        LeaveRequest request = leaveRequestRepository.findForDecision(tenantId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found: " + id));

        access.requireDecision(tenantId, caller, request.getEmployeeId());

        RevisionPreconditions.require(ifMatch, request.getVersion());

        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is not in PENDING status: " + request.getStatus());
        }

        int year = request.getStartDate().getYear();
        String entry = accounting.forRequest(request.getPaidCharge()).apply(
                new com.hrflow.leave.accounting.LeaveAccounting.Account(tenantId, request.getEmployeeId(), request.getType(), year),
                com.hrflow.leave.accounting.LeaveAccounting.Action.APPROVE, request.getDays());

        // Update request
        request.setStatus("APPROVED");
        request.setApproverId(approverId);
        if (decision != null) {
            request.setDecisionReason(decision.reason());
            request.setDecisionNote(decision.note());
        }
        request.setVersion(request.getVersion() + 1);
        request.setUpdatedAt(Instant.now());
        leaveRequestRepository.saveAndFlush(request);

        // Record ledger
        LeaveLedger ledger = new LeaveLedger(
                UUID.randomUUID().toString(),
                tenantId,
                request.getEmployeeId(),
                request.getType(),
                year,
                entry,
                request.getDays(),
                id,
                decision != null ? decision.reason() : "Leave request approved"
        );
        leaveLedgerRepository.save(ledger);

        // Record outbox event
        leaveOutboxWriter.recordEvent(
                tenantId,
                "LEAVE_REQUEST",
                id,
                "org.leave.request.approved.v1",
                request,
                request.getVersion()
        );

        return leaveRequestMapper.toResponse(request);
    }

    @Transactional
    public LeaveRecordResponse rejectLeaveRequest(String tenantId, String approverId,
                                                  Collection<String> roles, String id,
                                                  String ifMatch, DecisionRequest decision) {
        return rejectLeaveRequest(tenantId, new LeaveCaller(approverId, approverId, roles), id, ifMatch, decision);
    }

    @Transactional
    public LeaveRecordResponse rejectLeaveRequest(String tenantId, LeaveCaller caller, String id, String ifMatch, DecisionRequest decision) {
        String approverId = caller.actor();
        tenantSqlScope.use(tenantId);
        access.requireApprover(caller);

        LeaveRequest request = leaveRequestRepository.findForDecision(tenantId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found: " + id));

        access.requireDecision(tenantId, caller, request.getEmployeeId());

        RevisionPreconditions.require(ifMatch, request.getVersion());

        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is not in PENDING status: " + request.getStatus());
        }

        int year = request.getStartDate().getYear();
        String entry = accounting.forRequest(request.getPaidCharge()).apply(
                new com.hrflow.leave.accounting.LeaveAccounting.Account(tenantId, request.getEmployeeId(), request.getType(), year),
                com.hrflow.leave.accounting.LeaveAccounting.Action.RELEASE, request.getDays());

        // Update request
        request.setStatus("REJECTED");
        request.setApproverId(approverId);
        if (decision != null) {
            request.setDecisionReason(decision.reason());
            request.setDecisionNote(decision.note());
        }
        request.setVersion(request.getVersion() + 1);
        request.setUpdatedAt(Instant.now());
        leaveRequestRepository.saveAndFlush(request);

        // Record ledger
        LeaveLedger ledger = new LeaveLedger(
                UUID.randomUUID().toString(),
                tenantId,
                request.getEmployeeId(),
                request.getType(),
                year,
                entry,
                request.getDays(),
                id,
                decision != null ? decision.reason() : "Leave request rejected"
        );
        leaveLedgerRepository.save(ledger);

        // Record outbox event
        leaveOutboxWriter.recordEvent(
                tenantId,
                "LEAVE_REQUEST",
                id,
                "org.leave.request.rejected.v1",
                request,
                request.getVersion()
        );

        // Capture the full owner dates in the terminal intent before releasing date reservations.
        leaveDayRepository.deleteByTenantIdAndRequestId(tenantId, id);

        return leaveRequestMapper.toResponse(request);
    }

    @Transactional
    public LeaveRecordResponse cancelLeaveRequest(String tenantId, String employeeId,
                                                  String id, String ifMatch, String reason) {
        tenantSqlScope.use(tenantId);

        LeaveRequest request = leaveRequestRepository.findForDecision(tenantId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found: " + id));

        if (!employeeId.equals(request.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the requester can cancel their leave request");
        }

        RevisionPreconditions.require(ifMatch, request.getVersion());

        String previousStatus = request.getStatus();
        if (!"PENDING".equals(previousStatus) && !"APPROVED".equals(previousStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel request in status: " + previousStatus);
        }

        if ("APPROVED".equals(previousStatus)) {
            var today = calendar.resolve(tenantId, employeeId, request.getStartDate(), request.getEndDate()).today();
            var reserved = leaveDayRepository.findByTenantIdAndRequestId(tenantId, id);
            if (reserved.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Original reserved dates are unavailable");
            if (reserved.stream().anyMatch(day -> !day.getDate().isAfter(today))) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Only future approved leave can be cancelled as a whole request");
            }
        }

        int year = request.getStartDate().getYear();
        var action = "PENDING".equals(previousStatus) ? com.hrflow.leave.accounting.LeaveAccounting.Action.RELEASE
                : com.hrflow.leave.accounting.LeaveAccounting.Action.CANCEL_APPROVED;
        String entryType = accounting.forRequest(request.getPaidCharge()).apply(
                new com.hrflow.leave.accounting.LeaveAccounting.Account(tenantId, employeeId, request.getType(), year), action, request.getDays());

        // Update request
        request.setStatus("CANCELLED");
        request.setDecisionReason(reason);
        request.setVersion(request.getVersion() + 1);
        request.setUpdatedAt(Instant.now());
        leaveRequestRepository.saveAndFlush(request);

        // Record ledger
        LeaveLedger ledger = new LeaveLedger(
                UUID.randomUUID().toString(),
                tenantId,
                employeeId,
                request.getType(),
                year,
                entryType,
                request.getDays(),
                id,
                reason != null ? reason : "Leave request cancelled"
        );
        leaveLedgerRepository.save(ledger);

        // Record outbox event
        leaveOutboxWriter.recordEvent(
                tenantId,
                "LEAVE_REQUEST",
                id,
                "org.leave.request.cancelled.v1",
                request,
                request.getVersion()
        );

        // Capture the full owner dates in the terminal intent before releasing date reservations.
        leaveDayRepository.deleteByTenantIdAndRequestId(tenantId, id);

        return leaveRequestMapper.toResponse(request);
    }

}
