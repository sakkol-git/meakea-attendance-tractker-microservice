# Week 05 — Durable local events and notifications

> **Navigation:** [Build](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md) · [Roadmap](../ROADMAP.md)

## Capacity and calendar

Dates: 2026-11-23 through 2026-11-27; weekend rest follows.

**Planned hours:** A=10; B=14.25.

**Extra hours:** A=2; B=0.

The single input is in YOU-ARE-HERE.md; this week's limit is 16.8 h each. Step durations include first reading/test/commit. Scheduled additional A review/mentoring is 2 h; B receives review during its already counted fixture/operations steps. This allocation is real time, not assumed backend expertise. Unused capacity is recovery reserve; no extra feature is funded automatically. All application outcomes are **NOT RUN**.

## Preserved objective and starting point

Week 05 — Durable local events and notifications remains the weekly objective, now under the accepted service/JPA/Kafka/Nuxt design. Inspect first before using proposed class/table/endpoint names; the existing source is scaffolding. Repeat the golden slice and add only the named domain/client/recovery delta. No application progress is claimed by authored documents.

## Day plan

| Day | Work | Per-person cap | Learning rule |
| --- | --- | --- | --- |
| Monday | Earliest prerequisite, then feature stones 1–2 | 4 h | At most two unfamiliar concepts |
| Tuesday | Continue feature stones 2–4 in order | 4 h | At most two unfamiliar concepts |
| Wednesday | Finish earliest feature/quality stones through 6 | 4 h | At most two unfamiliar concepts |
| Thursday | Stone 7 buffer, review/mentoring and existing failures | 2.5 h | No new concept or scope |
| Friday | Stone 8 checked redeploy, smoke, restore and gate | 2.3 h | No new concept or scope |
| Weekend | Rest | 0 h | No catch-up allocation |

A stone may span working days; stop at the day cap and save its next exact step. A's scheduled review may also happen beside a B fixture Monday–Wednesday, with the same time removed from Thursday allowance; do not count it twice. If prerequisites/learning overrun, use buffer for repair and report the schedule impact, never cramming new concepts into Thursday/Friday.

## Stone table

| Stone | Visible result group | Steps | A/B step h |
| --- | --- | --- | --- |
| 1 | First feature red and green | W05-S01 to W05-S03 | 1.25/0.5 |
| 2 | Feature scope and negative paths | W05-S04 to W05-S06 | 0.75/1 |
| 3 | Client and integration deltas | W05-S07 to W05-S12 | 0.75/2.75 |
| 4 | Remaining domain invariants | W05-S13 to W05-S18 | 0.75/2.75 |
| 5 | Quality and failure recovery | W05-S19 to W05-S24 | 1.5/2 |
| 6 | Final feature integration | W05-S25 to W05-S30 | 0.75/3 |
| 7 | Thursday recovery buffer | W05-S31 to W05-S33 | 1/1 |
| 8 | Friday deploy restore and gate | W05-S34 to W05-S36 | 1.25/1.25 |

## Minimum Viable Gate

- [ ] All required feature, tenant, role, validation, race/replay and recovery outcomes in VERIFY-AND-GATE have actual report evidence.
- [ ] Every stone has reviewed tests/startup and its unused new green tag; red work is not deployed.
- [ ] Friday intended-host redeploy, authenticated/denied smoke and external-backup scratch restore pass. Week 1 is explicitly disposable/local only.
- [ ] No critical failure is downgraded for schedule pressure; record exact next repair before the next week.

## Stretch and exact cuts

- [ ] Visual polish, additional filters/reports and provider push/SSE only after the required gate.
- [ ] Builder/Adapter/Specification refactors only if the preserved pattern admission test passes; they are not prerequisites.
- [ ] Cloud AWS/Terraform/Kubernetes deployment is future improvement by latest user decision, not a local deployment accomplishment.

At the time cap cut optional styling, extra variants/reports and unadmitted patterns first. Keep security, tenant tests, data/event correctness and backup restore. The minimum feature remains a narrow tested end-to-end behavior, not every catalog variant. The full final architecture is preserved; richer inference training, extra channels and managed cloud operations are not secretly prerequisite work.

## Continue and shutdown

- [Build](BUILD-GUIDE.md), [part two](BUILD-GUIDE-PART-2.md), [part three](BUILD-GUIDE-PART-3.md), [part four](BUILD-GUIDE-PART-4.md).
- [Part five](BUILD-GUIDE-PART-5.md), [part six](BUILD-GUIDE-PART-6.md), [part seven](BUILD-GUIDE-PART-7.md), [part eight](BUILD-GUIDE-PART-8.md).
- [Verify and gate](VERIFY-AND-GATE.md), [troubleshooting](TROUBLESHOOTING.md), [ID map](../STEP-ID-MAP.md).
- [Standards](../ENGINEERING-STANDARDS.md), [golden slice](../GOLDEN-SLICE.md), [self-review](../CODE-REVIEW-CHECKLIST.md).

Five-minute close: reviewed commit, actual evidence, next exact ID/command, blocker/energy and stop. Failed critical work is tomorrow's first action. No weekend rescue work and no guarantee of delivery from estimates alone.
