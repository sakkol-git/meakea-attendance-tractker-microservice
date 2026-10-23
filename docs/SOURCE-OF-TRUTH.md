# Documentation source-of-truth map

## Current authority

The latest approved [Architecture](Architecture.md), including its four linked final-design parts, is authoritative. [ADR 008](adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md) supersedes the monolith/React choices in ADR 006 and records the subsequent cloud deferral. Historical ADRs and tags are retained. Source is evidence of implementation, not automatic authority over a newer design.

| Topic | Canonical owner | Current decision |
| --- | --- | --- |
| Service topology, boundaries and cloud future | [Architecture](Architecture.md) | Seven services, Kafka/Avro, JDBC, reactive gateway; cloud deferred |
| API/data conventions | [Contracts](REST-API-DATABASE-DESIGN.md) | One OpenAPI file, service-owned PostgreSQL schemas and scoped JDBC |
| Choices and supersession | [Decision register](roadmap/DECISION-REGISTER.md) | ADR 008 active; preserve older rationale |
| Formal decisions | [ADR index](adr/README.md) | Status and scope of each ADR |
| Weekly execution | [Roadmap](roadmap/README.md) | Final V2 steps/gates; all application execution remains Not run |
| Status/capacity | [Dashboard](roadmap/YOU-ARE-HERE.md) | Not started; 24 h each; 70 percent planned |
| Quality | [Standards](roadmap/ENGINEERING-STANDARDS.md) | Q01–Q14, evidence required |
| Resume V2 authoring | [Progress](roadmap/PROGRESS-V2.md) | Exact remaining changes and blockers |

## Confirmed and unknown

Nuxt is retained, not migrated to React. Existing service roots stay; services/hrflow-app is superseded. Kafka and Registry remain required locally. AWS/Terraform/Kubernetes stay in the final production design, but the user's latest reply defers implementation beyond this finish line. No paid service is assumed free.

The user confirmed unchanged W01-S01/not started, A=24 and B=24 hours weekly, and host not ready. Start date, production computer, external backup, public domain and network reachability remain unknown. Cloud region/credits/budget are deferred with cloud execution. HR policy sign-off and push-provider credentials remain open. Do not infer employee policy approval from architecture approval.

No feature is done because its document exists. Current build blockers, safe commands and missing source are recorded in the repository audit and V2 architecture review. All weekly guides now follow ADR 008; historical C records remain context only.
