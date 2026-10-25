package com.hrflow.identityorg.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.nimbusds.jose.jwk.RSAKey;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class SigningKeysTest {
    @TempDir Path directory;

    @Test
    void localKeyPersistsAndJwksDoesNotExposePrivateMaterial() throws Exception {
        SigningKeys signing = new SigningKeys();
        RSAKey key = signing.rsaKey(directory.toString(), true);
        assertTrue(Files.exists(directory.resolve("private.pem")));
        assertEquals(key.getKeyID(), signing.rsaKey(directory.toString(), false).getKeyID());
        JwtEncoder encoder = signing.jwtEncoder(key);
        JwtDecoder decoder = signing.jwtDecoder(key, "hrflow-local");
        AuthUser user = new AuthUser(new AuthTenant("demo"), "owner@example.test", "hash", "Owner");
        AccessTokens access = new AccessTokens(encoder, key, "hrflow-local");
        assertTrue(access.matches(decoder.decode(access.issue(user)), user));
        assertFalse(access.jwks().toString().contains("PRIVATE"));
        assertFalse(access.jwks().toString().contains("\"d\""));
    }

    @Test
    void productionModeRequiresMountedKey() {
        assertThrows(IllegalStateException.class,
                () -> new SigningKeys().rsaKey(directory.toString(), false));
    }
}
