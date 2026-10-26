package com.hrflow.gateway.config;

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
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

class AccessTokenRotationTest {
    @Test
    void refreshesUnknownNewKeyAndRejectsRetiredKeyOnFreshDecoder() throws Exception {
        RSAKey retiring = new RSAKeyGenerator(2048).keyID("retiring").generate();
        RSAKey active = new RSAKeyGenerator(2048).keyID("active").generate();
        AtomicReference<JWKSet> published = new AtomicReference<>(
                new JWKSet(retiring.toPublicJWK()));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            byte[] body = published.get().toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var stream = exchange.getResponseBody()) {
                stream.write(body);
            }
        });
        server.start();
        try {
            String uri = "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks";
            var decoder = new AccessTokenDecoder().jwtDecoder(uri, "hrflow-local");
            String oldToken = token(retiring);
            String newToken = token(active);
            assertNotNull(decoder.decode(oldToken).block(Duration.ofSeconds(5)));

            published.set(new JWKSet(List.of(retiring.toPublicJWK(), active.toPublicJWK())));
            assertNotNull(decoder.decode(newToken).block(Duration.ofSeconds(5)));

            published.set(new JWKSet(active.toPublicJWK()));
            var freshDecoder = new AccessTokenDecoder().jwtDecoder(uri, "hrflow-local");
            assertNotNull(freshDecoder.decode(newToken).block(Duration.ofSeconds(5)));
            assertThrows(JwtException.class,
                    () -> freshDecoder.decode(oldToken).block(Duration.ofSeconds(5)));
        } finally {
            server.stop(0);
        }
    }

    private String token(RSAKey key) throws Exception {
        Instant now = Instant.now();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(key.getKeyID()).build(), new JWTClaimsSet.Builder()
                .issuer("hrflow-local").audience("hrflow-clients")
                .subject("00000000-0000-0000-0000-000000000001")
                .issueTime(Date.from(now.minusSeconds(5)))
                .expirationTime(Date.from(now.plusSeconds(900))).build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
