# Week 01 build guide part 3

> **Navigation:** [Overview](README.md) · [First guide](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md)

## Stone 5 — Build quality checks

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W01-S18 — Pin and check Java formatting (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/STATIC-ANALYSIS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S17 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `pom.xml` `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect first: no formatter is inferred. Pin a compatible Spotless plugin/version after a small local trial, configure the existing Java roots, run check, then format only the touched future implementation files. A reviews the root-pom change inside the review allowance.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn spotless:check
```

**Expect:** Formatting check succeeds on formatted input and fails on an intentionally unformatted temporary copy.

**Common mistake:** Running the formatter locally while leaving its check out of CI.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S18` for evidence-only work, or `feat(w01): w01-s18` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S19 — Fail static analysis on one real defect (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/STATIC-ANALYSIS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S18 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `pom.xml` `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Pin compatible PMD plugin/rules in the existing Maven build. Trial one synthetic unused/unsafe code fixture outside production; record rule ID and nonzero exit, then remove the fixture. Do not suppress all warnings to get green.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn pmd:check
```

**Expect:** PMD check fails on its recorded bad fixture and succeeds after removal; actual build rules remain enabled.

**Common mistake:** Disabling the rule that found a real defect merely to obtain a green build.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S19` for evidence-only work, or `feat(w01): w01-s19` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S20 — Scope coverage to critical packages (45–45 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/COVERAGE.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S19 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `pom.xml` `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Pin compatible JaCoCo and record the actual auth/attendance/leave domain/application package selectors. Set 80 percent lines and 70 percent branches on nonempty implemented critical packages; missing future packages are explicitly not covered, not reported as 100 percent.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn verify
```

**Expect:** Existing tests produce a report; a deliberate uncovered implemented critical branch fails the configured check.

**Common mistake:** Treating an empty package selector as 100 percent coverage.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S20` for evidence-only work, or `feat(w01): w01-s20` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S21 — Keep PostgreSQL integration tests real (60–60 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/TESTCONTAINERS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S20 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/pom.xml` `services/identity-org/src/test/java/com/hrflow/identityorg/DiagnosticDatabaseTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Review the container fixture from W01-S08: pin PostgreSQL 16 image, use mapped connection details, and remove any fallback to H2 or silent skip when Docker is absent. Prove an invalid DB constraint reaches PostgreSQL.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Container-backed test passes; stopping Docker produces an environment failure, never a skipped success.

**Common mistake:** Silently skipping required integration tests when Docker is unavailable.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S21` for evidence-only work, or `feat(w01): w01-s21` for the isolated implementation change.
- [ ] Tagged `w01-s21-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Stone 6 — Event spike and candidate images

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W01-S22 — Inspect the existing Avro and listener paths (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/KAFKA-BASICS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S21 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Read the actual avsc under libs/hrflow-contracts/events, libs poms and Compose. Record host/container listener addresses and SQL bind-mount source; identify Registry bootstrap mismatch without changing volumes.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
docker compose -f infra/docker/docker-compose.yaml config --quiet
```

**Expect:** Compose syntax exits zero; findings separately name bad mount/listener values because syntax validation does not prove connectivity.

**Common mistake:** Using the host Kafka listener address from inside a container.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S22` for evidence-only work, or `feat(w01): w01-s22` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S23 — Time-box Avro Registry compatibility (45–45 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/KAFKA-BASICS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S22 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. After disk/Docker gates, use a disposable isolated broker/Registry environment to trial the exact serializer/runtime versions from the spike. Record accepted compatible evolution and rejected incompatible schema. Output is a finding, not a production publisher.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -pl libs/hrflow-contracts -am test
```

**Expect:** Finding records resolved versions, actual round-trip result or exact blocker; an unavailable image/dependency is not a business pass.

**Common mistake:** Calling successful schema registration proof of backward compatibility.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S23` for evidence-only work, or `feat(w01): w01-s23` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S24 — Record the first event round-trip result (45–45 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/KAFKA-BASICS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S23 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Use only synthetic tenant/employee data and the inspected event schema in the isolated spike. Record produce/consume and a duplicate replay observation with a bounded wait. Explain why this is not yet outbox/inbox exactly-once processing.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -pl libs/hrflow-contracts -am verify
```

**Expect:** Actual producer/consumer evidence is recorded or the spike is blocked; no durable business delivery is claimed.

**Common mistake:** Calling one event round trip proof of durable exactly-once business processing.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S24` for evidence-only work, or `feat(w01): w01-s24` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S25 — Build pinned private candidate images (60–60 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/DOCKER-COMPOSE.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S24 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/Dockerfile` `services/api-gateway/Dockerfile` `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect actual jar names from Maven outputs. Add the two proposed Dockerfiles with pinned reviewed Java 21 runtime image digests, non-root users and no secrets; build each with the matching existing service context. Record digests, total size and memory gate. This does not prove the full seven-service host fits.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
git diff --stat
```

**Expect:** Both local candidate images build from reviewed jars, run as non-root and have recorded digests; absent/pull-failed images block the CI image-scan gate.

**Common mistake:** Using a floating base-image tag or putting build secrets in an image layer.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S25` for evidence-only work, or `feat(w01): w01-s25` for the isolated implementation change.
- [ ] Tagged `w01-s25-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Continue

[Next build part](BUILD-GUIDE-PART-4.md).
