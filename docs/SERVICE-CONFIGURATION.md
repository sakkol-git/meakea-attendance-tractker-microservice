# Service configuration baseline

## Scope and entry points

The seven Java service roots now inherit the root Maven/Boot version, use valid YAML and select Spring Data JPA for business services plus the reactive gateway (ADR-009). Each service and the Python confidence prototype has a tracked .env.example and a local, empty .env. Blank files are deliberately ignored, so a fresh clone must create them locally. No application secrets, login flow, tenant policies, business migrations, Kafka handlers or production deployment are supplied by this baseline.

[Architecture](Architecture.md), [ADR 008](adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md) and [engineering standards](roadmap/ENGINEERING-STANDARDS.md) still define the target. The following settings are implementation configuration, not completion of all Q-items. Historical audits retain their older findings; inspect current source before repeating a roadmap repair step.

## Service ports and environment files

| Service root | HTTP default | Management default | Owner schema | Files at service root |
| --- | --- | --- | --- | --- |
| services/api-gateway | 8080 | 9080 | None | .env and .env.example |
| services/identity-org | 8101 | 9101 | identity_org | .env and .env.example |
| services/attendance | 8102 | 9102 | attendance | .env and .env.example |
| services/leave | 8103 | 9103 | leave | .env and .env.example |
| services/request | 8104 | 9104 | request | .env and .env.example |
| services/notification | 8105 | 9105 | notification | .env and .env.example |
| services/reporting | 8106 | 9106 | reporting | .env and .env.example |
| ml/confidence-service | 8107 | Existing /health | Not a database owner | .env and .env.example |

All default host bindings are loopback. Ports avoid the Schema Registry listener on 8081. The default Java readiness group includes the database for business services; liveness does not depend on PostgreSQL/Kafka. Kafka delivery/consumer readiness still needs actual worker implementation and failure tests. A shallow healthy probe does not prove an end-to-end event flow.

## Java environment loading and local startup

Spring imports the adjacent .env as Java properties, with a second service-specific path allowing invocation from the repository root. Run from the service directory or repository root, not an arbitrary working directory. Process environment and command-line arguments override imported properties. Service-specific imports override an optional root .env when both exist; avoid a root credential file that accidentally supplies another service's credentials.

Use plain KEY=value lines with comments on separate lines. Do not add export statements, shell quotes or inline comments; values are not evaluated as shell commands. Java-properties backslash escaping applies. Use exported process variables for values that would be ambiguous in a properties file. No new dotenv dependency is needed for Java.

From the repository root, prepare one service without overwriting an existing nonempty environment file:

```bash
cd services/identity-org
test -s .env || cp .env.example .env
chmod 600 .env
```

Fill DB_PASSWORD and FLYWAY_PASSWORD locally, and set usernames/URL to roles actually provisioned for that owner. The examples contain no password and do not provision users. Runtime roles must be non-owner and non-BYPASSRLS. Migration roles own only their service's schema; do not use a PostgreSQL superuser as an application login. Existing dependency Compose credentials/schema creation are not a complete role/grant/RLS setup and are not changed here.

After Java 21 and the actual database/schema/roles are ready, run from the service directory:

```bash
mvn spring-boot:run
```

An empty .env is a placeholder, not a runnable database configuration. Unresolved required credentials or inaccessible dependencies must fail startup rather than use hard-coded credentials. Keep Flyway's own history in the owner schema. There are no business migration files yet; create/review them through the roadmap before accepting database readiness.

## Java runtime and security defaults

- Application and management listeners bind separately to 127.0.0.1. Health is the only exposed management endpoint; details/components are hidden. Check the management port, not the business port, for Actuator probes.
- Security allows health probes only. Anonymous requests to other paths return 401 and authenticated requests remain denied with 403. Generated development users/passwords, browser login and HTTP Basic are disabled. No gateway forwarding routes exist yet. This is closed scaffolding, not implemented JWT or method/resource authorization.
- CSRF remains enabled; state is not persisted in server sessions. Secure/HttpOnly/SameSite cookie defaults are configured for servlet services. Do not disable CSRF or Secure cookies to get a later login demo working.
- Forwarded headers are not trusted automatically. Add the approved proxy/header sanitization and tenant provenance checks before enabling forwarding or opening business routes. Keep service/management ports private.
- Shutdown allows twenty seconds per phase. Logs use Boot's JSON format. Stack traces, exception names, validation internals and exception messages are excluded from default HTTP errors; framework problem-details support is enabled. Stable business error codes and correlation/tenant context still need implementation and tests.
- Connection pools default to five maximum connections with bounded acquisition, connection and socket waits; connection timezone is UTC. Each pool selects its own schema, which is not tenant isolation by itself. Spring Data JPA (Hibernate) is configured with ddl-auto: validate and open-in-view: false per ADR-009.
- Flyway validation is enabled; clean, schema auto-creation and automatic baselining are disabled. Separate credentials are required for migrations. No applied migration is edited by this change.
- Kafka uses Avro, producer idempotence/all acknowledgments, manual consumer acknowledgment, read-committed isolation, bounded batches and explicit schema/topic registration. The local endpoints use private development transport; broker TLS/SASL and full inbox/outbox guarantees are not implied by these properties.
- The gateway uses the accepted reactive starter, a bounded codec buffer and explicit connection/response timeouts. No automatic retry of mutating HTTP requests is introduced.

For containers, set SERVER_ADDRESS to 0.0.0.0 inside the container namespace and configure an appropriate private management listener. Use loopback/private host bindings, network policy and the target trust model; changing the bind address alone does not secure a deployment. No container deployment is performed by this setup.

## Python confidence service

From ml/confidence-service, install requirements into its virtual environment and launch:

```bash
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python run.py
```

The launcher reads the adjacent .env using python-dotenv without overriding process variables. The example contains the exact consumed CONFIDENCE_HOST/PORT/LOG_LEVEL/WORKERS/MAX_CONCURRENCY/KEEP_ALIVE_SECONDS/SHUTDOWN_SECONDS keys. It defaults to private binding, one worker, bounded concurrency/timeouts, no access logs, no server header and no forwarded-header trust. Invalid ports/resource limits are rejected. Calling uvicorn directly bypasses this launcher configuration.

The Docker command uses the same launcher, binding to 0.0.0.0 inside its namespace; .dockerignore excludes local environments, caches and the virtual environment from build context. The existing model artifact path and scoring rules are unchanged. This prototype has no authentication or tenant isolation; keep it private, use synthetic data and follow the approved integration work before any real HR use.

## Verification and remaining work

Run from the repository root with a compatible JDK selected:

```bash
mvn -o verify
python3 docs/roadmap/tools/docs-lint.py docs README.md
```

The Java tests exercise each service's actual security chain and YAML/property import, including command-line override precedence; they do not connect to PostgreSQL/Kafka. From ml/confidence-service, run:

```bash
.venv/bin/python -m pytest -q -p no:cacheprovider
```

Python tests include the existing scoring checks and launcher defaults, invalid ports and process-over-file precedence. Offline Maven requires the necessary dependencies to be cached. Missing cache/network/container prerequisites are not passing integration tests.

Java 21 is the target; the setup check uses the existing JDK 23 with compiler release 21 because Java 21 is not installed. That does not substitute for the later Java-21 runtime/CI gate. Actual database roles/grants/RLS, migrations, Kafka/Registry integration, JWT/cookie flows, correlation IDs, stable errors, vulnerability scans, deployment and restore remain required roadmap work. Do not mark a whole roadmap step Run from this configuration-only change.

## Recorded setup checks

On 2026-10-23, the ten-project offline Maven verify completed successfully: 28 Java tests passed and seven executable service jars were packaged. The existing JDK 23 compiled with release 21; Mockito emitted dynamic-agent warnings. The Python suite passed 12 tests with one pre-existing Starlette/httpx deprecation warning. Live temporary-loopback checks passed for the packaged gateway (liveness/readiness 200, protected paths 401) and Python launcher (health 200, server header absent). Both test processes were stopped. All eight local .env files remained zero bytes, ignored, with examples trackable.

The structured receipt is docs/validation/SERVICE-CONFIGURATION-RESULTS.json. These checks establish the configuration baseline only; no live PostgreSQL/Kafka, business authentication, production deployment or restore was executed.
