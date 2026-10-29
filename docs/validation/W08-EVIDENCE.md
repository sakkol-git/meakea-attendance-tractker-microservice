# Week 08 local candidate evidence

Date: 2026-10-27. Owner week: 08 candidate; verified roadmap position: W02-S26. This is an early local candidate for the 2026-12-14 to 2026-12-18 week, not a Week 08 gate result. Root, web and mobile changes are uncommitted in separate repositories. The intended Acer Nitro 5 and encrypted external backup disk are unavailable for the required deploy and restore gate.

## Accepted intent and current behavior

- Canonical contract alignment: All implemented routes in `services/identity-org` are validated against `contracts/openapi.json`. Undocumented response properties and schema drift trigger regression failures (`CompareOpenapiLiveResponsesTest`).
- Nuxt role navigation and server enforcement: Client navigation is scoped by role (Employee, Manager, HR Admin). Hidden navigation never bypasses server role/resource checks (`tests/unit/navigation.test.ts`), which fail closed with 403 Forbidden.
- Staged API configuration: Release configurations reject implicit demo mode and unversioned/unsafe API URLs (`tests/build-config.test.mjs`, `test/configuration_test.dart`).
- Refresh token concurrency and revocation: Parallel refresh requests serialize and reissue credentials without race conditions. Revoked or consumed refresh tokens immediately invalidate their entire session family (`AuthSessionServiceImplTest`, `test/session_test.dart`).
- Queued command durability: Offline-created mutations persist unique idempotency keys and request bodies across process restarts. Replaying commands after reconnecting preserves idempotency keys; stale version conflicts (412) and payload mismatches (409) require explicit user review without silent overwrite (`test/pending_commands_test.dart`, `tests/data.test.ts`).
- Failure recovery and accessibility: Lost responses after commit allow explicit retry with the same key without duplicate ledger effects. Web forms manage keyboard focus, returning focus to invalid inputs and exposing accessible error labels (`tests/unit/workspace.test.ts`, Playwright E2E). Mobile forms render contrast-compliant layouts, accessible at 360px width with 200% text scale (`test/visual_test.dart`, `test/acceptance/enterprise_regressions_test.dart`).
- Cross-client consistency: Web and Flutter clients observe identical state transitions across multi-role workflows. Denied optional permissions (location, push, biometrics) do not block manual authenticated workflows (`test/feature_workflows_test.dart`, `tests/e2e/workspace.spec.ts`).

## Stone 1 verification (W08 Stone 1, W08-S01–S06)

- Steps: `W08-S01` to `W08-S03` (compare OpenAPI and live responses: schema validation against `contracts/openapi.json`, rejection of undocumented properties) and `W08-S04` to `W08-S06` (Nuxt role-based feature navigation and server authorization enforcement).
- Targets: `services/identity-org/src/test/java/com/hrflow/identityorg/CompareOpenapiLiveResponsesTest.java`, `web/tests/unit/navigation.test.ts`.
- Verified behaviors: live profile response conforms to `UserProfile` schema in canonical OpenAPI; undocumented response properties trigger contract drift assertion failure; navigation items correspond to employee, manager, and HR admin roles; direct invocation without required role returns 403 Forbidden.
- Focused verification - backend: `./mvnw test -pl services/identity-org -Dtest="CompareOpenapiLiveResponsesTest"` exited 0 (2 tests, 0 failures, 0 errors, 0.106s).
- Focused verification - web: `npm --prefix web run test:unit tests/unit/navigation.test.ts` exited 0 (4 tests, 0 failures, 0 errors, 106ms).

## Stone 2 verification (W08 Stone 2, W08-S07–S09)

- Steps: `W08-S07` to `W08-S09` (connect Flutter to staged API: environment base URL configuration outside secrets, expired session recovery, rejection of unversioned URLs).
- Targets: `mobile/test/configuration_test.dart`, `mobile/test/session_test.dart`, `web/tests/build-config.test.mjs`.
- Verified behaviors: release configuration refuses implicit local data and requires HTTPS versioned API URL; expired session clears cached credentials and prompts re-authentication; wrong environment URLs fail gracefully without credential leakage.
- Focused verification - web config: `node --test web/tests/build-config.test.mjs` exited 0 (2 tests, 0 failures, 0 errors, 67ms).
- Focused verification - mobile config & session: `.fvm/flutter_sdk/bin/flutter test test/configuration_test.dart test/session_test.dart` exited 0 (11 tests, 0 failures, 0 errors, 4.0s).

## Stone 3 verification (W08 Stone 3, W08-S10–S12)

- Steps: `W08-S10` to `W08-S12` (test refresh concurrency, single-use rotation, family revocation, and short access expiry window).
- Targets: `services/identity-org/src/test/java/com/hrflow/identityorg/auth/service/AuthSessionServiceImplTest.java`, `services/identity-org/src/test/java/com/hrflow/identityorg/auth/service/RefreshTokenServiceImplTest.java`, `services/identity-org/src/test/java/com/hrflow/identityorg/auth/RefreshSessionUpgradeIT.java`, `mobile/test/session_test.dart`.
- Verified behaviors: parallel refresh requests serialize correctly; reuse of consumed refresh token immediately revokes entire session family; logout revokes only the presented session family; database schema upgrade preserves tenant ownership.
- Focused verification - backend: `TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw test -pl services/identity-org -Dtest="AuthSessionServiceImplTest,RefreshTokenServiceImplTest,RefreshSessionUpgradeIT"` exited 0 (8 tests, 0 failures, 0 errors, 4.311s) against Testcontainers PostgreSQL 18.6.
- Focused verification - client: `.fvm/flutter_sdk/bin/flutter test test/session_test.dart` verified parallel refresh serialization and family revocation.

## Stone 4 verification (W08 Stone 4, W08-S13–S15)

- Steps: `W08-S13` to `W08-S15` (test queued command durability: offline command storage, process restart recovery, replay idempotency, conflict/stale error classification).
- Targets: `mobile/test/pending_commands_test.dart`, `web/tests/data.test.ts`.
- Verified behaviors: queued mutation survives process restart and reuses stable idempotency key; editing body produces a new key; logout wipes pending command keys; write failures distinguish conflict (412), validation failure (422), and unknown outcome (0/timeout); lost response after commit replays receipt safely.
- Focused verification - mobile: `.fvm/flutter_sdk/bin/flutter test test/pending_commands_test.dart` exited 0 (5 tests, 0 failures, 0 errors, 3.5s).
- Focused verification - web: `node --test web/tests/data.test.ts` exited 0 (10 tests, 0 failures, 0 errors, 168ms).

## Stone 5 verification (W08 Stone 5, W08-S16–S21)

- Steps: `W08-S16` to `W08-S18` (Nuxt timeout and stale state handling) and `W08-S19` to `W08-S21` (accessible keyboard focus, form error labels, contrast, text scaling).
- Targets: `web/tests/unit/workspace.test.ts`, `mobile/test/visual_test.dart`, `mobile/test/adaptive_layout_test.dart`.
- Verified behaviors: timeout after commit refetches canonical state rather than optimistically finalizing balances; stale version (412) prompts user refresh; keyboard focus returns to invalid input on submission error; mobile views render without overflow at 360px width with 200% text scale.
- Focused verification - web unit: `npm --prefix web run test:unit tests/unit/workspace.test.ts` exited 0 (17 tests, 0 failures, 0 errors, 38ms).
- Focused verification - full web unit suite: `npm --prefix web run test:unit` exited 0 (17 test files, 93 tests, 0 failures, 0 errors, 846ms).
- Focused verification - mobile layout & visual: `.fvm/flutter_sdk/bin/flutter test test/visual_test.dart test/adaptive_layout_test.dart` exited 0.

## Stone 6 verification (W08 Stone 6, W08-S22–S27)

- Steps: `W08-S22` to `W08-S24` (review permissions and phone limits: denied location/push/biometrics do not break manual check-in) and `W08-S25` to `W08-S27` (cross-client acceptance story: consistent outcome across web and mobile).
- Targets: `mobile/test/feature_workflows_test.dart`, `mobile/test/acceptance/enterprise_regressions_test.dart`, `web/tests/e2e/workspace.spec.ts`.
- Verified behaviors: denied optional permissions leave authenticated manual workflows operable; cross-client acceptance verifies employee submission on one client and manager approval on another reflect consistent state; 12 Playwright E2E scenarios pass across viewports.
- Focused verification - mobile workflows: `.fvm/flutter_sdk/bin/flutter test test/feature_workflows_test.dart` exited 0.
- Focused verification - web E2E: `npm --prefix web run test:e2e` exited 0 (12 tests, 0 failures, 1.1m).
- Full mobile suite: `.fvm/flutter_sdk/bin/flutter test` exited 0 (155 tests passed, 1 live test skipped by default, 18.0s).

## Stone 7 verification (W08 Stone 7, W08-S28–S30)

- Steps: `W08-S28` (reproduce earliest failed check); `W08-S29` (recheck client and contract negatives: 401 unauthenticated, 403 forbidden, 409 conflict, 412 stale version, 422 unprocessable); `W08-S30` (review carry-forward and reserve).
- Targets: `contracts/openapi.json`, `docs/validation/W08-EVIDENCE.md`, root/web/mobile diffs.
- Verified behaviors: OpenAPI contract validation; contract compatibility against `origin/main`; git diff check clean across root, web, and mobile repositories; documentation lint passed.
- Diff check: `git diff --check && git -C web diff --check && git -C mobile diff --check` exited 0.
- Contract validation: `python3 scripts/verify-project.py --scope foundation` exited 0 (177 operations, 6 contract tests).
- Tooling verification: `python3 scripts/verify-project.py --scope tooling` exited 0 (23 tests passed).
- Web full verification: `python3 scripts/verify-project.py --scope web` exited 0 (bundle check, typecheck, lint, unit tests, 12 Playwright E2E scenarios).
- Mobile full verification: `python3 scripts/verify-project.py --scope mobile` exited 0 (bundle check, flutter analyze, 155 tests passed).
- Documentation lint: `python3 docs/roadmap/tools/docs-lint.py docs README.md AGENTS.md contracts/README.md tools/project_mcp/README.md` exited 0 (317 Markdown files, 0 errors, 0 warnings).

## Stone 8 assessment & physical blockers (W08 Stone 8, W08-S31–S33)

- Steps: `W08-S31` (redeploy checked service candidate via `ops/deploy.sh`); `W08-S32` (restore release backup in scratch via `ops/restore-scratch.sh`); `W08-S33` (decide gate).
- Physical blocker - deploy: `ops/deploy.sh` requires the dedicated Acer Nitro 5 private host, which remains physically unavailable in this development environment.
- Physical blocker - restore: `ops/restore-scratch.sh` requires the encrypted external backup disk, which remains physically disconnected.
- Gate decision: `BLOCKED`. The Week 08 release gate remains **NOT RUN**. Verified roadmap position remains `W02-S26` in `docs/roadmap/YOU-ARE-HERE.md`. No weekly gate is claimed passed.

## Executed checks

| Check | Result | Limit |
| --- | --- | --- |
| OpenAPI response comparison and Nuxt navigation, `CompareOpenapiLiveResponsesTest`, `navigation.test.ts` | Exit 0 (6 tests); profile response validated against schema, undocumented properties rejected, role-based navigation validated | In-memory comparison; synthetic tokens |
| Client configuration and session expiry, `configuration_test.dart`, `session_test.dart`, `build-config.test.mjs` | Exit 0 (13 tests); safe URL enforcement, demo mode rejection in release, expired session cleanup | Headless Dart and Node runtimes |
| Refresh token concurrency and revocation, `AuthSessionServiceImplTest`, `RefreshTokenServiceImplTest`, `RefreshSessionUpgradeIT` | Exit 0 (8 tests); family revocation, single-use tokens, parallel refresh serialization | Testcontainers PostgreSQL 18.6 |
| Queued command durability, `pending_commands_test.dart`, `data.test.ts` | Exit 0 (15 tests); offline persistence across restart, key stability, conflict error classification | In-memory secrets; simulated network |
| Accessible focus and error display, `workspace.test.ts`, `visual_test.dart` | Exit 0 (17 tests web, full visual suite mobile); focus management, 360px responsive layout, 200% text scale | Headless Chromium and Flutter |
| Cross-client consistency, `feature_workflows_test.dart`, Playwright E2E | Exit 0 (12 E2E scenarios, 155 mobile tests); consistent multi-role workflow outcomes, phone limits | Local browsers and Flutter test runner |
| Foundation scope, `python3 scripts/verify-project.py --scope foundation` | Exit 0; 177-operation OpenAPI validation, 6 contract tests, docs lint, and diff check passed | Automated local baseline only |
| Web scope, `python3 scripts/verify-project.py --scope web` | Exit 0; 93 unit tests, bundle check, typecheck, lint, 12 Playwright E2E scenarios, and diff check passed | Local Chromium browser |
| Mobile scope, `python3 scripts/verify-project.py --scope mobile` | Exit 0; 155 tests passed, Flutter analyze clean, bundle check, and diff check passed | Headless Flutter runtime |
| Tooling scope, `python3 scripts/verify-project.py --scope tooling` | Exit 0; 23 tests passed across MCP server, test runner, and workspace traversal | Local Python virtual environment |

## Boundary review

- Backend contract alignment: `services/identity-org` routes conform strictly to `contracts/openapi.json`.
- Web client: Nuxt 3 client in `web/` provides responsive, role-scoped navigation with CSRF and Bearer token handling.
- Mobile client: Flutter client in `mobile/` provides offline command queueing with stable idempotency keys, safe session recovery, and accessible adaptive layouts.
- HTTP contract: Sole source of truth remains `contracts/openapi.json`.
- Authentication & Authorization: OAuth2 Resource Server validates tokens. Reused or expired refresh tokens immediately revoke session families.
- Concurrency & Idempotency: Client pending commands preserve exact UUID keys across restarts; server rejects payload alterations under existing keys with 409 Conflict.
- Accessibility: 360px viewport support, 200% text scaling, dark mode, accessible focus return on error, and keyboard skip links verified.
- Documentation: `docs-lint.py` passes 317 Markdown files with 0 errors and 0 warnings.

## Disposition

`CANDIDATE — LOCALLY VERIFIED`

Local implementation and tests across foundation, backend, web, mobile, and tooling are green. The Acer Nitro 5, encrypted external backup disk, and physical device gates remain blocked. No weekly gate is claimed passed.

## Client UX implementation checkpoint — 29 October 2026

The user requested application of the reviewed UI improvements to both clients. This checkpoint supersedes earlier client appearance snapshots only. Backend code-readiness evidence and manual host/device/business gates retain their original scope. Backend revision is `2ad32602d686c8134492aa4d40146562c52a879e`; web `d1db592ae67c472c06b9fe7298c6773ad3bf3f22`; native `6066d91d711d7652f49d08edc1acf6ea0c60ae7a`. All three working trees contain preserved, uncommitted user changes.

### Implemented presentation changes

- Grouped web navigation and a role-eligible Tasks page replace the everyday technical workflow catalog. All 40 demo journeys remain covered inside an initially collapsed, local-only developer catalog. Native Tasks omits the service-readiness group and uses Attendance/Tasks labels with parent-tab selection for detail routes.
- Web management roles receive a Team/HR/organization work card before personal attendance. Native role cards link directly to team attendance, people and owner audit. Failed approval reads stay unavailable rather than showing a fabricated zero.
- Web attendance records use phone cards with date, clock times, duration and text status. The summary is compact, date fields remain readable at 320px, and the open-session indicator reads unfiltered history. Desktop tables retain pagination.
- Forms use consistent Leave/Attendance correction wording and result-specific messages. Dialog actions stay visible; native FormActions sits outside the scrolling fields and above the open keyboard. Existing request-draft, validation, ETag, command and unsaved-change controls are preserved.
- Missing approval context is unknown; client absence of team or stage data is not asserted as zero. Task reference cards display names, with raw source paths/identifiers removed from the normal card. Success notifications replace previous success/snackbar feedback.
- Browser location is an explicit choice, with permission recovery text and browser-estimate labels. Invalid/missing site coordinates and radius no longer produce a fabricated office or verified result. The shell no longer opens the unsupported optional SSE route or describes it as workspace health.
- The shared component matrix and remaining localization/manual-accessibility phase are recorded in `web/docs/MOBILE-ALIGNMENT.md`; client integration guides describe the new display behavior.

### Acceptance and regression evidence

Given a signed-in employee, opening Tasks presents eligible workday actions while technical processes remain hidden in the collapsed demo catalog. Given a phone attendance screen, each record shows its date, times, duration and state without horizontal scrolling. Given a 320px phone with enlarged text and a 260px keyboard, the request save action remains above the keyboard. Given no configured work-site boundary, a location estimate cannot be labeled verified.

- Navigation tests were added first. Native reproduced the old Activity/Work labels and wrong selected tab; the web test initially failed to import the not-yet-created task module, a compilation gate rather than a behavioral red. Green results: web navigation 4 cases; native navigation 3 cases.
- The geofence regression first failed because no configured sites returned `withinBoundary=true`. After repair all 6 geofence cases passed.
- The new keyboard assertion first failed at y=628 with a required maximum of y=380. FormActions now accounts for the keyboard inset; navigation/adaptive selection passes 5 cases.
- The first complete native run passed 177 cases, skipped 3 opt-in live cases and failed 12 intentional golden comparisons. All 12 changed light/dark screenshots were visually inspected before refreshing the macOS references. The complete rerun passed 189 cases, skipped 3, failed 0: `/tmp/hrflow-ux-mobile-final.log`. Analysis passed with no issues. The separate golden update/run passed all 16 visual cases.
- `npm run check` passed formatting, structure, type checking, 10 data cases, 2 configuration cases, 131 Vitest cases, demo production build and bundle budgets: `/tmp/hrflow-ux-web-final-check.log`.
- The first browser command encountered a concurrent Nuxt build lock and did not run tests. Its next run passed 17/18; adding a Reports shortcut made an old unscoped link selector ambiguous. The selector was restricted to Main navigation. The final stable-output browser run passed all 18 cases in 1.1 minutes: `/tmp/hrflow-ux-web-final-browser.log`. This includes all 40 demo journeys, accessibility scans, keyboard/focus checks, 320px attendance cards, role/task menus and controlled remote adapters. It does not establish a deployed backend journey.

### Boundaries and remaining acceptance

The last task-discovery review found that advanced web workflow routes remain contract-gated in remote mode. The remote task catalog now directs users to existing feature pages instead of advertising those provisional routes; its additional regression brings navigation coverage to 5 cases. The report browser journey also verifies the live Tasks page contains no BF links or developer catalog. Native task search excludes report recovery as well as service readiness. Its initial assertion incorrectly included the search field's own EditableText; it was narrowed to ListTile results and scrolled to the end, then passed (`/tmp/hrflow-ux-native-menu-final.log`).

Linux visual references use the pinned Flutter revision `abaf9c523780a608bd46686fd5e53740a07077f8` in an isolated local Ubuntu 24.04 amd64 container. Batch image previews appeared to omit dark hero contents, initially mistaken for a rendering defect. Original-resolution, individual previews showed complete contents. SHA-256 checks proved the supposedly incomplete and complete manager images were byte-identical (`89766c4d1600573cceb7451497f70d67bd52c462cf2b7c6441abbb4a4dcc7c35`), as were the HR images. A retained-layer rendering diagnosis was therefore unsupported. The exploratory test-only reassemble change was removed; its awaited version had stalled and was stopped. No production rendering, assertion or golden tolerance was changed to accommodate the preview issue.

Linux reference generation/comparison was also checked per role/theme process: 14 visual cases, 2 separate text-scale/dark-layout cases, then 173 nonvisual cases with 3 opt-in skips (`/tmp/hrflow-ux-linux-isolated.log`). Final references were individually inspected. The final unchanged visual test and complete native suite then pass together in Linux: 189 cases, 3 opt-in skips, zero failures (`/tmp/hrflow-ux-linux-verified.log`). The full macOS run also passes 189 cases with 3 skips (`/tmp/hrflow-ux-mobile-final.log`). Actual hosted CI and native device behavior remain separate acceptance; the preview observations do not establish an application rendering defect.

The HRFlow structured test tool returned a generic execution error for the initial web/native selections; verification therefore used the existing CLI suites. Foundation and `origin/main` compatibility passed (`/tmp/hrflow-ux-foundation.log`). Both contract bundles match, shared demo-data and 40-workflow inventory checks pass, and the final root/client guide lint passes 327 Markdown files (`/tmp/hrflow-ux-final-docs.log`). All three repository whitespace checks pass. CLIENT-UX-CHECKPOINT.json records the new client source/log hashes; earlier manifests remain dated evidence.

Web/native presentation, local navigation and their tests/guides changed. No HTTP route, payload, adapter mapping, tenant derivation, business transition, migration, Avro definition, runtime dependency or deployment setting changed. The previously recorded backend/provider evidence was not rerun for this display pass; fresh backend and tooling scopes are omitted. No API compatibility waiver or fake remote response was added.

Manual TalkBack/VoiceOver, native permission handling and representative-user/device UAT remain outstanding. Khmer localization remains a later content/implementation phase requiring a reviewed glossary and complete screen/error coverage; this English UI pass does not claim localization. Broader generic-editor decomposition, server/device release acceptance and final production readiness are not certified. No commit, push or deployment occurred.

## Client usability polish — 29 October 2026

The user asked to apply the prioritized UI/UX backlog and continue the code work. This checkpoint extends the existing English UAT client. The current coverage tracker is in `web/docs/MOBILE-ALIGNMENT.md`; it distinguishes implemented or retained behavior from pending localization and human/device acceptance. It does not promote the entire 32-area backlog to complete.

### Behavior and acceptance examples

- Given a request form with invalid inputs, when the user saves, then entries remain, a summary is announced and the first invalid interactive control is revealed. Composite web controls focus their actual input. Native error feedback stays beside the keyboard-safe action, with bounded scroll space.
- Given half-day leave on multiple dates or a range crossing a calendar year, when it is submitted, then the client explains the existing approved restriction before a write. Server validation remains authoritative.
- Given a valid leave form and a slow owner preview, when Send for approval is pressed, then the action shows Saving and prevents repeated submissions or edits. Preview failure restores the action and keeps the entered reason and dates.
- Given a list search with no matches, when Clear filters is used, then the unfiltered list returns. Native cancels any pending search timer. Web request/leave filters and paging survive detail navigation in memory, and sign-in, sign-out, expiry or demo-role changes clear them.
- Given an attendance correction, when the user chooses company-clock dates and times, then explicit instants are sent through the existing adapter. Overnight dates are preserved; invalid or ambiguous local times are rejected. Original sessions in the loaded history are shown with explicit unavailable/empty states. Users do not type UTC offsets.
- Given a phone correction dialog, when its last date/time field receives focus, then the complete field remains inside the scrolling content above the separate dialog footer. The new panel uses semantic text contrast.
- Given an explicit web appearance choice, when the browser reloads, then it is restored. Use device setting follows device changes; explicit Light or Dark overrides them. The existing theme system and a browser-local preference implement this without a new dependency or server-preference field.
- Given an empty unread filter, when Show all updates is used, then all loaded notices return. Unsupported legacy links are explained. Native read state is text as well as color/icon, and Mark all read is guarded while running and disabled when there are no loaded unread notices.
- Given saved offline information, then the mobile notice shows task names and the original update timestamp instead of technical paths or IDs. Existing offline decision denial remains tested.

Normal leave labels are aligned across primary lists, approval tabs, record details, reporting and home. Native count grammar and read-failure wording are corrected. Web home balances use the returned type, unit and year, with an explicit empty state; no pending zero is invented for an absent balance. Password visibility is available on web login, and native login errors expose a live region. Shared native loading and busy-button feedback respect reduced-motion settings. Replaced web confirmations leave the visual/accessibility tree immediately so an old fading toast does not overlap the next result.

The native draft-edit page was extracted into `edit_request_page.dart`. The owner form remains a reviewed large stateful editor and shrank below its existing 1019-line limit; neither the architecture test nor its limits were weakened. Larger feature-specific editor decomposition remains maintenance work. Web ResourceForm remains below its 300-line budget. New files have cohesive responsibilities: appearance preference/theme, list preferences, invalid-field focus, timezone input conversion and the draft-edit page.

### Failures and repair history

The HRFlow structured test tool returned a generic execution error for web/native selections, so the repository CLI suites were used. The initial leave test path was outside Vitest's configured unit directory and selected zero tests; moving it to `tests/unit` produced the intended two failing date-rule assertions before implementation. Correction and appearance tests first had missing-module setup failures, then passed their behavioral assertions after implementation; setup errors are not semantic red evidence.

The focused browser regressions initially failed for missing error-summary, Clear filters and password-visibility behavior. A follow-up exposed focus landing on a PrimeVue wrapper rather than its interactive child; the helper now selects the actual control. Password locators were made exact to distinguish the input from Show password. A malformed multiline template click expression caused one build failure and was replaced with a named clear action. The list-return test failed because its search was reset, then passed after memory-only view state was added and cleared on account changes.

Native focused checks reproduced the missing nearby validation summary, missing Clear filters action and missing busy state during a delayed leave preview. They passed after implementation. The first full visual run reported eight intended copy changes; the light/dark manager, HR, owner and leave references were reviewed and updated for macOS and Linux. An accessibility test's temporary semantics handle was initially disposed too late; it now disposes explicitly. Full analysis found two unused test imports and a braces warning after form reformatting; these were fixed.

The initial dark-appearance browser setup found an unnamed composite control, fixed with an explicit Appearance label. The full dark scan then exposed the demo strip, segmented tabs, people metadata and report-label contrast; semantic overrides fixed these. Individual original-resolution previews were used. The final small-phone visual review found a sticky footer covering a field, so ResourceForm actions were moved to the dialog footer outside its scrolling form. A later browser scan caught a fading, replaced success toast; leaving confirmations now hide immediately. Another browser run exceeded the overall test time during an unusually long role-selection click; the captured final state already had cleared filters. No timeout or assertion was relaxed.

Broad native runs also caught the old path-based saved-data wording assertion and form size growth. The assertion now expects Leave while retaining the exact timestamp and disabled-offline-approval checks. Extracting the draft-edit page repaired the size failure. No golden tolerance, security denial, command key, opaque ETag or contract check was weakened.

### Boundaries and limitations

Web and native presentation, local navigation preferences, validation feedback, tests and current guides changed. Client leave date hints reflect the already accepted calendar-year/half-day policy; they introduce no new HR rule. Correction pickers serialize the same explicit instants required by the existing contract. No HTTP route, DTO, adapter, server authorization, database migration, Avro schema, runtime dependency or deployment setting changed. Canonical contract compatibility and bundle freshness remain required checks. Backend/provider and tooling scopes were not rerun for this presentation-only change; prior evidence remains dated.

Complete English/Khmer message coverage, reviewed HR translations, native-speaker review, physical-device profiling, TalkBack/VoiceOver and representative-user observations remain outstanding. Hosted CI, an updated release APK, actual gateway/device acceptance and deployment were not performed. No commit, push or deployment occurred.

### Final executed checks

| Check | Result and evidence |
| --- | --- |
| Web `npm run check` | Passed format, structure, types, 10 data cases, 2 configuration cases, 139 unit cases in 34 files, demo production build and bundle budgets; `/tmp/hrflow-polish-web-complete-check.log` |
| Web `npm run test:e2e` | 23 scenarios passed, including 40 developer/demo journeys, controlled remote contracts, dark appearance across 13 core pages, field focus/footer clearance and single visible confirmation; `/tmp/hrflow-polish-web-complete-browser.log` |
| Web budgets | JavaScript 1,376,800/1,500,000 bytes; largest chunk 456,764/500,000; CSS 60,925/80,000; WOFF2 122,920/160,000 |
| Native `flutter analyze` | No issues; `/tmp/hrflow-polish-native-final-analysis2.log` |
| Native `flutter test --reporter expanded` | 194 passed, 3 explicit skips, zero failures on macOS; `/tmp/hrflow-polish-native-final-pass.log` |
| Linux full native suite | 194 passed, 3 explicit skips, zero failures in isolated local Ubuntu 24.04 amd64 Docker with pinned Flutter revision `abaf9c523780a608bd46686fd5e53740a07077f8`; `/tmp/hrflow-polish-linux-final-pass.log` |
| Native format | 214 files, zero changes required; `/tmp/hrflow-polish-native-format-final.log`. Final test-only formatting was followed by its passing 2-case accessibility run in `/tmp/hrflow-polish-native-accessibility-final.log` |
| Foundation verifier | `.venv-checks/bin/python scripts/verify-project.py --scope foundation --base origin/main` passed including compatibility, both bundle freshness checks, shared demo data, workflow inventory and documentation; `/tmp/hrflow-polish-final-foundation.log` |
| Documentation and whitespace | Root/current client guide lint and all three repository whitespace checks pass; `/tmp/hrflow-polish-final-docs.log` |

The skips require LIVE_AUTH_PASSWORD or disposable real-provider fixture URLs and role tokens. They are not counted as acceptance. Browser remote cases use controlled responses; the demo build is explicitly synthetic. Linux runs are local emulation, not hosted CI or a physical device. No golden comparison tolerance was changed. The Linux source mount and host SDK were read-only; temporary containers were removed after completion. An initial temporary run was stopped because its cache volume was mounted at the wrong path; reruns used the configured cache path without changing dependencies.

The final source/log receipt is CLIENT-UX-POLISH.json. It records current dirty source/test hashes, including preserved unrelated changes, and does not attribute the entire working tree to this pass. Earlier receipts are retained unchanged as history. Original-resolution desktop and phone previews are saved under `docs/validation/client-ux-polish/`. The broader verifier's backend and tooling scopes and fresh real-provider client journeys were intentionally omitted for this presentation-only pass.

## Focused forms and first use — 29 October 2026

The owner selected two additional improvements: focused form sections and an understandable first-use experience. The later instruction explicitly authorizes finishing verification, merging to main and pushing all three repositories for a weekend manual release. It supersedes the earlier no-commit/no-push constraint for this handoff only; deployment remains outside this task.

### Behavior and acceptance examples

- Given entered request details, when Review details is expanded and a field changes, then the summary follows the edit, other entries remain and no command is submitted. Both clients explain that a request is saved as a draft before submission from its details.
- Given a native workflow form, request details, materials and date/time inputs appear in focused components. The existing parent owns controllers, recovered drafts, dirty state, command identity and submission. Date pickers open within their supported bounds, and the end picker starts at the selected start date; cancelling does not overwrite entered values.
- Given remote mode without access, setup/password routes show HR account/workspace assistance rather than offering unsupported public setup or email recovery. Given demo mode, fictional setup remains available with explicit demo completion copy.
- Given a person starting work, public Help and getting started explains company workspace access, initial destinations, location permissions, saved drafts and unknown command outcomes. Sign-in and account/settings navigation link to it without weakening protected-route checks.

The owning boundaries are the accepted request/leave/correction rules, provisional contract status, BF003's designed-only onboarding boundary and the approved private linked-account setup in SERVICE-CONFIGURATION. No new public provisioning or password-recovery provider is claimed.

### Implementation and boundaries

Native request details, material controls and workflow date/time fields were extracted by responsibility. The parent form shrank from 985 to about 800 lines without increasing its reviewed size allowance; it remains a state coordinator with further maintenance work possible. Both clients add a read-only expandable review. Remote first-use navigation and help copy now match actual provider capability. No dependency, API, DTO, authorization rule, database migration, Avro schema or deployment setting changes for this UI slice. Earlier backend work is included in the coordinated handoff and receives a fresh full verifier run.

New first-use tests were observed failing for missing access guidance before implementation, then passed. Review tests were observed failing for missing summaries before implementation, then passed. A native focused run initially named a nonexistent correction test file; the corrected file selection passed five cases. An extraction analyzer run found a removed theme import and two braces warnings; these were repaired. The HRFlow MCP test runner returned an unstructured error, so pinned CLI tests provide the evidence. No skipped or partial run is counted as acceptance.

Personal documentation-editor state and generated native failure images are ignored and remain local. A staged secret scan's findings were inspected: source SHA-256 receipt values and the deliberate nonfunctional JWT negative fixture explain the initial hits. This is a bounded scan, not a security certification.

### Verification and release boundary

The coordinated command was `.venv-checks/bin/python scripts/verify-project.py --scope all --base origin/main`. It executed all five scopes but exited nonzero for two findings: DailyReportingKafkaIT could not create topics because its broker timed out during fixture setup, and the web format check rejected workspace-ux.css. The stylesheet was formatted; no behavior or test assertion was weakened. The original log is `/tmp/hrflow-release-verify.log`.

| Check | Final executed result |
| --- | --- |
| Backend | Every non-reporting reactor module passed in the full run. `HRFLOW_VERIFY_CLIENT_PROVIDERS=true ./mvnw --batch-mode -pl services/reporting -am verify` then passed the entire reporting slice, including DailyReportingKafkaIT and ClientProviderJourneyIT; `/tmp/hrflow-release-reporting-rerun.log`. Fresh XML reports across all modules total 486 cases, zero failures/errors/skips. The broker timeout's underlying cause is not established by the passing rerun. |
| Foundation and tooling | Contract validation, baseline compatibility, bundle freshness, shared demo data, workflow inventory, six foundation tests, documentation and 23 tooling tests passed in the coordinated run. |
| Web check | Final `npm run check` passed format, structure, types, 10 data cases, two configuration cases, 139 unit cases, demo build and budgets; `/tmp/hrflow-release-web-check.log`. |
| Web browser | 25 scenarios passed in the coordinated run, including new first-use and live review checks. Controlled responses are consumer evidence; ClientProviderJourneyIT supplies the separate real-provider evidence. |
| Native macOS | Analysis passed with no issues; 196 cases passed with three explicit live-environment skips in the coordinated run. |
| Native Linux | 196 cases passed with the same three explicit skips in local Ubuntu amd64 Docker; `/tmp/hrflow-release-linux.log`. This is not hosted CI or a physical device. |
| Native format | 221 files, zero changes required; `/tmp/hrflow-release-native-format.log`. |
| Native CI builds | Remote release APK and explicit demo web build passed using the repository CI arguments; `/tmp/hrflow-release-apk-build.log` and `/tmp/hrflow-release-mobile-web-build.log`. The APK uses example.invalid for build validation, is not a deployable company configuration and is not published. Android tooling emitted Kotlin migration and SDK XML version warnings. |
| Current documentation | Root documentation and the five updated client guides pass: 328 Markdown files, zero errors/warnings; `/tmp/hrflow-release-current-docs.log`. An optional broader root-style lint across every independent client document initially reported 218 style violations. The installation guide was normalized; unrelated legacy client documents were not rewritten. The failed broader invocation remains `/tmp/hrflow-release-docs.log`. |
| Dependency audit | `npm audit --audit-level=high` exited zero with seven low-severity advisories; `/tmp/hrflow-release-web-audit.log`. No force downgrade or dependency update was performed. |

Phone-width remote help and access screens were visually inspected; originals are in `client-forms-first-use/`. An initial preview attempt used a server that had already stopped and returned connection refused; a separately started production preview supplied the successful visual review. The preview was stopped afterward. Formatting removed trailing blank lines from new unpublished candidate files before initial publication; no previously released migration was edited.

CLIENT-FORMS-FIRST-USE.json binds final source/log hashes and the client commits for the coordinated handoff. Earlier CLIENT-UX-POLISH.json and CURRENT-UAT-CODE.json remain unchanged historical receipts. Real-device permissions, assistive technology, representative-user acceptance, Khmer localization, server setup and release/restore gates remain outstanding. A push is not deployment or stakeholder UAT acceptance, and historical roadmap gates are not advanced by this handoff.
