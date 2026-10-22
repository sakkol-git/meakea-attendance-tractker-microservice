# Plan reference 01

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## 1. Purpose and baseline

Revision: 2026-10-23. Status: implementation plan, not a completion report. HRFlow is a placeholder product name for an event-driven multi-tenant HR platform. The twelve-week clock starts at an agreed kickoff; no calendar start date or historical completion is assumed. Contributor A owns backend, web, infrastructure, and operations preparation; contributor B owns Flutter and mobile validation. A tenant/product representative must decide HR policy; neither developer silently decides legal retention or leave precedence.

Source of truth: [architecture](../Architecture.MD) for boundaries/behavior, [API/database/event specification](../REST-API-DATABASE-DESIGN.md) for contracts, this document for sequence and acceptance. [Requirement IDs R01–R12](../Architecture.MD) connect work to tests. [Open questions O01–O08](../Architecture.MD) are explicit decision gates.

Current evidence: seven Java startup scaffolds, two partial shared libraries, Nuxt welcome page, local dependency Compose, and a standalone confidence prototype. There are no implemented business REST controllers, migrations, outbox/inbox workers, Flutter source here, CI workflows, or cloud deployment resources. The existing five Python tests and starter web build are useful but do not demonstrate HRFlow end to end. The earlier plan's week-12 vision service and Avro migration are not mandatory delivery: vision is unapproved optional scope, and serialization must be decided early rather than migrated during final hardening.

## 2. Goals, scope, and release tiers

Goal: demonstrate a reliable authenticated check-in, auditable leave/request approvals, and tenant-safe visibility, with recoverable asynchronous delivery. The smallest valuable slice is login → schedule projection → durable check-in → notification/report projection → visible client result. No payroll, recruitment, continuous GPS, biometric-template collection, or authoritative inventory management is included.

| Tier | Included capabilities | Exit standard |
|---|---|---|
| MVP | Auth/RBAC/tenant/resource checks; employee/shift/holiday setup; check-in/out and statuses; leave balances/approval; material and special requests; inbox and one push path; audit/corrections; CSV reports; reproducible deployment | R01–R10 tested end to end |
| Should-have | Geofence/Wi-Fi reminders; rule-based inference and next-day confirmation; SSE live view; EN/KH; XLSX | Only after correctness/recovery gates pass |
| Stretch | ML confidence integration, NFC, dark mode | Separate evidence and explicit capacity approval |
| Production launch | Secrets/TLS/grants; reviewed policies; monitoring; restore/rollback; security review and operator | Required before real users, even if demo MVP is complete |
| Future optional | Vision/camera processing, complex inventory, service extraction/scaling, EKS | Separate ADR and schedule |

Inference target ≥90% recovery ±10 minutes and battery target <3% remain pilot research targets, not MVP release promises. Do not spend the core platform schedule improving synthetic ML metrics.

## 3. Responsibilities and working agreements

| Responsibility | A | B | External decision owner |
|---|---|---|---|
| Domain/API/event/database design | Author and implement | Review client needs and fixtures | HR/product approves business ambiguity |
| Backend and shared event foundation | Primary | Test client retry/error behavior | Security reviewer for auth/isolation |
| Web | Primary; narrow screens first | Cross-check mobile parity | HR representative usability |
| Flutter/offline/device/push | Supply contracts and test environment | Primary | Device/platform account owner |
| Infrastructure/CI/restore | Primary | Smoke test release on devices | Operator/budget owner |
| Acceptance/demo | Joint | Joint | Product signs evidence, not undocumented claims |

A's backend/web/infra work is sequentially prioritized within each week, not three simultaneous full-time workstreams. B uses agreed JSON fixtures and mocks until the first slice is live. Contract changes require both contributors' review and updated examples/tests before implementation diverges. Daily integration uses synthetic tenant fixtures; weekly exit review records passing evidence and remaining blockers. No percentage-complete claims based on directories or dependencies alone.

## 4. Dependencies and work breakdown

| Work package | Prerequisites | Output | Requirement |
|---|---|---|---|
| WP1 foundation | Repository access, Java 21, Docker | Working toolchain/config, version/serialization/client decisions | R10 |
| WP2 contracts and mocks | O01/O06, policy assumptions | Auth/attendance API fixtures, event envelope, error model | R01–R04 |
| WP3 identity/security | WP1/WP2 | Tenant/user/employee/shift/holiday, JWT/refresh, grants | R01/R02 |
| WP4 reliability | WP1/WP2 | Migrations, outbox/inbox, compatibility/retry/replay tests | R04 |
| WP5 attendance slice | WP3/WP4 | Idempotent check-in/out, projection, minimal client views | R03/R07 |
| WP6 leave | Stable org snapshots, O02 | Locked reservations/ledger, approvals/conflicts | R05 |
| WP7 requests | Org/roles, WP4 | Advisory material forms and assigned chain | R06 |
| WP8 reporting/audit | Stable daily/leave/request facts | Corrections, authorized CSV, shadow rebuild | R08/R09 |
| WP9 delivery/operations | Reproducible build, O04/O05/O08 | Staging/pilot deploy, smoke, rollback, restore | R10 |
| WP10 hardening | WP5–WP9 | Failure/security/load/device evidence and demo | R01–R12 |

No feature team can assume a projection is populated merely because Kafka is running. Seed identity data through the owner API/migrations and verify downstream revisions before attendance acceptance.

## 5. Twelve-week roadmap

Each weekly gate is an exit condition, not a date-driven permission to move broken foundations forward. If a gate fails, use section 10 cut lines and explicitly revise scope; do not skip security/durability.

### Week 1 — Reproducible foundation and first contracts

- Owner: A primary; B reviews contract/client requirements.
- Dependencies: repository/tools; external O01 web choice, O06 serialization choice, O07 Maven policy.
- A sequence: correct documented startup/config defects; establish Java 21 build; choose inheritance without unnecessary upgrades; create first migrations/contracts and local launch instructions.
- B sequence: confirm Flutter repository/toolchain; create auth/home feature skeleton and mock API client.
- Deliverables: auth/check-in/today JSON schemas/examples, error/idempotency conventions, synthetic two-tenant fixtures, build/config checks, decision notes.
- Tests: clean dependency startup; schema generation; gateway route smoke; mock contract/example validation; full Java compile under target JDK.
- Acceptance/exit: fresh checkout can build the first modules, PostgreSQL initializes intended schemas, Registry connects if selected, and B can exercise mock success/error/retry shapes.
- Risks: existing wrong Compose mount/Registry listener, malformed YAML, Avro path, mixed parents; unresolved external Flutter location.

### Week 2 — Identity and reliability infrastructure

- Owner: A backend/foundation; B mobile auth.
- Dependencies: week 1; O08 authentication policy; O03 initial date/correction constraints.
- A sequence: implement login/refresh/logout, trusted tenant context and service authorization; then outbox/inbox and local integration harness.
- B: secure token storage, serialized refresh, logout, authentication errors; role-appropriate shell.
- Deliverables: synthetic tenant/user provisioning, employee seed path, rotating tokens, starter auto-configuration and migrations, durable relay/consumer example.
- Tests: forged headers/JWT, tenant ID substitution, refresh reuse, unique inbox, crash after broker ack, offset-after-commit behavior.
- Acceptance/exit: one employee can log in and refresh; wrong tenant cannot read data; business write survives relay crash without repeated effect.
- Risks: overbuilding identity UX; narrow admin provisioning before broad user management.

### Week 3 — Organization projections and first check-in

- Owner: A backend then minimal admin web; B profile/home/check-in client.
- Dependencies: week 2; stable employee/shift/holiday snapshots and topic keys.
- A sequence: owner CRUD for employee/site/shift/assignment/holiday, snapshot consumers, employee/day guard, idempotent check-in.
- B: real login/profile integration, stable key persisted before sending, check-in UI with clear pending state.
- Deliverables: active schedule visible to attendance, source revisions, 201 session response, outbox fact, minimal Nuxt-or-approved web setup form.
- Tests: missing projection, duplicate key/body, changed body/same key, parallel same-employee check-in, effective dates and tenant constraints.
- Acceptance/exit: phone/mocked mobile checks in once with one evidence/session/outbox effect; API can query today after timeout retry.
- Risks: reference ordering and stale assignment data; fail explicitly rather than guessing shift.

### Week 4 — Checkout and visible end-to-end result

- Owner: A attendance and minimal notification/report projection; B checkout/offline queue.
- Dependencies: week 3; working event foundation.
- A: implement checkout, day calculations and flags, minimal inbox plus daily projection, narrow dashboard view.
- B: local durable queue, network-loss recovery, checkout If-Match handling, current state screen, local biometric unlock if feasible.
- Deliverables: full first vertical slice, freshness-labeled daily state, deterministic shift/date fixtures.
- Tests: checkout before check-in; stale version; duplicate checkout; offline retry after commit; two closed sessions and one-open constraint.
- Acceptance/exit: check-in/out appears on phone and authorized web view; restart does not lose commands; no model/provider call on hot path.
- Risks: expanding all notification channels early; implement durable inbox before providers.

### Week 5 — MVP-1 and deployment rehearsal

- Owner: A CI/deploy then notification provider; B device registration/push validation.
- Dependencies: week 4; O04 hosting budget decision; provider development credentials.
- A: establish PR checks/image build, first staging/container deployment, secret handling, inbox-to-push worker and backlog metrics.
- B: register/deactivate push tokens; receive generic notification; validate permission refusal and foreground/background behavior.
- Deliverables: reproducible staged attendance demo, operational signal dashboard/log queries, kill-consumer evidence, provider acceptance state.
- Tests: stop notification/check in/restart; one logical notification; provider timeout/invalid token; deploy smoke and basic rollback.
- Acceptance/exit: MVP-1 proves durable attendance independently of consumer/provider availability; B uses staged API rather than only local mocks.
- Risks: deployment/provider account delays; preserve inbox and local test environment while escalating dependency, do not claim push delivery without device evidence.

### Week 6 — Leave submission and balance safety

- Owner: A leave domain; B leave forms/history.
- Dependencies: org projections, outbox/inbox; O02 leave/calendar conflict policy reviewed.
- A: type/policy setup, day expansion, balance/ledger, pending reservation, self-service leave endpoints.
- B: type/balance loading, date/half-day validation, stable command keys, own request history.
- Deliverables: authoritative balances, policy snapshot and expanded days, private attachment path where required.
- Tests: simultaneous requests, insufficient balance, AM/PM/FULL overlap, holidays excluded, year boundary locks, duplicate submission.
- Acceptance/exit: two parallel requests cannot overspend; available balance and ledger agree; mobile shows authoritative response/failure.
- Risks: complex accrual; start with configured upfront entitlement, retain monthly/year-end as gated admin capabilities.

### Week 7 — Leave approval and conflict recovery

- Owner: A saga/manager web; B approval and cancellation screens.
- Dependencies: week 6; approved O02 conflict semantics, assigned approver scope.
- A: approve/reject/cancel, attendance leave overlays, conflict detection, NEEDS_REVIEW handling and resolution; balance report event.
- B: manager actions with If-Match, stale-state reload, cancellation and visible syncState.
- Deliverables: leave end-to-end, revised day status, conflict inbox, compensating ledger path and reconciliation procedure.
- Tests: self-approval denied, duplicate approval once, cancel-before-delayed-approval, conflict-after-cancel ignored, stopped attendance then replay.
- Acceptance/exit: approved leave reaches attendance eventually; cancellation reverses exactly once; unresolved conflicts never silently overwrite manual work.
- Risks: saga complexity; keep one approver and full-day baseline before extra policy variants.

### Week 8 — Requests and MVP-2

- Owner: A request service and narrow HR catalogue/approval view; B request forms/inbox.
- Dependencies: org/roles, typed form contracts, attachment quarantine path.
- A: draft/submit, advisory material lines, snapshotted one-or-two-step approvals, rejection/cancel/fulfill and workflow events.
- B: dynamic bounded form, draft recovery, participant comments, assigned approval screens.
- Deliverables: materials/general requests, actor audit, validated uploads/downloads, final decision notifications.
- Tests: concurrent step decisions, unauthorized approver, self approval, draft-only edit, cross-tenant attachment, duplicate fulfillment.
- Acceptance/exit: MVP-2 demonstrates attendance, leave, and material request with secure approval and asynchronous visibility.
- Risks: inventory creep; stock remains advisory; no purchasing/warehouse subsystem.

### Week 9 — Corrections, audit, and reports

- Owner: A corrections/reporting then export web; B correction flows.
- Dependencies: stable event revisions; decision windows O03.
- A: correction/action ledger, manual replacement revision, authorized summary projections, CSV jobs, projection freshness/rebuild.
- B: correction submit/review result, original-versus-effective history, report summary if needed.
- Deliverables: auditable timesheet correction, tenant-scoped dashboard/calendar/CSV, job polling and expiring download.
- Tests: raw evidence unchanged, double approval race, out-of-order daily snapshot, summary subtract/add, formula-safe CSV, revoked role before download, shadow rebuild counts.
- Acceptance/exit: corrected day produces a higher revision and one accurate export; projection rebuild creates no real notifications.
- Risks: reporting UI expansion; deliver specified CSV before XLSX/charts.

### Week 10 — Cutoff, operations, and should-have gate

- Owner: A cutoff/security/operations; B reminder/device validation.
- Dependencies: all core flows and stable staging; O05 privacy/retention reviewed before evidence collection.
- A: durable absent cutoff, missing-checkout handling, restore/rollback rehearsal, runbooks; add rule inference only if core gates pass.
- B: permission-aware nudges, battery/background tests, inferred confirmation if enabled; EN/KH if capacity remains.
- Deliverables: restart-safe cutoff, reviewed operational controls, optional rules path with explicit pending state.
- Tests: holiday/leave/stale-projection cutoff, repeated job, late evidence after finalization, consent denied, no continuous GPS, backup restore.
- Acceptance/exit: absence is not inferred from missing projections; restored system passes tenant/ledger/outbox checks; optional inference can be disabled without breaking attendance.
- Risks: overcommitting geofence/ML or Kubernetes; cut those before resilience/security work.

### Week 11 — Hardening and conditional pilot

- Owner: A load/security/recovery, B device/offline acceptance, product/HR policy sign-off.
- Dependencies: week 10 release controls; approved real-data policy and operator.
- A: execute 100-concurrent scenario, p95/p99/error/propagation measurement, authorization review, migration upgrade rehearsal.
- B: representative Android/iOS device tests, token expiry/rotation, offline/conflicting commands, accessibility/localization checks.
- Deliverables: test evidence, known limitations, release candidate, pilot feedback for 5–10 consenting users if launch gate passes.
- Tests: two-tenant adversarial matrix, kill relay/consumer, broker outage, export isolation, smoke/rollback and restore.
- Acceptance/exit: no unresolved critical isolation/durability defect; measured targets either pass or have explicit scoped remediation; pilot is blocked if real-data controls are unapproved.
- Risks: pilot feedback consumes feature time; prioritize defects and keep feature freeze.

### Week 12 — Freeze, release decision, and demonstration

- Owner: A+B; product/operator approve release evidence.
- Dependencies: week 11; no new feature work unless required to fix a blocking defect.
- A: repair verified blockers, finalize deployment/runbooks/contract traceability and reproducibility; record immutable release artifacts.
- B: final device smoke, user guide/demo recording, confirm queued-command recovery on release candidate.
- Deliverables: demonstrable build, release notes, measured test report, open-risk register, operational handover and demo video.
- Tests: exact release-image smoke, end-to-end attendance/leave/request/correction/export, consumer restart, authorized access and rollback verification.
- Acceptance/exit: all MVP definition-of-done evidence linked, demo reproducible, limitations disclosed; production launch can remain blocked even when synthetic demo is complete.
- Risks: last-minute framework/cloud/schema changes; defer to next increment rather than destabilize release.

## 6. Integration milestones

| Milestone | Owner | Dependencies | Deliverable / verifiable exit | Required tests |
|---|---|---|---|---|
| M0 end W1 | A+B | Toolchain, O01/O06/O07 | Mockable auth/check-in/today and clean setup | Compile, schema/examples, dependency health |
| M1 end W2 | A | M0, O08 | Tenant-auth and durable event foundation | JWT/header/tenant, relay crash/inbox |
| M2 end W4 | A+B | M1 + org | First phone-to-dashboard slice | Keys, checkout, offline retry, projection |
| MVP-1 end W5 | A+B | M2 + staging | Durable attendance and notification recovery | Kill consumer, provider failure, deploy smoke |
| M3 end W7 | A+B | Leave balances + O02 | Approval/cancel/conflict end to end | Reservation race, stale conflict, compensation |
| MVP-2 end W8 | A+B | M3 + request chain | Material/general requests end to end | Approval race, attachment isolation |
| RC end W10 | A+B | Audit/reports/ops | Core complete, cutoff and recoverability | Rebuild/export, restore, cutoff |
| Release end W12 | A+B+product/operator | Security/policy gates | Reproducible release or explicitly limited demo | Final E2E/load/device/rollback evidence |

A missed milestone is recorded with blocker, owner, revised dependency, and scope impact; later milestone labels are not claimed complete automatically.

## 7. Quality gates and definition of done

A feature is done only when:

1. Contract, authorization, tenant scope, validation, error and retry semantics are reviewed.
1. Owner migration/constraints/indexes and current-state/append-only behavior are tested on PostgreSQL.
1. Mutation, audit, idempotency, and outbox share one transaction where required.
1. Consumer transaction/inbox and offset behavior are tested for duplicates and out-of-order data.
1. The client handles loading/empty/error/stale/offline states without duplicating commands.
1. Acceptance tests cover the requirement ID and negative role/tenant cases.
1. Logging/metrics reveal failure without exposing secrets or unnecessary personal data.
1. Documentation and machine contracts reflect actual implementation status.
1. Clean build and release smoke pass; remaining limitations are disclosed.

No fixed test coverage percentage is required or claimed. Test depth follows failure risk, not line count. Business unit tests do not replace PostgreSQL/Kafka integration tests for durability and locking.

## 8. Release readiness

### Synthetic demonstration gate

- [ ] Two synthetic tenants and employees can be provisioned reproducibly.
- [ ] Login/check-in/out, leave, request, correction, and CSV flows work on release candidate.
- [ ] Duplicate keys and consumer restart produce one business effect.
- [ ] Freshness and asynchronous job status are visible.
- [ ] Exact toolchain/configuration and startup order are documented.
- [ ] No production credentials or real evidence are needed for demo.

### Real-user launch gate

- [ ] O02/O03/O05/O08 policies approved or dependent feature disabled explicitly.
- [ ] TLS, key rotation, refresh revocation window, runtime/migration grants reviewed.
- [ ] Resource-level and cross-tenant tests pass for APIs/SSE/files/exports/jobs.
- [ ] Attachment privacy/quarantine, evidence minimization, retention/erasure process verified.
- [ ] Deployment target, cost forecast, operator/backup, monitoring and incident contacts agreed.
- [ ] Backup restore and compatible rollback demonstrated; RPO/RTO agreed from evidence.
- [ ] No open critical security/data-loss defects; known lower risks accepted with owner/date.
- [ ] Real-device permissions/offline/push behavior and employee consent verified.

These boxes are acceptance criteria, not a claim that any check has already passed.

## 9. Risk register and assumptions

| Risk | Likelihood / impact | Mitigation | Owner / trigger |
|---|---|---|---|
| A overloaded across seven services/web/infra | High / high | Narrow slice, sequential work, merge projections if needed | A; missed weekly gate |
| B blocked by unstable APIs | High / high | W1 fixtures/mocks and contract review before changes | A+B; payload drift |
| Tenant leakage | Medium / critical | Scoped query/constraint/event/file test matrix; block launch | A/security; any isolation failure |
| Event duplicate/order defect | High / high | Outbox/inbox plus aggregate revisions, crash/replay tests | A; repeated/wrong effects |
| Attendance precedence ambiguity | High / high | O02 decision and visible review state | Product/HR; before W7 |
| Policy/timezone complexity | Medium / high | Same-day MVP, explicit snapshots, reject unsupported cases | A/HR; W2 |
| Cloud budget/cluster overhead | High / high | Cost forecast, simple pilot target, no EKS mandate | A/operator; W5 |
| Device/background delivery limits | High / medium | Explicit check-in fallback; real-device evidence | B; W5/W10 |
| Synthetic ML confidence mistaken for accuracy | Medium / high | Defer ML; separate real labeled test set | A/product; any auto-inference proposal |
| Provider/account unavailable | Medium / medium | Durable inbox and clear pending state; resolve account dependency | A+B; W5 |
| Registry/build/config scaffold defects | High / high | W1 repair and full compile/startup gates | A; M0 |
| Privacy/retention approval absent | Medium / critical | Synthetic data only until approved | Tenant/security; before pilot |
| Scope creep to inventory/vision/payroll | Medium / high | Explicit change gate and cut line | Product; new request |

Assumptions: contributors are available for the full twelve-week effort; HR/product can decide policies promptly; test devices and provider accounts are available; repository contains all current backend/web source. Flutter repository, production domain, hosting budget, legal policy, and availability commitment are unknown until confirmed.

## 10. Scope changes and contingency

Any change records business value, affected requirement/contract, effort, risk, owner, and displaced work. A+B review API/event implications; product/HR approves business-policy changes; operator approves hosting changes. Do not add scope by silently redefining “enterprise-grade” to require more infrastructure.

Cut first: ML, vision, NFC, dark mode, distributed tracing, Kafka Streams, Redis, DLQ UI, advanced inference, XLSX, custom charts, Kubernetes/EKS. Next simplify: keep one push provider, narrow approval chains, merge reporting/notification projections, seed low-frequency admin data through controlled tools. EN/KH and reminders remain should-have and can be deferred with explicit product notice. Never cut: authentication/tenant isolation, resource authorization, durable attendance, leave and general requests, outbox/inbox, audit/corrections, tested deployment/recovery. Do not remove policy checks or downgrade deduplication to meet the latency target. If core scope exceeds twelve weeks, present a demonstrable synthetic MVP and a revised production backlog rather than claim launch readiness.

## 11. Demonstration sequence

1. Provision company, employee, assignment, and holiday; show two-tenant isolation.
1. Log in on mobile; check in and show committed response plus dashboard freshness.
1. Repeat the same command/key; show unchanged session/event count.
1. Stop notification consumer, check in with another employee, restart; show one recovered notification.
1. Approve leave; show eventual calendar update, then conflict/cancel recovery with ledger evidence.
1. Submit/approve/fulfill material request; show assigned-approver authorization.
1. Correct a time record; show original evidence and new effective revision.
1. Export authorized CSV; show job status and expired/revoked download behavior.
1. If enabled, demonstrate consent-based nudge/inferred confirmation with uncertainty disclosed.
1. Show measured 100-client results, reproducible deployment, CI evidence, rollback/restore and open limitations.

Kubernetes pods/Terraform are shown only if actually implemented; they are not required props for a successful product demo.

## 12. Requirements-to-test traceability

| Requirement | Delivery gate | Test artifact required |
|---|---|---|
| R01 auth/tenant | W2 then every feature | JWT/refresh/header and tenant/object matrix |
| R02 org/calendar | W3 | Assignment/holiday/source-version fixtures |
| R03 check-in/out | W4 | Atomic transaction, duplicate/parallel/offline cases |
| R04 event recovery | W2/W5/W11 | Relay crash, inbox/order/kill-consumer evidence |
| R05 leave | W6–W7 | Balance lock, reservation, overlap, saga/cancel tests |
| R06 requests | W8 | Chain, self-approval, file access, fulfillment races |
| R07 notification/live | W5/W9 | Logical dedupe, provider acceptance, SSE reconnect/freshness |
| R08 audit/correction | W9 | Immutable evidence/actions, approved replacement revision |
| R09 reporting | W9 | Revision guard, rebuild counts, authorized CSV/job expiry |
| R10 deployment/ops | W1/W5/W10/W12 | Fresh setup, image smoke, rollback/restore report |
| R11 evidence/inference | Conditional W10–W11 | Consent/device/battery and independent labeled evaluation |
| R12 performance | W11 | Hardware/dataset/config, p95/p99/error/propagation samples |

Current passing checks are documented in the validation record, not treated as satisfaction of these future feature gates.
