package com.hrflow.identityorg.organization.controller;

import com.hrflow.identityorg.exception.AuthFailedException;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

final class TenantIdentity {
    private TenantIdentity() {}

    static UUID tenantId(Jwt jwt) {
        if (jwt == null) throw AuthFailedException.invalid("Invalid JWT");
        try {
            return UUID.fromString(jwt.getClaimAsString("tenantId"));
        } catch (RuntimeException invalid) {
            throw AuthFailedException.invalid("Invalid tenant claim");
        }
    }

    static UUID actorId(Jwt jwt) {
        if (jwt == null) throw AuthFailedException.invalid("Invalid JWT");
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException invalid) {
            throw AuthFailedException.invalid("Invalid subject claim");
        }
    }
}
