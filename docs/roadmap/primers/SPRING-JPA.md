# Spring Data JPA and scoped queries

## Ten-minute concept

Spring Data JPA uses Hibernate to map Java entities to database tables. Repositories
extend `JpaRepository` and generate common queries; complex or native SQL lives in
`@Query` methods. Flyway remains the only owner of the schema — Hibernate never
auto-creates or alters tables in production (`ddl-auto: validate`).

Two safety settings belong in every service:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate      # Hibernate checks, Flyway owns
    open-in-view: false       # no hidden DB access in controllers
```

## HRFlow use

Repositories receive trusted tenant context from the application boundary and bind it
in every select, update, delete and count. Extend focused repository interfaces, never
expose `deleteAll()` or `findAll()` without a tenant predicate. For outbox claiming use
one native query with `FOR UPDATE SKIP LOCKED` so multiple workers never claim the same
batch.

```java
// Tenant-scoped Spring Data method — no manual SQL needed
Optional<Department> findByTenantIdAndId(UUID tenantId, UUID id);
```

```java
// Native query for the outbox worker — one method, no raw JDBC required
@Query(value = "SELECT * FROM outbox WHERE status = 'NEW' ORDER BY id LIMIT 50 FOR UPDATE SKIP LOCKED",
       nativeQuery = true)
List<OutboxEvent> claimBatch();
```

## Tenant isolation

Every personal table has a `tenant_id` column. Plan to use Hibernate's `@TenantId`
annotation in Week 2, which adds the tenant predicate to every query automatically,
instead of trusting every repository author to remember `WHERE tenant_id = :tenantId`.
Test with the cross-tenant fixture; absent context, denied insert and pooled-reuse
cannot leak.

## Design-pattern priority

Prefer design patterns over complex queries: Specification for dynamic filters,
Strategy for policy variants, Template for repeatable flows. A complex native query is
the last resort, never the starting point. JPA handles HRFlow's persistence comfortably;
a pattern-first approach keeps the code maintainable as the tenant count grows.

## Common mistake

> **Pitfall:** An unscoped count/export query leaks information even when find-by-id
> is scoped. RLS is a second guard, not permission to remove tenant predicates.

> **Pitfall:** Using `saveAndFlush` only when the test needs the duplicate constraint
> to fire immediately. Plain `save` batches until commit and the duplicate might not
> surface in time.

## Check understanding

- [ ] Explain the `ddl-auto: validate` boundary in two sentences.
- [ ] Name one failing test before returning to the step.

[Final architecture](../../Architecture.md), [standards](../ENGINEERING-STANDARDS.md), and [dashboard](../YOU-ARE-HERE.md).
