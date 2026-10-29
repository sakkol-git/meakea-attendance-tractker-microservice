package com.hrflow.identityorg.organization.controller;

import com.hrflow.identityorg.organization.dto.NamedCreateRequest;
import com.hrflow.identityorg.organization.dto.NamedRecord;
import com.hrflow.identityorg.organization.service.CatalogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
public class CatalogController {
    private final CatalogService catalogs;

    @GetMapping("/departments")
    public Map<String, Object> departments(@AuthenticationPrincipal Jwt jwt) {
        return page(catalogs.departments(TenantIdentity.tenantId(jwt)));
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    public NamedRecord createDepartment(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody NamedCreateRequest request) {
        return catalogs.createDepartment(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), key, request);
    }

    @GetMapping("/positions")
    public Map<String, Object> positions(@AuthenticationPrincipal Jwt jwt) {
        return page(catalogs.positions(TenantIdentity.tenantId(jwt)));
    }

    @PostMapping("/positions")
    @ResponseStatus(HttpStatus.CREATED)
    public NamedRecord createPosition(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody NamedCreateRequest request) {
        return catalogs.createPosition(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), key, request);
    }

    private Map<String, Object> page(List<NamedRecord> rows) {
        return Map.of("items", rows, "page", 0, "size", rows.size(), "total", rows.size());
    }
}
