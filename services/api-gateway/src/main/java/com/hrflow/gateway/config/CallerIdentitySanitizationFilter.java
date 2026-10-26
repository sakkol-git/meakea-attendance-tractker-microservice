package com.hrflow.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

public class CallerIdentitySanitizationFilter implements GlobalFilter, Ordered
{

    private static final List<String> FORBIDDEN_IDENTITY_HEADERS = List.of(
            "X-Tenant-Id",
            "X-Employee-Id",
            "X-Roles",
            "X-Role",
            "X-User-Id"
    );


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var mutatedRequest = exchange.getRequest().mutate().headers(headers -> {
            for (var header : FORBIDDEN_IDENTITY_HEADERS) {
                headers.remove(header);
            }
        }).build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // Run with high precedence right before routing filters
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
