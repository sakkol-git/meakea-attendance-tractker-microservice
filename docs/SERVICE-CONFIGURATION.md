# Service configuration baseline

## Scope and entry points

The seven Java service roots now inherit the root Maven/Boot version, use valid YAML and select Spring Data JPA for business services plus the reactive gateway (ADR-009). The six owner services share `services/owner-service-parent/pom.xml`; the gateway keeps its reactive-only dependency boundary. See the [Maven dependency baseline](development/MAVEN-DEPENDENCIES.md). Each service and the Python confidence prototype has a tracked .env.example and a local, empty .env. Blank files are deliberately ignored, so a fresh clone must create them locally. No application secrets, login flow, tenant policies, business migrations, Kafka handlers or production deployment are supplied by this baseline.

[Architecture](Architecture.md), [ADR 008](adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md), [ADR 010](adr/010-DUAL-JWT-VALIDATION.md) and [engineering standards](roadmap/ENGINEERING-STANDARDS.md) define the target. The following settings are implementation configuration, not completion of all Q-items. Historical audits retain their older findings; inspect current source before repeating a roadmap repair step.

## Service ports and environment files

| Service root | HTTP default | Management default | Owner schema | Files at service root |
| --- | --- | --- | --- | --- |
| services/api-gateway | 8080 | 9080 | None | .env and .env.example |
| services/identity-org | 8101 | 9101 | identity_org | .env and .env.example |
| services/attendance | 8102 | 9102 | attendance | .env and .env.example |
| services/leave | 8103 | 9103 | leave | .env and .env.example |
| services/request | 8104 | 9104 | request | .env and .env.example |
| services/notification | 8105 | 9105 | notification | .env and .env.example |
| services/reporting | 8106 | 9106 | reporting | .env and .env.example |
| ml/confidence-service | 8107 | Existing /health | Not a database owner | .env and .env.example |

All default host bindings are loopback. Ports avoid the Schema Registry listener on 8081. The default Java readiness group includes the database for business services; liveness does not depend on PostgreSQL/Kafka. Kafka delivery/consumer readiness still needs actual worker implementation and failure tests. A shallow healthy probe does not prove an end-to-end event flow.

### Week 3 organization relay candidate

Identity-org migration V10 adds a UUID-only tenant registry in the private `identity_jobs` schema. The application HTTP runtime role must have no grant there. `ops/provision-db-roles.sh` creates a separate optional job login with `SELECT` on that schema; an enabled relay requires `HRFLOW_DB_JOB_USER` and `HRFLOW_DB_JOB_PASSWORD`. `HRFLOW_ORG_RELAY_ENABLED` defaults to false. Before enabling it on the intended host, create the compacted topics with `infra/scripts/create-topic.sh` and register the reviewed Avro envelope under each `org.*.v1-value` subject with `infra/scripts/register-organization-schema.sh`. The latter uses Schema Registry's compatibility policy and fails on a rejected schema. Both scripts require the private Kafka/Registry containers to be running. The registration helper passed against the local dev Registry, but neither script has been run on the Acer. The relay holds a dedicated PostgreSQL advisory-lock connection, then reads personal outbox rows in tenant-scoped transactions and waits for a Kafka acknowledgment before marking a row sent. Local tests include a real Kafka broker with a mock Registry subject and manual attendance bootstrap; deployed Registry compatibility and consumer/readiness gates remain open. [ADR 012](adr/012-ORGANIZATION-EVENT-V1.md) owns the provisional wire.

The owner now builds typed JSON snapshots in the mutation transaction, stores them in the outbox and converts them to the accepted Avro envelope only at publication. Exact numeric checks prevent fractional or overflowing shift integers from reaching Kafka. The relay keeps its ADR 008 single-leader advisory lock and logs failed sends with their causes; a failed send leaves the row pending for retry.

## Java environment loading and local startup

Spring imports the adjacent .env as Java properties, with a second service-specific path allowing invocation from the repository root. Run from the service directory or repository root, not an arbitrary working directory. Process environment and command-line arguments override imported properties. Service-specific imports override an optional root .env when both exist; avoid a root credential file that accidentally supplies another service's credentials.

Use plain KEY=value lines with comments on separate lines. Do not add export statements, shell quotes or inline comments; values are not evaluated as shell commands. Java-properties backslash escaping applies. Use exported process variables for values that would be ambiguous in a properties file. No new dotenv dependency is needed for Java.

From the repository root, prepare one service without overwriting an existing nonempty environment file:

```bash
cd services/identity-org
test -s .env || cp .env.example .env
chmod 600 .env
```

Fill DB_PASSWORD and FLYWAY_PASSWORD locally, and set usernames/URL to roles actually provisioned for that owner. The examples contain no password and do not provision users. Runtime roles must be non-owner and non-BYPASSRLS. Migration roles own only their service's schema; do not use a PostgreSQL superuser as an application login. Existing dependency Compose credentials/schema creation are not a complete role/grant/RLS setup and are not changed here.

After Java 21 and the actual database/schema/roles are ready, run from the service directory:

```bash
mvn spring-boot:run
```

An empty .env is a placeholder, not a runnable database configuration. Unresolved required credentials or inaccessible dependencies must fail startup rather than use hard-coded credentials. Keep Flyway's own history in the owner schema. There are no business migration files yet; create/review them through the roadmap before accepting database readiness.

## Java runtime and security defaults

- Application and management listeners bind separately to 127.0.0.1. Health is the only exposed management endpoint; details/components are hidden. Check the management port, not the business port, for Actuator probes.
- Security allows health probes only. Anonymous requests to other paths return 401 and authenticated requests remain denied with 403. Generated development users/passwords, browser login and HTTP Basic are disabled. No gateway forwarding routes exist yet. This is closed scaffolding, not implemented JWT or method/resource authorization.
- CSRF remains enabled; state is not persisted in server sessions. Secure/HttpOnly/SameSite cookie defaults are configured for servlet services. Do not disable CSRF or Secure cookies to get a later login demo working.
- Forwarded identity headers are never trusted. The gateway must strip them, validate the bearer JWT and forward the original `Authorization` credential; each destination service must validate that JWT independently before deriving tenant, employee or roles. Keep service/management ports private as defense in depth.
- Shutdown allows twenty seconds per phase. Logs use Boot's JSON format. Stack traces, exception names, validation internals and exception messages are excluded from default HTTP errors; framework problem-details support is enabled. Stable business error codes and correlation/tenant context still need implementation and tests.
- Connection pools default to five maximum connections with bounded acquisition, connection and socket waits; connection timezone is UTC. Each pool selects its own schema, which is not tenant isolation by itself. Spring Data JPA (Hibernate) is configured with ddl-auto: validate and open-in-view: false per ADR-009.
- Flyway validation is enabled; clean, schema auto-creation and automatic baselining are disabled. Separate credentials are required for migrations. No applied migration is edited by this change.
- Kafka uses Avro, producer idempotence/all acknowledgments, manual consumer acknowledgment, read-committed isolation, bounded batches and explicit schema/topic registration. The local endpoints use private development transport; broker TLS/SASL and full inbox/outbox guarantees are not implied by these properties.
- The gateway uses the accepted reactive starter, a bounded codec buffer and explicit connection/response timeouts. No automatic retry of mutating HTTP requests is introduced.
- All seven services include the Prometheus registry, but only health is exposed by the current management configuration. Scrape exposure, private routing, bounded labels and dashboards remain Week 9 work.
- The resource-server libraries are present on the gateway and owner services. Current deny-by-default chains are still scaffolding; dependency presence does not establish either ADR-010 validation gate, bearer forwarding, tenant provenance or business authorization.

For containers, set SERVER_ADDRESS to 0.0.0.0 inside the container namespace and configure an appropriate private management listener. Use loopback/private host bindings, network policy and the target trust model; changing the bind address alone does not secure a deployment. No container deployment is performed by this setup.

## Browser authentication at the edge

`HRFLOW_AUTH_REFRESH_COOKIE_PATH` must match the auth prefix visible to the browser. It defaults to `/auth` for direct identity-service calls. Both Compose gateway paths set it to `/api/v1/auth`, matching the public routes used by the web client. Login, refresh and logout must create and clear the refresh cookie with the same configured path; changing it makes existing cookies unreachable and requires users to sign in again once.

The gateway ignores `X-Forwarded-For` unless its connection peer is listed in `HRFLOW_GATEWAY_TRUSTED_PROXIES`. The self-host topology assigns Caddy `172.31.250.2` on the private `hrflow-edge` network and trusts only that address. Caddy replaces untrusted incoming forwarded-address values before proxying. Direct callers and other edge containers therefore cannot select the source address used by login throttling. Changing the pinned edge subnet or Caddy address requires recreating the `hrflow-edge` network and updating the trusted-proxy setting together.

Local account and signing-key bootstrap is disabled by default. Development Compose enables it explicitly; self-host Compose keeps it disabled and mounts operator-managed signing keys. Generated `.local-keys` directories are ignored so private signing material cannot be staged by a broad Git add.

## Python confidence service

From ml/confidence-service, install requirements into its virtual environment and launch:

```bash
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python run.py
```

The launcher reads the adjacent .env using python-dotenv without overriding process variables. The example contains the exact consumed CONFIDENCE_HOST/PORT/LOG_LEVEL/WORKERS/MAX_CONCURRENCY/KEEP_ALIVE_SECONDS/SHUTDOWN_SECONDS keys. It defaults to private binding, one worker, bounded concurrency/timeouts, no access logs, no server header and no forwarded-header trust. Invalid ports/resource limits are rejected. Calling uvicorn directly bypasses this launcher configuration.

The Docker command uses the same launcher, binding to 0.0.0.0 inside its namespace; .dockerignore excludes local environments, caches and the virtual environment from build context. The existing model artifact path and scoring rules are unchanged. This prototype has no authentication or tenant isolation; keep it private, use synthetic data and follow the approved integration work before any real HR use.

## Verification and remaining work

Run from the repository root with a compatible JDK selected:

```bash
python3 scripts/check-maven-dependencies.py
mvn -o verify
python3 docs/roadmap/tools/docs-lint.py docs README.md
```

The Java tests exercise each service's actual security chain and YAML/property import, including command-line override precedence; they do not connect to PostgreSQL/Kafka. From ml/confidence-service, run:

```bash
.venv/bin/python -m pytest -q -p no:cacheprovider
```

Python tests include the existing scoring checks and launcher defaults, invalid ports and process-over-file precedence. Offline Maven requires the necessary dependencies to be cached. Missing cache/network/container prerequisites are not passing integration tests.

Java 21 is the target; the setup check uses the existing JDK 23 with compiler release 21 because Java 21 is not installed. That does not substitute for the later Java-21 runtime/CI gate. Actual database roles/grants/RLS, migrations, Kafka/Registry integration, JWT/cookie flows, correlation IDs, stable errors, vulnerability scans, deployment and restore remain required roadmap work. Do not mark a whole roadmap step Run from this configuration-only change.

## Recorded setup checks

### Attendance session and private CSV candidate (2026-10-28)

The optional live path is attendance owner → `attendance.session.v1` → reporting session projection → immutable CSV queue → private authenticated download. Install the attendance V3–V10 and reporting V4–V5 forward migrations, create the existing session topic, and register its reviewed value subject using `bash infra/scripts/register-attendance-schema.sh` before enabling publication. Schema Registry compatibility must remain BACKWARD or stricter, and `auto.register.schemas` remains false. The registration helper is operator tooling; code verification does not execute it against a deployed environment.

Use separate schema-owning migration, non-owner/NOBYPASSRLS runtime and metadata-only job roles for both services. Precreate `attendance_jobs` and `reporting_jobs` and grant each owning migrator USAGE/CREATE on its private job schema. Grant runtime roles the required DML on their own service tables, plus sequence usage for attendance's outbox position, with no access to either private job schema. Grant each job role only USAGE on its own job schema and SELECT on its `tenant_registry`, including the migrator's matching default privileges. Job roles must not read business payload tables or receive BYPASSRLS. Each worker uses its dedicated job connection only for a leadership lock and UUID tenant discovery; payload transactions use the scoped runtime connection.

Set `HRFLOW_ATTENDANCE_JOB_USER/PASSWORD` and enable `HRFLOW_ATTENDANCE_RELAY_ENABLED`. Set `REPORTING_JOB_USER/PASSWORD`, `REPORTING_SESSION_CONSUMER_ENABLED=true` and `REPORTING_EXPORT_WORKER_ENABLED=true`; keep the unsupported legacy `REPORTING_KAFKA_CONSUMER_ENABLED=false`. Both workers default off and reject missing private job credentials when enabled. Each polls bounded work and retries pending publication after a missing broker acknowledgment. Failed consumer records remain unacknowledged for repair/replay; automated DLQ recovery is not yet installed.

Configure `REPORTING_EXPORT_STORAGE_DIR` to a private durable directory on a filesystem supporting atomic rename; `REPORTING_EXPORT_ROW_CAP` defaults to 10000. The worker captures bounded input when the HTTP job is created and permits one running job per tenant. Open sessions have no completed duration. A new leader fails interrupted running jobs before processing another queued job. Failed jobs require a new explicit request. Completed CSV files are served only by `GET /reports/exports/{id}/download` with the original requester, tenant and HR_ADMIN token checked on each request; no public directory or filesystem path is exposed. Direct file navigation does not carry bearer authentication, so both clients use their authenticated API transport.

This path reports effective session snapshots, not finalized daily absence/holiday/leave decisions. The verified LEAVE CSV source is available after typed leave convergence; the REQUESTS source requires typed request startup replay and a verified original creation basis. A session without its real schedule basis must not be relabeled or backfilled with guessed values. The self-host release topology and production role provisioning remain separate manual release work.

On 2026-10-23, the ten-project offline Maven verify completed successfully: 28 Java tests passed and seven executable service jars were packaged. The existing JDK 23 compiled with release 21; Mockito emitted dynamic-agent warnings. The Python suite passed 12 tests with one pre-existing Starlette/httpx deprecation warning. Live temporary-loopback checks passed for the packaged gateway (liveness/readiness 200, protected paths 401) and Python launcher (health 200, server header absent). Both test processes were stopped. All eight local .env files remained zero bytes, ignored, with examples trackable.

The structured receipt is docs/validation/SERVICE-CONFIGURATION-RESULTS.json. These checks establish the configuration baseline only; no live PostgreSQL/Kafka, business authentication, production deployment or restore was executed.

### UAT manager-report employee replay (2026-10-28)

After reporting V6, enable `REPORTING_EMPLOYEE_CONSUMER_ENABLED=true` only with the existing `org.employee.v1` topic and reviewed organization Avro subject available. `REPORTING_EMPLOYEE_BOOTSTRAP_TIMEOUT_MS` defaults to 60000 (allowed 1000–600000). Startup replays the compacted owner snapshots to captured broker positions before manager access becomes available; missing topics, invalid snapshots or timeout fail startup. The live listener commits projection and inbox before acknowledgment. Reassignment/termination applies only newer owner revisions and removes direct-report access; duplicate/stale snapshots cannot restore it. The projection stores only IDs, active state and revision, with FORCE RLS.

Managers receive `REFERENCE_NOT_READY` while the employee consumer/replay is unavailable. Runtime roles need the same own-schema DML defaults for V6 as other reporting tables, and no access to another service schema. HR_ADMIN tenant scope remains explicit. Live current-team behavior is verified separately from the remaining finalized daily-status inputs; a successful relationship replay is not proof that the daily attendance/leave/request reports are complete.

### UAT leave snapshot relay (2026-10-28)

The leave V2 forward migration adds typed, ordered intents without relabeling old wire-version-0 rows. Precreate `leave_jobs` for the owning migrator, give it USAGE/CREATE there, and give the separate leave runtime role its own-schema DML plus sequence usage/default privileges for the new stream position. Give the separate NOSUPERUSER/NOBYPASSRLS leave job role only USAGE on `leave_jobs` and SELECT on `leave_jobs.tenant_registry` (and matching migrator defaults), never leave business payload access. These are the same private metadata/runtime boundaries as the other owner relays.

Register the reviewed `LeaveRequestFact` schema for `leave.request.v1-value` with actual Registry compatibility checks before enabling the relay; producer auto-registration stays false. Set `HRFLOW_LEAVE_JOB_USER/PASSWORD` and then `HRFLOW_LEAVE_RELAY_ENABLED=true`. The optional relay defaults off, rejects missing job credentials when enabled, serializes employee-stream insertion, waits for broker acknowledgment before publishing the status, and leaves failed sends pending. All fields come from the managed owner snapshot and actual expanded dates; names, reasons and medical details are excluded. Downstream attendance/reporting subscribers and final calendar policy acceptance are separate steps.

After reporting V7, `REPORTING_LEAVE_CONSUMER_ENABLED=true` enables the typed `leave.request.v1` listener. Apply own-schema DML defaults for the new columns, register the reviewed subject and retain manual acknowledgment. Receipt, newest full snapshot and checkpoint commit together; review retains the approved overlay and actual charged days. This is independent of the unsupported legacy POJO consumer switch, which stays off. LEAVE CSV captures immutable owner-verified snapshots, with interval-overlap filtering, exact charged days and actual working dates. Rows with unknown legacy basis fail explicitly. Attendance overlays require their separately verified implementation.

Attendance V11 stores the newest full leave reference with tenant RLS and a transactional receipt. Enable `HRFLOW_ATTENDANCE_LEAVE_CONSUMER_ENABLED=true` only after the reviewed `leave.request.v1` subject/topic and owner publisher are available. Runtime roles need own-schema DML defaults for the two new tables; no cross-schema grant is required. Owner working dates, rather than the outer date interval, determine leave overlay. Work on approved/review leave retains evidence and raises review without changing a debit. Bootstrap freshness, daily finalization and owner conflict-resolution propagation remain separate gates.

Attendance V12 captures the real assigned weekday mask for new effective session schedules. Daily overview uses captured historical schedule inputs and returns an explicit unavailable result for an unknown/disagreeing basis, without backfilling guessed calendars. The accepted UAT timing is original shift end + 30 minutes, ten-minute early allowance, and equal chronological halves for MORNING/AFTERNOON leave. No operator environment setting silently overrides this HR decision. The overview separates calendar type, review flag, expected window and raw completed duration; open sessions have unknown total and remain nonfinal. Durable daily event/finalization workers remain separately verified work.

The staged daily materialization/publisher switch is `HRFLOW_ATTENDANCE_DAILY_STATUS_ENABLED=false` by default. When its full integration gate is accepted, enable it with the attendance relay and reviewed `attendance.daily-status.v1-value` subject. `infra/scripts/register-attendance-schema.sh` registers the separate session and daily schemas; existing session consumers are unchanged. V13 adds tenant-RLS stable day revision/snapshot and recalculation rows. Owner commands commit an immutable daily intent with their session command; no-op/replayed commands do not inflate revisions. Unknown old calendars retain repair work rather than blocking taps. Daily consumer, cutoff scheduler and external-view convergence are still separate prerequisites for UAT acceptance.

Reporting V8 stores newest daily decisions separately from session observations. Enable `REPORTING_DAILY_CONSUMER_ENABLED=true` with the reviewed `attendance.daily-status.v1` topic/subject and owner publisher available. Own-schema DML defaults apply; no attendance-schema grant is needed. Daily receipt/effect/checkpoint commit before acknowledgment. Dashboard attendance counts one employee/day in the actual owner timezone, with explicit provisional/review metadata and no provisional absence. The effective-session summary and ATTENDANCE CSV keep their existing installed-client meaning. Startup convergence, actual no-tap cutoff and owner-to-dashboard integration remain separate acceptance gates.

V14 daily recalculation uses the existing metadata-only attendance job credentials, a separate leadership lock and employee-scoped runtime transactions. `HRFLOW_ATTENDANCE_DAILY_WORKER_ENABLED` defaults false and requires daily materialization enabled. Leave events queue the union of old and new owner dates; unchanged snapshots add no revision, unknown calendars stay retryable with a 30-second delay. Approved session date moves refresh both days locally. Neither path changes leave debit. Employee projection changes register UUID-only tenant metadata so employees with no taps can later receive cutoff work. Cutoff and organization-change fanout remain independently verified prerequisites.

The cutoff publisher and consumer are independent opt-ins: `HRFLOW_ATTENDANCE_CUTOFF_ENABLED` and `HRFLOW_ATTENDANCE_CUTOFF_CONSUMER_ENABLED` default false. Enable them only with daily materialization, the private attendance job role, existing attendance relay, and a precreated `time.shift-cutoff.v1` topic plus reviewed `ShiftCutoffFact` subject. V15 stores tenant-scoped durable date cursors and cutoff receipts. The worker starts from the first verified local date it observes, catches up later missed dates, and emits once per day at actual shift end + 30 minutes. The subscriber validates the captured basis, commits the receipt and daily effect, then acknowledges. No historic pre-enrollment absence or tap is invented.

Leave V3 enforces private detail/current-manager scope using a minimal own-schema employee reference. Enable `HRFLOW_LEAVE_EMPLOYEE_CONSUMER_ENABLED=true` only with `org.employee.v1` and the accepted organization subject. `HRFLOW_LEAVE_EMPLOYEE_BOOTSTRAP_TIMEOUT_MS` defaults 60000 (1000–600000 allowed). Startup captures broker positions and replays before authorizing manager work; unknown/inactive references fail closed and termination/reassignment only apply newer revisions. HR decisions remain independent of the requester’s account and employee IDs. Own-schema DML defaults suffice; no identity SQL/HTTP or cross-owner grants are used. Retry/DLQ recovery and ongoing lag readiness remain separate acceptance gates.

Leave calendar integration requires V4 and `HRFLOW_LEAVE_CALENDAR_CONSUMER_ENABLED=true` alongside employee consumption. The independent shift/holiday replay uses `HRFLOW_LEAVE_CALENDAR_BOOTSTRAP_TIMEOUT_MS` (60000 default, 1000–600000 supported), precreated `org.shift.v1` / `org.holiday.v1` topics and accepted organization Avro subjects. Charge calculation stays closed until both replay gates complete. The runtime stores only effective assignment IDs/dates and minimal shift/holiday fields, under tenant RLS. Missing or ambiguous effective assignments, unknown shifts and unresolved DST windows fail explicitly; no Monday–Friday, eight-hour, first-shift or cross-tenant holiday fallback is used. Exact scheduled seconds accompany each reservation; legacy hours remain a display precision only. No historical unknown assignments are fabricated: only the matching actual owner revision can fill V4’s unknown basis.

Leave V5 expands reservations into exclusive MORNING/AFTERNOON slots and adds tenant/request/employee consistency. FULL_DAY claims both slots; terminal release removes only its own request’s slots. Existing active reservations are backfilled under the owning Flyway transaction with FORCE RLS restored before commit, without granting runtime BYPASSRLS. The existing V2 CREATE SCHEMA IF NOT EXISTS statement requires database CREATE for the trusted migrator even when leave_jobs was precreated; provide that migration-only DDL capability when applying the historical chain, and never grant it to the runtime/job role. A migration error blocks startup and requires explicit repair; no legacy inconsistency is silently rewritten.


Leave HTTP paging compatibility: type/balance catalogs and own/pending request lists return bounded `items/page/size/total` envelopes when `page` is explicit. Historical callers omitting `page` retain the prior arrays. New clients should supply page/size; list filters and current-manager scope are applied in the database before count/limit. Own requests default newest, pending default oldest. Search is a literal case-insensitive substring; `minAgeDays` uses the owner clock. No environment switch or migration is needed for this additive query behavior.


Leave receipt upgrade V7 preserves the historical `command_receipt` table and writes new outcomes to tenant-RLS `command_receipt_v2`, scoped by verified account subject, POST path and UUID-v4 key. Legacy keys with unknown actor/path return 409 and require review against the original owner record; do not generate a new key blindly for an unknown outcome. All retryable leave commands now require a valid key (428 if absent/invalid), and successful retries return their original revision/ETag. Apply forward migrations before using the new owner code; no historical provenance is fabricated and no receipt cleanup is added.

Request receipt upgrade V2 follows the leave V7 behavior: retain historical receipts, write tenant/account/POST/path-scoped UUID-v4 outcomes to command_receipt_v2, and reconcile unknown legacy keys instead of repeating them. Apply the forward migration before the new request owner; no global settings or receipt expiry is added.

Request organization references: after V4, REQUEST_EMPLOYEE_CONSUMER_ENABLED=true replays org.employee.v1 to captured startup positions before accepting relationship-dependent submission/manager authorization. REQUEST_EMPLOYEE_BOOTSTRAP_TIMEOUT_MS defaults to 60000. Disabled, unknown or incomplete replay returns 503 REFERENCE_NOT_READY; a verified null manager is the only basis for SKIPPED. Typed listener commits inbox/effect before acknowledgment; ongoing lag/DLQ readiness remains a separate reliability gate. No cross-service database reads.

Request UAT chain V5: all request types resolve MANAGER → HR_ADMIN on submit. Known absent manager is SKIPPED (NO_MANAGER_ASSIGNED); disabled/unready employee replay fails 503. Do not pass client approver IDs as a workaround. Submitted assignments and reference revisions remain captured; later reassignment revokes the prior manager and requires an explicit reviewed reassignment process rather than an automatic chain rewrite. HR_ADMIN may decide only the current HR step, never a preceding manager step. No fake shared HR employee is provisioned.


Request typed publication V6: register request.workflow.v1-value using infra/scripts/register-request-schema.sh before enabling REQUEST_RELAY_ENABLED. REQUEST_JOB_DB_USER/PASSWORD must identify a private NOSUPERUSER/NOBYPASSRLS role with USAGE on request_jobs and SELECT on request_jobs.tenant_registry only; it cannot read request payload tables. The runtime role performs tenant-scoped payload work. The relay is opt-in, bounded to one oldest pending event per ordered request stream and marks published only after broker acknowledgment. REQUEST_RELAY_POLL_MS defaults 250. Older untyped rows remain unsupported pending explicit reconciliation. Migration V6 adds a metadata registry trigger and separate append-only tenant-RLS workflow history; no private form/reason/name/handover/file text is emitted. The new schema/role examples and fresh-init schema have not been deployed.


Request reporting V9: set REPORTING_REQUEST_CONSUMER_ENABLED=true after owner schema registration; REPORTING_REQUEST_BOOTSTRAP_TIMEOUT_MS defaults 60000. Startup replays retained request.workflow.v1 records to captured broker positions and gates REQUESTS exports until complete. The request's exact original createdAt is retained and date bounds are UTC calendar dates; updated_at is never a substitute. Unknown historical creation data returns 503 rather than silently disappearing from filtered exports. Private download authorization/row caps/immutable queue snapshots are shared with ATTENDANCE/LEAVE. Retention-gap rebuild and ongoing lag/DLQ readiness remain separate gates; startup replay alone does not prove a full historical rebuild.

Notification runtime RLS: provision a non-owner role with no BYPASSRLS. Inbox, preference and consumer effect operations establish the verified tenant using transaction-local app.tenant_id; pooled connections must expose no rows after the transaction ends. Owner-role test passes alone do not establish runtime operation. Domain-event recipient resolution remains separately verified work.

Private PostgreSQL job lease reservations (one database): organization relay 73104217, attendance relay 73104218, reporting export worker 73104219, leave relay 73104220, attendance daily worker 73104221, attendance cutoff worker 73104222, request relay 73104223, attendance holiday fanout 73104224. Service schemas do not scope PostgreSQL advisory locks; distinct jobs must not reuse these keys.

Forward attendance V17 records old/new holiday dates atomically with an accepted organization snapshot. `HRFLOW_ATTENDANCE_HOLIDAY_FANOUT_ENABLED` defaults false; enable it with daily materialization and the daily recalculation worker. It uses the existing metadata-only attendance job login, then tenant-scoped runtime transactions. Each pass queues at most 100 observed daily keys and durably retains a two-part cursor until all keys are visited. Holiday changes never manufacture tap evidence. Employee/shift change fanout and historical no-session shift policy remain separate work.

Attendance cutoff upgrade V16 retains future rechecks in tenant-RLS cutoff_recheck. An old trigger cannot finalize a later effective no-session shift early or lose its revised end+30 trigger. Existing cutoff/daily feature flags and private job role are reused; there is no new scheduler or cross-owner SQL. Same date/schedule-basis triggers are deduplicated, while changed bases can emit a new valid version-1 trigger. Apply the forward migration and register reviewed schema changes before enabling the new owner candidate.

Request comments and attachment reservation/completion now share the existing scoped command-receipt transaction used by lifecycle commands. Supply UUID-v4 `Idempotency-Key` on each POST and preserve it for an uncertain outcome. Exact retries return the original private result; changed content conflicts. Upload PUT remains the bounded checksum-verified private file operation. This adds no role, migration, public storage URL or event type.

General-request submit also requires the reviewed draft ETag in `If-Match`, checked under the request row lock before chain capture. Missing/stale preconditions leave no command or workflow effects. Retrying an already committed command uses its original key and preserves the recorded result. Current clients must retain their fetched opaque ETag when submitting.

### Private UAT account setup

The user approved explicit linked-account setup on 2026-10-29; invitation/email delivery remains separate. Apply identity V12 through the normal reviewed migration process first. It prevents multiple enabled accounts for one tenant/employee; existing duplicate links must be reconciled explicitly, never guessed or rewritten. Use the existing identity runtime database credentials from its private environment file (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), not a database superuser. Run from the repository root on a private interactive terminal:

```bash
java -Dloader.main=com.hrflow.uat.IdentityUatAccountSetup \
  -cp services/identity-org/target/identity-org-0.1.0-SNAPSHOT.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  --tenant=your-workspace \
  --employee=00000000-0000-4000-8000-000000000001 \
  --email=employee@example.test \
  --roles=EMPLOYEE,MANAGER \
  --time-zone=Asia/Phnom_Penh
```

Replace the illustrative tenant, employee UUID, login email and roles with explicit approved UAT inputs. The employee must already exist and be ACTIVE in that tenant. The selected login email may differ from the employee contact email; neither is used to guess an association. Always include EMPLOYEE and only the documented MANAGER, HR_ADMIN or OWNER additions actually intended. The command prompts privately twice for a nonblank password of at least 12 characters, hashes through the existing BCrypt encoder, then clears its input buffers. Do not put passwords in command arguments, redirected input or screenshots.

This separate non-web entry point does not start the identity HTTP server, local seed account, signing-key setup or scheduled workers. It disables Flyway and validates the existing schema. One transaction creates a new account with the exact employee link, appends the normal organization snapshot with the real existing `userId` field, and writes private setup audit metadata. Audit actor is explicitly absent; the actual database principal and PRIVATE_UAT_SETUP source are recorded without email/password/hash. Existing accounts, roles and links are never overwritten. Inspect duplicate or uncertain outcomes before retrying. This command does not deliver invitations, create employee/shift data, deploy services or establish client UAT acceptance.

Private request attachment transfers use the additive canonical PUT upload/GET file operations and the existing authenticated API origin, with exact request/attachment path validation in clients. Presign accepts `size`/`sha256` alongside legacy aliases. Completion retains its original persisted revision/time and provides both legacy metadata and the shared workflow-record envelope. Content is checked against the reserved length, SHA-256, extension and PDF/PNG/JPEG signature before atomic private publication. Downloads are no-store/nosniff attachments. Signature checking is not malware scanning or complete document validation; no antivirus assurance is implied.

Explicit request list pages (`/requests/me`, `/requests/inbox`, `/requests/types`, `/materials`) return `{items,page,size,total}` when `page` is present; omit page only for legacy array consumers. Use size 1–100 and stable owner ordering. Inbox totals already apply the current assigned pending step and organization relationship; missing manager references are unavailable, not an empty approval chain. Catalog pages include active tenant rows only. This adds no configuration switch.

Attendance V18 retains each materialized day's actual schedule inputs in its tenant-RLS daily_schedule table. Past no-session days keep that basis by their retained local timezone; today/future days can adopt verified organization edits. Calendar and leave overlays remain separately recalculable. Old materializations with missing original weekday/grace inputs are not guessed from the newest shift: they return unavailable and remain queued for explicit repair. Session-captured schedules are unchanged. No additional service, role or feature switch is introduced; organization fanout remains independently required.

Full `scripts/verify-project.py` enables `HRFLOW_VERIFY_CLIENT_PROVIDERS=true` and requires the actual web/mobile repositories and runtimes for the disposable direct-provider journey. Backend-only Maven/verification omits that cross-repository fixture unless explicitly opted in; it cannot certify client integration. The fixture uses the pinned local Flutter binary when present, otherwise installed Flutter, and never assumes a macOS package-manager path. It launches/stops disposable loopback providers and supplies synthetic tokens only through bounded subprocess environment.

UAT attendance-to-leave feedback (2026-10-29): HRFLOW_LEAVE_ATTENDANCE_CONSUMER_ENABLED=true enables the private leave consumer for attendance.daily-status.v1. It requires the actual typed daily publisher and compatible schema registration first. Explicit per-reference overlap transitions a matching APPROVED record to NEEDS_REVIEW without balance compensation; default false keeps unconfigured local scaffolds from silently attempting integration. Configure alongside notification domain consumption to deliver HR review notices. The actual conflict/Kafka test evidence is in W06; this note does not establish deployment.

UAT schedule reference changes (2026-10-29): HRFLOW_ATTENDANCE_SCHEDULE_FANOUT_ENABLED=true enables the durable employee/shift-to-observed-day fanout. It uses the existing attendance private job credentials, schema/tenant registry and a separate lease. Enable the daily recalculation worker as well; fanout alone queues work. Each change retains its observed clock instant and each daily record's timezone determines whether that date is historical. Default false remains explicit for unconfigured local scaffolds; W04 holds scoped evidence.

UAT shared event recovery (2026-10-29): set HRFLOW_EVENTS_RECOVERY_ENABLED=true for canonical owner consumers only after provisioning retry/DLQ topics and registering schemas. Spring Kafka stores exact original wire bytes during consumption; retry publication uses those bytes rather than registering schemas under retry subjects. The three retry topics match source partition count; private DLQ has one partition. The existing owner parent imports the implemented event starter. Unknown/stale lag is -1; a private eventRecovery health contributor and bounded broker probe are active with this flag. Optional HRFLOW_EVENTS_RECOVERY_LAG_POLL_MS defaults to 10000; source/retry offsets, not old event timestamps, drive the measurement. Full operator instructions are in the Week 05 README. The prior generic notification lag gauge now reads this actual broker monitor when enabled.
