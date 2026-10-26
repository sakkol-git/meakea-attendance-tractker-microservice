package com.hrflow.identityorg.auth.controller;

import com.hrflow.identityorg.auth.controller.utils.AuthResponseFactory;
import com.hrflow.identityorg.auth.dto.ChangePasswordRequest;
import com.hrflow.identityorg.auth.dto.LoginRequest;
import com.hrflow.identityorg.auth.dto.RefreshRequest;
import com.hrflow.identityorg.auth.dto.TokenResponse;
import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.auth.service.AuthSessionService;
import com.hrflow.identityorg.auth.service.CookieHandlerService;
import com.hrflow.identityorg.auth.service.PasswordService;
import jakarta.validation.Validator;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String CLIENT_ADDRESS_HEADER = "X-HRFlow-Client-Address";
    private final AuthSessionService sessions;
    private final CookieHandlerService cookies;
    private final AuthResponseFactory responseFactory;
    private final Validator validator;
    private final PasswordService passwords;

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return responseFactory.csrfResponse();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest body,
            HttpServletRequest request,
            @RequestHeader(value = CLIENT_ADDRESS_HEADER, required = false) String gatewaySource,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "X-XSRF-TOKEN", required = false) String csrfHeader,
            @CookieValue(value = "XSRF-TOKEN", required = false) String csrfCookie) {
        cookies.verify(origin, csrfCookie, csrfHeader);
        validate(body);
        return responseFactory.tokenResponse(
                sessions.login(gatewaySource == null ? request.getRemoteAddr() : gatewaySource,
                        body.tenant(), body.email(), body.password()), origin);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody(required = false) RefreshRequest body,
            @CookieValue(value = "hrflow_refresh", required = false) String cookie,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "X-XSRF-TOKEN", required = false) String csrfHeader,
            @CookieValue(value = "XSRF-TOKEN", required = false) String csrfCookie) {
        cookies.verify(origin, csrfCookie, csrfHeader);
        if (origin == null) validate(body);
        String token = origin != null ? cookie : (body != null ? body.refreshToken() : null);
        return responseFactory.tokenResponse(sessions.refresh(token), origin);
    }

    @PostMapping("logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest body,
            @CookieValue(value = "hrflow_refresh", required = false) String cookie,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "X-XSRF-TOKEN", required = false) String csrfHeader,
            @CookieValue(value = "XSRF-TOKEN", required = false) String csrfCookie) {
        cookies.verify(origin, csrfCookie, csrfHeader);
        if (origin == null && body != null) validate(body);
        sessions.logout(origin != null ? cookie : (body != null ? body.refreshToken() : null));
        return responseFactory.logoutResponse();
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @RequestBody ChangePasswordRequest body) {
        validate(body);
        passwords.change(jwt, body.currentPassword(), body.newPassword());
        return Map.of("message", "Password changed.");
    }

    private void validate(Object body) {
        if (body == null || !validator.validate(body).isEmpty()) {
            throw new AuthFailedException(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_ERROR",
                    "Check the submitted fields.");
        }
    }
}
