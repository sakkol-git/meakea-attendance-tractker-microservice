# Week 05 verification and gate

> **Navigation:** [Overview](README.md) · [Build](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md)

## Evidence rule

All implementation gates are **NOT RUN**. Save exact command, exit, assertion/report path, sanitized result, operator and elapsed time in proposed docs/validation/W05-EVIDENCE.md. Scripts/classes are authored in the steps before invocation; a missing target or skipped container never counts as a passed behavior. The weekly runner dispatches real tests and preserves nonzero exits, never echo-only pass messages.

Step range: W05-S01 to W05-S36. Required new stone tags: `w05-s05-green`, `w05-s10-green`, `w05-s15-green`, `w05-s20-green`, `w05-s25-green`, `w05-s30-green`, `w05-s33-green`, `w05-s36-green`. Existing historical tags are not moved.

## Feature and failure assertions

| Focus | Observable check | Evidence |
| --- | --- | --- |
| W05-S03 | A written trace identifies one owner transaction and Kafka acknowledgment; no local-only dispatcher is substituted. | Exact focused test and full owner regression report |
| W05-S06 | Clean and upgrade migrations pass; duplicate receipt fails its constraint and foreign tenant rows remain hidden. | Exact focused test and full owner regression report |
| W05-S09 | Failed commit writes none of the four; retry after lost response returns the same committed resource ID. | Exact focused test and full owner regression report |
| All implemented routes | Shared anonymous/invalid-token/wrong-role/foreign-tenant/list/count/file/export/job cases; explicit reason only for genuinely inapplicable cases | Route registry agrees with sole OpenAPI |
| Recovery | Duplicate/lost-response/concurrency/rollback and latest backup restore leave no forbidden duplicate or lost state | Actual DB/Kafka/client reports, not mocked success |

## Enterprise gate

- [ ] Q01: One /api/v1 OpenAPI validates and its request/response/error fixtures match implemented routes; broken schema copy fails. Evidence: W05-S01 to W05-S36, focused result and this week release/restore report.
- [ ] Q04: Every implemented route/count/list/file/job has token-derived tenant, method/resource negatives and scoped SQL; personal data RLS/pool tests apply from Week 3. Evidence: W05-S01 to W05-S36, focused result and this week release/restore report.
- [ ] Q08: Same retry key returns one result; owner outbox and effect/inbox/offset crash/replay preserve state; I/O/shutdown are bounded. Evidence: W05-S01 to W05-S36, focused result and this week release/restore report.
- [ ] Q09: Required domain/real PostgreSQL/Kafka/client assertions execute, implemented critical coverage selectors are nonempty, future packages are explicitly not yet covered, and container failures are not skipped. Evidence: W05-S01 to W05-S36, focused result and this week release/restore report.
- [ ] Q11: Private readiness, safe correlation/technical-tenant logs, bounded business/lag/error metrics and a firing/recovering alert have evidence. Evidence: W05-S01 to W05-S36, focused result and this week release/restore report.

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
