package com.hrflow.identityorg.auth.service;

import com.hrflow.identityorg.auth.service.record.SessionTokens;

public interface AuthSessionService {
    SessionTokens login(String sourceAddress, String tenant, String email, String password);
    SessionTokens refresh(String rawToken);
    void logout(String rawToken);

}
