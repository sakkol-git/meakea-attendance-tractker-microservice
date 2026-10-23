# Decision register

## Final system design

- **Decision:** Seven Spring services in their existing roots, reactive API gateway, explicit JDBC, PostgreSQL 16 service schemas, Kafka KRaft, Avro/Schema Registry, Nuxt/Vue/TypeScript, Flutter and Python confidence service.
- **Reason:** User explicitly accepted the latest Architecture as final; this supersedes ADR 006 monolith/React simplification.
- **Rejected alternatives:** Monolith, React migration, replacing Kafka with a local dispatcher. Cloud execution was initially requested in twelve weeks and then explicitly deferred as future improvement.
- **Implementation impact:** Align seven builds, replace scaffold JPA/MVC gateway drift, preserve service-owned schemas and event-built read models. Outbox producer acknowledgment and consumer effect/inbox/offset ordering require real Kafka tests.
- **Cost and operational impact:** Local Compose on one confirmed computer; no paid service required by the current finish line. Hardware, disk, power/domain/provider accounts are still real dependencies. The approved cloud topology stays documented but unbuilt.
- **Reconsider when:** User changes the design or measured compatibility/resource/capacity evidence requires a superseding ADR. Never silently change topology to hide a schedule shortfall.

[ADR 008](../adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md), [final Architecture](../Architecture.md) and [capacity review](ARCHITECTURE-REVIEW-V2.md) govern. No guarantee of twelve-week feasibility: the revised planning demand currently exceeds available backend hours.

## Fixed choices

| Area | Choice | Evidence or implementation target | Gate |
| --- | --- | --- | --- |
| Java/build | Java 21, Maven, root Boot 4.1.1 | pom.xml; service 4.0.8 drift must be aligned after compatibility spike | W1 compile/tests |
| Web | Existing Nuxt/Vue TypeScript | web/package.json and lockfile | W1 build, W8 integration |
| Mobile | Flutter, inspect actual root first | No existing Flutter source inferred | W1 mock, W8 real tests |
| HTTP contract | Proposed contracts/openapi.yaml | One versioned /api/v1 authority, not yet installed | W1 contract CI |
| Persistence | JDBC and Flyway, schema per owner | Existing physical identity_org retained | W3 constraints/RLS |
| Events | Kafka/Avro/Registry plus owner outbox and consumer inbox | libs/hrflow-contracts/events actual root | W2 spike, W5 crash/replay |
| Auth | Gateway JWT, hashed rotating refresh family, method/resource roles | No trusted caller identity headers | W2 security negatives |
| Host | Local Compose, private until safe | Host not ready; exact resources measured before pull | W2 private host, W10 restore |
| External access | Cloudflare Tunnel to private edge/gateway origin | Domain/DNS unknown; no automatic public exposure | HTTPS/auth/firewall/backup |
| Backups | Nightly custom pg_dump, 14 days and external encrypted copy | Include private files, Registry schemas/config and replay position | W2 first, W10/12 clean restore |
| Observability | Structured logs, Actuator/Micrometer, Prometheus/Grafana and uptime alert | Private endpoints; bounded labels, no personal data | Incremental W2–9 |
| Cloud | Preserve final AWS/Terraform/Kubernetes design as future | Latest explicit user deferral | No Week-12 cloud claim |

## Scope and quality protection

All existing Must business features stay requirements until explicitly cut: auth/tenant, organization, attendance/corrections, leave/approval, material/special requests, in-app notification, audit, basic CSV, Nuxt and Flutter, event reliability, deployment and restore. The full final design also describes confidence/event integration; its capacity is not hidden as a free add-on.

Move visual polish, extra reports, optional push/SSE and unadmitted pattern refactors to Stretch first. Do not drop security, tenant/race/replay tests or backup restore to recover time. AWS/Kubernetes execution is deferred by user instruction; no other required service is silently removed. Optional features must not become prerequisites of later Must work.

## Boundary and policy limits

Tenant comes from a verified JWT at the gateway, not body/path/caller headers. Final header trust requires tested sanitization and private ingress; it does not excuse method/resource authorization or scoped SQL/RLS. [ADR 007](../adr/007-QUALITY-AND-TENANT-DEFENSE.md) quality clauses carry forward except its superseded local-dispatch topology.

Raw attendance evidence is append-only; approved manual interpretation takes precedence over inference. Leave NEEDS_REVIEW retains debit until explicit HR resolution. HR policy/retention approval remains unknown. One computer is not high availability; restore evidence does not claim compliance certification.
