# Week 02 — Identity and private host deployment

> **Navigation:** [Build](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md) · [Roadmap](../ROADMAP.md)

## Capacity and calendar

Dates: 2026-11-02 through 2026-11-06; weekend rest follows.

**Planned hours:** A=14.25; B=11.

**Extra hours:** A=1; B=1.

The single input is in YOU-ARE-HERE.md; this week's limit is 16.8 h each. Step durations include first reading/test/commit. Scheduled additional A review/mentoring is 1 h; B review/recovery is 1 h. This allocation is real time, not assumed backend expertise. Unused capacity is recovery reserve; no extra feature is funded automatically.

Execution status on 26 October 2026: W02-S01 through W02-S23 retain automated evidence, and W02-S25 has Compose syntax evidence. W02-S24 remains blocked on direct Acer evidence. W02-S26 through W02-S29 have local guarded configuration/script drafts; Gitleaks history scans passed locally and in all three remotes. Key rotation, the web dependency audit, intended-host deployment, encrypted external backup, full scratch restore, physical-device credentials and the weekly gate remain open. The user chose to leave the host gate blocked until access to the separate Acer Nitro 5 and external backup disk is available.

## Preserved objective and starting point

Week 02 — Identity and private host deployment remains the weekly objective, now under the accepted service/JPA/Kafka/Nuxt design. Inspect first before using proposed class/table/endpoint names; the existing source is scaffolding. Repeat the golden slice and add only the named domain/client/recovery delta. No application progress is claimed by authored documents.

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
| 1 | Prerequisites and compatibility | W02-S01 to W02-S05 | 2.5/0.5 |
| 2 | First private service baseline | W02-S06 to W02-S10 | 2.25/0.5 |
| 3 | Contract and client fixtures | W02-S11 to W02-S15 | 3/0.5 |
| 4 | Error and integrity boundaries | W02-S16 to W02-S20 | 1.5/1.5 |
| 5 | Repeatable tools and event proof | W02-S21 to W02-S25 | 0.5/3 |
| 6 | CI and private operational prerequisites | W02-S26 to W02-S31 | 2/2 |
| 7 | Thursday recovery buffer | W02-S32 to W02-S33 | 0.5/1 |
| 8 | Friday deploy restore and gate | W02-S34 to W02-S35 | 1/1 |

## Minimum Viable Gate

- [ ] All required feature, tenant, role, validation, race/replay and recovery outcomes in VERIFY-AND-GATE have actual report evidence.
- [ ] The gateway and identity-org independently validate the forwarded bearer JWT; no trusted identity header supplies tenant, employee or role authority.
- [ ] Every stone has reviewed tests/startup and its unused new green tag; red work is not deployed.
- [ ] Friday intended-host redeploy, authenticated/denied smoke and external-backup scratch restore pass. Week 1 is explicitly disposable/local only.
- [ ] No critical failure is downgraded for schedule pressure; record exact next repair before the next week.

## Stretch and exact cuts

- [ ] Visual polish, additional filters/reports and provider push/SSE only after the required gate.
- [ ] Builder/Adapter/Specification refactors only if the preserved pattern admission test passes; they are not prerequisites.
- [ ] Cloud AWS/Terraform/Kubernetes deployment is future improvement by latest user decision, not a local deployment accomplishment.

At the time cap cut optional styling, extra variants/reports and unadmitted patterns first. Keep security, tenant tests, data/event correctness and backup restore. The minimum feature remains a narrow tested end-to-end behavior, not every catalog variant. The full final architecture is preserved; richer inference training, extra channels and managed cloud operations are not secretly prerequisite work.

## Private HTTPS setup

Read [host and TLS setup](ACCESS-SETUP.md) before the private browser smoke. Week 1 HTTP is diagnostic only; Week 2 uses trusted Caddy internal TLS without weakening Secure cookies. Public domain/Tunnel access remains separately gated.

## Continue and shutdown

- [Build](BUILD-GUIDE.md), [part two](BUILD-GUIDE-PART-2.md), [part three](BUILD-GUIDE-PART-3.md), [part four](BUILD-GUIDE-PART-4.md).
- [Part five](BUILD-GUIDE-PART-5.md), [part six](BUILD-GUIDE-PART-6.md), [part seven](BUILD-GUIDE-PART-7.md), [part eight](BUILD-GUIDE-PART-8.md).
- [Verify and gate](VERIFY-AND-GATE.md), [troubleshooting](TROUBLESHOOTING.md), [ID map](../STEP-ID-MAP.md).
- [Standards](../ENGINEERING-STANDARDS.md), [golden slice](../GOLDEN-SLICE.md), [self-review](../CODE-REVIEW-CHECKLIST.md).

Five-minute close: reviewed commit, actual evidence, next exact ID/command, blocker/energy and stop. Failed critical work is tomorrow's first action. No weekend rescue work and no guarantee of delivery from estimates alone.
