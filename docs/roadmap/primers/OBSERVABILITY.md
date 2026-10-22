# Health and safe logs

## Read in ten to twenty minutes

What: Health and structured logs make failures visible without exposing private data.

Why HRFlow needs this: Kuma checks availability; outbox/backup age signals reveal incomplete work.

## Tiny example

Log event ID and outcome, never JWT, refresh secret or raw HR evidence.

## Common mistake

> **Pitfall:** A healthy container does not prove a recent backup can be restored.

## Try and explain

- [ ] Show readiness, last successful external copy and scratch restore evidence.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
