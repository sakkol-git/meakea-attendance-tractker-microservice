# ADR-003: Event delivery and contracts

- Status: Proposed
- Date: 2026-10-22
- Decision owners: contributor A and contributor B; product/HR, security, and operator review where applicable.

## Supersession

[Current ADR](006-FINISH-LINE-MODULAR-MONOLITH.md) supersedes any old seven-service/Kafka/cloud assumptions; other proposals require current review.

## Context

The existing plan requires outbox/inbox and versioned events. The repository has one flat Avro check-in record, a partially configured Registry, and no relay/consumer implementation. JSON Schema/AsyncAPI in the old plan conflicts with Avro source.

## Problem

Need durable publication, safe duplicate/order handling, and one explicit contract strategy without claiming distributed exactly-once behavior.

## Decision

Implement atomic business+outbox writes, lease-based ordered aggregate-head relay, stable event IDs, local inbox+effect transactions, and Kafka acknowledgment only after commit. Snapshots guard aggregateVersion. Proposed normalized envelope and catalog are authoritative design; serialization choice O06 remains open. Prefer completing existing Avro path only if its operational cost/compatibility tests are accepted; otherwise choose JSON explicitly before publishing.

## Alternatives Considered

Direct DB+Kafka dual-write can lose/phantom facts; Kafka transactions do not atomically commit PostgreSQL; CDC/Debezium reduces custom relay work but adds operations; in-memory deduplication fails across restart.

## Consequences

At-least-once delivery is expected. Duplicate publication after crash is harmless only with tested consumer/domain safeguards. SKIP LOCKED does not guarantee aggregate order without head eligibility. Replay retention and compaction need intentional keys.

## Security and Operational Implications

Restrict producers by topic ACL; protect DLQ payloads. Never replay live provider notifications during projection rebuild. Monitor age/backlog, not only broker process health.

## Implementation and Migration

Correct Avro generation path and Registry connectivity if retained. Do not mutate legacy flat schema in place into an incompatible envelope. Add contract fixtures/compatibility checks and a controlled transition before first real producer.

## References

- [Detailed specification](../Architecture.md).
- [Execution plan](../PLAN.md).
