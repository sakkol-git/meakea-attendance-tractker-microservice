# Reporting workflows

[DESIGN] This is a domain body of the [business-flow reference](../../business-flow.md). Evidence labels distinguish targets from current source; status applies to the whole named workflow. Source absence is bounded by the [inventory](_inventory.md).

## Workflows

### BF-029  Projection updates and report queries — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Serve dashboards from owned read models rather than querying transactional owners. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2)
- Trigger / entry point: [DESIGN] Domain event ingestion followed by reporting query. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2)
- Actors and permissions: [DESIGN] Design HR/manager scoped to tenant/team; no implemented report endpoint. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2)
- Preconditions and inputs: [DESIGN] Employee/day facts and event revisions; filters/data-as-of semantics need active contract. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2)
- Main flow:

    1. [DESIGN] Apply employee/attendance/leave/request facts to local projection. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2)
    1. [DESIGN] Recompute daily_summary on change and intended nightly job. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2)
    1. [DESIGN] Read scoped projection and show data-as-of timestamp. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Consistency | Eventually consistent; under-one-second statement is an unmeasured target | §15.2; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2 |
    | [DESIGN] Reporting query | No cross-service SQL/HTTP business lookup | §15.1; ADR 008; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Duplicate or older fact | Inbox/version guard needed; absent | §10.7; ADR 008; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2 |
    | [DESIGN] Lag or missing retention | Stale display/rebuild policy required; no SLA proof | §15.2–15.4; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2 |

- State transitions: [DESIGN] New fact revision → projected revision; stale/lagging model is not strong consistency. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2)
- Data effects: [DESIGN] Proposed employee_dim/attendance_fact/leave_fact/request_fact/daily_summary; no CREATE TABLE exists. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] org.employee.v1, attendance.daily-status.v1, leave.request.v1, request.workflow.v1 in; no reporting publisher. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1–15.2; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Projection receipt/revision guards prevent duplicate or older facts; reporting reads have no mutation idempotency key. [IMPL] No projection consumer/query exists. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md; docs/business-flow/_inventory.md)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/reporting/src/main/java/com/hrflow/reporting/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-057, QA-058; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-030  Report export — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Produce a bounded authorized report artifact. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md)
- Trigger / entry point: [DESIGN] Design shorthand POST /reports/exports; full historical route in API map. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md)
- Actors and permissions: [DESIGN] Design scoped HR/manager export; exact data visibility must match report query. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md)
- Preconditions and inputs: [DESIGN] Report filters/format, row cap and tenant identity; numerical cap UNKNOWN. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md)
- Main flow:

    1. [DESIGN] Create intended export_job. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md)
    1. [DESIGN] Worker reads scoped reporting data and builds minimum-path CSV. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md)
    1. [DESIGN] Record completion and private download access; S3/XLSX belong to future/broader design. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Tenant export concurrency | One running export per tenant in approved design | §15.3; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md |
    | [DESIGN] Cloud/XLSX detail | Not evidence of current local implementation | ADR 008 cloud deferral; Week09 scope; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Row cap exceeded / worker interrupted | Response and job recovery implementation absent | §15.3; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md |
    | [DESIGN] Foreign/expired download | Must not expose artifact; no download handler | API tenant/security rules; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md |

- State transitions: [DESIGN] Requested export → running/completed/failed conceptually; actual enum UNKNOWN. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md)
- Data effects: [DESIGN] Proposed export_job and private artifact; file/row transaction boundary and cleanup not implemented. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] No export worker topic is named in the active design; do not invent one. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.3; docs/roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/reporting/src/main/java/com/hrflow/reporting/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-059, QA-060; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-031  Reporting full rebuild beyond retention — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Recover a reporting projection when retained events are insufficient. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4)
- Trigger / entry point: [DESIGN] Operator/admin rebuild or empty reporting model beyond thirty-day retained facts. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4)
- Actors and permissions: [DESIGN] Trusted operator and controlled admin export; implementation/access path UNKNOWN. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4)
- Preconditions and inputs: [DESIGN] Retained offsets plus authoritative older facts and reconciliation checkpoint. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4)
- Main flow:

    1. [DESIGN] Replay available reference/event facts. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4)
    1. [DESIGN] If daily-status retention is insufficient, use the documented one-off attendance admin re-export exception. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4)
    1. [DESIGN] Reconcile newer revisions/offsets before marking reports current. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Beyond retained daily-status history | Admin re-export exception is explicitly allowed | §15.4; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4 |
    | [DESIGN] Business request integration | Ordinary commands still event-only | ADR 008; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Export/replay gap or concurrent writes | Snapshot/catch-up ordering unspecified | §15.4 incomplete recovery contract; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4 |
    | [DESIGN] Cross-tenant/full-history exposure | No implementation proves scoped operator recovery | No admin job; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4 |

- State transitions: [DESIGN] Unavailable/incomplete projection → reconciled projection; readiness checkpoint UNKNOWN. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4)
- Data effects: [DESIGN] Rebuild proposed reporting facts; no mechanism reconstructs expired history from Kafka alone. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] Retained fact topics plus not-yet-defined admin recovery mechanism; no ordinary service HTTP is inferred. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §15.1, §15.4; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/reporting/src/main/java/com/hrflow/reporting/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-061, QA-062; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)
