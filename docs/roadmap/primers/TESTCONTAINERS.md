# Integration testing with Testcontainers

> **Note:** Testcontainers provides lightweight, throwaway instances of real databases, message brokers, and web servers running inside Docker containers for reliable automated integration testing.

## Purpose and core concept

Testcontainers is an open-source Java testing library that wraps Docker's API to manage the lifecycle of containerized dependencies during test execution.

Instead of testing against in-memory mocks (such as H2 Database) or relying on a pre-existing external database server, Testcontainers programmatically launches real Docker containers before test execution and tears them down immediately when tests finish.

## Problem being solved

Relying on in-memory embedded databases (like H2 or Fongo) for testing creates false confidence:

- SQL dialect discrepancies: H2 does not support advanced PostgreSQL features like `JSONB` operators, `ON CONFLICT DO UPDATE`, `SELECT FOR UPDATE SKIP LOCKED`, or strict schema partitioning. Queries that pass in H2 fail in production PostgreSQL.
- Shared state pollution: Using a shared external test database causes parallel test runs to overwrite each other's test fixtures, leading to flaky, non-deterministic test suites.
- Fragile developer setup: Requiring every developer to manually install and configure matching database versions locally creates "works on my machine" bottlenecks.

## Simple mental model

Testing with H2 is like practicing flight maneuvers in a low-resolution desktop computer game. It is fast and accessible, but it does not behave like a real Boeing 747 in high-wind turbulence.

Testing with Testcontainers is like practicing in a certified full-motion hydraulic flight simulator with authentic cockpit controls and real aerodynamic physics.

## How it works

Testcontainers integrates directly with JUnit Jupiter (Boot-managed JUnit 6) and Docker:

1. Test initialization: JUnit starts a test class annotated with `@Testcontainers`.
1. Container provision: Testcontainers calls the local Docker daemon to pull the exact production image (`postgres:16-alpine`) and start a fresh container on a dynamically assigned host port.
1. Dynamic properties: Spring Boot's `@DynamicPropertySource` dynamically injects the container's randomized JDBC URL, username, and password into the Spring environment.
1. Flyway migration: Spring Boot starts up and Flyway applies all production migrations to the fresh container database.
1. Test execution: Integration test methods execute real database queries against real PostgreSQL.
1. Automatic cleanup: When tests finish (or if the JVM crashes unexpectedly), the Testcontainers Ryuk sidecar container terminates and removes the temporary container.

## Why HRFlow needs it

In HRFlow's final service architecture:

- Multi-tenant constraint verification: Verifying composite foreign keys, unique constraint enforcement, and PostgreSQL check constraints requires a real PostgreSQL 16 engine.
- Transactional outbox validation: Testing `SELECT FOR UPDATE SKIP LOCKED` and concurrent worker polling cannot be simulated accurately in H2.
- CI/CD reproducibility: Automated CI pipelines run tests inside isolated ephemeral containers with zero pre-configured database servers.

## Minimal example

Proposed illustration, not existing source. Boot 4.1.1 manages Testcontainers 2.0.5: import org.testcontainers.postgresql.PostgreSQLContainer and use its non-generic type. Confirm all other imports/fixtures against the owner service before creating this test.


JUnit Jupiter (Boot-managed JUnit 6) test class with PostgreSQL Testcontainers:

```java
@Testcontainers
@SpringBootTest
class AttendanceRepositoryTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("hrflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AttendanceSessionRepository sessionRepo;

    @Test
    void shouldPersistAndRetrieveAttendanceSession() {
        UUID empId = UUID.randomUUID();
        AttendanceSession session = AttendanceSession.create("tenant-a", empId, Instant.now());
        sessionRepo.save(session);

        Optional<AttendanceSession> found =
                sessionRepo.findById(session.getTenantId(), session.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTenantId()).isEqualTo("tenant-a");
    }
}
```

## Application-specific implementation

Reusable base integration test class in HRFlow:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("hrflow_it")
                .withUsername("hrflow")
                .withPassword("hrflow_secret")
                .withReuse(true);
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.schemas",
                     () -> "identity_org,attendance,leave,request,notification,reporting");
    }
}
```

By defining a static container with `.withReuse(true)` in an abstract parent class, all test classes share a single container instance across the entire test suite, dramatically reducing startup time.

## Common mistakes and pitfalls

Avoid these Testcontainers misconfigurations:

> **Pitfall:** Starting a fresh container per test method. Spinning up a new Docker container takes 2 to 5 seconds. If you start a new container for every test method across 100 tests, your test suite will take 10 minutes. Always use static container singletons shared across the test class or test suite.

- Docker daemon socket permissions: On macOS and Linux, ensure the user executing Maven has read/write permissions to `/var/run/docker.sock`.
- Port hardcoding: Never hardcode port numbers like `5432` in test properties. Testcontainers maps container ports to random available host ports to avoid port conflicts with local running databases.
- Leaving test state dirty: Because tests share the singleton container, individual test methods must clean up their created rows or wrap test methods in `@Transactional` to roll back changes upon completion.

## Testing and debugging

Verification checks:

- Docker availability test: Verify that Docker Desktop or Docker Engine is running before executing `./mvnw test`.
- Visual inspection: Run `docker ps` while tests are running to observe the temporary `postgres` container and the `testcontainers/ryuk` cleanup container.
- Flyway log validation: Check test output logs to verify that Flyway migrations applied successfully against the container before tests ran.

## Trade-offs and alternatives

Integration testing comparison:

- Testcontainers (HRFlow choice): Exact production parity, zero mock drift, portable across developer laptops and CI; requires Docker daemon and moderate memory overhead.
- In-memory H2: Blazing fast startup (sub-second), zero Docker dependency; suffers from SQL syntax differences and false positives/negatives.
- Pre-shared external database: Fast test runs; fragile, susceptible to concurrent test state collisions, and requires manual maintenance.

## When not to use it

Do not use Testcontainers for:

- Pure domain logic unit tests: Business calculation rules (such as computing overtime hours or checking leave eligibility formulas) should be tested using pure JUnit unit tests with zero database overhead.
- Simple value object mapping: DTO converters, date formatters, and utility functions require no container dependencies.

## Related documentation

- [Flyway migrations primer](FLYWAY.md)
- [Database locks primer](DATABASE-LOCKS.md)
- [Docker Compose primer](DOCKER-COMPOSE.md)
- [Week 01 foundation build guide](../01-FOUNDATION/BUILD-GUIDE.md)
