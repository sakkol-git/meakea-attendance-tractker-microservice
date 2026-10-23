# HRFlow engineering documentation handbook

> **Navigation:** [Root README](../README.md) · [Source of truth](SOURCE-OF-TRUTH.md) · [Start here](roadmap/START-HERE.md) · [Dashboard](roadmap/YOU-ARE-HERE.md)

## Introduction and system identity

HRFlow is an event-driven, multi-tenant human resources management platform designed specifically for small-to-medium enterprises and organizations operating under strict data sovereignty, privacy, and budget constraints.

The system provides end-to-end workforce lifecycle automation: company organization structures, employee records, tamper-resistant shift attendance, concurrent leave reservation and approval, general material requests, in-app notifications, and comprehensive audit reporting.

## Core problems solved

Traditional HR software presents serious trade-offs for resource-constrained organizations. Commercial SaaS solutions impose recurring per-user fees, store sensitive personal employee data on third-party cloud infrastructure, and require high-bandwidth continuous internet connectivity. Conversely, manual spreadsheets and legacy custom scripts create data duplication, untracked attendance disputes, race conditions in leave balances, and zero auditability.

HRFlow resolves these problems through three architectural guarantees:

- Absolute tenant isolation: Multi-tenant data residency enforced by verified JWT claims, mandatory tenant foreign keys, composite unique constraints, and strict database schema separation.
- Resilient self-hosting: Packaged as a lightweight Docker Compose deployment suitable for existing on-premises hardware, requiring zero paid cloud databases or Kubernetes clusters.
- Atomic domain consistency: Concurrency-safe transactions, deterministic pessimistic row locking for leave balances, append-only attendance evidence, and an in-database Transactional Outbox.

## User roles and system actors

HRFlow serves four primary human actors and one background automated system:

| Role | Responsibilities | Key capabilities |
| --- | --- | --- |
| Employee | Daily workforce member | Check in and out, view shift status, submit leave requests, request workplace equipment. |
| Manager | Team leader and direct supervisor | Review and approve attendance corrections, approve or reject leave requests, inspect team calendars. |
| HR Administrator | Company policy and compliance manager | Configure organization units, job titles, shift policies, public holidays, and export compliance CSVs. |
| Platform Operator | System administrator | Manage Docker Compose, execute nightly backups, verify scratch restores, manage Cloudflare tunnels. |
| System Worker | Automated background processor | Poll transactional outbox, dispatch event receipts, compute attendance summaries, deliver notifications. |

## Minimum Viable Product scope

The initial production release delivers a complete, cohesive walking skeleton across six essential functional domains:

- Identity and organization: Multi-tenant tenant onboarding, user authentication via short-lived JWTs and rotating refresh tokens, departments, and positions.
- Attendance core: Tamper-resistant check-in and check-out, shift scheduling, auto-checkout grace periods, dispute corrections, and audit history.
- Leave workflow: Entitlement balance tracking, concurrent reservation locking, manager approvals, medical certificate attachments, and cancellation compensations.
- Material and general requests: Multi-step approval workflows, advisory inventory checks, and private attachment downloads with short-lived tokens.
- Outbox and notifications: Database-backed Transactional Outbox, at-least-once local event dispatch, consumer idempotency receipts, and in-app user notifications.
- Audit and CSV reporting: Tamper-evident audit logging of all mutations, attendance roll-ups, leave utilization matrices, and streaming CSV exports.
- Client interfaces: Nuxt/Vue web client with TypeScript and a lightweight Flutter mobile client for daily employee attendance and request tracking.
- Operational infrastructure: Single-host Docker Compose deployment, PostgreSQL 16 domain schemas, Caddy reverse proxy, Cloudflare Tunnel ingress, and verified nightly backup scripts.

## Authoritative specifications

To avoid conflicting requirements, documentation follows the hierarchy established in the [Source of truth map](SOURCE-OF-TRUTH.md):

- [Architecture](Architecture.md): Authoritative definition of the final service runtime, owner boundaries, database schema boundaries, and security model.
- [API and persistence mapping](REST-API-DATABASE-DESIGN.md): Authoritative contract rules, REST route patterns, idempotency keys, outbox schema, and status code standards.
- [Decision register](roadmap/DECISION-REGISTER.md): Authoritative record of architectural choices, scope boundaries, and accepted operational trade-offs.
- [Architecture decision records](adr/README.md): ADR 001 through 006 providing formal context, options considered, and superseding rationale.
- [Reference index](reference/README.md): Historical reference material detailing earlier seven-service microservice and Kafka contracts, retained for historical background.

## Getting started as a new developer

New contributors must follow this reading and setup sequence to understand the project and prepare a working development environment:

1. Read this documentation handbook in full to understand the project goals, user roles, and system boundaries.
1. Inspect the [Source of truth map](SOURCE-OF-TRUTH.md) to understand which documents are normative.
1. Review [Start here](roadmap/START-HERE.md) for the daily 5-minute implementation routine and operational boundaries.
1. Inspect [You are here](roadmap/YOU-ARE-HERE.md) to determine the current implementation step, last verified milestone, and active blockers.
1. Review the [Project baseline](roadmap/00-PREPARATION/PROJECT-BASELINE.md) and [Repository audit](roadmap/00-PREPARATION/REPOSITORY-AUDIT.md) to understand the starting codebase state.

## Implementation roadmap structure

The engineering journey is organized into twelve sequential weekly phases, taking the project from initial repository preparation to production release:

| Week | Phase | Primary deliverable | Gate criteria |
| --- | --- | --- | --- |
| 00 | [Preparation](roadmap/00-PREPARATION/README.md) | Baseline audit, hardware assessment, and migration strategy | Disk and toolchain gates verified |
| 01 | [Foundation](roadmap/01-FOUNDATION/README.md) | Walking skeleton, single-service boot, Docker Compose, and mock Flutter | Private web-to-app-to-DB ping **NOT RUN** |
| 02 | [Identity and host](roadmap/02-IDENTITY-AND-HOST/README.md) | Multi-tenant auth, JWT refresh rotation, private host deployment | Two-tenant auth and deploy **NOT RUN** |
| 03 | [Organization and admin](roadmap/03-ORGANIZATION-AND-ADMIN/README.md) | Org units, positions, employee profiles, and builder pattern lesson | Employee hierarchy tests **NOT RUN** |
| 04 | [Attendance core](roadmap/04-ATTENDANCE-CORE/README.md) | Append-only evidence, shift windows, auto-checkout, corrections | Attendance invariant tests **NOT RUN** |
| 05 | [Outbox and notifications](roadmap/05-OUTBOX-AND-NOTIFICATIONS/README.md) | Transactional outbox, inbox deduplication receipts, in-app notifications | At-least-once outbox test **NOT RUN** |
| 06 | [Leave workflow](roadmap/06-LEAVE-WORKFLOW/README.md) | Entitlement balances, pessimistic reservation locks, manager approvals | Concurrency overspend test **NOT RUN** |
| 07 | [Requests and approvals](roadmap/07-REQUESTS-AND-APPROVALS/README.md) | Material requests, private signed attachments, specification pattern | Attachment auth security **NOT RUN** |
| 08 | [Web and mobile integration](roadmap/08-WEB-AND-MOBILE-INTEGRATION/README.md) | Nuxt frontend forms, live Flutter integration, offline command queue | End-to-end user journeys **NOT RUN** |
| 09 | [Reporting and audit](roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md) | Tamper-evident audit logs, CSV exports, Prometheus metrics, Uptime Kuma | Streaming export and health **NOT RUN** |
| 10 | [Operations and restore](roadmap/10-OPERATIONS-AND-RESTORE/README.md) | Automated pg_dump, off-site disk replication, scratch disaster recovery | Clean-host disaster restore **NOT RUN** |
| 11 | [Hardening and pilot](roadmap/11-HARDENING-AND-PILOT/README.md) | Security vulnerability scan, rate limiting, measured load testing | 50 concurrent virtual users **NOT RUN** |
| 12 | [Release and demonstration](roadmap/12-RELEASE-AND-DEMONSTRATION/README.md) | Code freeze, user acceptance testing, operator training, release handoff | Stakeholder demonstration **NOT RUN** |

## Build and test execution

The development environment requires Java 21, Maven 3.9+, Docker Desktop or Docker Engine, Node.js 20+, and Flutter 3.19+:

```bash
# Verify build environment prerequisites
java -version
docker compose -f infra/docker/docker-compose.yaml config --quiet
mvn -o validate
```

```bash
# Execute documentation suite linting
python3 docs/roadmap/tools/docs-lint.py docs
```

```bash
# Run unit and slice tests
mvn test
```

## Deployment, backup, and disaster recovery

HRFlow runs as a single self-contained application stack on a dedicated host machine:

- Runbook instructions: Detailed operational commands are documented in [Host runbook](roadmap/10-OPERATIONS-AND-RESTORE/HOST-RUNBOOK.md) and [Access setup](roadmap/02-IDENTITY-AND-HOST/ACCESS-SETUP.md).
- Automated backup: A scheduled nightly cron job executes `pg_dump` with custom compressed format, retaining 14 days of encrypted rolling snapshots.
- Off-site replication: Backup archives and uploaded private attachments are mirrored to an external physical storage disk disconnected from network exposure.
- Disaster recovery: A weekly scratch restore script recreates the database on an isolated temporary cluster, validating database integrity constraints and data recoverability.

## Risks, open decisions, and project tracking

Project health, risks, and progress are continuously tracked across central dashboards:

- [Risk register](roadmap/RISK-REGISTER.md): Active inventory of technical, operational, and schedule risks with documented mitigations.
- [Decision register](roadmap/DECISION-REGISTER.md): Record of all accepted architectural decisions and scope cut criteria.
- [Global checklist](roadmap/GLOBAL-CHECKLIST.md): Complete requirements coverage map connecting functional goals to verified tests.
- [Roadmap progress](roadmap/PROGRESS.md): Historical ledger of documentation phases, tool validations, and dry-run corrections.
- [Open questions](SOURCE-OF-TRUTH.md): Central register of outstanding decisions (O01 through O08) requiring stakeholder alignment.

## Final execution entry

[Start here](roadmap/START-HERE.md) is the final V2 guide; [current quality coverage](roadmap/QUALITY-COVERAGE.md) and [actual validation](roadmap/V2-VALIDATION-LOG.md) separate planned gates from executed work. ADR 008 supersedes C/React; AWS/Kubernetes execution is deferred.
