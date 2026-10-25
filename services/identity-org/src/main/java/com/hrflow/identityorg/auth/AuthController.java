package com.hrflow.identityorg.auth;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final AuthSessions sessions;
    private final AccessTokens access;
    private final AuthCookies cookies;
    private final Validator validator;

    public AuthController(AuthSessions sessions, AccessTokens access, AuthCookies cookies, Validator validator) {
        this.sessions = sessions;
        this.access = access;
        this.cookies = cookies;
        this.validator = validator;
    }

    @GetMapping("/.well-known/jwks.json")
    Map<String, Object> jwks() {
        return access.jwks();
    }

    @GetMapping("/auth/csrf")
    ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.csrfCookie())
                .header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }

    @PostMapping("/auth/login")
    ResponseEntity<Map<String, Object>> login(@RequestBody Login body,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "X-XSRF-TOKEN", required = false) String csrfHeader,
            @CookieValue(value = "XSRF-TOKEN", required = false) String csrfCookie) {
        cookies.verify(origin, csrfCookie, csrfHeader);
        validate(body);
        return response(sessions.login(body.tenant(), body.email(), body.password()), origin != null);
    }

    @PostMapping("/auth/refresh")
    ResponseEntity<Map<String, Object>> refresh(@RequestBody(required = false) Refresh body,
            @CookieValue(value = "hrflow_refresh", required = false) String cookie,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "X-XSRF-TOKEN", required = false) String csrfHeader,
            @CookieValue(value = "XSRF-TOKEN", required = false) String csrfCookie) {
        cookies.verify(origin, csrfCookie, csrfHeader);
        if (origin == null) validate(body);
        String token = origin == null ? body == null ? null : body.refreshToken() : cookie;
        return response(sessions.refresh(token), origin != null);
    }

    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout(@RequestBody(required = false) Refresh body,
            @CookieValue(value = "hrflow_refresh", required = false) String cookie,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "X-XSRF-TOKEN", required = false) String csrfHeader,
            @CookieValue(value = "XSRF-TOKEN", required = false) String csrfCookie) {
        cookies.verify(origin, csrfCookie, csrfHeader);
        if (origin == null && body != null) validate(body);
        sessions.logout(origin == null ? body == null ? null : body.refreshToken() : cookie);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.clearRefreshCookie())
                .header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }

    @GetMapping("/auth/me")
    Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) throw AuthFailure.invalid();
        return sessions.me(UUID.fromString(jwt.getSubject()), jwt);
    }

    private ResponseEntity<Map<String, Object>> response(AuthSessions.TokenPair pair, boolean browser) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accessToken", pair.accessToken());
        body.put("tokenType", "Bearer");
        body.put("expiresIn", 900);
        if (!browser) body.put("refreshToken", pair.refreshToken());
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store");
        if (browser) response.header(HttpHeaders.SET_COOKIE, cookies.refreshCookie(pair.refreshToken()));
        return response.body(body);
    }

    private void validate(Object body) {
        if (body == null || !validator.validate(body).isEmpty()) {
            throw new AuthFailure(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_ERROR",
                    "Check the submitted fields.");
        }
    }

    public record Login(
            @NotBlank @Pattern(regexp = "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?") String tenant,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 1024) String password) {}

    public record Refresh(@NotBlank @Size(min = 32, max = 512) String refreshToken) {}
}
