# Repository boundary

## Read in ten to twenty minutes

What: A repository translates a scoped persistence query into domain records.

Why HRFlow needs this: Every query must keep tenant and resource scope; controllers do not write SQL.

## Tiny example

findByTenantIdAndEmployeeId uses both IDs; findById alone is unsafe.

## Common mistake

> **Pitfall:** Assuming an ID is globally unique means authorization is unnecessary.

## Try and explain

- [ ] Explain why a count query must also be tenant-scoped.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
