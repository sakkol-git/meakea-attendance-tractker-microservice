package com.hrflow.identityorg.auth.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jose.jwk.RSAKey;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class SigningKeysAudienceTest {
    @Test
    void decoderRejectsTokenIssuedForAnotherAudience(@TempDir Path directory) throws Exception {
        SigningKeys configuration = new SigningKeys();
        RSAKey key = configuration.rsaKey(directory.toString(), true);
        JwtEncoder encoder = configuration.jwtEncoder(key);
        JwtDecoder decoder = configuration.jwtDecoder(
                new SigningKeyRing(key, null, null, Clock.systemUTC()), "hrflow-local");
        Instant now = Instant.now();
        String token = encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .issuer("hrflow-local")
                .subject("00000000-0000-0000-0000-000000000001")
                .audience(List.of("another-application"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build())).getTokenValue();

        assertThrows(Exception.class, () -> decoder.decode(token));
    }
}
