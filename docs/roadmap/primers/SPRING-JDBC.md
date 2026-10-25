# Spring JDBC — superseded

> **Decision:** HRFlow uses Spring Data JPA (Hibernate) for all persistence, not JdbcClient. This file is kept for reference only. Follow [SPRING-JPA.md](SPRING-JPA.md) instead.

- **Decision:** Spring Data JPA (Hibernate) for persistence, not JdbcClient.
- **Reason:** Team already knows JPA; time is the scarcest resource.
- **Rejected alternatives:** JdbcClient adds boilerplate and a new concept to learn.
- **Implementation impact:** Keep `spring-boot-starter-data-jpa`; Flyway owns the
  schema; `ddl-auto: validate`; `open-in-view: false`.

- **Cost and operational impact:** None.
- **Reconsider when:** Outbox or report queries become awkward in JPA; at that point
  add a single native `@Query` first before introducing a new dependency.
