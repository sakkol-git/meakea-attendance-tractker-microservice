package com.hrflow.gateway.config;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public final class SecurityProblems {
    private SecurityProblems() {}

    static ServerAuthenticationEntryPoint unauthorized() {
        return (exchange, error) -> write(exchange, HttpStatus.UNAUTHORIZED,
                "GATEWAY-UNAUTHENTICATED", "Sign in required.");
    }

    static ServerAccessDeniedHandler forbidden() {
        return (exchange, error) -> write(exchange, HttpStatus.FORBIDDEN,
                "GATEWAY-FORBIDDEN", "This action is not permitted.");
    }

    private static Mono<Void> write(ServerWebExchange exchange, HttpStatus status,
                                    String code, String detail) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.valueOf("application/problem+json"));
        response.getHeaders().setCacheControl("no-store");
        String body = "{\"type\":\"about:blank\",\"title\":\"" + status.getReasonPhrase()
                + "\",\"status\":" + status.value() + ",\"code\":\"" + code
                + "\",\"detail\":\"" + detail + "\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
    }
}
