# Decision register

## Final system design

- **Decision:** Seven Spring services in their existing roots, reactive API gateway, Spring Data JPA (Hibernate) for persistence, PostgreSQL 16 service schemas, Kafka KRaft, Avro/Schema Registry, Nuxt/Vue/TypeScript, Flutter and Python confidence service.
- **Reason:** User explicitly accepted the latest Architecture as final; this supersedes ADR 006 monolith/React simplification. Persistence uses JPA per ADR-009: team familiarity and time are the deciding factors; design patterns (Specification, Strategy, Template) address complex queries before raw SQL is considered.
- **Rejected alternatives:** Monolith, React migration, replacing Kafka with a local dispatcher. JdbcClient rejected (more boilerplate, new concept to learn). Cloud execution was initially requested in twelve weeks and then explicitly deferred as future improvement.
- **Implementation impact:** Align seven builds, remove scaffold MVC from gateway drift, preserve service-owned schemas and event-built read models. Flyway is the only schema owner (`ddl-auto: validate`); `open-in-view: false` in every service. Outbox claiming uses one native `@Query` with `FOR UPDATE SKIP LOCKED`. Outbox producer acknowledgment and consumer effect/inbox/offset ordering require real Kafka tests.
- **Cost and operational impact:** Local Compose on one confirmed computer; no paid service required by the current finish line. Hardware, disk, power/domain/provider accounts are still real dependencies. The approved cloud topology stays documented but unbuilt.
- **Reconsider when:** User changes the design or measured compatibility/resource/capacity evidence requires a superseding ADR. Never silently change topology to hide a schedule shortfall.

[ADR 008](../adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md), [ADR 009](../adr/009-JPA-OVER-JDBC.md), [ADR 010](../adr/010-DUAL-JWT-VALIDATION.md), [ADR 011](../adr/011-SPRING-RESOURCE-SERVER-JWT.md), [final Architecture](../Architecture.md) and [capacity review](ARCHITECTURE-REVIEW-V2.md) govern. ADR 010 supersedes only ADR 008's gateway-header trust clause; ADR 011 supersedes only ADR 010's custom-filter implementation mechanism. The initial coarse demand exceeded available backend hours; final decomposed allocations were within the ceiling before this decision delta. Measure the dual-validation rework against remaining reserve and replan if it exceeds that reserve; no completion guarantee is implied.

## Fixed choices

| Area | Choice | Evidence or implementation target | Gate |
| --- | --- | --- | --- |
| Java/build | Java 21, Maven, root Boot 4.1.1 | pom.xml; service 4.0.8 drift must be aligned after compatibility spike | W1 compile/tests |
| Web | Existing Nuxt/Vue TypeScript | web/package.json and lockfile | W1 build, W8 integration |
| Mobile | Flutter, inspect actual root first | No existing Flutter source inferred | W1 mock, W8 real tests |
| HTTP contract | Proposed contracts/openapi.yaml | One versioned /api/v1 authority, not yet installed | W1 contract CI |
| Persistence | Spring Data JPA, Flyway owns schema, `ddl-auto: validate`, `open-in-view: false` | ADR-009; `spring-boot-starter-data-jpa`; outbox via native `@Query` | W1 context test, W3 constraints/RLS |
| Events | Kafka/Avro/Registry plus owner outbox and consumer inbox | libs/hrflow-contracts/events actual root | W2 spike, W5 crash/replay |
| Auth | Spring Security OAuth2 Resource Server validates bearer JWT independently at gateway and each Java service; hashed rotating refresh family; method/resource roles | ADR 010 boundary and ADR 011 mechanism; no trusted caller identity headers or custom JWT filter | W2 gateway/identity negatives; W3 remaining service gates |
| Host | Local Compose, private until safe | Host not ready; exact resources measured before pull | W2 private host, W10 restore |
| External access | Cloudflare Tunnel to private edge/gateway origin | Domain/DNS unknown; no automatic public exposure | HTTPS/auth/firewall/backup |
| Backups | Nightly custom pg_dump, 14 days and external encrypted copy | Include private files, Registry schemas/config and replay position | W2 first, W10/12 clean restore |
| Observability | Structured logs, Actuator/Micrometer, Prometheus/Grafana and uptime alert | Private endpoints; bounded labels, no personal data | Incremental W2–9 |
| Cloud | Preserve final AWS/Terraform/Kubernetes design as future | Latest explicit user deferral | No Week-12 cloud claim |

## Scope and quality protection

All existing Must business features stay requirements until explicitly cut: auth/tenant, organization, attendance/corrections, leave/approval, material/special requests, in-app notification, audit, basic CSV, Nuxt and Flutter, event reliability, deployment and restore. The full final design also describes confidence/event integration; its capacity is not hidden as a free add-on.

Move visual polish, extra reports, optional push/SSE and unadmitted pattern refactors to Stretch first. Do not drop security, tenant/race/replay tests or backup restore to recover time. AWS/Kubernetes execution is deferred by user instruction; no other required service is silently removed. Optional features must not become prerequisites of later Must work.

## Boundary and policy limits

Tenant comes from the JWT independently verified by the destination service, never from body/path/caller headers or a gateway assertion. The gateway performs the first validation gate and forwards the original bearer credential; the service performs the second gate before method/resource authorization and scoped persistence/RLS. Private ingress and header sanitization remain required defense in depth. [ADR 007](../adr/007-QUALITY-AND-TENANT-DEFENSE.md) quality clauses carry forward except its superseded local-dispatch topology.

Raw attendance evidence is append-only; approved manual interpretation takes precedence over inference. Leave NEEDS_REVIEW retains debit until explicit HR resolution. HR policy/retention approval remains unknown. One computer is not high availability; restore evidence does not claim compliance certification.

## Web and packaging execution choices

Nuxt is a static client with ssr:false for the MVP: run generate and serve .output/public through the private Caddy edge. Java identity owns cookie/CSRF token transport; no new BFF runtime is added. One pinned generic Java Service.Dockerfile/build-images script packages the existing owner modules from verified Maven jar paths. Week 2 deploys the private foundation profile, Week 3 adds reviewed owner contexts/images, and later endpoints/features pass their own gates. Cloud deployment and richer SSR remain future decisions, not silently added services.
