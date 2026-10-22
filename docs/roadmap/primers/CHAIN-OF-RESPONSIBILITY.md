# Validation chain candidate

## Read in ten to twenty minutes

What: A chain runs ordered checks with explicit stop/continue behavior.

Why HRFlow needs this: Check-in has validations, but ordinary methods may be clearer.

## Tiny example

Validate tenant, active employee, schedule and duplicate under proper transaction order.

## Common mistake

> **Pitfall:** A chain can conceal required checks or move them outside a lock.

## Try and explain

- [ ] Prefer explicit sequence unless composition pain is demonstrated.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
