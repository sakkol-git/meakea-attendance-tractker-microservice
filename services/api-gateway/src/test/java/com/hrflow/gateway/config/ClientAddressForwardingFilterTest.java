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

    private static String forward(String trusted, String peer, String xff) {
        var builder = MockServerHttpRequest.post("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress(peer, 43210));
        if (xff != null) builder.header("X-Forwarded-For", xff);
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        new ClientAddressForwardingFilter(trusted).filter(MockServerWebExchange.from(builder.build()), exchange -> {
            forwarded.set(exchange);
            return reactor.core.publisher.Mono.empty();
        }).block();
        return forwarded.get().getRequest().getHeaders().getFirst(ClientAddressForwardingFilter.HEADER);
    }

    @Test
    void trustedProxyForwardedClientBecomesTheThrottleSource() {
        assertEquals("203.0.113.7", forward("172.18.0.0/16", "172.18.0.5", "203.0.113.7"));
    }

    @Test
    void untrustedPeerCannotSupplyForwardedClient() {
        assertEquals("192.0.2.44", forward("172.18.0.0/16", "192.0.2.44", "203.0.113.7"));
    }

    @Test
    void forwardedHeaderIsIgnoredWhenNoProxyIsTrusted() {
        assertEquals("172.18.0.5", forward("", "172.18.0.5", "203.0.113.7"));
    }

    @Test
    void spoofedLeftEntriesAreSkippedInFavourOfTheRightmostUntrustedHop() {
        assertEquals("203.0.113.7", forward("172.18.0.0/16", "172.18.0.5", "198.51.100.1, 203.0.113.7, 172.18.0.9"));
    }

    @Test
    void malformedForwardedValueFallsBackToTheConnectionAddress() {
        assertEquals("172.18.0.5", forward("172.18.0.0/16", "172.18.0.5", "not-an-ip.example"));
    }
}
