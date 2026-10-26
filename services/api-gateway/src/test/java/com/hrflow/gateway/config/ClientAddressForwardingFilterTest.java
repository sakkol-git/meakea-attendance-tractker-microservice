package com.hrflow.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

class ClientAddressForwardingFilterTest {
    @Test
    void replacesUntrustedClientHeaderWithTheConnectionAddress() {
        var request = MockServerHttpRequest.post("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("192.0.2.44", 43210))
                .header(ClientAddressForwardingFilter.HEADER, "203.0.113.99")
                .build();
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        new ClientAddressForwardingFilter().filter(MockServerWebExchange.from(request), exchange -> {
            forwarded.set(exchange);
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertEquals("192.0.2.44", forwarded.get().getRequest().getHeaders()
                .getFirst(ClientAddressForwardingFilter.HEADER));
    }

    @Test
    void forwardsAnUnresolvedConnectionHostWithoutFailing() {
        var request = MockServerHttpRequest.post("/api/v1/auth/login")
                .remoteAddress(InetSocketAddress.createUnresolved("edge.internal", 43210))
                .build();
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        new ClientAddressForwardingFilter().filter(MockServerWebExchange.from(request), exchange -> {
            forwarded.set(exchange);
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertEquals("edge.internal", forwarded.get().getRequest().getHeaders()
                .getFirst(ClientAddressForwardingFilter.HEADER));
    }
}
