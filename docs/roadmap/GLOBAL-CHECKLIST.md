# Global finish-line checklist

## Stable acceptance IDs

Nothing is pre-checked. A completed task needs actual evidence and review, not an authored guide.

- [ ] SEC-01: owner A; Week 02; depends on W1 skeleton; evidence: JWT tenant and resource negative suite.
- [ ] API-01: owner A+B; Week 01; depends on Wire examples; evidence: One validated OpenAPI file and generated mock.
- [ ] DB-01: owner A; Week 01; depends on Java/Docker gate; evidence: Clean Flyway and tenant constraints.
- [ ] ATT-01: owner A+B; Week 04; depends on SEC-01 and org; evidence: Check-in/out/correction duplicate and concurrent tests.
- [ ] LEV-01: owner A+B; Week 06; depends on ATT-01 and balances; evidence: Locked reservation, approve/reject/cancel and conflict ledger.
- [ ] REQ-01: owner A+B; Week 07; depends on SEC-01 and org; evidence: Materials/other request step and participant tests.
- [ ] EVT-01: owner A; Week 05; depends on ATT-01 intent; evidence: Atomic outbox, receipts, crash/replay/order tests.
- [ ] FE-01: owner A; Week 08; depends on API-01; evidence: React core story and accessibility tests.
- [ ] MOB-01: owner B; Week 08; depends on API-01 Week1 mock; evidence: Flutter secure auth, offline retry and real-device story.
- [ ] TST-01: owner A+B; Week 11; depends on Core features; evidence: JUnit/Testcontainers, two-tenant race and load reports.
- [ ] OBS-01: owner A; Week 09; depends on DEP-01; evidence: Private Actuator/Kuma, safe logs and backup/outbox age.
- [ ] DEP-01: owner A; Week 02; depends on Host disk/Docker/domain gates; evidence: Confirmed-host Compose smoke, weekly redeploy.
- [ ] BKP-01: owner A+B; Week 02; depends on External disk; evidence: Nightly custom dump, external copy and scratch restore.
- [ ] BKP-02: owner A+B; Week 10; depends on BKP-01; evidence: Clean-machine restore repeated Week12.
- [ ] REL-01: owner A+B; Week 12; depends on All critical IDs; evidence: Exact artifact freeze, acceptance, rollback and limitations.
- [ ] DEMO-01: owner A+B; Week 12; depends on REL-01; evidence: Synthetic timed recording and fallback evidence.

## Coverage

Special leave belongs to LEV-01 SPECIAL type; general special requests belong to REQ-01 OTHER type. Audit/correction evidence is required in ATT-01/LEV-01/REQ-01 and reporting under OBS-01. Notifications are EVT-01 in-app records; push is optional. All Must features have an explicit owner/week/evidence above.

[Gate definitions](DEFINITION-OF-DONE.md) and [milestones](MILESTONE-MAP.md).
