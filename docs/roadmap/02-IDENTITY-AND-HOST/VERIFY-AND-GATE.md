# Week 02 verification and gate

> **Navigation:** [Overview](README.md) · [Build](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md)

## Evidence rule

All implementation gates are **NOT RUN**. Save exact command, exit, assertion/report path, sanitized result, operator and elapsed time in proposed docs/validation/W02-EVIDENCE.md. Scripts/classes are authored in the steps before invocation; a missing target or skipped container never counts as a passed behavior. The weekly runner dispatches real tests and preserves nonzero exits, never echo-only pass messages.

Step range: W02-S01 to W02-S35. Required new stone tags: `w02-s05-green`, `w02-s10-green`, `w02-s15-green`, `w02-s20-green`, `w02-s25-green`, `w02-s31-green`, `w02-s33-green`, `w02-s35-green`. Existing historical tags are not moved.

## Feature and failure assertions

| Focus | Observable check | Evidence |
| --- | --- | --- |
| W02-S01 | Inventory distinguishes existing scaffold from new auth targets and names the next failing test. | Exact focused test and full owner regression report |
| W02-S02 | The login behavior assertion fails for missing response; red commit is not green-tagged or deployed. | Exact focused test and full owner regression report |
| W02-S03 | Password tests pass, stored value differs from plaintext, wrong password fails; outstanding login red stays on work branch. | Exact focused test and full owner regression report |
| All implemented routes | Shared anonymous/invalid-token/wrong-role/foreign-tenant/list/count/file/export/job cases; explicit reason only for genuinely inapplicable cases | Route registry agrees with sole OpenAPI |
| Recovery | Duplicate/lost-response/concurrency/rollback and latest backup restore leave no forbidden duplicate or lost state | Actual DB/Kafka/client reports, not mocked success |

## Enterprise gate

- [ ] Q04: Every implemented route/count/list/file/job has token-derived tenant, method/resource negatives and scoped SQL; personal data RLS/pool tests apply from Week 3. Evidence: W02-S01 to W02-S35, focused result and this week release/restore report.
- [ ] Q05: Missing/invalid tokens, wrong role/resource, enumeration, throttle, refresh race/reuse and browser Origin/CSRF negatives all fail closed. Evidence: W02-S01 to W02-S35, focused result and this week release/restore report.
- [ ] Q06: No secret in Git/logs; secret scan and environment/key rotation evidence prove bounded old-key retirement. Evidence: W02-S01 to W02-S35, focused result and this week release/restore report.
- [ ] Q11: Private readiness, safe correlation/technical-tenant logs, bounded business/lag/error metrics and a firing/recovering alert have evidence. Evidence: W02-S01 to W02-S35, focused result and this week release/restore report.
- [ ] Q13: Intended-host deploy/rollback/external backup/scratch restore and applicable failure drills verify rows/files/schema/replay metadata; no high-availability claim. Evidence: W02-S01 to W02-S35, focused result and this week release/restore report.

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
