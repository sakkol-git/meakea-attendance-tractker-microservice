# Operations script interface

## Status and creation order

This is the required interface for proposed scripts, not installed application code. Week 1 creates private manifest, deploy/smoke and disposable backup/restore before Friday invocation. Week 2 adds confirmed host, external mount, private files/schema/replay inventory and production refusal tests. Week 10 creates/completes rollback, clean restore, reboot, rotation and failure drills. Execution is **NOT RUN** until each script has real isolated reports.

## Common target and safety contract

Each script reads an operator-controlled ignored configuration file selected through HRFLOW_OPS_CONFIG, then validates HRFLOW_ROOT, HRFLOW_TARGET_ID, HRFLOW_MODE and COMPOSE_PROJECT_NAME before touching a host. Mode is explicitly disposable in Week 1 and production from Week 2; missing mode/target always refuses. Do not source an untrusted downloaded file. Credentials use protected mounts/config and never command-line echo, Git or reports.

The reviewed config records actual manifest, service names, DB/roles, image digests, external mount/backup path, private files, Registry schema inventory and scratch target. Backup uses a separate all-tenant read role; business runtime roles remain non-owner/non-BYPASSRLS. Confirm actual names before running; no fictitious app container is assumed.

Scratch restore requires a unique explicit HRFLOW_SCRATCH_DB matching the allowed hrflow_restore_ prefix, a separate project/volume and notifications disabled. Refuse source DB/project/volume, already-existing target without inspection and production-looking names. Clean restore additionally requires a fresh isolated VM/container/host and matching PostgreSQL major. No script uses down -v, prune or production overwrite as automatic recovery.

## Script responsibilities and exact invocations

| Script | Created/completed | Success must prove | Failure behavior |
| --- | --- | --- | --- |
| ops/deploy.sh | W1/W2/W10 | Pinned candidate, prior manifest, additive migration/readiness/smoke | Nonzero, preserve prior compatible release |
| ops/selfhost-smoke.sh | W1 then feature weeks | Readiness and current authenticated/denied route assertions | Nonzero on missing assertion, timeout or wrong status |
| ops/backup.sh | W1 disposable/W2 external/W10 schedule | Custom dump, private-file consistency, checksum/schema/replay manifest and mounted external copy | Refuse missing mount, retain last valid backup, prune only after new copy verifies |
| ops/restore-scratch.sh | W1/W2 then every Friday | Isolated rows/ledger/files/grants/auth and original DB unchanged | Refuse wrong/corrupt target or archive; no green result from listing only |
| ops/restore-clean.sh | W10/W12 | Fresh-target release recovery, checksums and measured time/data loss | Missing clean target blocks gate; scratch is not falsely called clean-machine recovery |
| ops/rollback-check.sh | W10/W12 | Previous compatible digest/config, data retained | No automatic SQL downgrade; incompatible schema requires forward repair |
| ops/host-reboot-check.sh | W10 | Documented login/awake/daemon startup and workload recovery | Does not reboot by itself; missing startup condition is a failed drill |
| ops/disk-check.sh | W10 | Threshold override, bounded logs and admission alert | Never fill disk or delete volumes |
| ops/recovery-check.sh | W10 | Isolated internet loss, bounded timeout and stable-key reconciliation | Never change arbitrary host/router network |
| ops/broker-recovery-check.sh | W10 | Schema/replay recovery and retention-gap snapshot | Never rewind/delete live topics automatically |

Commands below run from repository root after the named creation step and explicit target config. No target is inferred from an IP or the current laptop.

```bash
bash ops/deploy.sh
bash ops/selfhost-smoke.sh
bash ops/backup.sh
bash ops/restore-scratch.sh
bash ops/restore-clean.sh
bash ops/rollback-check.sh
bash ops/host-reboot-check.sh
bash ops/disk-check.sh
bash ops/recovery-check.sh
bash ops/broker-recovery-check.sh
```

## Evidence and undo

Use set -euo pipefail, quoted variables, bounded waits, explicit exit propagation and refusal tests. A script syntax check uses bash -n but does not execute a drill. Record operator, target alias, exact command/exit, artifact IDs, safe result and timing. A backup is valid only after its restore passes. git revert HEAD reverses code/config only; applied SQL/data follows forward repair or the separately reviewed restore runbook. All six runbooks share this contract.

## Offline release artifacts

An image digest or Git commit ID is a reference, not the image/source itself. Before a clean-host drill, the encrypted external backup must contain the verified release image archive and Git source bundle (or a tested independently available registry/repository), plus config/model-version inventory. The local-only finish line uses the external offline archive; no cloud repository availability is assumed.

Save immutable release images and the reviewed Git bundle once per release, checksum them, and reference that artifact set from nightly data manifests instead of duplicating it every night. In scratch/clean recovery verify the bundle, load the images, compare their IDs to the release manifest, then restore DB/private files/roles/schema/replay metadata with live delivery disabled. Test access to the recovery keys separately; do not place secret values in Git or reports.

Use the actual allowlisted image references and protected release directory chosen by the script interface; no arbitrary user-supplied shell/image arguments. git bundle verify and docker image load must succeed on the isolated target before the clean-host application smoke. Data-only restore does not claim a complete lost-host recovery.
