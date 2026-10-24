# ADR 009 — Spring Data JPA over JdbcClient

**Status:** Accepted  
**Date:** 2026-10-24  
**Supersedes:** Nothing; clarifies the persistence implementation choice left open in ADR 008.

## Context

ADR 008 chose PostgreSQL with Flyway and service-owned schemas. It did not lock the
Java persistence helper. The roadmap previously documented "explicit JDBC" as the
implementation, but the team's working knowledge is Spring Data JPA (Hibernate).
Switching to `JdbcClient` would introduce a new concept to learn at a time when
delivery pace is the primary constraint.

Modern HRFlow architecture puts **design patterns first**: Specification for dynamic
filters, Strategy for policy variants, Template for repeatable flows. Complex raw SQL
is the last resort when a pattern-based solution becomes awkward, not the starting
point. JPA supports this philosophy directly; JDBC pulls in the opposite direction by
making every filter a hand-written predicate.

## Decision

Use **Spring Data JPA (Hibernate)** for all persistence in every owner service.

| Concern | JPA choice |
| --- | --- |
| Schema ownership | Flyway exclusively; `ddl-auto: validate` only |
| Hidden lazy loading | `open-in-view: false` in every service |
| Tenant isolation | `@TenantId` annotation (Week 2); every personal table has `tenant_id` |
| Complex query | Named `@Query` JPQL, then native query — never raw JDBC |
| Outbox claiming | One native query: `FOR UPDATE SKIP LOCKED` |
| Design patterns | Specification, Strategy, Template — before writing complex SQL |

## Consequences

- Keep `spring-boot-starter-data-jpa` in every service that touches a database.
- Remove `spring-boot-starter-jdbc` where it was added as a JDBC alternative.
- Every context test should see Hibernate in the dependency tree (previously banned).
- Read "JDBC" in older guide sections as "JPA"; follow this ADR, not the older text.
- Every guide step that said "replace JPA with JDBC" is superseded; keep JPA.

## Rejected alternatives

- **JdbcClient:** More boilerplate per query, no generated finders, a new API for the
  team to learn, and it pulls toward hand-coded predicates rather than pattern-first
  design.
- **jOOQ:** Compile-time SQL safety is valuable, but adds a generator build step and
  a learning curve the schedule cannot absorb.

## Reconsider when

Outbox or report queries become genuinely awkward to express in JPQL and a native
`@Query` is insufficient. That signals a targeted micro-optimization, not a wholesale
persistence switch.
