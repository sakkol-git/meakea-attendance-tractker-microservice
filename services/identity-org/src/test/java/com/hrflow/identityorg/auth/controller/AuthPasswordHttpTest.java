package com.hrflow.identityorg.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.identityorg.auth.controller.utils.AuthResponseFactory;
import com.hrflow.identityorg.auth.service.AuthSessionService;
import com.hrflow.identityorg.auth.service.CookieHandlerService;
import com.hrflow.identityorg.auth.service.PasswordService;
import com.hrflow.identityorg.config.SecurityConfiguration;
import com.hrflow.identityorg.exception.AuthExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfiguration.class, AuthExceptionHandler.class})
class AuthPasswordHttpTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthSessionService sessions;

    @MockitoBean
    CookieHandlerService cookies;

    @MockitoBean
    AuthResponseFactory responses;

    @MockitoBean
    PasswordService passwords;

    @MockitoBean
    JwtDecoder decoder;

    @Test
    void changePasswordRequiresAuthentication() throws Exception {
        mvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"current-password","newPassword":"new-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordUpdatesCredentialsForTheAuthenticatedUser() throws Exception {
        mvc.perform(post("/auth/change-password")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"current-password","newPassword":"new-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed."));

        verify(passwords).change(any(Jwt.class), eq("current-password"), eq("new-password"));
    }

    @Test
    void changePasswordRejectsInvalidInput() throws Exception {
        mvc.perform(post("/auth/change-password")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"current-password","newPassword":"short"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
