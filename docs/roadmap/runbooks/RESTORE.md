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

## Offline release artifacts

An image digest or Git commit ID is a reference, not the image/source itself. Before a clean-host drill, the encrypted external backup must contain the verified release image archive and Git source bundle (or a tested independently available registry/repository), plus config/model-version inventory. The local-only finish line uses the external offline archive; no cloud repository availability is assumed.

Save immutable release images and the reviewed Git bundle once per release, checksum them, and reference that artifact set from nightly data manifests instead of duplicating it every night. In scratch/clean recovery verify the bundle, load the images, compare their IDs to the release manifest, then restore DB/private files/roles/schema/replay metadata with live delivery disabled. Test access to the recovery keys separately; do not place secret values in Git or reports.

Use the actual allowlisted image references and protected release directory chosen by the script interface; no arbitrary user-supplied shell/image arguments. git bundle verify and docker image load must succeed on the isolated target before the clean-host application smoke. Data-only restore does not claim a complete lost-host recovery.
