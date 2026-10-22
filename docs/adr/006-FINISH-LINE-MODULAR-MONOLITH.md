# ADR 006 — Finish-line modular monolith

## Status and owners

Status: Accepted for the build guide by the explicit 2026-10-23 user instructions and the default consolidation rule for mostly unbuilt code. Owners: A and B; HR/privacy/host readiness still require the named external input. Supersedes ADR 001 deployment/communication baseline and ADR 005 client/hosting proposals. No claim that code has migrated.

## Context and problem

Seven services contain only startup scaffolds, source is Nuxt, current host has about 5 GiB free, and no deployed system exists. A junior team cannot justify seven deployment/broker boundaries from existing behavior. User explicitly requires React, existing-host Compose, safe HTTPS, nightly external backups and a twelve-week deadline.

## Decision

- **Decision:** Outcome C, one Boot app with domain packages and owner schemas; local transactional outbox/receipts for notification/reporting; React and early mocked Flutter.
- **Reason:** Counted step ledger is 195.5 core contributor-hours plus reading/buffer within provisional 393.12 planned hours at interpreted 24 h each. More deployables add unproven operational work.
- **Rejected alternatives:** Seven microservices, separate worker runtime, Kubernetes/Terraform/cloud infrastructure, Kafka runtime without two truly independent async components.
- **Implementation impact:** Migration is incremental on code branches, old source preserved; routes and tenant/domain correctness remain. OpenAPI is one wire source; package APIs/projections enforce ownership.
- **Cost and operational impact:** One host/DB remains a shared failure domain. Tunnel is free publishing plus domain cost, not free hardware/power/ISP or high availability. External backup media is required.
- **Reconsider when:** Measured independent deployment/load need and a tested capacity/operations budget justify extracting a component.

## Security and operational implications

Token-derived tenant, object checks, scoped constraints, HTTPS/auth/firewall/secrets/verified backup before exposure remain mandatory. Cloudflare public TLS and private Caddy origin are chosen because WAN capability is unknown. Nightly custom dump and external copy plus scratch/clean-host restores protect data; provider/ISP/power outage still stops remote availability.

## Implementation and migration

[Migration plan](../roadmap/00-PREPARATION/MIGRATION-PLAN.md) names the inspected current files and proposed target paths. Java 21/Boot 4.1.1 existing root baseline remains; no app dependencies changed by documentation. Kafka-specific tests are not falsely claimed for local receipts. Future Kafka reintroduction requires new ADR and real broker crash/offset tests.

## Consequences

Development gets fewer operational boundaries, while data/package discipline requires explicit tests. Estimated capacity remains conditional on actual hours and junior velocity. Unknown host/domain/disk/start date are blockers, not blank completed tasks. No pattern quota overrides current need.

## References

- [Architecture](../Architecture.MD).
- [Decisions](../roadmap/DECISION-REGISTER.md).
- [Capacity](../roadmap/00-PREPARATION/PROJECT-BASELINE.md).
