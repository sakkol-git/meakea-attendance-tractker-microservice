# Decision register

## Architecture outcome C

- **Decision:** One Spring Boot application in proposed `services/hrflow-app`, packages `com.hrflow.app.identityorg`, `attendance`, `leave`, `request`, `notification`, and `reporting`; one Caddy edge gateway. No Kafka runtime for MVP.
- **Reason:** Seven services are startup scaffolds. The counted plan fits only provisional capacity with the fewest deployables.
- **Rejected alternatives:** Seven service deployments and a separately deployed worker add integration/operations work without existing behavior to preserve.
- **Implementation impact:** Preserve domain package/schema ownership, migrate endpoints without URL changes, keep PostgreSQL transactional outbox and per-handler inbox. Local workers replace Kafka delivery; no Kafka offsets exist on this path.
- **Cost and operational impact:** One app, PostgreSQL, Caddy, Uptime Kuma, and optional tunnel connector. No cloud database, Kubernetes, Terraform, Registry, or broker bill. Existing computer/power/domain/disk still cost money.
- **Reconsider when:** Two independently deployed components demonstrably need broker decoupling and a measured capacity budget supports migration.

[Capacity math](00-PREPARATION/PROJECT-BASELINE.md) and [migration](00-PREPARATION/MIGRATION-PLAN.md) are normative. The decision is settled for this guide; uncertain hours remain a feasibility assumption.

## Fixed choices

| Area | Choice | Implementation point | Gate |
| --- | --- | --- | --- |
| Java | Java 21 with existing root Boot 4.1.1 baseline | New app inherits reviewed parent; no framework upgrade | W1 compile |
| Web | React with TypeScript | Preserve Nuxt history, migrate deliberately | W1 thin view |
| Mobile | Flutter, repository location verified first | Mock generated from one OpenAPI file | W1 mock, W8 real integration |
| API | Proposed `contracts/openapi.yaml` sole wire authority | Preserve old domain routes, incremental implemented coverage | W1 validation |
| Persistence | PostgreSQL 16, schema per domain, Flyway | Never edit an applied migration | W1 and every deploy |
| Events | JSON outbox and transactional local handlers | Handler effects/inbox in DB transaction; external delivery separate | W5 recovery |
| Auth | JWT access, rotating hashed refresh families | Token tenant only; local admin provisioning separate | W2 negative tests |
| Host | Existing computer, Docker Compose | Private deployment in W2, weekly redeploy | W2 and W10 |
| Access | Cloudflare Tunnel to private Caddy origin | Unknown WAN/CGNAT; no port forwarding | External access blocked until safe |
| TLS | Automatic public TLS at Cloudflare edge | Caddy private origin not falsely advertised as ACME TLS | W2 verify chain |
| Backups | Nightly custom pg_dump, 14 days, external copy | Weekly scratch restore; no automatic production overwrite | W2 first restore |
| Monitoring | Actuator, Docker health/restart/log limits, Uptime Kuma | Private monitoring/admin only | W2 then W9 |
| Storage | Private host directory with metadata/backups | No S3 requirement; attachments capped and authorized | W7 |
| Notifications | Durable in-app inbox, local delivery adapter | Push should-have until provider/account/device proof | W5 |
| Patterns | Two admitted lessons, six conditional candidates | Baseline/pain before abstraction, no quota | Learning path |

## Scope tiers

- Must: auth/tenant and resource checks; org; attendance; leave/approval; material/special requests; in-app notifications; audit; basic CSV; React; Flutter; local outbox reliability; self-host deploy and tested restore.
- If stable: device push, SSE, geofence/Wi-Fi nudges, rule inference, EN/KH, extra reports, additional pattern refactors.
- Defer: ML, Kafka runtime, vision/camera, NFC, Redis, Kubernetes, Terraform, tracing stacks, dark mode, payroll, recruitment, continuous GPS, authoritative inventory.

## Product ambiguity decisions

Calendar and evidence are separate: holiday/leave never deletes work evidence. Manual approved corrections supersede inferred interpretations; overlap requires HR review. NEEDS_REVIEW retains leave debit until explicit HR cancellation or keep-leave resolution. Single same-day shift is MVP; reject unsupported overnight input. These are the guide's safe default policies, requiring HR approval before real data, not fabricated prior agreement.

## External dependencies

Cloudflare Tunnel free publishing requires domain/DNS control and relies on Cloudflare plus the ISP; there is no hosting SLA. Domain registrar price is unknown. Direct Caddy public TLS requires reachable 80/443 and is deferred unless new network evidence justifies an ADR change.
