# Operate during internet loss

## Status and prerequisites

Procedure defined; execution **NOT RUN**. Referenced ops scripts are proposed, not installed today. Inspect/create them in named weekly steps first. Complete operational drills in Weeks 10–12; preserve the original detailed host runbook.

## Numbered procedure

1. Record app/private DB health and current approved route; remote Tunnel access depends on ISP/provider even while local data remains.
1. Use controlled test connectivity loss on private candidate, not arbitrary changes to user router/firewall or credential settings.
1. Run reviewed ops/recovery-check.sh and observe remote failure, retained commands and bounded provider retries.
1. Verify authenticated private LAN behavior if supported, then restore connectivity and replay stable keys once.
1. Record outage and user-visible limits; local Kuma cannot externally alert if its whole host/network is down. No HA claim.

## Verification and recovery

- [ ] Actual target, command exit, safe output, operator and previous green release are recorded.
- [ ] No database/backup/private file was destroyed to obtain a green health check.
- [ ] Failure remains visible and the next action names a safe reproduction or recovery checkpoint.

[Host detail](../10-OPERATIONS-AND-RESTORE/HOST-RUNBOOK.md), [daily commands](../DAILY-COMMANDS.md), and [stuck protocol](../STUCK-PROTOCOL.md).

[Script interface and safe target configuration](SCRIPT-INTERFACE.md) defines exact creation order, refusal guards and invocations. Missing prerequisite or report is a failed gate, not permission to guess a target.
