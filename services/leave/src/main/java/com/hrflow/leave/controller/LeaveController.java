package com.hrflow.leave.controller;

import com.hrflow.leave.dto.CreateLeaveRequestDto;
import com.hrflow.leave.dto.DecisionRequest;
import com.hrflow.leave.dto.LeaveBalanceResponse;
import com.hrflow.leave.dto.LeavePreviewResponse;
import com.hrflow.leave.dto.LeaveRecordResponse;
import com.hrflow.leave.dto.LeaveTypeResponse;
import com.hrflow.leave.service.CommandReceipts;
import com.hrflow.leave.service.LeaveQueries;
import com.hrflow.leave.service.LeaveSubmissionService;
import com.hrflow.leave.service.LeaveDecisions;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@lombok.RequiredArgsConstructor
@RequestMapping("/leave")
@PreAuthorize("isAuthenticated()")
public class LeaveController {

    private final LeaveQueries queries;
    private final LeaveSubmissionService submissions;
    private final LeaveDecisions decisions;
    private final CommandReceipts commandReceipts;
    private final com.hrflow.leave.service.LeaveRequestPages pages;


    @GetMapping("/types")
    public ResponseEntity<?> listLeaveTypes(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer page, @RequestParam(defaultValue = "100") int size) {
        String tenant = tenantId(jwt);
        return ResponseEntity.ok(page == null ? queries.listLeaveTypes(tenant) : queries.typePage(tenant, page, size));
    }

    @GetMapping("/balances/me")
    public ResponseEntity<?> getMyBalances(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer page, @RequestParam(defaultValue = "100") int size) {
        String tenant = tenantId(jwt);
        String employee = employeeId(jwt);
        return ResponseEntity.ok(page == null ? queries.getMyBalances(tenant, employee, year)
                : queries.balancePage(tenant, employee, year, page, size));
    }

    @PostMapping("/requests/preview")
    public ResponseEntity<LeavePreviewResponse> previewLeaveRequest(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateLeaveRequestDto dto) {
        String tenant = tenantId(jwt);
        String employee = employeeId(jwt);
        return ResponseEntity.ok(submissions.previewLeaveRequest(tenant, employee, dto));
    }

    @PostMapping("/requests")
    public ResponseEntity<LeaveRecordResponse> submitLeaveRequest(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid CreateLeaveRequestDto dto) {
        String tenant = tenantId(jwt);
        String employee = employeeId(jwt);
        String employeeName = employeeName(jwt);

        LeaveRecordResponse response = commandReceipts.execute(
                tenant,
                actorId(jwt),
                "/leave/requests",
                idempotencyKey,
                dto,
                LeaveRecordResponse.class,
                () -> submissions.submitLeaveRequest(tenant, employee, employeeName, dto)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag('"' + Long.toString(response.version()) + '"')
                .body(response);
    }

    @GetMapping("/requests/me")
    public ResponseEntity<?> getMyRequests(@AuthenticationPrincipal Jwt jwt,
            @org.springframework.web.bind.annotation.ModelAttribute com.hrflow.leave.dto.LeaveListQuery filter) {
        String tenant = tenantId(jwt);
        String employee = employeeId(jwt);
        return ResponseEntity.ok(filter.page() == null ? queries.getMyRequests(tenant, employee)
                : pages.read(tenant, caller(jwt), false, filter));
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<?> getPendingRequests(@AuthenticationPrincipal Jwt jwt,
            @org.springframework.web.bind.annotation.ModelAttribute com.hrflow.leave.dto.LeaveListQuery filter) {
        String tenant = tenantId(jwt);
        return ResponseEntity.ok(filter.page() == null ? queries.getPendingRequests(tenant, caller(jwt))
                : pages.read(tenant, caller(jwt), true, filter));
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<LeaveRecordResponse> getRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        String tenant = tenantId(jwt);
        LeaveRecordResponse response = queries.getRequest(tenant, caller(jwt), id);
        return ResponseEntity.ok()
                .eTag('"' + Long.toString(response.version()) + '"')
                .body(response);
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<LeaveRecordResponse> approveRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestBody(required = false) DecisionRequest decision) {
        String tenant = tenantId(jwt);
        LeaveRecordResponse response = commandReceipts.execute(tenant, actorId(jwt), "/leave/requests/" + id + "/approve",
                key, decision, LeaveRecordResponse.class,
                () -> decisions.approveLeaveRequest(tenant, caller(jwt), id, ifMatch, decision));
        return ResponseEntity.ok()
                .eTag('"' + Long.toString(response.version()) + '"')
                .body(response);
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<LeaveRecordResponse> rejectRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestBody(required = false) DecisionRequest decision) {
        String tenant = tenantId(jwt);
        LeaveRecordResponse response = commandReceipts.execute(tenant, actorId(jwt), "/leave/requests/" + id + "/reject",
                key, decision, LeaveRecordResponse.class,
                () -> decisions.rejectLeaveRequest(tenant, caller(jwt), id, ifMatch, decision));
        return ResponseEntity.ok()
                .eTag('"' + Long.toString(response.version()) + '"')
                .body(response);
    }

    @PostMapping("/requests/{id}/cancel")
    public ResponseEntity<LeaveRecordResponse> cancelRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestBody(required = false) DecisionRequest decision) {
        String tenant = tenantId(jwt);
        String employee = employeeId(jwt);
        String reason = decision != null ? decision.reason() : null;

        LeaveRecordResponse response = commandReceipts.execute(tenant, actorId(jwt), "/leave/requests/" + id + "/cancel",
                key, decision, LeaveRecordResponse.class,
                () -> decisions.cancelLeaveRequest(tenant, employee, id, ifMatch, reason));
        return ResponseEntity.ok()
                .eTag('"' + Long.toString(response.version()) + '"')
                .body(response);
    }

    private String tenantId(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication token");
        }
        String claim = jwt.getClaimAsString("tenantId");
        if (claim == null || claim.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing tenant claim");
        }
        return claim;
    }

    private String actorId(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid actor subject");
        }
        return sub;
    }

    private String employeeId(Jwt jwt) {
        String claim = jwt.getClaimAsString("employeeId");
        if (claim != null && !claim.isBlank()) {
            return claim;
        }
        return actorId(jwt);
    }

    private String employeeName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        return name != null ? name : "Employee";
    }

    private Collection<String> roles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null ? roles : List.of();
    }

    private com.hrflow.leave.service.LeaveCaller caller(Jwt jwt) {
        return new com.hrflow.leave.service.LeaveCaller(actorId(jwt), employeeId(jwt), roles(jwt));
    }
}
