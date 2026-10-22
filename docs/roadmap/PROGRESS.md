# Roadmap engineering progress

> **Note:** Guide complete as documentation; HRFlow implementation is not complete.

## Resume state

Branch: `docs/roadmap-playbook`. All twelve weekly manuals and master/support documents are written and committed in phase/pair checkpoints. There are 60 stones and 182 steps; 0 implementation steps are Run and 182 are Not run. Open START-HERE.md, then W01-S01. No green application tags were fabricated.

Weekly authoring complete: yes.

## Phase ledger

| Phase | Result | Evidence | Remaining work |
| --- | --- | --- | --- |
| A audit | **PASS** | Actual source/config/build/host inspection | Application blockers remain |
| B reconcile | **PASS** | ADR 006, outcome C, separate case-only Git rename | Execute migration later |
| C0 format | **PASS** | 154 Markdown files lint zero; selftest pass; broken fixture rejected | Manual viewers below |
| C navigation | **PASS** | Master files, primers and daily dashboard | User chooses kickoff date |
| D weeks | **PASS** | Six two-week commits; all gates/tests/recovery paths authored | Execute build-along later |
| E dry-run | **CONDITIONAL PASS** | Auth split, capacity/prerequisite review, code/diagram checks | Three-viewer manual acceptance not run |
| F report | **PASS** | Actual validation and folder tree ready for final handoff | No app completion claim |

## Verified outputs

[Actual validation](ACTUAL-VALIDATION.md) records commands/results, manual UI denials, example checks and source integrity. [Exact tree](FOLDER-TREE.md) lists files. [Baseline](00-PREPARATION/PROJECT-BASELINE.md) contains capacity math.

Full linter output is `PASS: 154 Markdown files; zero errors`. Before every documentation commit the linter was executed successfully. A deliberately broken temporary file produced five format errors and nonzero exit; it was never committed.

## Important corrections from dry-run

Oversized Week-2 auth work was split into separate password, JWT and refresh steps; final count is 182. Thursday/Friday caps now fit assigned joint work. Minimal attendance outbox intent precedes worker implementation. A local scratch-restore script is created before its first Friday use. All names for unbuilt code/scripts are marked proposed with Inspect first instructions.

## Inputs and blockers

User replied 24, interpreted as 24 hours each: 16.8 planned and 14.28 in Weeks 4/8. Combined capacity is 393.12, core step estimate 197.5, reading 48 and planned recovery/review buffer 147.62. These are estimates, not junior velocity proof. Start date remains unknown. Production setup is not ready, per user.

Disk has about 5 GiB free; target JDK 21 absent; Docker daemon stopped; host/domain/external backup not confirmed. Safe public exposure and real restore gates remain unexecuted. Offline Maven tests stop on uncached Avro/Jackson dependencies; Nuxt build/five Python prototype tests pass but do not prove HR workflows.

## Manual rendering and continuation

IntelliJ and Obsidian app inventory exists, but Computer Use denied access to each app. Notion ZIP import was not run. FORMAT-SELFTEST.md has the three-minute user acceptance procedure; mark viewer compatibility only after actual checks. Mermaid parser and text fallbacks are available; no all-viewer claim is made.

No authored guide files remain unfinished. If continuing documentation, start with manual rendering evidence and the actual kickoff date/host inventory; if building, start at W01-S01 and keep the security/backup gates. Do not treat remaining application steps as already executed.

## Git and source integrity

Repository began without commits and with staged/untracked source. Documentation-only temporary indexes preserved original staged source blobs. All 44 tracked source/config fingerprints match the pre-task snapshot; commits contain only docs or README. Legacy content was preserved in historical reference parts before normalization. The requested Architecture.md rename was performed with git mv and recorded in its own verified case-only commit; history was not rewritten.
