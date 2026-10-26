package com.hrflow.identityorg.auth.service;

public interface RefreshTokenService {
    String generateRawToken();
    String hashToken(String rawToken);
}
