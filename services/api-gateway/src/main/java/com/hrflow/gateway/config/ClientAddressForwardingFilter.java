package com.hrflow.gateway.config;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Forwards the throttle source address. X-Forwarded-For is honoured only when the TCP peer is a
 * configured trusted proxy (the edge reverse proxy), so direct callers cannot choose their source.
 */
@Component
public class ClientAddressForwardingFilter implements GlobalFilter, Ordered {
    public static final String HEADER = "X-HRFlow-Client-Address";
    private static final Pattern IP_LITERAL = Pattern.compile("[0-9a-fA-F:.]+");

    private final List<Cidr> trustedProxies;

    public ClientAddressForwardingFilter() {
        this("");
    }

    @Autowired
    public ClientAddressForwardingFilter(@Value("${hrflow.gateway.trusted-proxies:}") String trustedProxies) {
        this.trustedProxies = Arrays.stream(trustedProxies.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).map(Cidr::parse).toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        String source = clientAddress(remoteAddress,
                exchange.getRequest().getHeaders().get("X-Forwarded-For"));
        var request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(HEADER);
            headers.set(HEADER, source);
        }).build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private String clientAddress(InetSocketAddress remoteAddress, List<String> forwardedFor) {
        if (remoteAddress == null) {
            return "unknown";
        }
        if (remoteAddress.getAddress() == null) {
            return remoteAddress.getHostString();
        }
        InetAddress peer = remoteAddress.getAddress();
        String connection = peer.getHostAddress();
        if (forwardedFor == null || !isTrusted(peer)) {
            return connection;
        }
        List<String> hops = new ArrayList<>();
        forwardedFor.forEach(line -> Arrays.stream(line.split(",")).map(String::trim)
                .filter(value -> !value.isEmpty()).forEach(hops::add));
        // Walk right to left: the rightmost hop not owned by a trusted proxy is the real client.
        for (int index = hops.size() - 1; index >= 0; index--) {
            InetAddress hop = literal(hops.get(index));
            if (hop == null) {
                return connection;
            }
            if (!isTrusted(hop)) {
                return hop.getHostAddress();
            }
        }
        return connection;
    }

    private boolean isTrusted(InetAddress address) {
        return trustedProxies.stream().anyMatch(range -> range.contains(address));
    }

    private static InetAddress literal(String value) {
        if (!IP_LITERAL.matcher(value).matches()) {
            return null;
        }
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException invalid) {
            return null;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private record Cidr(byte[] network, int prefix) {
        static Cidr parse(String text) {
            String[] parts = text.split("/", 2);
            InetAddress address = literal(parts[0]);
            if (address == null) {
                throw new IllegalArgumentException("Invalid trusted proxy address: " + text);
            }
            int bits = address.getAddress().length * 8;
            int prefix = parts.length == 2 ? Integer.parseInt(parts[1]) : bits;
            if (prefix < 0 || prefix > bits) {
                throw new IllegalArgumentException("Invalid trusted proxy prefix: " + text);
            }
            return new Cidr(address.getAddress(), prefix);
        }

        boolean contains(InetAddress candidate) {
            byte[] bytes = candidate.getAddress();
            if (bytes.length != network.length) {
                return false;
            }
            for (int bit = 0; bit < prefix; bit++) {
                int mask = 0x80 >> (bit % 8);
                if ((bytes[bit / 8] & mask) != (network[bit / 8] & mask)) {
                    return false;
                }
            }
            return true;
        }
    }
}
