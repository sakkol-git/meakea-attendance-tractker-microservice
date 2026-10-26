package com.hrflow.identityorg.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrflow.identityorg.auth.controller.utils.AuthResponseFactory;
import com.hrflow.identityorg.auth.dto.RefreshRequest;
import com.hrflow.identityorg.auth.service.AuthSessionService;
import com.hrflow.identityorg.auth.service.CookieHandlerService;
import com.hrflow.identityorg.auth.service.PasswordService;
import com.hrflow.identityorg.auth.service.record.SessionTokens;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthControllerTest {
    private final AuthSessionService sessions = mock(AuthSessionService.class);
    private final CookieHandlerService cookies = mock(CookieHandlerService.class);
    private final AuthResponseFactory responses = mock(AuthResponseFactory.class);
    private final PasswordService passwords = mock(PasswordService.class);
    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(sessions, cookies, responses,
                Validation.buildDefaultValidatorFactory().getValidator(), passwords);
        when(sessions.refresh(any())).thenReturn(new SessionTokens("access", "rotated-refresh"));
    }

    @Test
    void nativeRefreshAndLogoutUseTheJsonBodyToken() {
        controller.refresh(new RefreshRequest("n".repeat(48)), null, null, null, null);
        controller.logout(new RefreshRequest("l".repeat(48)), null, null, null, null);

        verify(sessions).refresh("n".repeat(48));
        verify(sessions).logout("l".repeat(48));
    }

    @Test
    void browserRefreshAndLogoutUseTheProtectedCookieToken() {
        controller.refresh(null, "b".repeat(48), "https://app.example", "csrf", "csrf");
        controller.logout(null, "c".repeat(48), "https://app.example", "csrf", "csrf");

        verify(cookies, times(2)).verify(eq("https://app.example"), eq("csrf"), eq("csrf"));
        verify(sessions).refresh("b".repeat(48));
        verify(sessions).logout("c".repeat(48));
    }
}
