# Week 02 build guide

> **Navigation:** [Week overview](README.md) · [Dashboard](../YOU-ARE-HERE.md) · [Quality standards](../ENGINEERING-STANDARDS.md)

## How to use this pair

This is the V2 final-service design draft. All steps are **Not run** until actual execution evidence exists. Future file/class names below are proposed; inspect before creating them. Never create hrflow-app or replace Nuxt with React. Upper duration includes test/commit; reading allowance is in README. If a slot overruns, stop and record the carry-forward instead of calling the result complete.

For each step stage only its reviewed explicit paths, then commit with the message in Check. A step with a behavior-red test stays on the implementation branch until green; never tag/deploy red work. Undo assumes the last commit belongs only to this step. A database migration already applied needs a reviewed forward migration, not just Git revert. Use [daily commands](../DAILY-COMMANDS.md) to run a recorded single test.

Only two new learning topics per day: token lifecycle and tenant boundary early, then credential-family concurrency and recovery; all other tools reuse Week 1 learning. Thursday introduces none. A blocked learning prerequisite moves the next topic forward and can invalidate the deadline; do not hide that carry.

## Stone 1 — Credentials and trusted token claims

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W02-S01 — Inspect identity and security before coding (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/JWT-REFRESH.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] Week 1 gate has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W02-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect existing Security configuration, user/refresh migrations, gateway filters and tests; record actual names or the proposed targets for this week. No endpoint/table is presumed present.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
rg --files services/identity-org services/api-gateway
```

**Expect:** Inventory distinguishes existing scaffold from new auth targets and names the next failing test.

**Common mistake:** Assuming the startup scaffold already contains security or user tables.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S01` for evidence-only work, or `feat(w02): w02-s01` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S02 — Write the valid-login red test (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/TEST-FIRST.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S01 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/test/java/com/hrflow/identityorg/auth/LoginTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect first: create proposed LoginTest if absent. Add a fixed synthetic active user fixture and assert valid login returns access-token response contract; do not implement login yet. Failed compilation is not a valid red result.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** The login behavior assertion fails for missing response; red commit is not green-tagged or deployed.

**Common mistake:** Accepting a missing-class compilation failure as the intended red assertion.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S02` for evidence-only work, or `feat(w02): w02-s02` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S03 — Hash passwords behind one tested function (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/JWT-REFRESH.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S02 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/java/com/hrflow/identityorg/auth/PasswordService.java` `services/identity-org/src/test/java/com/hrflow/identityorg/auth/PasswordServiceTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Use the existing Spring Security password encoder after inspecting its version; test matches/wrong password and unique salt. Never log/store plaintext or compare hashes as raw string equality.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Password tests pass, stored value differs from plaintext, wrong password fails; outstanding login red stays on work branch.

**Common mistake:** Storing plaintext passwords or comparing salted hashes as raw strings.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S03` for evidence-only work, or `feat(w02): w02-s03` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S04 — Add the minimal credential lookup and login (45–45 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/SPRING-JDBC.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S03 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/java/com/hrflow/identityorg/auth/LoginService.java` `services/identity-org/src/main/java/com/hrflow/identityorg/auth/LoginController.java` `services/identity-org/src/main/resources/db/migration/V2__users.sql`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect migrations first: choose next unused version if V2 is taken. Add tenant-scoped normalized credential lookup and use the password function. Token creation is an injected port filled next; do not return fake production tokens or an entity. Pre-auth lookup is a narrow audited exception, not arbitrary tenant business access.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Credential path works in the test fixture; absent token port leaves full login acceptance red until W02-S05.

**Common mistake:** Allowing a pre-auth tenant selector to authorize arbitrary business rows.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S04` for evidence-only work, or `feat(w02): w02-s04` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S05 — Sign and validate the access-token claims (45–45 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/JWT-REFRESH.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S04 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/java/com/hrflow/identityorg/auth/AccessTokens.java` `services/identity-org/src/test/java/com/hrflow/identityorg/auth/AccessTokensTest.java` `services/identity-org/src/main/resources/application.yaml`. At most these three files change; reading dependencies is not permission to edit them all.
1. Load signing private key from environment/secret mount; sign short-lived asymmetric JWT, validate issuer/audience/signature/expiry in tests with test keys. Never put a production key in YAML. Wire the login port and run its original red assertion green.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Valid login returns a verified short-lived access token; expired/wrong-audience/wrong-key tests fail safely.

**Common mistake:** Committing a signing key or omitting issuer/audience checks.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S05` for evidence-only work, or `feat(w02): w02-s05` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S06 — Derive tenant and identity only from the user (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/MULTI-TENANCY.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S05 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/java/com/hrflow/identityorg/auth/AccessTokens.java` `services/identity-org/src/test/java/com/hrflow/identityorg/auth/AccessTokensTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Add tenant/employee/role claims from the authenticated persisted user. Test that body/path/client-header tenant/roles cannot override the record. Finish the first stone only when its complete login tests are green.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Token tenant equals the fixture user tenant; spoofed tenant/roles do not change claims.

**Common mistake:** Copying tenant or roles from the login request into signed claims.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S06` for evidence-only work, or `feat(w02): w02-s06` for the isolated implementation change.
- [ ] Tagged `w02-s06-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Stone 2 — Gateway negatives and refresh family

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W02-S07 — Reject missing tokens and forged headers (45–45 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/REACTIVE-GATEWAY.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S06 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/api-gateway/src/main/java/com/hrflow/gateway/IdentityFilter.java` `services/api-gateway/src/test/java/com/hrflow/gateway/IdentityFilterTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect reactive hooks; strip incoming identity headers, validate bearer public-key JWT, add trusted principal fields only after verification. Test protected route without/invalid token and forged X-Tenant-Id. Keep service ports unpublished.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/api-gateway/pom.xml test
```

**Expect:** Anonymous/invalid token is 401 with problem shape; forged caller tenant header never reaches a business handler.

**Common mistake:** Appending trusted identity headers without stripping every caller-supplied copy.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S07` for evidence-only work, or `feat(w02): w02-s07` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S08 — Make unknown users and wrong passwords indistinguishable (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/JWT-REFRESH.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S07 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/java/com/hrflow/identityorg/auth/LoginService.java` `services/identity-org/src/test/java/com/hrflow/identityorg/auth/LoginTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Add both negative cases with identical status/code/body shape; use dummy password verification for missing credentials and avoid exposing account state. Do not promise constant network timing; test both paths invoke an equivalent hash check.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Both cases return the same safe 401 shape and no account-existence detail.

**Common mistake:** Returning different account-existence errors for the two credential failures.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S08` for evidence-only work, or `feat(w02): w02-s08` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S09 — Throttle repeated login attempts (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/JWT-REFRESH.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S08 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/java/com/hrflow/identityorg/auth/LoginLimiter.java` `services/identity-org/src/test/java/com/hrflow/identityorg/auth/LoginLimiterTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Add bounded per-source/account attempt limits with expiry and a fixed-clock test. Use only trusted proxy address configuration, not arbitrary forwarded headers. Return 429/Retry-After; single-process limits are an honest local-deployment limit.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Threshold attempt receives 429, expiry permits retry, map has a bounded eviction policy and no plaintext login data is logged.

**Common mistake:** Trusting caller-controlled forwarded IP headers or using an unbounded limiter map.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S09` for evidence-only work, or `feat(w02): w02-s09` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S10 — Add the hashed refresh-family migration (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/FLYWAY.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S09 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/resources/db/migration/V3__refresh_families.sql` `services/identity-org/src/test/java/com/hrflow/identityorg/auth/RefreshSchemaTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect next unused migration version first. Persist only refresh-token hash, tenant/user/family IDs, expiry/revoked/consumed metadata and constraints/indexes. Verify clean and upgrade migrations against real PostgreSQL. No raw refresh token is stored.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Fresh/upgrade migration tests pass and DB rejects an invalid foreign tenant/user reference within the owner schema.

**Common mistake:** Storing raw refresh tokens instead of hashes.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S10` for evidence-only work, or `feat(w02): w02-s10` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S11 — Rotate refresh tokens transactionally (60–60 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/DATABASE-LOCKS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S10 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/java/com/hrflow/identityorg/auth/RefreshService.java` `services/identity-org/src/test/java/com/hrflow/identityorg/auth/RefreshTest.java` `services/identity-org/src/main/java/com/hrflow/identityorg/auth/LoginService.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Issue the initial random refresh token at successful login; hash it in DB. Lock the presented token/family, mark consumed and insert successor in one transaction. Add red/green tests that one rotation succeeds and consumed predecessor cannot mint two successors.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** A valid rotation returns new tokens; transaction rollback does not consume the old token; concurrent rotation cannot issue two active successors.

**Common mistake:** Consuming the old token outside the transaction that inserts its successor.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S11` for evidence-only work, or `feat(w02): w02-s11` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S12 — Revoke the family on refresh reuse (60–60 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/DATABASE-LOCKS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S11 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/java/com/hrflow/identityorg/auth/RefreshService.java` `services/identity-org/src/test/java/com/hrflow/identityorg/auth/RefreshTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Present a consumed predecessor after a successful rotation. Revoke the whole family atomically and test that its current successor is rejected. Preserve only safe security audit metadata. No reuse is treated as a successful idempotent refresh.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Replay rejects predecessor and successor after family revocation; no second usable token escapes the race test.

**Common mistake:** Rejecting a reused predecessor while leaving its current successor usable.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S12` for evidence-only work, or `feat(w02): w02-s12` for the isolated implementation change.
- [ ] Tagged `w02-s12-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Continue

[Next build part](BUILD-GUIDE-PART-2.md).
