# Leave workflows

[DESIGN] This is a domain body of the [business-flow reference](../../business-flow.md). Evidence labels distinguish targets from current source; status applies to the whole named workflow. Source absence is bounded by the [inventory](_inventory.md).

## Workflows

### BF-014  Leave submission/reservation — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Reserve leave availability before an approver decides. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4)
- Trigger / entry point: [DESIGN] Leave submission; historical POST /api/v1/leave/requests API-053. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4)
- Actors and permissions: [DESIGN] Employee self by intended token scope. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4)
- Preconditions and inputs: [DESIGN] Leave type, working-date/half-day list and required evidence; exclude holidays. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4)
- Main flow:

    1. [DESIGN] Validate working days, allowed half-days, attachments and no pending/approved overlap. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4)
    1. [DESIGN] Reserve pending days with the designed conditional availability update. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4)
    1. [DESIGN] Write request/ledger SUBMIT and owner outbox in intended local transaction. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Available = entitled + carried_over - used - pending | Requested paid days must fit | §12.2–12.4; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4 |
    | [DESIGN] Zero rows on conditional update | 422 LEAVE_BALANCE_INSUFFICIENT | §12.3–12.4; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Concurrent overspend | One conditional reservation must fail, not go negative | §12.4; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4 |
    | [DESIGN] Request/ledger/outbox write fails | Intended rollback of reservation, no implementation | API outbox convention; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4 |

- State transitions: [DESIGN] No request → PENDING; pending increases by requested days. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4)
- Data effects: [DESIGN] Current catalog leave_requests/leave_balances/leave_request_days; approved chapter leave_balance singular; ledger name unresolved. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] LeaveSubmitted on leave.request.v1 → manager notification; publication absent. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.4; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/leave/src/main/java/com/hrflow/leave/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-027, QA-028; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-015  Leave approval — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Convert reserved leave into approved absence. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5)
- Trigger / entry point: [DESIGN] Approve command on a pending request; historical API-058. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5)
- Actors and permissions: [DESIGN] Design manager/HR scope; actual independent-approver policy requires approval. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5)
- Preconditions and inputs: [DESIGN] Pending request and expected revision; per-day list for downstream projection. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5)
- Main flow:

    1. [DESIGN] Check approver scope and current pending revision. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5)
    1. [DESIGN] Move pending days to used and append APPROVE ledger reason. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5)
    1. [DESIGN] Publish approval; attendance projects leave_ref, reporting leave_fact and notification informs employee. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Pending approval | pending -= days; used += days | §12.2; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5 |
    | [DESIGN] Downstream projection | Not synchronous with approval response | §12.5; part03 §18.3; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Parallel approvers / stale command | Only one state transition; response follows active API 412/428 design | API concurrency; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5 |
    | [DESIGN] Consumer offline | Owner approval remains; calendar effect delayed | §12.5; part03 §18.3; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5 |

- State transitions: [DESIGN] PENDING → APPROVED; overlay appears only after consumer effect. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5)
- Data effects: [DESIGN] Request/balance/ledger/outbox intended owner transaction; cross-owner updates are separate. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] LeaveApproved on leave.request.v1 → attendance/notification/reporting. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.5; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/leave/src/main/java/com/hrflow/leave/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-029, QA-030; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-016  Leave rejection — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Decline pending leave and release reserved availability. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
- Trigger / entry point: [DESIGN] Reject command; historical API-059. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
- Actors and permissions: [DESIGN] Design authorized approver for the employee scope. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
- Preconditions and inputs: [DESIGN] Pending request, reason and revision; precise required reason rule not in active contract. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
- Main flow:

    1. [DESIGN] Verify pending state/scope. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
    1. [DESIGN] Release pending reservation with RELEASE ledger reason. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
    1. [DESIGN] Record rejection and publish owner intent. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Reject pending | pending decreases by reserved days | §12.2; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5 |
    | [DESIGN] Already approved/rejected | No second reservation release intended | §12.1; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Duplicate decision | Idempotency/state guard is unimplemented | API idempotency convention; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5 |
    | [DESIGN] Transaction fails | Intended balance/request/ledger rollback | API outbox convention; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5 |

- State transitions: [DESIGN] PENDING → REJECTED; used unchanged. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
- Data effects: [DESIGN] Proposed leave request/balance/ledger/outbox; no code proves release once. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] LeaveRejected on leave.request.v1; attendance soft-removes projected leave where applicable. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/leave/src/main/java/com/hrflow/leave/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-031, QA-032; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-017  Leave cancellation — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Undo pending or eligible future approved leave with accounting history. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
- Trigger / entry point: [DESIGN] Employee cancellation command; historical API-056. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
- Actors and permissions: [DESIGN] Design self scope; approval/future-day restrictions need resolved policy. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
- Preconditions and inputs: [DESIGN] Request, future day subset/revision where applicable; exact partial-cancel contract unresolved. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
- Main flow:

    1. [DESIGN] Identify pending versus approved eligible future leave. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
    1. [DESIGN] Release pending or compensate used with CANCEL/RELEASE ledger reason. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
    1. [DESIGN] Publish cancellation and remove/recompute projected leave facts. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Pending cancellation | pending -= days; RELEASE | §12.2; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5 |
    | [DESIGN] Approved future cancellation | used -= days; CANCEL | §12.1–12.2; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Past approved days | Future-only restriction described; exact response unknown | §12.1; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5 |
    | [DESIGN] Ambiguous lifecycle sketch | Do not implement REJECTED-to-CANCELLED from its misplaced arrow | §12.1 discrepancy; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5 |

- State transitions: [DESIGN] PENDING or eligible APPROVED → CANCELLED; rejected cancellation arrow is unresolved design. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5)
- Data effects: [DESIGN] Intended owner request/balance/ledger/outbox; compensation must be revision/idempotency guarded. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] LeaveCancelled on leave.request.v1 → soft removal in attendance and recompute. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.1–12.2, §12.5; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/leave/src/main/java/com/hrflow/leave/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-033, QA-034; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-018  Leave balance query and HR adjustment — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Explain entitlement/availability and record authorized adjustments. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Trigger / entry point: [DESIGN] Balance read and HR adjustment; no current handler. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Actors and permissions: [DESIGN] Employee self read; HR/owner tenant adjustment is designed, active permission details missing. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Preconditions and inputs: [DESIGN] Leave type/year; adjustment amount and note for mutation. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Main flow:

    1. [DESIGN] Read entitlement/carried-over/used/pending. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
    1. [DESIGN] Derive available using the documented exact arithmetic. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
    1. [DESIGN] For authorized adjustment append ADJUST with a note and update entitlement. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Available calculation | entitled + carried_over - used - pending | §12.2; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership |
    | [DESIGN] Accounting types | Exact numeric, not floating-point balance | part03 §17.2; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Foreign employee balance | No implemented tenant/object protection | API multi-tenant rules; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership |
    | [DESIGN] Concurrent adjustment/reservation | Consistent ledger/balance transaction required, absent | §12.4; API outbox rules; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership |

- State transitions: [DESIGN] Balance view only for query; adjustment → newer balance/ledger. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Data effects: [DESIGN] Proposed leave_balances and ledger; reconstruction from ledger described, unimplemented. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] Adjustment event contract/topic meaning beyond leave.request.v1 is UNKNOWN; do not invent one. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/leave/src/main/java/com/hrflow/leave/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-035, QA-036; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-019  Year-end rollover — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Carry eligible days into the next accounting year. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension))
- Trigger / entry point: [DESIGN] Historical async admin year-end command; no actual scheduled rollover. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension))
- Actors and permissions: [DESIGN] HR administrative scope proposed; effective authorization unknown. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension))
- Preconditions and inputs: [DESIGN] Source/target year, carry policy maximum and treatment of pending leave require approved policy. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension))
- Main flow:

    1. [DESIGN] Determine eligible closing balance under approved policy. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension))
    1. [DESIGN] Carry no more than the policy maximum. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension))
    1. [DESIGN] Record ROLLOVER ledger reason and job status if the historical async extension is adopted. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension))

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Carry eligibility | Cap by policy maximum; value not specified | §12.2; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension) |
    | [DESIGN] Repeated run | No actual run key/restart guarantee | Missing job implementation; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension) |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Interrupted rollover | Recovery/ordering of employee-years unspecified | No rollover source; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension) |
    | [UNKNOWN] Pending cross-year leave | Policy UNKNOWN; must not invent settlement | §12.2 limited scope; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension) |

- State transitions: [DESIGN] Closing year → next-year carry; exact job states UNKNOWN. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension))
- Data effects: [DESIGN] Proposed balances/ledger; no job/checkpoint table is created. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension); docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] No authoritative rollover event contract; any external effects must be specified first. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §12.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-066/067 (historical proposed extension); docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/leave/src/main/java/com/hrflow/leave/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-037, QA-038; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-020  Leave-versus-attendance conflict review — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Resolve observed work overlapping approved leave without losing evidence. (docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
- Trigger / entry point: [DESIGN] Proposed conflict from approved leave plus later attendance/manual interpretation. (docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
- Actors and permissions: [DESIGN] Independent authorized HR/manager; no automatic policy approval is inferred. (docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
- Preconditions and inputs: [DESIGN] Leave revision, attendance evidence and explicit keep/cancel decision with reason. (docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
- Main flow:

    1. [DESIGN] Record proposed NEEDS_REVIEW without deleting evidence. (docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
    1. [DESIGN] Retain leave debit/overlay until an authorized explicit decision. (docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
    1. [DESIGN] If leave is cancelled, compensate once and publish a newer snapshot. (docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Manual plus approved leave | Review instead of silent refund | ADR 004 Decision; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed) |
    | [DESIGN] Late conflict after cancellation | Revision guard prevents resurrection | ADR 004 Implementation; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed) |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] No HR policy approval | Block silent overlap finalization; choice remains open | ADR 004 Implementation; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed) |
    | [DESIGN] Duplicate compensation | Must be rejected/no-op, no implementation | ADR 004 Implementation; docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed) |

- State transitions: [DESIGN] APPROVED overlap → proposed NEEDS_REVIEW → keep or cancel; not an adopted runtime lifecycle. (docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed))
- Data effects: [DESIGN] Intended preserved sessions, leave ledger compensation and audit; exact conflict persistence unknown. (docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed); docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] attendance.leave-conflict.v1 is scripted but absent from main approved topic catalogue; no publisher/consumer. (docs/adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md — Decision (Proposed); docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/leave/src/main/java/com/hrflow/leave/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-039, QA-040; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)
