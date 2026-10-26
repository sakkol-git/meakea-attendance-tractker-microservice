package com.hrflow.identityorg.auth.service.impl;

import com.hrflow.identityorg.auth.repository.RefreshSessionRepository;
import com.hrflow.identityorg.auth.service.JwkProviderService;
import com.hrflow.identityorg.auth.service.PasswordService;
import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.exception.ProfileNotFoundException;
import com.hrflow.identityorg.profile.entity.User;
import com.hrflow.identityorg.profile.repository.UserRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {
    private final UserRepository users;
    private final RefreshSessionRepository sessions;
    private final JwkProviderService tokens;
    private final PasswordEncoder encoder;
    private final Clock clock;

    @Override
    @Transactional
    public void change(Jwt jwt, String currentPassword, String newPassword) {
        UUID userId = claimUuid(jwt != null ? jwt.getSubject() : null);
        UUID tenantId = claimUuid(jwt != null ? jwt.getClaimAsString("tenantId") : null);
        User user = users.findForUpdateByIdAndTenantIdAndEnabledTrue(userId, tenantId)
                .orElseThrow(ProfileNotFoundException::new);
        if (!tokens.matches(jwt, user) || !encoder.matches(currentPassword, user.getPasswordHash())) {
            throw AuthFailedException.invalid("Current password is incorrect");
        }
        user.changePassword(encoder.encode(newPassword));
        users.save(user);
        sessions.revokeUser(tenantId, userId, clock.instant());
    }

    private UUID claimUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException invalidClaim) {
            throw AuthFailedException.invalid("Invalid access token");
        }
    }
}
