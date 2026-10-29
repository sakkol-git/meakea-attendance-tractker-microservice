# Identity and organization workflows

This is a domain body of the [business-flow reference](../../business-flow.md). Evidence labels distinguish designed targets from implemented source; each workflow status applies only to that named workflow. Source absence is bounded by the [inventory](_inventory.md).

## Workflows

### BF-001  Login — Status: IMPLEMENTED

- Purpose: Establish a tenant-scoped user session through `POST /auth/login`; the gateway exposes it as `POST /api/v1/auth/login`.
- Inputs: Tenant slug, email and password follow `contracts/openapi.json`. Browser callers also require the exact allowed Origin and matching double-submit CSRF token.
- Main behavior: The service performs a tenant-scoped active-user lookup, verifies BCrypt credentials, issues an RS256 access JWT, and persists only the SHA-256 hash of a random opaque refresh token.
- Token policy: Access tokens last 15 minutes and contain `sub`, `tenantId`, `employeeId`, validated roles, `iss`, `aud`, `iat`, `exp`, `jti` and `kid`. Refresh sessions last 14 days.
- Failure behavior: Unknown accounts and wrong passwords return the same 401 problem. The gateway replaces the internal client-address header from its connection peer; account and source attempt keys are SHA-256 hashes. The threshold attempt returns 429 with `Retry-After`. Invalid request bodies return the canonical Problem `code` and `detail` fields.
- Security boundary: Tenant and relationship authorization remain server responsibilities. The login tenant slug selects the credential namespace and does not grant access to another tenant.
- Evidence: `AuthSessionServiceImplTest`, `AttemptThrottlerServiceImplTest`, `JwkProviderServiceImplTest`, controller/security tests, and `IdentityPersistenceIT`. The integration test applies all Flyway migrations to PostgreSQL and validates JPA startup.

### BF-002  Refresh token rotation — Status: IMPLEMENTED

- Purpose: Renew access using `POST /auth/refresh` while detecting stolen-token reuse.
- Transport: Native clients send the opaque refresh token in JSON. Browsers use an HttpOnly, Secure, SameSite=Lax cookie scoped to the configured browser-visible auth prefix plus Origin and CSRF checks. The direct service default is `/auth`; gateway deployments use `/api/v1/auth`.
- Main behavior: The service hashes the presented token, locks its row, marks it consumed and creates one successor in the same transaction. Sessions carry tenant, user and family IDs; a composite database foreign key prevents cross-tenant user/session ownership.
- Reuse behavior: Presenting a consumed predecessor revokes every unrevoked session in that tenant-scoped family and returns the uniform 401 problem. Concurrent presentation permits one rotation, then revokes the resulting family.
- Lifetime and storage: Refresh sessions expire after 14 days. Raw refresh tokens are returned once and are never stored.
- Evidence: `AuthSessionServiceImplTest` covers rotation, reuse and tenant-scoped revocation. `IdentityPersistenceIT` verifies clean PostgreSQL migration, invalid cross-tenant ownership rejection and concurrent reuse behavior.

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

### BF-040  Logout and token-family revocation — Status: IMPLEMENTED

- Purpose: Revoke future refresh capability when a session is ended.
- Trigger: `POST /auth/logout`, exposed through the gateway as `POST /api/v1/auth/logout`.
- Inputs: Native callers send the refresh token in JSON. Browsers send the protected refresh cookie with the exact allowed Origin and matching CSRF token.
- Main flow:

    1. Hash and lock the presented refresh session.
    1. Revoke every active session with the same tenant and family ID.
    1. Return 204 and clear the browser refresh cookie on the same configured browser-visible auth path used when it was created.

- Invalid, missing, unknown or already-revoked tokens receive the uniform 401 Problem response.
- Existing access JWTs remain valid until their 15-minute expiry; logout does not claim immediate stateless-token revocation.
- Family revocation is tenant-scoped and transactional. No logout event is required by the accepted design.
- Evidence: `AuthSessionsTest.logoutRevokesOnlyThePresentedFamily` and the PostgreSQL integration suite.

### BF-041  Read and update own profile — Status: IMPLEMENTED

- Purpose: Let an authenticated user read their current profile through `GET /auth/me` and update the canonical `name` and `timeZone` fields through `PATCH /auth/profile`; the gateway exposes both below `/api/v1`.
- Main behavior: The service derives user and tenant IDs from the validated JWT, performs an enabled-user lookup scoped by both IDs, checks the token claims against the stored user, and returns the canonical profile projection. Updates strip surrounding whitespace from names and require a valid IANA time-zone identifier when `timeZone` is supplied.
- Security boundary: Both the gateway and identity service require JWT authentication. Tenant identity is never accepted from the request body or query, and a mismatched, disabled or missing account cannot be used to read or update a profile.
- Data effects: The update changes only `identity_org.auth_user.display_name` and `time_zone`. The accepted contract does not authorize phone, email, role, tenant or employee changes through this operation.
- Failure behavior: Missing or invalid authentication returns 401, an unavailable scoped profile returns 404, and request or time-zone validation failures return the canonical 422 Problem response.
- Evidence: `UserProfileServiceImplTest`, `UserProfileHttpTest`, gateway `SecurityConfigurationTests`, and the PostgreSQL scenario in `IdentityPersistenceIT`. The focused unit/HTTP and gateway suites pass, and the Docker-backed Maven verification completed all six identity integration tests successfully on 2026-10-26.

### BF-042  Change own password — Status: IMPLEMENTED

- Purpose: Let an authenticated user change their own password through `POST /auth/change-password`, exposed by the gateway as `POST /api/v1/auth/change-password`.
- Main behavior: The service derives user and tenant IDs from the service-validated JWT, locks the enabled tenant-scoped account, verifies the submitted current password with BCrypt, stores only the encoded replacement and revokes every active refresh session for that user and tenant.
- Security boundary: Both gateway and identity service require JWT authentication. The request cannot select a user or tenant, and stored token claims must still match the account before the credential mutation occurs.
- Failure behavior: Missing or invalid authentication and an incorrect current password return 401; malformed input or a new password shorter than eight characters returns the canonical 422 Problem response. Failed verification does not update the password or revoke sessions.
- Data and events: The transaction updates `identity_org.auth_user.password_hash` and tenant-scoped `refresh_session.revoked_at`; it publishes no business event. Existing access JWTs remain valid until expiry.
- Evidence: `PasswordServiceImplTest`, `AuthPasswordHttpTest`, the full identity unit suite and gateway `SecurityConfigurationTests` pass. The Docker-backed Maven verification also completed the PostgreSQL hash/session-revocation scenario and all six identity integration tests successfully on 2026-10-26.
