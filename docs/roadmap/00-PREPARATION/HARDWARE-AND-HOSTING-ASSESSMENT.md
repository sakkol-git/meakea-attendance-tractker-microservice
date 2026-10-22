# Hardware and hosting assessment

## Observed local host

| Item | Observed evidence | Status | Gate |
| --- | --- | --- | --- |
| Model | `sysctl -n hw.model`: Mac16,12 | **Verified** | User confirms this is production machine |
| Memory | `sysctl -n hw.memsize`: 17179869184 bytes | **Verified** 16 GiB | Load test before pilot |
| CPU | `sysctl -n hw.ncpu`: 10 | **Verified** logical CPU count | Do not infer exact marketed processor |
| OS | `sw_vers`: macOS 26.5.1 | **Verified** | Docker starts only after user login unless proven otherwise |
| Disk | `df -h .`: about 5.1 GiB free, 98 percent capacity | **Blocked** | At least 30 GiB free before pulls; 50 GiB preferred |
| Docker | CLI 29.6.2, Compose v5.5.1; daemon absent | **Partial** | Desktop startup/reboot demonstrated |
| Backup disk | No external disk confirmed | **Unknown** | Dedicated mounted destination with free space and restore test |
| Public IP/router/ISP | Not inspected or provided | **Unknown** | Tunnel path selected; no router forwarding |
| Domain | Not provided | **Unknown** | Owned domain and DNS control before public TLS |

The disk threshold is a conservative pilot requirement, not a measurement of HRFlow's future footprint. Do not delete user files automatically. Review storage in macOS settings, archive with owner agreement, and rerun the disk check.

## Check each unknown

1. Confirm whether this computer is the production host and can remain powered and awake during agreed hours.
1. Inspect disk health in Disk Utility First Aid and record result; do not run repair during development without a backup.
1. Ask the ISP/router administrator for WAN address type, inbound restrictions, CGNAT, dynamic IP behavior, and router ownership. Compare router WAN IP with an external-IP site privately; do not paste addresses in this repo.
1. Run a browser bandwidth test at the host; record upload/download and latency privately, then test with two phones on different networks.
1. Verify firewall allows only approved ingress; inspect published Compose ports locally. Never expose PostgreSQL, Kafka, Docker API, or Uptime Kuma publicly.
1. Mount the external backup disk and record path, capacity, encryption, ownership, and unplugged behavior privately. A directory existing at an unmounted path is not a mounted disk.
1. Simulate a short power/network interruption after a valid backup and synthetic data. Record startup order, recovery, and missed cron behavior.
1. Agree pilot user count; use 5 to 10 synthetic or consenting pilot users as a test scenario, not a capacity promise.

## Chosen access path

Use Cloudflare Tunnel because router/public-IP capability is unknown. Tunnel traffic is outbound; no port forwarding is configured. Caddy remains the internal reverse proxy. Cloudflare terminates public automatic TLS; Caddy serves a private origin listener reachable only by cloudflared. Caddy public ACME is not claimed through an unconfigured tunnel.

The free Tunnel path requires no paid Access plan for publishing an app; budget for a domain at its registrar's actual price. Existing power, internet, external disk, and replacement hardware still have costs. There is no free-tier uptime guarantee. Internet or Cloudflare failure removes remote access; the data remains on the host. LAN emergency access must remain authenticated and private.

[Caddy HTTPS requirements](https://caddyserver.com/docs/quick-starts/https) and [Cloudflare published applications](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/routing-to-tunnel/) were checked 2026-10-23. A verified directly reachable host is new evidence that can justify an access ADR change; do not operate both public paths accidentally.

## Hard launch blockers

Disk headroom, target JDK, running Docker, domain/DNS control, external backup target, production-host confirmation, and unattended reboot behavior are unresolved. If blocked in week 2, redeploy privately on the real host and mark remote exposure **FAIL** until prerequisites pass. Never call localhost on the development laptop a successful production deployment on another machine.
