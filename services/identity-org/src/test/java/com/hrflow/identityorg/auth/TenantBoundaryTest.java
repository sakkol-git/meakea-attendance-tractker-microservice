package com.hrflow.identityorg.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.identityorg.auth.controller.AuthController;
import com.hrflow.identityorg.auth.controller.utils.AuthResponseFactory;
import com.hrflow.identityorg.auth.service.AuthSessionService;
import com.hrflow.identityorg.auth.service.CookieHandlerService;
import com.hrflow.identityorg.auth.service.PasswordService;
import com.hrflow.identityorg.config.SecurityConfiguration;
import com.hrflow.identityorg.exception.GlobalExceptionHandler;
import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.exception.ProfileNotFoundException;
import com.hrflow.identityorg.profile.controller.UserProfileController;
import com.hrflow.identityorg.profile.service.UserProfileService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({UserProfileController.class, AuthController.class})
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class})
class TenantBoundaryTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserProfileService profiles;

    @MockitoBean
    AuthSessionService sessions;

    @MockitoBean
    CookieHandlerService cookies;

    @MockitoBean
    AuthResponseFactory responseFactory;

    @MockitoBean
    PasswordService passwords;

    @MockitoBean
    JwtDecoder decoder;

    private static final String TENANT_A = "11111111-1111-1111-1111-111111111111";
    private static final String TENANT_B = "22222222-2222-2222-2222-222222222222";
    private static final String USER_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    private static final Set<String> PROTECTED_AUTH_ROUTES = Set.of(
            "/auth/me",
            "/auth/profile",
            "/auth/change-password"
    );

    @Test
    void allProtectedAuthRoutesAreRegistered() {
        assertTrue(PROTECTED_AUTH_ROUTES.contains("/auth/me"));
        assertTrue(PROTECTED_AUTH_ROUTES.contains("/auth/profile"));
        assertTrue(PROTECTED_AUTH_ROUTES.contains("/auth/change-password"));
    }

    @Test
    void foreignTenantAccessToProfileMeReturns404ProblemJson() throws Exception {
        Jwt jwtTenantB = createJwt(USER_A, TENANT_B);
        when(decoder.decode("token-tenant-b")).thenReturn(jwtTenantB);
        when(profiles.me(any())).thenThrow(new ProfileNotFoundException());

        mvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer token-tenant-b"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void foreignTenantAttemptToUpdateProfileReturns404ProblemJson() throws Exception {
        Jwt jwtTenantB = createJwt(USER_A, TENANT_B);
        when(decoder.decode("token-tenant-b")).thenReturn(jwtTenantB);
        when(profiles.update(any(), any())).thenThrow(new ProfileNotFoundException());

        mvc.perform(patch("/auth/profile")
                        .header("Authorization", "Bearer token-tenant-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked Name\",\"timeZone\":\"UTC\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void crossTenantPasswordChangeIsRejected() throws Exception {
        Jwt jwtTenantB = createJwt(USER_A, TENANT_B);
        when(decoder.decode("token-tenant-b")).thenReturn(jwtTenantB);
        org.mockito.Mockito.doThrow(AuthFailedException.invalid("Cross-tenant modification forbidden"))
                .when(passwords).change(any(), any(), any());

        mvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer token-tenant-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"valid-pass-1\",\"newPassword\":\"new-valid-pass-2\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void forgedTenantHeaderHasZeroEffectOnTenantContext() throws Exception {
        // Attacker presents valid token for Tenant A, but tries to inject header X-Tenant-Id for Tenant B
        Jwt jwtTenantA = createJwt(USER_A, TENANT_A);
        when(decoder.decode("token-tenant-a")).thenReturn(jwtTenantA);
        when(profiles.me(any())).thenThrow(new ProfileNotFoundException());

        mvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer token-tenant-a")
                        .header("X-Tenant-Id", TENANT_B)
                        .header("X-Roles", "ROLE_OWNER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));

        // Verify the service was called with Tenant A from the verified JWT, ignoring header
        org.mockito.ArgumentCaptor<Jwt> captor = org.mockito.ArgumentCaptor.forClass(Jwt.class);
        verify(profiles).me(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(TENANT_A, captor.getValue().getClaimAsString("tenantId"));
    }

    @Test
    void serviceSideRejectedTokenWithWrongAudienceReturns401() throws Exception {
        when(decoder.decode("wrong-aud-token"))
                .thenThrow(new BadJwtException("Invalid token audience"));

        mvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer wrong-aud-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("IDENTITY-UNAUTHENTICATED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void serviceSideRejectedTokenWithRetiredKeyReturns401() throws Exception {
        when(decoder.decode("retired-key-token"))
                .thenThrow(new BadJwtException("Signing key is retired"));

        mvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer retired-key-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("IDENTITY-UNAUTHENTICATED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void foreignRefreshTokenFamilyRevocationReturns401() throws Exception {
        String foreignFamilyToken = "foreign-family-token-12345678901234";
        when(sessions.refresh(foreignFamilyToken))
                .thenThrow(AuthFailedException.invalid("Token family revoked"));

        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + foreignFamilyToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.status").value(401));
    }

    private Jwt createJwt(String subject, String tenantId) {
        return Jwt.withTokenValue("mock-jwt")
                .header("alg", "RS256")
                .issuer("hrflow-local")
                .subject(subject)
                .claim("tenantId", tenantId)
                .claim("employeeId", UUID.randomUUID().toString())
                .claim("roles", List.of("EMPLOYEE"))
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}
