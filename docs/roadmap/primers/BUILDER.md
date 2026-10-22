# Builder

## Read in ten to twenty minutes

What: A builder gives named optional values and safe defaults for construction.

Why HRFlow needs this: Repeated test fixture setup can obscure the one field a test changes.

## Tiny example

EmployeeFixture.builder().tenantId(T).status(ACTIVE).build().

## Common mistake

> **Pitfall:** Using a builder to bypass required tenant validation creates unsafe test defaults.

## Try and explain

- [ ] Demonstrate duplicated setup in two real tests before adopting.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
