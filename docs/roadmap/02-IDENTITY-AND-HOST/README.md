# Week 02 — Identity and private host deployment

> **Navigation:** [Build](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md) · [V2 progress](../PROGRESS-V2.md)

## Capacity and status

**Planned hours:** A=15.25; B=7.75.

**Step hours:** A=14.25; B=6.75. Reading/self-review allowance outside steps: 1 h each. Closing review inside a step is already counted. Normal 70 percent limit is 16.8 h each from the single dashboard input. Remaining limit is recovery reserve, not new scope. Gate **NOT RUN**; these are planning estimates, not measured junior velocity. B owns selected build/CI tasks with A's review included in the shared review slots; inability to do that work is a capacity blocker, not free pairing.

## Objective and prerequisite

Use the final seven-service/JDBC/Kafka/Nuxt architecture. This week narrows its output to the auth ladder, trusted tenant boundary, intended private host and first external backup/restore. Unknown names are inspected first; all new paths are explicitly proposed. No documentation commit is a green app tag.

## Day plan

| Day | Ordered work | New learning cap | Rule |
| --- | --- | --- | --- |
| Monday | Stones 1–2 until day cap; carry remainder | At most two linked topics | Stop at 4 h per person |
| Tuesday | Earliest unfinished stone, then next | At most two linked topics | Stop at 4 h per person |
| Wednesday | Finish scheduled feature/quality slots | At most two linked topics | Stop at 4 h per person |
| Thursday | Stone 7 buffer and earlier failures only | None | Stop at 2.5 h per person |
| Friday | Stone 8 review/deploy/restore/gate only | None | Stop at 2.3 h per person |
| Weekend | Rest | None | Zero scheduled work |

Day caps total 16.8 h. A stone is an outcome, not permission to exceed a day cap; resume its next uncommitted step next working day. If Stones 1–6 do not fit by Wednesday, Friday gate stays blocked until prerequisites are repaired. New concepts are not crammed into Thursday/Friday to protect a date.

## Stepping stones

| Stone | Observable group | Step range | A/B upper step h |
| --- | --- | --- | --- |
| 1 | Credentials and trusted token claims | W02-S01 to W02-S06 | 3.5/0 |
| 2 | Gateway negatives and refresh family | W02-S07 to W02-S12 | 4.25/0 |
| 3 | Logout roles and tenant boundary | W02-S13 to W02-S16 | 2/0 |
| 4 | Safe logs contract and clients | W02-S17 to W02-S20 | 1/1.75 |
| 5 | Confirmed private host procedure | W02-S21 to W02-S24 | 0.5/2 |
| 6 | Backup restore and access prerequisites | W02-S25 to W02-S28 | 1.5/1 |
| 7 | Thursday recovery only | W02-S29 to W02-S30 | 0.5/1 |
| 8 | Friday release and restore | W02-S31 to W02-S32 | 1/1 |

## Minimum Viable Gate

- [ ] All scheduled critical outcomes have actual tests/findings, not an empty evidence placeholder.
- [ ] No security/tenant/data-integrity/restore assertion is skipped to recover time.
- [ ] Friday private redeploy and scratch restore are verified, or the gate remains failed.
- [ ] The gate document and dashboard link real reports and next exact action; no task is prechecked.

## Stretch and unallocated required work

- [ ] UI polish, extra filters, extra reports and unadmitted patterns only after all gates.
- [ ] AWS/Terraform/Kubernetes execution stays future improvement by latest user instruction.

The full system is not funded by this week's slots. Other service build alignment, later business features, full event durability and full recovery remain required later work. [Architecture review](../ARCHITECTURE-REVIEW-V2.md) records the overall demand shortfall. Spikes prove findings only; blocked prerequisites carry into the next week and may break the deadline. No silent scope cut, optional prerequisite or borrowed weekend.

At a cap, stop the current step and record its smallest unresolved result. Do not replace real PostgreSQL/Kafka, skip security tests or omit backup restore. Pattern lessons are optional until the admission test proves benefit.

## Files and shared recipe

- [Build guide](BUILD-GUIDE.md).
- [Build part two](BUILD-GUIDE-PART-2.md).
- [Build part three](BUILD-GUIDE-PART-3.md).
- [Build part four](BUILD-GUIDE-PART-4.md).
- [Verify and gate](VERIFY-AND-GATE.md).
- [Troubleshooting](TROUBLESHOOTING.md).
- [Golden slice](../GOLDEN-SLICE.md).
- [Step map](../STEP-ID-MAP.md).

Repeat the golden slice for later feature sets with a short delta checklist. Introduce only the linked concept when needed, save evidence and next action, and close the laptop after Friday gate. Status stays **Not run** until actual execution.

## Draft budget validation still required

The 1-hour reading allowance is provisional and has not been checked against each person’s unique primers or actual learning speed. The daily distribution must also be checked: feature/quality work plus first-time reading must fit Monday–Wednesday, with no new concepts moved into Thursday buffer or Friday deployment. Until those checks and client transport gaps are fixed, this pair is a draft, not a beginner-ready validated plan. Do not interpret a total below 16.8 as proof that every daily/learning constraint passes.
