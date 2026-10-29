# Week 06 local candidate evidence

Date: 2026-10-27. Owner week: 06 candidate; verified roadmap position: W02-S26. This is an early local candidate for the 2026-11-30 to 2026-12-04 week, not a Week 06 gate result. Root, web and mobile changes are uncommitted in separate repositories. The intended Acer Nitro 5 and encrypted external backup disk are unavailable for the required deploy and restore gate.

## Accepted intent and current behavior

- ADR 010 and ADR 011 require token validation at the gateway and each Spring service via Spring Security OAuth2 Resource Server. `services/leave` decodes RS256/JWKS tokens and protects all `/leave/**` endpoints. Gateway routing for `/api/v1/leave/**` strips prefix 2 and sanitizes caller headers, verified in `SecurityConfigurationTests` and `ApiGatewayApplicationTest`.
- Leave core migration V1 (`V1__leave_core.sql`) adds PostgreSQL 18 tables: `leave.leave_type`, `leave.leave_policy`, `leave.leave_balance`, `leave.leave_ledger`, `leave.leave_request`, `leave.leave_day`, `leave.leave_outbox`, `leave.leave_inbox`, and `leave.command_receipt`. Row Level Security (`FORCE ROW LEVEL SECURITY`) is enabled on all tables, isolating tenant data via PostgreSQL session variable `app.tenant_id`. Check constraints enforce positive requested days, non-negative balances, valid date ranges, and allowed portions. Unique constraint `(tenant_id, employee_id, date)` on `leave_day` strictly prevents overlapping leave for any employee.
- MapStruct mapping policy (ADR 014) is implemented per explicit user decision: `LeaveTypeMapper`, `LeaveBalanceMapper`, and `LeaveRequestMapper` use `@Mapper(componentModel = "spring")`. `LeaveBalanceResponse` provides fields `id`, `name`, `type`, `allocated`, `entitled`, `used`, `pending`, `available`, `unit`, and `year`, seamlessly satisfying Web, Mobile, and OpenAPI schemas.
- Server-side Authoritative Calculation (`LeaveCalculationService`): working day calculation skips weekends and public holidays deterministically; half-day portions (`MORNING`, `AFTERNOON`) are strictly validated and permitted only on a single calendar date; short notice and insufficient balance warnings are emitted authoritatively without trusting client-sent day totals.
- Deterministic Locking and Atomic Reservation (`LeaveService`): uses pessimistic `SELECT ... FOR UPDATE` locking on `leave_balance` in a deterministic order; moves available to pending; inserts request, expanded days, reservation ledger entry, and transactional outbox event `org.leave.request.created.v1`. Concurrent parallel requests attempting to overspend fail atomically with complete rollback, preserving exact ledger and balance invariants.
- Approval and Rejection Lifecycle: requires approver role (`ROLE_MANAGER`, `ROLE_HR_ADMIN`, `ROLE_TENANT_ADMIN`); prevents requester self-approval (403); enforces `If-Match` concurrency checking (412 on stale version); rejects duplicate decisions (409); moves pending balance to used on approval with `USED` ledger entry, or releases pending balance back to available on rejection with `RELEASE` ledger entry and deleted day slots.
- Cancellation and Compensating Ledger: employees can cancel their own pending or approved leave requests with `If-Match` concurrency; pending cancellation releases pending balance; approved cancellation creates compensating ledger entry (`COMPENSATION`) and restores available balance while preserving the entire original historical ledger trail.
- Complete Route Tenant Matrix (`LeaveTenantMatrixIT`): all leave routes enforce authentication (401), reject missing tenant claim (403), enforce role restrictions on pending queues and approval actions, and prove complete cross-tenant boundary isolation.

## Stone 1 verification (W06 Stone 1, W06-S01–S03)

- Steps: `W06-S01` to `W06-S03` (create type policy balance and ledger: `V1__leave_core.sql`, check constraints on balance and portions, RLS isolation).
- Targets: `services/leave/src/main/resources/db/migration/V1__leave_core.sql`, `services/leave/src/test/java/com/hrflow/leave/CreateTypePolicyBalanceLedgerTest.java`.
- Verified behaviors: negative available balance rejected by DB check constraint; inverted date ranges rejected; invalid half-day portions rejected; cross-tenant query isolation verified under non-superuser role.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/leave -Dtest="CreateTypePolicyBalanceLedgerTest"` exited 0 (2 tests, 0 failures, 0 errors, 4.819s) against Testcontainers PostgreSQL 18.6.

## Stone 2 verification (W06 Stone 2, W06-S04–S06)

- Steps: `W06-S04` to `W06-S06` (calculate eligible leave days: deterministic weekend/holiday skipping, portion rules, notice period).
- Targets: `services/leave/src/main/java/com/hrflow/leave/service/LeaveCalculationService.java`, `services/leave/src/test/java/com/hrflow/leave/CalculateEligibleLeaveDaysTest.java`.
- Verified behaviors: working days calculation skips weekends and holidays; half-day allowed only on single calendar date; inverted dates return error; insufficient balance and short notice warnings emitted authoritatively.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/leave -Dtest="CalculateEligibleLeaveDaysTest"` exited 0 (5 tests, 0 failures, 0 errors, 0.040s).

## Stone 3 verification (W06 Stone 3, W06-S07–S12)

- Steps: `W06-S07` to `W06-S12` (reserve under deterministic locks: pessimistic balance locking, atomic reservation, overspend race protection).
- Targets: `services/leave/src/main/java/com/hrflow/leave/service/LeaveService.java`, `services/leave/src/test/java/com/hrflow/leave/ReserveUnderDeterministicLocksTest.java`.
- Verified behaviors: parallel threads attempting to overspend a 10-day balance with two 6-day requests result in exactly one success and one atomic rollback; ledger entries and balance counters stay exact.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/leave -Dtest="ReserveUnderDeterministicLocksTest"` exited 0 (1 test, 0 failures, 0 errors, 4.954s) against Testcontainers PostgreSQL 18.6.

## Stone 4 verification (W06 Stone 4, W06-S13–S18)

- Steps: `W06-S13` to `W06-S18` (approve or reject once: approver role check, requester self-approval denial, concurrency control, duplicate decision rejection).
- Targets: `services/leave/src/main/java/com/hrflow/leave/controller/LeaveController.java`, `services/leave/src/test/java/com/hrflow/leave/ApproveOrRejectOnceTest.java`.
- Verified behaviors: requester self-approval denied with 403; non-approver role denied with 403; stale `If-Match` returns 412; approval moves pending to used with `USED` ledger entry; duplicate decision returns 409; rejection releases pending balance.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/leave -Dtest="ApproveOrRejectOnceTest"` exited 0 (4 tests, 0 failures, 0 errors, 4.847s) against Testcontainers PostgreSQL 18.6.

## Stone 5 verification (W06 Stone 5, W06-S19–S24)

- Steps: `W06-S19` to `W06-S24` (cancel compensating ledger: pending cancellation, approved cancellation with compensating ledger, ownership enforcement).
- Targets: `services/leave/src/main/java/com/hrflow/leave/service/LeaveService.java`, `services/leave/src/test/java/com/hrflow/leave/CancelCompensatingLedgerTest.java`.
- Verified behaviors: pending cancellation releases pending balance; approved cancellation writes compensating ledger (`COMPENSATION`) and restores available balance while preserving complete original audit history; non-owner cancellation denied with 403.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/leave -Dtest="CancelCompensatingLedgerTest"` exited 0 (3 tests, 0 failures, 0 errors, 4.694s) against Testcontainers PostgreSQL 18.6.

## Stone 6 verification (W06 Stone 6, W06-S25–S30)

- Steps: `W06-S25` to `W06-S30` (route tenant matrix and full leave integration: 10 endpoints verified for 401 unauthenticated, 403 missing tenant, 403 insufficient role, cross-tenant isolation, clean regression).
- Targets: `services/leave/src/test/java/com/hrflow/leave/LeaveTenantMatrixIT.java`, `services/leave/src/test/java/com/hrflow/leave/LeaveIntegrationTest.java`, `services/leave/src/test/java/com/hrflow/leave/LeaveContextTest.java`.
- Verified behaviors: complete leave lifecycle from preview through submit, idempotency replay, approve, cancel, and balance restoration; all 10 endpoints enforce security, role, and tenant boundaries.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/leave -Dtest="LeaveTenantMatrixIT,LeaveIntegrationTest,LeaveContextTest"` exited 0 (6 tests, 0 failures, 0 errors, 8.749s).
- Module clean regression: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw --batch-mode -pl services/leave clean test` exited 0 (23 tests run, 0 failures, 0 errors, 0 skipped, 16.889s).

## Stone 7 verification (W06 Stone 7, W06-S31–S33)

- Steps: `W06-S31` (reproduce earliest failed check); `W06-S32` (recheck client and contract negatives: 401 unauthenticated, 403 missing tenant, 403 self-approval, 412 stale If-Match, 409 overspend and duplicate decision); `W06-S33` (review carry-forward and reserve).
- Targets: `contracts/openapi.json`, `docs/validation/W06-EVIDENCE.md`, root/web/mobile diffs.
- Verified behaviors: OpenAPI contract validation; contract compatibility against `origin/main`; git diff check clean across root, web, and mobile repositories; documentation lint passed.
- Diff check: `git diff --check && git -C web diff --check && git -C mobile diff --check` exited 0.
- Contract validation: `.venv-checks/bin/python scripts/contracts.py validate && compat --base origin/main && check` exited 0 (177 operations, 6 contract tests).
- Documentation lint: `python3 docs/roadmap/tools/docs-lint.py docs README.md AGENTS.md contracts/README.md tools/project_mcp/README.md` exited 0 (316 Markdown files, 0 errors, 0 warnings).

## Stone 8 assessment & physical blockers (W06 Stone 8, W06-S34–S36)

- Steps: `W06-S34` (redeploy checked service candidate via `ops/deploy.sh`); `W06-S35` (restore release backup in scratch via `ops/restore-scratch.sh`); `W06-S36` (decide gate).
- Physical blocker - deploy: `ops/deploy.sh` requires the dedicated Acer Nitro 5 private host, which remains physically unavailable in this development environment.
- Physical blocker - restore: `ops/restore-scratch.sh` requires the encrypted external backup disk, which remains physically disconnected.
- Gate decision: `BLOCKED`. The Week 06 release gate remains **NOT RUN**. Verified roadmap position remains `W02-S26` in `docs/roadmap/YOU-ARE-HERE.md`. No weekly gate is claimed passed.

## Executed checks

| Check | Result | Limit |
| --- | --- | --- |
| DB constraints, negative portions and RLS, `CreateTypePolicyBalanceLedgerTest` | Exit 0 (2 tests); negative available balance rejected by DB check constraint, inverted dates rejected, invalid portion rejected, cross-tenant isolation verified under non-superuser role | Disposable PostgreSQL 18; no live host deployment |
| Eligible leave day calculation and policy rules, `CalculateEligibleLeaveDaysTest` | Exit 0 (5 tests); weekend/holiday skipping, half-day single date constraint, inverted date rejection, insufficient balance and notice warnings verified | Plain unit test with fixed dates |
| Deterministic locks and concurrent reservation, `ReserveUnderDeterministicLocksTest` | Exit 0 (1 test); two parallel threads attempting to overspend 12 days on a 10-day balance result in exactly 1 success and 1 rollback; database balance and single ledger row remain exact | Concurrent multi-threaded test against PostgreSQL 18 |
| Approval, rejection, self-decision denial, and concurrency, `ApproveOrRejectOnceTest` | Exit 0 (4 tests); self-approval denied 403, non-approver denied 403, stale If-Match returns 412, approval moves pending to used, duplicate decision returns 409, rejection releases balance | Disposable PostgreSQL 18; synthetic JWT |
| Cancellation and compensating ledger, `CancelCompensatingLedgerTest` | Exit 0 (3 tests); pending cancellation releases pending balance, approved cancellation writes compensating ledger and preserves original history, non-owner cancellation denied 403 | Disposable PostgreSQL 18; synthetic JWT |
| Route tenant matrix, `LeaveTenantMatrixIT` | Exit 0 (4 tests); 10 routes tested for 401 unauthenticated, 403 missing tenant, 403 insufficient role, and complete cross-tenant isolation | Disposable PostgreSQL 18; synthetic JWT |
| Full leave integration, `LeaveIntegrationTest` | Exit 0 (1 test); complete leave workflow lifecycle tested from preview through submit with idempotency replay, approve, cancel, and balance restoration | Disposable PostgreSQL 18; synthetic JWT |
| Leave module verify, `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw -pl services/leave verify` | Exit 0; 23 tests (18 unit/lifecycle, 5 IT/matrix) passed in 22s | Local containers; Schema Registry mocked for event tests |
| Gateway routed security and routing, `./mvnw -pl services/api-gateway test` | Exit 0; 18 tests passed including routed security and prefix stripping for leave routes | In-process MockWebServer; no full Acer deployment |
| Foundation scope, `.venv-checks/bin/python scripts/verify-project.py --scope foundation --base origin/main` | Exit 0; 177-operation OpenAPI validation, 6 contract tests, docs lint (315 Markdown files, 0 errors, 0 warnings), diff check, and compatibility check passed | Automated local baseline only |
| Web scope, `.venv-checks/bin/python scripts/verify-project.py --scope web` | Exit 0; 89 unit tests, bundle check, 12 Playwright E2E scenarios, and diff check passed | Local Chromium browser; no live provider |
| Mobile scope, `.venv-checks/bin/python scripts/verify-project.py --scope mobile` | Exit 0; 155 tests passed (1 live test skipped by default), Flutter analyze clean, bundle check, and diff check passed | Local headless Flutter runtime; no physical device |
| Tooling scope, `.venv-checks/bin/python scripts/verify-project.py --scope tooling` | Exit 0; 23 tests passed across MCP server, test runner, and workspace traversal | Local Python virtual environment |

## Boundary review

- Backend domain logic: `LeaveService`, `LeaveCalculationService`, `CommandReceipts`, `LeaveOutboxWriter` implement business rules without speculative design patterns.
- Web: Nuxt client decodes `/leave/requests`, `/leave/requests/me`, `/leave/requests/pending`, and `/leave/balances/me` matching expected schema fields (`type`, `available`, `pending`, `unit`, `year`).
- Mobile: Flutter client decodes `/leave/balances/me`, `/leave/requests/preview`, `/leave/requests`, and `/leave/requests/pending` matching expected models (`LeaveBalance`, `LeavePreview`, `LeaveRecord`).
- HTTP contract: `contracts/openapi.json` owns the canonical specification. All implemented leave routes conform to schema definitions.
- Authentication & Authorization: Spring Security OAuth2 Resource Server validates JWTs. Standard endpoints require authenticated employee; approval and rejection endpoints require `ROLE_MANAGER`, `ROLE_HR_ADMIN`, or `ROLE_TENANT_ADMIN`. Requester self-approval is rejected on the server.
- Tenant scope: `TenantSqlScope` sets `app.tenant_id` session variable on each transaction; PostgreSQL RLS policies enforce isolation on all tables.
- Concurrency & Stale writes: `RevisionPreconditions.require(ifMatch, version)` prevents lost updates on approval, rejection, and cancellation.
- Idempotency: `CommandReceipts` stores SHA-256 request hash with `Idempotency-Key` header, returning stored response on replays and rejecting altered payloads with 409 Conflict.
- Events & Replay: Transactional outbox records `org.leave.request.created.v1`, `org.leave.request.approved.v1`, `org.leave.request.rejected.v1`, `org.leave.request.cancelled.v1`.
- Documentation: `docs-lint.py` passes 316 Markdown files with 0 errors and 0 warnings.

## Disposition

`CANDIDATE — LOCALLY VERIFIED`

Local implementation and tests across backend, web, mobile, foundation, and tooling are green. The Acer Nitro 5, encrypted external backup disk, and physical device gates remain blocked. No weekly gate is claimed passed.

## UAT repair stone 3a — typed leave intent (2026-10-28)

The approved leave lifecycle topic is `leave.request.v1`, keyed by tenant and employee. Its new full snapshot schema is in `libs/hrflow-contracts/events/leave-request-fact.v1.avsc`, with fields traced to the persisted leave request and expanded leave days. Charged days remain exact decimal text. Names, reasons, titles and medical/supporting details are excluded.

`LeaveSnapshotIntentTest` initially failed because the old outbox lacked a typed envelope/stream position. After the forward V2 migration and MapStruct payload mapping, its focused run passed one case spanning actual pending and approved commands: each intent matches the managed database row's committed revision plus one, includes actual expanded dates and state, and excludes private text. Driver numeric-wrapper/decimal-scale test assumptions were corrected without changing their numeric assertions. The writer flushes and reads the managed owner row rather than trusting a detached caller's revision. Stable stream insertion is serialized per employee. Legacy wire-version-0 intents remain unresolved, never relabeled.

The complete leave reactor is running next; publication/replay and downstream reporting/attendance consumers are still separate unverified substeps. The user separately approved the UAT work/leave-overlap rule on this date: flag HR review, retain debit, never auto-refund. That decision is in the decision register; this transport change alone does not implement conflict resolution. No commit/deploy occurred.

The complete leave reactor for stone 3a exited 0: 28 leave tests and 3 contract tests passed, zero failures/errors/skips. Log: `/tmp/hrflow-uat-leave-snapshot-verify.log`. Broker publication is the next small task and remains unverified until its separate result.

### UAT repair stone 3b — actual leave publication and failed-send recovery

The initial `LeaveKafkaPublicationTest` ran actual leave commands against the restricted leave runtime role but received zero Kafka records, reproducing the missing relay. After the owner-local single-leader relay and explicit codec were added, its focused run passed through real disposable Kafka and Schema Registry with producer auto-registration disabled. The test uses a separate NOSUPERUSER/NOBYPASSRLS metadata-only leave job role, denies broker sends first, verifies zero intents were marked published, then restores sending and receives pending/approved snapshots in order. Positive revisions increase, owner working dates survive Avro round-trip, and private text is absent. The next complete leave reactor is running; no result is presumed. Reporting/attendance integration is still separate work.

Configuration additions default the relay off and require explicit private job credentials; metadata discovery never reads business tables under the job role. The schema-registration helper was created and syntax-checked only, not executed against any deployed environment. No commit/push/deploy occurred.

The complete stone 3b leave reactor exited 0: 29 leave tests and 3 shared-contract tests passed, zero failures/errors/skips. Log: `/tmp/hrflow-uat-leave-publication-verify.log`. The registration helper passes `bash -n`; it has not been run against a deployed environment.

### UAT repair stone 5a — terminal snapshots preserve released dates (2026-10-28)

Extending the actual owner-command snapshot test to cancellation failed because its working-date list was empty (`0ed316eba2694b50`). Rejection/cancellation now capture the full owner dates in the terminal intent before releasing the date reservations in the same transaction. The same snapshot case passed (`35ee67c8d665436b`); the complete leave reactor passed 29 leave and 3 shared-contract tests (`/tmp/hrflow-uat-terminal-leave-verify.log`), zero failures/errors/skips. Its real restricted-role Kafka/Registry failed-send recovery now receives five ordered states: PENDING, APPROVED, CANCELLED, PENDING, REJECTED. Both terminal records retain actual dates, and the second request reuses the freed date; no leave_day reservation remains.

The codec now rejects empty owner-date snapshots rather than publishing an unusable record. This changes transaction ordering only: balance compensation/authorization policy and wire shape are unchanged, and no new schema/client/infrastructure component is introduced. Reporting and attendance can use the terminal full snapshot to remove overlays without guessing dates. No commit/deploy.

### UAT repair stone 5b — independent leave decisions and private detail (2026-10-28)

The provider regression proved three unauthorized successes: unrelated manager approval without a relationship, HR self-approval when account subject differs from employee ID, and another employee reading private same-tenant detail (`51874100242e4c49`, all three expected denials returned 200). The first test draft had stale If-Match values; it was corrected to the actual entity revision before using the behavioral red. With centralized LeaveAccess and separate verified actor/employee identities, the same three cases passed (`fba4b21fa85b4180`).

Forward V3 stores minimal tenant-RLS employee relationships. Manager decisions require a known active current direct report and active manager; missing references return REFERENCE_NOT_READY, wrong relationships 403. Both actor and employee identities prohibit self-decision. Detail is requester/HR/current-team only and other employees receive 404. Pending listing applies team scope in the database, excluding self; HR keeps tenant scope. No unscoped public read overload remains. Approver roles follow the approved Manager/HR responsibilities; tenant-admin alone no longer confers HR approval. Canonical descriptions and client bundles were updated. New positive/current-reassignment cases and existing regression fixtures use explicit local relationship records; they do not prove live organization replay. The complete leave reactor is running; the actual organization subscriber/bootstrap is the next separately verified step. No client shape, cross-owner table grant or extra dependency was added. No commit/deploy.

Complete stone 5b verification exited 0: 33 leave and 3 shared-contract tests passed, zero failures/errors/skips (`/tmp/hrflow-uat-leave-access-fixed-verify.log`). The first broad run exposed a missing positive-manager fixture and an existing error-text compatibility assertion; both were repaired without weakening authorization. Current-team reassignment/private detail/pending filtering and the real terminal publication suite passed. Live owner organization replay is still the next gate.

### UAT repair stone 5c — actual employee replay and revocation (2026-10-28)

With owner employee snapshots already in real Kafka, the leave manager request initially returned 503 (`44b3adef479d4cb7`). The explicit employee projection consumer, bounded startup replay and acknowledgment-after-transaction listener made the same restricted-runtime/real Registry/Kafka case pass (`825433d9898643c0`). It also verifies live termination revokes manager access and a stale ACTIVE revision cannot restore it. Auto-registration stays false. Bootstrap captures all topic end positions and completes before manager authorization; retained rows alone cannot authorize while replay is absent/incomplete.

Local scope/transaction tests mock only replay readiness so their positive relationship fixtures remain isolated; actual replay is verified in the separate broker test. The shared disposable leave transport fixture now serves both owner publication and reference replay, without changing production dependencies. The full leave reactor is running with rollback/routing/stale-reference and closed-bootstrap cases; no broad result presumed. Employee-calendar assignments, shift and holiday consumption remain subsequent work. No commit/deploy.

Complete stone 5c verification exited 0: 36 leave and 3 shared-contract cases passed, zero failures/errors/skips (`/tmp/hrflow-uat-leave-employee-verify.log`). Actual restricted-role bootstrap/termination/stale recovery and real terminal publication ran, plus local receipt/relationship rollback and closed-readiness cases.

### UAT repair stone 5d — separate requests per balance year (2026-10-28)

The pure policy case initially accepted a December–January range instead of rejecting it (`90fc91c34e7b4efe`). After recording the user’s explicit decision and validating the range before owner lookups, the selected calculation and provider scope suites passed 13 cases (`/tmp/hrflow-uat-leave-year-green.log`), zero failures/errors/skips. Both preview and submit return 422; balance availability, existing request count, ledger, day reservations and outbox remain unchanged. Each year needs its own request. The pure validator is shared with calculation; the HTTP application boundary translates validation to a clear response. Canonical descriptions and both client bundles were synced; existing form error handling consumes the response. No database/event shape or rollover policy changed. No commit/deploy.

### UAT repair stone 5e — cohesive leave responsibilities (2026-10-28)

The changed 482-line leave service mixed private reads, calendar/submission calculation and approval/cancellation settlement. The cohesive extraction now wires `LeaveQueries` (80 lines), `LeaveSubmissionService` (185) and `LeaveDecisions` (253) directly into the existing controller; there is no new forwarding facade, framework or artificial interface. Lombok supplies only mechanical constructor injection, and existing MapStruct response mapping/transaction boundaries remain. Actual test callers now use the owning responsibility. Two compile checks caught the old explicit controller constructor during rewiring; it was removed before acceptance.

The complete leave reactor passed 38 leave and 3 shared-contract tests, zero failures/errors/skips (`/tmp/hrflow-uat-leave-cohesion-verify.log`), including both real broker suites, owner-year guards, private/current-team reads, stale writes and compensation history. Contracts check, dependency policy and docs lint passed. This is a behavior-preserving structure change; calendar policy correctness is still the next independently tested task. No database, event or client wire shape changed. No commit/deploy.

### UAT repair stone 5f — effective leave calendar inputs (2026-10-28)

The provider test first reserved a weekday using no employee calendar (`b8d9dc6073e9430e`); after the explicit assigned-calendar resolver the same missing-reference case passed (`277182cb91134109`). Eight selected policy/calendar cases then passed (`/tmp/hrflow-uat-calendar-local-green.log`): an actual Sunday/Monday shift charges Sunday while excluding the tenant Monday holiday, a foreign tenant holiday has no effect, full/half leave preserves exact assigned durations (21600/10800 seconds), and missing references change no balance.

V4 adds nullable unknown assignment basis, tenant-RLS shift/holiday references and exact scheduled seconds; no old calendar is backfilled by guesswork. Calculation now consumes the owner’s actual working-date set; the earlier pure calendar fixture is relabeled to its proper charge-policy responsibility, and actual exclusion is verified through the provider resolver. New tests explicitly seed their own assigned calendars/readiness fixture instead of relying on Monday–Friday or eight-hour fallback. Actual duration seconds are authoritative; the existing hours column is a rounded display representation and never determines day debit. Preview/submit descriptions and client bundles were updated during this stone; no response shape changed.

The complete leave reactor is running. This local resolver step does not yet prove actual shift/holiday transport or readiness: those subscribers/replay are the next small integration task. No cross-owner SQL, infrastructure addition or speculative HR calendar is introduced. No commit/deploy.

Complete stone 5f local-calendar verification exited 0: 40 leave and 3 shared-contract tests passed (`/tmp/hrflow-uat-calendar-local-verify.log`), zero failures/errors/skips. Existing approval/cancel/lock fixtures now explicitly own their assigned calendars. Actual employee/shift/holiday replay into calculation is the next separately executed step.

### UAT repair stone 5g — live assigned calendar and replay gate (2026-10-28)

The extended actual organization-event fixture first returned 503 for preview despite full employee assignment/shift/holiday records already in real Kafka (`baaca8c5c51341de`). With structural MapStruct assignment conversion, typed calendar subscriber and bounded startup replay, both organization broker cases passed (`e1f86b91b91e4748`). Sunday work plus tenant Monday holiday charges one day; live holiday removal changes the preview to two; a stale holiday snapshot cannot restore the old exclusion. Manager termination/stale-active revocation still passes under restricted leave runtime credentials and actual Registry with auto-registration disabled.

Employee and calendar replay gates are independently required before calculating a charge. Existing local unit/provider suites mock readiness only and explicitly seed their owned calendars; the broker suite uses actual replay. V4 unknown assignments may be restored from the exact full owner revision during replay, including an existing receipt; a stale different revision cannot fill them. No guessed backfill is performed. The complete leave reactor is running with upgrade repair and calendar rollback/routing cases; no broad result presumed. The shared HTTP shape, balance-year decision, wire schema, privacy boundary and topology remain unchanged. No commit/deploy.

Complete stone 5g verification exited 0: 43 leave and 3 shared-contract tests passed (`/tmp/hrflow-uat-live-calendar-verify.log`), zero failures/errors/skips. The real replay/holiday-removal/manager-revocation and terminal publication paths ran, alongside exact-revision upgrade repair and rollback/routing cases.

### UAT repair stone 5h — request-first settlement locking (2026-10-28)

A real database lock-wait regression reproduced simultaneous approval attempting a second settlement: the second command violated the nonnegative pending constraint and surfaced an unexpected error (`17309e100e4642f8`). Its initial mock-repository draft did not exercise a valid real method and was replaced by an actual external balance lock plus observed PostgreSQL waiters before accepting the behavioral red. With tenant-scoped pessimistic request locking before eligibility/ETag/balance checks, four selected races passed (`2bc8eb98943d4263`): approve, reject, pending cancel and approved cancel settle exactly once; losers return 409/412 and no second ledger effect. Database constraints remain intact.

The broader leave verifier is running with an explicit omission of `LeaveCalendarTest`: its newly proposed complementary-half acceptance example awaits the user’s policy decision. Four earlier calendar/upgrade/rollback cases passed at the prior broad checkpoint; the proposal is not accepted business behavior yet. No existing security test is disabled or rewritten to hide the policy gap. Request locking changes no wire/API/schema or cross-owner boundary; the same canonical stale-write behavior applies. No commit/deploy.

Stone 5h broader verification exited 0 with 43 leave and 3 shared-contract cases (`/tmp/hrflow-uat-leave-decision-lock-verify.log`), zero failures/errors/skips. `LeaveCalendarTest` was explicitly omitted while its new complementary-half proposal awaits policy confirmation; this command is not a complete baseline. Both real broker suites and all four settlement races ran.

### UAT repair stone 5i — approved cancellation is future-only (2026-10-28)

The provider case initially refunded an already elapsed approved request (`04cbcbadb1654e20`). The approved architecture §12.1–12.2 permits future approved cancellation only. Guarding the original reserved dates against the employee-local date now rejects elapsed/mixed requests before any refund; all eight selected settlement-race/cancellation cases passed (`/tmp/hrflow-uat-future-cancel-green.log`). The historical compensation fixture explicitly fixes its clock before the leave instead of depending on the current month. Pending cancellation is unchanged; partial cancellation is not invented. The canonical description and both client bundles were synchronized before implementation. No schema/event shape changed; no commit/deploy.

### UAT repair stone 5j — complementary portions and database upgrade (2026-10-28)

After the user accepted complementary halves, the proposed example became an acceptance requirement. Its original run hit the old whole-date unique constraint (`24ba7d37a88a4d48`). With forward V5, the five calendar cases passed (`c440ac64d1824aa9`); the complete reactor now passes 51 leave and 3 shared-contract cases (`/tmp/hrflow-uat-leave-portions-final-verify.log`), zero failures/errors/skips. That includes separate MORNING/AFTERNOON requests, overlapping full-day rejection with no second debit, cancellation releasing only its own half, replacement reservation, and simultaneous overlapping requests across different leave types returning one success/one 409. Per-employee command serialization complements the database’s half-slot primary key; tenant/request/employee foreign keys prevent cross-owner reservation references.

`LeavePortionMigrationTest` verifies V4→V5 with preexisting full/half requests in two tenants under a NOSUPERUSER/NOBYPASSRLS migrator (`f4d100de97774c8d` focused pass, then included in the broad run). It proves legacy reservation expansion, runtime tenant filtering, duplicate-slot rejection and restored FORCE RLS. Backfill temporarily removes FORCE only for the table-owning migration transaction; runtime RLS remains enabled and FORCE is restored before commit. The upgrade fixture exposed a real prerequisite of existing V2: PostgreSQL checks database CREATE for CREATE SCHEMA IF NOT EXISTS even when precreated. The isolated migrator fixture now grants that DDL capability; runtime gets no such grant. No applied migration was edited. A historical HTTP compensation fixture now fixes its business clock before its leave to preserve the approved future-only rule. No commit/deploy.

### UAT repair stone 5k — captured paid/unpaid accounting (2026-10-28)

The new accepted unpaid case first returned a fabricated zero balance instead of untracked entitlement (`f7cff6931d9a4daa`). With forward V6 and explicit paid/unpaid accounting policies, the same case passed (`a3823ba5a8d3475a`): preview exposes null balance amounts without an insufficient-balance warning; submission/approval/cancellation retain request, dates, events and distinct UNPAID ledger history while creating/changing no entitlement balance. Changing the type’s paid flag after submission does not change the request’s captured settlement mode.

This is the concrete variation admitted by ADR 013: two small consumer-focused accounting implementations selected in composition, with workflow, audit and HTTP responsibilities remaining separate. Paid settlement retains its locks/constraints; unpaid never fabricates an allocation. V6 leaves historical unknown accounting modes unknown, causing explicit repair instead of inferring from today’s type definition. MapStruct supplies the preview structural mapping. The canonical nullable preview fields and balanceTracked flag were specified first and both client bundles synced. The full leave reactor is running; mobile handling of the new explicit untracked state and known preview/catalog field mismatches are the next small consumer tasks. No payroll deduction/rollover policy is invented; no new framework/dependency, commit or deployment.


The complete unpaid-accounting reactor then passed 52 leave cases plus 3 contract cases, zero failures/errors/skips (`/tmp/hrflow-uat-unpaid-accounting-verify.log`). This includes the real PostgreSQL/Kafka tests and the migration/concurrency cases; no deployment was performed.

### UAT repair stone 5l — mobile untracked preview (2026-10-28)

The actual mobile preview adapter first rejected the owner’s null unpaid balance (`/tmp/hrflow-mobile-leave-preview-red.log`). After adding a feature-owned strict preview mapper and nullable domain amounts, all 8 focused cases passed (`/tmp/hrflow-mobile-leave-preview-green.log`): explicit unpaid nulls, paid amounts, older paid-only numeric previews, and rejection of inconsistent flags, null tracked amounts, text amounts and NaN. The form shows “Not tracked” for unpaid amounts. Remote errors remain visible; zero is never substituted for unknown/untracked amounts. The MCP mobile runner returned an execution error, so the installed Flutter CLI ran the test instead.

The server, data, events and infrastructure need no further change for this consumer decoding stone; the canonical preview and generated client bundles were already updated in stone 5k. Web form/catalog alignment and the mobile type-code/collection boundary still need repair before claiming an end-to-end client leave journey. The existing large shared form receives only the balance rendering change; a broader cohesion review remains part of integration. No commit/deploy.


### UAT repair stone 5m — paged leave catalogs without breaking legacy clients (2026-10-28)

The actual HTTP test first failed because an explicit page request still returned a bare array (`d81b1c313f7b42d9`, no `$.total`). With scoped database pagination for type and balance catalogs, both provider cases passed (`9dfeb0fb54bf431b`), including the complete lifecycle regression: stable catalog order, distinct pages, own-employee balance counts, page/size validation and the unchanged historical no-page array. Queries apply tenant/employee/year before count and limit. No data, event or deployment change is required. A generic page envelope copies transport metadata; MapStruct still owns entity/response conversion.

Compatibility inspection also caught an earlier response-schema narrowing on daily status. Daily status and preview keep their original open object definitions; additive named schemas remain linked in provider metadata. After the correction, `.venv-checks/bin/python scripts/contracts.py compat --base origin/main` reports “Existing wire definitions preserved”; validation covers 178 operations, and both bundles are synced. The checker was not weakened. Actual provider legacy arrays remain explicitly described; the canonical paged request shape is the integration target.


### UAT repair stone 5n — mobile owner type and balance mapping (2026-10-28)

All three new adapter cases initially failed (`/tmp/hrflow-mobile-leave-owner-red.log`): ID sent instead of code, unknown type accepted and owner balance rejected for missing demo-only leaveTypeId. A feature-owned command-body adapter now resolves the selected catalog identity to its current code for both preview and submission; it preserves the command key and returned opaque ETag. The balance mapper accepts the actual owner type code and the existing separate demo field. All 12 selected owner/preview/correction cases passed (`/tmp/hrflow-mobile-leave-owner-green.log`). The source change is transport mapping, not client-side authorization or accounting. No new endpoints, migrations, events or environment variables. Backend catalog support was independently verified in 5m; mock adapters alone are not live provider acceptance. The mobile integration guide is updated; broader mobile verification follows. No commit/deploy.


### UAT repair stone 5o — authorized leave list pagination and filters (2026-10-28)

The real HTTP list case initially failed on the missing paged envelope (`0adc3e4dba5f4d39`). The selected 8 scope cases now pass (`f7e6566c2a724e53`): own-employee pagination, current manager scope, scoped total/count, deterministic ordering, literal case-insensitive search (percent is not a wildcard), minimum age and immediate removal after reassignment. Employee callers cannot read the approver list. Database tenant/relationship and filter predicates precede count/offset/limit; repeatable-read keeps each response’s count and rows consistent. Only validated static ASC/DESC fragments are interpolated; all user values are bound parameters. Explicit page requests use the canonical envelope; old callers omitting page keep the existing array. Invalid page/size/sort fails visibly. No event or data migration; both client bundles and operation descriptions are synced. Full leave verification is running.

The mobile full run also identified growth in the already-large shared form. Its leave preview is now a cohesive feature component rather than increasing the reviewed size limit; that limit/check remains unchanged. Newly added widget fixtures initially omitted the application theme; the fixture was corrected to use the actual app theme. Complete rerun evidence follows; no claim of a green full run is made from the earlier failure.


After paging and client cohesion repairs, the full leave reactor passed 54 owner cases plus 3 shared contract cases, zero failures/errors/skips (`/tmp/hrflow-uat-leave-paging-verify.log`). Mobile analysis found no issues and the full mobile suite passed 171 cases with one pre-existing opt-in live test skipped (`/tmp/hrflow-mobile-leave-final-analyze.log`, `/tmp/hrflow-mobile-leave-final-test.log`). The preview widget cases explicitly check unpaid “Not tracked” labels and paid amounts/evidence selection; architecture checks pass without raising the large-form limit. These local checks do not establish a deployed device/gateway journey. The wider MVP audit continues; no commit/deploy.


### UAT repair stone 5p — caller/path-scoped leave command outcomes (2026-10-28)

The real HTTP regression reproduced two defects (`012cfde54c2a442c`): another employee received the first caller’s cached private response for a reused key, and a successful approval retry returned 412 instead of its original result. After the forward V7 scoped receipt table and explicit tenant/account/POST/resource-path binding, all 3 selected provider cases pass (`d373ea5b65854826`). Same-caller retries preserve original response/ETag; different accounts with the same key execute their independent commands. Missing/invalid UUID-v4 keys return 428 before mutation. Approval/rejection/cancellation now use the same atomic receipt boundary as submission. Existing negative test fixtures supply keys so their authorization/business assertions remain exercised.

Historical receipts remain unchanged and retained. Since their original actor/path cannot be inferred safely, an attempted historical-key retry returns 409 requiring reconciliation rather than exposing the old result or repeating its mutation. The new table enforces tenant RLS. A restricted-runtime test of rollback, scope and legacy handling is running; full leave verification remains required. Canonical operation descriptions and both bundles are synced. No UI, accounting or event payload change; installed clients already generate/preserve UUID keys. No commit/deploy.

The two new restricted-runtime receipt cases also passed (`036ba5bdac3d4b78`) using a real PostgreSQL leave_app role with neither superuser nor BYPASSRLS: account/path/tenant separation, exact replay, body conflict, interrupted-command rollback, unscoped invisibility and retained historical-key reconciliation. The full reactor is now rerunning. Contract drift and target-branch compatibility both pass; the narrow foundation-doc check passes 13 files and does not replace the final full documentation baseline.


The first full V7 reactor attempt failed during Testcontainers PostgreSQL startup (`/tmp/hrflow-uat-leave-receipts-verify.log`). The mapped port 60871 returned an HTTP response; `lsof` showed the desktop Electron process bound specifically to 127.0.0.1 alongside Docker’s wildcard listener. PostgreSQL itself reported ready, while its JDBC init connection could not reach it. No application process was stopped or settings changed. The test-only diagnostic connection was stopped; the reactor is rerunning with newly allocated disposable ports. The failed/partial attempt is not counted as acceptance.

Review of that failed attempt also found one real fixture failure: the cross-year case supplied two different Idempotency-Key headers after the header update, so it reached the key precondition (428) instead of date validation (422). The duplicate fixture header was removed; its existing single UUID key is retained. This is separate from the port collision and must be verified again; no test expectation was relaxed.

The corrected cross-year/authorization scope suite now passes all 8 cases (`/tmp/hrflow-uat-receipt-regressions.log`, leave module). The immediately preceding whole-reactor rerun executed 57 cases with exactly the already-identified duplicate-header fixture failure and no environment errors (`/tmp/hrflow-uat-leave-receipts-retry-verify.log`). That aggregate remains a failed run; the corrected selected suite is separate evidence. The final full candidate verification must still pass.

The combined reactor now passes all 57 leave cases plus 3 shared contracts, zero leave failures/errors/skips (`/tmp/hrflow-uat-owner-receipts-verify.log`). This supersedes the failed/partial receipt attempts as the current leave-module baseline. The request module in that same run has one separately added, expected employee/account identity regression; the combined build is therefore not green.

### UAT repair — web owner leave catalog and calculation (2026-10-29)

All 8 new adapter cases first failed for absent owner preview/catalog and missing explicit page selection (`/tmp/hrflow-web-leave-owner-red.log`). The same 8 pass with a feature-owned leave adapter (`/tmp/hrflow-web-leave-owner-green.log`). Remote forms load actual catalog codes, preserve nullable untracked balances, reject inconsistent previews, and request the provider's paged response form. Submission retains that code rather than adding the demo label-to-ID mapping. Local demonstration calendars remain local.

After correcting sign-in/navigation fixture setup, the browser regression reached the expected missing owner-catalog option (`/tmp/hrflow-web-leave-preview-ui-red.log`). It now passes (`/tmp/hrflow-web-leave-preview-ui-green.log`): real component/transport code displays the owner half-day amount and unpaid accounting, sends the owner type code, and shows an unavailable preview without a substituted weekday calculation. All 105 web unit cases, type checking and structure checks pass (`/tmp/hrflow-web-leave-unit.log`, `...leave-typecheck.log`, `...leave-structure.log`). Browser routes are synthetic provider fixtures; previous real leave owner tests independently establish those payloads, not a deployed browser/gateway journey. No server, database, event or infrastructure change in this client stone. Existing canonical bundles remain the contract source. No commit/deploy.

### UAT repair — reach every leave page (2026-10-29)

The next adapter regression initially returned 25 balances where 26 existed (`/tmp/hrflow-web-leave-paging-red.log`), and the real page component had no way to reach the next owner page (`/tmp/hrflow-web-leave-paging-ui-red.log`). All 9 selected adapter cases now pass (`/tmp/hrflow-web-leave-paging-green.log`). Balance cards follow validated owner pages; catalog traversal is also exercised across 101 entries. The own-request screen requests each server page through the existing accessible pager and labels its search/count as applying to that page, with no nested client paginator.

The extended browser case reaches request 26, then completes the preceding preview/submission/error checks (`/tmp/hrflow-web-leave-paging-ui-green.log`, 1 passed). A draft exact-name locator had excluded the link's date label; its corrected accessible-name assertion still verifies the actual request. The calendar branch is explicit so adding pagination cannot alter the demo calendar. Full web CI checks are running. This is client fixture acceptance; no new endpoint, policy, persistence, event or infrastructure change. No commit/deploy.

The complete web CI command `npm run check` exits 0 after the paging repair (`/tmp/hrflow-web-leave-final-check.log`): formatting, structure, type checking, data/config/unit tests, demo build and bundle-size checks pass. The separately executed selected remote browser journey also passes. The command does not establish a deployed HR backend journey.

### UAT repair — attendance feedback reaches the leave owner (2026-10-29)

The user-approved rule is flag for HR review and retain the debit. Generic daily review flags cannot identify which of complementary half-day requests actually overlaps work. Attendance therefore adds nullable/default-null per-reference workRecorded evidence; null is legacy unknown, never a positive conflict. The new field test first failed (`/tmp/hrflow-uat-leave-conflict-contract-red.log`) and all 5 contract cases then passed (`/tmp/hrflow-uat-leave-conflict-contract-green.log`), checking both readers against the preserved original daily schema. Real Schema Registry FULL_TRANSITIVE registration also passes in the broker selection below.

LeaveAttendanceReview first failed compilation because its API did not exist (`/tmp/hrflow-uat-leave-attendance-review-red.log`). A subsequent source-location compilation mistake is preserved in `/tmp/hrflow-uat-leave-attendance-review-missing-source.log`; after correction, both restricted PostgreSQL cases pass (`/tmp/hrflow-uat-leave-attendance-review-green.log`). Forward V8 atomically retains tenant/event receipts, daily revisions and minimal conflict provenance. Only an explicitly overlapping portion with matching current approval revision, employee and original reserved date can change APPROVED to NEEDS_REVIEW. Existing debit, accounting mode, ledger and reserved dates remain unchanged. Duplicate/stale facts, legacy unknown, cancelled leave, wrong employee and foreign tenant cannot manufacture a review/refund. Outer rollback remains retryable. HR pending lists include NEEDS_REVIEW; manager pending lists retain their PENDING scope. No automatic HR resolution policy is invented.

The combined selection passes 16 owner cases (`/tmp/hrflow-uat-leave-conflict-owner-kafka.log`, 11 attendance + 5 leave, no failures/errors/skips). Attendance distinguishes the two half portions at the exact midpoint using the captured shift despite later edits. Real Kafka replay plus owner submission/approval receives a typed explicit overlap, commits NEEDS_REVIEW, acknowledges actual offsets and publishes exactly one review owner snapshot through the actual relay, with no compensation entry. This incoming overlap is a synthetic attendance fact; an all-services tap-to-review journey is not established by it. Broader owner suites remain due. HRFLOW_LEAVE_ATTENDANCE_CONSUMER_ENABLED is opt-in until manual release configuration; no commit/deploy.

### UAT safety guard — required supporting evidence remains unavailable (2026-10-29)

The client already prevents unsupported evidence-required leave submission, but the provider incorrectly accepted such a request without any supporting file. A new provider regression reproduced the missing rejection (`/tmp/hrflow-uat-leave-evidence-guard-red.log`). Submission now returns 422 with LEAVE_EVIDENCE_WORKFLOW_UNAVAILABLE before reserving balance/dates or creating request/ledger/outbox rows. Preview still exposes the actual requirement. Both this guard and the existing captured unpaid-accounting lifecycle pass (`/tmp/hrflow-uat-leave-evidence-guard-green.log`, 2 cases, zero failures/errors/skips).

This is fail-closed UAT behavior, not completion or waiver of the private leave attachment feature. Evidence-required leave types remain explicitly unavailable; ordinary supported leave types retain their accepted workflow. No contract field, new storage framework or unapproved evidence bypass was invented. No commit/deploy.

Scope clarification from the owning execution guide: Week 06 BUILD-GUIDE-PART-3 explicitly says “attachments remain disabled unless secure path is complete” for the supported leave client slice. The fail-closed provider guard matches that conditional boundary; ordinary leave may enter UAT while evidence-required types stay disabled. The richer private evidence feature remains future implementation work and is not counted as completed or silently waived.
