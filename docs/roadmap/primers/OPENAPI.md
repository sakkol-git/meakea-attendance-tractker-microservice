# One API contract

## Read in ten to twenty minutes

What: OpenAPI describes paths, bodies, errors and auth in a machine-readable file.

Why HRFlow needs this: Flutter must mock stable responses before A finishes backend.

## Tiny example

contracts/openapi.yaml describes 201 session response and 409 duplicate conflict.

## Common mistake

> **Pitfall:** Editing separate client schema copies creates drift and late integration failures.

## Try and explain

- [ ] Validate examples and compare live response against the same contract.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
