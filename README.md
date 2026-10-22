# HRFlow

An event-driven, multi-tenant HR platform under development.
The repository currently contains seven Spring Boot service scaffolds, shared-library scaffolds, a Nuxt/Vue starter, local PostgreSQL/Kafka configuration, and a standalone Python confidence prototype. Core HR workflows are planned, not implemented.

## Documentation

- [Architecture and implementation status](docs/Architecture.md).
- [REST APIs, persistence, and event contracts](docs/REST-API-DATABASE-DESIGN.md).
- [Twelve-week execution plan](docs/PLAN.md).
- [Architecture decision records](docs/Architecture.md).
- [Documentation validation and limits](docs/validation/README.md).
- [Historical project analysis](docs/PROJECT-ANALYSIS.md).

## Toolchain and startup status

Java target is 21; build uses Maven. The root currently declares Spring Boot parent 4.1.1, while individual services declare 4.0.8; gateway declares Spring Cloud 2025.1.3. Align inheritance/toolchain before treating the reactor as a verified application build.
Web uses Nuxt/Vue; React remains a previous plan pending the explicit frontend decision. Flutter source is not present here.

Local dependencies are defined in `infra/docker/docker-compose.yaml`. The documented PostgreSQL initialization mount and Schema Registry listener defects must be corrected before a fresh local startup is considered reproducible. Compose does not launch applications.

See the [architecture discrepancy register](docs/Architecture.md) for precise repair tasks. Documentation DDL/API examples are proposed contracts, not installed migrations or running endpoints.
