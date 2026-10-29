package com.hrflow.identityorg.auth.service.impl;

import com.hrflow.identityorg.exception.AuthFailedException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.hrflow.identityorg.auth.service.CookieHandlerService;
import com.hrflow.identityorg.auth.service.record.AuthTokenPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieHandlerServiceImpl implements CookieHandlerService {
    private final SecureRandom random = new SecureRandom();
    private final boolean secure;
    private final Set<String> allowedOrigins;
    private final String refreshPath;

    public CookieHandlerServiceImpl(@Value("${hrflow.auth.secure-cookies:true}") boolean secure,
                                    @Value("${hrflow.auth.browser-origin:http://localhost:3000}") String allowedOrigin) {
        this(secure, allowedOrigin, "/auth");
    }

    /** refreshPath must be the browser-visible auth prefix (e.g. /api/v1/auth behind the edge proxy). */
    @Autowired
    public CookieHandlerServiceImpl(@Value("${hrflow.auth.secure-cookies:true}") boolean secure,
                                    @Value("${hrflow.auth.browser-origin:http://localhost:3000}") String allowedOrigin,
                                    @Value("${hrflow.auth.refresh-cookie-path:/auth}") String refreshPath) {
        this.refreshPath = requireCookiePath(refreshPath);
        this.secure = secure;
        this.allowedOrigins = Arrays.stream(allowedOrigin.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String csrfCookie() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return cookie("XSRF-TOKEN", Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),
                false, 3600, "/");
    }

    @Override
    public String refreshCookie(String token) {
        return cookie("hrflow_refresh", token, true, AuthTokenPolicy.REFRESH_LIFETIME.toSeconds(), refreshPath);
    }

    @Override
    public String clearRefreshCookie() {
        return cookie("hrflow_refresh", "", true, 0, refreshPath);
    }

    @Override
    public void verify(String origin, String csrfCookie, String csrfHeader) {
        if (origin == null && csrfCookie == null) return;
        if (origin == null || !allowedOrigins.contains(origin) || csrfCookie == null || csrfHeader == null
                || !MessageDigest.isEqual(csrfCookie.getBytes(), csrfHeader.getBytes())) {
            throw new AuthFailedException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "CSRF_REJECTED", "This browser request failed security validation.");
        }
    }

    private String cookie(String name, String value, boolean httpOnly, long seconds, String path) {
        return ResponseCookie.from(name, value).httpOnly(httpOnly).secure(secure)
                .sameSite("Lax").path(path).maxAge(seconds).build().toString();
    }

    private static String requireCookiePath(String path) {
        if (path == null || path.isBlank() || !path.startsWith("/") || path.contains(";")) {
            throw new IllegalArgumentException("Refresh cookie path must be an absolute HTTP path");
        }
        return path;
    }
}
