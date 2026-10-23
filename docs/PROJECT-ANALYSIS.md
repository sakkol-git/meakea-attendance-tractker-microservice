# Repository technical analysis and architecture audit

> **Evidence:** This audit analyzes the actual repository codebase, configuration files, and infrastructure manifests on branch `docs/roadmap-playbook`. It documents verified starting conditions and records the initial audit; ADR 008 and V2 now guide final service alignment.

## Repository baseline inventory

An inspection of the repository root, source directories, and configuration files reveals the baseline application state:

| Component path | Technology and frameworks | Observed implementation status | Target resolution |
| --- | --- | --- | --- |
| `services/*` (7 services) | Spring Boot 4.0.8, Java 21 | Minimal startup boilerplate; zero domain controllers, entities, or business services. | Retain the seven service roots and align JDBC/reactive gateway/build versions under ADR 008. |
| `libs/hrflow-contracts` | Maven, Apache Avro | Avro schema files for attendance check-in; no compiled Java classes. | Defer Avro/Kafka runtime; use JSON outbox payloads in PostgreSQL. |
| `libs/hrflow-event-starter` | Spring Boot autoconfig, Kafka | Kafka producer/consumer template wrappers; no active integration tests. | Superseded by local Spring ApplicationEvent or direct outbox dispatcher. |
| `web/` | Nuxt 3, Vue 3, TypeScript | Default web starter template with basic routing; zero HR business views. | Retain in Git history; develop production web dashboard in React. |
| `ml/confidence-service` | Python 3.12, scikit-learn, FastAPI | Standalone attendance confidence scoring script; passing unit tests. | Deferred from MVP; core attendance rules rely on shift policies and HR review. |
| `infra/docker/` | Docker Compose, PostgreSQL, Kafka | Compose file defining Postgres, Zookeeper, Kafka, Schema Registry, Redis. | Replace with lightweight Compose: PostgreSQL 16, Caddy, Uptime Kuma. |

## Technical inconsistencies and architectural debt

The audit identified several technical discrepancies that must be reconciled during early roadmap phases:

- Build inheritance conflicts: The root `pom.xml` declares Spring Boot parent 4.1.1, while individual service pom files reference 4.0.8. Gateway declares Spring Cloud 2025.1.3. These must be aligned under a unified parent in `services/hrflow-app`.
- Missing database migrations: No Flyway or Liquibase migration scripts exist in any service. The initial schema exists only as proposed DDL in historical documentation.
- Missing business tests: Running `mvn test` in the reactor fails due to missing cached Avro dependencies and nonexistent test classes. There are zero unit or integration tests for attendance, leave, or multi-tenant security.
- Docker Compose mount defects: The existing `docker-compose.yaml` mounts a nonexistent `postgres-init.sql` script and includes invalid listener configuration for Schema Registry.
- Resource headroom limitations: Host system inspection indicates approximately 5 GiB of available disk space and 16 GiB of RAM. Running seven JVM services alongside Kafka and Schema Registry would exhaust memory and cause container crashes.

## Verified command execution record

The following verification commands were executed directly against the workspace to establish ground truth:

```bash
# Maven reactor validation (offline)
mvn -o validate
# Observed result: Ten reactor projects passed syntax validation; no Java compilation performed.
```

```bash
# Nuxt web frontend build check
npm --prefix web run build
# Observed result: Build succeeded for default Nuxt template; no business views present.
```

```bash
# Python machine learning prototype tests
python3 -m pytest -q ml/confidence-service
# Observed result: 5 passed, 1 warning; prototype only, not integrated.
```

```bash
# Compose configuration syntax check
docker compose -f infra/docker/docker-compose.yaml config --quiet
# Observed result: Configuration syntax valid; does not test runtime startup.
```

## Migration path to modular monolith

The findings confirm that the seven-service architecture represents planned scaffolding rather than running production software. Consequently, consolidating into a modular monolith (ADR 006) carries zero regression risk for business features:

1. Create `services/hrflow-app` with domain package boundaries: `identityorg`, `attendance`, `leave`, `request`, `notification`, `reporting`.
1. Establish Flyway migrations for six discrete database schemas in PostgreSQL 16.
1. Implement the Transactional Outbox pattern in `notification.outbox_events` to replace external message brokers.
1. Deploy a private Caddy gateway to handle internal routing and integrate Cloudflare Tunnel for secure remote access.
1. Preserve existing service directories in Git history to maintain provenance without impeding modular development.
