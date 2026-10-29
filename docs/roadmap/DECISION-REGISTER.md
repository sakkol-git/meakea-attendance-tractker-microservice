# Decision register

## Delegated remaining MVP decisions

On 2026-10-29 the user explicitly instructed the agent to decide remaining project choices, selecting affordable options that best fit and work with HRFlow. Apply this delegation within the agreed MVP and accepted architecture; preserve the user's earlier explicit policy selections. Record each material new policy or interface choice with its requirement, tradeoffs and verification. Prefer the existing stack and avoid speculative paid services. This does not provide missing real credentials or authorize commits, pushing or deployment.

## UAT private account setup

On 2026-10-29 the user explicitly approved a private local UAT setup command that creates accounts linked to explicitly selected organization employee records with documented roles, reads passwords privately, and leaves email invitation delivery for later. Do not infer employee links from names or email addresses. This authorizes implementation and disposable verification, not creating real accounts without explicit account inputs, exposing a public bootstrap endpoint, changing role vocabulary or deploying.

## UAT historical no-session schedule

On 2026-10-29 the user explicitly selected: “Preserve past days’ original shift rules; apply edits to today and future days.” Existing sessions retain their captured schedules. Past materialized daily results without sessions also retain their original verified schedule basis when organization shifts or employee assignments change. Use the employee's original local day boundary; do not fabricate historic schedules where no verified basis was ever captured. Calendar/leave corrections remain separate facts and may still refresh the result against its retained schedule. This decision does not define overnight shifts, retroactive HR corrections or automatic leave refunds.

## UAT general-request approval chain

On 2026-10-28 the user explicitly selected: “Manager → HR_ADMIN for every request type; skip a missing manager, as the docs specify.” The request owner selects this chain for every UAT request type, resolves MANAGER from the requester’s verified current organization relationship, and records a genuinely absent manager as SKIPPED. Unknown/unready relationship data is not an absent manager. The tenant HR_ADMIN stage remains independently required and shared among authorized HR admins; the first valid decision wins. Caller-supplied approver IDs/roles cannot override this policy. Submitted chains retain their approved snapshot; relationship/role revocation still requires server authorization. This decision does not authorize self-approval, new roles, automatic material fulfillment or notification delivery claims.

## UAT complementary leave portions

On 2026-10-28 the user explicitly selected: “Allow complementary halves; reject overlapping portions.” Independent MORNING and AFTERNOON requests may reserve their distinct equal halves on one employee/date, including different leave types. FULL_DAY conflicts with either half; the same half cannot be reserved twice while pending/approved/review. A terminal cancellation/rejection releases only its own reservations. Preserve database and concurrency guards.

## UAT unpaid leave accounting

On 2026-10-28 the user explicitly selected: “Yes, unpaid leave does not consume an entitlement balance.” Unpaid leave retains its request, actual reserved dates, decisions and audit history but does not reserve, consume or refund an entitlement balance. Do not fabricate an allocation or unlimited numeric balance; report untracked balance explicitly. Capture the payment/accounting mode with the request so later leave-type edits cannot change a pending settlement. This does not define payroll deductions or year-end policy.

## UAT leave across calendar years

On 2026-10-28 the user explicitly selected: “Require a separate request for each calendar year.” Both leave preview and submission reject a date range spanning two years with a clear validation error and no reservation, ledger or event side effects. Each year’s request uses its own year balance. This does not approve rollover/carry-over policy or partial cancellation semantics.

## UAT half-day attendance window

On 2026-10-28 the user explicitly selected: “Split the assigned shift into two equal time windows.” MORNING covers the first half of that employee’s effective assigned shift and AFTERNOON the second half. Use the actual same-day shift instants and owner working dates; remove the approved half from the expected attendance window. Work intersecting the approved half is flagged for HR review with debit unchanged. No fixed company-wide hours or lunch deduction is inferred.

## UAT attendance finalization timing

On 2026-10-28 the user explicitly selected: “Close at shift end + 30 minutes; allow 10 minutes early.” UAT daily absence/finalization therefore waits until the assigned, captured shift end plus 30 minutes. A completed session is early only when its final clock-out is strictly before shift end minus 10 minutes. Preserve owner timezone, grace and effective assignment; do not infer overnight or daylight-saving policy. Implementation and verification remain separate evidence gates.

## UAT attendance and approved-leave overlap

On 2026-10-28 the user explicitly selected: “Flag for HR review; keep debit unchanged.” For the UAT implementation, recorded work overlapping approved leave is retained as evidence and flagged for HR review. The leave debit stays unchanged and no automatic refund or cancellation is performed. This accepts that narrow behavior from proposed ADR 004; it does not approve unrelated retention, overnight/inference windows, disciplinary use or automatic balance compensation. Follow the owner event/projection boundaries and publish newer daily revisions when the underlying facts change. Implementation and executed verification are recorded separately in W04/W06/W09 evidence; this decision alone closes no code gate.

## Java mapping policy

On 2026-10-27 the user selected MapStruct for Java mappings wherever the mapping is straightforward and asked to retain custom mappers only for complex mapping. [ADR 014](../adr/014-MAPSTRUCT-MAPPING-POLICY.md) defines the boundary: MapStruct owns deterministic structural conversion, while explicit Java owns business rules, authorization, tenant derivation, external lookups, guarded updates and contextual event metadata. Apply this rule to new or materially changed mappings; do not fund an unrelated bulk rewrite or infer that generated mapping proves business acceptance.

## Domain pattern plan

The user finalized the domain pattern plan on 2026-10-27 and explicitly required a familiar enterprise architecture without overengineering. [ADR 013](../adr/013-DOMAIN-PATTERN-PLAN.md) makes plain feature code and the existing stack the default, preserves required reliability boundaries and makes Strategy a conditional candidate for approved leave-type variation, distinct request-type behavior, and maintained rules/model scorers. The admission test still applies; this decision adds no funded GoF refactor, changes no HR policy, and does not advance a weekly gate.

## Organization event wire

The user accepted the archived catalog's employee, shift and holiday snapshot shapes as the provisional v1 contract on 2026-10-27. [ADR 012](../adr/012-ORGANIZATION-EVENT-V1.md) owns the exact new Avro schema and compatibility limits; the archived catalog remains historical. Publication, replay and consumer acceptance still need executable evidence.

## Organization command receipt retention

On 2026-10-27 the user chose to keep command receipts indefinitely until a reviewed retention policy is approved. Do not add a cleanup job or expiry that could make an installed client retry create a second result. This is a temporary operational decision, not approval to retain personal data forever; storage growth and privacy policy still require review before production acceptance.

## Final system design

- **Decision:** Seven Spring services in their existing roots, reactive API gateway, Spring Data JPA (Hibernate) for persistence, PostgreSQL 16 service schemas, Kafka KRaft, Avro/Schema Registry, Nuxt/Vue/TypeScript, Flutter and Python confidence service.
- **Reason:** User explicitly accepted the latest Architecture as final; this supersedes ADR 006 monolith/React simplification. Persistence uses JPA per ADR-009: team familiarity and time are the deciding factors. Start with plain queries and methods; ADR 013 admits named patterns only when real variation or composition pain is demonstrated.
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
| Host | Local Compose on the separate Acer Nitro 5, private until safe | User reports Core i5, 16 GB RAM and 2 TB hard-disk capacity; exact model/CPU, OS, free space, disk health/performance, power policy and runtime must be measured on that Acer before acceptance. The current Mac is development-only. | W2 private host, W10 restore |
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

## UAT notification recipient authority

Under the user's delegated remaining-MVP decisions on 2026-10-29, notification recipients are resolved only from organization snapshots backed by explicitly linked enabled identity accounts. The existing optional userId is retained; a nullable/defaulted accountRoles list carries the documented role vocabulary. Legacy missing roles stay unknown, never an implicit HR grant. This avoids paid identity lookup services and cross-owner SQL joins while implementing approved notification §14 employee/manager/HR resolution. Compatibility must pass old/new Avro readers and actual Registry FULL_TRANSITIVE before this wire is accepted.

The MVP delivers in-app notices. Optional email, push and real-time SSE are not prerequisites. Approval-needed notices remain mandatory in-app even when optional in-app preferences are off; preference values are not silently rewritten. Generic templates contain only workflow state/link context, no private reasons, medical notes or file content. Unavailable authority stays retryable instead of substituting a recipient. This selects delivery behavior within the accepted critical-category rule, not a new approval chain or HR role.

### Delegated UAT event implementation choices (2026-10-29)

Under the user's instruction to choose remaining best-fit implementation details, attendance's daily wire adds nullable/default-null workRecorded to each leave reference. Only the attendance owner's captured-shift overlap calculation may populate it; null is legacy unknown and cannot request an automatic owner transition. The leave owner validates employee, portion, original reserved date and exact current approval revision before APPROVED → NEEDS_REVIEW. Debit and reservations remain unchanged, and no refund/resolution rule is added. This implements the previously approved HR-review choice; W06 records compatibility and broker evidence.

The existing attendance.correction.v1 topic now has a minimal canonical AttendanceCorrectionFact (employee, work date, state and standard envelope). Private proposed/original times and reason stay behind the authorized owner detail endpoint. Wire-zero legacy rows remain reconciliation input, not publishable accepted facts. This fills the existing correction publication/in-app Must using the current Kafka/Avro stack and existing authorization; it adds no channel, paid service or HR approval stage. W04/W05 evidence owns verification.
