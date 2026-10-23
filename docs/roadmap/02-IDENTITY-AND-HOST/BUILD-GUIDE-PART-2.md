# Week 02 build guide part 2

> **Navigation:** [Overview](README.md) · [First guide](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md)

## Stone 3 — Logout roles and tenant boundary

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W02-S13 — Revoke refresh credentials on logout (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/JWT-REFRESH.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S12 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/java/com/hrflow/identityorg/auth/LogoutController.java` `services/identity-org/src/test/java/com/hrflow/identityorg/auth/LogoutTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Add logout family revocation using verified identity and tenant scope. Test repeated logout is harmless, another tenant family cannot be revoked, and access tokens expire within the documented short lifetime.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Logout returns documented 204 without body; refresh is denied afterward and foreign family is unaffected.

**Common mistake:** Revoking a refresh family without checking its owner and tenant.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S13` for evidence-only work, or `feat(w02): w02-s13` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S14 — Enforce method and resource roles (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/MULTI-TENANCY.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S13 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/java/com/hrflow/identityorg/auth/MethodSecurity.java` `services/identity-org/src/test/java/com/hrflow/identityorg/auth/AuthorizationTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect actual service methods; enable method checks and build the service principal only from the verified gateway boundary. Assert employee cannot perform HR operation and unrelated-manager resource is hidden. Do not merely hide a UI button.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Visible forbidden action returns 403; hidden foreign resource returns 404; permitted HR path succeeds.

**Common mistake:** Protecting only controller buttons while leaving application methods unrestricted.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S14` for evidence-only work, or `feat(w02): w02-s14` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S15 — Register every auth route in the tenant suite (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/MULTI-TENANCY.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S14 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/test/java/com/hrflow/identityorg/auth/TenantBoundaryTest.java` `contracts/openapi.yaml`. At most these three files change; reading dependencies is not permission to edit them all.
1. Repeat the golden slice with delta: auth bootstrap exception and tenant-scoped profile/logout/refresh. Match implemented route registry to OpenAPI; test tenant B IDs, foreign-family revocation, forged headers and direct service ingress. Do not call private Docker network cryptographic identity.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Every implemented protected auth route has a negative tenant/provenance assertion; missing registry entry fails the test.

**Common mistake:** Adding a new protected route without registering its cross-tenant cases.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S15` for evidence-only work, or `feat(w02): w02-s15` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S16 — Unify authentication failure problems (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/PROBLEM-DETAILS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S15 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/java/com/hrflow/identityorg/auth/AuthProblemHandlers.java` `services/identity-org/src/test/java/com/hrflow/identityorg/auth/AuthProblemTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Wire authentication/authorization handlers to the shared problem contract. Assert 401, 403, 429 and malformed login errors; exclude stack traces, raw credentials and enumeration messages. Compare reactive gateway errors too.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** All negative auth responses use application/problem+json and stable codes; no raw token/password/stack is present.

**Common mistake:** Leaving security-filter errors outside the shared problem contract.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S16` for evidence-only work, or `feat(w02): w02-s16` for the isolated implementation change.
- [ ] Tagged `w02-s16-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Stone 4 — Safe logs contract and clients

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W02-S17 — Log security outcomes without credentials (30–30 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/OBSERVABILITY.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S16 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `services/identity-org/src/main/java/com/hrflow/identityorg/auth/LoginService.java` `services/identity-org/src/test/java/com/hrflow/identityorg/auth/SecurityLogTest.java`. At most these three files change; reading dependencies is not permission to edit them all.
1. Add structured outcome/correlation/technical tenant fields and bounded failure/refresh-reuse counters. Test with sentinel password/token strings and assert neither appears in captured logs; tenant UUID follows approved retention/access policy.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Outcome/trace fields exist; sentinel secrets and personal identifiers are absent; metric labels do not contain user/tenant IDs.

**Common mistake:** Logging the password, JWT, refresh token or high-cardinality metric identity.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S17` for evidence-only work, or `feat(w02): w02-s17` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S18 — Publish auth examples from the one contract (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/CONTRACT-TESTS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S17 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `contracts/openapi.yaml` `docs/validation/W02-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Update login/refresh/logout/profile examples and bearer/security rules using implemented tests. For Nuxt choose HttpOnly Secure SameSite refresh cookie with CSRF defense; access token stays in memory. Mobile uses platform secure storage, never normal preferences. Record request collection import from this contract, not a second schema.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
git diff -- contracts/openapi.yaml
```

**Expect:** Contract validates; safe examples contain placeholders only; web CSRF and mobile storage acceptance cases are listed.

**Common mistake:** Treating cookie flags as a replacement for implemented CSRF protection.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S18` for evidence-only work, or `feat(w02): w02-s18` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S19 — Review the complete auth test ladder (30–30 min)

**Why:** A and B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/CODE-REVIEW.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S18 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W02-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Run the shared self-review and all auth/gateway tests including wrong password, throttling, expiry, reuse, roles, tenant/provenance and logs. If cookie/CSRF transport is not implemented, record a critical blocker; do not tag a working backend as completed web security.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -pl services/identity-org,services/api-gateway -am test
```

**Expect:** All named auth cases have actual test reports; any absent required case keeps the stone/gate failed.

**Common mistake:** Tagging auth green while required browser security behavior is still unbuilt.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S19` for evidence-only work, or `feat(w02): w02-s19` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S20 — Test the mobile credential lifecycle (45–45 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/JWT-REFRESH.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S19 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `mobile/lib/auth/session.dart` `mobile/test/auth_session_test.dart` `docs/validation/W02-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect actual mobile root first. Add the minimal platform secure-storage adapter and one fixture proving single-flight refresh, stale-session logout once and no token in ordinary preferences/logs. Use the locked compatible secure-storage package after inspecting current platform configuration; record any native entitlement blocker. Nuxt cookie/CSRF implementation remains explicitly required, not satisfied by these mobile tests.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
flutter test
```

**Expect:** Selected mobile auth tests pass from confirmed root; refresh failure prompts one login without an infinite loop or token persistence leak.

**Common mistake:** Persisting refresh tokens in ordinary preferences or retrying refresh concurrently.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S20` for evidence-only work, or `feat(w02): w02-s20` for the isolated implementation change.
- [ ] Tagged `w02-s20-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Continue

[Next build part](BUILD-GUIDE-PART-3.md).
