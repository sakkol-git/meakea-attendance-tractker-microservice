# Dependency injection

## Read in ten to twenty minutes

What: An object receives dependencies instead of creating global services internally.

Why HRFlow needs this: A handler can use a test repository or real repository without a service locator.

## Tiny example

A constructor accepts Clock; a fixed Clock makes late-arrival tests repeatable.

## Common mistake

> **Pitfall:** Adding interfaces for every class without an isolation need creates noise.

## Try and explain

- [ ] Point to a constructor dependency in the new app.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
