# Roll back a failed deployment

## Status and prerequisites

First draft for implementation; **NOT RUN**. Referenced ops scripts are proposed, not installed today. Inspect/create them in named weekly steps first. Complete operational drills in Weeks 10–12; preserve the original detailed host runbook.

## Numbered procedure

1. Inspect previous/candidate image digests and migration compatibility before touching runtime. Preserve failed release logs without secrets.
1. Choose the previously recorded compatible release manifest; no floating latest tag or blind SQL down migration.
1. Run reviewed ops/rollback-check.sh against the explicit private target; switch only image/config when schema permits.
1. Verify authenticated smoke, new data retained, audit/outbox receipts intact and readiness recovered.
1. If schema is incompatible, stop automatic rollback and follow tested forward repair or separately authorized restore plan; record incident.

## Verification and recovery

- [ ] Actual target, command exit, safe output, operator and previous green release are recorded.
- [ ] No database/backup/private file was destroyed to obtain a green health check.
- [ ] Failure remains visible and the next action names a safe reproduction or recovery checkpoint.

[Host detail](../10-OPERATIONS-AND-RESTORE/HOST-RUNBOOK.md), [daily commands](../DAILY-COMMANDS.md), and [stuck protocol](../STUCK-PROTOCOL.md).
