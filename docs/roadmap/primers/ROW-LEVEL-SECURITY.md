# PostgreSQL row-level security

## Ten-minute concept

What: RLS filters/denies rows for a database role using a policy.

Why HRFlow needs it: It adds defense if a scoped query is accidentally omitted; it never replaces object checks.

## Tiny example

Transaction-local app.tenant_id plus USING/WITH CHECK denies absent context.

## Common mistake

> **Pitfall:** Testing as table owner/superuser can bypass the protection being tested.

## Check understanding

- [ ] Explain what the check prevents in two sentences.
- [ ] Name one observable failing result before returning to the step.

[Standards](../ENGINEERING-STANDARDS.md) and [dashboard](../YOU-ARE-HERE.md).

## Workers cannot bypass the policy

A worker cannot read a personal outbox with no tenant context and call that a scheduler. The selected owner registry holds only trusted tenant UUID/lifecycle/revision metadata; a separate private job role discovers those IDs, then sets transaction-local tenant and runs scoped data queries. HTTP business repositories have no discovery grant and cannot choose that role. This narrow infrastructure exception is documented in ADR 008; it is not a cross-tenant business-data API. Test worker pool reuse and absent-context payload denial too.
