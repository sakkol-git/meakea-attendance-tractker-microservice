# Lifecycle transitions

## Read in ten to twenty minutes

What: A state machine defines which actions are valid in each current state.

Why HRFlow needs this: Leave and general requests reject stale or repeated approval decisions.

## Tiny example

PENDING can become APPROVED or REJECTED; APPROVED cannot be edited like DRAFT.

## Common mistake

> **Pitfall:** A State class per enum value is not automatically better than a clear switch.

## Try and explain

- [ ] Write transition-table tests first and keep the simple version if it stays clear.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
