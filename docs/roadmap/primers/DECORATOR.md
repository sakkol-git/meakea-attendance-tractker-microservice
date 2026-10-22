# Decorator candidate

## Read in ten to twenty minutes

What: A decorator wraps a behavior with another behavior using the same interface.

Why HRFlow needs this: Custom wrapping is useful only when framework transaction/audit features do not suffice.

## Tiny example

One wrapper could record duration, but normal framework metrics are simpler.

## Common mistake

> **Pitfall:** Wrapping transactions/idempotency casually changes atomicity.

## Try and explain

- [ ] Reject until a concrete isolation/composition need survives review.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
