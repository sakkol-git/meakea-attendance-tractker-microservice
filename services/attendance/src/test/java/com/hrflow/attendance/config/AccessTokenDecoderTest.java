package com.hrflow.attendance.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class AccessTokenDecoderTest {
    @Test
    void validatesSignatureClaimsAndLifetimeLocally() throws Exception {
        RSAKey trusted = new RSAKeyGenerator(2048).keyID("trusted").generate();
        RSAKey foreign = new RSAKeyGenerator(2048).keyID("foreign").generate();
        byte[] jwks = new JWKSet(trusted.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            try (var body = exchange.getResponseBody()) {
                body.write(jwks);
            }
        });
        server.start();
        try {
            JwtDecoder decoder = new AccessTokenDecoder().jwtDecoder(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks", "hrflow-local");
            Instant now = Instant.now();
            var valid = claims("hrflow-local", "hrflow-clients", now.minusSeconds(5),
                    now.plusSeconds(600), "tenant", "employee", List.of("HR_ADMIN"));
            assertNotNull(decoder.decode(token(trusted, "trusted", JWSAlgorithm.RS256, valid)));
            assertThrows(JwtException.class, () -> decoder.decode("malformed"));
            assertRejected(decoder, trusted, claims("foreign", "hrflow-clients",
                    now.minusSeconds(5), now.plusSeconds(600), "tenant", "employee", List.of("HR_ADMIN")));
            assertRejected(decoder, trusted, claims("hrflow-local", "foreign",
                    now.minusSeconds(5), now.plusSeconds(600), "tenant", "employee", List.of("HR_ADMIN")));
            assertRejected(decoder, trusted, claims("hrflow-local", "hrflow-clients",
                    now.minusSeconds(180), now.minusSeconds(120), "tenant", "employee", List.of("HR_ADMIN")));
            assertRejected(decoder, trusted, claims("hrflow-local", "hrflow-clients",
                    now.plusSeconds(600), now.plusSeconds(1200), "tenant", "employee", List.of("HR_ADMIN")));
            assertRejected(decoder, trusted, claims("hrflow-local", "hrflow-clients",
                    now.minusSeconds(5), now.plusSeconds(600), null, "employee", List.of("HR_ADMIN")));
            assertRejected(decoder, trusted, claims("hrflow-local", "hrflow-clients",
                    now.minusSeconds(5), now.plusSeconds(600), "tenant", null, List.of("HR_ADMIN")));
            assertRejected(decoder, trusted, claims("hrflow-local", "hrflow-clients",
                    now.minusSeconds(5), now.plusSeconds(600), "tenant", "employee", List.of()));
            assertThrows(JwtException.class, () -> decoder.decode(
                    token(trusted, "trusted", JWSAlgorithm.PS256, valid)));
            assertThrows(JwtException.class, () -> decoder.decode(
                    token(foreign, "trusted", JWSAlgorithm.RS256, valid)));
        } finally {
            server.stop(0);
        }
    }

    private void assertRejected(JwtDecoder decoder, RSAKey key, JWTClaimsSet claims) throws Exception {
        assertThrows(JwtException.class, () -> decoder.decode(token(key, "trusted", JWSAlgorithm.RS256, claims)));
    }

    private JWTClaimsSet claims(String issuer, String audience, Instant notBefore, Instant expires,
            String tenant, String employee, List<String> roles) {
        return new JWTClaimsSet.Builder().issuer(issuer).audience(audience)
                .subject("user").issueTime(Date.from(Instant.now().minusSeconds(10)))
                .notBeforeTime(Date.from(notBefore)).expirationTime(Date.from(expires))
                .claim("tenantId", tenant).claim("employeeId", employee)
                .claim("roles", roles).build();
    }

    private String token(RSAKey signingKey, String kid, JWSAlgorithm algorithm, JWTClaimsSet claims)
            throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(algorithm).keyID(kid).build(), claims);
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }
}
