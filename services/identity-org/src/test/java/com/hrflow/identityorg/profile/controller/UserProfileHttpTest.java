package com.hrflow.identityorg.profile.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.identityorg.config.SecurityConfiguration;
import com.hrflow.identityorg.exception.GlobalExceptionHandler;
import com.hrflow.identityorg.profile.dto.UserProfileResponse;
import com.hrflow.identityorg.profile.service.UserProfileService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserProfileController.class)
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class})
class UserProfileHttpTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserProfileService profiles;

    @MockitoBean
    JwtDecoder decoder;

    @Test
    void profileRoutesRequireAuthenticationAndGetReturnsTheContractProfile() throws Exception {
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        when(profiles.me(any(Jwt.class))).thenReturn(profile("Current Name", "Asia/Phnom_Penh"));
        mvc.perform(get("/auth/me").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.employeeId").value("employee-1"))
                .andExpect(jsonPath("$.tenantId").value("tenant-1"))
                .andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"))
                .andExpect(jsonPath("$.name").value("Current Name"))
                .andExpect(jsonPath("$.timeZone").value("Asia/Phnom_Penh"));
    }

    @Test
    void patchProfileAcceptsCanonicalPartialFields() throws Exception {
        when(profiles.update(any(Jwt.class), any())).thenReturn(profile("Updated Name", "Asia/Bangkok"));

        mvc.perform(patch("/auth/profile")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated Name","timeZone":"Asia/Bangkok"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.timeZone").value("Asia/Bangkok"));
    }

    @Test
    void patchProfileRejectsContractValidationFailures() throws Exception {
        mvc.perform(patch("/auth/profile")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"x"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private static UserProfileResponse profile(String name, String timeZone) {
        return new UserProfileResponse("user-1", "employee-1", "tenant-1",
                List.of("EMPLOYEE"), name, "user@example.test", timeZone, "en");
    }
}
