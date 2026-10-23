# HRFlow — Multi-Tenant HR Engineering Platform

> **Note:** HRFlow is an event-driven, multi-tenant HR management platform under development. The documentation provides a reviewed twelve-week implementation roadmap for building the system with the final seven-service Java 21/JDBC/Kafka architecture.

## Overview

HRFlow delivers comprehensive workforce management for small and medium enterprises, encompassing employee organization hierarchies, tamper-resistant attendance tracking, concurrent leave balance management, material requests, and audit-ready reporting.

The platform is designed for reliable self-hosting on private infrastructure, combining Docker Compose, PostgreSQL 16 domain schemas, local transactional event outbox processing, Caddy gateway routing, and secure Cloudflare Tunnel ingress without requiring cloud subscriptions or public open router ports.

## Documentation navigation

The engineering documentation suite is organized into authoritative specifications, decision records, implementation roadmaps, and technical primers:

- [Documentation landing guide](docs/README.md): Ten-minute onboarding guide answering core architectural, operational, and project questions.
- [Source of truth map](docs/SOURCE-OF-TRUTH.md): Canonical document ownership, conflict resolution rules, and open questions.
- [System architecture](docs/Architecture.md): Authoritative final service architecture, schema isolation, security, and deployment topology.
- [Execution plan](docs/PLAN.md): Structured twelve-week roadmap, capacity allocation, milestone gates, and delivery commitments.
- [API and database design](docs/REST-API-DATABASE-DESIGN.md): REST conventions, PostgreSQL domain schemas, outbox tables, and status codes.
- [Architecture decision records](docs/adr/README.md): ADR 001 through 008 documenting decisions and supersession.
- [Start here](docs/roadmap/START-HERE.md): Daily five-minute onboarding routine for active implementation.
- [Current progress dashboard](docs/roadmap/YOU-ARE-HERE.md): Real-time step status, verified test evidence, and active blockers.
- [Decision register](docs/roadmap/DECISION-REGISTER.md): Fixed architectural choices, scope tiers, and accepted operational decisions.
- [Technical primers](docs/roadmap/primers/README.md): Conceptual deep-dives into multi-tenancy, outbox events, database locking, and design patterns.
- [Documentation validation](docs/validation/README.md): Linter commands, syntax checks, and evidence-based verification records.

## Technical foundation and repository state

The repository currently represents the pre-implementation phase. Existing directories contain prototype scaffolding that is being aligned with the final service design in ADR 008:

- Backend target: Java 21 Spring Boot services in their existing service roots, with owner schemas (`identity_org`, `attendance`, `leave`, `request`, `notification`, `reporting`).
- Persistence target: Single PostgreSQL 16 instance partitioned into six strict domain schemas without cross-schema foreign keys.
- Web frontend target: Nuxt/Vue with TypeScript, retaining the existing starter and lockfile.
- Mobile client target: Flutter client targeting iOS and Android, beginning with an OpenAPI-driven mock shell in Week 01.
- Ingress and networking: Cloudflare Tunnel terminating public TLS and forwarding traffic to a local Caddy reverse proxy on a private port.
- Event architecture: Transactional Outbox pattern storing events in PostgreSQL `outbox_events` and tracking consumer progress via `inbox_receipts`.
