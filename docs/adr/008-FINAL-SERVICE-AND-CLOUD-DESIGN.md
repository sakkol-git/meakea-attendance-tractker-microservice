# ADR 008 — Final service design and cloud deferral

## Status and evidence

Accepted by explicit user instruction on 2026-10-23. Supersedes ADR 006 topology/client/hosting choices and the local-dispatch portion of ADR 007. Historical files/commits remain intact. The latest Architecture text was approved as final; a subsequent answer initially required AWS/Kubernetes within twelve weeks, followed by the latest instruction to skip it for now as a future improvement. The final instruction controls: retain cloud design, defer its execution. This is design authorization, not cloud spending approval or an implementation completion claim.

## Decision

- **Decision:** Seven Spring services, Kafka KRaft with Avro/Schema Registry, Spring JDBC, reactive gateway, Nuxt web, Flutter mobile and the Python confidence worker. Local Compose is the twelve-week deployment target. Preserve the approved AWS/Terraform/Kubernetes topology for future implementation, including its database/storage design; no cloud distribution or service sizing is selected without budget evidence.
- **Reason:** The user's latest final design replaces the earlier finish-line simplification. The latest cloud deferral removes cloud execution from the deadline without changing service/data/event boundaries.
- **Rejected alternatives:** Modular monolith and React are superseded. An AWS deadline is superseded by the latest deferral; selecting EKS versus k3s, RDS/MSK sizing and networking awaits a separate budgeted plan.
- **Implementation impact:** Retain existing service roots and Nuxt; align poms, replace business JPA with JDBC, switch gateway MVC to reactive dependencies with a supported Cloud/Boot compatibility spike, add real Kafka integration tests and Avro compatibility checks. Services communicate through events/local projections, never cross-service SQL or business HTTP.
- **Cost and operational impact:** Open-source runtime software does not make EC2, EBS, S3, public networking or egress free. Region, credits and budget must be recorded before apply. Single-computer deployment has outage and data-loss risks; test external backup and full restore before acceptance.
- **Reconsider when:** The user changes the requirement, or measured resource/velocity evidence requires an explicit budget, scope or deadline decision. Do not silently return to ADR 006 to make the schedule fit.

## Tenant and quality carry-forward

Retain ADR 007's tenant-scoped JDBC, non-owner database role, tested personal-data RLS, RFC 9457 errors and owner-local audit/outbox. Replace its in-process dispatcher with publisher acknowledgment and Kafka consumers: crash after broker acknowledgment before marking sent can duplicate, so every consumer commits receipt and effect together before committing offset. Outbox ordering follows aggregate revision and partition key; dead letters are observable and replayable.

The final design explicitly chooses gateway header trust. Preserve it as a boundary decision; strip all incoming identity headers, derive trusted identity only after JWT verification, deny direct service ingress and restrict monitoring to management endpoints. Services enforce method/resource roles and repository tenant scope. Tests must prove NetworkPolicy enforcement by the selected CNI; a YAML file alone is not evidence. Q04 remains conditional until these tests prove provenance. A narrower per-service JWT defense is a potential future security change, not silently accepted here.

RLS is adopted for personal-data tables as planned defense, with transaction-local context and separate migration/runtime roles. Its engineering estimate is included in the review; capacity is currently insufficient, so adoption is not called implemented or funded by an invented saving.

## Ordered migration and verification

1. Preserve staged/untracked source and record current service poms, package roots, schema names and Nuxt lockfile. No application move to hrflow-app and no React rewrite.
1. Establish Java 21 and disk/Docker prerequisites. Spike the exact Boot/Cloud/Avro/Testcontainers combination before selecting versions. Keep the gateway reactive and JDBC explicit; compile each change separately.
1. Repair Compose SQL mount and listener addresses from observed config. Start PostgreSQL/Kafka/Registry privately, test the Avro sample and owner transaction/consumer receipt under crash and replay.
1. Build identity golden slice with token-derived gateway identity, scoped JDBC and RLS. Verify spoofing, direct ingress denial, auth/refresh/throttle, method authorization and error contract before public exposure.
1. Migrate every old roadmap reference through the impact register and ID map. Preserve old green tags; do not rename a prior tag to imply a different topology passed.
1. Deploy all final service workloads locally using reviewed image versions, private networks, real secrets and verified external backup. No AWS, Terraform apply or Kubernetes cluster is a prerequisite for Week 12.
1. Repeat clean-host Compose deployment/rollback/restore in Week 10 and release by Week 12 only if quality gates have actual evidence. Keep the AWS/Kubernetes backlog explicitly future and unexecuted. A failed capacity or cost gate is not a release pass.

## Consequences

The old capacity assessment does not transfer. [Architecture review](../roadmap/ARCHITECTURE-REVIEW-V2.md) gives the recalculated planning estimates and blocking conflicts. [Architecture](../Architecture.md) owns the final design. This record does not assert high availability, compliance certification, a cloud spend ceiling, or guaranteed delivery.

## Relay ordering refinement from the approved design

Retain one active outbox relay per owner service, chosen with PostgreSQL advisory lock on a dedicated relay connection; standby instances do not publish. Bounded SKIP LOCKED batches do not by themselves preserve per-key ordering across multiple publishers. Connection/lock loss stops publication until leadership is re-established. Inbox/revision checks still handle duplicate or stale delivery; this does not create host/broker failover.

## Tenant context for background discovery

Requests get tenant only from the verified token boundary. Background processing must not query personal outbox/job payloads with missing RLS context or give a runtime role BYPASSRLS just to find work. Each owner maintains a proposed non-personal tenant registry containing only trusted organization-event UUID/lifecycle/revision metadata. A separate private job role can enumerate that fixed metadata; business HTTP repositories cannot use this infrastructure-only discovery grant.

The worker then enters one tenant transaction at a time, sets transaction-local context from the stored trusted metadata and uses tenant-scoped predicates for its personal outbox/job/effect queries. Discovery exposes no employee, payload or business row and is the explicit infrastructure metadata exception to ordinary repository scoping; every business query remains scoped. Tests prove absent-context payload denial, worker pool tenant switches, rollback cleanup and no HTTP access to discovery. The existing organization bootstrap/RLS-role steps own this small metadata/grant change; no new service or public tenant selector is introduced.
