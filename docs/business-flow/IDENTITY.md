# Identity and organization workflows

[DESIGN] This is a domain body of the [business-flow reference](../../business-flow.md). Evidence labels distinguish targets from current source; status applies to the whole named workflow. Source absence is bounded by the [inventory](_inventory.md).

## Workflows

### BF-001  Login — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Establish a user identity and tenant-scoped session. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4)
- Trigger / entry point: [DESIGN] Design shorthand POST /auth/login; historical full route API-001 is in the API map. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4)
- Actors and permissions: [DESIGN] User; future token roles OWNER/HR_ADMIN/MANAGER/EMPLOYEE; no live login exists. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4)
- Preconditions and inputs: [DESIGN] Credentials; exact active request schema is absent because contracts/openapi.yaml is absent. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4)
- Main flow:

    1. [DESIGN] Resolve credentials and verify the password. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4)
    1. [DESIGN] Issue RS256 access JWT with tenant/employee/roles and a hashed random refresh token. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4)
    1. [DESIGN] Gateway later validates signature/expiry/issuer/audience before trusting claims. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Valid credentials | Access token 15 minutes; refresh lifetime 14 days | §8.1; docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4 |
    | [DESIGN] Wrong password or unknown user | Uniform response and throttling are intended | §8.6; docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Invalid credentials | Design 401/uniform error; no session issued | §8.6; API conventions; docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4 |
    | [DESIGN] Current Java request | 401 anonymous / 403 authenticated; handler absent | Current security scaffold; docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4 |

- State transitions: [DESIGN] No session → successful login → tokens; no implemented transition. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4)
- Data effects: [DESIGN] Proposed identity_org.users/refresh_tokens; no actual persistence or password-verification transaction. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] N/A: no login event is implemented or required by the cited authentication sequence. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1–8.4; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Login is pre-authentication: verify credentials and use the audited narrow credential lookup exception, never trust request-selected business tenant identity. [IMPL] Current identity chain denies the operation and has no login handler. (docs/REST-API-DATABASE-DESIGN.md — RLS and role requirements; docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.1; services/identity-org/src/main/java/com/hrflow/identityorg/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain)
- QA scenarios: QA-001, QA-002; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-002  Refresh token rotation — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Renew access without reusing a stolen refresh token. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2)
- Trigger / entry point: [DESIGN] Refresh command; full historical API-002 remains candidate, not a live route. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2)
- Actors and permissions: [DESIGN] Token holder; family tied to the original user/tenant. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2)
- Preconditions and inputs: [DESIGN] Refresh token; rotated tokens are stored as hashes. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2)
- Main flow:

    1. [DESIGN] Look up and validate refresh state. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2)
    1. [DESIGN] Rotate on every successful use and issue replacement credentials. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2)
    1. [DESIGN] Detect reuse and revoke the family rather than issue more tokens. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Unused valid refresh | Rotate token | §8.2; docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2 |
    | [DESIGN] Reused or revoked refresh | Revoke/reject per Q05 design | docs/roadmap/ENGINEERING-STANDARDS.md — Q05; docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Concurrent reuse | Design revoke family; exact transaction/replay response remains unspecified | Q05; docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2 |
    | [DESIGN] Expired token | Design rejection; exact active handler missing | §8.2; docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2 |

- State transitions: [DESIGN] Valid refresh → rotated; reused family → revoked. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2)
- Data effects: [DESIGN] Proposed identity_org.refresh_tokens; family/revocation columns and race-safe write are not migrated. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] N/A: no refresh event/consumer is implemented. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Authenticate the opaque refresh grant/family, rotate and detect reuse; no access-token prerequisite is invented. [IMPL] Refresh/session lookup is absent and current chain denies the operation. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/ENGINEERING-STANDARDS.md — Q05; services/identity-org/src/main/java/com/hrflow/identityorg/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain)
- QA scenarios: QA-003, QA-004; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-003  Tenant onboarding — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Establish the company boundary for subsequent HR work. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Trigger / entry point: [DESIGN] Tenant onboarding/admin command; no implemented endpoint. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Actors and permissions: [DESIGN] Tenant owner/platform bootstrap permission is UNKNOWN: active bootstrap contract missing. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Preconditions and inputs: [DESIGN] Tenant identity and initial owner; slug/timezone/uniqueness details need an approved contract. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Main flow:

    1. [DESIGN] Create the proposed tenant boundary. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
    1. [DESIGN] Associate initial users with that tenant before HR actions. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
    1. [DESIGN] Publish owner reference facts only as explicitly designed; no tenant topic is implemented. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] New tenant | Own logical data scope | §9; §7 of architecture part01; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership |
    | [DESIGN] Personal data access | Trusted tenant and RLS required | ADR 008 tenant carry-forward; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [UNKNOWN] Duplicate tenant or partial bootstrap | Response/compensation UNKNOWN; no transaction code | Missing active contract; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership |
    | [DESIGN] Untrusted caller chooses tenant | Must be rejected by future boundary tests | API multi-tenant rules; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership |

- State transitions: [DESIGN] No tenant → configured tenant; lifecycle enum UNKNOWN. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Data effects: [DESIGN] Proposed identity_org.tenants/users; no schema grants, inserts or bootstrap migration. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] No tenant-specific topic is in the creation script; org.employee.v1 is only relevant when employee data exists. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [UNKNOWN] First-tenant bootstrap authorization is unspecified; do not assume public registration or a pre-existing tenant token. [IMPL] Current chain denies business requests. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; services/identity-org/src/main/java/com/hrflow/identityorg/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain)
- QA scenarios: QA-005, QA-006; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-004  Employee and organization onboarding — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Create employee/org master data consumed locally by other services. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Trigger / entry point: [DESIGN] Organization/employee administration; historical candidate routes are mapped separately. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Actors and permissions: [DESIGN] Design HR/owner tenant scope, manager team reads and employee self reads per architecture part01 §8.4. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Preconditions and inputs: [DESIGN] Employee identity, organization and manager assignments; exact mandatory schemas remain historical. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Main flow:

    1. [DESIGN] Maintain proposed departments, positions and employee ownership. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
    1. [DESIGN] Commit an owner change and its outbox intent under the target transaction rule. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
    1. [DESIGN] Replicate employee reference facts to consuming services. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Employee master changes | Publish org.employee.v1 | §9; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership |
    | [DESIGN] Read by manager | Scope to current direct reports | architecture part01 §8.4; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Duplicate tenant employee code/email | Proposed scoped uniqueness rejects conflict; no installed constraint | API ownership catalog; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership |
    | [DESIGN] Out-of-tenant manager link | Must reject; no repository/FK enforcement exists | API multi-tenant rules; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership |

- State transitions: [DESIGN] Unregistered employee → configured employee; ACTIVE is a designed attendance precondition. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership)
- Data effects: [DESIGN] Proposed identity_org.departments/positions/employees/users and owner outbox; physical schema identity_org, not identity. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] org.employee.v1 → attendance/leave/request/notification/reporting local copies; retry not implemented. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9; docs/REST-API-DATABASE-DESIGN.md — PostgreSQL domain schemas and table ownership; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/identity-org/src/main/java/com/hrflow/identityorg/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-007, QA-008; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-005  Shift definition and assignment — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Supply working windows used for lateness, leave days and cutoff. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5)
- Trigger / entry point: [DESIGN] Shift/admin assignment command; no production route or scheduler. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5)
- Actors and permissions: [DESIGN] HR/owner administrative scope is design; exact assignment approver requires active contract. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5)
- Preconditions and inputs: [DESIGN] Employee/shift reference, work days, start/end, tenant timezone and grace (default ten minutes). (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5)
- Main flow:

    1. [DESIGN] Maintain the shift definition/assignment in identity-org. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5)
    1. [DESIGN] Publish the shift fact. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5)
    1. [DESIGN] Attendance consumes local shift_ref for calculations and scheduling. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Grace omitted | Design default ten minutes | §11.5; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5 |
    | [DESIGN] Shift topic key | tenantId:shiftId, not employee key | §10.1; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Missing/stale local assignment | Reference-readiness response is not implemented | §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5 |
    | [DESIGN] Overnight/DST/overlapping rules | Policy requires explicit decision; no calculation code | ADR 004 implementation/open windows; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5 |

- State transitions: [DESIGN] Unassigned → assigned; effective-date/version transition UNKNOWN. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5)
- Data effects: [DESIGN] Design shift master and local shift_ref; current API catalog shift_rules name is not a migration. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] org.shift.v1 (scripted compacted) → attendance; scheduler later emits cutoff. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §10.10, §11.5; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/identity-org/src/main/java/com/hrflow/identityorg/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-009, QA-010; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-006  Holiday management — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Share tenant holiday dates with attendance and leave. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3)
- Trigger / entry point: [DESIGN] Holiday administrative creation/removal; historical API-031–033. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3)
- Actors and permissions: [DESIGN] Design tenant HR/owner scope; implemented permissions absent. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3)
- Preconditions and inputs: [DESIGN] Holiday date and tenant calendar; active payload and removal/tombstone convention UNKNOWN. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3)
- Main flow:

    1. [DESIGN] Maintain holiday master. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3)
    1. [DESIGN] Publish org.holiday.v1. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3)
    1. [DESIGN] Update local holiday_ref before recomputing attendance/leave working-day decisions. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Holiday overlay | Highest priority in the simple attendance design | §11.4; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3 |
    | [DESIGN] Holiday topic key | tenantId:date | §10.1; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Stale or duplicate calendar fact | Newer-state handling required, not implemented | §10.9; §17.4; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3 |
    | [DESIGN] Worked holiday | Preserve evidence; final policy conflicts with manual-wins shorthand | ADR 004 proposed conflict review; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3 |

- State transitions: [DESIGN] No holiday reference → holiday reference; withdrawal semantics need a contract. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3)
- Data effects: [DESIGN] Holiday owner/local holiday_ref proposed; no actual table or soft-delete code. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] org.holiday.v1 → attendance and leave; compaction script does not prove correct tombstones. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9, §10.1, §11.4, §12.3; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/identity-org/src/main/java/com/hrflow/identityorg/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-011, QA-012; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-040  Logout and token-family revocation — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Revoke future refresh capability when a session is ended. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md)
- Trigger / entry point: [DESIGN] Logout command; no implemented logout controller. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md)
- Actors and permissions: [DESIGN] Authenticated session owner; exact active-session selection contract absent. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md)
- Preconditions and inputs: [DESIGN] Current token/session family as design specifies; do not place tokens in evidence. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md)
- Main flow:

    1. [DESIGN] Resolve the current refresh session/family. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md)
    1. [DESIGN] Revoke the intended refresh capability. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md)
    1. [DESIGN] Reject subsequent refresh without claiming stateless access JWT immediately disappears. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Logout | Refresh revoked by design | §8.2; docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md |
    | [DESIGN] Already issued access token | Immediate revocation semantics UNKNOWN; short lifetime alone is not instant revocation | No active auth code; docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [UNKNOWN] Repeated logout | Exact response/idempotency behavior UNKNOWN | No controller; docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md |
    | [DESIGN] Stolen token family after logout | Must fail refresh; no implementation | §8.2; docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md |

- State transitions: [DESIGN] Active refresh session → revoked; actual access-token lifetime policy needs implementation. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md)
- Data effects: [DESIGN] Proposed refresh_tokens revocation state; no write or audit transaction implemented. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] No logout event is required by the cited design; do not invent one. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; docs/roadmap/02-IDENTITY-AND-HOST/README.md; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Revoke only the authenticated owner/session family under the eventual auth contract; exact multi-session policy is unknown. [IMPL] No logout handler exists. (docs/reference/INCOMING-ARCHITECTURE-V2-01.md — §8.2; services/identity-org/src/main/java/com/hrflow/identityorg/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain; docs/business-flow/_inventory.md)
- QA scenarios: QA-079, QA-080; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)
