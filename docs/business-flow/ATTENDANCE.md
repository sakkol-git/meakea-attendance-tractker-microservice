# Attendance workflows

[DESIGN] This is a domain body of the [business-flow reference](../../business-flow.md). Evidence labels distinguish targets from current source; status applies to the whole named workflow. Source absence is bounded by the [inventory](_inventory.md).

## Workflows

### BF-007  One-tap check-in — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Record a tap and return a provisional attendance result. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11)
- Trigger / entry point: [DESIGN] Design shorthand POST /check-in; historical full path /api/v1/attendance/check-in (API-035). (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11)
- Actors and permissions: [DESIGN] Design EMPLOYEE self; validated gateway identity and bound-device checks required. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11)
- Preconditions and inputs: [DESIGN] Device, idempotency key and employee-local reference data; server receive time is authoritative by design. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11)
- Main flow:

    1. [DESIGN] Validate identity, ACTIVE employee, device and existing open session. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11)
    1. [DESIGN] Insert TAP session, provisional daily status and owner outbox intent in one intended local transaction. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11)
    1. [DESIGN] Return designed 201 after commit; downstream effects follow asynchronously. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Already open session | 409 ALREADY_CHECKED_IN | §11.7.1; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11 |
    | [DESIGN] Mock-location tap | Save with REVIEW flag rather than erase evidence | §11.7.1; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11 |
    | [DESIGN] Holiday/leave tap | Record tap; overlay precedence remains a policy conflict | §11.4; ADR 004; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Inactive employee | 422 EMPLOYEE_NOT_ACTIVE; no accepted session | §11.7.1; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11 |
    | [DESIGN] Device mismatch | 422 DEVICE_MISMATCH with flagged evidence in design | §11.7.1; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11 |
    | [DESIGN] Database failure before commit | Design 503 and rollback; not proven | API status conventions; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11 |

- State transitions: [DESIGN] No effective open session → TAP OPEN + provisional status. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11)
- Data effects: [DESIGN] Design attendance_session/daily_status vs current catalog attendance_sessions/raw_attendance_events aliases; owner write/outbox transaction not implemented. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] attendance.session.v1 and attendance.daily-status.v1 → notification/reporting; sole Avro record is incomplete for these flows. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.7–11.7.1, §11.11; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/attendance/src/main/java/com/hrflow/attendance/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-013, QA-014; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-008  Check-out — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Close an open session while preserving chronology and totals. (docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5)
- Trigger / entry point: [DESIGN] Historical POST /api/v1/attendance/check-out; not an active contract. (docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5)
- Actors and permissions: [DESIGN] Design employee self; current service denies non-health requests. (docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5)
- Preconditions and inputs: [DESIGN] Session, device, version/If-Match and server-time chronology; full schema is historical. (docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5)
- Main flow:

    1. [DESIGN] Find the tenant-scoped open session. (docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5)
    1. [DESIGN] Validate version and checkout after check-in; close effective session. (docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5)
    1. [DESIGN] Recompute daily totals/revision and publish owner intent. (docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] No open session | Historical 409 | API-036; docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5 |
    | [DESIGN] Checkout not after check-in | Historical 422 | API-036; §11.2; docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Stale version | Current API convention 412; older generic design says 409 | API concurrency conventions; docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5 |
    | [UNKNOWN] Concurrent close/retry | Exact replay result UNKNOWN; no handler | Inventory; docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5 |

- State transitions: [DESIGN] OPEN → CLOSED; no delete or source mutation intended. (docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5)
- Data effects: [DESIGN] Proposed session/daily status and outbox; locking/rollback only described, not coded. (docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] attendance.session.v1 / attendance.daily-status.v1; no implemented publisher. (docs/reference/CONTRACTS-REFERENCE-06.md — API-036 (historical); docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.2, §11.5; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/attendance/src/main/java/com/hrflow/attendance/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-015, QA-016; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-009  Evidence ingestion — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Persist aggregate device presence evidence for review/inference. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8)
- Trigger / entry point: [DESIGN] Design shorthand POST /attendance/evidence; historical API-037 supplies full prefix. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8)
- Actors and permissions: [DESIGN] Employee/device self scope; no implemented Java ingestion route. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8)
- Preconditions and inputs: [DESIGN] Batched geofence transitions, Wi-Fi/BLE observations and sensor summaries; not raw GPS tracks. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8)
- Main flow:

    1. [DESIGN] Receive a device evidence batch. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8)
    1. [DESIGN] Append employee/device evidence into intended monthly partitions. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8)
    1. [DESIGN] Publish evidence facts for later processing. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Evidence item | Immutable/append-only | §11.2; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8 |
    | [DESIGN] Location privacy | Store transitions/accuracy, not raw tracks | §11.8; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Retry duplicate batch | Dedup identity/response needs an active contract | §11.11; API conventions; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8 |
    | [DESIGN] Bad device or tenant | Must not accept foreign evidence; enforcement absent | §11.7.1; API tenant rules; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8 |

- State transitions: [DESIGN] New signal → recorded evidence; this alone creates no TAP session. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8)
- Data effects: [DESIGN] Approved evidence vs current catalog raw_attendance_events; no partition/migration/retention implementation. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] attendance.evidence.v1; no producer or consumer is present. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.8; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/attendance/src/main/java/com/hrflow/attendance/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-017, QA-018; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-010  Inference orchestration — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Turn forgotten-tap evidence into a proposed inferred session or prompt. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5)
- Trigger / entry point: [DESIGN] Approaching cutoff/no TAP triggers intended scoring request; no actual scheduled handler. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5)
- Actors and permissions: [DESIGN] Attendance worker and private confidence worker; no human principal substitutes for tenant provenance. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5)
- Preconditions and inputs: [DESIGN] Employee-day feature snapshot, model revision and correlation; scoring event schemas missing. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5)
- Main flow:

    1. [DESIGN] Publish attendance.scoring-requested.v1 rather than call the demo HTTP API in production. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5)
    1. [DESIGN] Consume attendance.confidence.v1 and choose AUTO_INFER, ASK_EMPLOYEE or NO_ACTION. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5)
    1. [DESIGN] For AUTO_INFER, append INFERRED/PENDING and prompt later confirmation; otherwise prompt or leave no session. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] AUTO_INFER | Append pending inferred session | §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5 |
    | [DESIGN] ASK_EMPLOYEE / NO_ACTION | Prompt / no session respectively | §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Scoring worker unavailable | Timeout rules fallback designed; timeout duration/handler absent | §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5 |
    | [DESIGN] Late duplicate score after real TAP | Resolution guard required; missing code | §11.11; ADR 004; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5 |

- State transitions: [DESIGN] No TAP → INFERRED/PENDING, prompt-only or unchanged; not executed by HTTP /score. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5)
- Data effects: [DESIGN] Proposed attendance_session, feature snapshot, daily_status and outbox; Python score is stateless. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] Two proposed scoring topics are absent from creation script; session/daily changes would fan out. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.5; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/attendance/src/main/java/com/hrflow/attendance/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-019, QA-020; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-011  Next-day confirmation or rejection — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Let employees correct inferred presence and supply model labels. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5)
- Trigger / entry point: [DESIGN] Designed next-morning prompt and historical confirm/reject routes API-041/042. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5)
- Actors and permissions: [DESIGN] Employee self; independent approver policy for corrections is separate. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5)
- Preconditions and inputs: [DESIGN] Inferred session ID, expected revision and decision; exact active response absent. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5)
- Main flow:

    1. [DESIGN] Load the tenant/self inferred session. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5)
    1. [DESIGN] Record CONFIRMED or REJECTED without deleting evidence. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5)
    1. [DESIGN] Recompute effective daily revision and emit confirmation/rejection for downstream facts/labels. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Rejected inference | Ignored in effective precedence | §11.3–11.4; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5 |
    | [DESIGN] Confirmation/rejection label | Feed training through session events | part03 §16.5; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Already decided / changed revision | No implemented replay or status response | API concurrency conventions; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5 |
    | [DESIGN] Foreign employee session | Must hide/reject using trusted identity; not implemented | part01 §8.4; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5 |

- State transitions: [DESIGN] INFERRED/PENDING → CONFIRMED or REJECTED; no direct OPEN-to-CLOSED interpretation inferred from ambiguous sketch. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5)
- Data effects: [DESIGN] Proposed immutable evidence plus confirmation/void marker and derived status; exact persisted marker unresolved. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] attendance.session.v1 → confidence labels plus ordinary consumers; no consumer exists. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.3, §11.6; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §16.4–16.5; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/attendance/src/main/java/com/hrflow/attendance/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-021, QA-022; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-012  Attendance correction/manual entry — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Preserve an auditable human-approved interpretation of worked time. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
- Trigger / entry point: [DESIGN] Employee correction request, then manager/HR decision; no production endpoint. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
- Actors and permissions: [DESIGN] Employee self submits; authorized independent manager/HR approves by proposed ADR 004. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
- Preconditions and inputs: [DESIGN] Reason, referenced session/day and revision; exact approval contract missing. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
- Main flow:

    1. [DESIGN] Submit correction with reason. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
    1. [DESIGN] Authorized approver accepts/rejects. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
    1. [DESIGN] On approval append MANUAL interpretation/correction, recompute daily status and higher version. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Approved manual interpretation | Supersedes inferred interpretation without deletion | ADR 004 Decision; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed) |
    | [DESIGN] Approved leave plus manual work | Review conflict; no silent refund under proposed policy | ADR 004 Decision; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed) |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Self-approval / foreign employee | Must reject; enforcement absent | ADR 004 Security implications; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed) |
    | [DESIGN] Duplicate approval / late event | Must not append duplicate compensation or older daily revision | ADR 004 Implementation; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed) |

- State transitions: [DESIGN] Requested correction → approved/rejected; exact status enum not established. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
- Data effects: [DESIGN] Proposed attendance_corrections plus session/evidence history and outbox; no actual append-only constraint. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed); docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] attendance.daily-status.v1 higher revision; conflict event may be needed under proposed ADR. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §11.10; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed); docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/attendance/src/main/java/com/hrflow/attendance/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-023, QA-024; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-013  Daily-status finalization — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Calculate the employee-day after its working window ends. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6)
- Trigger / entry point: [DESIGN] Intended time.shift-cutoff.v1 per employee; emitting scheduler is BF-037. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6)
- Actors and permissions: [DESIGN] Background attendance worker using trusted tenant/shift references. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6)
- Preconditions and inputs: [DESIGN] Tenant-local day, shift/grace/tolerance, active sessions, holiday and approved leave references. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6)
- Main flow:

    1. [DESIGN] Consume cutoff and load owner-local reference snapshots. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6)
    1. [DESIGN] Apply calendar/evidence precedence and calculate late/early/worked values. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6)
    1. [DESIGN] Upsert one daily revision, publish final facts, and let consumers update later. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] First check-in > start + grace | Late; equality is not late; grace default ten minutes | §11.5; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6 |
    | [DESIGN] Multiple closed sessions | Worked time is the sum of session durations | §11.5; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6 |
    | [DESIGN] Holiday/full leave/day off | Calendar design applies; ADR 004 policy refinement unresolved | §11.4–11.5; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Missing reference / overnight/DST ambiguity | No implemented fallback/finalization policy | §10.9; ADR 004; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6 |
    | [DESIGN] Duplicate/older cutoff or recomputation | Must preserve newer revision | §11.11; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6 |

- State transitions: [DESIGN] Provisional day → derived final revision; late evidence may cause a newer revision. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6)
- Data effects: [DESIGN] Proposed daily_status unique tenant/employee/date; no installed row uniqueness or version guard. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] time.shift-cutoff.v1 in; attendance.daily-status.v1 out to reporting/notification. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.4–11.6; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/attendance/src/main/java/com/hrflow/attendance/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-025, QA-026; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)
