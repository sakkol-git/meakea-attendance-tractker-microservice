# Private host and tunnel access setup

## Preconditions and safe order

Production setup is not ready. First confirm the exact host, at least 30 GiB free, Docker runtime, domain/DNS control, external encrypted disk and operator. Complete W02 security tests and restore before enabling a public hostname. Until then deploy loopback/private LAN only and mark external access **FAIL**. Never forward router ports or publish database/monitoring administration.

## Caddy is the application gateway

Create proposed `infra/selfhost/Caddyfile`. This private origin example assumes the new app listens on internal 8080 and React production files are mounted read-only under `/srv/web`. It is not a config already present in the repository.

```text
http://:80 {
    handle /api/* {
        reverse_proxy app:8080
    }
    handle {
        root * /srv/web
        try_files {path} /index.html
        file_server
    }
}
```

Only cloudflared and approved internal clients can reach the origin network in public tunnel mode. Do not route Actuator/diagnostic/admin paths through the public proxy. Validate upstream auth and resource authorization with direct API attempts; static navigation hiding is not security. Add response security headers and explicit body/time limits in the tested deployment config before public release.

## Configure outbound access

1. Use the owned domain in Cloudflare DNS; verify current plan/registrar terms and actual annual price privately. Tunnel publishing itself does not require a paid Access plan.
1. Create a named Tunnel in the account you control. Copy connector credentials into a private host secret file with restrictive permissions, never Git or this guide.
1. Add cloudflared to the private Caddy network only, with restart/log bounds. Use the provider's documented token-file mechanism for the chosen locked image version; inspect its help before configuring secrets. Do not print the token in command history or Compose rendered output.
1. Map only the application hostname to `http://caddy:80`; no Docker/Kuma/PostgreSQL/Kafka routes. Public HTTPS terminates automatically at Cloudflare; origin is private HTTP. Do not claim a Caddy ACME certificate was issued in this mode.
1. Before enabling the hostname, rerun security, firewall/published-port, secrets-redaction and external-copy restore gates. Test invalid/missing tokens and cross-tenant IDs from a phone on a different network.
1. Enable only the approved hostname; inspect the browser certificate and confirmed issuer/expiry. Test refresh cookies/origins/CORS on the exact hostname, not merely localhost.
1. Disable connector and confirm remote access fails while local data/private authenticated access remain intact. Record ISP/provider dependency and restart behavior.

Use the official [Tunnel setup](https://developers.cloudflare.com/tunnel/get-started/) and [publishing guide](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/routing-to-tunnel/) for current dashboard terminology. Their UI can change; verify in your account rather than following invented screenshots.

## Week 2 host gate

- [ ] Private confirmed-host skeleton redeployed, not merely development localhost.
- [ ] Auth/tenant/resource tests and rotating refresh tests pass.
- [ ] HTTPS certificate/origin flow is verified before external access.
- [ ] Firewall/private networks expose none of database, Docker or monitoring administration.
- [ ] Encrypted external copy and scratch restore pass before hostname goes live.
- [ ] Unknown domain/router/host issues have owner and recovery action; remote gate stays **FAIL**.

## Friday repeat

Run the deployment manifest/smoke script on the real host with the exact checked image. Preserve previous digest/config and test a known authenticated request from React and Flutter. Backup/restore still run when remote access is blocked; label private versus public result separately. Complete final operational drills with the Week 10 host runbook.
