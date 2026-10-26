package com.hrflow.gateway.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

class AccessTokenDecoderTest {

    private OAuth2TokenValidator<Jwt> validator;

    @BeforeEach
    void setUp() {
        validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("hrflow-local"),
                new JwtClaimValidator<List<String>>("aud",
                        audience -> audience != null && audience.contains("hrflow-clients")));
    }

    @Test
    void acceptsValidTokenWithExpectedIssuerAndAudience() {
        Jwt jwt = createJwt("hrflow-local", List.of("hrflow-clients"),
                Instant.now().minusSeconds(30), Instant.now().plusSeconds(300));
        assertFalse(validator.validate(jwt).hasErrors());
    }

    @Test
    void rejectsTokenWithWrongIssuer() {
        Jwt jwt = createJwt("untrusted-issuer", List.of("hrflow-clients"),
                Instant.now().minusSeconds(30), Instant.now().plusSeconds(300));
        assertTrue(validator.validate(jwt).hasErrors());
    }

    @Test
    void rejectsTokenWithWrongAudience() {
        Jwt jwt = createJwt("hrflow-local", List.of("unrelated-audience"),
                Instant.now().minusSeconds(30), Instant.now().plusSeconds(300));
        assertTrue(validator.validate(jwt).hasErrors());
    }

    @Test
    void rejectsExpiredToken() {
        Jwt jwt = createJwt("hrflow-local", List.of("hrflow-clients"),
                Instant.now().minusSeconds(600), Instant.now().minusSeconds(60));
        assertTrue(validator.validate(jwt).hasErrors());
    }

    @Test
    void rejectsNotYetValidToken() {
        Jwt jwt = createJwtWithNotBefore("hrflow-local", List.of("hrflow-clients"),
                Instant.now().plusSeconds(300), Instant.now().plusSeconds(600));
        assertTrue(validator.validate(jwt).hasErrors());
    }

    private Jwt createJwt(String issuer, List<String> audience, Instant issuedAt, Instant expiresAt) {
        return Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(audience)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    private Jwt createJwtWithNotBefore(String issuer, List<String> audience, Instant notBefore, Instant expiresAt) {
        return Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(audience)
                .notBefore(notBefore)
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(expiresAt)
                .build();
    }
}
