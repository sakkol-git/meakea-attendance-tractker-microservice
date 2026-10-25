package com.hrflow.identityorg.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AccessTokens {
    private final JwtEncoder encoder;
    private final RSAKey key;
    private final String issuer;

    public AccessTokens(JwtEncoder encoder, RSAKey key,
                        @Value("${hrflow.auth.issuer:hrflow-local}") String issuer) {
        this.encoder = encoder;
        this.key = key;
        this.issuer = issuer;
    }

    String issue(AuthUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer).audience(List.of("hrflow-clients"))
                .issuedAt(now).expiresAt(now.plusSeconds(900))
                .subject(user.id.toString())
                .claim("tenantId", user.tenantId.toString())
                .claim("employeeId", user.employeeId.toString())
                .claim("roles", Arrays.asList(user.roles.split(",")))
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build(), claims))
                .getTokenValue();
    }

    Map<String, Object> profile(AuthUser user) {
        return Map.of("userId", user.id.toString(), "employeeId", user.employeeId.toString(),
                "tenantId", user.tenantId.toString(), "roles", Arrays.asList(user.roles.split(",")),
                "name", user.displayName, "email", user.email, "timeZone", user.timeZone,
                "locale", "en");
    }

    boolean matches(Jwt jwt, AuthUser user) {
        return jwt != null && user.id.toString().equals(jwt.getSubject())
                && user.tenantId.toString().equals(jwt.getClaimAsString("tenantId"))
                && jwt.getAudience().contains("hrflow-clients") && user.enabled;
    }

    public Map<String, Object> jwks() {
        return new JWKSet(key.toPublicJWK()).toJSONObject();
    }
}
