# Offline commands

## Read in ten to twenty minutes

What: A durable queue saves command identity/payload before network submission.

Why HRFlow needs this: A phone restart must not lose a pending check-in or submit it twice.

## Tiny example

Persist key and immutable body; retry same command; 412 requires user reconciliation.

## Common mistake

> **Pitfall:** Editing body but retaining key creates an idempotency mismatch.

## Try and explain

- [ ] Restart phone app offline and verify exactly one final server resource.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
