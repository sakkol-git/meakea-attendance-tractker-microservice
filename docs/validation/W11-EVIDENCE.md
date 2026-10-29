# Week 11 partial candidate evidence

Date: 2026-10-28. Owner week: 11 partial candidate; verified roadmap position: W02-S26. This is a partial local candidate for the 2027-01-04 to 2027-01-08 week, not a Week 11 gate result. Root, web and mobile changes are uncommitted in separate repositories. The intended Acer Nitro 5 and encrypted external backup disk are unavailable for the required deploy and restore gate.

## Accepted intent and current behavior

- Multi-tenant boundary isolation: Server derives tenant context strictly from authenticated tokens; client-supplied tenant headers (`X-Tenant-Id`) and JSON body fields are ignored in local policy logic simulation (`ops/tenant-boundary-check.sh`). Real JWT, gateway, and service tenant isolation drills remain **NOT RUN**.
- Concurrency invariants: Sequential policy logic verifies balance reservation and interval overlap prevention without database races (`ops/concurrency-invariants-check.sh`). Wrapped 409/422 return codes are removed; live concurrent database race testing remains **NOT RUN**.
- Client credential hygiene: Web client unit tests verify credential purging and token redaction (`npm --prefix web run test -- --run`).
- Kafka stream replay and DLQ quarantine: Connectivity-negative check verifies closed broker endpoint fails closed (`ops/stream-replay-dlq-check.sh`). Connectivity failure does not prove stream replay ordering or dead-letter quarantine behavior.
- Pilot consent and readiness: Real pilot activation requires authentic owned evidence files (`HRFLOW_PILOT_CONSENT_FILE`, `HRFLOW_BACKUP_VERIFIED_RECORD`, `HRFLOW_TLS_VERIFIED_RECORD`); caller-provided strings are rejected and absent evidence results in **BLOCKED** (`ops/pilot-consent-check.sh`). Real 10-user pilot journey requires isolated service endpoints and user fixtures; an ID-generation loop is rejected and the journey remains **NOT RUN** without real targets (`ops/pilot-readiness-check.sh`).
- Load benchmark: `ops/load-benchmark-check.sh` issues real curl requests with contract-valid `CheckInRequest` fields (latitude, longitude, accuracyMeters, deviceModel) and RFC-4122 UUID-v4 idempotency keys. One token represents one actor; a single-token run is labeled "100 concurrent commands from one actor", while 100 independent actors requires an explicit 100-token fixture (`HRFLOW_CLIENT_TOKENS_FILE`). If independent actors are required and absent, the benchmark strictly exits 2 **NOT RUN**. In the absence of target service credentials, the benchmark is strictly **NOT RUN** (exit code 2).

## Stone 1 verification (W11 Stone 1, W11-S01–S03)

- Steps: `W11-S01` to `W11-S03` (two-tenant route matrix and forged tenant header/body hardening).
- Targets: `ops/tenant-boundary-check.sh`, `scripts/acceptance/w11.sh`.
- Verified behaviors: Policy logic verifies that tenant headers and body properties are ignored in favor of derived token claims.
- Execution limitations: Policy simulation on local records only; did not execute against running microservice endpoints.

## Stone 2 verification (W11 Stone 2, W11-S04–S06)

- Steps: `W11-S04` to `W11-S06` (race approvals, check-in, and balance reservations).
- Targets: `ops/concurrency-invariants-check.sh`, `scripts/acceptance/w11.sh`.
- Verified behaviors: Sequential policy logic validates conflict detection on overlapping intervals and balance depletion without wrapped HTTP shell return codes.
- Execution limitations: Sequential script simulation only; did not execute concurrent database transaction races against live PostgreSQL.

## Stone 3 verification (W11 Stone 3, W11-S07–S12)

- Steps: `W11-S07` to `W11-S09` (device credential boundaries) and `W11-S10` to `W11-S12` (Kafka stream replay and poison pill quarantine).
- Targets: `ops/stream-replay-dlq-check.sh`, `scripts/acceptance/w11.sh`, web unit tests.
- Verified behaviors: 93 web unit tests pass; connectivity-negative check verifies unopened broker port fails closed.
- Execution limitations: Stream replay ordering and dead-letter queue quarantine remain unverified without a running Kafka cluster; connectivity failure provides connectivity-negative evidence only.

## Stone 4 verification (W11 Stone 4, W11-S13–S18)

- Steps: `W11-S13` to `W11-S15` (measure 100-client attendance load) and `W11-S16` to `W11-S18` (network and device acceptance).
- Targets: `ops/load-benchmark-check.sh`, `scripts/acceptance/w11.sh`.
- Verified behaviors: Benchmark generates real UUID-v4 idempotency keys, real `CheckInRequest` fields, and nearest-rank percentiles. One token represents one actor; run is labeled as 100 concurrent commands from one actor unless an explicit 100-token fixture is supplied.
- Execution limitations: In the absence of running attendance infrastructure and valid credentials, the benchmark is strictly **NOT RUN** (exit code 2). No synthetic passes are claimed.

## Stone 5 verification (W11 Stone 5, W11-S19–S24)

- Steps: `W11-S19` to `W11-S21` (review unresolved policy and gate pilot consent) and `W11-S22` to `W11-S24` (fix smallest failing reproduction).
- Targets: `ops/pilot-consent-check.sh`, `scripts/acceptance/w11.sh`.
- Verified behaviors: Synthetic demo mode policy passes; live pilot consent strictly requires authentic owned evidence files (`HRFLOW_PILOT_CONSENT_FILE`, `HRFLOW_BACKUP_VERIFIED_RECORD`, `HRFLOW_TLS_VERIFIED_RECORD`) and exits **BLOCKED** when caller-provided strings or absent inputs are encountered.
- Execution limitations: Live pilot consent remains **BLOCKED** without verified external backup and TLS records.

## Stone 6 verification (W11 Stone 6, W11-S25–S28)

- Steps: `W11-S25` to `W11-S27` (rehearse limited pilot story with 10 synthetic users) and `W11-S28` (score quality gates honestly).
- Targets: `ops/pilot-readiness-check.sh`, `scripts/acceptance/w11.sh`.
- Verified behaviors: Release blockers check enforces physical gate controls; 10-user pilot journey strictly requires real isolated endpoints and fixtures, reporting **NOT RUN** when ID-only loops are avoided.
- Execution limitations: Multi-user live journey remains **NOT RUN** without isolated running services and fixture files.

## Stone 7 verification (W11 Stone 7, W11-S29–S31)

- Steps: `W11-S29` (reproduce earliest failed check), `W11-S30` (recheck client and contract negatives), and `W11-S31` (review carry-forward and reserve).
- Targets: `scripts/acceptance/w11.sh`, `contracts/openapi.json`, `docs/validation/W11-EVIDENCE.md`, git diff across all three repositories.
- Verified behaviors: Static negative suite passes all 6 checks; live suite preserves all blockers (2 **PASS**, 2 **BLOCKED**, 2 **NOT RUN**; exit code 2); foundation scope passes 177 OpenAPI operations and 6 contract tests; tooling scope passes 23 tests; web unit tests pass 93 tests; git diff checks across root, web, and mobile are clean.
- Diff check: `git diff --check && git -C web diff --check && git -C mobile diff --check` exited 0.
- Contract validation: `python3 scripts/verify-project.py --scope foundation` exited 0 (177 operations, 6 contract tests).
- Tooling verification: `python3 scripts/verify-project.py --scope tooling` exited 0 (23 tests passed in 2.009s).

## Stone 8 assessment & physical blockers (W11 Stone 8, W11-S32–S34)

- Steps: `W11-S32` (redeploy checked service candidate via `ops/deploy.sh`); `W11-S33` (restore release backup in scratch via `ops/restore-scratch.sh`); `W11-S34` (decide gate).
- Physical blocker - deploy: `ops/deploy.sh` requires the dedicated Acer Nitro 5 private host, which remains physically unavailable in this development environment.
- Physical blocker - restore: `ops/restore-scratch.sh` requires the encrypted external backup disk, which remains physically disconnected.
- Gate decision: `BLOCKED`. The Week 11 release gate remains **NOT RUN**. Verified roadmap position remains `W02-S26` in `docs/roadmap/YOU-ARE-HERE.md`. No weekly gate is claimed passed.

## Executed checks

| Check | Result | Limit |
| --- | --- | --- |
| Hardening static checks, `bash scripts/acceptance/w11.sh static` | Exit 0 (6 **PASS** checks); negative assertion validation for tenant derivation, interval collision, DLQ port, benchmark configuration, consent rules, and pilot gate controls | Local shell execution |
| Hardening live checks, `bash scripts/acceptance/w11.sh live` | Exit 2 (2 **PASS**, 2 **BLOCKED**, 2 **NOT RUN**; INCOMPLETE / **BLOCKED**); policy simulations pass, live broker/consent blocked, benchmark/journey not run | Local execution without running backend target |
| Web unit tests, `npm --prefix web run test -- --run` | Exit 0 (93 tests passed across 17 test files); client-side data and unit tests | Local Node.js test runner |
| Foundation scope, `python3 scripts/verify-project.py --scope foundation` | Exit 0; 177-operation OpenAPI validation, 6 contract tests, docs lint, and diff check passed | Automated local baseline only |
| Tooling scope, `python3 scripts/verify-project.py --scope tooling` | Exit 0; 23 tests passed across MCP server, test runner, and workspace traversal | Local Python virtual environment |

## Boundary review

- Hardening scripts: `ops/` scripts provide deterministic policy logic validation and fail-closed negative checks; they do not claim live concurrency, live load performance, or server-side authorization.
- Load testing: Benchmark issues real HTTP requests with UUID-v4 keys and valid payloads; single-token runs are honestly labeled as one actor; independent multi-actor runs require 100-token fixtures and remain **NOT RUN** without live infrastructure.
- Documentation: `docs-lint.py` passes all Markdown files with 0 errors and 0 warnings.

## Disposition

`PARTIAL`

Web client tests and foundation contract checks pass cleanly. Hardening scripts enforce honest policy logic and fail-closed negatives without claiming database concurrency or multi-actor load acceptance. The load benchmark and 10-user pilot journey remain **NOT RUN**; physical host deployment and external restore gates remain **BLOCKED**. The Week 11 release gate remains **NOT RUN**, and the verified roadmap position remains W02-S26.
