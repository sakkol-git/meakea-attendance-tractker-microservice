# Reverse proxy and HTTPS ingress

> **Note:** A reverse proxy acts as an intermediary gateway between incoming client requests and backend application services, providing routing, TLS termination, request buffering, and origin shielding.

## Purpose and core concept

A reverse proxy sits in front of backend web applications to receive all incoming network traffic. Unlike a forward proxy (which routes client traffic out to the internet), a reverse proxy routes internet traffic inward to private backend services.

In modern self-hosted architectures, reverse proxies terminate Transport Layer Security (TLS/HTTPS), compress responses, manage HTTP headers, and protect internal application runtimes from direct exposure.

## Problem being solved

Directly exposing application servers (such as Spring Boot on port 8080 or Node.js on port 3000) to the public internet creates major security and reliability hazards:

- Complex TLS certificate renewal: Java applications require Java KeyStore (JKS) files and restart cycles to rotate Let's Encrypt certificates, complicating certificate automation.
- Slow-client attacks: Directly connected slow clients tie up application worker threads with slow HTTP headers (Slowloris attacks).
- Origin disclosure: Exposing internal application ports directly reveals technology stacks, runtime versions, and internal network layouts.

## Simple mental model

A reverse proxy is like a receptionist at the front desk of a corporate office. Visitors cannot walk directly into private engineering cubicles or executive boardrooms.

Instead, the receptionist checks visitor badges, verifies credentials, handles basic inquiries, and escorts approved guests to specific conference rooms.

## How it works

Caddy acts as HRFlow's lightweight edge reverse proxy:

1. Client connection: A web browser or mobile client establishes an HTTP/HTTPS connection to Caddy on the edge.
1. TLS negotiation: Caddy negotiates TLS certificates automatically using ACME protocols (Let's Encrypt) or handles internal HTTP when paired with Cloudflare Tunnel.
1. Header enrichment: Caddy appends standard forwarding headers (`X-Forwarded-For`, `X-Forwarded-Proto`, `X-Real-IP`) to pass client IP information to the backend.
1. Upstream routing: Caddy forwards the request over the private internal Docker bridge network to the Spring Boot application container on port 8080.
1. Response buffering: Caddy receives the backend response, compresses text payloads using Gzip or Zstandard, and streams the response back to the client.

## Why HRFlow needs it

In HRFlow's self-hosted deployment:

- Unified entry point: Caddy provides a single port 80/443 ingress, routing `/api/*` to the reactive Spring API gateway and serving generated Nuxt static assets.
- Cloudflare Tunnel integration: When external traffic arrives through Cloudflare Tunnel, Caddy serves as the private origin server, validating hostnames and stripping unauthorized headers.
- Header security: Caddy injects mandatory HTTP security headers (`Content-Security-Policy`, `X-Content-Type-Options`, `Strict-Transport-Security`) globally.

## Minimal example

A concise, production-ready `Caddyfile`:

```text
:80 {
    # Security headers
    header {
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        Referrer-Policy "strict-origin-when-cross-origin"
        Strict-Transport-Security "max-age=31536000; includeSubDomains"
    }

    # Route API traffic to reactive API gateway
    handle /api/* {
        reverse_proxy api-gateway:8080
    }

    # Route Actuator health check
    handle /actuator/health {
        reverse_proxy api-gateway:8080
    }

    # Default fallback
    handle {
        respond "HRFlow Gateway Active" 200
    }
}
```

## Application-specific implementation

Spring Boot configuration for reverse proxy awareness in `application.yaml`:

```yaml
server:
  port: 8080
  forward-headers-strategy: framework
  tomcat:
    remote-ip-header: x-forwarded-for
    protocol-header: x-forwarded-proto
    internal-proxies: "10\\..*|192\\.168\\..*|172\\..*"
```

This configuration ensures that `request.getRemoteAddr()` and `request.isSecure()` reflect the actual client IP and HTTPS protocol rather than the internal Docker proxy connection.

## Common mistakes and pitfalls

Avoid these critical proxy misconfigurations:

> **Pitfall:** Failing to configure `forward-headers-strategy: framework` in Spring Boot. If Spring Boot is unaware of the reverse proxy, generated redirect URLs (such as OAuth redirects or pagination links) use `http://` instead of `https://`, causing mixed-content browser errors.

- Misconfigured trusted proxies: Trusting `X-Forwarded-For` from any IP address allows malicious clients to spoof their client IP address by simply attaching a fake header. Always restrict trusted proxies to known private subnet ranges.
- Exposing internal actuator endpoints publicly: Never expose `/actuator/env` or `/actuator/heapdump` through the reverse proxy. Restrict administrative endpoints to localhost or private VPNs.
- Missing request size limits: Omitting `request_body` limits allows clients to send multi-gigabyte POST bodies, exhausting server RAM. Always enforce strict upload limits (e.g., 10 MB for receipts).

## Testing and debugging

Validation commands:

- Header inspection: Run `curl -I http://localhost:8080/api/v1/health` and verify that security headers (`X-Content-Type-Options`, `X-Frame-Options`) are present.
- Forwarding verification: Send a request through Caddy with a test header and inspect Spring Boot application logs to confirm that the client IP is parsed accurately.
- Caddyfile validation: Run `caddy validate --config infra/caddy/Caddyfile` to verify proxy configuration syntax before restarting containers.

## Trade-offs and alternatives

Reverse proxy comparison:

- Caddy (HRFlow choice): Automatic TLS by default, simple human-readable configuration file, single binary, low memory footprint (30 MB).
- Nginx: High performance and extensive ecosystem; however, complex configuration syntax and requires external tools (Certbot) for Let's Encrypt certificate automation.
- Traefik: Excellent dynamic discovery for Kubernetes and Docker labels; however, steep configuration curve for static self-hosted topologies.

## When not to use it

Do not use a reverse proxy for:

- Business-service integration: HRFlow uses Kafka and local read models, not synchronous HTTP through a reverse proxy or Java calls into another service artifact.
- Raw database connections: PostgreSQL client connections should connect directly to port 5432 on the private network, not through an HTTP reverse proxy.

## Related documentation

- [Docker Compose primer](DOCKER-COMPOSE.md)
- [Outbound tunnels primer](TUNNELS.md)
- [Week 02 identity and host build guide](../02-IDENTITY-AND-HOST/BUILD-GUIDE.md)
- [Access setup runbook](../02-IDENTITY-AND-HOST/ACCESS-SETUP.md)
