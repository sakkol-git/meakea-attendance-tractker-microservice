# Specification

## Read in ten to twenty minutes

What: Small predicates compose a query rule without duplicating its combinations.

Why HRFlow needs this: Repeated report filters may need tenant/date/department composition.

## Tiny example

Keep mandatory tenant predicate outside optional filter composition.

## Common mistake

> **Pitfall:** Generic user-controlled predicates can erase scope or enable expensive queries.

## Try and explain

- [ ] Prove two actual composed filters are clearer than a simple method.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
