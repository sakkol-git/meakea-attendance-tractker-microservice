# Backups and restores

## Read in ten to twenty minutes

What: A backup is a recoverable independent copy; a restore proves data can be used again.

Why HRFlow needs this: Host/disk/power failure can erase both app and same-disk dump.

## Tiny example

Nightly pg_dump -Fc, checksum, external copy, weekly scratch pg_restore and row/file verification.

## Common mistake

> **Pitfall:** A green cron exit or same-disk file alone is not a valid backup.

## Try and explain

- [ ] Restore into a clean named target and verify business state without touching production.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
