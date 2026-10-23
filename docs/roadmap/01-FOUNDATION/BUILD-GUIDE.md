# Week 01 build guide

> **Navigation:** [Week overview](README.md) · [Dashboard](../YOU-ARE-HERE.md) · [Quality standards](../ENGINEERING-STANDARDS.md)

## How to use this pair

This is the V2 final-service design draft. All steps are **Not run** until actual execution evidence exists. Future file/class names below are proposed; inspect before creating them. Never create hrflow-app or replace Nuxt with React. Upper duration includes test/commit; reading allowance is in README. If a slot overruns, stop and record the carry-forward instead of calling the result complete.

For each step stage only its reviewed explicit paths, then commit with the message in Check. A step with a behavior-red test stays on the implementation branch until green; never tag/deploy red work. Undo assumes the last commit belongs only to this step. A database migration already applied needs a reviewed forward migration, not just Git revert. Use [daily commands](../DAILY-COMMANDS.md) to run a recorded single test.

Only two new learning topics per day: prerequisite environment; JDBC/gateway; contract/problem; testing; CI/event integration are revisited across this pair, not all introduced in one sitting. Thursday introduces none. A blocked learning prerequisite moves the next topic forward and can invalidate the deadline; do not hide that carry.

## Stone 1 — Prerequisite evidence

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W01-S01 — Inventory the dirty workspace (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/GIT.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] preparation has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect first: record staged/untracked paths and current branch; save a reviewed checkpoint plan without adding source or secrets. Record the evidence file as a documentation-only commit.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
git status --short
```

**Expect:** Existing source paths remain staged/untracked exactly as before; checkpoint plan names the work to preserve.

**Common mistake:** Staging the entire dirty tree includes unrelated work or secrets.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S01` for evidence-only work, or `feat(w01): w01-s01` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S02 — Measure the disk gate (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/DOCKER-COMPOSE.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S01 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Record available disk and the agreed cleanup owner. Do not delete files automatically. Stop image pulls until at least 30 GiB is free.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
df -h .
```

**Expect:** Available column proves at least 30 GiB, or the gate records BLOCKED and no image is pulled.

**Common mistake:** Counting total disk size instead of currently available free space.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S02` for evidence-only work, or `feat(w01): w01-s02` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S03 — Verify the Java 21 toolchain (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/DEPENDENCY-INJECTION.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S02 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect installed JDKs with /usr/libexec/java_home -V; select an installed Java 21 in this shell/IDE or record the installation blocker. Do not change the project Java target to 17.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
java -version
```

**Expect:** Version line contains major 21; a different major leaves this prerequisite blocked.

**Common mistake:** Changing the project target to match Java 17 instead of selecting Java 21.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S03` for evidence-only work, or `feat(w01): w01-s03` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S04 — Verify Docker without starting workloads (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/DOCKER-COMPOSE.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S03 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. After the disk gate, start the installed Docker Desktop normally; record daemon version and memory allocation. Do not start PostgreSQL/Kafka yet.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
docker info
```

**Expect:** Server section responds without a daemon-socket error; no business/database port is published.

**Common mistake:** Treating a running Docker icon as proof that its daemon responds.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S04` for evidence-only work, or `feat(w01): w01-s04` for the isolated implementation change.
- [ ] Tagged `w01-s04-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Stone 2 — Identity and gateway baseline

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W01-S05 — Record the build compatibility spike (45–45 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/DEPENDENCY-INJECTION.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S04 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect pom.xml and all seven service poms plus shared libraries. Time-box resolution/compile of current reactor under Java 21; record Boot/Cloud/Avro versions, compatibility evidence and one smallest correction to make next. Output is a finding, not seven unreviewed pom edits.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -o validate
```

**Expect:** Reactor lists ten projects and BUILD SUCCESS for validation; compilation is separately recorded and never inferred from validate.

**Common mistake:** Calling Maven validate a successful Java compilation or compatibility test.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S05` for evidence-only work, or `feat(w01): w01-s05` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S06 — Align the identity JDBC scaffold (60–60 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/SPRING-JDBC.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S05 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/pom.xml` `services/identity-org/src/test/java/com/hrflow/identityorg/IdentityOrgApplicationTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect first: use the spike findings to align this one service to the reviewed root baseline; replace JPA with JDBC and add the minimum test dependencies. Create the proposed context test using the actual existing IdentityOrgApplication. Do not align six other poms in this step.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Context test executes with Failures: 0; dependency tree has JDBC and no accidental Hibernate runtime for this owner.

**Common mistake:** Removing a JPA import without removing its transitive runtime dependency.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S06` for evidence-only work, or `feat(w01): w01-s06` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S07 — Prove the reactive gateway starts (45–45 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/REACTIVE-GATEWAY.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S06 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/api-gateway/pom.xml` `services/api-gateway/src/test/java/com/hrflow/gateway/ApiGatewayApplicationTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect first: switch this scaffold from MVC to the compatible reactive gateway dependency established by the spike. Add one context test; deny business routes until later verified routing/auth. No repository/database dependency belongs in this service.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/api-gateway/pom.xml test
```

**Expect:** Gateway context test passes on the reactive stack; no business route is open anonymously.

**Common mistake:** Adding the MVC gateway starter to a reactive gateway project.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S07` for evidence-only work, or `feat(w01): w01-s07` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S08 — Test one diagnostic migration in PostgreSQL (60–60 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/FLYWAY.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S07 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/resources/db/migration/V1__diagnostic.sql` `services/identity-org/src/test/java/com/hrflow/identityorg/DiagnosticDatabaseTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect first: if a migration already exists, select the next unused version instead of V1. In a disposable real PostgreSQL container create a non-personal diagnostic row, query it through JdbcClient and assert duplicate constraint failure. Keep it out of public business endpoints.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Real PostgreSQL test passes clean migration, one stored diagnostic row and rejected duplicate; H2 is not used.

**Common mistake:** Running a database assertion against H2 or an existing production database.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S08` for evidence-only work, or `feat(w01): w01-s08` for the isolated implementation change.
- [ ] Tagged `w01-s08-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Continue

[Next build part](BUILD-GUIDE-PART-2.md).
