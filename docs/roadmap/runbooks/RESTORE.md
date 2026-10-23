# Restore and prove usable data

## Status and prerequisites

Procedure defined; execution **NOT RUN**. Referenced ops scripts are proposed, not installed today. Inspect/create them in named weekly steps first. Complete operational drills in Weeks 10–12; preserve the original detailed host runbook.

## Numbered procedure

1. Check exact dump checksum, timestamp, PostgreSQL major, external mount and private-file manifest; failed mount/expired backup is not a pass.
1. Select a named scratch or clean host with no production volume; refuse a default production target. Disable live outbound delivery and old credential sessions.
1. Run reviewed ops/restore-scratch.sh weekly, ops/restore-clean.sh in Weeks 10 and 12; restore DB plus private files and approved secret references.
1. Verify all-tenant row counts, leave ledger totals, effective attendance, file checksums, RLS/grants, login and authorized export.
1. Record actual duration/data-loss window, operator and artifact IDs. Delete only explicitly disposable scratch resources after evidence; never overwrite production as a trial.

## Verification and recovery

- [ ] Actual target, command exit, safe output, operator and previous green release are recorded.
- [ ] No database/backup/private file was destroyed to obtain a green health check.
- [ ] Failure remains visible and the next action names a safe reproduction or recovery checkpoint.

[Host detail](../10-OPERATIONS-AND-RESTORE/HOST-RUNBOOK.md), [daily commands](../DAILY-COMMANDS.md), and [stuck protocol](../STUCK-PROTOCOL.md).

## Kafka recovery addition

Record owner database cut, Registry schemas/configuration and consumer replay position together. Restore business DB/private files to scratch first, then restore event schemas and rebuild projections from a verified replay point or owner snapshot. A retention gap fails the drill until snapshot bootstrap is proved. Never rewind a live production consumer or delete a topic as an automatic restore action.

[Script interface and safe target configuration](SCRIPT-INTERFACE.md) defines exact creation order, refusal guards and invocations. Missing prerequisite or report is a failed gate, not permission to guess a target.
