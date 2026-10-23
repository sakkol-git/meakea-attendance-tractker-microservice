# Week 11 verification and gate

> **Navigation:** [Overview](README.md) · [Build](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md)

## Evidence rule

All implementation gates are **NOT RUN**. Save exact command, exit, assertion/report path, sanitized result, operator and elapsed time in proposed docs/validation/W11-EVIDENCE.md. Scripts/classes are authored in the steps before invocation; a missing target or skipped container never counts as a passed behavior. The weekly runner dispatches real tests and preserves nonzero exits, never echo-only pass messages.

Step range: W11-S01 to W11-S34. Required new stone tags: `w11-s05-green`, `w11-s10-green`, `w11-s15-green`, `w11-s20-green`, `w11-s24-green`, `w11-s28-green`, `w11-s31-green`, `w11-s34-green`. Existing historical tags are not moved.

## Feature and failure assertions

| Focus | Observable check | Evidence |
| --- | --- | --- |
| W11-S03 | Forged tenant body/header cannot expose foreign lists/counts/files/exports/jobs. | Exact focused test and full owner regression report |
| W11-S06 | Racing approvals, check-in and balances preserve exactly the DB invariants. | Exact focused test and full owner regression report |
| W11-S09 | Reinstall/stale installation/logout never reveals saved tokens. | Exact focused test and full owner regression report |
| All implemented routes | Shared anonymous/invalid-token/wrong-role/foreign-tenant/list/count/file/export/job cases; explicit reason only for genuinely inapplicable cases | Route registry agrees with sole OpenAPI |
| Recovery | Duplicate/lost-response/concurrency/rollback and latest backup restore leave no forbidden duplicate or lost state | Actual DB/Kafka/client reports, not mocked success |

## Enterprise gate

- [ ] Q04: Every implemented route/count/list/file/job has token-derived tenant, method/resource negatives and scoped SQL; personal data RLS/pool tests apply from Week 3. Evidence: W11-S01 to W11-S34, focused result and this week release/restore report.
- [ ] Q05: Missing/invalid tokens, wrong role/resource, enumeration, throttle, refresh race/reuse and browser Origin/CSRF negatives all fail closed. Evidence: W11-S01 to W11-S34, focused result and this week release/restore report.
- [ ] Q08: Same retry key returns one result; owner outbox and effect/inbox/offset crash/replay preserve state; I/O/shutdown are bounded. Evidence: W11-S01 to W11-S34, focused result and this week release/restore report.
- [ ] Q09: Required domain/real PostgreSQL/Kafka/client assertions execute, implemented critical coverage selectors are nonempty, future packages are explicitly not yet covered, and container failures are not skipped. Evidence: W11-S01 to W11-S34, focused result and this week release/restore report.
- [ ] Q12: Every-push build/test/format/static/contract/dependency/image/secret jobs run pinned tools/images; scan/runner failure is not success. Evidence: W11-S01 to W11-S34, focused result and this week release/restore report.
- [ ] Q13: Intended-host deploy/rollback/external backup/scratch restore and applicable failure drills verify rows/files/schema/replay metadata; no high-availability claim. Evidence: W11-S01 to W11-S34, focused result and this week release/restore report.

## Minimum Viable Gate

- [ ] Every required feature output and its negative/race/replay assertion passed on actual reports; full owner regression has zero errors.
- [ ] The current private app starts, all eight reviewed stone tags identify checked commits, and no red work was deployed.
- [ ] Friday intended-host redeploy and authenticated/denied smoke pass; Week 1 explicitly uses a disposable local diagnostic skeleton.
- [ ] Latest backup/external copy and scratch restore verify known rows/ledgers/files/schema/replay metadata; Week 1 is local disposable mode, Week 2 requires external storage.
- [ ] Public access remains closed until TLS/auth/tenant/firewall/secrets/external-restore prerequisites pass. Private deployment is not a false public-launch claim.
- [ ] Dashboard/global checklist/actual time and exact next action are updated; any critical failure remains next week's first action.

## Gate decision and limitations

Result: **NOT RUN**. **PASS** requires all critical evidence. **CONDITIONAL PASS** is restricted to named noncritical Stretch/UX defects with owner/date. Security, tenant, migration, event integrity and restore failures are **FAIL**, block the gate and carry forward. No compliance certification, host/broker failover or guaranteed twelve-week delivery.

Optional push/SSE/styling/additional filters/patterns can be dropped without breaking the next week's prerequisites. Cloud deployment is future improvement. If actual duration exceeds plan, cut Stretch first and report the residual scope/date conflict; never consume weekends or omit critical tests.

## Release readiness scorecard

| Quality | Rating | Required evidence | Limitation or action |
| --- | --- | --- | --- |
| Q01 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |
| Q02 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |
| Q03 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |
| Q04 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |
| Q05 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |
| Q06 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |
| Q07 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |
| Q08 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |
| Q09 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |
| Q10 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |
| Q11 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |
| Q12 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |
| Q13 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |
| Q14 | **NOT RUN** | [Coverage chain](../QUALITY-COVERAGE.md) plus actual report path | Record **PASS**, **CONDITIONAL PASS**, **FAIL** or **NOT RUN** after execution only |

Known limitations: single host/broker/DB, power/internet/tunnel dependency, macOS Docker startup if selected, limited synthetic/pilot data, unverified performance until measured, no certification; AWS/Kubernetes and richer channels/inference training remain future work. A low-confidence score cannot override manual evidence. Record measured restore time/data-loss window and open defects without disguising them as passed controls.
