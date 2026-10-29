# Maven dependency baseline

## Purpose

This baseline keeps the seven Java services consistent without putting every library in every service. The root POM owns versions and build rules. `services/owner-service-parent/pom.xml` owns the dependencies shared by the six database-owning servlet services. The reactive gateway declares its smaller stack directly. A service POM should contain only dependencies that serve that service's accepted responsibility.

Run the fast policy check after changing a service POM:

```bash
python3 scripts/check-maven-dependencies.py
```

The same check is available as the local MCP `run_project_check` name `dependency_policy` and is part of the backend verification scope.

## Shared owner-service baseline

The six owner services inherit:

- Actuator and the Prometheus registry for the accepted metrics target. Metrics remain private until management exposure and network controls are implemented and verified.
- Spring Security and the Boot 4 resource-server starter. These supply JWT machinery; they do not define claims, tenant provenance, role mapping or resource authorization.
- Spring Data JPA, Flyway PostgreSQL support and the PostgreSQL driver, following accepted ADR 009. Flyway owns schema changes and Hibernate validates them.
- Spring Kafka and the Confluent Avro serializer for the accepted event platform. Their presence does not prove outbox, inbox, retries or delivery semantics.
- Bean Validation and Spring MVC for owner HTTP adapters.
- Lombok (provided scope) and MapStruct 1.6.3, with shared compiler annotation processors for Lombok, `lombok-mapstruct-binding` 0.2.0 and MapStruct. The binding stays on the processor path. Avoid Lombok `@Data` on JPA entities.
- Focused Boot test starters plus Testcontainers modules for PostgreSQL and Kafka integration tests. Real container-backed tests must be added with each implemented persistence or messaging slice.
- ArchUnit core for small JUnit tests that protect proven dependency boundaries. Add rules only when packages and responsibilities exist; avoid speculative layer rules.

The owner parent also applies the executable-jar plugin. Failsafe runs integration tests named by Maven's standard conventions during `verify`. Enforcer requires Java 21 or newer and Maven 3.9.x, while the compiler target and CI runtime remain Java 21; it also rejects duplicate dependency declarations.

Owner-service Surefire runs use Testcontainers' `httpclient5` transport, matching the existing integration-test transport. This keeps renamed `*Test` PostgreSQL/Kafka suites executable through the ordinary verifier and local test tools without a manually supplied shell variable.

## Service-specific dependencies

| Service | Extra dependency | Accepted need |
| --- | --- | --- |
| reporting | Apache Commons CSV 1.14.1 | CSV is the accepted MVP export format |
| api-gateway | Reactive Gateway/WebFlux stack | Routing and edge security; no JPA, Flyway or database driver |

Java records, JPA Specifications, transactions, locking and RFC 9457 `ProblemDetail` come from Java or the existing Spring stack and need no extra dependency.

## Add when implementation reaches the requirement

Add dependencies in the smallest owning module with the first behavior and tests that use them:

- Add `spring-boot-starter-restclient` to attendance only when the Stretch confidence-service integration is admitted and its timeout/failure behavior is accepted.
- Add mail or push-provider clients to notification with the approved delivery channel and retry/privacy requirements.
- Add an XLSX library only if XLSX moves into accepted scope; CSV is the MVP.
- Add springdoc only if runtime-generated API exploration is needed. `contracts/openapi.json` remains the canonical shared HTTP contract.
- Add tracing export only with an operated collector and an explicit data/cardinality policy.

Redis, service discovery, a configuration server, a workflow engine, Elasticsearch and another message broker have no current requirement. Do not add them as a generic enterprise checklist.

## Change rules

Use the Boot and Cloud BOMs for managed versions. Put an unmanaged version in the root property and dependency-management sections, then declare the dependency only in the owner that uses it. Prefer an additive upgrade and run `./mvnw --batch-mode verify`; dependency presence does not replace behavior, security, migration or provider tests.

UAT recovery implementation (2026-10-29): owner services also inherit the existing hrflow-event-starter module. Its implemented recovery adapter uses the already selected Spring Kafka and Confluent libraries, plus Boot health and Micrometer APIs already present in owners. No additional broker, runtime service or paid platform is introduced. HRFLOW_EVENTS_RECOVERY_ENABLED explicitly enables bounded retry/dead-letter handling and broker-backed lag measurement; disabled configurations retain the prior non-skipping handlers. The library no longer supplies a competing application.yaml with localhost defaults. W05 evidence records the regression and real broker scope.
