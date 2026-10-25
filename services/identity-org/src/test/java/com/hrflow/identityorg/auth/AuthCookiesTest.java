package com.hrflow.identityorg.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AuthCookiesTest {
    private final AuthCookies cookies = new AuthCookies(false, "http://localhost:3000");

    @Test
    void browserNeedsExactOriginAndMatchingCsrfToken() {
        cookies.verify("http://localhost:3000", "token", "token");
        assertThrows(AuthFailure.class,
                () -> cookies.verify("https://attacker.example", "token", "token"));
        assertThrows(AuthFailure.class,
                () -> cookies.verify("http://localhost:3000", "token", "other"));
        assertThrows(AuthFailure.class,
                () -> cookies.verify(null, "token", null));
    }

    @Test
    void nativeRequestCanUseBodyTokenWithoutBrowserCookie() {
        cookies.verify(null, null, null);
        assertTrue(cookies.refreshCookie("secret").contains("HttpOnly"));
        assertFalse(cookies.csrfCookie().contains("HttpOnly"));
    }
}
