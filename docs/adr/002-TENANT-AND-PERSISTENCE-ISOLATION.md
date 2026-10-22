# ADR-002: Tenant and persistence isolation

- Status: Proposed
- Date: 2026-10-22
- Decision owners: contributor A and contributor B; product/HR, security, and operator review where applicable.

## Supersession

[Current ADR](006-FINISH-LINE-MODULAR-MONOLITH.md) supersedes any old seven-service/Kafka/cloud assumptions; other proposals require current review.

## Context

Bootstrap SQL creates six schemas on one PostgreSQL instance; attendance selects its schema but shares local hrflow credentials. The old API draft expects tenant-owned records. No migrations or tenant authorization exist.

## Problem

Schema names and tenant_id columns alone do not prevent cross-service or cross-tenant access.

## Decision

Use service-owned schemas with distinct runtime roles and separate migration privileges. Derive tenant from verified token/event/job. Use tenant-scoped queries, composite keys/FKs, resource checks, scoped files/caches/exports. Evaluate RLS only as additional defense with transaction-local context and pool tests. Canonical identity schema is identity_org.

## Alternatives Considered

Database per service improves physical separation but increases cost; database per tenant increases migrations/operations; application-only tenant predicates are simpler but provide fewer database safeguards.

## Consequences

One physical instance keeps MVP cost manageable but shares capacity/outage risk. Composite constraints add verbosity and require consistent repository design. RLS remains optional pending tested operational support.

## Security and Operational Implications

Fail closed on unknown/stale authorization projections. Do not grant runtime roles schema-owner/superuser privileges. Tenant-controlled paths/headers never establish trust.

## Implementation and Migration

Introduce owner migrations/grants before business tables. Backfill and validate tenant associations before enabling stricter constraints on existing data. Test IDs/counts/SSE/files/jobs from a second tenant. This ADR is not approval of an implemented RLS policy.

## References

- [Detailed specification](../REST-API-DATABASE-DESIGN.md).
- [Execution plan](../PLAN.md).
