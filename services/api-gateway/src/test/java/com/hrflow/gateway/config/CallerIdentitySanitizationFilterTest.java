package com.hrflow.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

class CallerIdentitySanitizationFilterTest {

    @Test
    void stripsAllCallerSuppliedIdentityHeadersWhilePreservingAuthorization() {
        var request = MockServerHttpRequest.get("/api/v1/auth/me")
                .header("Authorization", "Bearer valid-token-sample")
                .header("X-Tenant-Id", "forged-tenant-999")
                .header("X-Employee-Id", "forged-emp-888")
                .header("X-Roles", "ROLE_ADMIN")
                .header("X-Role", "ROLE_SUPERUSER")
                .header("X-User-Id", "forged-user-777")
                .build();

        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        new CallerIdentitySanitizationFilter().filter(MockServerWebExchange.from(request), exchange -> {
            forwarded.set(exchange);
            return reactor.core.publisher.Mono.empty();
        }).block();

        var headers = forwarded.get().getRequest().getHeaders();
        assertNull(headers.getFirst("X-Tenant-Id"));
        assertNull(headers.getFirst("X-Employee-Id"));
        assertNull(headers.getFirst("X-Roles"));
        assertNull(headers.getFirst("X-Role"));
        assertNull(headers.getFirst("X-User-Id"));
        assertEquals("Bearer valid-token-sample", headers.getFirst("Authorization"));
    }
}
