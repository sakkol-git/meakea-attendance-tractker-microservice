package com.hrflow.identityorg.organization.controller;

import com.hrflow.identityorg.organization.dto.EmployeeCreateRequest;
import com.hrflow.identityorg.organization.dto.EmployeePageV2;
import com.hrflow.identityorg.organization.dto.EmployeePatchRequest;
import com.hrflow.identityorg.organization.dto.EmployeeRecordV2;
import com.hrflow.identityorg.organization.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
public class EmployeeController {
    private final EmployeeService employees;

    @GetMapping
    public EmployeePageV2 list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return employees.list(TenantIdentity.tenantId(jwt), page, size, search);
    }

    @PostMapping
    public ResponseEntity<EmployeeRecordV2> create(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeRecordV2 row = employees.create(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), key,
                request);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(etag(row)).body(row);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeRecordV2> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        EmployeeRecordV2 row = employees.get(TenantIdentity.tenantId(jwt), id);
        return ResponseEntity.ok().eTag(etag(row)).body(row);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EmployeeRecordV2> patch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody EmployeePatchRequest request) {
        EmployeeRecordV2 row = employees.patch(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt),
                id, ifMatch, request);
        return ResponseEntity.ok().eTag(etag(row)).body(row);
    }

    private String etag(EmployeeRecordV2 row) {
        return '"' + Long.toString(row.version()) + '"';
    }
}
