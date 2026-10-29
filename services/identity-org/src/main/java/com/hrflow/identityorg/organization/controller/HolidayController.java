package com.hrflow.identityorg.organization.controller;

import com.hrflow.identityorg.organization.dto.HolidayInput;
import com.hrflow.identityorg.organization.dto.HolidayPatch;
import com.hrflow.identityorg.organization.dto.HolidayRecordV2;
import com.hrflow.identityorg.organization.service.HolidayService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/holidays")
@RequiredArgsConstructor
public class HolidayController {
    private final HolidayService holidays;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> list(@AuthenticationPrincipal Jwt jwt) {
        var rows = holidays.list(TenantIdentity.tenantId(jwt));
        return Map.of("items", rows, "page", 0, "size", rows.size(), "total", rows.size());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
    public ResponseEntity<HolidayRecordV2> create(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody HolidayInput request) {
        var row = holidays.create(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), key, request);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(etag(row)).body(row);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch) {
        holidays.delete(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), id, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
    public ResponseEntity<HolidayRecordV2> patch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody HolidayPatch request) {
        var row = holidays.patch(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), id, ifMatch, request);
        return ResponseEntity.ok().eTag(etag(row)).body(row);
    }

    private String etag(HolidayRecordV2 row) { return '"' + Long.toString(row.version()) + '"'; }
}
