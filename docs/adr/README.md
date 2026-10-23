# Architecture decision records

> **Decision:** ADR 006 is the authoritative active decision governing the HRFlow architecture. ADRs 001 through 005 document the design evolution from the initial microservice proposals to the accepted modular monolith.

## Architecture decision register

The following formal records document significant technical decisions, structural trade-offs, and operational boundaries:

| ID | Title | Status | Date | Primary scope |
| --- | --- | --- | --- | --- |
| [001](001-SERVICE-AND-EVENT-BOUNDARIES.md) | Service and event boundaries | Superseded | 2026-10-22 | Initial seven-service microservices and Kafka proposal; superseded by ADR 006. |
| [002](002-TENANT-AND-PERSISTENCE-ISOLATION.md) | Tenant and persistence isolation | Proposed, refined by 007 | 2026-10-22 | Strict database schema boundaries, composite tenant keys, and token-derived tenancy. |
| [003](003-EVENT-DELIVERY-AND-CONTRACTS.md) | Event delivery and contracts | Proposed, refined by 006/007 | 2026-10-22 | Transactional outbox and consumer inbox receipts; runtime broker deferred by ADR 006. |
| [004](004-ATTENDANCE-AND-LEAVE-CONFLICTS.md) | Attendance and leave conflicts | Proposed policy | 2026-10-22 | Precedence rules, manual correction precedence, and human review states. |
| [005](005-DEPLOYMENT-AND-CLIENT-BASELINE.md) | Deployment and client baseline | Superseded | 2026-10-22 | Initial proposal to retain Nuxt and explore Kubernetes; superseded by ADR 006. |
| [006](006-FINISH-LINE-MODULAR-MONOLITH.md) | Finish-line modular monolith | Accepted | 2026-10-23 | Authoritative baseline: single Spring Boot app, domain schemas, React, Caddy, and Tunnel. |

## Supersession and dependency matrix

Understanding the relationship between historical and active decisions ensures consistent implementation:

- Service topology: ADR 001 proposed seven microservices connected via Kafka. ADR 006 supersedes this topology, consolidating services into domain packages (`identityorg`, `attendance`, `leave`, `request`, `notification`, `reporting`) within one Spring Boot modular monolith in `services/hrflow-app`.
- Persistence boundaries: ADR 002 established schema-level isolation. ADR 006 preserves this rule: six separate PostgreSQL schemas within a single shared database instance, forbidding cross-schema foreign keys and queries.
- Asynchronous messaging: ADR 003 established the Transactional Outbox pattern. ADR 006 refines this pattern by utilizing local database tables (owner-local outbox and handler-local inbox receipts) polled by an in-process worker, deferring Kafka runtime dependencies.
- Business conflicts: ADR 004 established attendance and leave reconciliation rules. ADR 006 adopts these domain invariants without modification.
- Frontend and hosting: ADR 005 considered retaining Nuxt and deploying to Kubernetes. ADR 006 supersedes ADR 005, mandating React with TypeScript for web, Flutter for mobile, and Docker Compose with Cloudflare Tunnel for self-hosting.

## Reading and lifecycle guidelines

When evaluating technical architecture or proposing modifications:

- Implementation guides must conform to active ADRs: An implementation guide or pull request cannot violate an accepted ADR without submitting a new superseding decision record.
- Historical context: Superseded ADRs are preserved to explain why specific alternatives were rejected and under what conditions they may be reconsidered.
- Canonical references: Cross-reference architectural specifications in [Architecture](../Architecture.md) and operational trade-offs in [Decision register](../roadmap/DECISION-REGISTER.md).

## Quality refinement

[ADR 007](007-QUALITY-AND-TENANT-DEFENSE.md) records adopted RLS defense and local event ownership; [V2 review](../roadmap/ARCHITECTURE-REVIEW-V2.md) states every verdict. Original ADR statuses remain Proposed where no product approval exists.
