# Kafka basics and deferral

## Read in ten to twenty minutes

What: Kafka stores keyed streams with consumer groups and partition offsets.

Why HRFlow needs this: The old scaffold has Kafka configuration; outcome C does not run Kafka because no two deployments require it.

## Tiny example

A key orders events in one partition, not across all topics or all employees.

## Common mistake

> **Pitfall:** Kafka producer idempotence does not make PostgreSQL/provider effects exactly once.

## Try and explain

- [ ] Explain what extra outage/offset tests a future broker ADR would require.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
