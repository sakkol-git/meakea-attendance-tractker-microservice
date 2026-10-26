package com.hrflow.identityorg.auth.service.impl;

import com.hrflow.identityorg.profile.entity.User;
import com.hrflow.identityorg.auth.service.JwkProviderService;
import com.hrflow.identityorg.auth.service.record.AuthRoles;
import com.hrflow.identityorg.auth.service.record.AuthTokenPolicy;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwkProviderServiceImpl implements JwkProviderService {
    private final JwtEncoder encoder;
    private final RSAKey key;
    private final Clock clock;
    private final String issuer;

    public JwkProviderServiceImpl(JwtEncoder encoder, RSAKey key, Clock clock,
                                  @Value("${hrflow.auth.issuer:hrflow-local}") String issuer) {
        this.encoder = encoder;
        this.key = key;
        this.clock = clock;
        this.issuer = issuer;
    }

    @Override
    public String issue(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer).audience(List.of(AuthTokenPolicy.AUDIENCE))
                .issuedAt(now).expiresAt(now.plus(AuthTokenPolicy.ACCESS_LIFETIME))
                .id(java.util.UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("tenantId", user.getTenantId().toString())
                .claim("employeeId", user.getEmployeeId().toString())
                .claim("roles", AuthRoles.parse(user.getRoles()))
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build(), claims))
                .getTokenValue();
    }

    @Override
    public Map<String, Object> jwks() {
        return new JWKSet(key.toPublicJWK()).toJSONObject();
    }

    @Override
    public boolean matches(Jwt jwt, User user) {
        return jwt != null && user != null && user.getId().toString().equals(jwt.getSubject())
                && user.getTenantId().toString().equals(jwt.getClaimAsString("tenantId"))
                && user.getEmployeeId().toString().equals(jwt.getClaimAsString("employeeId"))
                && jwt.getAudience() != null && jwt.getAudience().contains(AuthTokenPolicy.AUDIENCE)
                && AuthRoles.parse(user.getRoles()).equals(jwt.getClaimAsStringList("roles"))
                && user.isEnabled();
    }
}
