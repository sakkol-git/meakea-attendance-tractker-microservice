package com.hrflow.identityorg.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.profile.dto.ProfilePatchRequest;
import com.hrflow.identityorg.profile.dto.UserProfileResponse;
import com.hrflow.identityorg.profile.service.UserProfileService;
import com.hrflow.identityorg.profile.controller.UserProfileController;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class UserProfileControllerTest {
    @Test
    void malformedAuthenticatedSubjectReturnsSafeAuthenticationFailure() {
        UserProfileService service = mock(UserProfileService.class);
        UserProfileController controller = new UserProfileController(service);
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "RS256"), Map.of("sub", "not-a-uuid"));
        when(service.me(jwt)).thenThrow(AuthFailedException.invalid("Invalid access token"));

        assertThrows(AuthFailedException.class, () -> controller.me(jwt));
    }

    @Test
    void delegatesProfilePatchUsingTheAuthenticatedJwt() {
        UserProfileService service = mock(UserProfileService.class);
        UserProfileController controller = new UserProfileController(service);
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "RS256"), Map.of("sub", "00000000-0000-0000-0000-000000000001"));
        ProfilePatchRequest request = new ProfilePatchRequest("Updated Name", "Asia/Bangkok");
        UserProfileResponse expected = new UserProfileResponse(
                "user", "employee", "tenant", List.of("EMPLOYEE"),
                "Updated Name", "user@example.test", "Asia/Bangkok", "en");
        when(service.update(jwt, request)).thenReturn(expected);

        assertEquals(expected, controller.update(jwt, request));
        verify(service).update(jwt, request);
    }
}
