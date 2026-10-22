# Private attachments

## Read in ten to twenty minutes

What: Files need bounded upload, generated keys, validated content and authorized reads.

Why HRFlow needs this: Leave/request evidence can contain sensitive employee information.

## Tiny example

Use a private disk path; verify size/type/checksum; check participant before download.

## Common mistake

> **Pitfall:** An unguessable URL or filename is not authorization.

## Try and explain

- [ ] Attempt traversal, oversized file and other-tenant download; all fail safely.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
