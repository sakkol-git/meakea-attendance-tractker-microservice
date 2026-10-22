# Locks and invariants

## Read in ten to twenty minutes

What: A transaction lock makes conflicting commands wait; constraints reject impossible stored states.

Why HRFlow needs this: Two leave reservations or approvals must not both spend/decide stale state.

## Tiny example

Lock employee then balance rows in a consistent order, update and commit.

## Common mistake

> **Pitfall:** Read balance then update without lock can overspend under concurrency.

## Try and explain

- [ ] Run two parallel requests and inspect final ledger/availability.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
