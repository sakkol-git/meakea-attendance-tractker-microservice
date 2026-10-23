# Spring JDBC and scoped SQL

## Ten-minute concept

JDBC executes explicit parameterized SQL without a JPA entity session. JdbcClient is a fluent Spring helper; transactions still define what commits together.

## HRFlow use

Repositories receive trusted tenant context from the application boundary and bind it in every select, update, delete and count. Never concatenate user values or table identifiers into SQL.

## Small proposed example

```java
// Proposed shape; table/column names are confirmed by the migration inventory first.
var names = jdbc.sql("select name from identity_org.department where tenant_id = :tenant")
    .param("tenant", trustedTenantId)
    .query(String.class)
    .list();
```

## Common mistake

> **Pitfall:** An unscoped count/export query leaks information even when find-by-id is scoped. RLS is a second guard, not permission to remove predicates.

## Check understanding

- [ ] Explain the boundary in two sentences.
- [ ] Name one failing test before returning to the step.

[Final architecture](../../Architecture.md), [standards](../ENGINEERING-STANDARDS.md), and [dashboard](../YOU-ARE-HERE.md).
