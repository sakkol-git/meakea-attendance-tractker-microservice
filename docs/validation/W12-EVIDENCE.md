# Week 12 partial candidate evidence

Date: 2026-10-28. Owner week: 12 partial candidate; verified roadmap position: W02-S26. This is a partial local candidate for the 2027-01-11 to 2027-01-15 week, not a Week 12 gate result. Root, web and mobile changes are uncommitted in separate repositories. The intended Acer Nitro 5 and encrypted external backup disk are unavailable for the required deploy and restore gate.

## Accepted intent and current behavior

- Release manifest check: Shell script validates exact immutable image digest patterns (`@sha256:[0-9a-f]{64}`) across all six services, records root, web, and mobile git revisions and dirty states, tracks source migration inventory, and blocks release identity when dirty content is not represented in an immutable commit (`ops/release-manifest-check.sh`).
- Backend module acceptance: Runner executes `./mvnw --batch-mode clean test -pl "services/$service"` from clean sources across all six Java backend microservices (`identity-org`, `attendance`, `leave`, `request`, `notification`, `reporting`), verifying all 204 tests green in Testcontainers PostgreSQL 18.6; omitted scopes (web, mobile, contract sync, live Kafka broker, and e2e) are explicitly reported as **NOT RUN** (`ops/full-matrix-acceptance-check.sh`).
- Safe release restore: Shell script requires explicit isolated scratch database with accepted disposability marker or created-by-run proof (name matching alone is rejected), detects SQL versus custom PGDMP dump formats, verifies sha256 checksums, and protects source database from modification (`ops/release-restore-check.sh`).
- Safe rollback without schema tampering: Shell script validates policy that deployment rollback strictly rejects schema migration downgrades (`ops/deploy-rollback-check.sh`).
- Timed demonstration and offline fallback: Rejects manufactured offline evidence records; strictly requires an existing artifact manifest with file paths, sha256 hashes, dates, and git revisions, reporting **NOT RUN** when absent (`ops/demonstration-check.sh`).
- Operational limits and support handover: Handover validation verifies documented single-host limits and operator duties, strictly rejecting unverified high-availability claims (`ops/demonstration-check.sh`).
- Demo rehearsal sanitization: Scans demo recordings for leaked tokens or private IP addresses (`ops/freeze-scorecard-check.sh`).
- 14 Quality gates scorecard audit: Rejects self-generated PASSED scorecards; strictly requires an existing owned scorecard and recording, verifying physical Acer and external disk gates remain **BLOCKED** (`ops/freeze-scorecard-check.sh`).

## Stone 1 verification (W12 Stone 1, W12-S01–S03)

- Steps: `W12-S01` to `W12-S03` (release candidate manifest verification and image pinning).
- Targets: `ops/release-manifest-check.sh`, `scripts/acceptance/w12.sh`.
- Verified behaviors: Validates exact `@sha256:[0-9a-f]{64}` image digests across all six images, records root, web, and mobile git commit shas and dirty states, records source migration inventory, and strictly blocks release identity when dirty repository content is present.
- Execution limitations: Release identity is **BLOCKED** while uncommitted working changes remain in root, web, and mobile repositories.

## Stone 2 verification (W12 Stone 2, W12-S04–S06)

- Steps: `W12-S04` to `W12-S06` (backend module acceptance execution across microservices).
- Targets: `ops/full-matrix-acceptance-check.sh`, `scripts/acceptance/w12.sh`.
- Verified behaviors: Runs serialized `clean test` from clean sources across all six microservices (`identity-org` [61 tests], `attendance` [22 tests], `leave` [23 tests], `request` [19 tests], `notification` [20 tests], `reporting` [59 tests]); all 204 backend tests pass in Testcontainers PostgreSQL 18.6.
- Execution limitations: Backend Java module verification only; web, mobile, contract sync, broker transport, and e2e matrix scopes are omitted and reported as **NOT RUN**.

## Stone 3 verification (W12 Stone 3, W12-S07–S12)

- Steps: `W12-S07` to `W12-S09` (isolated scratch restore) and `W12-S10` to `W12-S12` (archive integrity verification).
- Targets: `ops/release-restore-check.sh`, `scripts/acceptance/w12.sh`.
- Verified behaviors: Rejects target databases without accepted disposability marker or created-by-run proof; distinguishes SQL vs custom PGDMP dump format; reports **BLOCKED** when backup archive is not provided.
- Execution limitations: Physical external disk restore remains **BLOCKED** / **NOT RUN** without physical backup archive and media.

## Stone 4 verification (W12 Stone 4, W12-S13–S18)

- Steps: `W12-S13` to `W12-S15` (failed-deploy rollback) and `W12-S16` to `W12-S18` (schema migration immutability).
- Targets: `ops/deploy-rollback-check.sh`, `scripts/acceptance/w12.sh`.
- Verified behaviors: Simulated policy validation proves that rollback procedures reject applied Flyway schema tampering and restore compatible image references.
- Execution limitations: Policy simulation only; live Compose deployment rollback remains **NOT RUN**.

## Stone 5 verification (W12 Stone 5, W12-S19–S24)

- Steps: `W12-S19` to `W12-S21` (timed demonstration script) and `W12-S22` to `W12-S24` (operational limits and support handover).
- Targets: `ops/demonstration-check.sh`, `scripts/acceptance/w12.sh`.
- Verified behaviors: Negative check rejects manufactured live success during network partitions and rejects false HA handover claims; regression check strictly requires existing artifact manifest and handover file, reporting **NOT RUN** when absent.
- Execution limitations: Timed demonstration verification remains **NOT RUN** without existing recorded demo manifest and handover file.

## Stone 6 verification (W12 Stone 6, W12-S25–S28)

- Steps: `W12-S25` to `W12-S27` (demo asset sanitization) and `W12-S28` (14 Q release scorecard audit).
- Targets: `ops/freeze-scorecard-check.sh`, `scripts/acceptance/w12.sh`.
- Verified behaviors: Negative check rejects leaked tokens, leaked private IPs, and fake physical gate passes; regression check strictly requires an existing owned scorecard and recording, reporting **BLOCKED** when absent.
- Execution limitations: Release freeze scorecard audit remains **BLOCKED** without existing owned scorecard and demo recording files.

## Stone 7 verification (W12 Stone 7, W12-S29–S31)

- Steps: `W12-S29` (reproduce earliest failed check), `W12-S30` (recheck client and contract negatives), and `W12-S31` (review carry-forward and reserve).
- Targets: `scripts/acceptance/w12.sh`, `contracts/openapi.json`, `docs/validation/W12-EVIDENCE.md`, git diff across all three repositories.
- Verified behaviors: Static suite passes all 6 checks; regression runner executes serialized backend matrix and aggregates status honestly (2 **PASS**, 3 **BLOCKED**, 1 **NOT RUN**; exit code 2); foundation scope passes 177 OpenAPI operations and 6 contract tests; tooling scope passes 23 tests; web unit tests pass 93 tests; git diff checks across root, web, and mobile are clean.
- Diff check: `git diff --check && git -C web diff --check && git -C mobile diff --check` exited 0.
- Contract validation: `python3 scripts/verify-project.py --scope foundation` exited 0 (177 operations, 6 contract tests).
- Tooling verification: `python3 scripts/verify-project.py --scope tooling` exited 0 (23 tests passed in 2.008s).
- Client unit tests: `npm --prefix web run test -- --run` exited 0 (10 data, 2 config, 93 unit tests passed across 17 files).

## Stone 8 assessment & physical blockers (W12 Stone 8, W12-S32–S34)

- Steps: `W12-S32` (redeploy checked service candidate via `ops/deploy.sh`); `W12-S33` (restore release backup in scratch via `ops/restore-scratch.sh`); `W12-S34` (decide gate).
- Physical blocker - deploy: `ops/deploy.sh` requires the dedicated Acer Nitro 5 private host, which remains physically unavailable in this development environment.
- Physical blocker - restore: `ops/restore-scratch.sh` requires the encrypted external backup disk, which remains physically disconnected.
- Gate decision: `BLOCKED`. The Week 12 release gate remains **NOT RUN**. Verified roadmap position remains `W02-S26` in `docs/roadmap/YOU-ARE-HERE.md`. No weekly gate is claimed passed.

## Executed checks

| Check | Result | Limit |
| --- | --- | --- |
| Acceptance static checks, `bash scripts/acceptance/w12.sh static` | Exit 0 (6 **PASS** checks); negative validation of image digests, missing suites, unverified scratch targets, schema tampering, false HA, and secret leaks | Local shell execution |
| Acceptance regression checks, `bash scripts/acceptance/w12.sh regression` | Exit 2 (2 **PASS**, 3 **BLOCKED**, 1 **NOT RUN**; INCOMPLETE / **BLOCKED**); serialized backend matrix passes, manifest/restore/scorecard blocked, demo not run | Local execution without target host and media |
| Serialized backend acceptance matrix, `bash ops/full-matrix-acceptance-check.sh regression` | Exit 0 (204 tests passed across 6 services: identity-org 61, attendance 22, leave 23, request 19, notification 20, reporting 59) | Clean sources in Testcontainers PostgreSQL 18.6 |
| Web unit tests, `npm --prefix web run test -- --run` | Exit 0 (93 tests passed across 17 test files); auth, remote business, csv, geofence, and navigation suites green | Local Node.js test runner |
| Foundation scope, `python3 scripts/verify-project.py --scope foundation` | Exit 0; 177-operation OpenAPI validation, 6 contract tests, docs lint, and diff check passed | Automated local baseline only |
| Tooling scope, `python3 scripts/verify-project.py --scope tooling` | Exit 0; 23 tests passed across MCP server, test runner, and workspace traversal | Local Python virtual environment |

## Boundary review

- Acceptance matrix: Full serialized acceptance matrix executed across all six backend services from clean sources, passing 204 tests in Testcontainers PostgreSQL 18.6. Omitted scopes (web, mobile, contract sync, live broker transport, e2e) are explicitly reported as **NOT RUN**.
- Release manifest: Requires exact `@sha256:[0-9a-f]{64}` image digests across all six images; records root, web, and mobile revisions and dirty states; blocks release identity when dirty working changes are present.
- Release restore: Requires explicit disposability marker or created-by-run proof; detects SQL vs custom PGDMP dump format; reports **BLOCKED** when archive is missing.
- Demonstration & Scorecard: Rejects manufactured offline records and self-generated PASSED scorecards; strictly requires existing owned files or reports **NOT RUN** / **BLOCKED**.
- Documentation: `docs-lint.py` passes all Markdown files with 0 errors and 0 warnings.

## Disposition

`PARTIAL`

Foundation contracts, tooling, client unit tests, and the serialized backend acceptance matrix (204 tests across all 6 microservices) pass cleanly. However, release manifest identity is **BLOCKED** by uncommitted dirty changes, release restore and scorecard audits remain **BLOCKED** awaiting authentic archives and scorecards, timed demo remains **NOT RUN**, and physical host deployment and external restore gates remain blocked. The Week 12 release gate remains **NOT RUN**, and the verified roadmap position remains W02-S26.
