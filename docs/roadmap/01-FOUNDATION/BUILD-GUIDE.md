# Week 01 build guide

> **Navigation:** [Overview](README.md) · [Dashboard](../YOU-ARE-HERE.md) · [Golden slice](../GOLDEN-SLICE.md)

## Working rules

All paths for new code/tests/scripts below are proposed targets, not discovered classes. Inspect current equivalents first and record exact selected migration, controller/DTO/repository and fixture names in the commit body before editing. Existing Application roots and service poms are source evidence. If an action needs an API adapter or migration, use its inspected actual path instead of blindly creating the proposed Service/Repository pair; at most three files change. A larger change becomes a 45-minute finding and the smallest next atomic step, never a hidden multi-hour task.

Read first is inside the stated duration, once per unfamiliar concept; later repetitions reuse the primer. A/B already need basic Java/SQL/Git or Flutter skills: check them in Week 1. Limit first-time learning to two concepts per day. If the skill check reveals more learning, carry the named step forward and flag the schedule; do not borrow Thursday, Friday or weekends for new concepts. A reviews B's Java fixture/operations work in the scheduled pairing allowance, not unpaid extra time.

A red step writes one behavior assertion against the existing golden fixture. Compile/setup failures are not its intended red. A green step changes the smallest real adapter; the next negative step covers the stated denied/failure case. Repeat the [golden slice](../GOLDEN-SLICE.md): shared contract/problem/tenant/CI/observability are reused; the scope and expected case below are the delta. Reuse existing tests rather than build a parallel framework.

Every step ends with a focused commit. Stage only its exact reviewed paths. Put command/exit/report path and a safe output line in the commit body; aggregate into the weekly evidence file during buffer/Friday, avoiding a fourth source file in each commit. Red commits are work-branch checkpoints, never deployable tags. Every stone ends in the ten-minute review and tests/startup before its tag. For a read-only finding before app creation, startup is the current baseline scaffold or an explicit failed prerequisite; never falsely claim a business app starts.

## Stone 1 — Prerequisites and compatibility

**You will have:** Reactor lists ten projects and BUILD SUCCESS for validation; compilation is separately recorded and never inferred from validate.

### W01-S01 — Inventory the dirty workspace (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/GIT.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] The project root is open and Git is available; no earlier green application tag is required for this first inventory/checkpoint.

**Do:**

1. Inspect first: review current equivalents for `docs/validation/W01-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect staged/untracked source and .gitignore first; confirm node_modules, .venv, generated output, real secrets and OS files will not be staged. Create the implementation branch with `git switch -c codex/w01-foundation` from this reviewed docs checkpoint; if it already exists inspect it and use `git switch codex/w01-foundation` without resetting. Review every explicitly selected existing source path and save a baseline source commit with `git commit -m "chore: preserve reviewed source baseline"` before CI/feature edits; no code is changed by making this checkpoint. Then save the inventory/baseline commit ID in the evidence file as the final documentation-only step commit. This source-inventory checkpoint is the explicit many-files exception; later feature commits stay narrow.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
git status --short
git diff --cached --stat
git check-ignore web/node_modules ml/confidence-service/.venv
```

**Expect:** The implementation branch has a reviewed baseline commit containing the real scaffold source, excluding generated files/secrets; the final evidence commit records its ID. git show --stat BASELINE_ID lists the reviewed paths. This step is performed during development, not by the documentation author.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Pushing documentation-only history to CI leaves untracked Application/web files absent; committing generated files or secrets to fix that creates a second problem.

**Undo:** After verifying HEAD is the final documentation-only inventory commit, `git revert HEAD` reverses that evidence only. Keep the baseline source commit; `git switch -c recovery/w01-baseline BASELINE_ID` recovers its reviewed files on a new branch. Substitute the recorded baseline ID; never reset or delete the protected source checkpoint.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `docs(w01): w01-s01` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S02 — Measure the disk gate (30–30 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/DOCKER-COMPOSE.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `docs/validation/W01-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Record available disk and the agreed cleanup owner. Do not delete files automatically. Stop image pulls until at least 30 GiB is free.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
df -h .
```

**Expect:** Available column proves at least 30 GiB, or the gate records BLOCKED and no image is pulled.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Calling a command invocation proof without inspecting the recorded assertion/output.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s02` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S03 — Verify the Java 21 toolchain (30–30 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/DEPENDENCY-INJECTION.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `docs/validation/W01-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect installed JDKs with /usr/libexec/java_home -V; select an installed Java 21 in this shell/IDE or record the installation blocker. Do not change the project Java target to 17.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
java -version
```

**Expect:** Version line contains major 21; a different major leaves this prerequisite blocked.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Calling a command invocation proof without inspecting the recorded assertion/output.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s03` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S04 — Verify Docker without starting workloads (30–30 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/DOCKER-COMPOSE.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `docs/validation/W01-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. After the disk gate, start the installed Docker Desktop normally; record daemon version and memory allocation. Do not start PostgreSQL/Kafka yet.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
docker info
```

**Expect:** Server section responds without a daemon-socket error; no business/database port is published.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Calling a command invocation proof without inspecting the recorded assertion/output.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s04` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S05 — Record the build compatibility spike (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/DEPENDENCY-INJECTION.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `docs/validation/W01-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect pom.xml and all seven service poms plus shared libraries. Time-box resolution/compile of current reactor under Java 21; record Boot/Cloud/Avro versions, compatibility evidence and one smallest correction to make next. Output is a finding, not seven unreviewed pom edits. Record JUnit Jupiter version resolved by Boot 4 (JUnit 6 line) and the Spring Cloud compatibility result; do not silently use JUnit 5 or assume service 4.0.8 and root 4.1.1 are aligned.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -o validate
```

**Expect:** Reactor lists ten projects and BUILD SUCCESS for validation; compilation is separately recorded and never inferred from validate.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Calling a command invocation proof without inspecting the recorded assertion/output.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `docs(w01): w01-s05` after staging only exact reviewed paths and recording command/exit/report evidence.
- [ ] Tagged `w01-s05-green` after the [ten-minute review](../CODE-REVIEW-CHECKLIST.md), all stone assertions and current private app startup pass; never overwrite an old tag.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

## Continue

[Next part](BUILD-GUIDE-PART-2.md).
