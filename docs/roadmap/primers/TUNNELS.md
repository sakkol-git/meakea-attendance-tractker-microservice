# Outbound secure network tunnels

> **Note:** Secure outbound tunnels allow private self-hosted servers to expose web applications to the public internet securely without requiring static public IP addresses, dynamic DNS, or open inbound firewall ports.

## Purpose and core concept

An outbound secure tunnel creates an encrypted, bidirectional communication channel from a private host inside a local network to an external cloud edge network (such as Cloudflare).

Rather than opening inbound router ports (e.g., port forwarding ports 80 and 443), an agent daemon running on the local host initiates an outbound connection to the edge network. The edge network proxies incoming internet user requests through the tunnel down to the local server.

## Problem being solved

Self-hosting on residential, small-office, or SME internet connections encounters severe networking obstacles:

- Carrier-Grade NAT (CGNAT): Internet Service Providers (ISPs) frequently share a single public IP address across hundreds of customers, making inbound port forwarding technically impossible.
- Dynamic IP address changes: Residential and SME internet connections change IP addresses unpredictably, breaking standard DNS A records.
- Inbound attack surface: Opening ports 80 and 443 exposes the home/office router directly to port scanners, automated botnets, and Distributed Denial of Service (DDoS) attacks.

## Simple mental model

An outbound tunnel is like an employee making an outgoing phone call to a corporate receptionist and leaving the line open.

When an external customer calls the receptionist, the receptionist speaks over the existing open telephone line to relay the customer's question to the employee. The employee's home phone number is never disclosed, and their home phone never rings directly.

## How it works

Cloudflare Tunnel (`cloudflared`) establishes edge connectivity:

1. Outbound connection: The `cloudflared` daemon on the local server establishes multiple encrypted outbound connections (over QUIC or HTTP/2) to Cloudflare's nearest edge data centers.
1. DNS routing: Cloudflare DNS points the organization's domain (e.g., `hrflow.example.com`) to Cloudflare's anycast IP network.
1. Edge TLS termination: When a user visits the URL, Cloudflare edge servers terminate public TLS, inspect traffic for DDoS attacks, and authenticate certificates.
1. Tunnel dispatch: Cloudflare streams the HTTP request down the existing outbound tunnel connection to the local `cloudflared` daemon.
1. Local proxying: The local `cloudflared` daemon proxies the request to Caddy on loopback (`http://127.0.0.1:8080`).
1. Secure return: Caddy returns the response to `cloudflared`, which sends it back up the tunnel to the Cloudflare edge for delivery to the client.

## Why HRFlow needs it

In HRFlow's deployment baseline:

- Zero cloud infrastructure bill: Cloudflare provides free named tunnels (`cloudflared`), eliminating the need for paid cloud load balancers or public static IP add-ons.
- Router neutrality: Works behind any router, NAT, or cellular LTE/5G hotspot without requiring router administrator passwords or port forwarding.
- DDoS protection: Cloudflare's global edge network absorbs volumetric DDoS attacks before they reach the self-hosted hardware.

## Minimal example

A standard `cloudflared` configuration file (`config.yml`):

```yaml
tunnel: a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d
credentials-file: /etc/cloudflared/credentials.json

ingress:
  - hostname: hrflow.example.com
    service: http://127.0.0.1:8080
    originRequest:
      noTLSVerify: false
      connectTimeout: 30s
  - service: http_status:404
```

Running the daemon in Docker:

```bash
docker run -d --name hrflow-tunnel \
  --restart unless-stopped \
  --network host \
  -v /etc/cloudflared:/etc/cloudflared \
  cloudflare/cloudflared:latest tunnel run
```

## Application-specific implementation

Integration with Docker Compose and Caddy:

```yaml
services:
  caddy:
    image: caddy:2.8-alpine
    container_name: hrflow-caddy
    restart: unless-stopped
    ports:
      - "127.0.0.1:8080:80"
    volumes:
      - ./infra/caddy/Caddyfile:/etc/caddy/Caddyfile:ro
    networks:
      - internal-net

  tunnel:
    image: cloudflare/cloudflared:latest
    container_name: hrflow-tunnel
    restart: unless-stopped
    network_mode: host
    environment:
      TUNNEL_TOKEN: ${CLOUDFLARE_TUNNEL_TOKEN:?Tunnel token required}
    command: tunnel --no-autoupdate run
```

## Common mistakes and pitfalls

Avoid these dangerous tunnel configurations:

> **Pitfall:** Exposing administrative interfaces through the public tunnel without authentication. Never route database ports, Docker daemon sockets, or raw Spring Actuator management endpoints through a public tunnel hostname.

- Storing tunnel credentials in public repositories: The tunnel token or credentials JSON file grants permission to route traffic to your domain. Never commit tunnel credentials to version control.
- Single-point dependency assumption: Free Cloudflare Tunnels do not include a high-availability Service Level Agreement (SLA). If the local ISP experiences an outage or Cloudflare terminates the tunnel, remote access goes down.
- Forgetting local network fallback: When working in the office on the local LAN, configure local DNS or hosts entries so office devices can reach Caddy directly without hairpinning through the internet tunnel.

## Testing and debugging

Validation commands:

- Tunnel connection check: Run `cloudflared tunnel info <tunnel-name>` to verify that active connections exist to multiple edge data centers.
- DNS propagation check: Run `dig hrflow.example.com CNAME` to verify that the domain points to `<tunnel-id>.cfargotunnel.com`.
- End-to-end latency test: Execute `curl -Iv https://hrflow.example.com/api/v1/health` and verify that the request returns HTTP 200 OK with `cf-ray` edge tracking headers.

## Trade-offs and alternatives

Comparing remote ingress methods:

- Cloudflare Tunnel (HRFlow choice): Free, zero router configuration, works behind CGNAT, built-in edge TLS and DDoS protection; dependent on third-party cloud provider.
- Direct port forwarding: Requires public static IP, router admin access, dynamic DNS, and manual firewall hardening; zero third-party dependency.
- WireGuard / Tailscale VPN: Highly secure point-to-point networking; however, requires every employee mobile device to install and maintain an active VPN client profile.

## When not to use it

Do not use public tunnels for:

- Local-only offline deployments: If the organization operates in an air-gapped facility with zero internet connectivity, use local LAN routing with private internal certificates.
- Ultra-high bandwidth raw file streaming: Tunneling hundreds of terabytes of bulk raw video or binary file transfers may violate free tunnel terms of service.

## Related documentation

- [Reverse proxy and HTTPS primer](REVERSE-PROXY-HTTPS.md)
- [Docker Compose primer](DOCKER-COMPOSE.md)
- [Week 02 identity and host build guide](../02-IDENTITY-AND-HOST/BUILD-GUIDE.md)
- [Access setup runbook](../02-IDENTITY-AND-HOST/ACCESS-SETUP.md)
