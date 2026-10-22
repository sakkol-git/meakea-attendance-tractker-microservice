# Roadmap engineering progress

> **Note:** This tracks documentation, not completed HRFlow features.

## Resume state

Branch: `docs/roadmap-playbook`. Phases A, B, C0 and C are complete; twelve weekly drafts exist and are being committed two weeks at a time. Next action is phase D pair commits, then final beginner validation. Source fingerprints and staged source blobs are preserved.

Weekly authoring complete: yes.

## Phase ledger

| Phase | Result | Evidence | Next action |
| --- | --- | --- | --- |
| A audit | **PASS** | Preparation inventory and actual command results | Retain blockers |
| B reconcile | **PASS** | ADR 006, outcome C, actual case-only rename commit | Follow settled baseline |
| C0 format | **PASS** | Full lint zero; selftest passes; broken temp fixture fails | Re-run before every commit |
| C navigation | **PASS** | Master files and primers linked | Commit weekly pairs |
| D weeks | **NOT RUN** | Pairs through Week 04 committed | Weeks 05–06 next |
| E dry-run | **NOT RUN** | Source comparison and final checks pending | Review beginner recovery paths |
| F report | **NOT RUN** | Final report pending | Summarize honest limits |

## Audit limitations

Maven validation passes; offline Maven test stops before compilation on missing cached Avro/Jackson artifacts. Java 21 is absent, Docker daemon is stopped, disk has about 5 GiB free. Nuxt build and five Python prototype tests pass; they are not React or HR workflow acceptance. Production setup is not ready, per user.

## Capacity correction

User replied 24. Interpreted as 24 hours each per week, 16.8 planned each, 14.28 in lighter Weeks 4/8. Total 393.12 planned contributor-hours. Start date remains unknown. Initial provisional 20-hour assumption is superseded.

## Format and Git sequence

Strict all-docs formatting required splitting prior long documents into historical references before the first audit checkpoint could pass lint. The repository began without commits, so a documentation-only temporary index preserved pre-existing staged source. On case-insensitive macOS a separate verified case-only rename commit was needed; no history was rewritten.
