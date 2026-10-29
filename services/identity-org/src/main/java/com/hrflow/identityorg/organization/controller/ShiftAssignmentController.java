package com.hrflow.identityorg.organization.controller;

import com.hrflow.identityorg.organization.dto.ShiftAssignmentClose;
import com.hrflow.identityorg.organization.dto.ShiftAssignmentInput;
import com.hrflow.identityorg.organization.dto.ShiftAssignmentRecordV2;
import com.hrflow.identityorg.organization.service.ShiftAssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ShiftAssignmentController {
    private final ShiftAssignmentService assignments;

    @GetMapping("/shift-assignments")
    @PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
    public Map<String, Object> list(@AuthenticationPrincipal Jwt jwt) {
        return page(assignments.list(TenantIdentity.tenantId(jwt)));
    }

    @GetMapping("/employees/{id}/shifts")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> forEmployee(@AuthenticationPrincipal Jwt jwt,
            Authentication authentication, @PathVariable UUID id) {
        boolean admin = authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_OWNER".equals(authority.getAuthority()) || "ROLE_HR_ADMIN".equals(authority.getAuthority()));
        if (!admin && !id.toString().equals(jwt.getClaimAsString("employeeId"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return page(assignments.listForEmployee(TenantIdentity.tenantId(jwt), id));
    }

    @PostMapping("/employees/{id}/shifts")
    @PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
    public ResponseEntity<ShiftAssignmentRecordV2> assign(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody ShiftAssignmentInput request) {
        var row = assignments.assign(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), id, key, request);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(etag(row)).body(row);
    }

    @PostMapping("/shift-assignments/{id}/close")
    @PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
    public ResponseEntity<ShiftAssignmentRecordV2> close(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ShiftAssignmentClose request) {
        var row = assignments.close(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), id, ifMatch, request);
        return ResponseEntity.ok().eTag(etag(row)).body(row);
    }

    private Map<String, Object> page(List<ShiftAssignmentRecordV2> rows) {
        return Map.of("items", rows, "page", 0, "size", rows.size(), "total", rows.size());
    }

    private String etag(ShiftAssignmentRecordV2 row) { return '"' + Long.toString(row.version()) + '"'; }
}
