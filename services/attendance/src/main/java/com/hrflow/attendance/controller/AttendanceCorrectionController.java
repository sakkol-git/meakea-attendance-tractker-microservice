package com.hrflow.attendance.controller;

import com.hrflow.attendance.dto.CorrectionRequest;
import com.hrflow.attendance.dto.CorrectionResponse;
import com.hrflow.attendance.service.AttendanceCorrectionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/attendance/corrections")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AttendanceCorrectionController {

    private final AttendanceCorrectionService service;
    private final com.hrflow.attendance.service.CorrectionQueries queries;

    @PostMapping
    public ResponseEntity<CorrectionResponse> requestCorrection(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CorrectionRequest request) {
        UUID tenantId = tenantId(jwt);
        UUID actorId = actorId(jwt);
        UUID employeeId = employeeId(jwt);
        CorrectionResponse response = service.requestCorrection(tenantId, actorId, employeeId, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(etag(response)).body(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<CorrectionResponse> approveCorrection(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch) {
        UUID tenantId = tenantId(jwt);
        UUID actorId = actorId(jwt);
        CorrectionResponse response = service.approveCorrection(tenantId, caller(jwt), id, ifMatch);
        return ResponseEntity.ok().eTag(etag(response)).body(response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<CorrectionResponse> rejectCorrection(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch) {
        UUID tenantId = tenantId(jwt);
        UUID actorId = actorId(jwt);
        CorrectionResponse response = service.rejectCorrection(tenantId, caller(jwt), id, ifMatch);
        return ResponseEntity.ok().eTag(etag(response)).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CorrectionResponse> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var response = queries.detail(tenantId(jwt), caller(jwt), id);
        return ResponseEntity.ok().eTag(etag(response)).body(response);
    }

    @GetMapping
    public com.hrflow.attendance.dto.CorrectionPage listCorrections(@AuthenticationPrincipal Jwt jwt,
            com.hrflow.attendance.dto.CorrectionListQuery query) {
        return queries.page(tenantId(jwt), caller(jwt), query);
    }

    private UUID tenantId(Jwt jwt) {
        String claim = jwt.getClaimAsString("tenantId");
        if (claim == null || claim.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing tenant claim");
        }
        return UUID.fromString(claim);
    }

    private com.hrflow.attendance.service.CorrectionAccess.Caller caller(Jwt jwt) {
        return new com.hrflow.attendance.service.CorrectionAccess.Caller(actorId(jwt), employeeId(jwt),
                hasAnyRole(jwt, "HR_ADMIN"), hasAnyRole(jwt, "MANAGER"));
    }

    private UUID actorId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid actor subject");
        }
    }

    private UUID employeeId(Jwt jwt) {
        String claim = jwt.getClaimAsString("employeeId");
        if (claim != null && !claim.isBlank()) {
            return UUID.fromString(claim);
        }
        return actorId(jwt);
    }

    private boolean hasAnyRole(Jwt jwt, String... roles) {
        List<String> userRoles = jwt.getClaimAsStringList("roles");
        if (userRoles == null) return false;
        for (String r : roles) {
            if (userRoles.contains(r)) return true;
        }
        return false;
    }

    private String etag(CorrectionResponse response) {
        return '"' + Long.toString(response.version()) + '"';
    }
}
