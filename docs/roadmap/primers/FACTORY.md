# Factory candidate

## Read in ten to twenty minutes

What: A factory chooses one implementation at a creation boundary.

Why HRFlow needs this: It may help only when multiple real configured providers exist.

## Tiny example

If only disk storage exists, construct DiskStorage directly.

## Common mistake

> **Pitfall:** Inventing providers solely to justify a factory fails admission.

## Try and explain

- [ ] Keep simple construction until a second real variation hurts.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
