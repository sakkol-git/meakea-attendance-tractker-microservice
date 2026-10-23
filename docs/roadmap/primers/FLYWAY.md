# Flyway database schema migrations

> **Note:** Flyway provides version-controlled, reproducible database migrations, ensuring that development, testing, and production databases evolve predictably alongside application source code.

## Purpose and core concept

Database migration tools manage relational database schema changes using sequential, immutable SQL scripts tracked in version control.

Instead of manual `ALTER TABLE` commands executed ad-hoc, Flyway executes numbered migration scripts in strict sequence and records execution checksums in a schema history table.

## Problem being solved

Without automated schema migrations, teams encounter critical database deployment issues:

- Environment divergence: Development, staging, and production databases drift apart, causing queries that work locally to fail with missing column errors in production.
- Untracked schema history: Nobody knows who added a specific constraint, whether an index exists in production, or why a column datatype was modified.
- Deployment failure: Deploying a new application binary before applying its database schema modifications causes application crashes on startup.

## Simple mental model

Flyway is like a ledger of bank account deposits. Each deposit has a sequential transaction number and an exact amount.

When you reconcile your bank statement, you start from transaction #1 and apply each transaction in exact order up to the latest entry. You never alter or erase an old deposit entry once it has cleared.

## How it works

Flyway automates schema synchronization through structured file naming and history tracking:

1. Migration file naming: Scripts follow standard conventions: `V{version}__{description}.sql` (e.g., `V1__init_identity_schema.sql`, `V2__add_employee_code.sql`).
1. History table check: On application startup, Flyway checks the `flyway_schema_history` table in the target schema.
1. Script discovery: Flyway scans the application classpath (e.g., `classpath:db/migration`) for migration scripts.
1. Checksum validation: For all previously executed migrations, Flyway recalculates the SHA-256 checksum and compares it against the stored checksum. If an applied script was modified, Flyway halts with a validation error.
1. Pending script execution: Flyway executes any new migration scripts whose version numbers are higher than the latest recorded version, running each script inside a database transaction.
1. History update: Upon successful execution, Flyway records the version, description, execution timestamp, and checksum.

## Why HRFlow needs it

HRFlow manages six distinct domain schemas (`identity_org`, `attendance`, `leave`, `request`, `notification`, `reporting`) within a single PostgreSQL 16 database:

- Multi-schema isolation: Flyway migrations are organized into per-schema folders or executed using dedicated schema targets, ensuring that domain migrations remain independent.
- Self-hosted reliability: Self-hosted deployments update automatically upon pulling new container images without manual database intervention.
- Automated testing: Integration tests use Testcontainers to spin up ephemeral PostgreSQL containers, applying all Flyway migrations from scratch to guarantee repeatable test baselines.

## Minimal example

Sample Flyway migration scripts:

`db/migration/V1__create_identity_tables.sql`:

```sql
CREATE TABLE identity_org.tenants (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE identity_org.users (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES identity_org.tenants(id),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tenant_email UNIQUE (tenant_id, email)
);
```

`db/migration/V2__add_user_status.sql`:

```sql
ALTER TABLE identity_org.users
ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_users_tenant_status
ON identity_org.users (tenant_id, status);
```

## Application-specific implementation

Spring Boot configuration in `application.yaml`:

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    schemas:
      - identity_org
      - attendance
      - leave
      - request
      - notification
      - reporting
    locations:
      - classpath:db/migration/identity
      - classpath:db/migration/attendance
      - classpath:db/migration/leave
      - classpath:db/migration/request
      - classpath:db/migration/notification
      - classpath:db/migration/reporting
    table: flyway_schema_history
```

## Common mistakes and pitfalls

Avoid these frequent migration mistakes:

> **Pitfall:** Editing an already applied migration script. If a migration was merged and executed in any environment, modifying its SQL invalidates its checksum and breaks future deployments. Always create a new versioned script (e.g., `V3__fix_column_name.sql`) to introduce corrections.

- Non-transactional migrations in PostgreSQL: DDL statements in PostgreSQL (such as `CREATE TABLE` and `ALTER TABLE`) are transactional. Ensure all related changes are grouped in a single migration script so failures roll back cleanly.
- Adding `NOT NULL` columns without default values: Adding a `NOT NULL` column to a table with existing rows fails immediately unless a default value is specified or a backfill migration is executed first.
- Destructive changes in single deployments: Renaming or dropping columns immediately breaks older application instances during rolling restarts. Use expand-contract patterns: add new column, migrate data, deploy code, and drop old column in a later release.

## Testing and debugging

Validation procedure:

- Clean migration test: Run `mvn test -Dtest=DatabaseMigrationTest`. Spin up an empty PostgreSQL container and verify that Flyway applies all migrations from V1 to latest with zero SQL errors.
- Checksum repair: If a developer accidentally modifies whitespace in a migration file during local development, run `mvn flyway:repair` locally (never on production without review).
- Repeatable test seeds: Use separate `R__seed_test_data.sql` repeatable scripts for local test fixtures that execute only in local test profiles.

## Trade-offs and alternatives

Comparing migration tools:

- Flyway (HRFlow choice): Pure SQL, simple to learn, native database features, zero proprietary abstraction layers.
- Liquibase: XML/YAML/JSON change-sets, database-independent abstractions, built-in rollback tags; however, verbose syntax and steep learning curve.
- ORM auto-generation (`hibernate.hbm2ddl.auto = update`): Uncontrolled, unsafe for production, cannot perform complex data backfills or index tuning.

## When not to use it

Do not use Flyway for:

- High-frequency application configuration: Dynamic feature flags and runtime settings belong in database configuration tables, not Flyway migration scripts.
- Ephemeral test mock data: Test assertion fixtures should be created inside individual test methods via builders, not hardcoded into global database migrations.

## Related documentation

- [Database locks primer](DATABASE-LOCKS.md)
- [Testcontainers primer](TESTCONTAINERS.md)
- [Week 01 foundation build guide](../01-FOUNDATION/BUILD-GUIDE.md)
- [API and database design](../../REST-API-DATABASE-DESIGN.md)
