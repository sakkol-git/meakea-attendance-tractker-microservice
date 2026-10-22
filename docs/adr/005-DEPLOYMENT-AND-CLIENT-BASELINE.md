# ADR-005: Deployment and client baseline

- Status: Superseded
- Date: 2026-10-22
- Decision owners: contributor A and contributor B; product/HR, security, and operator review where applicable.

## Supersession

[Current ADR](006-FINISH-LINE-MODULAR-MONOLITH.md) supersedes any old seven-service/Kafka/cloud assumptions; other proposals require current review.

## Context

The old plan specifies React and eventual EKS/k3s, while source is Nuxt/Vue and dependency-only Compose. No cloud/cluster/IaC/CI resources exist. One contributor owns backend/web/infrastructure and the budget alert is `$30`, not a cost forecast.

## Problem

Framework rewrite and premature Kubernetes can consume the twelve-week delivery without improving core correctness.

## Decision

Propose retaining Nuxt for MVP and using a simple reproducible container pilot deployment before Kubernetes. Keep Flutter as intended mobile client. O01 selects frontend and O04 selects hosting/cost/operations. EKS is optional future infrastructure, not a release requirement. No dependency changes are authorized by this ADR.

## Alternatives Considered

React aligns with original UI plan but requires replacing scaffold; k3s provides Kubernetes with self-managed operations; EKS supplies managed control plane but adds cost and networking complexity; a single host is simpler but not highly available.

## Consequences

Preserves implementation momentum with explicit limitations. Production launch still needs TLS, secrets, backups, restore/rollback, private stores, and an operator. Deployment simplicity does not waive security.

## Security and Operational Implications

Single-host/broker failures are expected pilot outage risks. Forecast cloud spend before provisioning; budget alert does not cap spend. Keep database/Kafka/Registry private.

## Implementation and Migration

Complete local startup, CI/images, and staged smoke first. Record final target and accepted risk before real users. If React is selected, migrate before feature UI work; API contracts remain independent of framework.

## References

- [Detailed specification](../Architecture.md).
- [Execution plan](../PLAN.md).
