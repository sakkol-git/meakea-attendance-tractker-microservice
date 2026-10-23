# HRFlow twelve-week execution plan

> **Decision:** ADR 008 restores the final seven-service/JDBC/Kafka/Nuxt architecture; cloud execution is future improvement. The target duration is twelve weeks (84 calendar days).

## Execution philosophy and delivery cadence

The execution plan prioritizes visible mini-wins, verifiable evidence, and strict capacity boundaries. Development follows a predictable weekly cadence designed to prevent burnout and ensure steady progress:

- Monday through Wednesday: Active feature development across Stones 1 through 6. Each day advances the next small, bounded steps within its time cap (30 to 90 minutes each), introducing at most two new concepts per day.
- Thursday: Review, buffer, and failure recovery in Stone 7. Zero new feature scope is introduced; time is dedicated to test hardening, diagnostic reviews, and refactoring admitted patterns.
- Friday: Weekly gate execution, host redeployment, and disaster recovery testing in Stone 8. Critical gates must be verified on confirmed host infrastructure.
- Saturday and Sunday: Mandatory rest days. Crunch time and weekend work are prohibited; unfinished non-critical tasks are simplified or carried over.

## Capacity model and contributor allocation

Capacity planning accounts for real-world constraints, learning curves, and unexpected blockers:

| Metric | Contributor A (Backend/Infra/Web) | Contributor B (Mobile/QA/Client) | Combined team capacity |
| --- | --- | --- | --- |
| Stated weekly commitment | 24.0 hours | 24.0 hours | 48.0 hours |
| Planned allocation (70%) | 16.8 hours | 16.8 hours | 33.6 hours |
| Light weeks allocation (Weeks 4 and 8) | 14.28 hours | 14.28 hours | 28.56 hours |
| Total planned twelve-week capacity | 196.56 hours | 196.56 hours | 393.12 hours |
| Final V2 atomic plan incl review | 138.75 hours | 162.5 hours | 301.25 hours |
| Remaining under 70-percent ceiling | 57.81 hours | 34.06 hours | 91.87 hours |

## Twelve-week milestone roadmap

The implementation is partitioned into twelve sequential milestone weeks, each producing an independently testable increment:

| Week | Phase name | Key deliverables | Primary acceptance gate |
| --- | --- | --- | --- |
| 01 | [Foundation](roadmap/01-FOUNDATION/README.md) | Private identity/reactive gateway skeleton, PostgreSQL and Kafka baseline, mock Flutter shell. | Web-to-Caddy-to-app-to-DB ping **PASS**. |
| 02 | [Identity and host](roadmap/02-IDENTITY-AND-HOST/README.md) | JWT access, rotating refresh tokens, private host deployment, initial dump. | Two-tenant auth and local deploy **PASS**. |
| 03 | [Organization and admin](roadmap/03-ORGANIZATION-AND-ADMIN/README.md) | Tenant onboarding, org units, positions, employee hierarchy, RLS and architecture tests. | Org tree and employee assignment **PASS**. |
| 04 | [Attendance core](roadmap/04-ATTENDANCE-CORE/README.md) | Append-only evidence, shift windows, auto-checkout, manual corrections. | Attendance invariant test matrix **PASS**. |
| 05 | [Outbox and notifications](roadmap/05-OUTBOX-AND-NOTIFICATIONS/README.md) | Transactional outbox, inbox receipts, in-app notification inbox. | At-least-once outbox dispatch **PASS**. |
| 06 | [Leave workflow](roadmap/06-LEAVE-WORKFLOW/README.md) | Entitlement balances, pessimistic reservation locks, manager approvals. | Concurrency overspend test **PASS**. |
| 07 | [Requests and approvals](roadmap/07-REQUESTS-AND-APPROVALS/README.md) | Material requests, multi-level approvals, private attachment downloads. | Attachment auth and download **PASS**. |
| 08 | [Web and mobile integration](roadmap/08-WEB-AND-MOBILE-INTEGRATION/README.md) | Nuxt frontend dashboard, live Flutter integration, offline command queue. | End-to-end client user flows **PASS**. |
| 09 | [Reporting and audit](roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md) | Tamper-evident audit logs, attendance summaries, CSV exports, Uptime Kuma. | Streaming CSV and health checks **PASS**. |
| 10 | [Operations and restore](roadmap/10-OPERATIONS-AND-RESTORE/README.md) | Automated pg_dump cron, off-site disk replication, clean-machine restore. | Full bare-metal disaster recovery **PASS**. |
| 11 | [Hardening and pilot](roadmap/11-HARDENING-AND-PILOT/README.md) | OWASP security review, rate limiting, measured load testing (100 bounded clients). | 100 bounded virtual clients **PASS**. |
| 12 | [Release and demonstration](roadmap/12-RELEASE-AND-DEMONSTRATION/README.md) | Code freeze, user acceptance testing, operator training, final handoff. | Stakeholder acceptance sign-off **PASS**. |

## Scope management and tiered features

Features are categorized into strict priority tiers to prevent deadline overruns:

- Tier 1 (Must-Have): Multi-tenant authentication, tenant isolation, organization hierarchy, attendance check-in/out, attendance corrections, leave balances with concurrency locks, material requests, private attachments, transactional outbox, in-app notifications, audit logs, basic CSV exports, Nuxt web UI, Flutter mobile client, Docker Compose deployment, automated backups, and verified scratch restore.
- Tier 2 (Should-Have): Mobile push notifications via APNs/FCM, Server-Sent Events (SSE) for live notification delivery, Wi-Fi network nudges, simple leave rule inference, Khmer language localization, advanced CSV filters.
- Tier 3 (Deferred / Out-of-Scope): Richer model training/validation beyond existing confidence integration, facial recognition biometrics, NFC hardware readers, continuous GPS tracking, Kubernetes manifests, Terraform cloud automation, distributed tracing (Zipkin/Jaeger), payroll processing, recruiting pipelines.

## Failure remediation and stop rules

When tasks encounter technical blockers or exceed time limits, developers must follow standard recovery rules:

- 45-minute rule: If a task makes zero observable progress for 45 minutes, stop coding immediately and consult [Troubleshooting](roadmap/01-FOUNDATION/TROUBLESHOOTING.md) and [Stuck protocol](roadmap/STUCK-PROTOCOL.md).
- 90-minute cut rule: If a task remains blocked after 90 minutes, apply the documented stone cut in the phase README. Never compromise tenant isolation or backup verification.
- Critical gate failures: If a Friday gate fails, record **FAIL** with safe reproduction output. The failed check carries over into Monday of the next week as Stone 1, cutting optional stretch scope.
- Git checkpointing: Every 30–90 minute step concludes with an atomic Git commit matching the prescribed message format. Stones conclude with a verified green tag (e.g., `w01-s03-green`).

## Final V2 execution authority

[Roadmap](roadmap/ROADMAP.md), [quality coverage](roadmap/QUALITY-COVERAGE.md), [step mapping](roadmap/STEP-ID-MAP.md) and [start](roadmap/START-HERE.md) supersede old five-stone/tag totals. Development starts 2026-10-26 and targets the 2027-01-15 rehearsal. All gates remain **NOT RUN** until executed; no architecture document certifies completion.
