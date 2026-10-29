package com.hrflow.identityorg.organization.controller;

import com.hrflow.identityorg.organization.dto.ShiftInput;
import com.hrflow.identityorg.organization.dto.ShiftPatch;
import com.hrflow.identityorg.organization.dto.ShiftRecordV2;
import com.hrflow.identityorg.organization.service.ShiftService;
import jakarta.validation.Valid;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
public class ShiftController {
    private final ShiftService shifts;

    @GetMapping
    public Map<String, Object> list(@AuthenticationPrincipal Jwt jwt) {
        var rows = shifts.list(TenantIdentity.tenantId(jwt));
        return Map.of("items", rows, "page", 0, "size", rows.size(), "total", rows.size());
    }

    @PostMapping
    public ResponseEntity<ShiftRecordV2> create(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody ShiftInput request) {
        var row = shifts.create(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), key, request);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(etag(row)).body(row);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ShiftRecordV2> patch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ShiftPatch request) {
        var row = shifts.patch(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), id, ifMatch, request);
        return ResponseEntity.ok().eTag(etag(row)).body(row);
    }

    private String etag(ShiftRecordV2 row) { return '"' + Long.toString(row.version()) + '"'; }
}
