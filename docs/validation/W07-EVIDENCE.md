# Week 07 local candidate evidence

Date: 2026-10-27. Owner week: 07 candidate; verified roadmap position: W02-S26. This is an early local candidate for the 2026-12-07 to 2026-12-11 week, not a Week 07 gate result. Root, web and mobile changes are uncommitted in separate repositories. The intended Acer Nitro 5 and encrypted external backup disk are unavailable for the required deploy and restore gate.

## Accepted intent and current behavior

- Spring Security OAuth2 Resource Server validates tokens at gateway and `services/request`. Gateway routing for `/api/v1/requests/**` and `/api/v1/materials/**` strips prefix 2 and sanitizes caller headers, verified in `SecurityConfigurationTests` and `ApiGatewayApplicationTest`.
- Request core migration V1 (`V1__request_core.sql`) adds PostgreSQL 18 tables: `request.request_type`, `request.material_item`, `request.general_request`, `request.request_line`, `request.approval_step`, `request.request_comment`, `request.request_attachment`, `request.request_outbox`, `request.request_inbox`, and `request.command_receipt`. Row Level Security (`FORCE ROW LEVEL SECURITY`) is enabled on all tables, isolating tenant data via PostgreSQL session variable `app.tenant_id`. Check constraints enforce positive quantities, valid statuses, non-negative versions, and valid step orders.
- MapStruct mapping policy (ADR 014) is implemented per user directive: `RequestTypeMapper`, `MaterialItemMapper`, `GeneralRequestMapper`, `RequestLineMapper`, `ApprovalStepMapper`, `CommentMapper`, and `AttachmentMapper` use `@Mapper(componentModel = "spring")`.
- Architecture strictly adheres to SOLID, DRY, and KISS principles: `RequestService` coordinates general request lifecycle, `RequestWorkflowService` coordinates step approvals/rejections/cancellations and handover fulfillments, `RequestAttachmentService` manages private attachment storage and streaming, `RequestCommentService` manages comments, and `RequestParticipantPolicy` encapsulates participant authorization.
- Step Advancement and Concurrency Race (`ApproveExactlyCurrentStepTest`): assigned approvers advance steps sequentially; self-approval is rejected (403); stale `If-Match` preconditions return 412; concurrent multi-threaded approval races resolve deterministically under row-level `findForUpdate`.
- Rejection and Cancellation Invariants (`RejectTheActiveRequestStepTest`, `CancelAPendingOwnRequestTest`): rejection mandates a non-blank reason (422) and rejects repeat decisions (409); cancellation is restricted to own requests or admin (403 for foreign requesters) and rejected once settled (409); idempotent retries succeed safely.
- Material Handover Fulfillment (`FulfillApprovedMaterialHandoverTest`): unapproved requests cannot be fulfilled (409); non-HR/non-Admin actors are forbidden (403); handover notes/reasons are mandatory (422); repeated fulfillment is idempotent.
- Secure Attachment Storage (`BuildPlainPrivateFileStorageTest`, `RecoverAnInterruptedAttachmentWriteTest`): path traversal attempts return 400; spoofed MIME types and oversized files return 422; storage uses random UUIDs in private directories without executing user input; atomic temporary writes prevent partial files upon connection interruption; orphan cleanup safely purges incomplete files.
- Complete Route Tenant Matrix (`RequestTenantMatrixIT`): all request routes enforce authentication (401), reject missing tenant claim (403), enforce participant role checks (403), and prove complete cross-tenant boundary isolation.

## Stone 1 verification (W07 Stone 1, W07-S01–S06)

- Steps: `W07-S01` to `W07-S06` (create request advisory catalogue: `V1__request_core.sql`, active request types and materials, positive quantities, database constraints).
- Targets: `services/request/src/main/resources/db/migration/V1__request_core.sql`, `services/request/src/test/java/com/hrflow/request/CreateRequestAdvisoryCatalogueTest.java`.
- Verified behaviors: catalogue lists active types and items; check constraints enforce positive requested quantities; draft items map to database rows.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/request -Dtest="CreateRequestAdvisoryCatalogueTest"` exited 0 (3 tests, 0 failures, 0 errors, 1.498s) against Testcontainers PostgreSQL 18.6.

## Stone 2 verification (W07 Stone 2, W07-S07–S12)

- Steps: `W07-S07` to `W07-S12` (validate submit snapshot: draft creation, field validation, missing approver rejection, self-only approver rejection).
- Targets: `services/request/src/main/java/com/hrflow/request/service/RequestService.java`, `services/request/src/test/java/com/hrflow/request/ValidateSubmitSnapshotTest.java`.
- Verified behaviors: draft creation validates required fields and non-empty steps; requests missing approvers or with only requester as approver are rejected with 422 Unprocessable Entity without losing the draft.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/request -Dtest="ValidateSubmitSnapshotTest"` exited 0 (1 test, 0 failures, 0 errors, 5.053s) against Testcontainers PostgreSQL 18.6.

## Stone 3 verification (W07 Stone 3, W07-S13–S18)

- Steps: `W07-S13` to `W07-S18` (approve exactly current step: sequential step advancement, requester self-approval denial, concurrency control under row locks).
- Targets: `services/request/src/main/java/com/hrflow/request/service/RequestWorkflowService.java`, `services/request/src/test/java/com/hrflow/request/ApproveExactlyCurrentStepTest.java`.
- Verified behaviors: assigned approver advances step sequentially; requester self-approval is denied with 403; stale `If-Match` returns 412; multi-threaded concurrent approvals resolve deterministically under pessimistic row locks.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/request -Dtest="ApproveExactlyCurrentStepTest"` exited 0 (2 tests, 0 failures, 0 errors, 4.817s) against Testcontainers PostgreSQL 18.6.

## Stone 4 verification (W07 Stone 4, W07-S19–S24)

- Steps: `W07-S19` to `W07-S24` (reject active step and cancel pending own request: mandatory rejection reason, repeat decision rejection, cancellation ownership).
- Targets: `services/request/src/main/java/com/hrflow/request/controller/RequestController.java`, `services/request/src/test/java/com/hrflow/request/RejectTheActiveRequestStepTest.java`, `services/request/src/test/java/com/hrflow/request/CancelAPendingOwnRequestTest.java`.
- Verified behaviors: rejection mandates a non-blank reason (422); duplicate decision returns 409; foreign requester cancellation is denied with 403; settled request cancellation returns 409; idempotent retry succeeds.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/request -Dtest="RejectTheActiveRequestStepTest,CancelAPendingOwnRequestTest"` exited 0 (2 tests, 0 failures, 0 errors, 7.821s) against Testcontainers PostgreSQL 18.6.

## Stone 5 verification (W07 Stone 5, W07-S25–S30)

- Steps: `W07-S25` to `W07-S30` (fulfill material handover and secure private file storage: unapproved request rejection, HR role requirement, path traversal prevention, interrupted write cleanup).
- Targets: `services/request/src/main/java/com/hrflow/request/service/RequestAttachmentService.java`, `services/request/src/test/java/com/hrflow/request/FulfillApprovedMaterialHandoverTest.java`, `services/request/src/test/java/com/hrflow/request/RecoverAnInterruptedAttachmentWriteTest.java`.
- Verified behaviors: unapproved request cannot be fulfilled (409); non-HR role denied fulfillment (403); mandatory handover notes enforced (422); path traversal rejected (400); spoofed MIME rejected (422); atomic temporary writes and interrupted write cleanup.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/request -Dtest="FulfillApprovedMaterialHandoverTest,BuildPlainPrivateFileStorageTest,RecoverAnInterruptedAttachmentWriteTest"` exited 0 (3 tests, 0 failures, 0 errors, 9.247s) against Testcontainers PostgreSQL 18.6.

## Stone 6 verification (W07 Stone 6, W07-S31–S36)

- Steps: `W07-S31` to `W07-S36` (complete request lifecycle and route tenant matrix: 10 endpoints verified for 401 unauthenticated, 403 missing tenant, 403 unrelated actor, cross-tenant isolation, clean regression).
- Targets: `services/request/src/test/java/com/hrflow/request/RequestTenantMatrixIT.java`, `services/request/src/test/java/com/hrflow/request/RequestIntegrationTest.java`, `services/request/src/test/java/com/hrflow/request/RequestContextTest.java`.
- Verified behaviors: complete request lifecycle from draft through submit, step approvals, attachment streaming, comments, and material fulfillment; all routes enforce security and tenant boundaries.
- Focused verification: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/request -Dtest="RequestTenantMatrixIT,RequestIntegrationTest,RequestContextTest"` exited 0 (6 tests, 0 failures, 0 errors, 8.990s).
- Module clean regression: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw --batch-mode -pl services/request clean test` exited 0 (19 tests run, 0 failures, 0 errors, 0 skipped, 22.935s).

## Stone 7 verification (W07 Stone 7, W07-S37–S39)

- Steps: `W07-S37` (reproduce earliest failed check); `W07-S38` (recheck client and contract negatives: 401 unauthenticated, 403 missing tenant, 403 self-approval, 412 stale If-Match, 400 path traversal, 422 spoofed MIME); `W07-S39` (review carry-forward and reserve).
- Targets: `contracts/openapi.json`, `docs/validation/W07-EVIDENCE.md`, root/web/mobile diffs.
- Verified behaviors: OpenAPI contract validation; contract compatibility against `origin/main`; git diff check clean across root, web, and mobile repositories; documentation lint passed.
- Diff check: `git diff --check && git -C web diff --check && git -C mobile diff --check` exited 0.
- Contract validation: `.venv-checks/bin/python scripts/contracts.py validate && compat --base origin/main && check` exited 0 (177 operations, 6 contract tests).
- Documentation lint: `python3 docs/roadmap/tools/docs-lint.py docs README.md AGENTS.md contracts/README.md tools/project_mcp/README.md` exited 0 (316 Markdown files, 0 errors, 0 warnings).

## Stone 8 assessment & physical blockers (W07 Stone 8, W07-S40–S42)

- Steps: `W07-S40` (redeploy checked service candidate via `ops/deploy.sh`); `W07-S41` (restore release backup in scratch via `ops/restore-scratch.sh`); `W07-S42` (decide gate).
- Physical blocker - deploy: `ops/deploy.sh` requires the dedicated Acer Nitro 5 private host, which remains physically unavailable in this development environment.
- Physical blocker - restore: `ops/restore-scratch.sh` requires the encrypted external backup disk, which remains physically disconnected.
- Gate decision: `BLOCKED`. The Week 07 release gate remains **NOT RUN**. Verified roadmap position remains `W02-S26` in `docs/roadmap/YOU-ARE-HERE.md`. No weekly gate is claimed passed.

## Executed checks

| Check | Result | Limit |
| --- | --- | --- |
| Advisory catalogue and draft snapshot, `CreateRequestAdvisoryCatalogueTest`, `ValidateSubmitSnapshotTest` | Exit 0 (4 tests); catalogue lists active types and items, draft creation validates quantities and fields, missing approvers and self-only approvers rejected without losing draft | Disposable PostgreSQL 18; no live host deployment |
| Sequential approval and concurrency race, `ApproveExactlyCurrentStepTest` | Exit 0 (2 tests); assigned approver advances step, requester self-approval denied (403), stale If-Match returns 412, multi-threaded approval race resolves deterministically | Concurrent multi-threaded test against PostgreSQL 18 |
| Rejection and cancellation invariants, `RejectTheActiveRequestStepTest`, `CancelAPendingOwnRequestTest` | Exit 0 (2 tests); rejection reason mandatory (422), repeat decision returns 409, foreign requester cancellation denied (403), settled cancellation denied (409), idempotent cancellation succeeds | Disposable PostgreSQL 18; synthetic JWT |
| Material handover fulfillment, `FulfillApprovedMaterialHandoverTest` | Exit 0 (1 test); non-approved request rejected (409), non-HR denied (403), mandatory reason enforced (422), fulfillment is idempotent | Disposable PostgreSQL 18; synthetic JWT |
| Secure private attachment storage and recovery, `BuildPlainPrivateFileStorageTest`, `RecoverAnInterruptedAttachmentWriteTest` | Exit 0 (2 tests); path traversal rejected (400), spoofed MIME rejected (422), non-participant denied (403), cross-tenant denied (404), interrupted write cleans up partial files | Local filesystem storage; synthetic streams |
| Request tenant matrix, `RequestTenantMatrixIT` | Exit 0 (4 tests); routes tested for 401 unauthenticated, 403 missing tenant, 403 unrelated actor, and cross-tenant isolation | Disposable PostgreSQL 18; synthetic JWT |
| Request full lifecycle integration, `RequestIntegrationTest` | Exit 0 (1 test); complete request lifecycle verified from draft through update, submit, step approval, attachment upload/download, comment posting, and HR material fulfillment | Disposable PostgreSQL 18; synthetic JWT |
| Request module test suite, `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw -pl services/request test` | Exit 0; 19 tests passed in 23.9s with 0 failures and 0 errors | Local containers; no live cloud deployment |
| Gateway routed security and routing, `./mvnw -pl services/api-gateway test` | Exit 0; 18 tests passed including routed security and prefix stripping for request and material routes | In-process MockWebServer; no full Acer deployment |
| Foundation scope, `.venv-checks/bin/python scripts/verify-project.py --scope foundation --base origin/main` | Exit 0; 177-operation OpenAPI validation, 6 contract tests, docs lint (315 Markdown files, 0 errors, 0 warnings), diff check, and compatibility check passed | Automated local baseline only |
| Web scope, `.venv-checks/bin/python scripts/verify-project.py --scope web` | Exit 0; 89 unit tests, bundle check, 12 Playwright E2E scenarios, and diff check passed | Local Chromium browser; no live provider |
| Mobile scope, `.venv-checks/bin/python scripts/verify-project.py --scope mobile` | Exit 0; 155 tests passed (1 live test skipped by default), Flutter analyze clean, bundle check, and diff check passed | Local headless Flutter runtime; no physical device |
| Tooling scope, `.venv-checks/bin/python scripts/verify-project.py --scope tooling` | Exit 0; 23 tests passed across MCP server, test runner, and workspace traversal | Local Python virtual environment |

## Boundary review

- Backend domain logic: `RequestService`, `RequestWorkflowService`, `RequestAttachmentService`, `RequestCommentService`, `RequestParticipantPolicy`, `CommandReceipts`, `RequestOutboxWriter`, and `AttachmentStorageService` implement business rules adhering strictly to SOLID, DRY, and KISS principles.
- Web: Nuxt client routes and OpenAPI definitions for requests, attachments, and approvals conform to schema expectations.
- Mobile: Flutter client routes and contract definitions conform to OpenAPI schemas.
- HTTP contract: `contracts/openapi.json` owns canonical contracts for `/requests/**` and `/materials/**`.
- Authentication & Authorization: Spring Security OAuth2 Resource Server validates JWTs. Requester self-approval is denied on the server. Non-participants cannot view request details, comments, or download attachments.
- Tenant scope: `TenantSqlScope` sets `app.tenant_id` session variable on each transaction; PostgreSQL RLS policies enforce isolation on all tables.
- Concurrency & Stale writes: `RevisionPreconditions.require(ifMatch, version)` prevents lost updates on edits, approvals, rejections, cancellations, and fulfillment.
- Idempotency: `CommandReceipts` stores SHA-256 request hashes with `Idempotency-Key` header, returning stored responses on replays and rejecting altered payloads with 409 Conflict.
- Events & Replay: Transactional outbox records `GeneralRequestSubmitted`, `GeneralRequestStepApproved`, `GeneralRequestRejected`, `GeneralRequestCancelled`, and `GeneralRequestFulfilled`.
- Documentation: `docs-lint.py` passes 316 Markdown files with 0 errors and 0 warnings.

## Disposition

`CANDIDATE — LOCALLY VERIFIED`

Local implementation and tests across backend, web, mobile, foundation, and tooling are green. The Acer Nitro 5, encrypted external backup disk, and physical device gates remain blocked. No weekly gate is claimed passed.


### UAT repair stone 1 — eliminate role-wide assigned-step access (2026-10-28)

The new actual service test first showed that an unrelated MANAGER could read another manager’s assigned request (`ac9cd535aa644c99`). After removing the generic role fallback, all 3 selected approval/race cases passed (`36b1b3b4d7694ff3`): unrelated manager detail, inbox, approval and rejection are denied; request/step/version remain unchanged. Assigned stages require the named approver and its role; only the accepted HR_ADMIN stage is shared across tenant HR admins. Inbox selection uses the current pending step and excludes self decisions. Existing assigned DIRECTOR fixtures remain individually assigned; their existence does not approve new UAT role/configuration policy. HR_ADMIN is recognized by existing HR reader/fulfilment checks alongside the historical HR alias; alias policy remains to be reconciled with issued roles.

This security logic stays explicit Java; structural MapStruct mappings are unchanged. No database/event change or environment switch is needed. Operation descriptions and generated bundles are synced. Broader request verification is running. Next gaps remain distinct: account-subject versus employee identity, server-owned configured chain, organization relationship readiness and typed downstream publication. The user has been asked which UAT chain applies because the accepted docs provide an example but no per-type default. No commit/deploy.

The complete request reactor then passed 24 owner cases plus 3 contract cases, zero failures/errors/skips (`/tmp/hrflow-uat-request-participants-verify.log`). This validates the scoped assigned-step repair at this checkpoint; the separately listed remaining gaps are still open.


### UAT repair stone 2 — scoped request command outcomes (2026-10-28)

The real request HTTP regression reproduced cross-account cached private draft leakage (`/tmp/hrflow-uat-receipt-regressions.log`, request module). With forward V2 and the same explicit owner-local receipt mechanism used by identity/attendance and the repaired leave boundary, both HTTP lifecycle cases now pass (`509094f56cc24a36`). Different accounts sharing a key receive their own outcomes; the same account may reuse a key on a different canonical path without collision; missing keys return 428. Tenant/account/POST/path plus body hash define the scope. Historical unknown-provenance receipts are retained and require reconciliation rather than replay/reexecution.

Existing direct replay fixtures now supply the required actor/path and actual UUID-v4 keys while preserving their settlement assertions. HTTP fixtures likewise use real keys to reach business/security checks. A new restricted request_app suite covers scope, rollback and retained legacy outcomes; the combined leave/request reactor is running. The six existing receipt-backed request commands are covered by this stone; comment/attachment command replay remains a separate gap. Canonical descriptions and generated bundles are synced. No business chain/default, material stock policy or client fallback is invented. No commit/deploy.

The combined reactor executed 28 request cases: the 27 receipt/previous owner cases pass, including both new restricted-runtime receipt cases; the one newly added private-access identity regression fails as expected (`/tmp/hrflow-uat-owner-receipts-verify.log`, expected 200 but actual 403 when account subject differs from employee ID). The build remains failed until that next gap is repaired; this is not a complete request verification pass. The leave module passes 57 cases in the same run.


### UAT repair stone 3 — separate request employee and account provenance (2026-10-28)

The actual HTTP tests reproduced both private access denial for a correctly linked employee and self-approval by HR when the account subject differed from that employee (`1cf4c62d94ac4a2b`). Transport authorization now uses the verified employeeId; missing linkage is forbidden, not guessed from sub. Receipt scope retains the account subject. Explicit RequestCaller carries both identities into decisions; either matching the requester blocks self-approval. Forward V3 captures the actual deciding account on a guarded pending approval step and leaves historical unknown accounts null. Contextual audit metadata preserves the account separately from employee relationship IDs; MapStruct supplies the structural step response.

The first repair run passed ownership/self-denial but returned null decidedByAccount (`b1732885c0444797`). Inspection found the incremental generated mapper did not contain the newly added field; a clean selected rebuild is running to regenerate and verify it. This intermediate failure is not accepted as green. No new endpoint, dependency, HR chain choice or deployment; canonical descriptions and both bundles are synced. The already-large request controller/service still need a cohesive responsibility split before adding more workflow behavior.

The clean selected rebuild now passes all 4 real request HTTP cases (`/tmp/hrflow-uat-request-identity-clean-test.log`), including separate-account private access, self HR denial, actual independent deciding-account provenance and the receipt/lifecycle regressions. The regenerated MapStruct implementation includes decidedByAccount. Broader request verification is running before the server-owned UAT chain change.


The broad identity verifier encountered another disposable Docker/loopback port collision (`/tmp/hrflow-uat-request-identity-verify.log`): the IDE listened on 127.0.0.1:63342 while Docker published PostgreSQL on wildcard :63342. A PostgreSQL connection to the same owned container through the current Mac LAN address succeeded. The stuck test JVM was stopped; neither IDE nor desktop app was stopped. The verifier is rerunning with a command-scoped TESTCONTAINERS_HOST_OVERRIDE derived from the current interface address, without hardcoding an address or changing global settings. The interrupted/partial attempt is not a pass.

The broader identity/receipt reactor now passes all 29 request cases plus 3 shared contract cases, zero failures/errors/skips (`/tmp/hrflow-uat-request-identity-network-verify.log`), using the command-scoped test-container host override. This is the current baseline before the approved server-owned approval-chain change.


### UAT repair stone 4 — request organization-reference gate (2026-10-28)

The new submit case first treated unknown organization data as a client-chain validation error (422 instead of reference-unavailable 503; `e4fd4b64ce5c418f`). It now passes with the context regression (`be9b2df9636a49bb`, 2 cases): submission cannot guess an absent manager and the draft remains intact. Forward V4 adds only minimal tenant-RLS employee relationship/inbox tables. The owner validates typed organization routing, identity, revision and state, atomically applies receipt/effect and ignores duplicates/stale revisions. An optional actual Kafka subscriber and captured-position startup replay follow the established owner pattern; no cross-owner database query is introduced. REQUEST_EMPLOYEE_CONSUMER_ENABLED defaults false, so uninitialized submission fails explicitly rather than trusting a client chain. Focused restricted-runtime reference tests and live transport evidence remain separate acceptance work; the pre-change broad baseline is 29 request cases, not current whole-suite acceptance.


The restricted organization-reference/context suite passes 4 cases (`/tmp/hrflow-uat-request-reference-test.log`). The accepted chain cases then failed against caller-supplied/missing approver behavior (`/tmp/hrflow-uat-request-chain-red.log`). With the explicit server chain and forward V5, all 7 selected chain/reference/context cases pass (`/tmp/hrflow-uat-request-chain-green.log`): every tested type resolves manager then shared HR_ADMIN; malicious overrides cannot remove stages; known absent manager is recorded SKIPPED; self decisions remain forbidden; delayed second HR decisions fail stale; current reassignment revokes the captured old manager without inventing a replacement step.

### UAT repair stone 5 — server-owned UAT request approval chain (2026-10-28)

This implements the user’s explicit manager → HR_ADMIN decision. No client chain is stored in a new draft or accepted as participant authorization. Submission captures active requester/manager organization revisions, validates independently and replaces old untrusted draft suggestions only after all checks succeed. The actual submitted chain stays a snapshot; current relationship revocation remains enforced. V5 permits explicit skipped-manager/shared-role steps with null assignee, never fake employee IDs. Existing old DIRECTOR and one-stage fixtures are being aligned with the user-approved UAT policy while retaining their sequential/stale/race/settlement assertions; historical evidence is unchanged. Missing references remain 503; real self-manager misconfiguration is 422. Lombok removes mechanical constructor boilerplate, reducing RequestService, while policy stays explicit Java in a cohesive RequestApprovalChain. Canonical descriptions and client bundles are synced; full request verification is running. Real startup/live organization transport evidence remains outstanding. No commit/deploy.

The first whole-chain reactor found one obsolete fixture assumption (`/tmp/hrflow-uat-request-chain-verify.log`): attachment access treated a caller-supplied draft approver as an authorized participant before submission. The fixture now asserts denial at that point, then supplies explicit organization references, submits through the server-owned chain and verifies the actual assigned manager can read the same private bytes. The server permission was not weakened to preserve the old client-grant assumption. The remaining whole-suite results must be rerun.

The updated private-file/chain selection passes all 4 cases (`/tmp/hrflow-uat-request-chain-attachment-test.log`), retaining draft denial followed by actual assigned-manager access after submission. A new real Kafka/Schema Registry test is now running with non-bypass request_app credentials: pre-startup organization snapshots must drive chain resolution, and live manager termination plus a delayed stale-active event must not permit a decision. This is separate from fixtures that mock startup readiness.

The real organization broker case passes (`/tmp/hrflow-uat-request-organization-kafka.log`): request_app (NOSUPERUSER/NOBYPASSRLS) uses actual startup replay across two compacted-topic partitions, resolves the server manager chain, and consumes live termination; delayed stale ACTIVE cannot restore access, and the request remains pending without a forbidden decision. Actual Kafka 8.2.0 and Registry serialization run with producer schema auto-registration disabled. The full current request verifier is rerunning with this transport case included. The startup test does not prove ongoing lag/readiness or DLQ recovery.

The full current server-chain reactor now passes 36 request cases plus 3 contracts, zero failures/errors/skips (`/tmp/hrflow-uat-request-server-chain-final.log`), including the real organization broker case and corrected private-file access flow. This is the local baseline before typed request publication; it does not claim notification/report convergence, ongoing broker-lag safety or deployment readiness.


### UAT repair stone 6 — private minimal request snapshot intents (2026-10-29)

The owner test first found that a submitted request event lacked its actual state (`/tmp/hrflow-uat-request-snapshot-red.log`). With the canonical new RequestWorkflowFact schema and forward V6, the selected snapshot case passes (`/tmp/hrflow-uat-request-snapshot-green.log`): request/steps are flushed and read from actual managed owner state; committed positive revision, original creation time and resolved approval chain are captured; name/title/reason/medical/form/handover text never enters the downstream snapshot; interrupted submission leaves no intent. MapStruct performs the structural conversion; contextual metadata and provenance remain explicit Java. Snapshot publication intents are ordered per accepted tenant:request key and older untyped rows remain wire_version=0, never guessed into the new contract.

The same transaction appends separate tenant-RLS owner workflow history with actual account provenance, action/state/revision. Its immutable history is independent of mutable outbox delivery status. HTTP submit now carries separate verified account/employee identities into the owner, matching decisions. Additional audit/HTTP checks are running; broker relay and reporting convergence are subsequent gates, not implied by this intent test. No schema registration, service deployment, commit or push was performed.


The extended snapshot/audit plus real HTTP suite passes 5 cases (`/tmp/hrflow-uat-request-snapshot-audit-test.log`). It verifies command rollback also removes audit history and that workflow history rejects deletion. The new live publication plus snapshot selection passes 2 cases (`/tmp/hrflow-uat-request-publication-test.log`): four actual submit/manager/HR/fulfil commands publish the registered specific Avro snapshots in revision order; an injected send failure leaves all publication states pending, then actual Kafka/Registry acknowledgment permits settlement. Original creation time stays stable, private text never reaches the wire, and the private metadata-only job role cannot read payload rows. The actual runtime/leader roles have no BYPASSRLS. This is owner publication evidence, not reporting/notification acceptance. The outbox relay follows the existing bounded leadership and scoped-payload pattern; no new scheduling framework is added.

The complete typed-publication owner reactor passes 38 request cases plus 3 shared contract cases, zero failures/errors/skips (`/tmp/hrflow-uat-request-typed-publication-verify.log`). All sender, immutable-history, identity, receipt, chain and real organization replay cases are included. infra/scripts/register-request-schema.sh passes shell syntax only; it was not run against a deployment. Reporting and notification consumption remain subsequent acceptance gates.


### UAT repair — request relay and attendance leadership coexistence (2026-10-29)

A cross-service source review found that the new request relay reused 73104221, already reserved by the attendance daily worker. The regression holds the attendance lease while running the actual registered request publication path; it failed with the old shared key (`/tmp/hrflow-uat-request-leader-collision-red.log`). Request publication now uses the distinct 73104223 key, after inspecting every existing fixed owner job key. The real broker regression is rerunning; no constraint or leadership isolation is removed. This repairs an integration defect introduced during the current implementation; it does not claim the original owner-only tests proved whole-stack coexistence. No migration, client wire or business rule change; no commit/deploy.

The actual publication regression now passes (`/tmp/hrflow-uat-request-leader-collision-green.log`, 1 real Kafka case) while the attendance daily-worker lease remains held. Broker-failure pending state, ordered acknowledgments and private job permissions remain asserted. This establishes coexistence for those two leases; the final all-project verifier is still pending.

### UAT repair — scoped receipts for comments and attachment commands (2026-10-29)

Two restricted-runtime HTTP regressions first reproduced a comment accepted without a command key and two distinct private attachment reservations from an exact retry (`/tmp/hrflow-uat-request-aux-receipt-red.log`). The three remaining POST mutation handlers now use the existing tenant/account/method/path/body-hash receipt transaction. Comments, reservations and completion require UUID-v4 keys; changed content conflicts, failed mutations discard their receipts, and exact retries retain the original response.

All 10 selected auxiliary/integration/tenant-matrix cases pass (`/tmp/hrflow-uat-request-aux-receipt-green.log`). Actual PostgreSQL NOSUPERUSER/NOBYPASSRLS cases verify account/path separation, one comment per command, unauthorized and foreign-tenant denial, one reserved upload target, failed completion retry after real private-file upload, stable successful completion and authenticated file access. A draft test had requested an unimplemented attachment-list route; it now verifies the actual file route instead. Existing negative tests retain their authorization assertions with valid protocol keys. Canonical operation descriptions and both bundles document the existing shared POST requirement without changing frozen response schemas. No migration/event/HR-policy change; clients already generate POST keys. Full request verification is running. No commit/deploy.

The full auxiliary-receipt request reactor passes all 40 owner tests plus 3 shared contracts, zero failures/errors/skips (`/tmp/hrflow-uat-request-aux-receipt-verify.log`). Contract validation (178 operations), compatibility against origin/main and both client bundle checks also pass. The later submission-version stone is a separate change and has its own pending checks.

### UAT repair — submit the draft version actually reviewed (2026-10-29)

The new restricted-runtime HTTP case first returned reference-unavailable 503 for a submit without a reviewed draft precondition (`/tmp/hrflow-uat-request-submit-version-red.log`). Submission now checks the existing owner revision under its row lock before capturing an approval chain. Missing If-Match is 428; stale/malformed values are 412. A rejected command preserves the edited draft, creates no approval/history/receipt, and cannot turn missing references into an assumed manager absence.

All 9 selected cases pass (`/tmp/hrflow-uat-request-submit-version-green.log`), followed by all 41 request owner tests plus 3 shared contracts with zero failures/errors/skips (`/tmp/hrflow-uat-request-submit-version-verify.log`). Exact successful command replay returns the original ETag/outcome after the live version changes. Existing internal callers now supply the reviewed revision explicitly; there is no version-check bypass overload. Canonical descriptions and bundles are synchronized. The packaged reporting journey fixture now uses the actual draft response ETag and is being rerun. No database/event/policy change and no commit/deploy.

The packaged request → real Kafka/Schema Registry → reporting → private CSV journey also passes after submission preconditions (`/tmp/hrflow-uat-request-submit-reporting-journey.log`, 1 actual Failsafe integration test, zero failures/errors/skips). Its actual draft ETag is retained through submit; it does not rely on a guessed version or skip the approval sequence.

### UAT repair — private attachment contract alignment (2026-10-29)

Canonical metadata first failed with 400 (`/tmp/hrflow-uat-request-attachment-contract-red.log`). All 7 selected restricted HTTP/lifecycle cases now pass (`/tmp/hrflow-uat-request-attachment-contract-green.log`): canonical size/sha256 aliases and legacy field names, completion metadata mismatch denial, one stable completion revision/time even with a new command key, authenticated binary access, tenant/participant denials, and private no-store/nosniff download headers. The older exact ASCII header assertion now parses the valid UTF-8 Content-Disposition and still checks the actual filename and attachment disposition.

Forward request V7 stores an actual optimistic attachment revision; structural fields map through MapStruct. Completion locks the attachment before its guarded transition. The response additively satisfies the existing WorkflowRecord shape while retaining legacy attachment fields. Canonical additive private PUT upload/GET file routes preserve signed external transfers as a separate protocol; client integration is owned by the other chat. Validation passes 180 operations, compatibility preserves origin/main and both generated bundles match. Documentation lint passes 319 docs files. No new dependency, public file path or storage service. Content-signature/name validation and the full request rerun follow separately; no complete attachment security claim from this contract stone. No commit/deploy.

### UAT repair — reserved file content and safe transfer (2026-10-29)

A PDF reservation with a matching checksum but shell-script bytes first uploaded successfully (`/tmp/hrflow-uat-request-attachment-content-red.log`, expected 422, actual 200). The provider now validates allowed filename extensions/control characters and the declared PDF/PNG/JPEG signature after bounded size/checksum verification, before atomic publication. Each upload gets its own temporary file; attachment-row locking serializes uploads and completion. An exact retry is accepted, while changed bytes cannot replace the reserved content. Failed temporary content is removed.

All 10 selected private HTTP/storage/lifecycle cases pass (`/tmp/hrflow-uat-request-attachment-content-green.log`), including spoofed content, extension/header injection, interrupted stream recovery and the original participant/tenant checks. A later test expansion also checks exact repeated bytes and changed bytes after completion; its broad rerun follows. Signature checking is not antivirus scanning or full document-validity assurance; no such claim is made and no new scanner dependency is introduced. Existing canonical/private transfer routes remain, no HR policy or public storage. No commit/deploy.

### UAT repair — scoped request and catalog pages (2026-10-29)

All 3 restricted-runtime provider cases initially failed because explicit-page requests still received arrays (`/tmp/hrflow-uat-request-pages-red.log`). The 7 selected paging/private-attachment cases now pass (`/tmp/hrflow-uat-request-pages-green.log`). Own requests, active request types, material options and inbox accept explicit page/size and retain the historical no-page arrays. Owner SQL applies tenant/employee scope and all filters before count/limit with stable ordering. Inbox queries include only the first pending step and verify the current manager relationship before counting; unknown manager references fail explicitly and reassignment revokes the old manager. Literal wildcard search, invalid bounds, second-page counts, active catalogs and unscoped RLS are covered.

Read endpoints moved into a cohesive list controller and query service, with structural response mapping through the existing MapStruct mappers. There is no client-side authorization or post-limit filtering. Accepted HR_ADMIN stage semantics remain unchanged. Canonical descriptions and both bundles document the additive envelope selection. No migration/event/infrastructure/HR-policy change. Full request verification includes the expanded reserved-file retry case and is running. No commit/deploy.

The complete request reactor after paging, private-file contract/signature checks and expanded upload retries passes 46 owner tests plus 3 shared contracts, zero failures/errors/skips (`/tmp/hrflow-uat-request-pages-verify.log`). All real request organization replay and publication regressions reran. This is the provider baseline before the separate submit-object and client harness work.

### UAT repair — submit input compatibility and actual client providers (2026-10-29)

The revised-version regression first rejected the client command object with 400 (`/tmp/hrflow-uat-request-submit-object-red.log`). The provider now compatibly accepts no body, a legacy approver array or a command object; none can override the server chain or put private notes into the minimal workflow event. The original input stays bound to its scoped receipt. All 5 selected revision/lifecycle cases pass (`/tmp/hrflow-uat-request-submit-object-green.log`).

The new `ClientProviderJourneyIT` passes (`/tmp/hrflow-uat-client-provider-journey.log`, 1 Failsafe case, zero failures/errors/skips). It launches the actual packaged request and notification services with restricted runtime credentials, real migrated PostgreSQL, real org.employee Kafka replay and actual RS256/JWKS verification. It runs both separately owned client harnesses: web 2 passed, native 2 passed, no skipped acceptance. Actual client repositories/adapters verify create/exact replay/body conflict, scoped pages, reviewed draft edit/stale412, canonical private PDF reservation/upload/completion/download, foreign404/anonymous401, submit, manager/HR inbox success, OWNER-only/EMPLOYEE-only inbox403, and provider preference ETags/stale412. Passwords/tokens are not printed; fixture tokens travel only through subprocess environment. Services stop afterward.

This is direct-provider HTTP acceptance. It does not prove gateway routing, browser cookie transport, real login onboarding or physical mobile behavior. Client files are now owned by the integration chat and later edits invalidate this snapshot; the final combined verifier must rerun. No deployment/persistent UAT accounts, commit or push.

### UAT repair — retain canonical material and date/time inputs (2026-10-29)

Both provider examples first lost canonical material/date fields (`/tmp/hrflow-uat-request-payload-contract-red.log`). All 7 selected provider/lifecycle/snapshot cases now pass (`/tmp/hrflow-uat-request-payload-contract-green.log`). Canonical items/materialId alias the old lines/materialItemId fields; actual active tenant material membership and unique positive quantities are enforced before persistence. Response detail retains both representations. Optional paired date/time fields persist in existing form_data JSON and return as top-level fields; invalid ranges return 422 without a committed request. Existing custom JSON keys and legacy formData remain. Material line persistence/read responsibilities moved into their own cohesive service; MapStruct still owns structural response conversion.

An intermediate new date case exposed Jackson's inferred-constructor missing primitive quantity failure (400). Nullable input with an explicit getter default now implements the existing canonical omitted quantity=1; the same new test passes. No attendance/leave/calendar adjustment is implied by recording a general-request form. No new HR policy, migration, event fields, paid services or framework. The additive named form-field schema and operation descriptions preserve the frozen provisional schemas and are synchronized to both clients. The client integration chat is extending real-provider harnesses; full request verification is running. No commit/deploy.

The complete typed/material payload checkpoint passes all 48 request owner tests plus 3 shared contracts, zero failures/errors/skips (`/tmp/hrflow-uat-request-payload-verify.log`). Canonical validation passes 181 operations, origin/main compatibility preserves existing wire definitions, both bundles match and dependency policy passes. The expanded actual-client material/date/time journey is prepared but not yet executed for this later source snapshot.

The final expanded three-provider client journey passes (`/tmp/hrflow-uat-expanded-client-provider-final.log`, one Failsafe case, web2/native2, zero skipped cases). Compared with the earlier GENERAL-only pass, both actual consumers additionally exercise canonical MATERIAL selection and reviewed quantity edit, REMOTE_WORK/LATE_ARRIVAL/EARLY_LEAVE date/time detail round-trip, and attendance correction create/detail ETag/current-manager paging/self-denial/stale precondition/approval/foreign-denial. Attendance uses explicit verified synthetic shift/current-manager fixtures; request organization references arrive through real Kafka. No gateway/login/device acceptance is inferred. The first expanded run failed a duplicate local variable in the native harness before loading; the client owner corrected it and separated correction dates to avoid the real interval-overlap guard. Earlier failed log remains `/tmp/hrflow-uat-expanded-client-provider-journey.log`. Current provider processes stop after both bounded harnesses.

### UAT repair — material-only handover (2026-10-29)

An approved OTHER request incorrectly accepted fulfilment and became FULFILLED. The new regression reproduced that transition (`/tmp/hrflow-uat-material-only-handover-red.log`, expected exception absent). Fulfilment now additionally requires MATERIAL, returning 422 before any status/version/outbox change. Both the new denial and existing material handover/retry case pass (`/tmp/hrflow-uat-material-only-handover-green.log`, 2 cases, zero failures/errors/skips). Existing HTTP schema/status families are preserved; no migration, client field, new role or event schema is needed. Broader request checks remain due; no commit/deploy.

### UAT repair — documented request roles only (2026-10-29)

A restricted HTTP regression reproduced a private request being returned to an unrelated ADMIN claimant (expected 403, actual 200; `/tmp/hrflow-uat-request-role-boundary-red.log`). Identity's actual role authority is EMPLOYEE, MANAGER, HR_ADMIN and OWNER; request policy no longer treats unrelated legacy ADMIN/HR names as privileged. OWNER alone still grants no HR review privilege. Cancellation remains the requester's action; fulfilment requires HR_ADMIN and MATERIAL. Existing positive handover fixtures now use the actual documented HR_ADMIN role. All 5 selected request payload/role/handover cases pass (`/tmp/hrflow-uat-request-role-boundary-green.log`, no failures/errors/skips). No role mapping was added to the issuer, no migration/contract changed, and no commit/deploy.
