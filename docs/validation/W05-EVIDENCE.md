# Week 05 local candidate evidence

Date: 2026-10-27. Owner week: 05 candidate; verified roadmap position: W02-S26. This is an early local candidate for the 2026-11-23 to 2026-11-27 week, not a Week 05 gate result. Root, web and mobile changes are uncommitted in separate repositories. The intended Acer Nitro 5 and encrypted external backup disk are unavailable for the required deploy and restore gate.

## Accepted intent and current behavior

- ADR 010 and ADR 011 require token validation at the gateway and each Spring service via Spring Security OAuth2 Resource Server. `services/notification` decodes RS256/JWKS tokens and protects all `/notifications/**` endpoints. Gateway routing for `/api/v1/notifications/**` strips prefix 2 and sanitizes caller headers, verified in `OrganizationGatewayRoutingIT`.
- Notification core migration V1 (`V1__notification_core.sql`) adds PostgreSQL 18 tables: `notification.notification_inbox` (PK `consumer_name, tenant_id, event_id`), `notification.notification_record`, `notification.notification_preference` (PK `tenant_id, employee_id`), `notification.notification_device`, and `notification.notification_outbox`. Row Level Security (`FORCE ROW LEVEL SECURITY`) is enabled on all tables, isolating tenant data via PostgreSQL session variable `app.tenant_id`.
- MapStruct mapping policy (ADR 014) is implemented per explicit user decision: `NoticeMapper` maps `NotificationRecord` to `NoticeResponse`, and `PreferenceMapper` maps `NotificationPreference` to `NotificationPreferencesResponse` using `@Mapper(componentModel = "spring")`. `NoticeResponse` provides fields `id`, `title`, `read`, `detail`, `body`, `createdAt`, `to`, `recordId`, and `kind`, satisfying both Web and Mobile client contract parsers.
- Bounded Ordered Publication (`OrderedOutboxRelay`): claims aggregate head records ordered by revision, publishes revision 1 before revision 2, and strictly prevents bypassing uncommitted or pending earlier revisions.
- Commit Effect and Inbox Before Offset (`NotificationConsumerService`): processes incoming Kafka events with transactional inbox deduplication before inserting notification records. The Kafka consumer offset is acknowledged only after database transaction commit, preventing message loss on crash.
- Optimistic Concurrency & ETag: `NotificationPreference` uses `@Version private long version`. `GET /notifications/preferences` returns ETag header; `PUT /notifications/preferences` requires `If-Match` matching entity version, returning 412 Precondition Failed on stale concurrency revision.
- Observability and Metrics (`NotificationMetrics`): exports Micrometer gauge for `hrflow.notification.consumer.lag` and counters for delivered and read notices.
- Complete route tenant matrix (`ExposeOwnInboxPreferencesTest`): all notification routes enforce authentication (401), reject missing tenant claim (403), enforce employee ownership, and prove complete cross-tenant boundary isolation.

## Stone 1 verification (W05 Stone 1, W05-S01–S03)

- Steps: `W05-S01` to `W05-S03` (inspect Kafka publication boundary, trace single transaction boundary, assert zero provider call inside command transaction).
- Targets: `services/notification/src/test/java/com/hrflow/notification/InspectKafkaPublicationBoundaryTest.java`.
- Verified behaviors: single transaction boundary trace verified; simulated Kafka producer failure aborts without trace leak; outbox record and command commit atomically.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/notification -Dtest="InspectKafkaPublicationBoundaryTest"` exited 0 (2 tests, 0 failures, 0 errors, 0.562s).

## Stone 2 verification (W05 Stone 2, W05-S04–S06)

- Steps: `W05-S04` to `W05-S06` (constrain owner outbox and consumer inbox tables: `V1__notification_core.sql`, composite primary key on inbox, RLS enforcement).
- Targets: `services/notification/src/main/resources/db/migration/V1__notification_core.sql`, `services/notification/src/test/java/com/hrflow/notification/ConstrainOwnerOutboxConsumerInboxTest.java`.
- Verified behaviors: duplicate inbox receipt fails primary key constraint `(consumer_name, tenant_id, event_id)`; transaction-local RLS blocks foreign tenant queries under non-superuser role.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/notification -Dtest="ConstrainOwnerOutboxConsumerInboxTest"` exited 0 (2 tests, 0 failures, 0 errors, 4.716s) against Testcontainers PostgreSQL 18.6.

## Stone 3 verification (W05 Stone 3, W05-S07–S12)

- Steps: `W05-S07` to `W05-S12` (publish bounded ordered Kafka work: `OrderedOutboxRelay`, monotonic revision order, blocking uncommitted earlier revisions).
- Targets: `services/notification/src/main/java/com/hrflow/notification/outbox/OrderedOutboxRelay.java`, `services/notification/src/test/java/com/hrflow/notification/PublishBoundedOrderedKafkaWorkTest.java`.
- Verified behaviors: outbox relay queries aggregate head ordered by revision; publishes revision 1 before revision 2; earlier pending revision blocks subsequent publication.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/notification -Dtest="PublishBoundedOrderedKafkaWorkTest"` exited 0 (2 tests, 0 failures, 0 errors, 4.669s) against Testcontainers PostgreSQL 18.6.

## Stone 4 verification (W05 Stone 4, W05-S13–S18)

- Steps: `W05-S13` to `W05-S18` (commit effect and inbox before offset: `NotificationConsumerService`, duplicate event skipping, transactional offset acknowledgment).
- Targets: `services/notification/src/main/java/com/hrflow/notification/consumer/NotificationConsumerService.java`, `services/notification/src/test/java/com/hrflow/notification/CommitEffectInboxBeforeOffsetTest.java`.
- Verified behaviors: incoming Kafka event is recorded in inbox and committed to DB before consumer offset is acknowledged; duplicate event is detected via inbox primary key and skipped idempotently.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/notification -Dtest="CommitEffectInboxBeforeOffsetTest"` exited 0 (2 tests, 0 failures, 0 errors, 4.662s) against Testcontainers PostgreSQL 18.6.

## Stone 5 verification (W05 Stone 5, W05-S19–S24)

- Steps: `W05-S19` to `W05-S24` (expose own inbox and preferences: REST endpoints, pagination, unread count, mark read, optimistic concurrency on preferences).
- Targets: `services/notification/src/main/java/com/hrflow/notification/controller/NotificationController.java`, `services/notification/src/main/java/com/hrflow/notification/service/NotificationService.java`, `services/notification/src/test/java/com/hrflow/notification/ExposeOwnInboxPreferencesTest.java`.
- Verified behaviors: list with pagination, unread count, mark individual notice read, mark all read, preferences GET/PUT with ETag and `If-Match` (stale returns 412), strict cross-tenant isolation.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/notification -Dtest="ExposeOwnInboxPreferencesTest"` exited 0 (4 tests, 0 failures, 0 errors, 4.984s) against Testcontainers PostgreSQL 18.6.

## Stone 6 verification (W05 Stone 6, W05-S25–S30)

- Steps: `W05-S25` to `W05-S30` (final feature integration: `NotificationIntegrationTest`, `NotificationContextTest`, Micrometer metrics `NotificationMetrics`, clean module regression).
- Targets: `services/notification/src/main/java/com/hrflow/notification/metrics/NotificationMetrics.java`, `services/notification/src/test/java/com/hrflow/notification/NotificationIntegrationTest.java`, `services/notification/src/test/java/com/hrflow/notification/NotificationContextTest.java`.
- Verified behaviors: full application context startup with PostgreSQL 18; actuator health endpoint; Micrometer gauge `hrflow.notification.consumer.lag` and delivery counters; zero dependency policy violations.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/notification -Dtest="NotificationIntegrationTest,NotificationContextTest"` exited 0 (2 tests, 0 failures, 0 errors, 7.273s).
- Module clean regression: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw --batch-mode -pl services/notification clean test` exited 0 (20 tests run, 0 failures, 0 errors, 0 skipped, 16.267s).

## Stone 7 verification (W05 Stone 7, W05-S31–S33)

- Steps: `W05-S31` (reproduce earliest failed check); `W05-S32` (recheck client and contract negatives: 401 unauthenticated, 403 missing tenant, 412 stale If-Match on preferences, duplicate event idempotency); `W05-S33` (review carry-forward and reserve).
- Targets: `contracts/openapi.json`, `docs/validation/W05-EVIDENCE.md`, root/web/mobile diffs.
- Verified behaviors: OpenAPI contract validation; contract compatibility against `origin/main`; git diff check clean across root, web, and mobile repositories; documentation lint passed.
- Diff check: `git diff --check && git -C web diff --check && git -C mobile diff --check` exited 0.
- Contract validation: `.venv-checks/bin/python scripts/contracts.py validate && compat --base origin/main && check` exited 0 (177 operations, 6 contract tests).
- Documentation lint: `python3 docs/roadmap/tools/docs-lint.py docs README.md AGENTS.md contracts/README.md tools/project_mcp/README.md` exited 0 (316 Markdown files, 0 errors, 0 warnings).

## Stone 8 assessment & physical blockers (W05 Stone 8, W05-S34–S36)

- Steps: `W05-S34` (redeploy checked service candidate via `ops/deploy.sh`); `W05-S35` (restore release backup in scratch via `ops/restore-scratch.sh`); `W05-S36` (decide gate).
- Physical blocker - deploy: `ops/deploy.sh` requires the dedicated Acer Nitro 5 private host, which remains physically unavailable in this development environment.
- Physical blocker - restore: `ops/restore-scratch.sh` requires the encrypted external backup disk, which remains physically disconnected.
- Gate decision: `BLOCKED`. The Week 05 release gate remains **NOT RUN**. Verified roadmap position remains `W02-S26` in `docs/roadmap/YOU-ARE-HERE.md`. No weekly gate is claimed passed.

## Executed checks

| Check | Result | Limit |
| --- | --- | --- |
| Kafka publication boundary, `InspectKafkaPublicationBoundaryTest` | Exit 0 (2 tests); single transaction boundary trace verified, simulated Kafka failure aborts without trace | Unit test with mocked Kafka producer |
| Schema invariants and RLS, `ConstrainOwnerOutboxConsumerInboxTest` | Exit 0 (2 tests); duplicate inbox receipt fails PK constraint, RLS blocks foreign tenant queries under non-superuser role | Disposable PostgreSQL 18; no live host deployment |
| Bounded ordered publication, `PublishBoundedOrderedKafkaWorkTest` | Exit 0 (2 tests); publishes revision 1 before 2, earlier pending revision blocks subsequent publication | Unit test with in-memory store and mock producer |
| Commit effect and inbox before offset, `CommitEffectInboxBeforeOffsetTest` | Exit 0 (2 tests); duplicate event skipped via inbox idempotency, DB commit must succeed before offset acknowledgment | Unit test with mock consumer and transactional helper |
| Notification REST APIs and tenant matrix, `ExposeOwnInboxPreferencesTest` | Exit 0 (4 tests); list with pagination, unread count, mark read, mark all read, preferences GET/PUT with ETag and If-Match, tenant isolation | In-process MockMvc with disposable PostgreSQL 18; synthetic JWT |
| Notification full integration, `NotificationIntegrationTest` | Exit 0 (1 test); Spring application context load, actuator health, security configuration verified | Disposable PostgreSQL 18 and dynamic properties |
| Notification module verify, `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw -pl services/notification verify` | Exit 0; 20 tests (11 unit, 9 integration) passed in 15s | Local containers; Schema Registry mocked for event tests |
| Avro schema compatibility, `./mvnw -pl libs/hrflow-contracts test` | Exit 0; 3 tests passed across Avro schemas and compatibility verification | Local JVM test suite |
| Gateway routed security and routing, `./mvnw -pl services/api-gateway test` | Exit 0; 18 tests passed including routed security and header sanitization for notification routes | In-process MockWebServer; no full Acer deployment |
| Foundation scope, `.venv-checks/bin/python scripts/verify-project.py --scope foundation --base origin/main` | Exit 0; 177-operation OpenAPI validation, 6 contract tests, docs lint (314 Markdown files, 0 errors, 0 warnings), diff check, and compatibility check passed | Automated local baseline only |
| Web scope, `.venv-checks/bin/python scripts/verify-project.py --scope web` | Exit 0; 89 unit tests, bundle check, 12 Playwright E2E scenarios, and diff check passed | Local Chromium browser; no live provider |
| Mobile scope, `.venv-checks/bin/python scripts/verify-project.py --scope mobile` | Exit 0; 155 tests passed (1 live test skipped by default), Flutter analyze clean, bundle check, and diff check passed | Local headless Flutter runtime; no physical device |
| Tooling scope, `.venv-checks/bin/python scripts/verify-project.py --scope tooling` | Exit 0; 23 tests passed across MCP server, test runner, and workspace traversal | Local Python virtual environment |

## Boundary review

- Backend domain logic: `NotificationService`, `NotificationConsumerService`, `OrderedOutboxRelay` implement business rules without speculative design patterns.
- Web: Nuxt client decodes `/notifications` and `/notifications/preferences` matching expected fields (`id`, `title`, `read`, `detail`, `createdAt`).
- Mobile: Flutter client decodes `/notifications` and `/notifications/preferences` matching expected fields (`id`, `title`, `read`, `body`, `recordId`, `kind`, `createdAt`).
- HTTP contract: `contracts/openapi.json` owns the canonical specification. All implemented notification routes conform to schema definitions.
- Authentication & Authorization: Spring Security OAuth2 Resource Server validates JWTs. All notification endpoints require authenticated employee in matching tenant.
- Tenant scope: `TenantSqlScope` sets `app.tenant_id` session variable on each transaction; PostgreSQL RLS policies enforce isolation on all tables.
- Concurrency & Stale writes: `NotificationPreference` optimistic locking with `@Version` and `If-Match` validation prevents lost updates on preferences.
- Idempotency: `notification_inbox` primary key `(consumer_name, tenant_id, event_id)` prevents duplicate event side-effects.
- Events & Replay: Transactional outbox records outgoing events; inbox records consumed incoming events before offset commit.
- Documentation: `docs-lint.py` passes 316 Markdown files with 0 errors and 0 warnings.

## Disposition

`CANDIDATE — LOCALLY VERIFIED`

Local implementation and tests across backend, web, mobile, foundation, and tooling are green. The Acer Nitro 5, encrypted external backup disk, and physical device gates remain blocked. No weekly gate is claimed passed.


### UAT repair — notification runtime tenant scope (2026-10-29)

The new real notification_app fixture first failed inserting a receipt because the service never established its tenant RLS context (`/tmp/hrflow-uat-notification-rls-red.log`). With mandatory transaction-local TenantSqlScope at the consumer and every inbox/preference service entry point, the same restricted-runtime case passes (`/tmp/hrflow-uat-notification-rls-green.log`). It verifies committed receipt/effect, private tenant/recipient listing, foreign-recipient 404, foreign-tenant invisibility, mark-read, preference creation and unscoped invisibility, using a role with neither superuser nor BYPASSRLS. The initial owner-role fixtures did not prove this operating boundary. Lombok removes only mechanical constructor boilerplate; explicit tenant/security logic remains outside MapStruct. No schema, DTO, client, broker wire or configuration change is needed for this scoped SQL repair. The case uses a synthetic event helper and does not claim domain listener routing or real Kafka acceptance. Broader notification checks remain required after the next consumer transaction repair. No commit/deploy.


### UAT repair — acknowledge only the actual notification commit (2026-10-29)

The new outer-rollback case first showed that the nested helper acknowledged an uncommitted delivery (`/tmp/hrflow-uat-notification-commit-red.log`). With transaction afterCommit synchronization, all 4 selected restricted-runtime/offset cases pass (`/tmp/hrflow-uat-notification-commit-green.log`). Rollback leaves no notice, no acknowledgment and no delivered-success count; a genuine outer commit performs the acknowledgment/count exactly afterward. Duplicate-receipt acknowledgment also waits for the actual commit. This closes a real transaction-boundary defect without changing the domain event schema, client or HR policy. No commit/deploy.


### UAT repair — concurrent notification redelivery (2026-10-29)

A deterministic PostgreSQL race held receipt insertion behind a table barrier until both contenders reached locks, then released them. The old check-then-insert path failed one delivery with duplicate notification_inbox_pk (`/tmp/hrflow-uat-notification-redelivery-red.log`). The consumer now serializes each tenant/event receipt with a transaction advisory lock before checking absence; the database unique key is retained. The targeted race and commit-order regressions are rerunning. The fixture checks one notice, one new effect, duplicate no-op and two acknowledgments only after settlement, under the non-bypass runtime role. No new schema/DTO/client/configuration or HR rule. No commit/deploy.

The deterministic redelivery race and commit-order selection now passes all 5 cases (`/tmp/hrflow-uat-notification-redelivery-green.log`), zero failures/errors/skips: both contenders settle, exactly one new private notice remains, and both committed deliveries acknowledge. The lock closes the check/insert race under real PostgreSQL; no unique constraint was removed.


### UAT repair — notification preference revision preconditions (2026-10-29)

The new restricted service case first accepted a preference update with no If-Match (`/tmp/hrflow-uat-notification-precondition-red.log`). Updates now require the current revision (428 absent, 412 stale or malformed), preserving the existing quoted revision and historical plain-number input without stripping arbitrary internal quotes. Negative mutations leave the prior revision intact. The selected provider/ETag suite is rerunning. This follows the accepted shared mutable-resource convention; no schema, DTO, role or HR preference rule changed. Broader preference creation/concurrency and domain delivery routing remain separate gates. No commit/deploy.

The preference precondition/provider selection passes all 8 cases (`/tmp/hrflow-uat-notification-precondition-green.log`), including the actual HTTP ETag/stale-write fixture and the restricted runtime cases. Deterministic initialization/edit races are the next independently tested delta.


### UAT repair — serialized notification preferences (2026-10-29)

Two deterministic PostgreSQL races reproduced initialization duplicate-key failure and an unhandled optimistic-lock failure during simultaneous edits (`/tmp/hrflow-uat-notification-preference-race-red.log`). Per-tenant/employee preference locking now precedes both find/create and revision checks. The target behavior is one shared initial row/revision, and exactly one accepted edit plus one clear 412 with no lost write. Existing primary key and optimistic version remain in place. The targeted restricted-runtime/HTTP suite is rerunning; no concurrency success is inferred until it finishes. No new schema, client, config or HR rule; no commit/deploy.

Both preference races and the HTTP/restricted selection now pass all 10 cases (`/tmp/hrflow-uat-notification-preference-race-green.log`): simultaneous initialization returns the same default revision; simultaneous edits settle as one 200 and one 412 without an internal exception. The complete notification module verifier is now running. This remains scoped storage/transaction evidence; domain routing and real Kafka notification delivery are still active gaps.

The complete notification module baseline now passes 26 owner/unit cases plus 3 shared contracts, zero failures/errors/skips (`/tmp/hrflow-uat-notification-scope-commit-verify.log`). This verifies current storage, tenant, preference and commit/redelivery repairs. It still does not prove actual domain-event recipient routing, templates, source-role replay or Kafka notification convergence. Those remain active gaps.

### UAT repair — authoritative notification references (2026-10-29)

The new reference tests first failed to compile because the local recipient projection API did not exist (`/tmp/hrflow-uat-notification-reference-red.log`); this is a preimplementation gate, not an executed business failure. Both actual restricted-runtime cases pass (`/tmp/hrflow-uat-notification-reference-green.log`): account/employee distinction, current HR role selection excluding requester, revocation/stale-event immunity, unknown legacy role/foreign-tenant denial, rollback and unready replay. Forward V2 is tenant-RLS and contains minimal employee/account/manager/active/roles/revision facts only.

Producer role evolution already passed exact old/new readers and actual Registry FULL_TRANSITIVE (W02 evidence). Notification replay/listener uses the existing private Kafka stack; no cross-owner SQL, email inference, paid identity service or new role. Domain delivery and real event routing are separate next work. No commit/deploy.

### UAT repair — committed typed request notices (2026-10-29)

The request-routing tests first could not compile because the feature API did not exist (`/tmp/hrflow-uat-request-notification-red.log`). All 10 selected restricted-runtime reference/delivery/receipt cases now pass (`/tmp/hrflow-uat-request-notification-green.log`). A corrected fixture count is tenant-scoped, so earlier test data cannot make rollback look failed. Current pending manager stage targets only its verified assigned current manager; HR stage targets independent active linked HR admins. Owner/employee role guesses are not used. Approval notices bypass only optional in-app suppression; stored preferences remain unchanged. Outcomes honor optional in-app preferences. Duplicate/stale events and outer rollback produce no extra/partial delivery. Notice text contains no private request reason.

Forward V3 stores monotonic owner revisions and a unique event/recipient/channel origin. Notice identity fields are guarded rather than broad entity setters. Existing resource kind/ID are preserved for client-owned route helpers; legacy `to` is an empty string rather than a fabricated recipient UUID link. Existing email/push/SSE remain outside this in-app Must. Failed records are currently retained for replay pending the separately verified bounded DLQ step.

A real PostgreSQL/Kafka/Schema Registry case passes (`/tmp/hrflow-uat-request-notification-kafka.log`, 1 case): replayed typed linked-role snapshots establish authority, actual request Avro manager→HR→outcome facts deliver exactly three notices, and duplicate/stale records advance offsets only after committed handling. Real group offsets are checked, not inferred from a quiet console. Leave/daily/correction routing and full notification/reliability checks remain separate work. No commit/deploy.

### UAT repair — leave routing and actual request-owner convergence (2026-10-29)

Leave routing follows the leave owner's single decision stage, rather than copying the separate general-request chain. PENDING targets the current verified manager and independent HR recipients; NEEDS_REVIEW targets HR and optionally informs the employee; terminal outcomes inform the employee according to optional preferences. The initial missing API compilation gate is `/tmp/hrflow-uat-leave-notification-red.log`. All 3 selected restricted domain cases pass in `/tmp/hrflow-uat-leave-notification-green.log`. Shared LeaveRequestWire now holds identical canonical validation previously duplicated in attendance/reporting; their broader checks remain due after this extraction.

The actual Avro broker case now covers request and leave together: duplicate/stale leave facts create exactly five expected leave notices and actual leave consumer offsets reach the published end (`/tmp/hrflow-uat-leave-notification-kafka.log`, 1 case, zero failures/errors/skips). This adds transport evidence to the restricted domain tests; it does not establish that an attendance conflict produces the owner's NEEDS_REVIEW transition.

The real request owner, notification process and reporting process also pass one combined journey (`/tmp/hrflow-uat-request-notice-reporting-journey.log`): signed HTTP submission and manager/HR decisions publish real owner outbox Avro, deliver stage-specific private in-app notices, converge the reporting projection and automatically produce the original HR requester's private CSV. Children use non-superuser/non-bypass schema roles and explicit synthetic linked-account fixtures. Sensitive request title/reason are excluded from notices and CSV. This is direct-provider evidence, not gateway/cookie login, physical device or deployment acceptance. No commit/deploy.

### UAT repair — publishable correction notifications (2026-10-29)

Correction creation previously wrote schema_version=0 and a full HTTP response (including private proposal/reason) into an outbox row ignored by the typed relay. The existing owner fixture now reproduces the missing wire acceptance (`/tmp/hrflow-uat-correction-typed-outbox-red.log`, expected 1 but actual 0). Forward V21 admits the minimal typed correction snapshot. A MapStruct structural mapper includes employee/date/state only; contextual envelopes retain positive owner revisions and the existing employee stream key. Legacy wire-zero payloads are preserved for explicit reconciliation, never relabelled. All 3 correction read/evidence cases pass (`/tmp/hrflow-uat-correction-typed-outbox-green.log`), preserving immutable original sessions and asserting no proposal/reason in publication intent.

The notification feature first failed compilation for a missing handler (`/tmp/hrflow-uat-correction-notification-red.log`). All 6 selected owner/domain/broker cases pass (`/tmp/hrflow-uat-correction-notification-owner-kafka.log`: 1 attendance publication, 4 notification domain and 1 combined notification broker case). Actual owner correction submission/approval publishes registered Avro PENDING/APPROVED, ordered with the related session stream and only marked sent after broker acknowledgment. Current independent manager/HR reviewers receive critical pending notices; employee outcomes honor optional preferences. Duplicate/stale facts do not reopen approval or duplicate notices. A shared small reviewer resolver preserves the separate leave single-stage policy. The broker notification case checks exact correction notice count and committed source offsets. Producer and consumer are independently real-broker verified; an all-services correction HTTP-to-notice journey remains a separate integration scope. Provision attendance.correction.v1 with its canonical Avro before enabling consumers. No commit/deploy.

### UAT repair — bounded exact-wire recovery and redrive (2026-10-29)

The new shared recovery tests first failed compilation for absent feature APIs (`/tmp/hrflow-uat-wire-recovery-red.log`). All 3 helper cases then passed (`/tmp/hrflow-uat-wire-recovery-green.log`): exact original value bytes, source provenance, privacy allowlist and recovery publication failure propagation. Initial real startup exposed a missing retry scheduler and then an overly specific template bean type (`/tmp/hrflow-uat-recovery-broker-startup.log`, `/tmp/hrflow-uat-recovery-broker-startup-2.log`); both were fixed using the installed Spring Kafka 4.1.1/Boot 4.1.1 APIs. The live poison/retry case passes (`/tmp/hrflow-uat-recovery-broker-poison.log`).

The operator-redrive selection passes all 4 cases (`/tmp/hrflow-uat-recovery-redrive.log`: 3 helpers and 1 actual PostgreSQL/Kafka/Registry notification journey). Invalid Avro is copied byte-for-byte to private DLQ with no credential or private exception text in headers. Unavailable recipient references exhaust delayed retries; actual source offsets advance after recovery publication. Once authoritative role/manager facts are repaired, the real single-record dry-run/redrive utility republishes the same event bytes/ID and the notification converges exactly once. No payload is printed, source offsets reset or DLQ rows deleted. Shared SourceRecords restores original topic/partition/offset when retry containers invoke domain projections.

Unknown/stale broker measurements were added with a separate missing-API gate (`/tmp/hrflow-uat-consumer-lag-red.log`). All 4 selected helper/state cases pass (`/tmp/hrflow-uat-consumer-lag-green.log`), including five-minute sustained backlog alert, recovery and unknown/stale distinction. The subsequent real provider selection passes 7 cases (`/tmp/hrflow-uat-recovery-provider-journeys.log`: 4 shared + 1 actual notification broker/replay + 2 actual packaged owner/reporting journeys), including an actual broker lag sample and request→notice→CSV plus attendance→daily/cutoff→report behavior with recovery enabled. Later admin timeout bounds, notification gauge delegation and unrelated evidence guard/role fixes still require the final combined baseline.

The existing event-starter module now implements this common transport behavior and is inherited by owners. It adds no new runtime service/framework. HRFLOW_EVENTS_RECOVERY_ENABLED remains explicit opt-in until topic provisioning; unconfigured disabled handlers retain failed records instead of silently skipping. Private health/metrics/log alerts are implemented; an external paging destination, real host outage drill and manual deployment are not claimed. No commit/deploy.
