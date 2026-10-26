package com.hrflow.identityorg.auth.service;

import com.hrflow.identityorg.profile.entity.User;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;

public interface JwkProviderService {
    String issue(User user);
    Map<String, Object> jwks();
    boolean matches(Jwt jwt, User user);
}
