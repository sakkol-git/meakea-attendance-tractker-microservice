# Event infrastructure and diagnostic workflows

[DESIGN] This is a domain body of the [business-flow reference](../../business-flow.md). Evidence labels distinguish targets from current source; status applies to the whole named workflow. Source absence is bounded by the [inventory](_inventory.md).

## Workflows

### BF-035  Owner outbox relay — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Publish committed owner facts after the user command completes. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement)
- Trigger / entry point: [DESIGN] Proposed scheduled polling, approximately 200 ms/batches100 in design. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement)
- Actors and permissions: [DESIGN] Private owner worker; trusted tenant registry/context before personal outbox access. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement)
- Preconditions and inputs: [DESIGN] Owner outbox row with event identity/key/revision; exact JSONB-vs-bytea storage model conflicts. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement)
- Main flow:

    1. [DESIGN] Business handler writes owner mutation/audit/outbox in one intended local transaction. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement)
    1. [DESIGN] One relay leader holds dedicated PostgreSQL advisory lock and publishes in owner order. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement)
    1. [DESIGN] Wait for broker acknowledgment before marking sent; stop if leadership/connection is lost. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Crash after ack before mark | Duplicate publication possible; consumer dedup required | §10.6; ADR 008; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement |
    | [DESIGN] Multiple relay instances | Only active elected owner relay publishes; SKIP LOCKED alone insufficient | ADR 008; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Broker unavailable | Keep committed row pending; retry/recovery unimplemented | §10.6; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement |
    | [DESIGN] Lost advisory lock/connection | Stop publishing; no leader election code exists | ADR 008; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement |

- State transitions: [DESIGN] Committed pending outbox → acknowledged/sent; duplicate window remains. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement)
- Data effects: [DESIGN] API proposes attendance.outbox_events JSONB/status vs approved chapter outbox bytea/published_at; neither migrated. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] Potentially every publishing owner topic; no producer code. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Send-before-mark may republish; owner advisory-lock leadership/ordering and consumer dedup are required, not implemented. HTTP keys belong to the originating command, not the relay poll. (docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Relay ordering refinement; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.6)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/api-gateway/src/main/java/com/hrflow/gateway/config/SecurityConfiguration.java#SecurityConfiguration.securityWebFilterChain.
- QA scenarios: QA-069, QA-070; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-036  Inbox consumption, retry and dead letter — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Apply received facts once per intended handler while retaining failed work. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables)
- Trigger / entry point: [DESIGN] Kafka consumer delivery; no KafkaListener exists. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables)
- Actors and permissions: [DESIGN] Private consumer with trusted event tenant and scoped local effect. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables)
- Preconditions and inputs: [DESIGN] Validated Avro record/eventId, handler identity and schema; poison raw bytes for quarantine. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables)
- Main flow:

    1. [DESIGN] Insert intended receipt and local effect in the same owner transaction. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables)
    1. [DESIGN] Commit effect/receipt before offset acknowledgment; skip a duplicate receipt. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables)
    1. [DESIGN] Retry transient errors, route exhausted/poison work to designed retry/DLQ and alert/replay. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Duplicate event | Skip effect only after matching committed handler receipt | §10.7; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables |
    | [DESIGN] Transient / poison failure | Design three in-process retries then retry-topic; poison to DLQ | §10.8; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Effect fails before commit | Intended receipt/effect rollback; offset must not advance | API broker boundary; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables |
    | [DESIGN] DLQ delivery fails or replay repeats | No handler/operator implementation or retry topic exists | Inventory; §10.8; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables |

- State transitions: [DESIGN] Received → committed/duplicate-skipped or quarantined; exact persistent retry states UNKNOWN. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables)
- Data effects: [DESIGN] Approved inbox(consumer,event_id) differs from API inbox_receipts(tenant_id,event_id,handler_name); no unique index installed. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] `<topic>.retry` and `<topic>.dlq` are only patterns, absent from script; eventual delay bounds UNKNOWN. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7–10.8; docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Receipt identity and effect must commit together before offset acknowledgment; current API receipt uniqueness includes tenant/event/handler. [IMPL] No inbox constraint/handler exists. (docs/REST-API-DATABASE-DESIGN.md — Transactional outbox and consumer inbox tables; docs/business-flow/_inventory.md)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/api-gateway/src/main/java/com/hrflow/gateway/config/SecurityConfiguration.java#SecurityConfiguration.securityWebFilterChain.
- QA scenarios: QA-071, QA-072; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-037  Shift cutoff scheduling — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Create a time event even when no attendance activity arrives. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6)
- Trigger / entry point: [DESIGN] Tenant-local shift end plus buffer; no scheduled Java method exists. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6)
- Actors and permissions: [DESIGN] Attendance background job with local shift_ref and trusted employee/tenant metadata. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6)
- Preconditions and inputs: [DESIGN] Employee/day shift window, timezone and buffer; exact buffer/default missed-run catch-up UNKNOWN. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6)
- Main flow:

    1. [DESIGN] Find eligible employee-local cutoff times. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6)
    1. [DESIGN] Publish time.shift-cutoff.v1 without editing another owner database. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6)
    1. [DESIGN] Attendance consumes the trigger through BF-013 finalization. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] No tap | Still emit time trigger so absence can be finalized | §10.10; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6 |
    | [DESIGN] Timezone | Derive from tenant/shift local references | §10.10; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6 |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Host down at cutoff | Catch-up/replay semantics not implemented | No scheduler source; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6 |
    | [DESIGN] DST or duplicate tick | Need explicit single-day/event identity guards | §11.11; unresolved windows; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6 |

- State transitions: [DESIGN] Scheduled cutoff due → cutoff fact; no actual job checkpoint. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6)
- Data effects: [DESIGN] Proposed local shift_ref/job/outbox; no installed scheduler table or transaction. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] time.shift-cutoff.v1 → attendance; notification time.* scheduling details incomplete. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10, §11.6; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [UNKNOWN] Duplicate-tick event identity, missed-cutoff catch-up and leader coordination are unspecified. [DESIGN] Daily recomputation uses newer revisions; no scheduler enforces it. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.10/11.11; docs/business-flow/_inventory.md)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/api-gateway/src/main/java/com/hrflow/gateway/config/SecurityConfiguration.java#SecurityConfiguration.securityWebFilterChain.
- QA scenarios: QA-073, QA-074; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-038  Local reference read-model bootstrap — Status: DESIGNED-ONLY

- Purpose (business): [DESIGN] Make local owner references usable before dependent commands. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery)
- Trigger / entry point: [DESIGN] Empty read model/service startup/rebuild; no bootstrap consumer implemented. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery)
- Actors and permissions: [DESIGN] Private operator/consumer; tenant discovery exception is metadata-only. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery)
- Preconditions and inputs: [DESIGN] Compacted org topics, consumer-group boundary and trusted tenant registry. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery)
- Main flow:

    1. [DESIGN] Replay compacted employee/shift/holiday references from beginning as required. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery)
    1. [DESIGN] Catch up to the chosen boundary and apply newer revisions only. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery)
    1. [DESIGN] Expose readiness only after reference bootstrap; enter tenant-scoped transactions for personal work. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [DESIGN] Empty references | Readiness DOWN until bootstrapped by intended design | §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery |
    | [DESIGN] Tenant discovery | Only fixed non-personal metadata exception, not broad unscoped personal reads | ADR 008; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [DESIGN] Topic retention/tombstone gap | Full snapshot/reconciliation details not implemented | §10.9; §17.4; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery |
    | [DESIGN] Current readiness | Only readinessState/db configured; no catch-up guard | services/attendance/src/main/resources/application.yaml; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery |

- State transitions: [DESIGN] Empty/stale local references → caught-up ready model in design. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery)
- Data effects: [DESIGN] Proposed employee_ref/shift_ref/holiday_ref and metadata tenant registry; no tables/consumer offsets gate exists. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [DESIGN] org.employee.v1, org.shift.v1, org.holiday.v1; compaction alone is not proof of a complete historical rebuild. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.9; docs/reference/INCOMING-ARCHITECTURE-V2-03.md — §17.4; docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant context for background discovery; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: [DESIGN] Replay must tolerate duplicates/older revisions and gate readiness; HTTP mutation keys are N/A to reference consumption. [IMPL] No consumer/replay guard exists. (docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.7/10.9; docs/business-flow/_inventory.md)
- Security: [DESIGN] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/api-gateway/src/main/java/com/hrflow/gateway/config/SecurityConfiguration.java#SecurityConfiguration.securityWebFilterChain.
- QA scenarios: QA-075, QA-076; additional cross-cutting tests are in Part VIII. [DESIGN] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Business controller/transaction/listener absent; designed sequence is not live behavior. (docs/business-flow/_inventory.md — Modules/source searches)

### BF-039  Technical probes and API documentation — Status: PARTIAL

- Purpose (business): [IMPL] Distinguish diagnostics/documentation from HR business behavior. (services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app)
- Trigger / entry point: [IMPL] Configured Java management health URLs and E04–E07 FastAPI generated docs. (services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app)
- Actors and permissions: [IMPL] Health allowlist only in Java; Python generated docs have no authentication. (services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app)
- Preconditions and inputs: [IMPL] N/A: diagnostic/docs reads. (services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app)
- Main flow:

    1. [IMPL] Java config exposes health/probes on private management ports; real handler behavior comes from Boot. (services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app)
    1. [IMPL] FastAPI app setup registers OpenAPI/Swagger/ReDoc routes in-process. (services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app)
    1. [IMPL] Do not count Java test ProbeController paths as production APIs. (services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app)

- Business rules and decisions:

    | Condition | Outcome | Where enforced or specified |
    | --- | --- | --- |
    | [IMPL] Non-health Java request | Anonymous 401; authenticated 403 in filter-chain tests | SecurityConfiguration and security tests; services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app |
    | [IMPL] Python docs paths | Generated route objects found; no server started in this task | Inventory route introspection; services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app |

- Alternative and failure flows:

    | Cause | Response/code | Resulting state and evidence |
    | --- | --- | --- |
    | [IMPL] DB healthy but reference replay missing | Current readiness does not cover business bootstrap | application.yaml readiness group; services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app |
    | [IMPL] Swagger/metadata on public interface | Private config is not an authorization layer | run.py server_options; services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app |

- State transitions: [IMPL] No business state; diagnostics may report only process/configured dependency status. (services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app)
- Data effects: [IMPL] N/A: no business table writes. (services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app; docs/business-flow/_inventory.md — Modules/source searches)
- Events and integrations: [IMPL] N/A: no business events or bootstrap guarantee. (services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app; docs/business-flow/_inventory.md — Modules/source searches)
- Idempotency and concurrency: N/A: diagnostic/documentation reads do not mutate business state. [IMPL] No replay checkpoint is implemented. (services/api-gateway/src/main/resources/application.yaml; docs/business-flow/_inventory.md — Modules)
- Security: [IMPL] Trusted gateway tenant plus service role/object scope and scoped persistence are required. [IMPL] Current Java chains deny business requests; no workflow authorization is implemented. (docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary; docs/business-flow/_inventory.md — Modules/source searches) [IMPL] Current scaffold: services/api-gateway/src/main/java/com/hrflow/gateway/config/SecurityConfiguration.java#SecurityConfiguration.securityWebFilterChain.
- QA scenarios: QA-077, QA-078; additional cross-cutting tests are in Part VIII. [IMPL] These are specifications to test, not execution claims. (../../business-flow.md — Part VIII)
- Findings: [IMPL] Technical route/configuration surfaces exist; business replay readiness does not. [UNKNOWN] No live HTTP validation was performed in this read-only investigation. (services/api-gateway/src/main/resources/application.yaml; ml/confidence-service/confidence/api.py#app; docs/business-flow/_inventory.md)
