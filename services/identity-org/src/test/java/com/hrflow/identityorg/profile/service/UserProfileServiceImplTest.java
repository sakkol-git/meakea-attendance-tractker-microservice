package com.hrflow.identityorg.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrflow.identityorg.auth.service.JwkProviderService;
import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.exception.ProfileValidationException;
import com.hrflow.identityorg.profile.dto.ProfilePatchRequest;
import com.hrflow.identityorg.profile.entity.Tenant;
import com.hrflow.identityorg.profile.entity.User;
import com.hrflow.identityorg.profile.mapper.UserMapper;
import com.hrflow.identityorg.profile.repository.UserRepository;
import com.hrflow.identityorg.profile.service.impl.UserProfileServiceImpl;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.oauth2.jwt.Jwt;

class UserProfileServiceImplTest {
    private final UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
    private final JwkProviderService tokens = org.mockito.Mockito.mock(JwkProviderService.class);
    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);
    private final UserProfileService service = new UserProfileServiceImpl(users, tokens, mapper);
    private User user;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        user = new User(new Tenant("demo"), "owner@example.test", "hash", "Original Name");
        jwt = jwt(user.getId().toString(), user.getTenantId().toString());
        when(users.findByIdAndTenantIdAndEnabledTrue(user.getId(), user.getTenantId()))
                .thenReturn(Optional.of(user));
        when(tokens.matches(jwt, user)).thenReturn(true);
        when(users.save(user)).thenReturn(user);
    }

    @Test
    void readsOnlyTheAuthenticatedTenantScopedProfile() {
        var result = service.me(jwt);

        assertEquals(user.getId().toString(), result.userId());
        assertEquals(user.getTenantId().toString(), result.tenantId());
        verify(users).findByIdAndTenantIdAndEnabledTrue(user.getId(), user.getTenantId());
    }

    @Test
    void patchesOnlyContractFieldsAndReturnsTheUpdatedProfile() {
        var result = service.update(jwt, new ProfilePatchRequest("  Updated Name  ", "Asia/Bangkok"));

        assertEquals("Updated Name", result.name());
        assertEquals("Asia/Bangkok", result.timeZone());
        verify(users).save(user);
    }

    @Test
    void rejectsInvalidTimeZoneWithoutSaving() {
        assertThrows(ProfileValidationException.class,
                () -> service.update(jwt, new ProfilePatchRequest(null, "Not/A_Zone")));
    }

    @Test
    void rejectsANameThatBecomesTooShortAfterTrimming() {
        assertThrows(ProfileValidationException.class,
                () -> service.update(jwt, new ProfilePatchRequest(" x ", null)));
    }

    @Test
    void rejectsMalformedOrCrossTenantClaimsBeforeLookup() {
        assertThrows(AuthFailedException.class, () -> service.me(jwt("not-a-uuid", user.getTenantId().toString())));
        assertThrows(AuthFailedException.class, () -> service.me(jwt(user.getId().toString(), "not-a-uuid")));
    }

    private static Jwt jwt(String subject, String tenantId) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "RS256"), Map.of(
                        "sub", subject,
                        "tenantId", tenantId,
                        "employeeId", "employee-id",
                        "roles", List.of("OWNER"),
                        "aud", List.of("hrflow-api")));
    }
}
