# Multi-tenancy and data isolation

> **Note:** Multi-tenancy allows a single application deployment and shared database to serve multiple distinct organizations while strictly isolating their data, configuration, and security domains.

## Purpose and core concept

Multi-tenancy enables an application to support multiple customer organizations (tenants) on shared computational and storage infrastructure. Complete isolation requires that Tenant A can never read, modify, or infer the existence of data belonging to Tenant B.

In HRFlow, multi-tenancy is enforced through discriminator columns (`tenant_id`), composite database constraints, and cryptographic token-derived tenant contexts.

## Problem being solved

Without rigorous tenant isolation, multi-tenant systems suffer catastrophic security and business vulnerabilities:

- Cross-tenant data leakage: A missing `WHERE tenant_id = ?` clause exposes employee salaries, personal contact numbers, and confidential medical certificates to unauthorized companies.
- Accidental cross-tenant mutations: An administrative update query without tenant scoping modifies job roles or shifts across all organizations simultaneously.
- Cross-tenant foreign key hijacking: An attacker in Tenant A creates a leave request referencing an employee ID belonging to Tenant B.

## Simple mental model

Multi-tenancy is like an apartment building. All residents share the same physical foundation, plumbing, and roof. However, each apartment has its own unique deadbolt lock and key.

A resident living in Apartment 301 can never use their key to unlock Apartment 402, nor can they view what is inside another resident's private rooms.

## How it works

HRFlow implements pooled multi-tenancy with discriminator column isolation and token trust:

1. Token issuance: When a user authenticates, the auth service includes their verified `tenant_id` inside the cryptographically signed JWT claims payload.
1. Security filter extraction: An incoming HTTP request passes through Spring Security's filter chain. The filter verifies the JWT signature and extracts the `tenant_id`.
1. ThreadLocal context: The verified tenant identifier is stored in a request-scoped `TenantContext` accessible throughout the service layer.
1. Query scoping: All repository methods append `AND e.tenantId = :tenantId` to database queries.
1. Database constraints: Every business table enforces a composite primary key or composite unique index on `(tenant_id, id)`. Foreign keys must reference both `tenant_id` and the parent ID.

## Why HRFlow needs it

HRFlow is designed for SME environments where multiple independent businesses share a single self-hosted server deployment:

- Privacy compliance: Labor regulations and privacy laws strictly forbid sharing employee attendance records or disciplinary logs between distinct corporate entities.
- Shared infrastructure cost: Running independent database instances per company exceeds the hardware budget and memory limits of self-hosted machines.
- Consolidated administration: One operator manages the service stack backup/update/monitoring; each business service still owns its schema.

## Minimal example

PostgreSQL composite constraint preventing cross-tenant references:

```sql
-- Parent table enforces composite uniqueness
CREATE TABLE identity_org.employees (
    tenant_id VARCHAR(64) NOT NULL,
    id UUID NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    CONSTRAINT pk_employees PRIMARY KEY (tenant_id, id)
);

-- Child table enforces composite foreign key
CREATE TABLE leave.leave_requests (
    tenant_id VARCHAR(64) NOT NULL,
    id UUID NOT NULL,
    employee_id UUID NOT NULL,
    start_date DATE NOT NULL,
    CONSTRAINT pk_leave_requests PRIMARY KEY (tenant_id, id),
    CONSTRAINT fk_leave_employee FOREIGN KEY (tenant_id, employee_id)
        REFERENCES identity_org.employees (tenant_id, id)
);
```

## Application-specific implementation

Tenant context holder and entity listener in Java:

```java
public final class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        String tenant = CURRENT_TENANT.get();
        if (tenant == null || tenant.isBlank()) {
            throw new IllegalStateException("No authenticated tenant context established");
        }
        return tenant;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
```

```java
@Entity
@Table(name = "attendance_sessions", schema = "attendance")
public class AttendanceSession {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @PrePersist
    protected void onPrePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getTenantId();
        }
    }

    public UUID getId() { return id; }
    public String getTenantId() { return tenantId; }
    public UUID getEmployeeId() { return employeeId; }
}
```

## Common mistakes and pitfalls

Avoid these dangerous multi-tenancy pitfalls:

> **Pitfall:** Trusting a tenant identifier passed in client request bodies, query strings, or custom headers (like `X-Tenant-ID`). Attackers can manipulate client parameters. Always extract and verify the tenant ID strictly from the authenticated cryptographic JWT.

- ThreadLocal memory leaks: Failing to call `TenantContext.clear()` in a `finally` block or filter completion hook allows worker threads to reuse a previous request's tenant identity.
- Omitting composite foreign keys: A foreign key referencing only `id` allows an employee in Tenant A to link records to a department in Tenant B. Always enforce composite foreign keys on `(tenant_id, parent_id)`.
- Global background jobs: Scheduled batch jobs running outside an HTTP request must explicitly iterate over active tenants and set the tenant context per iteration.

## Testing and debugging

Mandatory verification criteria:

- Two-tenant isolation test: Create an entity in Tenant A. Issue a request as an authenticated user in Tenant B attempting to read, update, or delete Tenant A's entity by ID. Assert that the response returns 404 Not Found or 403 Forbidden.
- Cross-tenant foreign key test: Attempt to insert a child record with `tenant_id = 'A'` referencing a parent record with `tenant_id = 'B'`. Assert that the database rejects the insertion with a foreign key violation.
- Leaked context test: Execute a sequence of requests alternating between Tenant A and Tenant B tokens; verify that no response contains cross-tenant entities.

## Trade-offs and alternatives

Architecture models for multi-tenancy:

- Column-level multi-tenancy (HRFlow choice): Shared database, shared schema, discriminator column. Highly resource-efficient and simple migrations; requires diligent query scoping.
- Schema-per-tenant: Separate schema for each tenant in one database. Stronger physical isolation; however, running Flyway migrations across hundreds of schemas becomes slow and complex.
- Database-per-tenant: Complete physical database separation. Ultimate security isolation; however, hardware cost and connection pool overhead make self-hosting impractical.

## When not to use it

Do not use multi-tenant filtering for:

- Platform operator administration: System-level health checks, server disk monitoring, and automated database backup routines operate globally across the physical host.
- Tenant lookup during initial login: Locating the tenant domain before user credentials are verified requires an isolated tenant-discovery lookup.

## Related documentation

- [JWT and refresh tokens primer](JWT-REFRESH.md)
- [Database locks primer](DATABASE-LOCKS.md)
- [Week 02 identity and host build guide](../02-IDENTITY-AND-HOST/BUILD-GUIDE.md)
- [Architecture decision record 002](../../adr/002-TENANT-AND-PERSISTENCE-ISOLATION.md)
