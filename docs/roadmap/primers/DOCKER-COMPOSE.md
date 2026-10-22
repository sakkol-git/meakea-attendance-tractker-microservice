# Docker Compose

## Read in ten to twenty minutes

What: A manifest describes related containers, networks, health checks and volumes.

Why HRFlow needs this: One app/PostgreSQL/Caddy can be reproduced on the existing host.

## Tiny example

Compose service names resolve privately; localhost inside a container is that container.

## Common mistake

> **Pitfall:** Publishing database ports or using down -v can expose or erase data.

## Try and explain

- [ ] Inspect config and ps; DB must not have a public port binding.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
