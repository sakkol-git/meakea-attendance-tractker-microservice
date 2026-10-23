# Week 01 build guide part 2

> **Navigation:** [Overview](README.md) · [First guide](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md)

## Stone 3 — One contract and both client fixtures

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W01-S09 — Write the first versioned OpenAPI slice (45–45 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/OPENAPI.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S08 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `contracts/openapi.yaml` `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect first: confirm no wire-authority file exists. Create the proposed sole spec for /api/v1/departments with the golden-slice request and 201/400/401/403/problem examples; mark it planned, not deployed. Record a pinned validator command from its official documentation and run it.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
rg --files contracts
```

**Expect:** Exactly one OpenAPI source is listed; its validator succeeds and a deliberate invalid local copy fails.

**Common mistake:** Creating a second OpenAPI file for the mobile client.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S09` for evidence-only work, or `feat(w01): w01-s09` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S10 — Retain and build the Nuxt client (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/OPENAPI.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S09 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `web/app/app.vue` `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect web/package.json, package-lock.json and current welcome source. Replace only the welcome view with a minimal contract-fixture panel, retaining Nuxt/Vue and the matching lockfile. Do not rewrite web to React.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
npm --prefix web run build
```

**Expect:** Nuxt build completes without error and the panel identifies its data as a fixture.

**Common mistake:** Replacing the existing Nuxt package manifest with React dependencies.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S10` for evidence-only work, or `feat(w01): w01-s10` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S11 — Locate or create the Flutter shell (45–45 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/OPENAPI.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S10 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `mobile/pubspec.yaml` `mobile/lib/main.dart` `mobile/test/widget_test.dart`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect first: locate an existing Flutter project before choosing mobile. If absent create a Flutter shell at proposed mobile, record flutter --version and lockfile, and keep one widget test. No backend call yet. Run flutter test from the confirmed mobile root.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
flutter --version
```

**Expect:** Confirmed Flutter version/root recorded; one shell widget test passes and the emulator shows the shell.

**Common mistake:** Creating another Flutter project without first locating existing mobile work.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S11` for evidence-only work, or `feat(w01): w01-s11` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S12 — Drive the mock from the one contract (45–45 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/OPENAPI.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S11 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `contracts/openapi.yaml` `mobile/test/department_contract_test.dart`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect the actual mobile root and pinned mock/validator from W01-S09. Start its local mock from the sole OpenAPI file, then make the Flutter fixture test assert the documented department response. Do not create a second independent mock schema.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
git diff --stat
```

**Expect:** Only the contract fixture and client test change; mock returns documented 201 fields and the client assertion passes.

**Common mistake:** Writing mock JSON independently from the contract schema.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S12` for evidence-only work, or `feat(w01): w01-s12` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S13 — Show one contract failure in both clients (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/PROBLEM-DETAILS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S12 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `web/app/app.vue` `mobile/test/department_contract_test.dart`. At most these three files change; reading dependencies is not permission to edit them all.
1. Repeat the golden slice with delta: invalid department fixture returns the documented problem code; both clients display safe detail and preserve input. Verify no technical stack or token is rendered.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
npm --prefix web run build
```

**Expect:** Nuxt build and selected Flutter test pass; invalid fixture displays VALIDATION_FAILED with no stack trace.

**Common mistake:** Displaying raw exception text as the user-facing error.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S13` for evidence-only work, or `feat(w01): w01-s13` for the isolated implementation change.
- [ ] Tagged `w01-s13-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Stone 4 — One problem shape and DB failure path

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W01-S14 — Add the shared problem contract to identity (45–45 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/PROBLEM-DETAILS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S13 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/java/com/hrflow/identityorg/api/ApiProblemHandler.java` `services/identity-org/src/test/java/com/hrflow/identityorg/api/ApiProblemHandlerTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect first: locate any error advice. If absent use these proposed paths. Write a failing validation/error assertion, then return the golden problem shape with a stable code/traceId and application/problem+json. Keep unknown exceptions sanitized.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Validation and unexpected-error tests assert content type and standard members, with no exception/stack in the response.

**Common mistake:** Letting an unexpected exception leak its message or stack.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S14` for evidence-only work, or `feat(w01): w01-s14` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S15 — Match the reactive gateway error shape (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/REACTIVE-GATEWAY.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S14 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/api-gateway/src/main/java/com/hrflow/gateway/ApiProblemWriter.java` `services/api-gateway/src/test/java/com/hrflow/gateway/ApiProblemWriterTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect the actual reactive error hooks selected by W01-S07; write the equivalent problem body for a denied/unroutable request. Reuse wire fixtures; do not copy MVC advice into WebFlux.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/api-gateway/pom.xml test
```

**Expect:** Reactive error test matches the same problem fields and never serializes a stack trace.

**Common mistake:** Assuming a servlet error handler catches a reactive filter error.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S15` for evidence-only work, or `feat(w01): w01-s15` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S16 — Register the negative contract examples (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/CONTRACT-TESTS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S15 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `contracts/openapi.yaml` `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Add the exact tested validation and sanitized-500 examples to the sole contract. Run its pinned validator and compare client fixtures; record the command/output instead of claiming an unused schema is checked.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
git diff -- contracts/openapi.yaml
```

**Expect:** Examples include stable codes and no secrets; validator and fixture assertions pass.

**Common mistake:** Checking only successful examples and omitting malformed/error fixtures.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S16` for evidence-only work, or `feat(w01): w01-s16` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S17 — Prove failures cannot create diagnostic data (45–45 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/TEST-FIRST.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S16 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/test/java/com/hrflow/identityorg/DiagnosticDatabaseTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Extend the existing disposable PostgreSQL test with invalid insert and rollback assertions. A failed transaction must leave its original row count unchanged. Run the assertion red before the minimum fix if a defect is found.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Rejected input/rolled-back insert changes zero committed rows; all identity tests pass.

**Common mistake:** Asserting an exception without checking that the transaction wrote zero rows.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S17` for evidence-only work, or `feat(w01): w01-s17` for the isolated implementation change.
- [ ] Tagged `w01-s17-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Continue

[Next build part](BUILD-GUIDE-PART-3.md).
