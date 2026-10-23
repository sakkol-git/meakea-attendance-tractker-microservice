# Notification workflows

[DESIGN] This is a domain body of the [business-flow reference](../../business-flow.md). Evidence labels distinguish targets from current source; status applies to the whole named workflow. Source absence is bounded by the [inventory](_inventory.md).

## Workflows

### BF-027  Notification delivery — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Inform recipients about domain changes using available channels. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14)
- Trigger / entry point: [DESIGN] Domain event from attendance/leave/request/org. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14)
- Actors and permissions: [DESIGN] Worker resolves recipients from local employee_ref; access remains tenant/user scoped. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14)
- Preconditions and inputs: [DESIGN] Event, category, locale, template and preference/channel data. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14)
- Main flow:

    1. [DESIGN] Deduplicate event and resolve recipient/category. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14)
    1. [DESIGN] Render localized message and create in-app inbox/channel attempt. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14)
    1. [DESIGN] Deliver enabled channel, retry described failures and record terminal outcome. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Provider failure | Design exponential retry up to five attempts then failed + alert | §14; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14 |
    | [DESIGN] Critical approval category | Cannot be fully disabled | §14; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Send succeeds but DB mark fails | External duplicate-send window unresolved, not prevented by unique row alone | §14; outbox/inbox failure boundary; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14 |
    | [DESIGN] No provider credentials | Delivery channel unavailable; no live adapter | Inventory; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14 |

- State transitions: [DESIGN] Event → intended inbox/delivery_attempt → delivered or failed; exact persisted enum UNKNOWN. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14)
- Data effects: [DESIGN] Proposed recipient inbox/delivery_attempt/employee_ref; no notification migrations. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] Consumes domain topics; optional notification.sent.v1 is not scripted; SSE/push/email are not implemented. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/notification/src/main/java/com/hrflow/notification/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-053, QA-054; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-028  Notification preferences and inbox reads — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Let a user manage allowed channels and read only their own notifications. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical))
- Trigger / entry point: [DESIGN] Preference GET/PUT and inbox actions; all candidate routes remain design. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical))
- Actors and permissions: [DESIGN] Authenticated user self; administrative template policy is separate. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical))
- Preconditions and inputs: [DESIGN] Category/channel preferences, optional quiet hours, locale and revision. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical))
- Main flow:

    1. [DESIGN] Read current own inbox/preferences. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical))
    1. [DESIGN] Validate critical categories and update permitted channel settings. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical))
    1. [DESIGN] Apply settings to later delivery decisions, not as proof of already sent message retraction. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical))

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Critical approval-needed category | Cannot disable every channel | §14; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical) |
    | [DESIGN] Quiet hours | Optional; reminders still allowed | §14; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical) |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Foreign recipient or stale preference revision | Active API 404/412 semantics designed; not enforced | API conventions; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical) |
    | [UNKNOWN] Preference change races queued delivery | Snapshot timing UNKNOWN | No delivery/preference implementation; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical) |

- State transitions: [DESIGN] Preference revision → newer revision; inbox read-state lifecycle not implemented. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical))
- Data effects: [DESIGN] Proposed preference/inbox tables; no exact installed names or uniqueness. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical); docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] No preference-changed topic is authoritative; SSE /notifications/stream is only design shorthand. (docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §14; docs/reference/CONTRACTS-REFERENCE-08.md — API-099/100 (historical); docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Intended mutation key scope is tenant/actor/method/path/body hash; mutable-version updates use 428/412. [IMPL] No business implementation enforces it for this workflow. (docs/REST-API-DATABASE-DESIGN.md — Wire conventions; docs/business-flow/_inventory.md — Modules/source searches)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/notification/src/main/java/com/hrflow/notification/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain.
- QA scenarios: QA-055, QA-056; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)
