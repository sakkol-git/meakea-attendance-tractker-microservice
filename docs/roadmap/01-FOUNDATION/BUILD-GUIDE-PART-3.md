# Week 01 build guide part 3

> **Navigation:** [Overview](README.md) · [Dashboard](../YOU-ARE-HERE.md) · [Golden slice](../GOLDEN-SLICE.md)

## Stone 3 — Contract and client fixtures

**You will have:** Validation and unexpected-error tests assert content type and standard members, with no exception/stack in the response.

### W01-S11 — Create the locked Nuxt test runner (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/CONTRACT-TESTS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `web/package.json`, `web/package-lock.json`, `web/vitest.config.ts`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Use the exact Nuxt-compatible Vitest/test-utils versions recorded by the prior tool lock. Add the test script and minimal defineVitestConfig from the official Nuxt test-utils docs, keep build/dev scripts and lockfile intact. Prove test runner discovery with a tiny temporary assertion, then remove the temporary fixture; real fixtures arrive in the following steps.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
npm --prefix web run test -- --run
```

**Expect:** The selected fixture is discovered and executes; a deliberately false assertion exits nonzero. No-test discovery is not a successful feature test.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Treating a successful production build as proof that client failure fixtures executed hides missing tests.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s11` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S12 — Retain and build the Nuxt client (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/OPENAPI.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `web/app/app.vue`, `web/nuxt.config.ts`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect web/package.json, package-lock.json and current welcome source. Replace only the welcome view with a minimal contract-fixture panel, retaining Nuxt/Vue and the matching lockfile. Do not rewrite web to React.
1.  Set the MVP Nuxt client to ssr:false in the inspected nuxt.config.ts; keep all auth/API work in the existing Java gateway/identity boundary, not a new Nuxt server. Generate .output/public with its index.html before Caddy serves it; the ordinary SSR build alone is not a static-site proof.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
npm --prefix web run build
npm --prefix web run generate
```

**Expect:** Nuxt build completes without error and the panel identifies its data as a fixture.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Replacing a pending local command key/body on retry can duplicate a committed effect.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s12` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S13 — Locate or create the Flutter shell (45–45 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/OPENAPI.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `mobile/pubspec.yaml`, `mobile/lib/main.dart`, `mobile/test/widget_test.dart`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect first: locate an existing Flutter project before choosing mobile. If absent create a Flutter shell at proposed mobile, record flutter --version and lockfile, and keep one widget test. No backend call yet. Run flutter test from the confirmed mobile root.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
flutter --version
```

**Expect:** Confirmed Flutter version/root recorded; one shell widget test passes and the emulator shows the shell.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Storing refresh credentials in ordinary preferences or looping retry after logout defeats the secure session boundary.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s13` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S14 — Drive the mock from the one contract (45–45 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/OPENAPI.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `contracts/openapi.yaml`, `mobile/test/department_contract_test.dart`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect the actual mobile root and pinned mock/validator from W01-S09. Start its local mock from the sole OpenAPI file, then make the Flutter fixture test assert the documented department response. Do not create a second independent mock schema. Start the already locked mock with docs/validation/node_modules/.bin/prism mock contracts/openapi.yaml on loopback; stop it after fixtures.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
git diff --stat
```

**Expect:** Only the contract fixture and client test change; mock returns documented 201 fields and the client assertion passes.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Storing refresh credentials in ordinary preferences or looping retry after logout defeats the secure session boundary.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s14` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S15 — Show one contract failure in both clients (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/PROBLEM-DETAILS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `web/app/app.vue`, `mobile/test/department_contract_test.dart`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice with delta: invalid department fixture returns the documented problem code; both clients display safe detail and preserve input. Verify no technical stack or token is rendered.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
npm --prefix web run build
```

**Expect:** Nuxt build and selected Flutter test pass; invalid fixture displays VALIDATION_FAILED with no stack trace.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Replacing a pending local command key/body on retry can duplicate a committed effect.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s15` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S16 — Add the shared problem contract to identity (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/PROBLEM-DETAILS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/identity-org/src/main/java/com/hrflow/identityorg/api/ApiProblemHandler.java`, `services/identity-org/src/test/java/com/hrflow/identityorg/api/ApiProblemHandlerTest.java`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect first: locate any error advice. If absent use these proposed paths. Write a failing validation/error assertion, then return the golden problem shape with a stable code/traceId and application/problem+json. Keep unknown exceptions sanitized.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Validation and unexpected-error tests assert content type and standard members, with no exception/stack in the response.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Maintaining a second mock schema lets mobile/server shapes drift from the sole OpenAPI file.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s16` after staging only exact reviewed paths and recording command/exit/report evidence.
- [ ] Tagged `w01-s16-green` after the [ten-minute review](../CODE-REVIEW-CHECKLIST.md), all stone assertions and current private app startup pass; never overwrite an old tag.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

## Continue

[Next part](BUILD-GUIDE-PART-4.md).
