# Documentation source-of-truth map

> **Decision:** ADR 006, refined by ADR 007, is the authoritative architectural baseline. Historical references in `docs/reference/` describe earlier seven-service Kafka proposals and are superseded where conflicts exist.

## Information classification

Each technical topic in HRFlow has a single canonical owner. Dependent implementation guides must link to the canonical owner rather than duplicate or alter the specification.

| Information category | Authoritative document | Scope and responsibility |
| --- | --- | --- |
| Canonical specification | [Architecture](Architecture.md) | Logical domain boundaries, modular monolith design, security model, and ingress. |
| Canonical contracts | [API and persistence](REST-API-DATABASE-DESIGN.md) | Wire conventions, schema ownership, outbox tables, and status codes. |
| Decision record | [Decision register](roadmap/DECISION-REGISTER.md) | Complete list of architectural decisions, accepted choices, and rejected alternatives. |
| Decision detail | [ADR index](adr/README.md) | Architecture Decision Records 001 through 006 with rationale and consequences. |
| Implementation guide | [Roadmap index](roadmap/README.md) | Twelve numbered weekly execution guides from walking skeleton to release. |
| Reference material | [Reference index](reference/README.md) | Historical schemas, domain contracts, and archived seven-service analysis. |
| Technical primers | [Primers index](roadmap/primers/README.md) | Foundational technical concepts, design patterns, and debugging guides. |
| Operational runbooks | [Operations guide](roadmap/10-OPERATIONS-AND-RESTORE/README.md) | Host setup, deployment, backup procedures, and disaster recovery. |
| Progress tracking | [Progress dashboard](roadmap/YOU-ARE-HERE.md) | Active step state, verified evidence, blockers, and weekly progress. |

## Reconciliation of conflicting requirements

When documentation files provide conflicting statements, the hierarchy of authority determines the correct interpretation.

| Topic | Historical proposal | Authoritative baseline | Reconciled decision |
| --- | --- | --- | --- |
| System topology | Seven Spring Boot microservices | One Spring Boot modular monolith | Modular monolith with domain packages under `com.hrflow.app`. |
| Message transport | Apache Kafka with Schema Registry | Local transactional outbox in PostgreSQL | In-database outbox and inbox receipts; Kafka runtime deferred. |
| Database architecture | Seven separate databases | One PostgreSQL database with domain schemas | Six schemas: `identity_org`, `attendance`, `leave`, `request`, `notification`, `reporting`. |
| Web frontend | Nuxt and Vue starter scaffold | React with TypeScript | Scaffold preserved in Git history; new development targets React. |
| Mobile client | Standalone Flutter production app | Flutter client starting with mock | Minimal Flutter shell mocked against OpenAPI in Week 01. |
| Ingress and TLS | Public port forwarding and Let's Encrypt | Cloudflare Tunnel to private Caddy | Origin port stays private; Cloudflare terminates public TLS. |
| Object storage | AWS S3 or MinIO bucket | Local disk storage with authorized access | Private disk directory with authorized controller streaming. |
| Host infrastructure | Kubernetes or AWS EKS | Single self-hosted machine with Compose | Docker Compose on existing hardware with local Uptime Kuma. |

## Central open questions register

The following decisions require explicit stakeholder input before the related implementation steps can be marked complete.

| ID | Topic | Current working assumption | Required stakeholder action |
| --- | --- | --- | --- |
| O01 | Host machine hardware | Existing macOS machine with Docker Desktop | Verify continuous uptime, awake state, and 30 GiB free disk. |
| O02 | External backup disk | External USB drive with manual rotation | Procure dedicated external storage and test encryption key. |
| O03 | Network ingress | Cloudflare Tunnel without open router ports | Confirm domain ownership and Cloudflare DNS delegation. |
| O04 | Contributor hours | 24 hours per week per contributor (70% planned) | Confirm actual weekly calendar availability for Contributors A and B. |
| O05 | Public domain name | Custom domain allocated for HRFlow | Register domain and configure nameservers. |
| O06 | Attendance conflict policy | Manual approved correction supersedes inference | HR stakeholder reviews shift overlap and holiday dispute rules. |
| O07 | Leave accrual rounding | Half-day minimum increment with pending lock | HR stakeholder approves balance rollover and accrual limits. |
| O08 | Mobile notification delivery | In-app notification inbox priority | Determine whether Apple APNs and Google FCM keys are available. |

## Document navigation rules

To keep the documentation clean and maintainable across all platforms, cross-references must follow these rules:

- Link to the canonical specification for architectural rules rather than re-explaining them.
- Link to [Decisions](roadmap/DECISION-REGISTER.md) for rationale rather than duplicating trade-off debates.
- In implementation guides, link to the relevant [Primers](roadmap/primers/README.md) for first-principles explanations.
- Operational steps must reference [Troubleshooting](roadmap/01-FOUNDATION/TROUBLESHOOTING.md) when diagnostic steps are shared.
- Never cite unverified test execution as **PASS**; use **NOT RUN** until verified evidence is recorded.
