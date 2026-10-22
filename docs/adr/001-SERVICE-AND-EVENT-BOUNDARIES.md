# ADR-001: Service and event boundaries

- Status: Superseded
- Date: 2026-10-22
- Decision owners: contributor A and contributor B; product/HR, security, and operator review where applicable.

## Supersession

[Current ADR](006-FINISH-LINE-MODULAR-MONOLITH.md) supersedes any old seven-service/Kafka/cloud assumptions; other proposals require current review.

## Context

The pre-existing Plan.md explicitly defines seven domain services, gateway-only client entry, Kafka-only business integration, local read models, and no cross-service joins. Source contains matching service scaffolds but not business implementations.

## Problem

Preserve domain ownership while avoiding synchronous business dependencies and an unrealistic all-services-first delivery strategy.

## Decision

Retain api-gateway, identity-org, attendance, leave, request, notification, reporting as logical boundaries. Clients call gateway; services integrate business facts through Kafka. No cross-service SQL/foreign keys or business HTTP. Acceptance refers only to the pre-existing intended baseline, not working deployment. A proposed security-control-plane exception permits cached JWKS discovery. Build one end-to-end slice first; merging reporting/notification projections remains an existing documented contingency.

## Alternatives Considered

A modular monolith reduces operations but changes the established baseline; independent services with HTTP simplify reads but couple availability; a service mesh adds unjustified MVP complexity.

## Consequences

Local reads keep check-in independent of remote latency. Eventual consistency, snapshot evolution, reconciliation, and additional deployments are real costs. Boundary changes require new evidence and ADR rather than directory renames.

## Security and Operational Implications

Gateway auth does not replace service authorization. Topic ownership, private listeners, and tenant scope must be enforced. A shared DB/broker still creates shared failure domains.

## Implementation and Migration

Implement identity/attendance/event foundation before expanding other workflows. Record and test any communication exception. No HTTP business fallback during projection lag.

## References

- [Detailed specification](../Architecture.md).
- [Execution plan](../PLAN.md).
