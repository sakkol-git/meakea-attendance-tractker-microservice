package com.hrflow.identityorg.audit;

import com.hrflow.identityorg.exception.AuthFailedException;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuditController {
    private final IdentityAuditService audit;

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<AuditPage> search(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String target,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID tenant;
        try {
            tenant = UUID.fromString(jwt.getClaimAsString("tenantId"));
        } catch (RuntimeException invalid) {
            throw AuthFailedException.invalid("Invalid tenant claim");
        }
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(audit.search(tenant,
                new AuditQuery(page, size, actorId, entity, actor, action, target, from, to)));
    }
}
