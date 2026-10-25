package com.hrflow.identityorg.auth;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthCookies {
    private final SecureRandom random = new SecureRandom();
    private final boolean secure;
    private final java.util.Set<String> allowedOrigins;

    public AuthCookies(@Value("${hrflow.auth.secure-cookies:true}") boolean secure,
                       @Value("${hrflow.auth.browser-origin:http://localhost:3000}") String allowedOrigin) {
        this.secure = secure;
        this.allowedOrigins = java.util.Set.of(allowedOrigin.split(","));
    }

    String csrfCookie() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return cookie("XSRF-TOKEN", Base64.getUrlEncoder().withoutPadding().encodeToString(bytes), false, 3600);
    }

    String refreshCookie(String token) {
        return cookie("hrflow_refresh", token, true, 30L * 86400);
    }

    String clearRefreshCookie() {
        return cookie("hrflow_refresh", "", true, 0);
    }

    void verify(String origin, String csrfCookie, String csrfHeader) {
        if (origin == null && csrfCookie == null) return;
        if (origin == null || !allowedOrigins.contains(origin) || csrfCookie == null || csrfHeader == null
                || !java.security.MessageDigest.isEqual(csrfCookie.getBytes(), csrfHeader.getBytes())) {
            throw new AuthFailure(org.springframework.http.HttpStatus.FORBIDDEN,
                    "CSRF_REJECTED", "This browser request failed security validation.");
        }
    }

    private String cookie(String name, String value, boolean httpOnly, long seconds) {
        return ResponseCookie.from(name, value).httpOnly(httpOnly).secure(secure)
                .sameSite("Lax").path("/").maxAge(seconds).build().toString();
    }
}
