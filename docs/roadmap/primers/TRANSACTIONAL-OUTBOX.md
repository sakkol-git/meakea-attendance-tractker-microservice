# Transactional outbox

## Read in ten to twenty minutes

What: Business data and a durable event intent are inserted in one database transaction.

Why HRFlow needs this: Notifications/reporting can recover without making user success depend on their workers.

## Tiny example

Commit session and outbox together; handler receipt plus inbox makes replay safe.

## Common mistake

> **Pitfall:** Sending provider request inside an open DB transaction is neither atomic nor fast.

## Try and explain

- [ ] Crash after handler commit and verify no repeated logical effect.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
