# Week 02 build guide part 4

> **Navigation:** [Overview](README.md) · [Dashboard](../YOU-ARE-HERE.md) · [Golden slice](../GOLDEN-SLICE.md)

## Stone 4 — Error and integrity boundaries

**You will have:** Allowed-origin matching CSRF request rotates once; missing/mismatched/hostile requests return safe 403 and no rotation. Cookie flags/path and no refresh in browser response are asserted.

### W02-S16 — Unify authentication failure problems (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/PROBLEM-DETAILS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/identity-org/src/main/java/com/hrflow/identityorg/auth/AuthProblemHandlers.java`, `services/identity-org/src/test/java/com/hrflow/identityorg/auth/AuthProblemTest.java`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Wire authentication/authorization handlers to the shared problem contract. Assert 401, 403, 429 and malformed login errors; exclude stack traces, raw credentials and enumeration messages. Compare reactive gateway errors too.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** All negative auth responses use application/problem+json and stable codes; no raw token/password/stack is present.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Maintaining a second mock schema lets mobile/server shapes drift from the sole OpenAPI file.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w02): w02-s16` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W02-S17 — Log security outcomes without credentials (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/OBSERVABILITY.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/identity-org/src/main/java/com/hrflow/identityorg/auth/LoginService.java`, `services/identity-org/src/test/java/com/hrflow/identityorg/auth/SecurityLogTest.java`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Add structured outcome/correlation/technical tenant fields and bounded failure/refresh-reuse counters. Test with sentinel password/token strings and assert neither appears in captured logs; tenant UUID follows approved retention/access policy.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Outcome/trace fields exist; sentinel secrets and personal identifiers are absent; metric labels do not contain user/tenant IDs.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Revoking only the reused predecessor leaves its successor usable; assert the complete family is rejected.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w02): w02-s17` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W02-S18 — Publish auth examples from the one contract (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/CONTRACT-TESTS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `contracts/openapi.yaml`, `docs/validation/W02-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Update login/refresh/logout/profile examples and bearer/security rules using implemented tests. For Nuxt specify the cookie/CSRF adapter contract and memory-only access token; the next ordered source/test steps implement it, so examples alone do not prove security; header choice controls token transport only, never tenant/role authority. Mobile uses platform secure storage, never normal preferences. Record request collection import from this contract, not a second schema.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
git diff -- contracts/openapi.yaml
```

**Expect:** Contract validates; safe examples contain placeholders only; web CSRF and mobile storage acceptance cases are listed.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Cookie flags alone do not stop CSRF; assert exact Origin and matching CSRF token/header before rotation.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w02): w02-s18` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W02-S19 — Read only the authenticated own profile (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/MULTI-TENANCY.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/identity-org/src/main/java/com/hrflow/identityorg/api/ProfileController.java`, `services/identity-org/src/test/java/com/hrflow/identityorg/api/ProfileTest.java`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect actual trusted principal/user repository. Add proposed GET /api/v1/me returning only safe own user/employee-link fields via tenant-scoped JDBC; no caller user/tenant selector. Test missing identity, foreign identifier injection and inactive/unlinked user safe states.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Own profile is 200 with no password/refresh data; anonymous is 401 and foreign selector never changes returned identity.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Checking only object lookup while leaving list/count/job queries unscoped still leaks another tenant.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w02): w02-s19` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W02-S20 — Protect browser refresh and logout with CSRF (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/BROWSER-CSRF.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/identity-org/src/main/java/com/hrflow/identityorg/auth/BrowserTokenTransport.java`, `services/identity-org/src/test/java/com/hrflow/identityorg/auth/BrowserTokenTransportTest.java`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Create a browser transport adapter around the existing login/refresh/logout service: HttpOnly Secure SameSite refresh cookie with narrow path; return access token only in body. Check exact Origin allowlist and a server-issued CSRF token/header on cookie-authenticated refresh/logout; test missing/mismatched token and hostile Origin. Mobile explicit JSON refresh uses existing secure storage. Transport selection never supplies authority and browser refresh token never appears in its JSON response.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Allowed-origin matching CSRF request rotates once; missing/mismatched/hostile requests return safe 403 and no rotation. Cookie flags/path and no refresh in browser response are asserted.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Cookie flags alone do not stop CSRF; assert exact Origin and matching CSRF token/header before rotation.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w02): w02-s20` after staging only exact reviewed paths and recording command/exit/report evidence.
- [ ] Tagged `w02-s20-green` after the [ten-minute review](../CODE-REVIEW-CHECKLIST.md), all stone assertions and current private app startup pass; never overwrite an old tag.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

## Continue

[Next part](BUILD-GUIDE-PART-5.md).
