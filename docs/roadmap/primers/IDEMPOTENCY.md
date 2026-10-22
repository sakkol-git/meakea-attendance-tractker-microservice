# Idempotency

## Read in ten to twenty minutes

What: Repeating one command identity produces the same intended result.

Why HRFlow needs this: A phone can lose the response after the server commits a check-in.

## Tiny example

Same key and canonical body returns same session ID; changed body under that key is 409.

## Common mistake

> **Pitfall:** A new key on every network retry defeats duplicate protection.

## Try and explain

- [ ] Simulate response loss and count exactly one session.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
