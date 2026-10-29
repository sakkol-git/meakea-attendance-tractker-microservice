# Week 10 partial candidate evidence

Date: 2026-10-28. Owner week: 10 partial candidate; verified roadmap position: W02-S26. This is a partial local candidate for the 2026-12-28 to 2027-01-01 week, not a Week 10 gate result. Root, web and mobile changes are uncommitted in separate repositories. The intended Acer Nitro 5 and encrypted external backup disk are unavailable for the required deploy and restore gate.

## Accepted intent and current behavior

- Operations policy harnesses: Shell scripts in `ops/` exercise deterministic logic for deployment rollback (`ops/rollback-check.sh`), backup checksum and retention pruning (`ops/backup-check.sh`), clean-host target rejection and archive validation (`ops/restore-clean.sh`), and non-destructive disk headroom and log bounds (`ops/disk-check.sh`).
- Connectivity prerequisites: Shell scripts verify fail-closed negative behavior and reachability prerequisites for JWT signing keypairs with mode-0600 verification (`ops/rotate-jwt.sh`), database credentials (`ops/rotate-database.sh`), Tailscale tunnel status (`ops/rotate-tunnel.sh`), mock notification sinks (`ops/restore-client-check.sh`), reporting API endpoints (`ops/recovery-check.sh`), PostgreSQL crash recovery targets (`ops/database-recovery-check.sh`), Kafka brokers (`ops/broker-recovery-check.sh`), and target host Docker sockets (`ops/host-reboot-check.sh`).
- Live drills (**NOT RUN** / **BLOCKED**): Live host reboot drills, container image rollbacks, credential rotations, WAL transaction recovery, stream replay, clean-machine restore, and physical external backup mounting remain strictly **NOT RUN** or **BLOCKED** without access to the designated Acer Nitro 5 self-host and encrypted external backup disk.

## Stone 1 verification (W10 Stone 1, W10-S01–S06)

- Steps: `W10-S01` to `W10-S03` (verify host reboot prerequisites and Docker readiness) and `W10-S04` to `W10-S06` (roll back a failed deployment).
- Targets: `ops/host-reboot-check.sh`, `ops/rollback-check.sh`, `scripts/acceptance/w10.sh`.
- Verified behaviors: Static negative check verifies invalid Docker sockets are rejected and schema migration downgrades are prevented; policy harness validates image rollback logic while live container rollback remains **NOT RUN**.
- Execution limitations: Harness uses simulated socket paths and local policy validation; live reboot and container rollbacks remain **NOT RUN** without target host.

## Stone 2 verification (W10 Stone 2, W10-S07–S12)

- Steps: `W10-S07` to `W10-S09` (rotate JWT signing keys) and `W10-S10` to `W10-S12` (rotate database runtime credentials).
- Targets: `ops/rotate-jwt.sh`, `ops/rotate-database.sh`, `scripts/acceptance/w10.sh`.
- Verified behaviors: Harness verifies key rotation logic using mode-0600 temporary files and public key verification without storing binary signatures in shell variables; database check verifies negative rejection of invalid credentials.
- Execution limitations: Real key rotation, token revocation, and live PostgreSQL credential rotation remain **BLOCKED** without operator key store and live PostgreSQL server.

## Stone 3 verification (W10 Stone 3, W10-S13–S18)

- Steps: `W10-S13` to `W10-S15` (rotate tunnel credentials) and `W10-S16` to `W10-S18` (nightly backup retention and mounts).
- Targets: `ops/rotate-tunnel.sh`, `ops/backup-check.sh`, `scripts/acceptance/w10.sh`.
- Verified behaviors: Controlled unavailable port checks fail closed; backup checksum and retention pruning policy harness validates archive integrity and pruning rules.
- Execution limitations: Live Tailscale credential rotation and physical encrypted backup mount remain **BLOCKED** / **NOT RUN** without target host and external disk.

## Stone 4 verification (W10 Stone 4, W10-S19–S24)

- Steps: `W10-S19` to `W10-S21` (clean machine restore) and `W10-S22` to `W10-S24` (phone client recovery).
- Targets: `ops/restore-clean.sh`, `ops/restore-client-check.sh`, `scripts/acceptance/w10.sh`.
- Verified behaviors: Clean-host policy harness verifies production target rejection and checksum validation; notification sink negative check fails closed against controlled unavailable endpoint.
- Execution limitations: Physical clean-machine restore and mobile phone recovery remain **NOT RUN** without dedicated target host and devices.

## Stone 5 verification (W10 Stone 5, W10-S25–S30)

- Steps: `W10-S25` to `W10-S27` (disk headroom and log bounds) and `W10-S28` to `W10-S30` (network loss and idempotency).
- Targets: `ops/disk-check.sh`, `ops/recovery-check.sh`, `scripts/acceptance/w10.sh`.
- Verified behaviors: Disk policy harness tests non-destructive warning thresholds and log rotation limits; API offline safety check verifies unreachable endpoint fails closed.
- Execution limitations: Live filesystem inspection and live network partition retry recovery drills remain **NOT RUN** without target host and running services.

## Stone 6 verification (W10 Stone 6, W10-S31–S36)

- Steps: `W10-S31` to `W10-S33` (scratch database crash recovery) and `W10-S34` to `W10-S36` (broker schema and replay boundaries).
- Targets: `ops/database-recovery-check.sh`, `ops/broker-recovery-check.sh`, `scripts/acceptance/w10.sh`.
- Verified behaviors: Controlled unavailable loopback endpoints fail closed for database and Kafka checks (connectivity-negative evidence only).
- Execution limitations: Live PostgreSQL WAL crash recovery and live Kafka retention gap replay drills remain **BLOCKED** / **NOT RUN** without running database and broker servers.

## Stone 7 verification (W10 Stone 7, W10-S37–S39)

- Steps: `W10-S37` (reproduce earliest failed check), `W10-S38` (recheck client and contract negatives), and `W10-S39` (review carry-forward and reserve).
- Targets: `scripts/acceptance/w10.sh`, `contracts/openapi.json`, `docs/validation/W10-EVIDENCE.md`, git diff across all three repositories.
- Verified behaviors: All 12 negative/static checks pass; 4 policy harnesses pass in prerequisite mode with 8 live prerequisites honestly preserved as **BLOCKED**; foundation scope passes 177 OpenAPI operations and 6 contract tests; tooling scope passes 23 tests; docs-lint passes all Markdown files with zero warnings; git diff checks across root, web, and mobile are clean.
- Diff check: `git diff --check && git -C web diff --check && git -C mobile diff --check` exited 0.
- Contract validation: `python3 scripts/verify-project.py --scope foundation` exited 0 (177 operations, 6 contract tests).
- Tooling verification: `python3 scripts/verify-project.py --scope tooling` exited 0 (23 tests passed in 1.959s).

## Stone 8 assessment & physical blockers (W10 Stone 8, W10-S40–S42)

- Steps: `W10-S40` (redeploy checked service candidate via `ops/deploy.sh`); `W10-S41` (restore release backup in scratch via `ops/restore-scratch.sh`); `W10-S42` (decide gate).
- Physical blocker - deploy: `ops/deploy.sh` requires the dedicated Acer Nitro 5 private host, which remains physically unavailable in this development environment.
- Physical blocker - restore: `ops/restore-scratch.sh` requires the encrypted external backup disk, which remains physically disconnected.
- Gate decision: `BLOCKED`. The Week 10 release gate remains **NOT RUN**. Verified roadmap position remains `W02-S26` in `docs/roadmap/YOU-ARE-HERE.md`. No weekly gate is claimed passed.

## Executed checks

| Check | Result | Limit |
| --- | --- | --- |
| Operations static checks, `bash scripts/acceptance/w10.sh static` | Exit 0 (12 checks passed); verifies negative policies, downgrade rejections, controlled unavailable endpoints, and bound limits | Local shell execution |
| Operations prerequisite checks, `bash scripts/acceptance/w10.sh prerequisite` | Exit 2 (4 **PASS**, 8 **BLOCKED**, 0 **NOT RUN**; INCOMPLETE / **BLOCKED**); policy harnesses pass, live infrastructure prerequisites blocked | Local execution without target host |
| Operations live checks, `bash scripts/acceptance/w10.sh live` | Exit 2 (4 **PASS**, 8 **BLOCKED**; live drills remain **NOT RUN**) | Live drills unexecuted |
| Foundation scope, `python3 scripts/verify-project.py --scope foundation` | Exit 0; 177-operation OpenAPI validation, 6 contract tests, docs lint, and diff check passed | Automated local baseline only |
| Tooling scope, `python3 scripts/verify-project.py --scope tooling` | Exit 0; 23 tests passed across MCP server, test runner, and workspace traversal | Local Python virtual environment |

## Boundary review

- Operations scripts: `ops/` scripts separate static/negative bounds enforcement, prerequisite connectivity checks, and honest policy harnesses; they do not claim live infrastructure execution.
- Missing live targets: Target host (Acer Nitro 5), Docker daemon, PostgreSQL server, Kafka cluster, Tailscale daemon, and external encrypted disks were not executed against.
- Documentation: `docs-lint.py` passes all Markdown files with 0 errors and 0 warnings.

## Disposition

`PARTIAL`

Operations scripts provide deterministic local harness validation of runbook constraints and negative cases, cleanly separating policy harnesses, connectivity prerequisites, and live drills. The Acer Nitro 5 host and encrypted external backup disk are unavailable. The Week 10 release gate remains **NOT RUN**, and the verified roadmap position remains W02-S26.
