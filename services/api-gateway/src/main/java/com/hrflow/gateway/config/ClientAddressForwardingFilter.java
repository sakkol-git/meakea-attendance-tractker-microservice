package com.hrflow.gateway.config;

import java.net.InetSocketAddress;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ClientAddressForwardingFilter implements GlobalFilter, Ordered {
    public static final String HEADER = "X-HRFlow-Client-Address";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        String source = clientAddress(remoteAddress);
        var request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(HEADER);
            headers.set(HEADER, source);
        }).build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private String clientAddress(InetSocketAddress remoteAddress) {
        if (remoteAddress == null) {
            return "unknown";
        }
        return remoteAddress.getAddress() == null
                ? remoteAddress.getHostString()
                : remoteAddress.getAddress().getHostAddress();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
