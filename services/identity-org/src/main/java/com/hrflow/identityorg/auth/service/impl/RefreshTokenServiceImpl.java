package com.hrflow.identityorg.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import com.hrflow.identityorg.auth.service.RefreshTokenService;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generateRawToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hashToken(String rawToken) {
        if (rawToken == null) {
            throw new IllegalArgumentException("rawToken cannot be null");
        }
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }
}
