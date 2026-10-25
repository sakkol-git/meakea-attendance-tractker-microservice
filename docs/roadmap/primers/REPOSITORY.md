# Repository pattern and data access boundaries

> **Decision:** HRFlow uses Spring Data JPA (Hibernate) for all persistence. Repositories extend `JpaRepository`; Flyway owns the schema (`ddl-auto: validate`); `open-in-view: false` prevents hidden DB access in controllers. For complex filters use the Specification pattern; for outbox claiming use a native `@Query` with `FOR UPDATE SKIP LOCKED`. Design patterns come before raw SQL.

> **Note:** The Repository pattern mediates between the domain and data mapping layers, providing an in-memory collection-like interface for accessing and mutating domain entities.

## Purpose and core concept

The Repository pattern isolates domain business logic from the underlying database technology and query mechanisms (SQL, JPA, Hibernate, JDBC).

By encapsulating query logic, entity persistence, and data mapping behind high-level repository interfaces, application services interact with data as if it were stored in an in-memory collection (such as a `List` or `Map`).

## Problem being solved

Spreading direct SQL queries or raw database calls throughout controller and service classes causes severe architectural degradation:

- Leaked persistence concerns: Service classes become cluttered with JDBC connections, SQL string concatenation, and database-specific dialect handling.
- Hardcoded queries without tenant checks: Writing ad-hoc queries in multiple places increases the risk that an engineer forgets a `WHERE tenant_id = :tenantId` clause, causing data leaks.
- Untestable business logic: Services tightly coupled to concrete database connections cannot be unit-tested without connecting to a live database.

## Simple mental model

A repository is like an automated warehouse retrieval desk.

When you need an item from the warehouse, you hand your requisition ticket to the retrieval clerk at the counter. The clerk locates the bin, operates the forklift, and hands you the item. You do not need to know which aisle, shelf, or warehouse section stores the physical item.

## How it works

The Repository pattern operates across clean architectural layers:

1. Interface definition: The domain layer defines a clean repository interface (e.g., `LeaveRequestRepository`) containing domain-focused methods (`findById`, `save`, `findByEmployeeAndYear`).
1. Implementation binding: The infrastructure layer implements the interface using Spring Data JPA, jOOQ, or raw JDBC.
1. Dependency injection: Application services declare dependencies on the interface, never on the concrete implementation class.
1. Entity persistence: When a service calls `repository.save(entity)`, the repository translates the domain entity into SQL statements and persists it within the active transaction.
1. Query encapsulation: Complex multi-predicate queries (such as active shift lookups) are encapsulated inside dedicated repository methods rather than exposed to service callers.

## Why HRFlow needs it

In HRFlow's final service architecture:

- Strict package boundaries: Each domain package (`identityorg`, `attendance`, `leave`, `request`, `notification`, `reporting`) owns its repositories. The `leave` package never queries `attendance` repositories directly.
- Multi-tenant query enforcement: Repositories guarantee that every query includes mandatory `tenant_id` parameters.
- Testability: Domain services can be tested with rapid unit tests by mocking repository interfaces using Mockito.

## Minimal example

Spring Data JPA repository interface:

```java
public interface EmployeeRepository extends Repository<Employee, UUID> {

    Optional<Employee> findByTenantIdAndId(String tenantId, UUID id);

    Optional<Employee> findByTenantIdAndEmployeeCode(String tenantId, String employeeCode);

    Employee save(Employee employee);

    @Query("SELECT e FROM Employee e WHERE e.tenantId = :tenantId AND e.department.id = :deptId")
    List<Employee> findActiveByDepartment(@Param("tenantId") String tenantId,
                                          @Param("deptId") UUID deptId);
}
```

## Application-specific implementation

Tenant-enforcing repository implementation pattern in HRFlow:

```java
@Repository
public class JpaAttendanceSessionRepository implements AttendanceSessionRepository {

    private final EntityManager entityManager;

    public JpaAttendanceSessionRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<AttendanceSession> findActiveSession(String tenantId,
                                                         UUID employeeId,
                                                         LocalDate date) {
        String jpql = "SELECT s FROM AttendanceSession s " +
                      "WHERE s.tenantId = :tenantId " +
                      "  AND s.employeeId = :employeeId " +
                      "  AND s.workDate = :date " +
                      "  AND s.status = 'OPEN'";

        List<AttendanceSession> results = entityManager.createQuery(jpql, AttendanceSession.class)
                .setParameter("tenantId", tenantId)
                .setParameter("employeeId", employeeId)
                .setParameter("date", date)
                .getResultList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public AttendanceSession save(AttendanceSession session) {
        if (session.getId() == null) {
            entityManager.persist(session);
            return session;
        } else {
            return entityManager.merge(session);
        }
    }
}
```

## Common mistakes and pitfalls

Avoid these common repository anti-patterns:

> **Pitfall:** Cross-domain repository leakage. Injecting `AttendanceSessionRepository` directly into a service inside the `leave` package violates package autonomy and creates tight domain coupling. Domains must communicate via public service interfaces or outbox events, never across repository boundaries.

- Generic CRUD repositories exposing dangerous operations: Extending full Spring Data `JpaRepository` exposes `deleteAll()` and `findAll()`, which are catastrophic in multi-tenant environments if invoked without tenant filters. Prefer focused custom repository interfaces.
- Placing business logic inside repositories: Repositories should only query and persist entities; they must never calculate leave balances, enforce shift rules, or dispatch notifications.
- Ignoring N+1 query problems: Failing to use `JOIN FETCH` or `@EntityGraph` when loading parent entities with collections causes hundreds of queries during list retrieval.

## Testing and debugging

Validation routine:

- Slice test (`@DataJpaTest`): Test repository methods against Testcontainers PostgreSQL to verify that custom JPQL or SQL queries execute without syntax errors.
- Tenant isolation test: Insert records for Tenant A and Tenant B. Call `findActiveByDepartment` with Tenant A's identifier and assert that Tenant B records are never returned.
- Mocking in service tests: In unit tests, use `when(repo.findByTenantIdAndId(eq("tenant-a"), eq(id))).thenReturn(Optional.of(fixture))` to test service logic in isolation.

## Trade-offs and alternatives

Data access patterns:

- Repository pattern with JPA (HRFlow choice, ADR-009): Clean abstraction, high testability, decouples domain logic from database drivers; Spring Data generates common finders; complex queries use Specification or native `@Query`; requires defining and maintaining interface boundaries.
- Active Record pattern: Entities contain their own save and delete methods (`employee.save()`); simpler for rapid prototyping, but tightly couples domain models to database infrastructure.
- Direct jOOQ / JDBC queries in services: Fast and direct; scatters SQL strings and complicates unit testing.

## When not to use it

Do not use repositories for:

- High-volume read-only analytics: Bulk data exports, complex aggregate cross-tabulations, and streaming CSV reports should bypass entity repositories and execute optimized SQL projections directly.
- Simple static file lookups: Reading static JSON configuration files or application properties.

## Related documentation

- [Multi-tenancy primer](MULTI-TENANCY.md)
- [Database locks primer](DATABASE-LOCKS.md)
- [Dependency injection primer](DEPENDENCY-INJECTION.md)
- [Week 01 foundation build guide](../01-FOUNDATION/BUILD-GUIDE.md)
