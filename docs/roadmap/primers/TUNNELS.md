# Outbound tunnel

## Read in ten to twenty minutes

What: A connector opens outbound connections so public requests can reach an approved local origin.

Why HRFlow needs this: CGNAT/router capability is unknown; tunnel avoids inbound port forwarding.

## Tiny example

Only application hostname maps to Caddy; never route Docker, Kafka or PostgreSQL.

## Common mistake

> **Pitfall:** A tunnel is still public exposure and does not replace app auth/backup/firewall.

## Try and explain

- [ ] Disable connector and observe remote outage while local data remains.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
