# HRFlow API and persistence mapping

## Current wire authority

The sole implementation wire contract will be `contracts/openapi.yaml`, created incrementally in Week 1 and extended alongside tested features. It does not exist today. Prior endpoint schemas/examples are preserved in historical references, not duplicated into a competing generated spec.

## Mapping to outcome C

- Preserve existing `/api/v1` domain route families and status/idempotency/error semantics.
- Replace owner service with owner package in one proposed `services/hrflow-app` JVM behind Caddy; use one private application port, not seven externally routed service ports.
- Keep owner schemas and tenant-scoped constraints. Do not copy all proposed reference tables into a first migration; add tested tables only when a current requirement needs them.
- Keep command idempotency, optimistic versions, explicit row locks, audit and transactional outbox writes. Local dispatcher uses per-handler receipts/inbox and revisions; remove Kafka offset/serializer assumptions from runtime code.
- Keep org/attendance/leave/request/notification/report models and authorized attachments/exports. Cross-domain data moves through explicit public application ports or local events/projections, never unauthorized cross-schema queries.
- Keep full snapshot event meaning and stable IDs, but serialize a bounded JSON envelope in PostgreSQL outbox. Kafka topics/Avro are deferred compatibility references; no broker is needed for MVP.
- Serve private host files with metadata, authorization and backup manifests rather than introducing cloud object storage.

## Evidence and correctness

Every implemented path gets request/response/schema examples in OpenAPI, two-tenant authorization tests, errors, retry semantics and a real database integration test where applicable. Only async job acceptance uses 202; synchronous durable writes use 200/201. Machine schemas are not claimed to exist simply because this mapping is written.

## Detailed preserved contracts

The [reference index](reference/README.md) links all earlier endpoint, DDL and event sections. Their domain constraints remain useful design inputs; seven-service topology, Kafka-only transport and cloud-specific deployment are superseded by [current architecture](Architecture.MD) and [migration plan](roadmap/00-PREPARATION/MIGRATION-PLAN.md).
