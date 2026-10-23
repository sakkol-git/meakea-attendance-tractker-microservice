# ADR 007 — Quality and tenant defense refinement

## Status and context

Status: Accepted for the guide by the explicit V2 quality request, 2026-10-23. Refines ADR 002/003 clauses; ADR 006 outcome C remains. No RLS, controller, architecture test or CI scan is claimed implemented. Existing schema selection and shared credentials are not isolation proof.

## Decision

- **Decision:** Adopt PostgreSQL 16 RLS for personal tenant business tables, in addition to mandatory scoped repositories, composite keys and object authorization. Keep owner-local outbox/audit and handler-local receipts.
- **Reason:** Q04 defaults to defense-in-depth when capacity fits; optional Week-3 Builder work is displaced, not security tests.
- **Rejected alternatives:** Tenant predicates alone; owner/superuser runtime roles; adding databases/services/broker to substitute for authorization.
- **Implementation impact:** ENABLE/FORCE policies with USING/WITH CHECK; set trusted tenant transaction-locally; test pool reuse/missing context/background jobs. Narrow pre-auth lookup is a reviewed exception, not an arbitrary bypass. Publish RFC 9457-style errors and add ArchUnit boundary tests.
- **Cost and operational impact:** Existing PostgreSQL/JVM only. Budget explicit atomic policy/test slots in Week 3 and drop optional pattern refactors. A restore role and migration role are separate; restore must recover all tenants.
- **Reconsider when:** Measured correctness/latency or actual capacity disproves the tested policy; no temporary disabling to force a green gate.

## Security boundaries

Runtime is non-owner, non-superuser and lacks BYPASSRLS. Empty tenant context denies rows/inserts; pooled connections use set_config with local=true inside the same transaction and never a request-header value. RLS is not a defense against arbitrary SQL executed with a role allowed to set its own tenant variable; prevent injection and retain repository/resource checks.

Tenant selection before authentication is limited to a fixed-query credential lookup by slug/email. A separately reviewed narrow credential path can access only credential material needed to verify login; no client-driven business-table scope or general SQL. If a security-definer function is used, lock search_path, revoke public execution, restrict ownership/grants, and test it as a dedicated spike before release. Worker/backup/operator scopes are explicit and audited.

## Implementation and migration

Inspect actual generated table/package names first. Policies arrive in new Flyway migrations, never edits to applied files. Add non-owner Testcontainers fixtures for tenant A/B, denied insert, absent context, pool reuse, transaction rollback and worker processing. Verify controlled backup captures every tenant and clean restore preserves grants/policies. Golden-slice organization writes introduce the same recipe later modules repeat.

## Consequences

Additional tests/configuration cost replaces optional Builder/Specification polish. No new deployable, managed service, certification or availability guarantee is introduced. A policy test failure blocks launch. Dependency fitness and shared problem responses are practices, not middleware products.

## References

- [Architecture review](../roadmap/ARCHITECTURE-REVIEW-V2.md).
- [Canonical contracts](../REST-API-DATABASE-DESIGN.md).
- [PostgreSQL row security](https://www.postgresql.org/docs/16/ddl-rowsecurity.html).
- [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html).
