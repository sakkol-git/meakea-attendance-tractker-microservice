package com.hrflow.identityorg.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hrflow.identityorg.auth.service.record.AuthTokenPolicy;
import com.nimbusds.jose.jwk.RSAKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class SigningKeyRingTest {
    @Test
    void loadsMountedRetiringPublicKeyAndRejectsIncompletePair(@TempDir Path temp) throws Exception {
        SigningKeys configuration = new SigningKeys();
        Path retiringDirectory = Files.createDirectory(temp.resolve("retiring"));
        RSAKey retiring = configuration.rsaKey(retiringDirectory.toString(), true);
        Path activeDirectory = Files.createDirectory(temp.resolve("active"));
        RSAKey active = configuration.rsaKey(activeDirectory.toString(), true);
        Clock clock = Clock.systemUTC();

        Files.copy(retiringDirectory.resolve("public.pem"), activeDirectory.resolve("retiring-public.pem"));
        assertThrows(IllegalStateException.class,
                () -> configuration.signingKeyRing(active, activeDirectory.toString(), clock));
        Files.writeString(activeDirectory.resolve("retiring-valid-until.txt"),
                Instant.now().plusSeconds(120).toString());

        SigningKeyRing mounted = configuration.signingKeyRing(active, activeDirectory.toString(), clock);
        assertEquals(2, mounted.jwks().getKeys().size());
        assertEquals(retiring.getKeyID(), mounted.jwks().getKeys().get(1).getKeyID());
    }

    @Test
    void acceptsRetiringKeyDuringOverlapThenRejectsIt(@TempDir Path temp) throws Exception {
        SigningKeys configuration = new SigningKeys();
        RSAKey retiring = configuration.rsaKey(Files.createDirectory(temp.resolve("retiring")).toString(), true);
        RSAKey active = configuration.rsaKey(Files.createDirectory(temp.resolve("active")).toString(), true);
        Instant now = Instant.now();
        String oldToken = token(configuration, retiring, now);
        String newToken = token(configuration, active, now);
        SigningKeyRing overlap = new SigningKeyRing(active, retiring.toPublicJWK(),
                now.plusSeconds(60), Clock.fixed(now, ZoneOffset.UTC));

        assertEquals(2, overlap.jwks().getKeys().size());
        configuration.jwtDecoder(overlap, "hrflow-local").decode(oldToken);
        configuration.jwtDecoder(overlap, "hrflow-local").decode(newToken);

        SigningKeyRing retired = new SigningKeyRing(active, retiring.toPublicJWK(),
                now.plusSeconds(60), Clock.fixed(now.plusSeconds(61), ZoneOffset.UTC));
        assertEquals(1, retired.jwks().getKeys().size());
        assertThrows(Exception.class, () -> configuration.jwtDecoder(retired, "hrflow-local").decode(oldToken));
        configuration.jwtDecoder(retired, "hrflow-local").decode(newToken);
    }

    private String token(SigningKeys configuration, RSAKey key, Instant now) {
        return configuration.jwtEncoder(key).encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build(),
                JwtClaimsSet.builder().issuer("hrflow-local")
                        .audience(List.of(AuthTokenPolicy.AUDIENCE))
                        .subject("00000000-0000-0000-0000-000000000001")
                        .issuedAt(now.minusSeconds(5)).expiresAt(now.plusSeconds(3600))
                        .build())).getTokenValue();
    }
}
