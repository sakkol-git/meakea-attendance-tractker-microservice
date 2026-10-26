package com.hrflow.identityorg.profile.service.impl;

import com.hrflow.identityorg.auth.service.JwkProviderService;
import com.hrflow.identityorg.profile.dto.ProfilePatchRequest;
import com.hrflow.identityorg.profile.dto.UserProfileResponse;
import com.hrflow.identityorg.profile.entity.User;
import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.exception.ProfileNotFoundException;
import com.hrflow.identityorg.exception.ProfileValidationException;
import com.hrflow.identityorg.profile.mapper.UserMapper;
import com.hrflow.identityorg.profile.repository.UserRepository;
import com.hrflow.identityorg.profile.service.UserProfileService;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final UserRepository users;
    private final JwkProviderService access;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse me(Jwt jwt) {
        return userMapper.toProfileResponse(authenticatedUser(jwt));
    }

    @Override
    @Transactional
    public UserProfileResponse update(Jwt jwt, ProfilePatchRequest request) {
        User user = authenticatedUser(jwt);
        String name = validatedName(request.name());
        String timeZone = validatedTimeZone(request.timeZone());
        user.updateProfile(name, timeZone);
        return userMapper.toProfileResponse(users.save(user));
    }

    private User authenticatedUser(Jwt jwt) {
        UUID userId = claimUuid(jwt != null ? jwt.getSubject() : null);
        UUID tenantId = claimUuid(jwt != null ? jwt.getClaimAsString("tenantId") : null);
        User user = users.findByIdAndTenantIdAndEnabledTrue(userId, tenantId)
                .orElseThrow(ProfileNotFoundException::new);
        if (!access.matches(jwt, user)) throw AuthFailedException.invalid("Invalid access token");
        return user;
    }

    private UUID claimUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException invalidClaim) {
            throw AuthFailedException.invalid("Invalid access token");
        }
    }

    private String validatedTimeZone(String value) {
        if (value == null) return null;
        String normalized = value.strip();
        try {
            return ZoneId.of(normalized).getId();
        } catch (DateTimeException invalidZone) {
            throw new ProfileValidationException("timeZone must be a valid IANA time zone.");
        }
    }

    private String validatedName(String value) {
        if (value == null) return null;
        String normalized = value.strip();
        if (normalized.length() < 2) {
            throw new ProfileValidationException("name must contain at least 2 non-whitespace characters.");
        }
        return normalized;
    }
}
