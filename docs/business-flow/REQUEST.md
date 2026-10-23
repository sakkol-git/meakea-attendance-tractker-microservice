# Request and attachment workflows

[DESIGN] This is a domain body of the [business-flow reference](../../business-flow.md). Evidence labels distinguish targets from current source; status applies to the whole named workflow. Source absence is bounded by the [inventory](_inventory.md).

## Workflows

### BF-021  Create/edit general or material request — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Capture a typed business request without leave accounting. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4)
- Trigger / entry point: [DESIGN] Draft creation/edit; historical API-074/077. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4)
- Actors and permissions: [DESIGN] Design employee self draft; configured administrative material catalogue is separate. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4)
- Preconditions and inputs: [DESIGN] Type, variable JSON form data and optional material/attachment references. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4)
- Main flow:

    1. [DESIGN] Resolve configured request type. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4)
    1. [DESIGN] Create or edit a DRAFT owned by requester/tenant. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4)
    1. [DESIGN] Validate form/material metadata before later submission. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] General request | Variable data jsonb; no leave balance effect | §13.4; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4 |
    | [DESIGN] Material request | May later reach FULFILLED | §13.2; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Invalid type/form | Validation response not implemented | No active request contract; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4 |
    | [DESIGN] Edit after final decision | Transition restriction needs active contract | §13.2; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4 |

- State transitions: [DESIGN] No request → DRAFT; DRAFT edit → newer draft. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4)
- Data effects: [DESIGN] Approved chapter request vs current catalog general_requests; no installed table or version rule. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] No draft-event name is authoritative here; do not fabricate a publication. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.4; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/request/src/main/java/com/hrflow/request/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-041, QA-042; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-022  Submit request and resolve approval chain — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Turn a draft into a pending sequence of approvers. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3)
- Trigger / entry point: [DESIGN] Submit draft command; historical API-078. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3)
- Actors and permissions: [DESIGN] Requester self; manager/HR resolution uses local employee_ref. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3)
- Preconditions and inputs: [DESIGN] Draft revision and configured chain (example MANAGER then HR_ADMIN). (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3)
- Main flow:

    1. [DESIGN] Materialize chain elements as approval steps on submission. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3)
    1. [DESIGN] Resolve manager from requester manager_id and HR inbox from tenant roles. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3)
    1. [DESIGN] Record missing-manager SKIPPED and activate the relevant decision stage. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] No manager | Step SKIPPED and recorded | §13.3; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3 |
    | [DESIGN] HR_ADMIN stage | Any tenant HR admin may decide; first wins | §13.3; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Employee reference not caught up | Readiness/response unspecified; cannot query another owner directly | §17.4; §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3 |
    | [DESIGN] Duplicate submit | Must not create duplicate chain rows | API idempotency; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3 |

- State transitions: [DESIGN] DRAFT → PENDING; no-manager step → SKIPPED. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3)
- Data effects: [DESIGN] Approved approval_step / current request_approvals alias; materialization/outbox transaction absent. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] request.workflow.v1 key tenantId:requestId → notification/reporting. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.1–13.3; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/request/src/main/java/com/hrflow/request/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-043, QA-044; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-023  Approve request step — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Advance a pending chain until the request is approved. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3)
- Trigger / entry point: [DESIGN] Current approver decision; historical API-085. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3)
- Actors and permissions: [DESIGN] Resolved manager or tenant HR inbox, not arbitrary employee role. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3)
- Preconditions and inputs: [DESIGN] Pending step, actor and expected request/step revision. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3)
- Main flow:

    1. [DESIGN] Authorize the actor for the active step. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3)
    1. [DESIGN] Record the first accepted approval. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3)
    1. [DESIGN] Advance to the next step or mark APPROVED when all required steps are approved/skipped. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] All steps accepted | Request becomes APPROVED | §13.2; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3 |
    | [DESIGN] Two HR admins decide | First decision wins | §13.3; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Stale/competing decision | No implemented optimistic lock/replay response | API concurrency; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3 |
    | [DESIGN] Self/foreign approver | Must reject under actual future policy; no role checks exist | part01 §8.4; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3 |

- State transitions: [DESIGN] PENDING step → approved; final accepted step → APPROVED request. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3)
- Data effects: [DESIGN] Proposed general_requests/request_approvals and outbox; no concurrency code. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] request.workflow.v1 → notification/reporting; no synchronous downstream response guarantee. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2–13.3; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/request/src/main/java/com/hrflow/request/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-045, QA-046; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-024  Reject or cancel request — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Stop a pending request or withdraw an undecided draft/request. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical))
- Trigger / entry point: [DESIGN] Approver reject or requester cancel. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical))
- Actors and permissions: [DESIGN] Approver rejects active step; requester cancels before final decision by design. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical))
- Preconditions and inputs: [DESIGN] Request/step ID, reason and revision as future contract defines. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical))
- Main flow:

    1. [DESIGN] Check allowed state and actor. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical))
    1. [DESIGN] Record REJECTED on any step rejection, or CANCELLED before final decision. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical))
    1. [DESIGN] Publish the new workflow revision for recipients/projections. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical))

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Any step rejects | Whole request REJECTED | §13.2; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical) |
    | [DESIGN] Cancel draft/pending | CANCELLED | §13.2; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical) |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [UNKNOWN] Already approved/fulfilled cancellation | Not allowed by depicted lifecycle; exact error UNKNOWN | §13.2; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical) |
    | [DESIGN] Duplicate competing decision | State/revision guard absent | API concurrency conventions; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical) |

- State transitions: [DESIGN] DRAFT/PENDING → CANCELLED; active pending chain → REJECTED. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical))
- Data effects: [DESIGN] Proposed request/header/steps history; no physical deletion/retention behavior implemented. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical); docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] request.workflow.v1; delivery consumers are unimplemented. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-079/086 (historical); docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/request/src/main/java/com/hrflow/request/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-047, QA-048; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-025  Material fulfillment — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Record that an approved material request was supplied. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical))
- Trigger / entry point: [DESIGN] Fulfill approved material command; no live endpoint. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical))
- Actors and permissions: [DESIGN] Fulfillment role and stock authority require approved active contract. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical))
- Preconditions and inputs: [DESIGN] Approved material request, supplied quantities/evidence as policy specifies. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical))
- Main flow:

    1. [DESIGN] Verify approved material type and actor. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical))
    1. [DESIGN] Record fulfillment under the target request transaction. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical))
    1. [DESIGN] Publish the resulting workflow revision. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical))

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Approved material request | May become FULFILLED | §13.2; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical) |
    | [DESIGN] General request | No automatic inventory or leave effect described | §13.4; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical) |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [UNKNOWN] Insufficient stock / partial fulfillment | Reservation/partial policy UNKNOWN; no inventory code | Missing fulfillment contract; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical) |
    | [DESIGN] Duplicate fulfillment | No actual idempotency guard | API idempotency convention; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical) |

- State transitions: [DESIGN] APPROVED material → FULFILLED; other inventory states UNKNOWN. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical))
- Data effects: [DESIGN] Material catalogue/request effect designed; exact stock tables/locking not established. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical); docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] request.workflow.v1; no stock event name or reservation guarantee inferred. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §13.2; docs/reference/CONTRACTS-REFERENCE-07.md — API-087 (historical); docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/request/src/main/java/com/hrflow/request/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-049, QA-050; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-026  Private attachment upload/download — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Attach private evidence without exposing files across users/tenants. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical))
- Trigger / entry point: [DESIGN] Historical presign/complete/download actions; no current controller or storage handler. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical))
- Actors and permissions: [DESIGN] Owner/authorized approver; foreign or hidden resources must not be exposed. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical))
- Preconditions and inputs: [DESIGN] Attachment UUID, object metadata/content checks and expiry; active storage contract missing. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical))
- Main flow:

    1. [DESIGN] Authorize the parent request/leave resource before file operations. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical))
    1. [DESIGN] Verify uploaded metadata and security state before exposing content. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical))
    1. [DESIGN] Authorize each private download with a bounded access grant. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical))

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Hidden foreign resource | Design 404, not an existence oracle | API status/tenant conventions; docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical) |
    | [DESIGN] Unsafe/unverified attachment | Historical quarantine/check policy; no actual scanner | API-082 historical extension; docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical) |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Expired link / malware / checksum mismatch | Exact response and resulting storage state unimplemented | Historical attachment design; docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical) |
    | [DESIGN] Parent deleted or permission changed | No revocation/cleanup behavior implemented | No storage controller/migration; docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical) |

- State transitions: [DESIGN] Pending upload → verified/private attachment in design; exact status enum UNKNOWN. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical))
- Data effects: [DESIGN] Proposed request_attachments/private bytes; retention/deletion linkage not migrated. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical); docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] No attachment event contract is established; do not invent a topic or scanner guarantee. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/roadmap/07-REQUESTS-AND-APPROVALS/README.md; docs/reference/CONTRACTS-REFERENCE-07.md — API-081/082/083 (historical); docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/request/src/main/java/com/hrflow/request/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-051, QA-052; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)
