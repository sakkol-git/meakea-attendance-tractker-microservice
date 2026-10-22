# Adapter

## Read in ten to twenty minutes

What: An adapter translates an external capability into a narrow internal port.

Why HRFlow needs this: Private disk storage needs deterministic failure tests without touching production files.

## Tiny example

StoragePort has put/get; DiskStorage and TestStorage implement the same behavior.

## Common mistake

> **Pitfall:** Putting authorization inside one adapter lets another bypass it.

## Try and explain

- [ ] Run identical storage behavior tests plus service authorization tests.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
