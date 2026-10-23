# Actual validation and beginner dry-run

> **Note:** Historical V1 record; current final design and execution status are in ADR 008, PROGRESS-V2 and V2-VALIDATION-LOG. Earlier C/React/Kafka-deferral statements are superseded, not current instructions.

## Scope and execution status

Validated on 2026-10-23. Documentation is authored, not a finished application. Twelve weeks contain 60 stones and 182 sequential steps. Implementation steps marked **Run**: 0. Steps marked **Not run**: 182. No green application tags were created by documentation work.

## Commands actually executed

| Command | Actual result | Limit |
| --- | --- | --- |
| `python3 docs/roadmap/tools/docs-lint.py docs` | **PASS** zero errors | Structural/format/links/guide checks, not app behavior |
| Linter on FORMAT-SELFTEST.md | **PASS** | Syntax portability, not viewer rendering |
| Linter on broken temporary Markdown | **FAIL** as expected, nonzero exit | Detected heading level/skip, tabs, trailing spaces and invalid folder link |
| `mvn -o validate` | **PASS** ten reactor entries | No Java compilation |
| `mvn -o test` | **FAIL** missing cached Avro compiler/Jackson dependencies | Stopped before compilation; Java 21 also absent |
| `npm run build` in web | **PASS** | Existing Nuxt scaffold, not target React |
| `.venv/bin/python -m pytest -q` in confidence-service | **PASS** five tests, one warning | Standalone prototype, not HR workflows |
| Compose `config --quiet` on existing manifest | **PASS** | Syntax only; runtime defects remain |
| Docker info | **FAIL** daemon socket absent | No runtime or Testcontainers proof |
| `java -version` and installed-JDK inventory | **Verified** JDK 17 active, 17/23 installed | Target 21 not installed |
| Model/RAM/CPU/disk/OS checks | **Verified** Mac16,12, 16 GiB, 10 CPUs, macOS 26.5.1, about 5 GiB free | Not confirmation of production host readiness |

The exact Compose command was `docker compose -f infra/docker/docker-compose.yaml config --quiet`. Docker info used a server-version template; sensitive daemon/network output was not published. No dependencies were downloaded merely to force a green Java result while disk headroom is blocked.

## Diagram and code examples

Three current Mermaid diagrams were exported and parsed with the existing temporary Mermaid 11.17.2/jsdom 26.1.0 validator dependencies. All three parsed. Historical diagrams are preserved as text source with explicitly superseded context.

Builder, Adapter and Specification complete Java examples were extracted to temporary directories, compiled with installed JDK 23 using `javac --release 21`, then run successfully. This verifies Java 21-compatible bytecode/examples; it does not prove execution on installed Java 21 or the complete application. The example JUnit wrapper was not run as an application integration test. All lesson implementation steps remain **Not run**.

## Beginner review corrections

- Split an oversized Week-2 auth task into password verification, JWT and refresh steps; total is 182, not the initial 180.
- Corrected Thursday/Friday day caps so assigned joint work fits each contributor's stated weekly cap.
- Established minimal outbox intent with attendance before Week-5 worker reliability, avoiding a missing prerequisite.
- Made first stone require the minimal mobile shell to start; no nonexistent backend is claimed ready.
- Created access and host runbooks for proposed scripts and safe preconditions; commands name future targets explicitly rather than claiming installed tools.
- Required Week-1 local scratch restore creation before its first Friday command; Week 2 extends it to external backups.
- Defined Kafka recovery as not applicable to outcome C, with absence check and future-ADR test obligations; no fictitious broker test result.
- Preserved full prior documentation in bounded historical parts; current architecture/mapping overrides old topology without discarding domain coverage.

## Manual viewer checks

Result: **NOT RUN**. App inventory showed IntelliJ and Obsidian, but each native-app selection returned “Computer Use was not approved.” No document preview was available through those calls. Notion import was not performed, and no content was uploaded to a third party.

Open FORMAT-SELFTEST.md for one minute in each viewer: IntelliJ Markdown preview/plugin; Obsidian with docs vault and relative links; Notion ZIP Markdown import. Verify in your installed version. Use the supplied text version if Mermaid imports as code. Record version and failure before claiming all-viewer compatibility. No viewer guarantee has been fabricated.

## Integrity and remaining blockers

44 source/configuration fingerprints match the pre-task snapshot; pre-existing staged source blobs remain unchanged. All new commits contain only documentation paths or root README. The case-only architecture rename is recorded in its own commit. No source refactor, dependency upgrade, public deployment, volume deletion, history rewrite, user green tag or production backup was performed.

User hours “24” are interpreted as 24 each; start date remains unknown. Host setup is not ready. Disk, Java 21, Docker, domain/DNS, external disk and unattended reboot behavior remain real blockers. Full HRFlow completion still requires executing the guide, not merely reading it.

[Progress](PROGRESS.md), [baseline](00-PREPARATION/PROJECT-BASELINE.md), and [format guide](FORMAT-GUIDE.md).
