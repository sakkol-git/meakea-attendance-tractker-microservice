package com.hrflow.identityorg.auth.controller.utils;

import com.hrflow.identityorg.auth.dto.TokenResponse;
import com.hrflow.identityorg.auth.service.record.SessionTokens;
import com.hrflow.identityorg.auth.service.CookieHandlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Factory Pattern: Encapsulates creation of HTTP responses with consistent
 * no-store caching headers and client-specific cookie handling.
 */
@Component
@RequiredArgsConstructor
public class AuthResponseFactory {
    private final CookieHandlerService cookies;

    public ResponseEntity<TokenResponse> tokenResponse(SessionTokens pair, String origin) {
        boolean isBrowser = origin != null;
        TokenResponse body = TokenResponse.of(pair, isBrowser);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store");
        if (isBrowser) {
            builder.header(HttpHeaders.SET_COOKIE, cookies.refreshCookie(pair.refreshToken()));
        }
        return builder.body(body);
    }

    public ResponseEntity<Void> logoutResponse() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clearRefreshCookie())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    public ResponseEntity<Void> csrfResponse() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.csrfCookie())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}
