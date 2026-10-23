# Start here

## Your first fifteen minutes

1. Open [You are here](YOU-ARE-HERE.md). Current position is W01-S01, not started; A and B have 24 hours each weekly, at most 70 percent planned. No application feature is checked off.
1. Read the first three paragraphs of [final decisions](DECISION-REGISTER.md): retain seven services, JDBC, Kafka/Avro, Nuxt and Flutter. AWS/Kubernetes deployment is future improvement. Proposed names in a step are targets to inspect/create, never discovered code.
1. Open [Week 1 overview](01-FOUNDATION/README.md), then [Week 1 Step 1](01-FOUNDATION/BUILD-GUIDE.md). W01-S01 inventories the dirty Git workspace; it does not delete files, reset history or deploy anything.
1. Read the linked Git primer only if unfamiliar. Git means versioned checkpoints; a commit saves one reviewed change, a branch keeps a line of work, a tag names a checked checkpoint. The guide uses ordinary Markdown links, so IntelliJ/Obsidian/Notion can follow the same path.
1. Run the safe status command printed in that step. Record the next action and blocker before changing code. Stop at the cap; no green tag exists yet.

## Calendar and rhythm

Start Monday 26 October 2026. Twelve-week release rehearsal is Friday 15 January 2027, with two rest days after each Friday. The 84-day boundary is Monday 18 January. Each week has eight small stones: six feature/quality groups, a Thursday repair buffer and Friday reviewed redeploy/restore/gate. Step reading, tests and commit fit the stated timebox; separately allocated pairing/review is included in weekly totals.

A owns backend decisions and reviews B's repeatable tests/scripts. B owns Flutter, narrow Nuxt work, fixtures and scripted acceptance. A's review time is budgeted. You need basic Java/SQL/Git or Flutter fluency; Week 1 records any gap. At most two unfamiliar concepts per day; new learning that exceeds that is a visible schedule risk, never weekend debt.

## When a term or command is unclear

Inspect first means locate the actual file/name before editing; absent names are explicitly proposed. Red/green means first observe a behavior assertion fail, then make the smallest change pass. A stone is two to six steps with one reviewed green tag; **Not run** means the complete step has not been executed. A gate is the checklist that blocks moving ahead when a critical result fails. An MVP is the minimum complete tested feature set, with cosmetic/richer variants in Stretch.

Use [glossary](GLOSSARY.md), [primers](primers/README.md), [daily commands](DAILY-COMMANDS.md), [stuck protocol](STUCK-PROTOCOL.md) and [definition of done](DEFINITION-OF-DONE.md). Never paste a production secret or real employee record into evidence. Inspect before running a command against a database or a host.

## Shared quality recipe

- [Engineering standards Q01–Q14](ENGINEERING-STANDARDS.md).
- [Quality coverage and exact enforcing IDs](QUALITY-COVERAGE.md).
- [Golden slice built in Weeks 1–3](GOLDEN-SLICE.md).
- [Ten-minute code review](CODE-REVIEW-CHECKLIST.md).
- [Test strategy](TEST-STRATEGY.md).
- [Security baseline](SECURITY-BASELINE.md).
- [API guidelines](API-GUIDELINES.md).
- [Daily commands](DAILY-COMMANDS.md).
- [Deploy/rollback/restore/reboot/disk/internet runbooks](runbooks/README.md).

## Progress and recovery

Use [roadmap](ROADMAP.md), [milestones](MILESTONE-MAP.md) and [global checklist](GLOBAL-CHECKLIST.md). Old progress translates through [STEP-ID-MAP](STEP-ID-MAP.md); the intermediate draft uses [DRAFT-ID-MAP](DRAFT-ID-MAP.md). Preserve every old tag and inspect evidence before repeating a migration or restore.

Five-minute daily routine: read dashboard/blocker, check branch/status, pick one next mapped step, read prerequisite/Expect/Undo, record energy/time and start or stop. End by saving the exact next command and closing the laptop. Critical failure is tomorrow's first action.

## Honest readiness

[V2 progress](PROGRESS-V2.md), [changelog](CHANGELOG-V2.md) and [validation log](V2-VALIDATION-LOG.md) distinguish completed documentation checks from unexecuted application work. Current prerequisites remain Java 21, at least 30 GiB free, running Docker and an intended host/external backup. Private deployment can be prepared while domain/DNS is unknown; public launch waits for TLS/auth/firewall/secrets/verified restore. One computer is not high availability and this guide is not compliance certification.

[Toolchain evidence](TOOLCHAIN-REFERENCE.md) and [operation script interface](runbooks/SCRIPT-INTERFACE.md) explain exact version/target prerequisites without inventing installed tools or production hosts.
