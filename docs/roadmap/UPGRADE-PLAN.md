# Roadmap V2 upgrade plan

## Scope and budget rule

Keep the twelve folders, objectives, existing useful domain explanations, primers and operational runbooks. Existing steps receive explicit scope, result sample, mistake, undo and habit; coupled goals split through test/change/negative/integration actions. All original IDs map forward, including optional work moved to Stretch. No old tag is deleted, moved or treated as automatically satisfying a new quality gate.

Every scheduled week must stay inside the unchanged 70 percent capacity from YOU-ARE-HERE.md. Thirty-minute atomic work slots include their test/commit and closing self-review; reading is separately budgeted. This is an estimate, not a promise to complete arbitrary scope in 30 minutes. A failed timebox consumes review reserve or carries forward; it never removes security/restore tests. Final measured slot totals replace phase-1 estimates after decomposition.

## Week-specific changes

| Week | Split or preserve | Quality work | Move to Stretch | Capacity treatment |
| --- | --- | --- | --- | --- |
| 01 | Bootstrap, diagnostic, contract and clients separated | Q01/Q02/Q09/Q12/Q14 foundation | Mock styling, extra diagnostic UI | Use existing review reserve; narrow walking skeleton |
| 02 | Twenty auth actions, then private host/backup steps | Q05/Q06 and early Q11 | Extra auth UI and provider polish | Count each A/B slot; no exposed host before security |
| 03 | Golden department write, org baseline, RLS and boundary tests | Q03/Q04/Q07/Q10 | Builder refactor, broad admin filters | Replace optional pattern time; policies are Must |
| 04 | Check-in/out/correction red-green-negative atoms | Q03/Q08 with shared tenant suite | Multi-shift/overnight/inference | Light budget 14.28 each; no new pattern |
| 05 | Intent/claim/receipt/retry/crash atoms | Q08 worker stop, bounded I/O and durability | Push/SSE, extra provider adapter | Kafka/Avro retained; receipt and offset crash tests mandatory |
| 06 | Expand days/reserve/decide/cancel/reconcile atoms | Q07/Q08/Q09 exact ledger and race tests | Monthly accrual/year-end UI | Upfront policy baseline, preserve leave correctness |
| 07 | Draft/validate/step decision/fulfill atoms | Q03/Q05/Q07/Q08 actor and race evidence | Optional attachment UI/Adapter refactor | Private file security remains mandatory if enabled |
| 08 | Contract and client reconciliation atoms | Q01/Q05/Q09 cross-client regression | Extra screens/localization/push | Light budget; no new feature domain |
| 09 | Projection/export/audit/metrics atoms | Q11 complete; Q08/Q09 export recovery | Specification refactor, custom charts/XLSX | Use freed pattern time for mandatory metrics |
| 10 | Deploy/backup/restore/reboot/rotation atoms | Q06/Q12/Q13 operational proof | Extra uptime dashboards | Restore and rollback protected, not optional |
| 11 | Route/race/replay/load/device atoms | All-Q release evidence | All new patterns/features | Repair highest-risk failures only |
| 12 | Freeze/accept/recover/demo atoms | Q13 semantic release and all-Q final gate | New dependencies/features | Preserve clean restore and exact artifact record |

## Fidelity to existing requirements

Must still covers auth/tenant, organization/employees/shifts/holidays, attendance/status/corrections, leave, material/special requests, in-app notification, audit, basic reports/CSV, Nuxt, Flutter and self-hosted tested backup/restore. The final design retains seven Java service roots, Kafka/Registry and confidence service. This increases work versus the superseded C plan; see the capacity shortfall rather than treating it as zero-cost reuse. Optional attachments never become a loophole for unprotected files; exports always authorize access. Existing detailed endpoint coverage is a design reference, not a promise to implement all optional admin variants in twelve weeks.

## Execution sequence and recovery

Current position is unstarted W01-S01, confirmed unchanged. Upgrade Weeks 1–2, 3–4, 5–6, 7–8, 9–10, then 11–12. After each pair lint, recompute owners/hours, update PROGRESS-V2.md and commit only touched docs. The linter extensions arrive after weekly migration, so intermediate commits are format-valid without pretending all new fields were already enforced.

## Historical pre-decomposition correction

ADR 008 replaces C after the user's latest architecture acceptance. Add explicit Boot/Cloud/Avro compatibility, JDBC/reactive gateway alignment, Kafka integration/replay/bootstrap, consumer metrics, seven-workload deployment and broker-aware recovery. The old reserve does not prove these fit. The initial coarse demand estimate was 489.5 combined h against 393.12 capacity, with A as the bottleneck. That was the pre-decomposition unallocated estimate; the final itemized allocation below supersedes it without increasing any 70-percent limit.

Cloud execution is future improvement by latest user instruction, while its final design is retained. Do not count deferral as successful AWS deployment. The architecture-specific confidence/event integration also needs an explicit step estimate; passing standalone Python tests is not proof of that integration.

## Final allocation

All original 182 IDs now map to 448 atomic steps. [Current roadmap ledger](ROADMAP.md) schedules 301.25 h including paired review, inside each owner/week/day limit; first-time reading is included in step durations. All Must feature deltas, five owner context alignments, CSRF/profile, real Kafka and restore have steps. The earlier blanket component/quality overhead is replaced by actual steps, not charged twice. Optional richer UI/reports, push/SSE, inference training, unadmitted patterns and cloud execution are Stretch/future; no required security or restore check was cut. The difference is recovery headroom and estimate uncertainty, not a claim of measured productivity.
