# Week 01 verification and gate

> **Navigation:** [Overview](README.md) · [First build step](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md)

## What counts as evidence

All results are **NOT RUN**. Proposed docs/validation/W01-EVIDENCE.md is created on the implementation branch and holds exact commands, exits, report paths, sanitized outputs, timing and blockers. Referenced tests/scripts must exist and assert behavior; echo-only runners, absent classes, skipped containers and empty architecture imports do not pass. Authoring a step is not running it.

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

- [ ] Q01: One OpenAPI validates; invalid copy fails; both client fixtures agree. Evidence: W01-S09, W01-S12, W01-S16; record actual report path in docs/validation/W01-EVIDENCE.md.
- [ ] Q02: MVC/reactive error assertions and rollback tests pass without stack leakage. Evidence: W01-S14, W01-S15, W01-S17; record actual report path in docs/validation/W01-EVIDENCE.md.
- [ ] Q09: Real PostgreSQL tests run; scoped nonempty coverage configuration is proved. Evidence: W01-S08, W01-S20, W01-S21; record actual report path in docs/validation/W01-EVIDENCE.md.
- [ ] Q12: Every-push build/test/docs/contract/format/static/dependency/image/secret checks actually ran. Evidence: W01-S25, W01-S26, W01-S27, W01-S28; record actual report path in docs/validation/W01-EVIDENCE.md.
- [ ] Q14: Formatter/static negative fixture fails and self-review is recorded. Evidence: W01-S18, W01-S19, W01-S32; record actual report path in docs/validation/W01-EVIDENCE.md.

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
