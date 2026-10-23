# Week 02 verification and gate

> **Navigation:** [Overview](README.md) · [First build step](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md)

## What counts as evidence

All results are **NOT RUN**. Proposed docs/validation/W02-EVIDENCE.md is created on the implementation branch and holds exact commands, exits, report paths, sanitized outputs, timing and blockers. Referenced tests/scripts must exist and assert behavior; echo-only runners, absent classes, skipped containers and empty architecture imports do not pass. Authoring a step is not running it.

## Required negative and recovery paths

| Boundary | Assertion | Evidence source |
| --- | --- | --- |
| Contract | Invalid request/problem fixtures fail validator or exact HTTP assertion | Sole OpenAPI plus client/server tests |
| Identity | No anonymous/invalid-token or forged identity access | Gateway and method/resource tests; Week 1 denies all business access |
| Database | Invalid/duplicate data rejected; failed transaction leaves zero new rows | Real PostgreSQL test report |
| Event | Recorded Avro round trip/compatibility result, not assumed delivery | Isolated spike finding; full durable flow is Week 5 |
| Failure | Absent host/image/scan DB/backup destination produces visible nonzero failure | Exact command output, never a green echo |
| Recovery | Restore to unique scratch target and verify data/auth without changing source DB | Scratch restore report and original-target checks |

## Enterprise gate

- [ ] Q05: Auth ladder, throttle, refresh race/reuse, method/resource roles and both client transport tests pass. Evidence: W02-S02–W02-S20; record actual report path in docs/validation/W02-EVIDENCE.md.
- [ ] Q06: Environment secrets, scan and bounded key-rotation tests pass; no production secret committed. Evidence: W02-S05, W02-S23; record actual report path in docs/validation/W02-EVIDENCE.md.
- [ ] Q04: Early trusted-tenant/forged-header/direct-ingress negatives pass; full DB RLS remains Week 3. Evidence: W02-S06, W02-S07, W02-S15; record actual report path in docs/validation/W02-EVIDENCE.md.
- [ ] Q11: Safe structured security logs and readiness/backup-failure alert have actual proof. Evidence: W02-S17, W02-S27; record actual report path in docs/validation/W02-EVIDENCE.md.
- [ ] Q13: Early external backup/scratch restore verified; full recovery runbooks remain Weeks 10–12. Evidence: W02-S25, W02-S26, W02-S32; record actual report path in docs/validation/W02-EVIDENCE.md.

## Minimum Viable Gate

- [ ] Every required stone has passing actual tests and starts the current private app before its new tag; no old tag is moved.
- [ ] All Enterprise gate tasks above have report evidence. Unimplemented critical transport/CI/restore behavior fails the gate.
- [ ] Friday private candidate redeployed, anonymous/tenant boundaries checked and scratch restore verified; Week 1 uses disposable local diagnostic data only.
- [ ] No public access before TLS/auth/firewall/secrets/verified external backup. Week 2 host not ready means failed deployment gate.
- [ ] Actual elapsed hours fit each contributor/day cap; any carried work and overall capacity conflict remain explicit.
- [ ] Dashboard, global checklist and current mapped next step updated without prechecking authored tasks.

## Honest limits and gate result

Result: **NOT RUN**. Record **PASS** only when required reports exist; **CONDITIONAL PASS** names a noncritical item/owner/due date. Security, tenant, missing CI tests, event correctness and backup restore cannot be conditional omissions. A critical failure is **FAIL** and becomes the next exact action before new scope. No single-host high-availability or certification claim.

The opening-week budget does not fund every later service requirement. Known draft gaps, including exact client transport/CSRF implementation and workflow/image command validation, must be closed before claiming this pair ready for a newcomer. [Progress](../PROGRESS-V2.md) tracks these separately from application completion.

## Ten-minute retro

- Record the slowest step and actual duration; split an overrun rather than revise its observed time downward.
- Cut optional polish/patterns first; preserve security and restore.
- Record the next exact failing command/owner, energy and Friday shutdown; keep two rest days.
