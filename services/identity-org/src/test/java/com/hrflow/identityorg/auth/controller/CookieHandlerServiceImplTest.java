package com.hrflow.identityorg.auth.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.auth.service.impl.CookieHandlerServiceImpl;
import com.hrflow.identityorg.auth.service.CookieHandlerService;
import org.junit.jupiter.api.Test;

class CookieHandlerServiceImplTest {
    private final CookieHandlerService cookies = new CookieHandlerServiceImpl(false, "http://localhost:3000");

    @Test
    void browserNeedsExactOriginAndMatchingCsrfToken() {
        cookies.verify("http://localhost:3000", "token", "token");
        assertThrows(AuthFailedException.class,
                () -> cookies.verify("https://attacker.example", "token", "token"));
        assertThrows(AuthFailedException.class,
                () -> cookies.verify("http://localhost:3000", "token", "other"));
        assertThrows(AuthFailedException.class,
                () -> cookies.verify(null, "token", null));
    }

    @Test
    void nativeRequestCanUseBodyTokenWithoutBrowserCookie() {
        cookies.verify(null, null, null);
        assertTrue(cookies.refreshCookie("secret").contains("HttpOnly"));
        assertTrue(cookies.refreshCookie("secret").contains("Path=/auth"));
        assertFalse(cookies.csrfCookie().contains("HttpOnly"));
        assertTrue(cookies.csrfCookie().contains("Path=/"));
    }
}
