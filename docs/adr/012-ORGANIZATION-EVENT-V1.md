# ADR 012 — Provisional organization event v1 snapshots

## Status and scope

**Accepted provisionally:** 2026-10-27 by explicit user choice to use the archived event catalog's employee, shift and holiday snapshot shapes, aligned with the accepted `employeeNo`, `departmentId` and `positionId` API decision.

This fills the typed payload gap for the three topics already owned by identity-org in ADR 008. It does not accept the archived catalog's other topics or HR policy. The machine contract is `libs/hrflow-contracts/events/organization-fact.v1.avsc`. The prior reference remains historical.

## Wire decision

- Identity-org publishes full snapshots on `org.employee.v1`, `org.shift.v1` and `org.holiday.v1`, keyed by `tenantId:aggregateId`. One Avro envelope carries a typed union of employee, shift and holiday payloads. `eventId` remains stable across retries. `aggregateVersion` is a positive owner revision, not an HTTP ETag or Kafka offset.
- Employee snapshots include all effective assignments, employee number, department and position IDs/names, manager, status and optional identity/device/locale fields. Shift snapshots include the assigned site, zone, location radius and optional Wi-Fi fields. Holiday snapshots include stable ID, date, kind and explicit removal time.
- UUIDs, UTC instants, local dates and local times are strings with the format documented in the catalog. New consumers validate them before applying effects. A null optional value means the owner does not currently know that fact; it is not a default HR policy. In particular, the current employee API permits missing hire date, so v1 represents `hireDate` as nullable until the business rule is approved.
- Delivery is at least once. The relay marks sent only after broker acknowledgment. A consumer commits its inbox receipt and projection effect in one database transaction before committing the offset; it ignores duplicate IDs and non-newer aggregate revisions. Snapshot bootstrap and replay must be verified before readiness.

## Boundaries and verification

The Avro schema is an additive new contract. Register and check its subject compatibility against an actual Schema Registry before publishing. Producer and consumer fixtures must encode/decode all three payload variants, reject wrong topic/type combinations and malformed tenant/identity values, and cover broker-ack/mark-sent crashes, duplicate delivery and tenant isolation. No schema file, mock or local outbox row alone proves broker delivery or attendance acceptance.

This decision adds contract maintenance and a small event mapping surface. It uses the existing Kafka/Schema Registry stack, with no new paid service. Changes to required fields or semantics after deployment require compatibility review or a new event version; historical events remain decodable.
