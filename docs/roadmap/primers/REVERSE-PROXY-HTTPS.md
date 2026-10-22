# Reverse proxy and HTTPS

## Read in ten to twenty minutes

What: A proxy forwards requests; HTTPS authenticates/encrypts the browser connection.

Why HRFlow needs this: Caddy is the edge gateway; tunnel mode uses Cloudflare public TLS and private Caddy origin.

## Tiny example

External browser HTTPS goes through cloudflared to caddy:80 on a private network.

## Common mistake

> **Pitfall:** Claiming Caddy ACME works behind an unconfigured tunnel hides an invalid certificate path.

## Try and explain

- [ ] Check actual certificate and prove database/admin interfaces are unreachable.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
