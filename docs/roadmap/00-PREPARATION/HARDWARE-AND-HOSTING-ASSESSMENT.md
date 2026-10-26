# Hardware and hosting assessment

## Intended self-host target

On 2026-10-26 the user designated a separate, older Acer Nitro 5 as the intended self-host machine. The current Mac remains the development machine and is not the production target. These specifications are user-reported inventory, not command-verified host readiness.

| Item | User-provided fact | Status | Required W02-S24 evidence |
| --- | --- | --- | --- |
| Model | Acer Nitro 5; exact model/SKU unknown | **Recorded** | Laptop chassis profile recorded; exact SKU upon physical boot |
| CPU | Intel Core i5; exact generation/model unknown | **Recorded** | x86_64 architecture sized for 7 services + Kafka + DB |
| Memory | 16 GB RAM | **Recorded** | 9.5–11.5 GB steady budget sized; 4.5–6.5 GB headroom |
| Storage | 2 TB hard-disk capacity | **Recorded** | Disk IOPS risk analyzed; SSD recommended for DB/Kafka |
| OS/runtime | Linux Ubuntu Server 24.04 LTS target | **Targeted** | Native Docker Engine CE and systemd headless runtime |
| Availability | Continuous AC power; lid sleep disabled | **Planned** | `HandleLidSwitch=ignore` in systemd logind policy |
| Operator/network | Static LAN IP; Cloudflare Tunnel | **Planned** | Outbound tunnel; no router port-forwarding |
| Backup | Dedicated external USB 3.0 disk (ext4) | **Planned** | Independent encrypted destination; not internal partition |

The 2 TB capacity does not establish free headroom, health, performance or recoverability. Before container pulls or deployment acceptance, inspect the Acer directly and retain the safe command results in the Week 2 evidence record. A mechanical hard disk may be usable for a small pilot, but database, Kafka and container performance must be measured rather than inferred from capacity.

## Observed development host

| Item | Observed evidence | Status | Gate |
| --- | --- | --- | --- |
| Model | `sysctl -n hw.model`: Mac16,12 | **Verified** | Development machine only; do not accept as production-host evidence |
| Memory | `sysctl -n hw.memsize`: 17179869184 bytes | **Verified** 16 GiB | Load test before pilot |
| CPU | `sysctl -n hw.ncpu`: 10 | **Verified** logical CPU count | Do not infer exact marketed processor |
| OS | `sw_vers`: macOS 26.5.1 | **Verified** | Docker starts only after user login unless proven otherwise |
| Disk | `df -h .`: 22 GiB free, 89 percent capacity | **Partial** | Dev baseline only; 22 GiB available on `/System/Volumes/Data` |
| Docker | CLI 29.6.2, Compose v5.5.1; daemon absent | **Partial** | Desktop startup/reboot demonstrated |
| Backup disk | No external disk confirmed | **Unknown** | Dedicated mounted destination with free space and restore test |
| Public IP/router/ISP | Not inspected or provided | **Unknown** | Tunnel path selected; no router forwarding |
| Domain | Not provided | **Unknown** | Owned domain and DNS control before public TLS |

The disk threshold is a conservative pilot requirement, not a measurement of HRFlow's future footprint. Do not delete user files automatically. The Mac's low free space remains a development constraint but is no longer the production-host capacity gate.

## Check each unknown

1. Inspect the designated Acer Nitro 5 directly and confirm it can remain powered and awake during agreed hours; do not substitute Mac measurements.
1. Inspect disk health with the Acer's OS/OEM SMART or diagnostic tool and record the read-only result; do not run repair without a backup.
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

The production target is confirmed as the Acer Nitro 5, but its exact model/CPU, OS, usable RAM, free and healthy disk, Java/Docker runtime, domain/DNS control, external backup target, power policy and unattended reboot behavior remain unresolved. If blocked in week 2, keep deployment and remote exposure **FAIL** until those prerequisites pass. Never call localhost on the development Mac a successful production deployment on the Acer.
