# Week 03 verification and gate

> **Navigation:** [Overview](README.md) · [Build](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md)

## Evidence rule

All implementation gates are **NOT RUN**. Save exact command, exit, assertion/report path, sanitized result, operator and elapsed time in proposed docs/validation/W03-EVIDENCE.md. Scripts/classes are authored in the steps before invocation; a missing target or skipped container never counts as a passed behavior. The weekly runner dispatches real tests and preserves nonzero exits, never echo-only pass messages.

Step range: W03-S01 to W03-S41. Required new stone tags: `w03-s05-green`, `w03-s11-green`, `w03-s17-green`, `w03-s23-green`, `w03-s29-green`, `w03-s35-green`, `w03-s38-green`, `w03-s41-green`. Existing historical tags are not moved.

## Feature and failure assertions

| Focus | Observable check | Evidence |
| --- | --- | --- |
| W03-S08 | Reject a cross-tenant department/position FK and manager hierarchy cycle. | Exact focused test and full owner regression report |
| W03-S12 | Employee stale If-Match is 412; absent employee is 404; foreign tenant list/count is empty. | Exact focused test and full owner regression report |
| W03-S15 | Missing employee link/inactive profile renders a safe empty/disabled state. | Exact focused test and full owner regression report |
| W03-S01–S05 | Attendance, leave, request, notification and reporting independently reject the invalid-token matrix and ignore identity authority headers after gateway acceptance. | Per-service security reports plus routed integration proof |
| All implemented routes | Shared anonymous/invalid-token/wrong-role/foreign-tenant/list/count/file/export/job cases; explicit reason only for genuinely inapplicable cases | Route registry agrees with sole OpenAPI |
| Recovery | Duplicate/lost-response/concurrency/rollback and latest backup restore leave no forbidden duplicate or lost state | Actual DB/Kafka/client reports, not mocked success |

## Enterprise gate

- [ ] Q03: Invalid edge/domain input writes no row, and direct invalid SQL is rejected by a real PostgreSQL constraint. Evidence: W03-S01 to W03-S41, focused result and this week release/restore report.
- [ ] Q04: Every implemented route/count/list/file/job has token-derived tenant, method/resource negatives and scoped SQL; personal data RLS/pool tests apply from Week 3. Evidence: W03-S01 to W03-S41, focused result and this week release/restore report.
- [ ] Q05: Attendance, leave, request, notification and reporting each execute local JWT validation for protected routes; missing/malformed/expired/not-yet-valid/wrong-issuer/wrong-audience/disallowed-algorithm/invalid-signature tokens fail closed, and gateway acceptance or forged identity headers cannot bypass the service gate. Evidence: W03-S01 to W03-S05 and routed smoke reports.
- [ ] Q07: Flyway clean/upgrade history, tenant FK/unique/checks, exact balances and stale/concurrent update assertions pass. Evidence: W03-S01 to W03-S41, focused result and this week release/restore report.
- [ ] Q08: Same retry key returns one result; owner outbox and effect/inbox/offset crash/replay preserve state; I/O/shutdown are bounded. Evidence: W03-S01 to W03-S41, focused result and this week release/restore report.
- [ ] Q10: Actual nonempty package and module tests reject forbidden imports/cycles; shared libraries contain no owner business repositories. Evidence: W03-S01 to W03-S41, focused result and this week release/restore report.

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
