# HRFlow glossary

## Terms used by the guide

**Tenant:** An isolated company scope derived from a verified token.

**Principal:** Authenticated user/service identity, distinct from employee record.

**JWT:** Signed short-lived access token; decoding is not signature validation.

**Refresh family:** Related rotating refresh secrets, stored hashed and revoked on reuse.

**Migration:** Versioned database change applied once.

**Invariant:** A condition that must remain true under retries and concurrency.

**Idempotency:** Same command identity yields one intended effect.

**Outbox:** Durable event intent committed with domain data.

**Inbox:** Receipt committed with a handler effect to prevent duplicates.

**Projection:** Derived read model with source revision and visible freshness.

**Aggregate revision:** Monotonic owner state version, not wall-clock order.

**Lease:** Time-limited worker claim recoverable after a crash.

**CGNAT:** ISP translation that prevents ordinary inbound router forwarding.

**Reverse proxy:** Gateway forwarding approved requests to private application.

**TLS:** Encrypted authenticated network transport.

**RPO:** Maximum agreed data-loss interval, not a promise from a backup filename.

**RTO:** Agreed recovery duration measured by drills.

**Green tag:** Git label for tested app startup and a stone win.

**Mock server:** Responses generated from OpenAPI before live implementation.

**Testcontainers:** Real disposable dependency containers started by tests.

**CI:** Automated build/tests on pushes, with secrets protected.

**Cron:** Host scheduler; sleeping/unavailable host can miss jobs.

**Uptime Kuma:** Private availability monitoring; cannot alert externally when its entire host is down.

**Caddy:** Reverse proxy; public automatic TLS requires reachable validation or configured DNS.

**Cloudflare Tunnel:** Outbound connector; remote access still depends on ISP/provider.

**JPA:** Spring Data JPA (Hibernate) is the HRFlow persistence layer. Repositories extend JpaRepository; Flyway owns the schema (ddl-auto: validate); open-in-view: false prevents hidden DB calls in controllers.

**Kafka:** Durable partitioned event log; an offset is a consumer's recorded position, not proof a database effect committed.

**Avro and Registry:** Versioned event schema and its compatibility catalog; payload fields must match the reviewed wire schema.

**Nuxt:** The existing Vue/TypeScript web framework; Flutter is the mobile UI framework.

**RLS:** PostgreSQL row-level policies that deny foreign-tenant data even when application SQL is faulty; runtime roles cannot bypass them.

**CSRF:** A hostile site tries to make a browser send its cookies; exact Origin and CSRF-token checks protect cookie-authenticated mutations.

**Idempotency:** Retrying the same scoped command key/body returns the same result instead of performing the effect twice.

**MDC:** Request-scoped logging context used for safe correlation/technical tenant fields; clear it when a request or job ends.

**Optimistic version:** A saved revision checked by If-Match; stale changes fail with 412 rather than overwriting a newer decision.

**JPA:** Spring Data JPA (Hibernate) is the HRFlow persistence layer. Repositories extend JpaRepository; Flyway owns the schema (ddl-auto: validate); open-in-view: false prevents hidden DB calls in controllers.

**Kafka:** Durable partitioned event log; an offset is a consumer's recorded position, not proof a database effect committed.

**Avro and Registry:** Versioned event schema and its compatibility catalog; payload fields must match the reviewed wire schema.

**Nuxt:** The existing Vue/TypeScript web framework; Flutter is the mobile UI framework.

**RLS:** PostgreSQL row-level policies that deny foreign-tenant data even when application SQL is faulty; runtime roles cannot bypass them.

**CSRF:** A hostile site tries to make a browser send its cookies; exact Origin and CSRF-token checks protect cookie-authenticated mutations.

**Idempotency:** Retrying the same scoped command key/body returns the same result instead of performing the effect twice.

**MDC:** Request-scoped logging context used for safe correlation/technical tenant fields; clear it when a request or job ends.

**Optimistic version:** A saved revision checked by If-Match; stale changes fail with 412 rather than overwriting a newer decision.
