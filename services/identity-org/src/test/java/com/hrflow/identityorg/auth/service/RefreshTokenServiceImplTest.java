package com.hrflow.identityorg.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import com.hrflow.identityorg.auth.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceImplTest {
    private final RefreshTokenService crypto = new RefreshTokenServiceImpl();

    @Test
    void generatesUniqueEntropySufficientTokens() {
        String token1 = crypto.generateRawToken();
        String token2 = crypto.generateRawToken();
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertTrue(token1.length() >= 48);
    }

    @Test
    void hashesDeterministicallyWithSha256() {
        String raw = "sample-test-token-value-with-entropy-12345";
        String hash1 = crypto.hashToken(raw);
        String hash2 = crypto.hashToken(raw);
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
        assertNotEquals(raw, hash1);
    }

    @Test
    void rejectsNullRawToken() {
        assertThrows(IllegalArgumentException.class, () -> crypto.hashToken(null));
    }
}
