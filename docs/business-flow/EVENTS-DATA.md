# Event and data reference

## VI Events and integration inventory

[IMPL] No production publisher, listener, scheduled relay or inbox handler exists in services/ or libs/. Scripted topics are declarations only. Producer/consumer columns below are DESIGN ownership from the approved service/topic catalogues; keys are unimplemented contracts. (docs/business-flow/_inventory.md — Events/topics/jobs; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §9–10)

| Topic or pattern | Source status | Intended producer → consumers | Intended key / meaning | Workflow |
| --- | --- | --- | --- | --- |
| org.employee.v1 | [IMPL] Scripted compacted | [DESIGN] identity-org → attendance/leave/request/notification/reporting | tenantId:employeeId; employee master | BF-004, BF-038 |
| org.shift.v1 | [IMPL] Scripted compacted | [DESIGN] identity-org → attendance | tenantId:shiftId; definitions/assignments | BF-005, BF-038 |
| org.holiday.v1 | [IMPL] Scripted compacted | [DESIGN] identity-org → attendance/leave | tenantId:date; holiday calendar | BF-006, BF-038 |
| attendance.evidence.v1 | [IMPL] Scripted | [DESIGN] attendance → later evidence/inference consumers | tenantId:employeeId; presence aggregates | BF-009, BF-010 |
| attendance.session.v1 | [IMPL] Scripted | [DESIGN] attendance → notification and confidence labels; reporting sees daily facts | tenantId:employeeId; tap/close/infer/confirm/reject | BF-007, BF-008, BF-011, BF-012 |
| attendance.daily-status.v1 | [IMPL] Scripted | [DESIGN] attendance → notification/reporting | tenantId:employeeId; revised employee-day result | BF-013, BF-029 |
| attendance.leave-conflict.v1 | [IMPL] Scripted; absent in main topic catalogue | [DESIGN] Conflict route under proposed ADR 004; exact bindings unresolved | [UNKNOWN] Active Avro/key contract missing | BF-020 |
| leave.request.v1 | [IMPL] Scripted | [DESIGN] leave → attendance/notification/reporting | tenantId:employeeId; submitted/approved/rejected/cancelled | BF-014–017, BF-029 |
| request.workflow.v1 | [IMPL] Scripted | [DESIGN] request → notification/reporting | tenantId:requestId; chain lifecycle | BF-022–025, BF-029 |
| time.shift-cutoff.v1 | [IMPL] Scripted | [DESIGN] attendance scheduler → attendance | tenantId:employeeId; window closed | BF-037, BF-013 |
| attendance.scoring-requested.v1 | [DESIGN] Proposed, not scripted | attendance → confidence | tenantId:employeeId; feature snapshot | BF-010, BF-032 |
| attendance.confidence.v1 | [DESIGN] Proposed, not scripted | confidence → attendance | tenantId:employeeId; score/decision/modelVersion | BF-010 |
| notification.sent.v1 | [DESIGN] Optional, not scripted | notification → unspecified subscribers | [UNKNOWN] Final key/schema/subscriber absent | BF-027 |
| `<topic>.retry` | [DESIGN] Pattern only, not scripted | failed handler → delayed retry consumer | same key; delay header, three-day target retention | BF-036 |
| `<topic>.dlq` | [DESIGN] Pattern only, not scripted | poison/exhausted path → operator replay | raw bytes/error, fourteen-day target retention | BF-036 |

[DESIGN] Topic ownership/keys/payload meanings above come from approved part02 §9/10.1 and part03 §12–16; exact attendance-evidence downstream subscription is not fully specified. [IMPL] Script policies are six partitions and replication one for all ten topics, compact only for the three org topics; script does not configure design retention durations or retry/DLQ topics. (infra/scripts/create-topic.sh; docs/reference/INCOMING-ARCHITECTURE-V2-02.md — §10.1)

### Existing Avro contract

[IMPL] Only libs/hrflow-contracts/events/attendance-checked-in.v1.avsc#AttendanceCheckedIn exists. It has eventId UUID-string, tenantId, employeeId, occurredAt timestamp-millis, source enum MANUAL/NUDGE/INFERRED, and nullable reason. There is no implemented serializer call, topic binding or listener. (same Avro record; libs/hrflow-contracts/pom.xml — schema plugin)

[DESIGN] The proposed common envelope adds type/aggregateId/correlationId/causationId, and the attendance chapter uses TAP sessions. [IMPL] Those fields/TAP enum symbol are absent from the sole Avro record; it must not be mistaken for the full session/evidence event contract. Its record name does not establish a topic name. (approved part02 §10.3/11.7; existing Avro record)

### Failure and consistency boundaries

[DESIGN] Owner mutation/audit/outbox commit precedes broker publication. Broker acknowledgment precedes marking sent; one active owner relay is elected by advisory lock. Consumer receipt/effect commit precedes offset acknowledgment. Duplicate transport is expected after send-before-mark crashes. These are requirements, not current guarantees. (docs/adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md — Tenant carry-forward and Relay ordering refinement; docs/REST-API-DATABASE-DESIGN.md — Broker and identity boundary)

[DESIGN] Relay approximately200ms, reporting typically-under-one-second, three in-process consumer retries and five channel delivery attempts are separate design statements. [UNKNOWN] No measured end-to-end delay, maximum consistency window, numeric lag-alert threshold or replay completion SLA exists. Sender uniqueness does not prove an external provider never accepts a duplicate. (approved part02 §10.6–10.8; part03 §14/15.2)

## VII Data map

[IMPL] There are no business CREATE TABLE migrations. The six schemas in infra/docker/postgres-init.sql are containers, not tenant policies or business records. Names below are proposed and sometimes conflict across documents; no installed database shape is inferred. (docs/business-flow/_inventory.md — Migrations and tables)

| Workflow group | Owner / proposed data | Intended constraints and transaction | Deletion / retention | Source |
| --- | --- | --- | --- | --- |
| BF-001–004, BF-040 | identity_org.tenants/users/refresh_tokens/departments/positions/employees | [DESIGN] Tenant email/employee-code uniqueness; token hashes; local owner transaction | Master data soft delete; token revocation/retention not migrated | API ownership catalog; architecture part01 §8 |
| BF-005/006/038 | Shift/holiday owner plus local employee_ref/shift_ref/holiday_ref | [DESIGN] Owner facts copied by event/revision; trusted tenant context | Compaction/tombstones and replay need actual contracts | part02 §9/10.9; part03 §17.4 |
| BF-007–013 | attendance_session/evidence/daily_status vs catalog attendance_sessions/raw_attendance_events/shift_rules/attendance_corrections | [DESIGN] Valid interval, one effective open session and one tenant/employee/day summary; preserve source/evidence | Append evidence/corrections; monthly evidence partitions; retention jobs absent | part02 §11; API catalog |
| BF-014–020 | leave_balances/leave_requests/leave_request_days/leave_types; chapter leave_balance and unnamed ledger | [DESIGN] Scoped employee/type/year uniqueness, exact numeric amounts and conditional reservation; write request/balance/ledger locally | Preserve accounting history; future cancellation compensates, not silent deletion | part02 §12; API catalog |
| BF-021–026 | general_requests/request_approvals/request_attachments vs chapter request/approval_step | [DESIGN] Parent-scoped child FK, versioned decision and private attachment authorization; no leave balance effect | Master-data soft delete only; attachment retention/grant cleanup UNKNOWN | part03 §13/17.2; API catalog |
| BF-027/028 | employee_ref, recipient inbox/preferences/templates/device tokens/delivery_attempt | [DESIGN] Intended event/recipient/channel dedup, private recipient reads | Retry up to five; retention and provider duplicate boundary UNKNOWN | part03 §14 |
| BF-029–031 | employee_dim/attendance_fact/leave_fact/request_fact/daily_summary/export_job | [DESIGN] Revisioned tenant facts; one active export/tenant; local projection transaction | Target thirty-day event retention does not remove long-lived facts; external retention deletion UNKNOWN | part03 §15 |
| BF-035/036/038 | owner outbox/inbox and private non-personal tenant registry | [DESIGN] Receipt + effect local transaction; metadata-only discovery exception and per-tenant personal work | Inbox must outlive replay window; cleanup implementation absent | ADR 008; part02 §10.6–10.9 |
| BF-032–034 | model.joblib/meta.json, synthetic/labeled CSV and process model | [IMPL] No DB constraints; model/meta loaded once; training writes two files sequentially | No artifact/data retention enforcement in source | ml/confidence-service/confidence/model.py#ConfidenceModel.__init__; ml/confidence-service/training/train.py#main |

[DESIGN] Current API owner catalog requires tenant predicates, tenant-scoped FKs/uniqueness, exact numeric balances, personal-table RLS and non-owner runtime roles. The approved generic inbox sketch lacks tenant in its key; API proposes (tenant_id,event_id,handler_name). [IMPL] No repository SQL, RLS policy or grant migration exists to resolve or enforce either. (docs/REST-API-DATABASE-DESIGN.md — Multi-tenant persistence rules/RLS and role requirements; inventory)

[UNKNOWN] Soft-delete filters, legal retention values, purging of model labels/private attachments, transaction isolation, inventory reservations and cross-year pending-leave settlement cannot be derived from installed migrations because none exist. Do not use the historical SQL reference export as an installed schema. (inventory; part03 §17.5; ADR 004 policy status)
