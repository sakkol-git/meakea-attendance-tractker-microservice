# Week 09 partial candidate evidence

Date: 2026-10-28. Owner week: 09 partial candidate; verified roadmap position: W02-S26. This is a partial local candidate for the 2026-12-21 to 2026-12-25 week, not a Week 09 gate result. Root, web and mobile changes are uncommitted in separate repositories. The intended Acer Nitro 5 and encrypted external backup disk are unavailable for the required deploy and restore gate.

## Accepted intent and current behavior

- Reporting projection and idempotency: Kafka fact events for attendance, leave, and requests are projected into dedicated reporting tables with aggregate revision ordering in Testcontainers PostgreSQL (`ProjectFullRevisedKafkaFactsTest`).
- Audit logging and freshness: Audit log queries are isolated by tenant RLS (`app.tenant_id`); projection lag is calculated against event timestamps and verified in Testcontainers PostgreSQL (`ExposeFreshnessAuditSafelyTest`).
- Observable CSV exports: Large exports write to atomic temporary files with atomic rename and cleanup on worker crash in Testcontainers PostgreSQL (`CreateObservableCsvJobTest`, `RendersSummaryEmptyStatesTest`).
- CSV sanitization and download security: Formula injection characters are escaped; revoked or foreign actors are denied download access; retries preserve terminal failed job invariants (`AuthorizeDownloadsPreventFormulasTest`, `VerifiesExportStatusTest`).
- Strict tenant filtering and metrics: Removing caller date filters preserves tenant isolation; unknown sort keys are rejected; Micrometer Prometheus metrics use bounded low-cardinality labels without sensitive IDs (`KeepReportFiltersTenantScopedTest`, `ExposePrivateBusinessAndBrokerMetricsTest`).
- Trace logging and alert recovery: Structured logs correlate requests with technical tenant IDs while scrubbing passwords and PII; threshold alerts fire on high lag and recover cleanly (`TraceSafeHttpAndKafkaOutcomesTest`, `ProveUptimeAndErrorAlertRecoveryTest`).
- Export recovery and empty owner: Tenant readiness gates on checkpoints; interrupted jobs reconcile to terminal FAILED (`BootstrapEmptyReportOwnerTest`, `RecoverInterruptedExportJobTest`).

## Stone 1 verification (W09 Stone 1, W09-S01–S06)

- Steps: `W09-S01` to `W09-S03` (project full revised Kafka facts into read models) and `W09-S04` to `W09-S06` (expose freshness and audit safely with tenant RLS).
- Targets: `services/reporting/src/test/java/com/hrflow/reporting/ProjectFullRevisedKafkaFactsTest.java`, `services/reporting/src/test/java/com/hrflow/reporting/ExposeFreshnessAuditSafelyTest.java`.
- Verified behaviors: higher aggregate revisions update projections; duplicate and older revisions are ignored; foreign tenant audit rows are hidden; freshness calculation avoids falsely flagging idle queues as lag.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/reporting -Dtest="ProjectFullRevisedKafkaFactsTest,ExposeFreshnessAuditSafelyTest"` exited 0 (4 tests, 0 failures, 0 errors, 9.199s).
- Execution limitations: Tests call Java service methods directly against Testcontainers PostgreSQL; they do not test HTTP controllers, JWT role/tenant derivation, live Kafka transport, or consumer offset commits.

## Stone 2 verification (W09 Stone 2, W09-S07–S12)

- Steps: `W09-S07` to `W09-S09` (render summary empty states) and `W09-S10` to `W09-S12` (create observable CSV job with atomic rename).
- Targets: `services/reporting/src/test/java/com/hrflow/reporting/RendersSummaryEmptyStatesTest.java`, `services/reporting/src/test/java/com/hrflow/reporting/CreateObservableCsvJobTest.java`.
- Verified behaviors: zero attendance and pending counts render as valid empty summaries; CSV export writes to temporary file and completes via atomic rename; worker crashes clean up temporary files and transition status to FAILED.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/reporting -Dtest="RendersSummaryEmptyStatesTest,CreateObservableCsvJobTest"` exited 0 (4 tests, 0 failures, 0 errors, 9.914s).
- Execution limitations: Service layer unit execution only; no HTTP routes or gateway routing verified.

## Stone 3 verification (W09 Stone 3, W09-S13–S18)

- Steps: `W09-S13` to `W09-S15` (authorize downloads and prevent spreadsheet formula injection) and `W09-S16` to `W09-S18` (verify export status transitions and terminal retry invariant).
- Targets: `services/reporting/src/test/java/com/hrflow/reporting/AuthorizeDownloadsPreventFormulasTest.java`, `services/reporting/src/test/java/com/hrflow/reporting/VerifiesExportStatusTest.java`.
- Verified behaviors: spreadsheet formulas starting with `=`, `+`, `-`, `@`, `\t`, `\r` are sanitized with single quotes; revoked actors cannot download exports; retrying a failed job creates a distinct new command while preserving original job as terminal FAILED.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/reporting -Dtest="AuthorizeDownloadsPreventFormulasTest,VerifiesExportStatusTest"` exited 0 (4 tests, 0 failures, 0 errors, 8.842s).
- Execution limitations: Service layer unit execution only; HTTP endpoints `/reports/exports` and `/reports/exports/{id}` are not yet wired via Spring Web controllers.

## Stone 4 verification (W09 Stone 4, W09-S19–S24)

- Steps: `W09-S19` to `W09-S21` (keep report filters tenant scoped) and `W09-S22` to `W09-S24` (expose private business and broker metrics with bounded labels).
- Targets: `services/reporting/src/test/java/com/hrflow/reporting/KeepReportFiltersTenantScopedTest.java`, `services/reporting/src/test/java/com/hrflow/reporting/ExposePrivateBusinessAndBrokerMetricsTest.java`.
- Verified behaviors: query with null date filters enforces tenant boundary without data leakage; disallowed or SQL injection sort fields are rejected; Micrometer Prometheus counters and gauges record lag and failures with bounded labels.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/reporting -Dtest="KeepReportFiltersTenantScopedTest,ExposePrivateBusinessAndBrokerMetricsTest"` exited 0 (4 tests, 0 failures, 0 errors, 6.900s).
- Execution limitations: Direct Java service calls; no HTTP controller or OpenAPI contract tests executed.

## Stone 5 verification (W09 Stone 5, W09-S25–S30)

- Steps: `W09-S25` to `W09-S27` (trace safe HTTP and Kafka outcomes with secret scrubbing) and `W09-S28` to `W09-S30` (prove uptime and error alert recovery).
- Targets: `services/reporting/src/test/java/com/hrflow/reporting/TraceSafeHttpAndKafkaOutcomesTest.java`, `services/reporting/src/test/java/com/hrflow/reporting/ProveUptimeAndErrorAlertRecoveryTest.java`.
- Verified behaviors: correlation ID and tenant ID are bound to logs while passwords and secrets are scrubbed to [REDACTED]; missing correlation ID or tenant ID is rejected; alerts fire when lag exceeds threshold and clear on metric recovery.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/reporting -Dtest="TraceSafeHttpAndKafkaOutcomesTest,ProveUptimeAndErrorAlertRecoveryTest"` exited 0 (4 tests, 0 failures, 0 errors, 2.538s).
- Execution limitations: In-process logging and alert utility unit tests; no live Kafka or live HTTP transport exercised.

## Stone 6 verification (W09 Stone 6, W09-S31–S35)

- Steps: `W09-S31` to `W09-S33` (confidence worker integration), `W09-S34` (bootstrap empty report owner gating readiness on checkpoints), and `W09-S35` (recover interrupted export job).
- Targets: `services/reporting/src/test/java/com/hrflow/reporting/ConnectExistingConfidenceWorkerTest.java`, `services/reporting/src/test/java/com/hrflow/reporting/BootstrapEmptyReportOwnerTest.java`, `services/reporting/src/test/java/com/hrflow/reporting/RecoverInterruptedExportJobTest.java`.
- Verified behaviors: `ConfidenceScoreService` delegates scoring to the Python confidence worker (`POST /score`) via HTTP `RestClient` without duplicating the model in Java; durable PostgreSQL inbox deduplication protects against duplicate requests; manual evidence strictly prevents automated inference; tenant readiness gates on checkpoint completion; interrupted jobs reconcile to terminal FAILED on recovery and partial files are never marked READY; Python tests in `ml/confidence-service` pass cleanly.
- Focused verification - Java: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/reporting -Dtest="ConnectExistingConfidenceWorkerTest,BootstrapEmptyReportOwnerTest,RecoverInterruptedExportJobTest"` exited 0 (5 tests, 0 failures, 0 errors).
- Focused verification - Python ML: `PYTHONPATH=ml/confidence-service ml/confidence-service/.venv/bin/pytest ml/confidence-service/tests` exited 0 (12 tests, 0 failures, 1.32s).

## Stone 7 verification (W09 Stone 7, W09-S36–S38)

- Steps: `W09-S36` (reproduce earliest failed check), `W09-S37` (recheck client and contract negatives), and `W09-S38` (review carry-forward and reserve).
- Targets: `contracts/openapi.json`, `docs/validation/W09-EVIDENCE.md`, git diff across all three repositories.
- Verified behaviors: foundation OpenAPI 177 operations and contract checks pass; tooling test suite passes 23 tests; docs-lint passes all Markdown files with zero warnings; git diff checks across root, web, and mobile are clean.
- Diff check: `git diff --check && git -C web diff --check && git -C mobile diff --check` exited 0.
- Contract validation: `python3 scripts/verify-project.py --scope foundation` exited 0 (177 operations, 6 contract tests).
- Tooling verification: `python3 scripts/verify-project.py --scope tooling` exited 0 (23 tests passed in 1.962s).

## Stone 8 assessment & physical blockers (W09 Stone 8, W09-S39–S41)

- Steps: `W09-S39` (redeploy checked service candidate via `ops/deploy.sh`); `W09-S40` (restore release backup in scratch via `ops/restore-scratch.sh`); `W09-S41` (decide gate).
- Physical blocker - deploy: `ops/deploy.sh` requires the dedicated Acer Nitro 5 private host, which remains physically unavailable in this development environment.
- Physical blocker - restore: `ops/restore-scratch.sh` requires the encrypted external backup disk, which remains physically disconnected.
- Gate decision: `BLOCKED`. The Week 09 release gate remains **NOT RUN**. Verified roadmap position remains `W02-S26` in `docs/roadmap/YOU-ARE-HERE.md`. No weekly gate is claimed passed.

## Executed checks

| Check | Result | Limit |
| --- | --- | --- |
| HTTP Controller and Route Security, `ReportingRouteAndKafkaTest` | Exit 0 (13 tests); JWT role/tenant security on `/reports/**`, contract-aligned object DTOs, date range validation, download removal | MockMvc with Testcontainers PostgreSQL |
| Gateway Routing for Reporting, `OrganizationGatewayRoutingIT` | Exit 0 (7 tests); gateway routes `/api/v1/reports/**` with StripPrefix=2 to reporting service | Spring Cloud Gateway WebTestClient |
| Genuine Row-Level Security, `ReportingRowLevelSecurityTest` | Exit 0 (5 tests); non-superuser `NOBYPASSRLS` role `reporting_app` proves same-tenant success, foreign-tenant concealment, missing context fail-closed, and transaction-local `set_config(..., true)` reset after commit on reused connection | Direct JDBC on Testcontainers PostgreSQL 18.6 |
| Migration Immutability & V1-to-V2 Upgrade, `ReportingMigrationVerificationTest` | Exit 0 (2 tests); proves existing V1 database upgrades to V2, preserves existing data with null date ranges, and passes Flyway validation | Testcontainers PostgreSQL 18.6 |
| Kafka Avro Ingestion and Offset Acknowledgment, `ReportingKafkaConsumerTest` | Exit 0 (2 tests); AttendanceCheckedIn processing marked **BLOCKED** because it lacks aggregate revision and session facts (no fabricated 0L or 1L revision); envelope validation throws IllegalArgumentException | Testcontainers PostgreSQL 18.6 (live broker transport **BLOCKED** / **NOT RUN**) |
| Kafka Configuration and Topic Binding, `KafkaConfigurationTest` | Exit 0 (2 tests); effective bound `KafkaProperties` listener has `missing-topics-fatal: true` and consumer default `enabled: false` | Spring Application Context |
| Kafka facts projection and audit freshness, `ProjectFullRevisedKafkaFactsTest`, `ExposeFreshnessAuditSafelyTest` | Exit 0 (4 tests); aggregate revision ordering, tenant RLS isolation; attendance session projection **BLOCKED** (no 480-minute fabrication); leave/request streaming **BLOCKED** due to missing Avro schemas | Testcontainers PostgreSQL 18.6; direct Java calls |
| Summary empty states and observable CSV job, `RendersSummaryEmptyStatesTest`, `CreateObservableCsvJobTest` | Exit 0 (4 tests); empty state labeling, atomic temp file write and rename, no uncontracted downloadUrl | Testcontainers PostgreSQL 18.6; direct Java calls |
| CSV formula sanitization and download security, `AuthorizeDownloadsPreventFormulasTest`, `VerifiesExportStatusTest` | Exit 0 (4 tests); CSV injection neutralization, download security | Testcontainers PostgreSQL 18.6; direct Java calls |
| Tenant report filters and Prometheus metrics, `KeepReportFiltersTenantScopedTest`, `ExposePrivateBusinessAndBrokerMetricsTest` | Exit 0 (4 tests); strict tenant isolation with null filters, sort allowlist | Testcontainers PostgreSQL 18.6; direct Java calls |
| Trace logging and alert recovery, `TraceSafeHttpAndKafkaOutcomesTest`, `ProveUptimeAndErrorAlertRecoveryTest` | Exit 0 (4 tests); correlation ID binding, secret scrubbing | Local in-memory and regex sanitizer |
| Confidence worker adapter and export recovery, `ConnectExistingConfidenceWorkerTest`, `BootstrapEmptyReportOwnerTest`, `RecoverInterruptedExportJobTest` | Exit 0 (6 tests); mocked RestClient adapter tests, full telemetry validation, response tenant/employee/workDate mismatch rejection; executable `processScore` marked **BLOCKED** awaiting accepted durable state model; live Python process gate **NOT RUN** | MockRestServiceServer with Testcontainers PostgreSQL |
| ML confidence service pytest suite, `ml/confidence-service/tests` | Exit 0 (12 tests); launcher and service test suites passing | Python 3.14 venv with FastAPI testclient |
| Reporting service regression suite, `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw --batch-mode -pl services/reporting clean test` | Exit 0 (59 tests, 0 failures, 0 errors, 0 skipped); verified in Testcontainers PostgreSQL 18.6 | Testcontainers PostgreSQL 18.6 |
| Foundation scope, `python3 scripts/verify-project.py --scope foundation` | Exit 0; 177-operation OpenAPI validation, 6 contract tests, docs lint, and diff check passed | Automated local baseline only |
| Tooling scope, `python3 scripts/verify-project.py --scope tooling` | Exit 0; 23 tests passed across MCP server, test runner, and workspace traversal | Local Python virtual environment |

## Boundary review

- Backend reporting owner: `services/reporting` provides `ReportingController` exposing OpenAPI routes (`GET /reports/dashboard`, `GET /reports/attendance/summary`, `POST/GET /reports/exports`, `GET /reports/exports/{id}`) secured by JWT claims, `ReportingMapper` (MapStruct), and `ReportingProjectionConsumer`. AttendanceCheckedIn event processing is marked **BLOCKED** because the event lacks aggregate revision and session facts (no fabricated 0L or 1L revision is stored). The uncontracted `/reports/exports/{id}/download` endpoint is removed from the controller. Production reporting Kafka consumer is disabled by default (`REPORTING_KAFKA_CONSUMER_ENABLED:false`) because required session snapshot event and producer do not exist in accepted contracts; effective bound `missing-topics-fatal` is enabled (`true`). Clean build issues resolved by setting `useIncrementalCompilation=false` in `services/owner-service-parent/pom.xml` and configuring `.vscode/settings.json` to prevent Antigravity / JDT LS file watcher and builders from cleaning/interfering with `target/classes`.
- API Gateway boundary: `services/api-gateway` routes `/api/v1/reports/**` with `StripPrefix=2` to `reporting-service:8106` and sanitizes forged client identity headers.
- ML confidence worker: `ConfidenceScoreService` adapter connects to the Python FastAPI worker (`ml/confidence-service`) via HTTP `RestClient` (`POST /score`) with validated telemetry features and strict response validation (`tenantId`, `employeeId`, `workDate`). Executable `processScore` integration is marked **BLOCKED** awaiting accepted durable state model defining reservation, result persistence, unknown outcomes, retry and reconciliation; no worker is invoked or `applied=true` returned. Live process execution is **NOT RUN** (mocked adapter verified).
- Database & Migrations: Migration immutability preserved: `V1__reporting_core.sql` restored; additive migration `V2__export_job_date_range.sql` added for export date range filter fields. Schema `reporting` strictly protected by `FORCE ROW LEVEL SECURITY` and `app.tenant_id`. Genuine RLS verified using a dedicated non-superuser `NOBYPASSRLS` role `reporting_app` in `ReportingRowLevelSecurityTest`, proving same-tenant access, foreign-tenant concealment/denial, missing context fail-closed, and transaction-local `set_config(..., true)` reset after commit on reused connection.
- Security & PII: Passwords, tokens, and SSNs scrubbed from logs; CSV injection neutralized; revoked actors forbidden from file downloads; uncontracted download link removed.
- Documentation: `docs-lint.py` passes Markdown files with 0 errors and 0 warnings.

## Disposition

`PARTIAL`

Java service, HTTP controller, and repository implementations across reporting pass 59 unit and integration tests in Testcontainers PostgreSQL, and Python ML tests pass. However, attendance session fact projection is **BLOCKED**, Kafka consumer is disabled by default due to missing session producer (**BLOCKED**), confidence worker executable integration is **BLOCKED**, live Kafka broker transport is **BLOCKED** / **NOT RUN**, and physical deployment gates remain blocked. The Week 09 release gate remains **NOT RUN**, and the verified roadmap position remains W02-S26.

## Independent validation after the RPT/TST task batch (2026-10-28)

- Initial clean serialized regression exposed a stale migration assertion: `ReportingMigrationVerificationTest.freshInstallationPreservesExportRows` expected two migrations after additive `V3__audit_log_append_only.sql` was introduced. The assertion and description were updated to cover all three current migrations; no applied migration or production schema was changed.
- Contract authority `docs/reference/CONTRACTS-REFERENCE-08.md` API-104 requires dashboard access to be limited to `HR_ADMIN` tenant or `MANAGER` team. Semantic red tests proved that plain `EMPLOYEE` tokens incorrectly received HTTP 200 both at the gateway and reporting service. The gateway now maps the JWT `roles` claim to `ROLE_` authorities and rejects employees before forwarding; `ReportingController` independently enforces the same roles.
- Focused red: `ReportingRouteAndKafkaTest.dashboardDeniesPlainEmployees` and `OrganizationGatewayRoutingTest.employeeTokenCannotReachReports` both failed because HTTP 200 was returned instead of 403.
- Focused green: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw --batch-mode -pl services/reporting -Dtest=ReportingRouteAndKafkaTest test` and `./mvnw --batch-mode -pl services/api-gateway -Dtest=OrganizationGatewayRoutingTest test` exited 0; the reporting route suite includes the employee denial and HR-admin success cases, and the gateway suite includes employee denial without a downstream call and manager success.
- Clean affected regressions: reporting exited 0 with 93 tests, 0 failures, 0 errors, 0 skipped; API gateway exited 0 with 26 tests, 0 failures, 0 errors, 0 skipped.
- Serialized backend regression before the authorization patch passed identity-org 81, attendance 31, leave 27, request 23, notification 20, reporting 92, and API gateway 25 tests, all with zero failures, errors, or skips. Only reporting and gateway were changed afterward and both were rerun clean with their increased counts above.
- `python3 scripts/docs-lint.py` passed 321 Markdown files with zero errors and zero warnings before this addendum. `git diff --check`, `git -C web diff --check`, and `git -C mobile diff --check` exited 0 after the code changes.
- Remaining disposition: `PARTIAL`. Four provisional shared-contract operations remain without live provider implementation (`GET /reports/attendance/employees/{id}`, `GET /reports/leave/summary`, `GET /reports/requests/summary`, and `GET /audit-logs` plus gateway ownership), and the previously recorded Kafka, confidence-worker, physical deploy, restore, and release gates remain blocked or not run.

## MVP code correctness checkpoint (2026-10-28, subsequent pass)

- Identity now owns the protected `/audit-logs` provider and gateway route; the verified organization audit slice and client role alignment are in W03 evidence. The earlier four-operation gap is therefore reduced by that provider slice, not declared fully closed across all audit sources.
- New semantic reds reproduced manager access to tenant-wide totals (expected 503 while team projection is missing), another HR admin reading a requester's export (expected 404), malformed tenant UUID causing an exception (expected 401), a newly created export returning wire version 0 (expected existing-contract version 1), and optional file fields serialized as null (expected absent).
- Corrections: managers fail closed with `503 REFERENCE_NOT_READY` until current local team scope exists; export creation/status/list require HR_ADMIN and status/list SQL include requester and tenant; malformed identity claims return 401; MapStruct translates Hibernate revision to one-based API version without changing stored revisions or applied migrations; absent optional file/link fields are omitted. The old `ExportJob` minimum version 1 is restored, so compatibility against `origin/main` passes without changing the checker.
- Focused reporting route coverage is now 19 cases. The full local verifier exited 0 with 96 reporting tests, 27 gateway tests, 94 web unit cases and 12 browser cases, together with the other scopes recorded in W03 evidence. No failures/errors/skips in those Java suites. The normal owner-service test lifecycle now inherits the tested Testcontainers transport setting.
- Review: authorization and claims remain explicit Java; structural response conversion remains MapStruct. `ReportingRouteAndKafkaTest` is a cohesive HTTP/security/contract matrix over 300 lines and is a recorded test-fixture exception, not a production service.
- On 2026-10-28 the user explicitly required live event integration before calling MVP ready. Next implementation remains attendance committed state → owner outbox → Avro/Kafka → reporting projection → observable CSV completion. Existing raw `AttendanceCheckedIn` remains unsupported for session facts; no synthetic hours or revisions are introduced. The MVP stays `PARTIAL` until this path and its failures have executed evidence.

## Independent live attendance and private CSV checkpoint — 2026-10-28

The user required live attendance integration before MVP readiness and explicitly approved `GET /reports/exports/{id}/download`. The following is a local implementation candidate, not a Week 09 gate or full MVP certification.

Given authenticated attendance check-in/out requests and explicit organization fixtures, the owner commits full session intents. Real Schema Registry/Avro/Kafka delivery updates the reporting projection. An HR_ADMIN queues an immutable bounded attendance CSV snapshot, the background worker completes it, and the original requester downloads the private bytes with current tenant/role authentication. `SessionReportingJourneyIT` passed this path with separate NOSUPERUSER/NOBYPASSRLS runtime and metadata-only job roles, real JWT signatures, private storage, a wrong-requester 404, removed-role 403 and foreign-tenant empty query. Identity bootstrap, gateway routing in that journey, real devices and deployment are excluded; gateway has its separate routing checks.

`SessionProjectionConsumer` validates envelope/routing and commits receipt, monotonic full snapshot and consumed position in one transaction before acknowledgment. Logical-key locks protect the initial-row case. Open duration stays null; completed duration comes from actual timestamps. State is an effective session state, not an invented final daily attendance policy. The unsupported legacy raw-check-in and POJO listeners remain disabled. Failed typed records are not silently skipped; automatic DLQ recovery remains unfinished.

`ExportQueue` captures one bounded filtered view when the job is created; later events cannot change that export. Defaults cap the input at 10000 rows and permit one running job per tenant. Unsupported leave/request sources and unknown legacy session basis fail visibly. The worker uses a private metadata-only leader connection, tenant-scoped runtime work, atomic file publication and interrupted-job reconciliation. A completed job cannot be rerun through the execution entry point. Cleanup uses only its own generated job ID and cannot delete another completed job's file through a damaged stored pointer.

The additive download contract was synchronized to both independent clients. It returns CSV bytes with `no-store` and a generated attachment name. Tenant/requester mismatch is hidden as 404, unfinished jobs return 409, unavailable/missing/symlink files return 503 and removed HR_ADMIN token authority returns 403. No public storage directory or absolute filesystem path enters the HTTP response. Both clients fetch through their authenticated API transport, retain UTF-8 and reject redirects; mobile also checks workspace generation and avoids export caching. Their remote report screens consume the actual attendance summary instead of assuming incompatible demo/dashboard fields or inventing expected attendance/leave statistics.

Focused verification executed before the broader run:

- `PrivateExportDownloadTest`: red missing-route failures, then two passing authorization/storage cases.
- `SessionProjectionTest`: two passing duplicate/stale/rollback/routing cases; no receipt/effect/checkpoint survives rollback.
- `AutomaticExportTest`: three passing immutable snapshot, row-cap/source availability and unknown legacy basis cases, after the legacy omission regression failed as expected.
- `RecoverInterruptedExportJobTest`: three passing cases, after reproducing deletion of another completed job's file.
- Web actual adapter tests: actual summary shape preserves null duration; authenticated UTF-8 CSV transport passes. The browser fixture journey is separately identified as simulated transport, not real backend evidence.
- Mobile actual remote/provider tests: authenticated UTF-8 CSV and actual summary route/null duration pass. The client fixture does not prove provider authorization; the real server journey supplies that separate evidence.

| Boundary | Result and reason |
| --- | --- |
| Web and mobile | Real attendance summary view and authenticated private CSV transport; demo behavior remains explicitly local. |
| HTTP/bundles | Additive private download; existing wire definitions preserved; both bundles synchronized. |
| Business/security | Correct session basis/source and correction authorization; tenant/requester checks on every download. |
| Database | Attendance forward migrations V3–V10 and reporting V4–V5; existing real-data upgrade/backup/restore acceptance remains unexecuted. |
| Events | New full Avro session snapshot and typed consumer; no GPS/device telemetry. Other domain topic integrations remain separate work. |
| Configuration | Optional workers/consumer default off; private job roles and durable atomic storage are required. No deployment performed. |
| Documentation | Current contract/client/configuration guides updated; historical evidence retained. |

Known remaining MVP/UAT implementation gaps include finalized daily status/absence/leave precedence, manager reporting scope, live leave/request export sources, manual entry without an existing session, and complete broker bootstrap/freshness/DLQ acceptance. The current effective-view-plus-revision-ledger approach must not be advertised as the entire accepted manual-session/daily-status workflow. No whole-roadmap readiness, weekly gate or progress percentage is advanced by these checks.

### Complete local verifier result for this checkpoint

`python3 scripts/verify-project.py --base origin/main` exited 0, running all scopes: foundation, backend, web, mobile and tooling. Full log: `/tmp/hrflow-mvp-current-full.log`. This run used the dirty root `2ad32602d686c8134492aa4d40146562c52a879e`, web `d1db592ae67c472c06b9fe7298c6773ad3bf3f22` and mobile `6066d91d711d7652f49d08edc1acf6ea0c60ae7a` working trees. Subsequent edits were documentation and POM formatting only; documentation, dependency policy and diff checks were rerun separately.

- Foundation: OpenAPI 178 operations validated, client bundles matched, existing wire definitions preserved against `origin/main`, six checker tests passed, demo data/workflow coverage passed, and 321 Markdown files linted without errors or warnings.
- Backend: contracts 3, gateway 27, identity 88, attendance 36, leave 27, request 23, notification 20 and reporting 104 tests passed, plus one Failsafe HTTP/Kafka/private-CSV journey. Total 329 Java tests, zero failures/errors/skips. Disposable PostgreSQL, Kafka and real Schema Registry were used where identified; other individual tests retain their own mock/fixture limits.
- Web: formatting, structure, type/build/bundle checks passed; 96 unit tests in 18 files, ten data checks, two build-configuration checks and 13 browser cases passed. The new remote attendance/CSV browser case uses simulated HTTP transport and is not claimed as an actual browser-to-live-provider test.
- Mobile: analysis passed; 157 tests passed and one existing live test remained skipped. The new provider/UTF-8 transport cases passed. No physical-device or installed-version acceptance was executed.
- Tooling: 23 MCP-tool tests passed. Compose configuration validated without starting the deployed stack. All three repository diff checks passed.

No code was committed, pushed or deployed. Tests establish this local candidate's observed behavior; they do not close the implementation gaps listed above or certify the whole roadmap/MVP.

## UAT repair stone 2a — manager attendance scope (2026-10-28)

Owning requirement: approved reporting architecture's employee dimension and manager-team scope; API-104/105 are historical supporting references, while canonical OpenAPI remains the provisional executable HTTP slice. The initial focused test could not compile because no reporting employee consumer existed. `ManagerReportScopeTest` now passes one PostgreSQL/HTTP case: a manager with a distinct account ID sees only the current direct report; a newer reassignment removes access; duplicate/older snapshots cannot restore it; a foreign tenant without a manager snapshot fails closed. The new employee projection has tenant/employee identity, FORCE RLS and only the manager/active/revision fields needed for authorization. Tenant scope and the direct-report predicate are mandatory before optional date predicates and ordering.

The canonical descriptions were updated and bundles synchronized before code. This is direct full-Avro-record application plus HTTP/database evidence, not live broker/bootstrap evidence. Manager dashboard aggregation and live organization subscription/bootstrap are the next substeps; no complete manager-reporting acceptance is claimed yet. Existing report consumers and installed wire shapes are preserved. No owner-to-owner HTTP/SQL join, dependency or deployment was added.

### UAT repair stone 2b — manager dashboard predicates

The dashboard subcase reproduced the remaining expected 503 for a known manager. The scoped query/aggregation now applies the same tenant and current direct-report predicates to attendance, leave and pending-request counts. The next focused run passed the summary case but exposed a PostgreSQL date-parameter binding error in the dashboard path; the step stays open while that error is repaired. No two-case green result is claimed at this point. Daily-status input completeness remains a separate domain task. No new HTTP response shape or role inheritance was added. Live employee subscription/bootstrap is still required before closing the manager integration stone.

The date-parameter regression was corrected with explicitly typed date null checks. The subsequent focused run `4b7cef1ad3304115` passed both manager summary and dashboard cases (2 passed, 0 failures/errors/skips). Live bootstrap is still a separate test below.

### UAT repair stone 2c — actual organization replay and live revocation

`ManagerReportingKafkaIT` first failed at the expected 503: seeded owner snapshots existed in real Kafka but reporting had no employee subscriber/bootstrap. The corrected focused Maven verify then exited 0 with 2 scope cases and 1 real broker/HTTP case. It used real Schema Registry with auto-registration disabled, seeded employee snapshots before application startup, restricted reporting runtime/RLS credentials and a signed manager JWT. Startup replay made the manager reference available; a session arriving over Kafka appeared in the team report; a later organization reassignment arriving over Kafka removed it.

Manager access now requires a completed replay gate. The live employee listener commits receipt and latest projection before acknowledging. Duplicate and older revisions cannot restore a removed relationship. A shared reporting error handler prevents failed typed records from being silently skipped; full DLQ recovery is still separate work. The focused transactional scope suite uses a mocked ready gate only to isolate authorization predicates; the real IT supplies the independent actual bootstrap evidence. The complete reporting verifier is running next; no result is presumed.

The complete reporting reactor `./mvnw --batch-mode --no-transfer-progress -pl services/reporting -am verify` exited 0 after the replay gate and additional not-ready case were added. Log: `/tmp/hrflow-uat-manager-reporting-verify.log`. It includes all reporting unit/provider cases and both real Kafka IT journeys; the exact final cross-project counts will be recorded at the final verifier. No commit/deploy occurred.

### UAT repair stone 3c — typed leave reporting (2026-10-28)

Given a full owner leave snapshot, reporting must converge from real Kafka without changing a balance. `LeaveReportingKafkaIT` initially failed: approved leave remained zero within 15 seconds (`/tmp/hrflow-uat-leave-report-red.log`). After the transactional typed listener, monotonic projection and additive V7 migration, the same real PostgreSQL/Kafka/Registry test passed (one case, `/tmp/hrflow-uat-leave-report-green.log`), including review retaining the overlay, cancellation, stale/duplicate and foreign-tenant records. Its synthetic producer fixture verifies the reporting boundary; the separate leave-owner command/publication test in W06 verifies the producer boundary, not a single-process end-to-end identity journey.

`run_tests` / `get_test_result` run `2c92cb8884434f1d` passed two additional PostgreSQL cases: receipt/revision/debit preservation and rejected routing/dates plus rollback of receipt, fact and checkpoint. The consumer validates owner fields before tenant access, serializes missing-row creation, commits receipt/effect/checkpoint atomically and acknowledges afterward. Legacy rows retain an explicit unknown basis; V7 does not fabricate actual dates. Manager/tenant authorization and HTTP shape are unchanged; no UI fields changed. The new opt-in `REPORTING_LEAVE_CONSUMER_ENABLED` defaults false and requires the existing reviewed leave subject/topic. No other schema or owner table is read. Broader reporting verification and CSV support remain separate steps.

Complete stone 3c reporting verification exited 0: 109 reporting tests, 3 shared-contract tests and 3 real Kafka integration journeys passed with zero failures/errors/skips (`/tmp/hrflow-uat-leave-report-verify.log`). Contracts sync/check, dependency policy and docs lint (321 files) also passed. The later CSV changes require their own checks.

### UAT repair stone 3d — immutable private leave CSV (2026-10-28)

The new leave CSV test first failed with the existing source-unavailable 503 (`run_id=5fa8cc77e45e4c6e`). The same two cases passed after implementation (`8e81478d908d4ee4`): a date-overlapping request exports owner working dates/charged days/review state from a fixed snapshot despite later cancellation, excludes foreign tenants, and the row cap/unknown legacy basis create no misleading jobs. `ExportRows` isolates the two actual allowlisted source queries from job claiming and filesystem publication; no runtime-selected SQL identifiers, new framework or policy inference was added. The canonical description was updated first and both client bundles synced. Existing API/authentication, atomic private worker, download authorization and database schema are unchanged; clients already support the LEAVE report selector. A combined attendance/leave regression and real Kafka → job → worker → private download test is running; no result is presumed. No commit/deploy.

Stone 3d combined verification exited 0: 5 attendance/leave export cases and the extended real Kafka/Registry leave journey passed (`/tmp/hrflow-uat-leave-csv-verify.log`). That journey now queues an HTTP LEAVE CSV job, waits for the metadata-role worker, downloads actual private bytes as the original HR requester, and rejects another HR requester (404). Other reporting tests were not rerun in this selected command.

### UAT repair stone 4f — owner daily decisions in dashboard (2026-10-28)

The real Kafka/Registry dashboard case first failed with finalized owner absence still counted as zero (`/tmp/hrflow-uat-daily-report-red.log`). With a separately typed daily consumer and V8, the same case passed (`/tmp/hrflow-uat-daily-report-green.log`, one case). The consumer rejects invalid envelope/routing/basis, preserves a stable day identity, applies only newer revisions, and commits receipt/projection/checkpoint before acknowledgment. A separate tenant-RLS daily table prevents treating effective session observations as finalized HR decisions. Structural mapping uses MapStruct and explicit metadata remains Java.

Dashboard attendance now counts one employee/day and selects the actual owner timezone, with finalized ABSENT only and explicit review/provisional summary counts. Manager and tenant filters remain mandatory. Effective session summary/CSV and installed client fields remain unchanged. The canonical dashboard description and client bundles were updated; `REPORTING_DAILY_CONSUMER_ENABLED` is an independent opt-in and the unsupported legacy wire stays off. The manager count fixture now supplies genuine daily snapshots instead of mislabeling a session as PRESENT. The complete reporting reactor is running, including new monotonic/rollback/identity/timezone cases; no broad result is presumed. This transport fixture does not yet prove the no-tap cutoff scheduler or whole-owner-to-dashboard journey. No commit/deploy.

Complete stone 4f reporting verification exited 0: 114 reporting and 3 shared-contract tests plus 4 real Kafka integration journeys passed, zero failures/errors/skips (`/tmp/hrflow-uat-daily-report-verify.log`). The selected current-team/day query, rejection/rollback/identity cases and owner-timezone midnight boundary ran. No no-tap finalizer or whole-MVP acceptance is inferred.

### UAT repair stone 4j — whole attendance cutoff to reporting journey (2026-10-28)

The extended `SessionReportingJourneyIT` passed against the newly packaged attendance service (`/tmp/hrflow-uat-cutoff-reporting-journey.log`, one real integration case). An actual attendance child process handles signed HTTP check-in/out and runs the private-role cutoff scheduler/subscriber; reporting consumes the separately registered daily Avro. A second synthetic employee with no tap and a verified already-closed assigned window converges to exactly one finalized dashboard absence with zero raw evidence. The first employee’s real session summary and automatic authenticated private CSV still pass, including foreign requester/employee download denial and foreign tenant isolation. The fixture chooses a valid timezone/window closed at execution time instead of depending on the developer’s wall-clock hour. Organization owner snapshots are seeded directly, not through a full identity command/event journey; that evidence limit remains. This selected command reran one integration journey, not every test/client. No commit/deploy.


### UAT repair — request report basis and private CSV (2026-10-29)

The new request CSV/count cases first reproduced unsupported request exports and an actual owner PENDING_APPROVAL count of zero (`/tmp/hrflow-uat-request-csv-red.log`). With forward reporting V9 and original owner creation-time retention, the 3 selected cases passed (`/tmp/hrflow-uat-request-csv-green.log`). The expanded source/automatic-export suite then passed 7 cases (`/tmp/hrflow-uat-request-csv-source-test.log`): immutable queued revision, original creation date, actual pending state, private requester access, unknown legacy creation basis fails rather than guessing from updated_at, and incomplete startup replay cannot produce a seemingly empty successful request CSV. Request date bounds use explicitly documented UTC creation dates, with exact createdAt in the CSV; no leave/workday or employee-local date is inferred.

The typed RequestWorkflowFact consumer is opt-in and validates accepted topic/key/identity/revision/creation/step facts. Tenant-scoped receipt, monotonic full projection and checkpoint share one transaction, with acknowledgment afterward. Shared protocol validation and structural MapStruct mapping are used; original immutable request identity cannot change. A captured-position retained-topic bootstrap gates request export availability. Its restricted-runtime projection suite initially found a native timestamp represented as Instant rather than OffsetDateTime (`/tmp/hrflow-uat-request-projection-test.log`); the conversion now explicitly accepts the inspected JDBC representations and rejects unknown types. The suite is rerunning. Actual Kafka/private HTTP CSV integration remains a separate required gate; fixtures alone do not establish it. No commit/deploy.

The restricted typed projection suite now passes both cases (`/tmp/hrflow-uat-request-projection-green.log`): complete latest owner revision, duplicate/stale idempotence, monotonic checkpoint, forbidden immutable identity changes, invalid routing, transaction rollback and unscoped RLS invisibility. The first packaged-owner journey returned 503 when it submitted immediately after the TCP/HTTP port opened, before the owner’s required bootstrap finished (`/tmp/hrflow-uat-request-reporting-journey.log`). The fixture now waits for the configured private readiness probe (which completes after startup replay), rather than treating an unauthenticated 401 as readiness. Its rerun is in progress; the 503 correctly preserved the unready business boundary.

The readiness rerun found a test-process configuration error: a management-specific bind address cannot share the main application port (`/tmp/hrflow-uat-request-reporting-journey-ready.log`). The fixture now allocates a separate private management port, preserving the actual service configuration, and waits on that readiness endpoint. No runtime binding or authorization was relaxed. The corrected private-readiness journey is running.

The corrected packaged-owner journey passes (`/tmp/hrflow-uat-request-reporting-journey-private-ready.log`, 1 Failsafe integration case). Signed synthetic HTTP commands drive the actual packaged request service under request_runtime RLS, server-resolved manager/HR steps, automatic acknowledged request.workflow.v1 relay, actual Kafka/Registry, reporting_runtime RLS projection, HTTP REQUESTS export creation, background worker and authenticated private CSV bytes. HR cannot bypass the manager stage; unrelated managers cannot read detail. The completed CSV contains original createdAt and the final owner state without private text. Another HR requester receives 404; employee download receives 403. Only the selected journey ran (unit suites were intentionally excluded); the broader reporting reactor is now running. No commit/deploy.

The first full reporting run executed 120 unit/owner cases and found one stale migration-count assertion: fresh-install expected 8 after adding forward V9 (`/tmp/hrflow-uat-reporting-requests-full.log`). The expected current migration count is now 9; upgrade/row preservation assertions are retained and no applied migration changed. The full reporting verifier is rerunning, including all real Failsafe journeys.

The complete current reporting reactor now passes 120 unit/owner cases, 3 shared contract cases and all 5 real Failsafe broker journeys, zero failures/errors/skips (`/tmp/hrflow-uat-reporting-requests-final.log`). This includes the new packaged request/private CSV path and all previous attendance/daily/leave/manager journeys. Canonical validation covers 178 operations, both client bundles match, target-branch compatibility preserves existing wire definitions, all three repository whitespace checks pass, and full documentation lint passes 321 files with zero errors/warnings. The final all-project verifier is still required after subsequent MVP fixes. No commit/deploy.

### Combined UAT backend/client-provider baseline (2026-10-29)

The complete clean root Maven verifier passes after the notification, correction, leave-feedback and schedule-fanout changes (`/tmp/hrflow-uat-backend-combined-baseline.log`, finished 13:39:30 Asia/Phnom_Penh, 7m28s). Command: dynamically selected TESTCONTAINERS_HOST_OVERRIDE plus HRFLOW_VERIFY_CLIENT_PROVIDERS=true ./mvnw --batch-mode --no-transfer-progress clean verify. All modules succeeded, including the opt-in real web/native direct-provider harness and the combined request→notification→report/private CSV journey. This establishes a combined source baseline before further recovery changes, not final MVP acceptance. Runtime/manual ingress, host operations and remaining recovery gaps are not relabelled as passing.

The current independent static checks also pass: dependency policy, canonical/client bundle drift, and full documentation lint (321 Markdown files, zero errors/warnings). No commit/deploy.

### Complete verifier result and remaining repair (2026-10-29)

The complete invocation TESTCONTAINERS_HOST_OVERRIDE=(current en0 address) .venv-checks/bin/python scripts/verify-project.py --base origin/main executed foundation/backend/web/mobile/tooling (`/tmp/hrflow-uat-final-all-scopes.log`). It correctly exited nonzero for one web formatting failure: tests/e2e/remote.spec.ts. Web check stopped at Prettier, so its later type/unit/build stages did not run in that invocation. This failure is assigned to the existing client-owned chat for a narrow formatting repair and complete npm run check rerun; it is not downgraded or hidden.

Other completed scopes: canonical validation/drift/compatibility and documentation checks; backend 484 cases including all 6 Failsafe provider journeys, zero failures/errors/skips; all 16 web browser cases; Flutter analysis and 188 passing cases with 3 explicit opt-in skips (the live-provider two-case native suite ran separately inside the required real provider harness); tooling 23 cases. Both client source manifests matched their 166 web and 244 mobile file hashes at inspection. The packaged private redrive entry point also resolves and rejects missing bounded selection before broker access. A final additional past-date regression was subsequently reproduced and is being repaired/verified in the attendance owner; do not treat the earlier artifact as containing that later fix. No commit/deploy.

### Final review boundary assessment

| Boundary | Current implementation and verification boundary |
| --- | --- |
| Web and mobile | Client-owned adapters retain server ETags, command keys, unknown outcomes and authenticated private transfers; no remote demo fallback. The final web formatting-only repair passes npm run check, including 128 unit tests/type checks/demo build/bundle budgets (`/tmp/hrflow-client-final-format-check.log`). The browser baseline is 16 passing cases; native analysis/188 cases and the separate two-case actual native provider harness are distinguished. |
| HTTP and installed clients | The current canonical additive private download/correction-read/general-form slices and generated bundles remain synchronized. origin/main compatibility preserves existing definitions. The final recovery/calendar changes add no client-required field. Unsupported evidence-required leave is rejected without mutation, following Week 06's conditional attachment boundary. |
| Domain and authorization | User-approved timing, half portions, unpaid accounting, per-year requests, independent approvals and no automatic overlap refund remain binding. General requests use the server manager→HR chain; legacy unrelated role names cannot grant private access. Request handover is MATERIAL-only. |
| Data and existing rows | Forward owner migrations retain legacy rows and null/unknown provenance. Missing historical schedule evidence is not backfilled from today's shift. Existing dated history remains authoritative; verified unchanged reference provenance supports missed-day catch-up. No applied migration or historical evidence is rewritten. |
| Events | Actual typed owner outbox/Registry/Kafka/inbox paths, per-portion overlap, correction lifecycle and private recipient resolution have executable evidence. Daily optional-field and organization-role evolution preserve original readers/writers; legacy unknown fields never grant authority or prove overlap. |
| Failure and recovery | Commit/receipt/order guards retain retryable work; exact-wire delayed retry/DLQ is explicitly enabled after provisioning. Recovery sends require acknowledgment. Operator redrive retains original event ID and DLQ record; source/retry lag comes from broker offsets and unknown stays visible. |
| Configuration and operating cost | The existing seven-service/PostgreSQL/Kafka stack is retained. Shared Spring Kafka recovery uses the existing module; no new broker/cloud/paid service is introduced. Flags and private job roles/schema registration remain explicit manual setup requirements. No running application or user database was deployed or modified by test fixtures. |
| Remaining manual scope | Real gateway/cookie/device end-to-end UAT, host configuration, backup/restore and release are outside this code-only completion instruction and remain unclaimed. Optional provider push/email/SSE and disabled secure leave attachments are not presented as implemented. Historical weekly gates/percentages remain unchanged. |

All repositories remain dirty and uncommitted by instruction. Final receipt follows after the complete attendance rerun; earlier failing invocations remain visible above.

### Supported UAT code handoff — 2026-10-29

The code-validation pass is complete for the supported UAT slice. The full all-scopes invocation's web formatting failure was repaired by the client owner; the complete web check passes 128 units, type/data/configuration checks, build and bundle budgets. The later complete attendance verifier passes all 77 cases after the verified historical-calendar/catch-up fix. The final rebuilt-provider selection passes both attendance/reporting and real web/native-provider journeys (`/tmp/hrflow-uat-rebuilt-provider-final.log`, 2 Failsafe cases, zero failures/errors/skips). All failures and reruns are preserved above rather than rewriting the original full command's exit status.

The combined tested case set is 486 backend cases (including all 6 real reporting/provider journeys), 128 web unit cases, 16 web browser cases and 188 mobile cases. Mobile has 3 intentional opt-in skips in its standalone suite; the actual two-case web and two-case native provider suites ran inside the required provider journey without skips. Contracts cover 181 operations with preserved origin/main definitions and matching client bundles. Foundation tests (6), tooling tests (23), docs lint (321 files), dependency policy, script syntax, packaged redrive entry and all three repository whitespace checks pass. Mock/demo browser coverage remains distinct from the actual provider tests.

Current source and verification receipt CURRENT-UAT-CODE.json binds 756 backend source/configuration files, exact root/client revisions, client source manifests and hashed local check logs. Root remains 2ad32602d686c8134492aa4d40146562c52a879e, web d1db592ae67c472c06b9fe7298c6773ad3bf3f22 and mobile 6066d91d711d7652f49d08edc1acf6ea0c60ae7a, all with uncommitted changes. Do not treat these Git revisions alone as the changed implementation: use the working-copy receipt.

Supporting leave files remain disabled until their secure path is complete, as Week 06 permits; evidence-required types fail closed. Optional provider push/email/SSE and broader provisional flows are not claimed. Actual server/gateway/cookie/device UAT, release and backup/restore remain manual and outside this code-only instruction. Historical weekly gates and percentages are unchanged. No commit, push or deployment.

The first handoff documentation check rejected Markdown links to the JSON receipt under the repository's Markdown-only relative-link rule (`/tmp/hrflow-uat-handoff-foundation.log`). The receipt references are now plain paths, retaining the rule; the final foundation check is rerunning. This documentation formatting repair changes no source/test/artifact behavior.

Final handoff foundation passes (`/tmp/hrflow-uat-handoff-foundation-final.log`), including the repository's full documentation lint and origin/main compatibility. The receipt source hashes were verified again after all tests; all three repository whitespace checks remain green. The current tested set has no remaining failing check in the supported code slice. The preserved full-command formatting failure is resolved through the documented complete web rerun and subsequent owner/provider/foundation checks; it is not relabelled as a successful original invocation.
