# Analysis reference 01

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

Reviewed: 22 October 2026. Scope: current working tree, including staged, modified, and untracked source files. This is a code and documentation assessment, not a production runtime audit. Existing application code was not changed.

> **Note:** Historical reconnaissance snapshot: the documentation suite was subsequently expanded. Empty-document and React-plan observations below describe the earlier review state, not the revised specification. See [current architecture](../Architecture.MD).

## 1. Overall assessment

HRFlow has a detailed product and architecture plan, an initial Java multi-service skeleton, local infrastructure definitions, a default Nuxt application, and a standalone Python attendance-confidence prototype. It does not yet implement an end-to-end HR workflow.

The largest gap is between the architecture described in `docs/Plan.md` and the executable backend. All seven Java services contain only their Spring Boot entry-point class. There are no business controllers, entities, repositories, migrations, Kafka listeners, authentication flows, or domain services. The shared event starter has dependencies and configuration but no implementation. The Python service is substantially further developed than the core HR platform.

The next milestone should be one authenticated, tenant-isolated attendance flow with persistent data, transactional event publication, and a visible web result. Developing all seven services in parallel would multiply integration work before proving the central workflow.

## 2. Repository structure and ownership

```text
hrflow/
  pom.xml                         Maven aggregator and parent for libraries
  services/
    api-gateway/                  Spring Cloud Gateway MVC dependency; entry point
    identity-org/                 Entry point and generated dependency setup
    attendance/                   Entry point plus PostgreSQL/Kafka configuration
    leave/                        Entry point and generated dependency setup
    request/                      Entry point and generated dependency setup
    notification/                 Entry point and generated dependency setup
    reporting/                    Entry point and generated dependency setup
  libs/
    hrflow-contracts/             One Avro schema and generation configuration
    hrflow-event-starter/         Dependencies and Kafka YAML; no Java source
  web/                            Nuxt/Vue starter
  ml/confidence-service/           FastAPI scoring API, rules, training, tests, model
  infra/
    docker/                       PostgreSQL, Kafka, UI, Schema Registry
    scripts/                      Topic creation script
  docs/                           Detailed plan plus empty design placeholders
  .github/workflows/              Empty
```


This structure expresses intended ownership clearly. However, directory boundaries alone do not establish service isolation. There are no database roles, application contracts, or domain implementations enforcing those boundaries yet. Flutter source, Terraform, Kubernetes manifests, and CI workflows are absent.

## 3. Product model and intended workflows

The plan describes a multi-tenant HR platform centered on attendance, with organization data, leave approval, material/special requests, notifications, and reports. Payroll and recruitment are explicitly outside scope.

The planned check-in flow is well oriented toward low latency: attendance reads local employee and shift projections, writes evidence, session, and outbox rows in one database transaction, then returns without waiting for downstream services. Notification and reporting consume published events later. This avoids making user success depend on notification availability.

The leave workflow is a choreographed saga: approved leave reaches attendance; attendance detects conflicts; leave moves a conflicted request to review. This requires explicit transition rules, event ordering, duplicate handling, and reconciliation. None is implemented yet.

The plan's attendance precedence is holiday, approved leave, manual, inferred, absent. This needs clarification: approved leave overriding a manual check-in must be reconciled with the separate statement that manual records always win. Preserve raw evidence independently of the calculated daily status so corrections and conflicts remain auditable.

Other unresolved domain decisions include overnight shifts, employee/site time zones, daylight-saving boundaries, multiple sessions in a day, shift reassignment effective dates, partial-day leave, and whether a correction recalculates historical reports. `workDate` must have a defined relationship to shift-local dates rather than server calendar dates.

## 4. Implementation compared with the plan

| Area | Planned | Present in source |
|---|---|---|
| Identity | Tenants, JWT, RBAC, employees, shifts, holidays | Application entry point and dependencies |
| Gateway | Routing, authentication, tenant context, rate limiting | MVC gateway dependency; no routes or security policy |
| Attendance | Check-in/out, status, evidence, corrections, inference | Entry point and datasource/Kafka configuration |
| Leave and requests | Balances, approval workflows, conflict handling | Entry points and dependencies |
| Notification | Push/email, in-app inbox, SSE | Entry point and dependencies |
| Reporting | Projections, dashboards, CSV | Entry point and dependencies |
| Event foundation | Outbox, inbox, retries, DLQ, correlation | Library skeleton and one Avro schema |
| Web | React, Tailwind, shadcn/ui, feature screens | Nuxt/Vue welcome screen |
| Mobile | Flutter, biometrics, offline queue | No mobile source in this repository |
| ML | Stretch feature, evidence confidence | Functional standalone prototype with five passing tests |
| Delivery | CI, cloud infrastructure, deployment | Local dependency Compose file only |

No implemented path currently connects web → gateway → attendance → database → Kafka → reporting. Therefore the plan's latency, recovery, battery, and load targets are targets, not measured capabilities.

## 5. Concrete configuration and integration findings

### High priority: infrastructure connectivity

1. `infra/docker/docker-compose.yaml` mounts `./infra/docker/postgres-init.sql`. With the Compose file in `infra/docker`, the default relative resolution points beneath that directory again. Use a path relative to the Compose file, such as `./postgres-init.sql`, or explicitly establish a different project directory. Otherwise initialization will not mount the intended SQL file.
1. Schema Registry uses `kafka:19092`, but Kafka declares listeners at 9092, 29092, and 9093. The existing internal broker address is `kafka:29092`. Registry startup cannot reach the broker at its configured port.
1. Compose starts infrastructure only. It does not build or launch Java services, web, or the confidence service. A successful Compose syntax check cannot prove HRFlow is runnable.
1. Topic creation is manual and Kafka disables automatic topic creation. The topic script is not wired into a readiness-aware initialization flow. Retry and DLQ topics described in the plan are not provisioned.

### High priority: backend configuration and build consistency

1. Six `application.yaml` files contain properties syntax such as `spring.application.name=leave`. These are not YAML mappings defining the intended Spring property and may fail configuration loading. Attendance is the only service with a substantive YAML mapping.
1. The root POM uses Spring Boot parent 4.1.1; service POMs independently use 4.0.8. Services are reactor modules but do not inherit `hrflow-parent`, so its dependency management and shared properties do not govern them. The root README also declares a different version string, while the plan describes Boot 3. Resolve one supported version strategy explicitly.
1. No service depends on `hrflow-event-starter` or `hrflow-contracts`. The shared event setup therefore cannot affect service behavior in its current state.
1. The Avro plugin reads `${project.basedir}/../../contracts/events`, but the actual schema resides in `libs/hrflow-contracts/events`. Generation is pointed at the wrong directory.
1. The active shell provides Java 17 while the project targets Java 21. Maven `validate` succeeds because it does not compile Java. A full backend build was not established by this review.
1. Only attendance defines a service-specific server port and datasource. Other services lack the runtime configuration needed for the planned shared local environment; web-enabled services would otherwise compete for default ports when run together.

### Architectural and security gaps

1. `hrflow-event-starter` has no outbox relay, inbox handling, auto-configuration classes, retries, or correlation propagation. A resource named `application.yaml` in a library is not an adequate explicit starter configuration contract, especially once services have their own configuration files.
1. Schema selection through `hikari.schema` is a default search context, not authorization isolation. All schemas are created under a shared database setup; separate service credentials and grants are needed to enforce ownership. Tenant isolation within each service remains a separate requirement.
1. Security dependencies do not implement tenant identity, JWT validation, or authorization rules. The gateway has no configured security boundary. Business services need their own trusted tenant and actor context, including object-level authorization.
1. Prometheus is listed in attendance's endpoint exposure configuration, but the reviewed attendance POM does not include the Prometheus registry dependency. Endpoint exposure alone does not establish metrics export.

## 6. Event contract design

The single schema, `attendance-checked-in.v1.avsc`, contains event ID, tenant ID, employee ID, occurrence time, source, and optional reason. It is a useful starting record but does not match the plan's generic envelope: type, aggregate ID, correlation ID, and payload are absent. No producer or consumer uses it yet.

The schema models a check-in fact while the provisioned attendance topics model evidence, session, and daily status. Define which topic carries the schema and whether a check-in is a distinct event or a session transition. Otherwise topic names and contracts will diverge before integration begins.

The partition-key rule also needs refinement. `tenantId:employeeId` makes sense for employee streams, but shifts and holidays are not employee aggregates. Compacted topics retain the latest value per key, so their keys must identify the reference entity, such as tenant plus shift ID. One universal employee key is unsuitable for the complete catalog.

Before adding producers, define event time versus effective time, aggregate revision, deletion/tombstone behavior, schema compatibility policy, and projection rebuild procedures. Inbox deduplication prevents duplicate application; it does not by itself prevent a stale event from overwriting a newer projection.

The outbox must publish at least once safely: claim rows, publish, acknowledge broker success, then mark delivery. A crash after broker acknowledgment can still produce a duplicate, which is why consumer deduplication remains necessary. Specify retry exhaustion, DLQ ownership, and operational replay procedures alongside the implementation.

## 7. Python confidence service

This is the only component with meaningful application behavior and tests. FastAPI exposes `/health`, `/model`, and `/score`. Pydantic validates numeric ranges. The score uses twelve evidence features and returns confidence, decision, source, model version, and feature effects. A mock-location flag forces confidence to zero. Without artifact files, the service falls back to a weighted rules score.

Training compares logistic regression and histogram gradient boosting using cross-validation, prefers logistic regression unless boosting improves AUC by more than 0.01, calibrates the chosen classifier, selects an automatic-inference threshold, and writes model metadata. This is a sensible prototype pipeline with centralized feature order.

Important limitations:

- **Evaluation leakage:** `training/train.py` chooses the automatic threshold using `X_te/y_te` and reports automatic precision on the same data. Those metrics do not constitute an untouched final evaluation of the decision policy. Use separate training, calibration/threshold-selection, and final evaluation data.
- **Threshold fallback:** when no threshold meets target precision, training uses 0.95 anyway. That value has no established precision guarantee. Disable automatic inference when validation does not justify a threshold.
- **Threshold rounding:** metrics use the unrounded threshold, while saved metadata rounds it to three decimals. Evaluate the exact persisted policy.
- **Artifact evidence:** metadata reports 4,000 rows, ROC-AUC 0.9437, automatic precision 0.9669, and automatic coverage 0.785. The repository provides a synthetic generator and demo instructions, but metadata does not record dataset provenance. These numbers do not establish real employee attendance accuracy.
- **Dependency reproducibility:** Python requirements use unbounded minimum versions. Training locally under Python 3.14 and serving from a Python 3.12 Docker image with freshly resolved packages creates serialization compatibility risk. Record the training environment and pin compatible serving dependencies.
- **Fallback is partial:** missing files trigger rules mode; corrupt or incompatible artifact loading and prediction errors do not. The comment describing a safety net overstates current behavior.
- **Rules differ:** direct `rules_score` returns 0.05 for spoofing while the model wrapper returns 0.0. The public API uses the wrapper, but the discrepancy should be explicit or unified.
- **Weak identity validation:** tenant and employee IDs accept arbitrary strings, and `workDate` is an unrestricted string. There is no authentication or authorization on these endpoints.
- **Limited explanations:** model effects are baseline occlusion differences, not causal explanations. Rules-only responses have no explanatory reasons.
- **Testing scope:** tests cover health, one positive case, a phone-left case, spoof blocking, and a single invalid input. They do not explicitly test missing-model mode, damaged artifacts, metadata compatibility, or threshold selection failure.

The service is not connected to attendance, Kafka, or Compose. Integrating it through synchronous service-to-service HTTP would contradict the plan's Kafka-only rule. Choose an asynchronous evidence/result contract, an embedded rules path, or a documented architectural exception before integration. Keep inference outside the manual check-in transaction.

## 8. Documentation quality

`docs/Plan.md` is the substantive design document. It defines scope, ownership, resilience principles, targets, and a demo sequence. Its explicit cut line and failure-oriented tests are useful.

`docs/Achitecture.MD` and `docs/RestAPI-Database-Design.md` are empty. `docs/adr` and `.github/workflows` are empty. The root README is too short to reproduce the environment, and the web README is generic Nuxt starter documentation.

Decisions requiring reconciliation are React versus Nuxt/Vue, Boot 3 versus the two Boot 4 parent versions, JSON Schema/AsyncAPI versus Avro/Schema Registry, and how a separate ML service fits Kafka-only communication. Record actual decisions in ADRs and distinguish planned capabilities from implemented ones.

The API/database document should next define tenant and actor context, endpoints and error shape, pagination, idempotency behavior, per-service tables, unique constraints, approval transitions, outbox/inbox structure, retention, and migration ownership. These details are prerequisites for coherent implementation rather than documentation polish.

## 9. Verification performed

| Check | Result | Meaning |
|---|---|---|
| Python: `.venv/bin/python -m pytest -q` from confidence-service | 5 passed, 1 dependency deprecation warning | Existing API examples work with the local model/environment |
| Python tests invoked from repository root | Import collection failed | Imports depend on service working directory; documented service-local invocation passes |
| Web: `npm run build` | Passed | Current Nuxt starter builds for production |
| Maven: `mvn -o validate` | Passed for all 10 reactor projects | POM/reactor validation only; no Java compilation or service startup |
| Docker Compose: `config --quiet` | Passed | Compose syntax valid; connectivity and mounted initialization not verified |
| Topic script: `bash -n` | Passed | Shell syntax valid; broker provisioning not executed |

Containers were not started, cloud resources were not provisioned, and no load, security, tenant-isolation, or end-to-end tests were run. The existing Git working tree contained substantial uncommitted work before review; conclusions apply to that working tree rather than a clean committed release.

## 10. Recommended implementation sequence

1. **Make the foundation reproducible.** Align Maven inheritance/version policy and Java 21 setup; correct YAML, Avro source path, Compose mount, and Registry listener; assign service ports; provide explicit startup instructions and dependency readiness checks.
1. **Decide the MVP architecture.** The existing service boundaries can remain, but with one backend/web/infrastructure developer, seven independently deployed services plus Kafka and Kubernetes are a substantial delivery burden. Concentrate on identity, attendance, and one downstream projection first. Merge reporting with notification if the schedule requires, as the plan already permits.
1. **Implement the event foundation.** Versioned contract, database migrations, transactional outbox, idempotent inbox, revision-aware consumers, retry/DLQ behavior, and integration tests proving crash/duplicate recovery.
1. **Deliver the first vertical workflow.** Authenticate an employee, resolve trusted tenant context, check in with an idempotency key, persist evidence/session/outbox atomically, publish an attendance event, and show the resulting status in the actual Nuxt UI. Prove cross-tenant access is rejected.
1. **Complete attendance rules.** Shifts, grace periods, holidays, checkout, corrections, cutoff-based absence, effective dates, and overnight/time-zone cases. Then measure the stated p95 and dashboard latency targets.
1. **Add leave and requests.** Explicit transition tables, authorization, balance consistency, duplicate approval handling, and compensating conflict behavior.
1. **Add delivery and operations.** CI checks, image builds, migration strategy, structured logs, operational metrics, failure recovery, backups, and one reproducible deployment environment before expanding cloud topology.
1. **Integrate inference after manual attendance is reliable.** Begin with auditable rules and employee confirmation. Establish labeled real-world evaluation and an isolated final test set before enabling automatic decisions based on model confidence.

The most valuable acceptance test now is: one employee from tenant A checks in twice with the same idempotency key, exactly one session is persisted, downstream delivery survives a stopped consumer, and a tenant B user cannot read or modify that session. Passing this demonstrates the central promises of the architecture more effectively than adding more empty service modules.
