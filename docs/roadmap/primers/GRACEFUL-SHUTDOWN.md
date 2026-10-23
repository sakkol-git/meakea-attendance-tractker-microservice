# Graceful shutdown

## Ten-minute concept

What: Stop accepting new work, finish/roll back active transactions and release recoverable claims.

Why HRFlow needs it: A deploy/reboot must not leave false success or permanently stuck worker lease.

## Tiny example

A stopped publisher retains outbox intent; a restarted consumer replays by inbox receipt. Commit DB effect before acknowledging Kafka offset, and bound drain time.

## Common mistake

> **Pitfall:** Killing a process then clearing its DB tables is not recovery.

## Check understanding

- [ ] Explain what the check prevents in two sentences.
- [ ] Name one observable failing result before returning to the step.

[Standards](../ENGINEERING-STANDARDS.md) and [dashboard](../YOU-ARE-HERE.md).
