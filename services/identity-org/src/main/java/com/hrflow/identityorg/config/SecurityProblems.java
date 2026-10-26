package com.hrflow.identityorg.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

public final class SecurityProblems {
    private SecurityProblems() {}

    static AuthenticationEntryPoint unauthorized() {
        return (request, response, error) -> write(response, HttpStatus.UNAUTHORIZED,
                "UNAUTHENTICATED", "Sign in required.");
    }

    static AccessDeniedHandler forbidden() {
        return (request, response, error) -> write(response, HttpStatus.FORBIDDEN,
                "FORBIDDEN", "This action is not permitted.");
    }

    private static void write(HttpServletResponse response, HttpStatus status,
                              String code, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\""
                + status.getReasonPhrase() + "\",\"status\":" + status.value()
                + ",\"code\":\"" + code + "\",\"detail\":\"" + detail + "\"}");
    }
}
