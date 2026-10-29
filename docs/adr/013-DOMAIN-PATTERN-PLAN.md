# ADR 013 — Domain pattern plan and Strategy admission

## Status and scope

Accepted as design guidance on 2026-10-27 after the user's request to finalize the pattern plan. This record refines pattern selection within the seven-service architecture; it does not change ADR 008–012, approve unresolved HR policy, add framework dependencies, or claim that planned domains are implemented. The current roadmap capacity and quality gates remain in force.

## Decision

Use the smallest implementation that protects each domain invariant. A pattern is required when the accepted architecture or a tested behavior needs it; a named object pattern is conditional until its admission test passes. Keep domain calculations independent of HTTP, JPA and Kafka where practical. Controllers translate transport, application services coordinate owner transactions, repositories persist owner rows, and adapters translate external I/O. Spring dependency injection wires those responsibilities.

### Non-overengineering rule

Use familiar enterprise building blocks already selected for HRFlow: Spring Security Resource Server, Spring Data JPA, Flyway/PostgreSQL, Kafka/Avro, the owner outbox and consumer inbox, Nuxt and Flutter. Within a feature, default to one cohesive service, explicit types and methods, a repository where persistence is needed, and a transaction at the owner boundary. A record, enum, switch or focused function is preferable when it expresses the rule clearly.

Do not add a generic command bus, per-endpoint use-case/interface pair, plugin registry, custom authentication framework, extra service, queue, database, paid dependency or pass-through layer merely to resemble an enterprise diagram. A new abstraction must reduce demonstrated duplication, isolate external I/O or protect an accepted variation; its owner must be able to name the current requirement, the simpler tested baseline, the concrete pain and the verification that behavior stays the same. Remove an abstraction if it adds navigation or operating cost without such value. Preserve the approved security, tenant, concurrency, outbox/inbox, backup and replay guarantees even when their implementation is necessarily more involved than CRUD.

| Domain | Required architecture and workflow patterns | Conditional named pattern and trigger |
| --- | --- | --- |
| Identity and access | Independent gateway/service Resource Server JWT gates, explicit refresh-session lifecycle, role/resource policy and tenant-scoped persistence | None selected. Do not add a custom JWT filter, auth Factory or validation Chain. |
| Organization | Owner aggregates and constraints, optimistic version/ETag checks, retry-sensitive command receipts, typed snapshot outbox, single-leader relay and consumer inbox/projection | Test-only Builder if two real employee/shift fixture variants make plain construction error-prone. The advisory lock remains required by ADR 008. |
| Attendance | Explicit session transitions, append-only evidence/corrections, pure daily-status calculation, versioned projection, cutoff event, command receipt and event inbox/outbox | Scorer Strategy only after rules and model scorers are both maintained and a common interface reduces actual duplication. Do not split each daily status into a GoF State object. |
| Leave | Explicit request transitions, immutable balance ledger, guarded reservation update, compensating ledger entry and lifecycle outbox | `LeaveTypePolicy` Strategy only after approved leave types have two materially different calculation, eligibility or validation behaviors. The ledger and concurrency guard remain common to every policy. |
| Requests and approvals | Explicit request/step transitions, concrete approval-step rows, first-decision-wins update, relationship authorization and workflow outbox | `RequestTypeHandler` Strategy only when two accepted request types require distinct validation or fulfillment. An attachment Storage Adapter is optional after external I/O isolation is demonstrated. The approval engine stays shared. |
| Notifications | Event recipient rules, rendered templates, inbox deduplication and durable channel attempts with bounded retry | A channel Adapter when an external provider exists. Do not add Observer or Decorator classes merely because Kafka delivers events or retry is needed. |
| Reporting and audit | CQRS read projections, idempotent consumer, replay/rebuild procedure, scoped export jobs and audit access rules | Specification only when at least two real filter combinations make plain queries hard to audit; mandatory tenant scope is never an optional predicate. |
| Confidence worker | Versioned feature/result events, rules fallback, model provenance and human confirmation | Scorer Strategy only with two maintained implementations; a single rules function remains sufficient first. The worker never becomes attendance's source of truth. |

The word *state machine* denotes explicit allowed transitions, not the GoF State pattern. Outbox/inbox, ledger, CQRS read models and database concurrency guards are reliability designs, not a target count of GoF classes. No generic Saga framework is selected: leave/attendance compensation depends on the unresolved conflict policy in ADR 004.

## Strategy admission and implementation order

For leave, requests and confidence scoring, start with a tested plain method or switch. Admit Strategy only when all four conditions hold: a current approved behavior needs it; the plain version has demonstrable maintenance pain; two real variations or an isolation need exist; and the interface can be explained in two sentences. Record the two variations and a before/after test result in the owning week's evidence. A Strategy may select a domain rule; it may not supply tenant identity, authorize an actor, reserve a balance, or bypass a database constraint.

- Week 2 identity and Week 3 organization establish the shared security, transaction, tenant and event boundaries. The existing Week 3 local candidate remains an unaccepted release candidate.
- Week 4 attendance implements the pure status calculation and transition/late-event tests before optional inference abstraction. Week 5 proves durable notification and event recovery.
- Week 6 leave implements the ledger, conditional reservation and state transitions before considering a type policy. Do not automate leave/attendance conflict resolution until ADR 004 receives business approval.
- Week 7 requests implements shared approval steps and first-wins decisions before extracting type handlers or attachment storage adapters.
- Week 9 reporting implements scoped projections and rebuild before any Specification refactor. Confidence integration uses the existing bounded worker when attendance's rules and review flow are ready.

## Verification and consequences

Given a candidate refactor, when both variants are exercised, then the same tenant, role, stale-command, concurrent-write, event replay and failure results must hold before and after it. Domain tests verify the rule without a database; PostgreSQL and Kafka integration tests verify constraints, transaction boundaries, acknowledgment and replay. For a documentation-only decision, documentation lint and the foundation checks are proportional; no code or gate pass is inferred.

This plan adds no mandatory GoF refactor to the funded critical path. Factory, Chain of Responsibility, Decorator, GoF State and Template Method remain unselected. Builder, attachment Adapter and reporting Specification remain conditional Stretch candidates. Strategy is an explicit conditional candidate for leave, request types and confidence scoring, not a blanket rejection or a commitment to invent unapproved policy. New framework/layer adoption requires a separate concrete need and reviewed cost.
