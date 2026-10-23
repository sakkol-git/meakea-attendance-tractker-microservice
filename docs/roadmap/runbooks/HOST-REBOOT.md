# Reboot the single host safely

## Status and prerequisites

First draft for implementation; **NOT RUN**. Referenced ops scripts are proposed, not installed today. Inspect/create them in named weekly steps first. Complete operational drills in Weeks 10–12; preserve the original detailed host runbook.

## Numbered procedure

1. Verify recent external copy and scratch restore, schedule a synthetic/private maintenance window, record current process/image state.
1. Reboot the confirmed host through its normal operator-controlled procedure; do not assume Docker Desktop starts before login/unlock.
1. Run reviewed ops/host-reboot-check.sh after the documented host startup prerequisite. Check PostgreSQL recovery, app readiness, Caddy/Tunnel and private Kuma.
1. Verify command retry does not duplicate session/approval, worker leases resume and missed nightly cron is detected.
1. Record unattended/awake/login limits honestly; one host remains a shared outage domain.

## Verification and recovery

- [ ] Actual target, command exit, safe output, operator and previous green release are recorded.
- [ ] No database/backup/private file was destroyed to obtain a green health check.
- [ ] Failure remains visible and the next action names a safe reproduction or recovery checkpoint.

[Host detail](../10-OPERATIONS-AND-RESTORE/HOST-RUNBOOK.md), [daily commands](../DAILY-COMMANDS.md), and [stuck protocol](../STUCK-PROTOCOL.md).
