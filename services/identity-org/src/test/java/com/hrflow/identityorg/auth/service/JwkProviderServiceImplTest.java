package com.hrflow.identityorg.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hrflow.identityorg.auth.config.SigningKeys;
import com.hrflow.identityorg.profile.entity.Tenant;
import com.hrflow.identityorg.profile.entity.User;
import com.hrflow.identityorg.auth.service.impl.JwkProviderServiceImpl;
import com.hrflow.identityorg.auth.service.record.AuthTokenPolicy;
import com.nimbusds.jose.jwk.RSAKey;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JwkProviderServiceImplTest {
    @Test
    void issuesCanonicalTenantEmployeeRoleAndJwtIdClaims(@TempDir Path directory) throws Exception {
        SigningKeys keys = new SigningKeys();
        RSAKey key = keys.rsaKey(directory.toString(), true);
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        JwkProviderServiceImpl tokens = new JwkProviderServiceImpl(keys.jwtEncoder(key), key,
                Clock.fixed(now, ZoneOffset.UTC), "hrflow-local");
        User user = new User(new Tenant("demo"), "owner@example.test", "hash", "Owner");

        var jwt = keys.jwtDecoder(key, "hrflow-local").decode(tokens.issue(user));

        assertEquals(user.getTenantId().toString(), jwt.getClaimAsString("tenantId"));
        assertEquals(user.getEmployeeId().toString(), jwt.getClaimAsString("employeeId"));
        assertEquals(List.of("EMPLOYEE", "OWNER"), jwt.getClaimAsStringList("roles"));
        assertNotNull(jwt.getId());
        assertEquals(now.plus(AuthTokenPolicy.ACCESS_LIFETIME), jwt.getExpiresAt());
    }

    @Test
    void issuesTheConfiguredIssuer(@TempDir Path directory) throws Exception {
        SigningKeys keys = new SigningKeys();
        RSAKey key = keys.rsaKey(directory.toString(), true);
        JwkProviderServiceImpl tokens = new JwkProviderServiceImpl(keys.jwtEncoder(key), key,
                Clock.systemUTC(), "https://identity.example.test");
        User user = new User(new Tenant("demo"), "owner@example.test", "hash", "Owner");

        var jwt = keys.jwtDecoder(key, "https://identity.example.test").decode(tokens.issue(user));

        assertEquals("https://identity.example.test", jwt.getIssuer().toString());
    }
}
