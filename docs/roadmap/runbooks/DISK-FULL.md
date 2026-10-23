# Detect and recover low disk without destructive cleanup

## Status and prerequisites

Procedure defined; execution **NOT RUN**. Referenced ops scripts are proposed, not installed today. Inspect/create them in named weekly steps first. Complete operational drills in Weeks 10–12; preserve the original detailed host runbook.

## Numbered procedure

1. Inspect df output, Docker logs/volume sizes, backup/export retention and minimum headroom; do not fill the production disk to test alerts.
1. Run reviewed ops/disk-check.sh with a test threshold override, proving alert/stop-admission behavior without consuming disk.
1. Pause new heavy exports/admission before durability fails. Review owner-approved archive/retention cleanup; never prune volumes or delete arbitrary files.
1. Verify bounded logs, valid external backup, database readiness and uncertain command reconciliation with same key after space is restored.
1. Record capacity trend and fix source of growth. Failed commit must not have returned success.

## Verification and recovery

- [ ] Actual target, command exit, safe output, operator and previous green release are recorded.
- [ ] No database/backup/private file was destroyed to obtain a green health check.
- [ ] Failure remains visible and the next action names a safe reproduction or recovery checkpoint.

[Host detail](../10-OPERATIONS-AND-RESTORE/HOST-RUNBOOK.md), [daily commands](../DAILY-COMMANDS.md), and [stuck protocol](../STUCK-PROTOCOL.md).

[Script interface and safe target configuration](SCRIPT-INTERFACE.md) defines exact creation order, refusal guards and invocations. Missing prerequisite or report is a failed gate, not permission to guess a target.
