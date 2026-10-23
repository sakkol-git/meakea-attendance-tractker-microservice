# Technical primers and pattern lessons catalog

> **Navigation:** [Root documentation](../../README.md) · [Start here](../START-HERE.md) · [Patterns learning path](../PATTERNS-LEARNING-PATH.md) · [Dashboard](../YOU-ARE-HERE.md)

## Just-in-time learning methodology

The technical primers provide focused, first-principles learning documents for the architectural, security, and design concepts used throughout HRFlow.

Each primer is designed to be read in 10 to 20 minutes immediately before starting the relevant roadmap implementation step:

- Read just in time: Study a primer only when linked from your current build guide step; do not attempt to read the entire catalog in one sitting.
- Pacing rule: Introduce at most two new technical concepts on any single build day. Thursday buffer days are reserved for revising familiar concepts without introducing new patterns.
- Active verification: Reading is never a substitute for running the step test. After studying a primer, return immediately to the build guide and implement the verified code increment.

## Architecture, persistence, and data isolation

Core foundational primers governing data boundaries, multi-tenancy, and relational database integrity:

- [Multi-tenancy and data isolation](MULTI-TENANCY.md): Discriminator columns, composite foreign keys, and token-derived tenant boundaries.
- [Database locks and invariants](DATABASE-LOCKS.md): Pessimistic row locking (`SELECT FOR UPDATE`), concurrency control, and deadlocks.
- [Flyway schema migrations](FLYWAY.md): Numbered immutable SQL scripts, checksum validation, and multi-schema evolution.
- [Repository pattern](REPOSITORY.md): Mediating between domain logic and persistence, custom interfaces, and query encapsulation.
- [Dependency injection](DEPENDENCY-INJECTION.md): Inversion of control, constructor injection, loose coupling, and test mocking.

## Messaging and asynchronous processing

Event delivery patterns, outbox persistence, and broker fundamentals:

- [Transactional outbox pattern](TRANSACTIONAL-OUTBOX.md): Dual-write elimination, atomic outbox persistence, and asynchronous worker polling.
- [API idempotency and duplicate prevention](IDEMPOTENCY.md): Client-supplied idempotency keys, duplicate response caching, and concurrent conflicts.
- [Apache Kafka fundamentals and delivery boundaries](KAFKA-BASICS.md): Topic partitions, consumer offsets, and retained Kafka/Avro and consumer/outbox failure boundaries.
- [Mobile offline command queues](OFFLINE-QUEUE.md): Client-side persistence, background synchronization, and network reconciliation.

## Security, ingress, and authentication

Identity management, token security, and secure network boundaries:

- [JWT and rotating refresh tokens](JWT-REFRESH.md): Short-lived access JWTs, SHA-256 hashed refresh tokens, and reuse breach detection.
- [Reverse proxy and HTTPS ingress](REVERSE-PROXY-HTTPS.md): Caddy gateway configuration, TLS termination, header sanitization, and origin shielding.
- [Outbound secure network tunnels](TUNNELS.md): Cloudflare Tunnel configuration, CGNAT traversal, and zero-inbound-port security.
- [Private file attachment security](ATTACHMENT-SECURITY.md): Storage isolation, authorized streaming controllers, and anti-traversal validation.

## Infrastructure, containerization, and operations

Production hosting, container orchestration, monitoring, and disaster recovery:

- [Docker Compose and orchestration](DOCKER-COMPOSE.md): Declarative multi-container topologies, internal bridge networks, and volume persistence.
- [Database backups and disaster recovery](BACKUPS-RESTORES.md): Automated `pg_dump`, rolling retention, external disk replication, and weekly scratch restore.
- [Integration testing with Testcontainers](TESTCONTAINERS.md): Ephemeral PostgreSQL containers, dynamic JDBC properties, and zero-mock parity.
- [Health checks, metrics, and structured logging](OBSERVABILITY.md): Spring Boot Actuator, Uptime Kuma monitoring, and sanitized MDC JSON logging.

## Design patterns and object modeling

Design pattern lessons for maintainable domain and test code:

- [Builder pattern and test fixtures](BUILDER.md): Fluent assembly, named parameters, and immutable test fixture construction (Week 03).
- [Adapter pattern and boundary isolation](ADAPTER.md): Anti-corruption layers, vendor SDK wrapping, and swappable notification channels.
- [Specification pattern and composable rules](SPECIFICATION.md): Composable boolean business rules, eligibility criteria, and JPA specifications (Week 07).
- [State machines and lifecycle transitions](STATE-MACHINES.md): Finite state machines, transition guards, and compensation workflows.
- [Factory pattern and object instantiation](FACTORY.md): Encapsulated object creation and Spring bean factory maps.
- [Strategy pattern and interchangeable algorithms](STRATEGY.md): Encapsulated calculation algorithms and interchangeable shift rules.
- [Chain of responsibility](CHAIN-OF-RESPONSIBILITY.md): Linear validation pipelines and early short-circuit processing.
- [Decorator pattern](DECORATOR.md): Dynamic cross-cutting behavior, transparent caching, and auditing wrappers.
- [Template method pattern](TEMPLATE-METHOD.md): Invariant algorithm skeletons, report generators, and subclass hooks (Week 09).
- [Contract-first APIs with OpenAPI](OPENAPI.md): Machine-readable contracts, schema modeling, and client code generation.
- [Git workflow and checkpoint safety](GIT.md): Atomic commits, conventional messages, and milestone tagging.

## V2 quality primers

- [Problem details](PROBLEM-DETAILS.md).
- [PostgreSQL row-level security](ROW-LEVEL-SECURITY.md).
- [Architecture tests](ARCHITECTURE-TESTS.md).
- [Scoped code coverage](COVERAGE.md).
- [Build and supply-chain scans](CI-SCANS.md).
- [Small self-review](CODE-REVIEW.md).
- [Formatting and static analysis](STATIC-ANALYSIS.md).
- [Semantic releases](SEMANTIC-RELEASES.md).
- [Contract tests](CONTRACT-TESTS.md).
- [Fail-first behavior tests](TEST-FIRST.md).
- [Graceful shutdown](GRACEFUL-SHUTDOWN.md).
- [Exact balance arithmetic](EXACT-BALANCES.md).

## Final service alignment

- [Spring JDBC](SPRING-JDBC.md).
- [Reactive gateway](REACTIVE-GATEWAY.md).
