package com.hrflow.identityorg.auth.service;

import org.springframework.security.oauth2.jwt.Jwt;

public interface PasswordService {
    void change(Jwt jwt, String currentPassword, String newPassword);
}
