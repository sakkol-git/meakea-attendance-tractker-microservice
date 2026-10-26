# Frontend-to-backend synchronization plan

Date: 25 October 2026. Status: planning baseline; no backend capability or production acceptance is claimed.

## Purpose and authority

This plan turns the implemented mobile and web workflows into an ordered backend delivery backlog. The clients supply interaction evidence, terminology and required outcomes. They do not own HR policy, authorization, accounting, stock, time-zone rules or durable workflow state. When a client conflicts with an accepted business or architecture document, resolve the conflict before adopting the behavior server-side.

Use [source of truth](../SOURCE-OF-TRUTH.md) for authority, [business flows](../business-flow/API-MAP.md) for workflow traceability, `contracts/openapi.json` for reviewed wire definitions, and the weekly guides for execution. This document is the synchronization overlay for those sources; it does not replace their gates or schedules.

## Inspected baseline

| Surface | Observed state | Planning consequence |
| --- | --- | --- |
| Mobile | Broad employee, manager, HR and owner workflows; canonical `/auth/profile` name/time-zone update and password-change UI; 73 feature-inventory routes; remote business adapters remain provisional | Treat behavior and validation as consumer requirements, then verify each rule against business ownership |
| Web | Forty workflow screens, aligned approval/delegation/material/coverage flows, canonical profile/password UI, admin audit screen, explicit demo and remote modes | Preserve desktop interaction differences while requiring the same server outcomes; audit remains synthetic until its pending provider contract is accepted |
| Shared OpenAPI | Broad synchronized client inventory; business operations remain marked `x-hrflow-implementation: pending` | Classify every operation before calling the provider accepted or complete |
| Java services | Seven services; gateway forwards /api/v1/auth and JWKS; identity-org auth slice implemented and verified; business domain controllers pending | First vertical slice active; proceed with organization catalogs, attendance and leave domain implementations |
| Business-flow inventory | Historical 112-route candidate map plus implemented Python confidence endpoints; Java business routes absent | Use workflow IDs for traceability, but re-approve historical candidates instead of copying them blindly |

The clients remain ahead of the business domain providers. The existing gateway/authentication evidence predates the complete ADR-010/011 acceptance matrix and does not establish every required service-side JWT gate. Next provider work verifies or configures Spring Security OAuth2 Resource Server independently in each Java service, then implements organization, attendance and leave behavior with tenancy, transactions and concurrency tests. Do not introduce custom JWT authentication filters.

## Launch assumptions to carry into decisions

- Password authentication, English-only launch UI and server-side sensitive drafts.
- Android API 24 and iOS 15 remain the minimum mobile targets.
- Delegation is included. Bulk approval and escalation are excluded unless later approved.
- Use `Asia/Phnom_Penh` as the launch company zone until per-company IANA-zone behavior is accepted. Store instants in UTC and calculate company dates with the authoritative company zone.
- Use 25 employees and 25 requests per month as the provisional small-tenant test floor. The earlier single value `25` did not distinguish the two measures, so confirm the final load profile before the performance gate.
- A real brand mark, physical devices, a reachable gateway and representative users remain external inputs.

## Decisions required before business implementation

| Decision | Current disagreement or gap | Required result |
| --- | --- | --- |
| Contract status | OpenAPI contents cover 101 paths while its description and supporting guides describe a leave-only slice | Label each operation `accepted`, `candidate` or `deferred`; correct metadata only after review |
| Browser/mobile authentication | Implemented and verified in gateway and identity-org | Dual transport active: Web uses HttpOnly refresh cookie and CSRF header; mobile uses native JSON refresh. Rotating token family, reuse detection and attempt throttling are verified |
| Approval queues | Shared remote contract uses `/requests/inbox`; demo adapters may retain internal compatibility routes | Provider must implement the canonical inbox with the accepted filters, sorting and totals |
| Half-day vocabulary | Both clients now send `portion` as `FULL_DAY/MORNING/AFTERNOON` | Provider must apply the same enum and revalidate policy at submit time |
| Delegation ownership | Clients demonstrate cross-workflow delegation; no accepted provider contract or owning service exists | Decide whether identity, request, or a shared approval capability owns eligibility, overlap and cancellation |
| Time zones | Demo behavior uses fixed UTC+07; future architecture allows company zones | Define storage, company-date calculation, DST policy, reporting boundaries and response fields |
| Onboarding | Setup, invitations, expiry, resend, first login and recovery are not one accepted flow | Approve state machine, security limits, delivery evidence and administrator recovery |
| Sensitive drafts | Mobile requires server-side drafts; web has no accepted durable draft contract | Define scope, schema version, no-store behavior, retention and logout/account-erasure semantics |
| Attachments and exports | Clients model signed URLs and asynchronous exports | Approve storage ownership, authorization, malware/checksum checks, expiry, cleanup and audit fields |
| Collection behavior | Clients require filtering before pagination, stable sorting and filtered totals | Standardize zero-based pages, sizes, grouped statuses, tie-breakers and maximum result limits |

Do not start dependent controllers while an unresolved decision can change identity, tenant scope, transaction boundaries or persisted data.

## Feature-to-service backlog

| Priority | Business outcome required by clients | Proposed owner | Backend work and acceptance boundary |
| --- | --- | --- | --- |
| 0 | One trusted API surface | Gateway plus all owners | Implemented: reactive Spring Cloud Gateway, JWT decoding, RFC 7807 problem details, and route forwarding |
| 1 | Login, refresh, logout and current profile | Identity/organization plus gateway | Implemented: password throttling, rotating refresh family, reuse detection, browser CSRF cookies, native tokens and profile |
| 2 | Company setup, invitations and organization catalogs | Identity/organization | Tenant creation, employee lifecycle, departments, positions, sites, shifts and holidays with relationship authorization and effective-date constraints |
| 3 | Clock, history, evidence, correction and manager review | Attendance | Atomic session rules, authoritative original correction snapshot, company-date calculation, version conflicts and allowed reviewer relationships |
| 4 | Leave preview, request, balances, cancellation and approval | Leave | Policy preview revalidated at submit, exact balance reservation, overlap checks, calendar coverage projection, evidence and conflict resolution |
| 5 | Draft request, approval chain and cancellation | Request | Persist immutable approval-chain snapshot, validate transitions, require decision reasons where defined, expose stable inbox queries |
| 6 | Partial material fulfillment | Request | Atomic stock ledger, requested/issued/remaining quantities, no over-issue, version protection and auditable fulfillment history |
| 7 | Delegation | Owner pending Decision 5 | Eligible reviewer relationships, bounded dates, no self-delegation, overlap protection, audit and consistent application to each approval domain |
| 8 | Notifications and preferences | Notification | Outbox-driven creation, inbox reads, read state, preferences, safe references, device lifecycle and no personal data in push payloads |
| 9 | Reports, audit and CSV exports | Reporting plus event owners | Rebuildable tenant projections, source drill-down, protected audit query, formula-safe CSV, requester-scoped jobs and expiring downloads |
| 10 | Sensitive drafts and account cleanup | Owner selected per data boundary | Tenant/employee scoping, encryption/retention, no shared cache, schema migration and verified deletion |

`Proposed owner` is a planning assignment. Confirm it against ADR 008 and transaction boundaries before adding it to the accepted contract.

## Twelve-week synchronization overlay

This keeps the existing roadmap order. It adds the frontend result that each backend week must unlock.

| Existing week | Provider result | Client synchronization evidence |
| --- | --- | --- |
| 01 Foundation | Contract classification, common errors, ETag/idempotency rules and one protected gateway-to-service slice | Gateway baseline starts, verified problem details match client format |
| 02 Identity and host | Implemented platform-aware authentication and tenant context | Web cookie/CSRF and mobile secure-token journeys pass; both clients use the implemented profile/password routes; wrong tenant, reuse, throttling and logout negatives pass in test suite |
| 03 Organization/admin | Tenant, employees, roles, sites, shifts, holidays and onboarding foundation | Both clients use provider catalogs; invitation and employee-impact gaps remain visibly blocked until accepted |
| 04 Attendance | Check-in/out, history, correction and review with company-time rules | Employee and manager journeys pass against the same provider records and stale ETags |
| 05 Events/notifications | Transactional outbox, replay-safe consumer and notification inbox | A committed domain action produces one notification; replay does not duplicate the effect |
| 06 Leave | Preview, request, balances, approval, cancellation and coverage reads | Mobile and web show the same charge, balance, status and decision after refresh |
| 07 Requests/approvals | Drafts, approval chain, material stock transaction, delegation and private files | Shared outcomes pass for requester, reviewer and fulfiller; unauthorized and over-issue cases fail safely |
| 08 Client integration | Accepted adapters replace provisional routes feature by feature | Remote mode has no demo fallback; loading, empty, error, conflict, timeout and success states are exercised |
| 09 Reporting/audit | Rebuildable dashboards, filtered audit and export lifecycle | Web and mobile expose role-gated filtered audit views, currently synthetic; provider values must reconcile to source records and jobs/downloads remain tenant/requester scoped |
| 10 Operations | Deploy, rollback, backup and clean restore | Restored environment serves the same accepted client smoke flows |
| 11 Hardening/pilot | Authorization matrix, accessibility, device and measured-load evidence | Two-tenant adversarial suite, API 24/iOS 15 checks and approved load profile pass |
| 12 Release/demo | Frozen compatible contract and repeatable release | Signed clients complete the demonstration with a live gateway; independent audit findings are resolved or accepted |

Weekly gates remain `NOT RUN` until their own evidence files and commands pass. Existing client tests can seed provider scenarios, but cannot satisfy a provider gate by themselves.

## Contract increment order

For each increment, complete these steps in order:

1. Select one visible business outcome and its workflow IDs.
1. Compare mobile calls, web calls, business rules and the candidate OpenAPI operation.
1. Resolve route, enum, field, authorization, time-zone and transaction differences.
1. Record request, success, problem, pagination, ETag and idempotency examples.
1. Mark only the reviewed operations accepted; keep later operations candidate or deferred.
1. Add provider contract tests and client fixture tests from the same examples.
1. Implement the owning service and gateway route in a later coding task.
1. Run the outcome against two tenants and all applicable roles.
1. Update the alignment matrix with provider evidence and remaining platform differences.

Avoid accepting all 101 paths at once. A smaller reviewed slice with a working provider is safer and easier to evolve than a large provisional specification treated as finished.

## Required behavior examples

### Tenant authorization

Given a manager from tenant A and a request from tenant B, when the manager reads or decides that request, then the API returns a safe denial without revealing whether the record exists, and no audit or workflow state is attributed to tenant A.

### Unknown command outcome

Given a client sends a command with an idempotency key and the response times out after commit, when the same principal retries the same payload with the same key, then the server returns the original result; a different payload with that key fails.

### Leave calculation

Given the accepted company zone, work schedule, holidays and current balance, when the employee previews and submits leave, then the server calculates the same charge at both steps or returns a versioned policy conflict for review.

### Material fulfillment

Given ten units requested, four already issued and five in stock, when a fulfiller attempts to issue seven, then the command fails without changing stock or history; issuing five commits one stock movement and leaves one unit outstanding.

### Delegation

Given an active delegation covering the request date, when an eligible delegated reviewer acts, then the decision records the acting and delegating identities; self-delegation, overlapping periods and unauthorized domains fail.

### Company time

Given a UTC instant near midnight and the company zone `Asia/Phnom_Penh`, when attendance, leave and reporting derive a business date, then all services and both clients receive the same date and display an explicit zone where ambiguity matters.

## Definition of aligned for one feature

A feature is `aligned` only when:

- its business state machine and permissions are approved;
- its OpenAPI operations and examples are accepted and compatibility checked;
- the gateway and owning service implement the contract without a demo fallback;
- tenant, relationship, invalid-input, duplicate, stale-version, timeout and failure cases pass where applicable;
- mobile and web provider tests reach the same business outcome;
- intentional platform differences are recorded;
- logs, audit, metrics and data retention are verified without exposing sensitive data;
- deployment and restore evidence includes the feature.

Use `candidate` when the clients agree but provider policy or ownership is unresolved. Use `blocked` when an external system, device, representative user, brand decision or gateway is unavailable.

## Planning deliverables before coding resumes

1. Review the 101 OpenAPI paths and publish an accepted/candidate/deferred register with service owner and workflow IDs.
1. Resolve the ten decisions above, starting with authentication, tenant propagation, request inbox naming, half-day values and delegation ownership.
1. Select the Week 1 vertical slice. Recommended scope: login/current profile plus one read-only tenant-scoped employee collection before any approval command.
1. Create provider acceptance scenarios from the BDD examples and name the evidence location; do not mark them passed.
1. Re-estimate weekly capacity after the accepted operation count is known. Preserve security, tenant isolation, transaction correctness and restore gates when cutting scope.

## External evidence still required

- Approved brand mark for final visual acceptance.
- Physical Android and iOS devices for native accessibility, secure storage, notifications and performance.
- A reachable gateway and dependent infrastructure for live integration.
- Representative employees, managers and HR users for usability evidence.
- Independent security and release audit for P5-B.

Until those inputs and the provider gates pass, describe the product as a tested client demonstration with a planned backend contract, not a production-ready integrated system.
