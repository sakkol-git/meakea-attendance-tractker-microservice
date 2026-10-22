# Multi-tenancy

## Read in ten to twenty minutes

What: One running app serves multiple isolated companies.

Why HRFlow needs this: Tenant A must never read or change tenant B employees, files or report counts.

## Tiny example

The JWT tenant plus employee ID scopes a query and its FK.

## Common mistake

> **Pitfall:** A tenantId column or hidden UI button alone does not enforce isolation.

## Try and explain

- [ ] Try a tenant B ID through a tenant A token and assert no row changed.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
