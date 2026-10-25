# Week 01 build guide part 2

> **Navigation:** [Overview](README.md) · [Dashboard](../YOU-ARE-HERE.md) · [Golden slice](../GOLDEN-SLICE.md)

## Stone 2 — First private service baseline

**You will have:** Exactly one OpenAPI source is listed; its validator succeeds and a deliberate invalid local copy fails.

### W01-S06 — Align the identity JPA scaffold (60–60 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/SPRING-JPA.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/identity-org/pom.xml`, `services/identity-org/src/test/java/com/hrflow/identityorg/IdentityOrgApplicationTest.java`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect first: use the spike findings to align this one service to the reviewed root baseline; **keep** `spring-boot-starter-data-jpa` (ADR-009) and add the minimum test dependencies. Create the proposed context test using the actual existing IdentityOrgApplication. Do not align six other poms in this step. Correct this service application.yaml to real YAML (or deliberately rename it to application.properties); do not leave properties syntax in a .yaml file. Add the two JPA safety settings to the YAML:

   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: validate
       open-in-view: false
   ```

   Run with explicit private DB/test configuration.

1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Context test executes with Failures: 0; Hibernate **is** present in the dependency tree (this is correct per ADR-009); no JDBC-only alternative dependency.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Accepting an empty context/class import as a successful service test hides dependency or configuration defects.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s06` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S07 — Prove the reactive gateway starts (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/REACTIVE-GATEWAY.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/api-gateway/pom.xml`, `services/api-gateway/src/test/java/com/hrflow/gateway/ApiGatewayApplicationTest.java`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect first: switch this scaffold from MVC to the compatible reactive gateway dependency established by the spike. Add one context test; deny business routes until later verified routing/auth. No repository/database dependency belongs in this service. Correct this service application.yaml to real YAML (or deliberately rename it to application.properties); do not leave properties syntax in a .yaml file. Run with explicit private DB/test configuration.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -f services/api-gateway/pom.xml test
```

**Expect:** Gateway context test passes on the reactive stack; no business route is open anonymously.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Accepting an empty context/class import as a successful service test hides dependency or configuration defects.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s07` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S08 — Test one diagnostic migration in PostgreSQL (60–60 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/FLYWAY.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/identity-org/src/main/resources/db/migration/V1__diagnostic.sql`, `services/identity-org/src/test/java/com/hrflow/identityorg/DiagnosticDatabaseTest.java`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect first: if a migration already exists, select the next unused version instead of V1. In a disposable real PostgreSQL Testcontainers container write one JPA entity and Spring Data repository for the diagnostic table. Save one row, assert `count() == 1`, then try `saveAndFlush` with a duplicate name and assert `DataIntegrityViolationException`. Use `saveAndFlush` for the duplicate — plain `save` may not flush until commit and the constraint would not fire in time. The migration `V1__diagnostic.sql` is unchanged; `ddl-auto: validate` will fail the test if the entity and table disagree. Keep it out of public business endpoints.

   Proposed minimal entity and repository (confirm actual column names in the migration first):

   ```java
   @Entity
   @Table(name = "diagnostic_check", schema = "identity_org")
   class DiagnosticCheck {
       @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
       Long id;
       @Column(name = "check_name", nullable = false, unique = true)
       String checkName;
   }
   interface DiagnosticCheckRepository extends JpaRepository<DiagnosticCheck, Long> {}
   ```

1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Real PostgreSQL test passes clean migration, one stored diagnostic row and rejected duplicate via `DataIntegrityViolationException`; H2 is not used; `ddl-auto: validate` confirms entity-table alignment.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Editing an applied Flyway file breaks reproducible upgrade history; add the next unused version.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s08` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S09 — Lock the contract and client test tools (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/CONTRACT-TESTS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `docs/validation/package.json`, `docs/validation/package-lock.json`, `docs/validation/TOOLCHAIN-LOCK.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect package engines against installed Node; choose compatible reviewed Redocly CLI and Prism mock versions, save exact devDependency pins and lockfile under docs/validation. Record install/checksum/version commands in TOOLCHAIN-LOCK. For web tests choose the Nuxt-compatible Vitest/test-utils versions in the same finding before client test scripts are added. No latest tag or paid account.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
npm --prefix docs/validation ci
```

**Expect:** npm ci installs exactly the reviewed lock; local redocly/prism help responds. On first creation use npm install --save-dev --save-exact with the inspected version numbers, then prove ci.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Replacing a pending local command key/body on retry can duplicate a committed effect.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s09` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S10 — Write the first versioned OpenAPI slice (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/OPENAPI.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `contracts/openapi.yaml`, `docs/validation/TOOLCHAIN-LOCK.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect first: confirm no wire-authority file exists. Create the proposed sole spec for /api/v1/departments with the golden-slice request and 201/400/401/403/problem examples; mark it planned, not deployed. Record a pinned validator command from its official documentation and run it. Use the already locked local Redocly binary; no dependency install in this step.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
docs/validation/node_modules/.bin/redocly lint contracts/openapi.yaml
```

**Expect:** Exactly one OpenAPI source is listed; its validator succeeds and a deliberate invalid local copy fails.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Calling a command invocation proof without inspecting the recorded assertion/output.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s10` after staging only exact reviewed paths and recording command/exit/report evidence.
- [ ] Tagged `w01-s10-green` after the [ten-minute review](../CODE-REVIEW-CHECKLIST.md), all stone assertions and current private app startup pass; never overwrite an old tag.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

## Continue

[Next part](BUILD-GUIDE-PART-3.md).
