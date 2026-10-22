# Real database tests

## Read in ten to twenty minutes

What: Testcontainers starts disposable dependency containers from tests.

Why HRFlow needs this: Locks, indexes and migrations must be tested on PostgreSQL rather than an incompatible in-memory imitation.

## Tiny example

A test starts PostgreSQL 16, migrates, attempts a duplicate row, and asserts rejection.

## Common mistake

> **Pitfall:** Deleting a production volume while trying to reset a test is unsafe.

## Try and explain

- [ ] Identify test-only container name/volume and prove teardown cannot target production.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
