# Business-flow evidence inventory

## Phase zero checkpoint

[IMPL] Inventory completed before writing the business-flow reference on 2026-10-23, source checkpoint 7141114. No server/Docker process was started and no secret file, key or token was read. Evidence comes from the read-only commands below; later tracing updates only coverage rows. No generated target/classes or test-only controller is treated as application behavior.

```bash
rg --files services libs infra ml/confidence-service -g '!*.env*' -g '!*.joblib'
rg -n '@(GetMapping|PostMapping|RequestMapping|KafkaListener|Scheduled)' services libs
rg -n '@Transactional|CREATE TABLE|\.send\(' services libs infra
```

[IMPL] A pathlib count of src/main and src/test Java files found the counts below; rg found mapping annotations only under src/test. Migration enumeration found no V*__*.sql and source SQL has no CREATE TABLE. These are bounded repository observations, not universal architectural guarantees.

## Modules

| Module | Status | Evidence |
| --- | --- | --- |
| `services/api-gateway` | partial infrastructure; business skeleton | [IMPL] 2 production Java files, 2 test files; Application and SecurityConfiguration only; no business controller/repository/listener/job/migration; file inventory command below |
| `services/identity-org` | partial infrastructure; business skeleton | [IMPL] 2 production Java files, 2 test files; Application and SecurityConfiguration only; no business controller/repository/listener/job/migration; file inventory command below |
| `services/attendance` | partial infrastructure; business skeleton | [IMPL] 2 production Java files, 2 test files; Application and SecurityConfiguration only; no business controller/repository/listener/job/migration; file inventory command below |
| `services/leave` | partial infrastructure; business skeleton | [IMPL] 2 production Java files, 2 test files; Application and SecurityConfiguration only; no business controller/repository/listener/job/migration; file inventory command below |
| `services/request` | partial infrastructure; business skeleton | [IMPL] 2 production Java files, 2 test files; Application and SecurityConfiguration only; no business controller/repository/listener/job/migration; file inventory command below |
| `services/notification` | partial infrastructure; business skeleton | [IMPL] 2 production Java files, 2 test files; Application and SecurityConfiguration only; no business controller/repository/listener/job/migration; file inventory command below |
| `services/reporting` | partial infrastructure; business skeleton | [IMPL] 2 production Java files, 2 test files; Application and SecurityConfiguration only; no business controller/repository/listener/job/migration; file inventory command below |
| `libs/hrflow-contracts` | partial contract | [IMPL] 1 Avro record and schema generation POM; file inventory command below |
| `libs/hrflow-event-starter` | skeleton | [IMPL] POM and application.yaml; no Java implementation; file inventory command below |
| `ml/confidence-service` | partial prototype; scoring substantial, HR integration absent | [IMPL] 5 confidence-package Python files, launcher, 2 test files, 2 training scripts and model metadata; file inventory command below |
| `web` | skeleton client | [IMPL] app/app.vue starter, no server routes found; file inventory command below |
| `mobile` | skeleton client | [IMPL] lib/main.dart counter and test/widget_test.dart; file inventory command below |
| `infra` | partial local configuration | [IMPL] Compose, six-schema init SQL, ten-topic creation script; no application deployment evidence; file inventory command below |

## HTTP endpoints

[IMPL] Explicit Python handlers are in ml/confidence-service/confidence/api.py#health, ml/confidence-service/confidence/api.py#info and ml/confidence-service/confidence/api.py#score. In-process inspection of app.routes (no HTTP server) additionally found the four FastAPI documentation paths below. Actual Java business controllers and gateway route entries found: zero; services/api-gateway/src/main/resources/application.yaml has routes: [].

| ID | Method | Route | Handler or source | Classification |
| --- | --- | --- | --- | --- |
| E01 | GET | `/health` | `confidence.api.health` | business/technical health |
| E02 | GET | `/model` | `confidence.api.info` | model metadata |
| E03 | POST | `/score` | `confidence.api.score` | scoring |
| E04 | GET, HEAD | `/openapi.json` | `FastAPI default openapi route` | technical docs |
| E05 | GET, HEAD | `/docs` | `FastAPI default swagger_ui_html route` | technical docs |
| E06 | GET, HEAD | `/docs/oauth2-redirect` | `FastAPI default swagger_ui_redirect route` | technical docs |
| E07 | GET, HEAD | `/redoc` | `FastAPI default redoc_html route` | technical docs |

[IMPL] Every Java application.yaml exposes health only on a loopback management listener with liveness/readiness enabled. Technical surfaces are GET /actuator/health, /actuator/health/liveness and /actuator/health/readiness on each management port (gateway 9080; business services 9101–9106). Actuator discovery and servlet error handling are framework surfaces, not business operations; their runtime registration was not tested in this read-only task. See each application.yaml and SecurityConfiguration.

[TEST] All seven SecurityConfigurationTests.ProbeController.probe methods map GET /__security_probe, /actuator/env and /actuator/health/liveness solely inside test slices. They are excluded from E01–E07 and must never be advertised as production APIs. The security tests describe anonymous denial, authenticated denial and a probe allowlist, not tenant authorization.

## Configured Java technical endpoint matrix

[IMPL] These twenty-one configured health surfaces are traced from each service application.yaml and its health allowlist; they are not application controller methods or live observations from this task. They are counted separately from the seven inspected Python route objects. Actuator discovery/servlet error handling remain framework surfaces with runtime registration not checked. (services/*/src/main/resources/application.yaml; per-service SecurityConfiguration)

| ID | Service / management port | Method and configured route | Workflow / coverage |
| --- | --- | --- | --- |
| J01H | api-gateway / 9080 | GET `/actuator/health` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J01L | api-gateway / 9080 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J01R | api-gateway / 9080 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J02H | identity-org / 9101 | GET `/actuator/health` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J02L | identity-org / 9101 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J02R | identity-org / 9101 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J03H | attendance / 9102 | GET `/actuator/health` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J03L | attendance / 9102 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J03R | attendance / 9102 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J04H | leave / 9103 | GET `/actuator/health` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J04L | leave / 9103 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J04R | leave / 9103 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J05H | request / 9104 | GET `/actuator/health` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J05L | request / 9104 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J05R | request / 9104 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J06H | notification / 9105 | GET `/actuator/health` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J06L | notification / 9105 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J06R | notification / 9105 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J07H | reporting / 9106 | GET `/actuator/health` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J07L | reporting / 9106 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical configuration traced; runtime not run |
| J07R | reporting / 9106 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical configuration traced; runtime not run |

## Migrations and tables

[IMPL] Each of api-gateway, identity-org, attendance, leave, request, notification and reporting has zero Flyway migrations and zero source CREATE TABLE statements. infra/docker/postgres-init.sql contains only CREATE SCHEMA for identity_org, attendance, leave, request, notification and reporting. No installed business table or constraint is established by source. Proposed SQL in docs is DESIGN only.

## Events, topics and jobs

[IMPL] The sole Avro record is libs/hrflow-contracts/events/attendance-checked-in.v1.avsc#AttendanceCheckedIn: eventId UUID-string, tenantId/employeeId strings, occurredAt timestamp-millis, source MANUAL/NUDGE/INFERRED and nullable reason. There is no producer-to-topic binding in Java source. libs/hrflow-contracts/pom.xml generates from its events directory.

| Topic from creation script | Policy | Part VI disposition |
| --- | --- | --- |
| `org.employee.v1` | compacted; six partitions; replication one | Listed; publisher/consumer not implemented |
| `org.shift.v1` | compacted; six partitions; replication one | Listed; publisher/consumer not implemented |
| `org.holiday.v1` | compacted; six partitions; replication one | Listed; publisher/consumer not implemented |
| `attendance.evidence.v1` | normal; six partitions; replication one | Listed; publisher/consumer not implemented |
| `attendance.session.v1` | normal; six partitions; replication one | Listed; publisher/consumer not implemented |
| `attendance.daily-status.v1` | normal; six partitions; replication one | Listed; publisher/consumer not implemented |
| `attendance.leave-conflict.v1` | normal; six partitions; replication one | Listed; publisher/consumer not implemented |
| `leave.request.v1` | normal; six partitions; replication one | Listed; publisher/consumer not implemented |
| `request.workflow.v1` | normal; six partitions; replication one | Listed; publisher/consumer not implemented |
| `time.shift-cutoff.v1` | normal; six partitions; replication one | Listed; publisher/consumer not implemented |

[IMPL] Topic names/policies are from infra/scripts/create-topic.sh, not proof that a broker contains them. Compose configures Kafka/Registry but does not create business topics. Production outbox publishers, inbox handlers, KafkaListener methods, Scheduled methods and transaction annotations found: zero. No retry/DLQ topic or job implementation was found. Kafka client defaults in services/*/application.yaml do not create those guarantees.

## Relevant behavior documents

[DESIGN] Current design starts with docs/Architecture.md and four approved INCOMING-ARCHITECTURE-V2 parts; docs/REST-API-DATABASE-DESIGN.md supplies API/data conventions. ADR 008 supersedes monolith/local-dispatch alternatives, while ADR 004 remains proposed policy. Historical CONTRACTS/ARCHITECTURE reference files supply older candidate routes/tables only, not active wire authority. The following is the complete pre-task documentation inventory, so historical context and weekly feature instructions are discoverable without silently treating every document as normative.

- `docs/Architecture.md`.
- `docs/DOCUMENTATION-CATALOG.md`.
- `docs/PLAN.md`.
- `docs/PROJECT-ANALYSIS.md`.
- `docs/README.md`.
- `docs/REST-API-DATABASE-DESIGN.md`.
- `docs/SERVICE-CONFIGURATION.md`.
- `docs/SOURCE-OF-TRUTH.md`.
- `docs/adr/001-SERVICE-AND-EVENT-BOUNDARIES.md`.
- `docs/adr/002-TENANT-AND-PERSISTENCE-ISOLATION.md`.
- `docs/adr/003-EVENT-DELIVERY-AND-CONTRACTS.md`.
- `docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md`.
- `docs/adr/005-DEPLOYMENT-AND-CLIENT-BASELINE.md`.
- `docs/adr/006-FINISH-LINE-MODULAR-MONOLITH.md`.
- `docs/adr/007-QUALITY-AND-TENANT-DEFENSE.md`.
- `docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md`.
- `docs/adr/README.md`.
- `docs/reference/ANALYSIS-REFERENCE-01.md`.
- `docs/reference/ARCHITECTURE-REFERENCE-01.md`.
- `docs/reference/ARCHITECTURE-REFERENCE-02.md`.
- `docs/reference/ARCHITECTURE-REFERENCE-03.md`.
- `docs/reference/CONTRACTS-REFERENCE-01.md`.
- `docs/reference/CONTRACTS-REFERENCE-02.md`.
- `docs/reference/CONTRACTS-REFERENCE-03.md`.
- `docs/reference/CONTRACTS-REFERENCE-04.md`.
- `docs/reference/CONTRACTS-REFERENCE-05.md`.
- `docs/reference/CONTRACTS-REFERENCE-06.md`.
- `docs/reference/CONTRACTS-REFERENCE-07.md`.
- `docs/reference/CONTRACTS-REFERENCE-08.md`.
- `docs/reference/CONTRACTS-REFERENCE-09.md`.
- `docs/reference/CONTRACTS-REFERENCE-10.md`.
- `docs/reference/CONTRACTS-REFERENCE-11.md`.
- `docs/reference/CONTRACTS-REFERENCE-12.md`.
- `docs/reference/CONTRACTS-REFERENCE-13.md`.
- `docs/reference/CONTRACTS-REFERENCE-14.md`.
- `docs/reference/CONTRACTS-REFERENCE-15.md`.
- `docs/reference/CONTRACTS-REFERENCE-16.md`.
- `docs/reference/INCOMING-ARCHITECTURE-V2-01.md`.
- `docs/reference/INCOMING-ARCHITECTURE-V2-02.md`.
- `docs/reference/INCOMING-ARCHITECTURE-V2-03.md`.
- `docs/reference/INCOMING-ARCHITECTURE-V2-04.md`.
- `docs/reference/PLAN-REFERENCE-01.md`.
- `docs/reference/README.md`.
- `docs/roadmap/00-PREPARATION/HARDWARE-AND-HOSTING-ASSESSMENT.md`.
- `docs/roadmap/00-PREPARATION/MIGRATION-PLAN.md`.
- `docs/roadmap/00-PREPARATION/PROJECT-BASELINE.md`.
- `docs/roadmap/00-PREPARATION/README.md`.
- `docs/roadmap/00-PREPARATION/REPOSITORY-AUDIT.md`.
- `docs/roadmap/01-FOUNDATION/BUILD-GUIDE-PART-2.md`.
- `docs/roadmap/01-FOUNDATION/BUILD-GUIDE-PART-3.md`.
- `docs/roadmap/01-FOUNDATION/BUILD-GUIDE-PART-4.md`.
- `docs/roadmap/01-FOUNDATION/BUILD-GUIDE-PART-5.md`.
- `docs/roadmap/01-FOUNDATION/BUILD-GUIDE-PART-6.md`.
- `docs/roadmap/01-FOUNDATION/BUILD-GUIDE-PART-7.md`.
- `docs/roadmap/01-FOUNDATION/BUILD-GUIDE-PART-8.md`.
- `docs/roadmap/01-FOUNDATION/BUILD-GUIDE.md`.
- `docs/roadmap/01-FOUNDATION/README.md`.
- `docs/roadmap/01-FOUNDATION/TROUBLESHOOTING.md`.
- `docs/roadmap/01-FOUNDATION/VERIFY-AND-GATE.md`.
- `docs/roadmap/02-IDENTITY-AND-HOST/ACCESS-SETUP.md`.
- `docs/roadmap/02-IDENTITY-AND-HOST/BUILD-GUIDE-PART-2.md`.
- `docs/roadmap/02-IDENTITY-AND-HOST/BUILD-GUIDE-PART-3.md`.
- `docs/roadmap/02-IDENTITY-AND-HOST/BUILD-GUIDE-PART-4.md`.
- `docs/roadmap/02-IDENTITY-AND-HOST/BUILD-GUIDE-PART-5.md`.
- `docs/roadmap/02-IDENTITY-AND-HOST/BUILD-GUIDE-PART-6.md`.
- `docs/roadmap/02-IDENTITY-AND-HOST/BUILD-GUIDE-PART-7.md`.
- `docs/roadmap/02-IDENTITY-AND-HOST/BUILD-GUIDE-PART-8.md`.
- `docs/roadmap/02-IDENTITY-AND-HOST/BUILD-GUIDE.md`.
- `docs/roadmap/02-IDENTITY-AND-HOST/README.md`.
- `docs/roadmap/02-IDENTITY-AND-HOST/TROUBLESHOOTING.md`.
- `docs/roadmap/02-IDENTITY-AND-HOST/VERIFY-AND-GATE.md`.
- `docs/roadmap/03-ORGANIZATION-AND-ADMIN/BUILD-GUIDE-PART-2.md`.
- `docs/roadmap/03-ORGANIZATION-AND-ADMIN/BUILD-GUIDE-PART-3.md`.
- `docs/roadmap/03-ORGANIZATION-AND-ADMIN/BUILD-GUIDE-PART-4.md`.
- `docs/roadmap/03-ORGANIZATION-AND-ADMIN/BUILD-GUIDE-PART-5.md`.
- `docs/roadmap/03-ORGANIZATION-AND-ADMIN/BUILD-GUIDE-PART-6.md`.
- `docs/roadmap/03-ORGANIZATION-AND-ADMIN/BUILD-GUIDE-PART-7.md`.
- `docs/roadmap/03-ORGANIZATION-AND-ADMIN/BUILD-GUIDE-PART-8.md`.
- `docs/roadmap/03-ORGANIZATION-AND-ADMIN/BUILD-GUIDE.md`.
- `docs/roadmap/03-ORGANIZATION-AND-ADMIN/PATTERN-LESSON.md`.
- `docs/roadmap/03-ORGANIZATION-AND-ADMIN/README.md`.
- `docs/roadmap/03-ORGANIZATION-AND-ADMIN/TROUBLESHOOTING.md`.
- `docs/roadmap/03-ORGANIZATION-AND-ADMIN/VERIFY-AND-GATE.md`.
- `docs/roadmap/04-ATTENDANCE-CORE/BUILD-GUIDE-PART-2.md`.
- `docs/roadmap/04-ATTENDANCE-CORE/BUILD-GUIDE-PART-3.md`.
- `docs/roadmap/04-ATTENDANCE-CORE/BUILD-GUIDE-PART-4.md`.
- `docs/roadmap/04-ATTENDANCE-CORE/BUILD-GUIDE-PART-5.md`.
- `docs/roadmap/04-ATTENDANCE-CORE/BUILD-GUIDE-PART-6.md`.
- `docs/roadmap/04-ATTENDANCE-CORE/BUILD-GUIDE-PART-7.md`.
- `docs/roadmap/04-ATTENDANCE-CORE/BUILD-GUIDE-PART-8.md`.
- `docs/roadmap/04-ATTENDANCE-CORE/BUILD-GUIDE.md`.
- `docs/roadmap/04-ATTENDANCE-CORE/README.md`.
- `docs/roadmap/04-ATTENDANCE-CORE/TROUBLESHOOTING.md`.
- `docs/roadmap/04-ATTENDANCE-CORE/VERIFY-AND-GATE.md`.
- `docs/roadmap/05-OUTBOX-AND-NOTIFICATIONS/BUILD-GUIDE-PART-2.md`.
- `docs/roadmap/05-OUTBOX-AND-NOTIFICATIONS/BUILD-GUIDE-PART-3.md`.
- `docs/roadmap/05-OUTBOX-AND-NOTIFICATIONS/BUILD-GUIDE-PART-4.md`.
- `docs/roadmap/05-OUTBOX-AND-NOTIFICATIONS/BUILD-GUIDE-PART-5.md`.
- `docs/roadmap/05-OUTBOX-AND-NOTIFICATIONS/BUILD-GUIDE-PART-6.md`.
- `docs/roadmap/05-OUTBOX-AND-NOTIFICATIONS/BUILD-GUIDE-PART-7.md`.
- `docs/roadmap/05-OUTBOX-AND-NOTIFICATIONS/BUILD-GUIDE-PART-8.md`.
- `docs/roadmap/05-OUTBOX-AND-NOTIFICATIONS/BUILD-GUIDE.md`.
- `docs/roadmap/05-OUTBOX-AND-NOTIFICATIONS/README.md`.
- `docs/roadmap/05-OUTBOX-AND-NOTIFICATIONS/TROUBLESHOOTING.md`.
- `docs/roadmap/05-OUTBOX-AND-NOTIFICATIONS/VERIFY-AND-GATE.md`.
- `docs/roadmap/06-LEAVE-WORKFLOW/BUILD-GUIDE-PART-2.md`.
- `docs/roadmap/06-LEAVE-WORKFLOW/BUILD-GUIDE-PART-3.md`.
- `docs/roadmap/06-LEAVE-WORKFLOW/BUILD-GUIDE-PART-4.md`.
- `docs/roadmap/06-LEAVE-WORKFLOW/BUILD-GUIDE-PART-5.md`.
- `docs/roadmap/06-LEAVE-WORKFLOW/BUILD-GUIDE-PART-6.md`.
- `docs/roadmap/06-LEAVE-WORKFLOW/BUILD-GUIDE-PART-7.md`.
- `docs/roadmap/06-LEAVE-WORKFLOW/BUILD-GUIDE-PART-8.md`.
- `docs/roadmap/06-LEAVE-WORKFLOW/BUILD-GUIDE.md`.
- `docs/roadmap/06-LEAVE-WORKFLOW/README.md`.
- `docs/roadmap/06-LEAVE-WORKFLOW/TROUBLESHOOTING.md`.
- `docs/roadmap/06-LEAVE-WORKFLOW/VERIFY-AND-GATE.md`.
- `docs/roadmap/07-REQUESTS-AND-APPROVALS/BUILD-GUIDE-PART-2.md`.
- `docs/roadmap/07-REQUESTS-AND-APPROVALS/BUILD-GUIDE-PART-3.md`.
- `docs/roadmap/07-REQUESTS-AND-APPROVALS/BUILD-GUIDE-PART-4.md`.
- `docs/roadmap/07-REQUESTS-AND-APPROVALS/BUILD-GUIDE-PART-5.md`.
- `docs/roadmap/07-REQUESTS-AND-APPROVALS/BUILD-GUIDE-PART-6.md`.
- `docs/roadmap/07-REQUESTS-AND-APPROVALS/BUILD-GUIDE-PART-7.md`.
- `docs/roadmap/07-REQUESTS-AND-APPROVALS/BUILD-GUIDE-PART-8.md`.
- `docs/roadmap/07-REQUESTS-AND-APPROVALS/BUILD-GUIDE.md`.
- `docs/roadmap/07-REQUESTS-AND-APPROVALS/PATTERN-LESSON.md`.
- `docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md`.
- `docs/roadmap/07-REQUESTS-AND-APPROVALS/TROUBLESHOOTING.md`.
- `docs/roadmap/07-REQUESTS-AND-APPROVALS/VERIFY-AND-GATE.md`.
- `docs/roadmap/08-WEB-AND-MOBILE-INTEGRATION/BUILD-GUIDE-PART-2.md`.
- `docs/roadmap/08-WEB-AND-MOBILE-INTEGRATION/BUILD-GUIDE-PART-3.md`.
- `docs/roadmap/08-WEB-AND-MOBILE-INTEGRATION/BUILD-GUIDE-PART-4.md`.
- `docs/roadmap/08-WEB-AND-MOBILE-INTEGRATION/BUILD-GUIDE-PART-5.md`.
- `docs/roadmap/08-WEB-AND-MOBILE-INTEGRATION/BUILD-GUIDE-PART-6.md`.
- `docs/roadmap/08-WEB-AND-MOBILE-INTEGRATION/BUILD-GUIDE-PART-7.md`.
- `docs/roadmap/08-WEB-AND-MOBILE-INTEGRATION/BUILD-GUIDE-PART-8.md`.
- `docs/roadmap/08-WEB-AND-MOBILE-INTEGRATION/BUILD-GUIDE.md`.
- `docs/roadmap/08-WEB-AND-MOBILE-INTEGRATION/README.md`.
- `docs/roadmap/08-WEB-AND-MOBILE-INTEGRATION/TROUBLESHOOTING.md`.
- `docs/roadmap/08-WEB-AND-MOBILE-INTEGRATION/VERIFY-AND-GATE.md`.
- `docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/BUILD-GUIDE-PART-2.md`.
- `docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/BUILD-GUIDE-PART-3.md`.
- `docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/BUILD-GUIDE-PART-4.md`.
- `docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/BUILD-GUIDE-PART-5.md`.
- `docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/BUILD-GUIDE-PART-6.md`.
- `docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/BUILD-GUIDE-PART-7.md`.
- `docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/BUILD-GUIDE-PART-8.md`.
- `docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/BUILD-GUIDE.md`.
- `docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/PATTERN-LESSON.md`.
- `docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md`.
- `docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/TROUBLESHOOTING.md`.
- `docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/VERIFY-AND-GATE.md`.
- `docs/roadmap/10-OPERATIONS-AND-RESTORE/BUILD-GUIDE-PART-2.md`.
- `docs/roadmap/10-OPERATIONS-AND-RESTORE/BUILD-GUIDE-PART-3.md`.
- `docs/roadmap/10-OPERATIONS-AND-RESTORE/BUILD-GUIDE-PART-4.md`.
- `docs/roadmap/10-OPERATIONS-AND-RESTORE/BUILD-GUIDE-PART-5.md`.
- `docs/roadmap/10-OPERATIONS-AND-RESTORE/BUILD-GUIDE-PART-6.md`.
- `docs/roadmap/10-OPERATIONS-AND-RESTORE/BUILD-GUIDE-PART-7.md`.
- `docs/roadmap/10-OPERATIONS-AND-RESTORE/BUILD-GUIDE-PART-8.md`.
- `docs/roadmap/10-OPERATIONS-AND-RESTORE/BUILD-GUIDE.md`.
- `docs/roadmap/10-OPERATIONS-AND-RESTORE/HOST-RUNBOOK.md`.
- `docs/roadmap/10-OPERATIONS-AND-RESTORE/README.md`.
- `docs/roadmap/10-OPERATIONS-AND-RESTORE/TROUBLESHOOTING.md`.
- `docs/roadmap/10-OPERATIONS-AND-RESTORE/VERIFY-AND-GATE.md`.
- `docs/roadmap/11-HARDENING-AND-PILOT/BUILD-GUIDE-PART-2.md`.
- `docs/roadmap/11-HARDENING-AND-PILOT/BUILD-GUIDE-PART-3.md`.
- `docs/roadmap/11-HARDENING-AND-PILOT/BUILD-GUIDE-PART-4.md`.
- `docs/roadmap/11-HARDENING-AND-PILOT/BUILD-GUIDE-PART-5.md`.
- `docs/roadmap/11-HARDENING-AND-PILOT/BUILD-GUIDE-PART-6.md`.
- `docs/roadmap/11-HARDENING-AND-PILOT/BUILD-GUIDE-PART-7.md`.
- `docs/roadmap/11-HARDENING-AND-PILOT/BUILD-GUIDE-PART-8.md`.
- `docs/roadmap/11-HARDENING-AND-PILOT/BUILD-GUIDE.md`.
- `docs/roadmap/11-HARDENING-AND-PILOT/README.md`.
- `docs/roadmap/11-HARDENING-AND-PILOT/TROUBLESHOOTING.md`.
- `docs/roadmap/11-HARDENING-AND-PILOT/VERIFY-AND-GATE.md`.
- `docs/roadmap/12-RELEASE-AND-DEMONSTRATION/BUILD-GUIDE-PART-2.md`.
- `docs/roadmap/12-RELEASE-AND-DEMONSTRATION/BUILD-GUIDE-PART-3.md`.
- `docs/roadmap/12-RELEASE-AND-DEMONSTRATION/BUILD-GUIDE-PART-4.md`.
- `docs/roadmap/12-RELEASE-AND-DEMONSTRATION/BUILD-GUIDE-PART-5.md`.
- `docs/roadmap/12-RELEASE-AND-DEMONSTRATION/BUILD-GUIDE-PART-6.md`.
- `docs/roadmap/12-RELEASE-AND-DEMONSTRATION/BUILD-GUIDE-PART-7.md`.
- `docs/roadmap/12-RELEASE-AND-DEMONSTRATION/BUILD-GUIDE-PART-8.md`.
- `docs/roadmap/12-RELEASE-AND-DEMONSTRATION/BUILD-GUIDE.md`.
- `docs/roadmap/12-RELEASE-AND-DEMONSTRATION/README.md`.
- `docs/roadmap/12-RELEASE-AND-DEMONSTRATION/TROUBLESHOOTING.md`.
- `docs/roadmap/12-RELEASE-AND-DEMONSTRATION/VERIFY-AND-GATE.md`.
- `docs/roadmap/ACTUAL-VALIDATION.md`.
- `docs/roadmap/API-GUIDELINES.md`.
- `docs/roadmap/ARCHITECTURE-REVIEW-V2.md`.
- `docs/roadmap/CHANGELOG-V2.md`.
- `docs/roadmap/CODE-REVIEW-CHECKLIST.md`.
- `docs/roadmap/DAILY-COMMANDS.md`.
- `docs/roadmap/DECISION-REGISTER.md`.
- `docs/roadmap/DEFINITION-OF-DONE.md`.
- `docs/roadmap/DRAFT-ID-MAP.md`.
- `docs/roadmap/ENGINEERING-STANDARDS.md`.
- `docs/roadmap/FINALIZATION-ID-MAP.md`.
- `docs/roadmap/FOLDER-TREE.md`.
- `docs/roadmap/FORMAT-GUIDE.md`.
- `docs/roadmap/FORMAT-SELFTEST.md`.
- `docs/roadmap/GIT-SAFETY.md`.
- `docs/roadmap/GLOBAL-CHECKLIST.md`.
- `docs/roadmap/GLOSSARY.md`.
- `docs/roadmap/GOLDEN-SLICE.md`.
- `docs/roadmap/LINTER-PROOFS.md`.
- `docs/roadmap/MILESTONE-MAP.md`.
- `docs/roadmap/PATTERNS-LEARNING-PATH.md`.
- `docs/roadmap/PROGRESS-V2.md`.
- `docs/roadmap/PROGRESS.md`.
- `docs/roadmap/QUALITY-COVERAGE.md`.
- `docs/roadmap/README.md`.
- `docs/roadmap/RISK-REGISTER.md`.
- `docs/roadmap/ROADMAP-AUDIT.md`.
- `docs/roadmap/ROADMAP.md`.
- `docs/roadmap/SECURITY-BASELINE.md`.
- `docs/roadmap/START-HERE.md`.
- `docs/roadmap/STEP-ID-MAP.md`.
- `docs/roadmap/STUCK-PROTOCOL.md`.
- `docs/roadmap/TEST-STRATEGY.md`.
- `docs/roadmap/TOOLCHAIN-REFERENCE.md`.
- `docs/roadmap/UPGRADE-PLAN.md`.
- `docs/roadmap/V2-REMAINING-REFERENCES.md`.
- `docs/roadmap/V2-VALIDATION-LOG.md`.
- `docs/roadmap/WEEKLY-STATUS-TEMPLATE.md`.
- `docs/roadmap/YOU-ARE-HERE.md`.
- `docs/roadmap/primers/ADAPTER.md`.
- `docs/roadmap/primers/ARCHITECTURE-TESTS.md`.
- `docs/roadmap/primers/ATTACHMENT-SECURITY.md`.
- `docs/roadmap/primers/BACKUPS-RESTORES.md`.
- `docs/roadmap/primers/BROWSER-CSRF.md`.
- `docs/roadmap/primers/BUILDER.md`.
- `docs/roadmap/primers/CHAIN-OF-RESPONSIBILITY.md`.
- `docs/roadmap/primers/CI-SCANS.md`.
- `docs/roadmap/primers/CODE-REVIEW.md`.
- `docs/roadmap/primers/CONTRACT-TESTS.md`.
- `docs/roadmap/primers/COVERAGE.md`.
- `docs/roadmap/primers/DATABASE-LOCKS.md`.
- `docs/roadmap/primers/DECORATOR.md`.
- `docs/roadmap/primers/DEPENDENCY-INJECTION.md`.
- `docs/roadmap/primers/DOCKER-COMPOSE.md`.
- `docs/roadmap/primers/EXACT-BALANCES.md`.
- `docs/roadmap/primers/FACTORY.md`.
- `docs/roadmap/primers/FLYWAY.md`.
- `docs/roadmap/primers/GIT.md`.
- `docs/roadmap/primers/GRACEFUL-SHUTDOWN.md`.
- `docs/roadmap/primers/IDEMPOTENCY.md`.
- `docs/roadmap/primers/JWT-REFRESH.md`.
- `docs/roadmap/primers/KAFKA-BASICS.md`.
- `docs/roadmap/primers/MULTI-TENANCY.md`.
- `docs/roadmap/primers/OBSERVABILITY.md`.
- `docs/roadmap/primers/OFFLINE-QUEUE.md`.
- `docs/roadmap/primers/OPENAPI.md`.
- `docs/roadmap/primers/PROBLEM-DETAILS.md`.
- `docs/roadmap/primers/REACTIVE-GATEWAY.md`.
- `docs/roadmap/primers/README.md`.
- `docs/roadmap/primers/REPOSITORY.md`.
- `docs/roadmap/primers/REVERSE-PROXY-HTTPS.md`.
- `docs/roadmap/primers/ROW-LEVEL-SECURITY.md`.
- `docs/roadmap/primers/SEMANTIC-RELEASES.md`.
- `docs/roadmap/primers/SPECIFICATION.md`.
- `docs/roadmap/primers/SPRING-JDBC.md`.
- `docs/roadmap/primers/STATE-MACHINES.md`.
- `docs/roadmap/primers/STATIC-ANALYSIS.md`.
- `docs/roadmap/primers/STRATEGY.md`.
- `docs/roadmap/primers/TEMPLATE-METHOD.md`.
- `docs/roadmap/primers/TEST-FIRST.md`.
- `docs/roadmap/primers/TESTCONTAINERS.md`.
- `docs/roadmap/primers/TRANSACTIONAL-OUTBOX.md`.
- `docs/roadmap/primers/TUNNELS.md`.
- `docs/roadmap/runbooks/DEPLOY.md`.
- `docs/roadmap/runbooks/DISK-FULL.md`.
- `docs/roadmap/runbooks/HOST-REBOOT.md`.
- `docs/roadmap/runbooks/INTERNET-DOWN.md`.
- `docs/roadmap/runbooks/README.md`.
- `docs/roadmap/runbooks/RESTORE.md`.
- `docs/roadmap/runbooks/ROLLBACK.md`.
- `docs/roadmap/runbooks/SCRIPT-INTERFACE.md`.
- `docs/roadmap/tools/README.md`.
- `docs/validation/README.md`.

## Relevant tests and clients

[TEST] Each service has src/test/java/com/hrflow/PACKAGE/config/SecurityConfigurationTests.java (three security checks) and EnvironmentConfigurationTests.java (property import/precedence). The package is gateway, identityorg, attendance, leave, request, notification or reporting. These are the complete fourteen Java test source files, not business invariant tests.

[TEST] ml/confidence-service/tests/test_service.py covers health, present/phone-left/mock-location scoring and invalid input. tests/test_launcher.py covers defaults, four invalid ports, launcher loading and real-file/process precedence. mobile/test/widget_test.dart is a counter starter test; no HR behavior is exercised. No Nuxt HR test or business UI was found in web/app/app.vue or mobile/lib/main.dart.

## Coverage checklist

[IMPL] Phase zero originally marked every row not traced; domain tracing is now complete for the discoverable source/design. Coverage below records the final disposition. Traced means the investigation followed all discovered source/design, including explicit missing implementation. Partial is reserved for a branch whose evidence could not be fully inspected; it is not a product-completion rating. All listed source roots were inspected; unresolved design policy is called UNKNOWN inside its workflow.

| Module or endpoint | traced | partial | not traced | Workflow / reason |
| --- | --- | --- | --- | --- |
| `services/api-gateway` | Yes | No | No | [IMPL] BF-001/002/039; routing/JWT business gap identified; source inventory above |
| `services/identity-org` | Yes | No | No | [IMPL] BF-001–006/040; business flows absent; source inventory above |
| `services/attendance` | Yes | No | No | [IMPL] BF-007–013; business flows absent; source inventory above |
| `services/leave` | Yes | No | No | [IMPL] BF-014–020; business flows absent; source inventory above |
| `services/request` | Yes | No | No | [IMPL] BF-021–026; business flows absent; source inventory above |
| `services/notification` | Yes | No | No | [IMPL] BF-027/028; business flows absent; source inventory above |
| `services/reporting` | Yes | No | No | [IMPL] BF-029–031; business flows absent; source inventory above |
| `libs/hrflow-contracts` | Yes | No | No | [IMPL] BF-035/036; one schema, no topic binding; source inventory above |
| `libs/hrflow-event-starter` | Yes | No | No | [IMPL] BF-035/036; configuration only; source inventory above |
| `ml/confidence-service` | Yes | No | No | [IMPL] BF-032–034; isolated prototype traced; source inventory above |
| `web` | Yes | No | No | [IMPL] Starter only; no HR operation found; source inventory above |
| `mobile` | Yes | No | No | [IMPL] Counter only; no HR operation found; source inventory above |
| `infra` | Yes | No | No | [IMPL] BF-035–039; scripted/configured, no runtime claim; source inventory above |
| E01 `/health` | Yes | No | No | [IMPL] BF-033; API-MAP.md |
| E02 `/model` | Yes | No | No | [IMPL] BF-033; API-MAP.md |
| E03 `/score` | Yes | No | No | [IMPL] BF-032; API-MAP.md |
| E04 `/openapi.json` | Yes | No | No | [IMPL] BF-039 technical; API-MAP.md |
| E05 `/docs` | Yes | No | No | [IMPL] BF-039 technical; API-MAP.md |
| E06 `/docs/oauth2-redirect` | Yes | No | No | [IMPL] BF-039 technical; API-MAP.md |
| E07 `/redoc` | Yes | No | No | [IMPL] BF-039 technical; API-MAP.md |
| J01H `api-gateway/actuator/health` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J01L `api-gateway/actuator/health/liveness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J01R `api-gateway/actuator/health/readiness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J02H `identity-org/actuator/health` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J02L `identity-org/actuator/health/liveness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J02R `identity-org/actuator/health/readiness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J03H `attendance/actuator/health` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J03L `attendance/actuator/health/liveness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J03R `attendance/actuator/health/readiness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J04H `leave/actuator/health` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J04L `leave/actuator/health/liveness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J04R `leave/actuator/health/readiness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J05H `request/actuator/health` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J05L `request/actuator/health/liveness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J05R `request/actuator/health/readiness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J06H `notification/actuator/health` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J06L `notification/actuator/health/liveness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J06R `notification/actuator/health/readiness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J07H `reporting/actuator/health` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J07L `reporting/actuator/health/liveness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
| J07R `reporting/actuator/health/readiness` | Yes | No | No | [IMPL] BF-039 technical; configured surface, not live test |
