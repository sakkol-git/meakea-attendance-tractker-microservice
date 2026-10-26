# Risk register

## Rating

Likelihood, impact and severity use High/Med/Low. High-impact correctness/data-loss issues remain critical even when likelihood is lower. These are planning judgments, not measured probabilities.

## Architecture complexity

- Likelihood: High; impact: High; severity: High.
- Early warning: Services/broker before first slice.
- Prevention: Choose one app and local outbox.
- Contingency: Park scaffold deployables, keep package boundaries.
- Owner: A; milestone: W1.

## Learning time

- Likelihood: High; impact: High; severity: High.
- Early warning: Step repeatedly exceeds 90 minutes.
- Prevention: Two concepts/day and primers.
- Contingency: Buffer and smaller reproduction, no weekend.
- Owner: A+B; milestone: Every gate.

## Burnout and overload

- Likelihood: High; impact: High; severity: High.
- Early warning: Tired shutdowns and missed rest.
- Prevention: 70 percent plan, two rest days, lighter W4/W8.
- Contingency: Cut stretch and stop sessions.
- Owner: A+B; milestone: W4/W8.

## Backend/mobile integration

- Likelihood: High; impact: High; severity: High.
- Early warning: Mock differs from live payload.
- Prevention: OpenAPI and B starts W1.
- Contingency: Fix one contract and both clients together.
- Owner: A+B; milestone: W8.

## Kafka reliability

- Likelihood: Med; impact: High; severity: High.
- Early warning: Someone adds broker without operational budget.
- Prevention: Measure single-host memory/disk for actual Kafka/Registry/services; bound JVM/log/retention usage and test restore.
- Contingency: Require new ADR and real-broker tests before adoption.
- Owner: A; milestone: W5/W10.

## API contract drift

- Likelihood: High; impact: High; severity: High.
- Early warning: Multiple schema files/DTO-only changes.
- Prevention: Single wire contract and CI fixture validation.
- Contingency: Freeze affected API and repair mocks/live tests.
- Owner: A+B; milestone: W8.

## Auth and tenant defects

- Likelihood: Med; impact: High; severity: High.
- Early warning: Scoped counts/object IDs leak.
- Prevention: Negative tests and scoped repository/constraints.
- Contingency: Block exposure; fix smallest tenant reproduction.
- Owner: A; milestone: W2/W11.

## Attendance defects

- Likelihood: High; impact: High; severity: High.
- Early warning: Grace/date/retry inconsistencies.
- Prevention: Fixed-clock interval and duplicate/race tests.
- Contingency: Preserve evidence, manual review, stop invalid mutations.
- Owner: A; milestone: W4.

## Concurrent leave balances

- Likelihood: High; impact: High; severity: High.
- Early warning: Two reservations spend same units.
- Prevention: Guard/balance locks and ledger.
- Contingency: Freeze affected approvals, reconcile ledger.
- Owner: A; milestone: W6.

## Lost or duplicated events

- Likelihood: High; impact: High; severity: High.
- Early warning: Pending intents age or double inbox.
- Prevention: Atomic write and per-handler receipt.
- Contingency: Resume replay idempotently, preserve malformed intent.
- Owner: A; milestone: W5.

## Weak tests

- Likelihood: High; impact: High; severity: High.
- Early warning: Echo-only runner or mocked database.
- Prevention: Real PostgreSQL Testcontainers and assertions.
- Contingency: Gate stays **FAIL** until reproducible proof.
- Owner: A+B; milestone: Every gate.

## Limited hardware

- Likelihood: High; impact: High; severity: High.
- Early warning: Designated Acer Nitro 5 has less than 30 GiB free, unhealthy or slow disk, sustained swapping, or cannot run the measured seven-service/Kafka profile.
- Prevention: Verify its exact Core i5 model, usable 16 GB RAM, free/healthy portion of the reported 2 TB disk and measured private-stack resource use; keep bounded JVM, log and retention settings.
- Contingency: No production pull/deploy until the Acer passes; reduce optional observability first, never remove security, backup or data-integrity controls to fit.
- Owner: A; milestone: W1/W2.

## ISP CGNAT dynamic IP

- Likelihood: High; impact: Med; severity: Med.
- Early warning: Remote timeout/router unavailable.
- Prevention: Outbound tunnel, inspect ISP privately.
- Contingency: Private host demo with external gate **FAIL**.
- Owner: A; milestone: W2.

## Power loss

- Likelihood: Med; impact: High; severity: High.
- Early warning: Machine sleeps or needs login.
- Prevention: Document awake/login behavior and backup age.
- Contingency: Restore/restart runbook and outage disclosure.
- Owner: A; milestone: W10.

## Single-host failure

- Likelihood: Med; impact: High; severity: High.
- Early warning: Hardware outage removes all components.
- Prevention: External backup and clean restore rehearsal.
- Contingency: Recover on replacement host, no HA claim.
- Owner: A; milestone: W10/W12.

## Disk exhaustion

- Likelihood: High; impact: High; severity: High.
- Early warning: Backup/export/log growth.
- Prevention: Log limits, retention, disk alerts.
- Contingency: Stop admission before false success; inspect without destructive prune.
- Owner: A; milestone: W10.

## Data loss

- Likelihood: Med; impact: High; severity: High.
- Early warning: Only copy is on same disk.
- Prevention: External verified backup.
- Contingency: Restore clean and reconcile business state.
- Owner: A+B; milestone: W10.

## Failed migrations

- Likelihood: Med; impact: High; severity: High.
- Early warning: Checksum mismatch or startup schema error.
- Prevention: Never edit applied SQL, expand-compatible change.
- Contingency: Forward fix or compatible image rollback.
- Owner: A; milestone: Every Friday.

## Untested backups

- Likelihood: High; impact: High; severity: High.
- Early warning: Cron success without restore.
- Prevention: Weekly scratch restore and age/checksum.
- Contingency: Do not expose externally, repair backup chain.
- Owner: A+B; milestone: W2/W10/W12.

## Scope creep

- Likelihood: High; impact: High; severity: High.
- Early warning: New patterns/cloud/ML before gates.
- Prevention: Fixed Must/If stable/Defer tiers.
- Contingency: Record displaced work and cut stretch.
- Owner: A+B; milestone: Every gate.

## Late deployment

- Likelihood: High; impact: High; severity: High.
- Early warning: Host first appears in Week10.
- Prevention: Private real-host skeleton W2.
- Contingency: Block host gate now; do not substitute dev laptop.
- Owner: A; milestone: W2.

## Pattern over-engineering

- Likelihood: High; impact: Med; severity: Med.
- Early warning: Interfaces precede real pain.
- Prevention: Four yes admission and baseline first.
- Contingency: Keep simple method/switch and reject refactor.
- Owner: A+B; milestone: W3/W7/W9.

## Quality work overloads the schedule

- Likelihood: High; impact: High; owner A+B; review every Friday.
- Signal: Actual step/learning time exceeds weekly ledger, especially first-time Java fixtures or service integration.
- Prevention: Count pairing/reading, reuse the golden slice once, keep narrow MVP behavior, cut optional polish/patterns/cloud first.
- Contingency: Preserve security/tenant/replay/restore; show exact remaining Must hours and scope/date conflict. A plan passing lint is not measured productivity.

## Step splitting increases total time

- Likelihood: Medium; impact: High; owner A; review Weeks 1–3.
- Signal: Commit/context switching consumes more than the stated included allowance.
- Prevention: Keep adjacent red/green steps on one work branch, reuse fixtures and commit exact paths; never green-tag a red assertion.
- Contingency: Use recovery headroom; merge only if one intent stays below 60 minutes and at most three files. Recompute hours and ID map, never omit tests.

## Dual JWT validation drift and single-host failure

- Likelihood: Medium; impact: High; owner A; gates Weeks 2/3/10/11.
- Signal: Any service trusts gateway identity headers, fails to validate the bearer token locally, uses mismatched issuer/audience/key-rotation settings, exposes direct business ingress, uses the wrong runtime DB role or lacks external restore proof.
- Prevention: Strip identity headers at the gateway; validate the same bearer JWT independently at gateway and service; keep business ports private; enforce method/resource checks, scoped JPA queries and personal-table RLS; run both-gate, cross-tenant and direct-ingress tests.
- Contingency: Block exposure, repair the failed validation gate/configuration and repeat security/rotation/restore suites. A single host/broker/DB is not highly available.
