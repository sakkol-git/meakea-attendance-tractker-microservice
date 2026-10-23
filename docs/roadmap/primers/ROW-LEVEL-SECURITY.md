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
