# Week 2 Evidence

## W02-S01

- Command: `mvn -f services/identity-org/pom.xml test`
- Result: 18/18 tests pass in 3.322 s; confirms identity-org module boundary, vertical slice subpackages (`config/`, `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`, `exception/`), strict SOLID interfaces (`TokenIssuer`, `TokenValidator`, `JwkProviderService`, `AttemptThrottlerService`, `RefreshTokenService`, `AuthSessionService`, `CookieHandlerService`), MapStruct, Lombok and test harness ready. **PASS**

## W02-S02 to W02-S05

- Files: `LocalAccount.java`, `SigningKeys.java`, `AccessTokens.java`, `AuthController.java`, `UserMapper.java`
- Command: `mvn -f services/identity-org/pom.xml test -Dtest=SigningKeysTest,UserMapperTest`
- Result: 4/4 tests pass; RSA keypair generated/loaded, JWKS format validated, access token claims signed and verified, DTO mapper contract verified. **PASS**

## W02-S06 to W02-S10

- Files: `AuthUser.java`, `AuthTenant.java`, `AttemptLimiter.java`, `V2__auth_sessions.sql`, `V3__auth_tenants.sql`, `V4__auth_attempts.sql`
- Command: `mvn -f services/identity-org/pom.xml test -Dtest=AttemptLimiterTest`
- Result: Distributed attempt throttling throttles brute-force logins with HTTP 429 after 5 failures. **PASS**
- Step W02-S07 (Gateway Gate 1): `CallerIdentitySanitizationFilter.java`, `SecurityConfiguration.java`, `AccessTokenDecoderTest.java`, `CallerIdentitySanitizationFilterTest.java`, `SecurityConfigurationTests.java`
- Command: `mvn -f services/api-gateway/pom.xml test`
- Result: 17/17 tests pass in 2.977 s; verifies ADR-010/011 Gate 1: `CallerIdentitySanitizationFilter` strips untrusted caller-supplied identity headers (`X-Tenant-Id`, `X-Employee-Id`, `X-Roles`, `X-Role`, `X-User-Id`), `AccessTokenDecoder` rejects wrong issuer, wrong audience, expired, and not-yet-valid tokens, and `SecurityConfiguration` returns RFC 7807 Problem JSON (`401 Unauthorized`, `GATEWAY-UNAUTHENTICATED`) for missing or malformed tokens. **PASS**

## W02-S11 to W02-S15

- Files: `AuthSessions.java`, `RefreshSession.java`, `SecurityConfiguration.java`, `MethodSecurity.java`, `AuthorizationTest.java`, `TenantBoundaryTest.java`
- Command: `mvn -f services/identity-org/pom.xml test`
- Result: 53/53 tests pass in 5.537 s; verifies ADR-010/011 Gate 2: `SecurityConfiguration` configures servlet Spring Security OAuth2 Resource Server DSL with `JwtAuthenticationConverter` mapping `"roles"` claim to `ROLE_*` authorities, `MethodSecurity` enforces `@PreAuthorize` role access (403 Problem JSON for unauthorized employee, 200 for HR Admin), `TenantBoundaryTest` verifies cross-tenant denial (404 Problem JSON for foreign tenant profile access), forged `X-Tenant-Id` headers have zero effect on tenant context, service-side rejected tokens return 401 Problem JSON, and cross-tenant refresh token revocation is enforced. **PASS**

## W02-S16 to W02-S20

- Files: `AuthCookies.java`, `SecurityProblems.java`, `AuthProblems.java`, `BrowserCors.java`
- Command: `mvn -f services/identity-org/pom.xml test -Dtest=AuthCookiesTest,SecurityConfigurationTests`
- Result: 5/5 tests pass; HttpOnly SameSite=Lax refresh cookie, XSRF-TOKEN CSRF cookie/header verification, RFC 7807 problem details on 401/403. **PASS**

## W02-S21 to W02-S22

- Files: `web/app/api/auth.ts`, `web/app/stores/session.ts`, `scripts/live-auth-smoke.py`, `contracts/postman/HRFlow_Complete_API.postman_collection.json`, `scripts/generate-postman-collection.py`
- Command: `npm test` (in web directory); `postman collection run contracts/postman/HRFlow_Complete_API.postman_collection.json -e contracts/postman/HRFlow_Local.postman_environment.json`
- Result: 14 test suites, 73/73 tests pass in web; Postman collection covering all 158 contract operations across 14 categories verified with dynamic token and secret extraction scripts. **PASS**
- Step W02-S22 (Complete Auth Test Ladder): `mvn -pl services/identity-org,services/api-gateway -am test`
- Result: 70/70 tests pass (17 gateway + 53 identity-org) across the complete multi-module reactor in 8.107 s; verifies ADR-010/011 dual-gate Resource Server integration across the complete authentication test ladder. **PASS**

## W02-S23

- Files: `mobile/lib/features/identity/data/session_client.dart`, `mobile/test/live_auth_test.dart`, secure-storage adapters and session tests.
- Commands: `fvm flutter analyze`; `fvm flutter test`; `fvm flutter test test/live_auth_test.dart --dart-define=LIVE_AUTH_PASSWORD=<local-only>`.
- Result: analysis reports no issues; 152 automated tests pass with one expected skipped live test in the default run; the separately enabled live test passes login, session restoration and logout through the gateway. The mobile profile editor now uses the implemented `PATCH /auth/profile` name/time-zone contract, and password validation matches the eight-character contract minimum. No token is persisted in ordinary client storage. **PASS** for automated acceptance.
- Limit: Android/iOS device keychain, biometric and entitlement behavior remains **NOT RUN** and is required before production readiness.

## Frontend-only client follow-up

- Web: added an HR-admin/owner audit screen with actor/action/target/date filters, paging, exact time-zone-aware timestamps, responsive layout, role navigation and an explicit synthetic-demo/provider-pending boundary.
- Web commands: `npm run check`; `npm run test:e2e`.
- Web result: formatting, structure budget, type checking, 8 data tests, 2 configuration tests, 89 unit tests, demo production build, bundle budgets and 12 Playwright scenarios pass. The browser suite includes audit filtering, responsive phone/tablet/desktop checks and WCAG A/AA automation. **PASS** for client behavior only.
- Mobile commands: `fvm flutter analyze`; `fvm flutter test`.
- Mobile result: analysis passes and 152 tests pass with one intentionally skipped live-gateway test. **PASS** for client behavior only.
- Boundary: `GET /audit-logs` remains `x-hrflow-implementation: pending`; client demo evidence does not establish provider authorization, tenant isolation, persistence or audit completeness.

## W02-S24

- Command: `df -h .`
- Baseline: `Filesystem Size Used Avail Capacity Mounted on /dev/disk3s1 228Gi 172Gi 22Gi 89% /System/Volumes/Data` (macOS Darwin arm64, 16 GiB RAM; development/test runner host only; not accepted as production host).
- Target inventory: Acer Nitro 5 (Intel Core i5 x86_64, 16 GB RAM, 2 TB disk).
- Resource budget: ~9.5–11.5 GB steady state (7 Java services @ 512–768 MB = ~4.5 GB, Kafka/KRaft = 1.5 GB, PostgreSQL = 1.0 GB, Caddy/Cloudflared = 0.2 GB, OS/buffers = 2.5–3.0 GB); leaves 4.5–6.5 GB headroom on 16 GB RAM.
- Storage profile: 2 TB reported capacity; mechanical HDD risk noted (low 4K random write IOPS creates latency spikes during PostgreSQL WAL fsync and Kafka broker segment flush); dedicated SSD recommended for DB/Kafka and container root.
- Runtime target: Headless Linux (Ubuntu Server 24.04 LTS x86_64) with native Docker Engine (CE) and systemd; Windows 10/11 Home with Docker Desktop (WSL2) rejected due to auto-reboot and standby sleep risks.
- Power policy: Continuous AC mains adapter; systemd configured with `HandleLidSwitch=ignore` to prevent laptop sleep upon lid closure.
- Ingress: Private LAN with static DHCP reservation (e.g., 192.168.1.x) and wired Ethernet (RJ45). Remote ingress via outbound Cloudflare Tunnel (`cloudflared`); no public router port forwarding.
- Backup: Dedicated external USB 3.0 encrypted disk (`ext4`); separate from host drive.
- Status: **BLOCKED**; target machine inventory, resource budget, disk IOPS risk and Cloudflare Tunnel access recorded in evidence; direct on-device command execution on the Acer Nitro 5 (`df -h .`, CPU/RAM/Docker verification) remains **NOT RUN** and is an explicit blocker for production host deployment.

## W02-S25

- Files: `infra/selfhost/compose.yaml`, `infra/selfhost/Caddyfile`
- Command: `docker compose -f infra/selfhost/compose.yaml config --quiet`
- Result: Exit 0. Validates production self-host manifest syntax and dual network isolation. Caddy binds edge ports 80/443; internal network has ZERO published database or broker ports (PostgreSQL 5432 and Kafka 29092 isolated). Caddyfile blocks `/actuator*` with HTTP 404, reverse proxies `/api/*` to `api-gateway:8080`, and serves static web assets with internal TLS (`tls internal`). Log rotation bounded to 10m/3 files per container. **PASS**

## W02-S26 to W02-S29 local preparation, 2026-10-26

- Self-host Compose now requires distinct runtime/migration credentials and an operator-mounted signing key pair, disables local key bootstrap, uses private loopback edge binding by default and moves gateway readiness to its configured management port. The Caddy fallback routes to a remote-mode Nuxt static bundle built from the separate web checkout because the earlier static path had no mounted `index.html`. `ops/common.sh` requires an operator-owned protected configuration, a named target/project and an encrypted external mount for backup/deploy/restore. `ops/provision-db-roles.sh` drafts separate runtime, migration and read-only backup roles.
- Identity signing now reads the configured issuer through constructor injection; the previous static-final annotation always emitted `hrflow-local` and would disagree with a configured gateway issuer. Given a custom issuer, the new regression first failed because the four-argument constructor did not exist; after the fix, `./mvnw -q -pl services/identity-org -am test -Dtest=JwkProviderServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` exited 0. The self-host manifest requires the same explicit issuer at both gates.
- `ops/deploy.sh`, `ops/backup.sh` and `ops/restore-scratch.sh` are local drafts. They refuse missing target confirmation, external mount and verified backup; backup writes an unverified receipt, and scratch restore refuses production-like names. The backup draft includes a custom dump, global-role metadata, private files/keys, schema/topic inventory, release images, Git bundle and checksums. No host command has executed.
- Local checks: `bash -n ops/*.sh` exited 0; `docker compose -f infra/selfhost/compose.yaml config --quiet` with dummy required variables exited 0; `bash ops/deploy.sh` without `HRFLOW_OPS_CONFIG` exited 1 as intended; `git diff --check` exited 0. These prove shell/config parsing and one refusal only, not database permissions, image recovery, deployment, rollback, a usable backup or restore.
- Local secret scan: Gitleaks 8.30.1 scanned all root, web and mobile histories with `gitleaks git --redact --no-banner --timeout 120 .`; all exited 0 after a narrow rule/path/line allowlist for the generated public SHA-256 receipt in each client. GitHub Actions secret scans passed on root `7b6ee1a`, web `a8a351f` and mobile `92e8a29`. [Gitleaks' official configuration guide](https://github.com/gitleaks/gitleaks/blob/master/README.md) documents the `AND` path/line allowlist, and the [official action guide](https://github.com/gitleaks/gitleaks-action) documents v3.
- `npm audit --omit=dev --json` in `web/` exited 1 and reported 7 high-severity advisories in the Nuxt/build dependency tree. Remote web `dependency-audit` failed for the same reason; its separate `verify` job passed. A local trial of `nuxt@4.1.3` raised 4 critical findings, so package and lockfile changes were reverted and `npm ci` restored the reviewed checkout. The candidate web image uses a generated static bundle served by Caddy, excluding the Node/Nitro runtime from the final image; this does not resolve or waive the dependency findings. A reviewed dependency fix or explicit gate decision remains open.
- Local web image proof: with dummy nonsecret Compose variables, `docker compose -f infra/selfhost/compose.yaml build web` exited 0; `docker run --rm -d --name hrflow-web-local-smoke -p 127.0.0.1:3300:3000 hrflow-selfhost-web:latest` started and `curl -fsS -o /dev/null -w '%{http_code}' http://127.0.0.1:3300/login` returned 200. The container was stopped. This proves the static candidate serves a login page locally, not Acer deployment, gateway authentication or release security acceptance.
- Verification checkpoint: `python3 scripts/verify-project.py --scope all --base origin/main` passed foundation, backend, web, mobile and tooling again after the standalone mobile test repair and parent workflow-parity check were added. The full run included 12 Playwright scenarios, 152 passing mobile tests with one skipped live test, 54 current identity unit/HTTP cases and six identity integration cases. Self-host Compose, Docker image, shell syntax, local page smoke and Gitleaks checks are listed above; no host drill ran. This local full run is not the remote cross-repository integration workflow.
- Coordinated pushed client revisions: web `a8a351f` and mobile `6066d91`. Mobile standalone CI `40349632014` passed after its workflow test stopped reading the parent checkout and Linux visual references were generated and visually reviewed. Exact cross-repository integration run `40349660474` passed with root `8158807`, web `a8a351f` and mobile `6066d91`. These runs predate the local signing-key rotation work below; they prove the stated revisions only.
- W02-S26 local key-rotation work: identity-org now reads a retiring public key and explicit UTC retirement deadline from its protected key directory, publishes both public keys during overlap, signs new tokens only with the active private key and rejects the retiring key after the deadline in its local decoder. The gateway test exercises discovery of a new key after JWKS refresh and rejection of the retired key by a fresh decoder. Focused command `./mvnw -q -pl services/api-gateway,services/identity-org -am test -Dtest=AccessTokenRotationTest,SigningKeyRingTest -Dsurefire.failIfNoSpecifiedTests=false` exited 0. The tests use synthetic keys; no live host or downstream cache-expiry timing was measured. A gateway with a cached JWKS may continue accepting the old key until cache refresh. The current five other owner-service gates are still future work under ADR 010/011.
- Complete local baseline after the key-ring changes: `python3 scripts/verify-project.py --scope all --base origin/main` exited 0 across foundation, backend, web, mobile and tooling. Maven reactor verification passed, web passed 89 unit tests and 12 Playwright journeys, and mobile analysis/tests passed with the live gateway test skipped by default. This command does not include the separate web dependency audit or physical host drills. The post-documentation `python3 docs/roadmap/tools/docs-lint.py docs README.md AGENTS.md contracts/README.md tools/project_mcp/README.md` check passed 308 Markdown files with zero errors and warnings; `git diff --check` exited 0.
- Pushed root code revision `e458893` passed backend/contract CI run `40361346784` and secret-scan run `40361347081`. Exact cross-repository integration run `40361382622` passed for root `e458893`, web `a8a351f` and mobile `6066d91`, including its full verifier. These CI results do not include the web `dependency-audit` job, which remains failed, or physical Acer/backup evidence.
- ShellCheck 0.11.0: `shellcheck -x -s bash ops/*.sh` exited 0 after the operator scripts' source/remote-shell annotations were made explicit. The annotations do not establish host execution.
- Web dependency audit resolution (2026-10-27): `npm audit fix --omit=dev` and `npm install` in `web/` updated `node-forge` from 1.4.0 to 1.4.1 (addressing GHSA-86w9-cpqp-85rv). Subsequent `npm audit --omit=dev` exited 0 with 0 vulnerabilities (`found 0 vulnerabilities`, 596 packages audited). `npm run check` (typecheck, 8 data tests, 2 config tests, 89 unit tests, bundle budgets, demo build) and Playwright 12 browser scenarios all passed with 0 errors. W02-S26 dependency audit blocker is resolved.
- W02-S26 remains **PARTIAL** pending a live cache-expiry retirement check on the target host. W02-S24 and W02-S27 through W02-S35 remain **BLOCKED/NOT RUN** on direct Acer access, the separate encrypted external disk, physical client devices and real drills. User explicitly chose to leave the host gate blocked while local work is completed. No `w02-s31-green` or `w02-s35-green` tag is justified.
- Boundary review: self-host configuration and operator scripts changed; no HTTP contract, DTO, business rule, database migration, event schema or client adapter changed in this preparation. Runtime role grants and backup completeness require host execution. Existing web/mobile changes are tracked separately and do not prove this gate.

## Authentication slice ledger

### Edge client address and browser cookie repair, 2026-10-29

- The gateway now accepts `X-Forwarded-For` only from configured IP/CIDR peers and selects the rightmost non-trusted hop. Self-host Compose pins Caddy to `172.31.250.2` and trusts only that address, preventing the web container or a direct caller from choosing the login-throttle source. Caddy's default proxy behavior replaces untrusted incoming forwarded-address values. Recreating the existing `hrflow-edge` network is required before this pinned address can take effect.
- The refresh cookie path is configurable. Direct identity-service use defaults to `/auth`; both gateway Compose paths set `/api/v1/auth`, matching the browser-visible login, refresh and logout routes. Existing `/auth` cookies are unreachable through the gateway, so browsers must sign in once after deployment.
- Local account/key bootstrap now defaults to disabled, and generated `.local-keys` directories are ignored. Development Compose enables bootstrap explicitly; self-host Compose keeps it disabled.
- Successful authentication now resets only that account's failure counter. It retains failures accumulated by the shared source address, so possession of one valid account cannot erase source-level throttling for other attempted accounts.
- Focused structured runs passed gateway client-address handling 7/7, cookie handling 4/4 and identity environment configuration 3/3. Both Compose manifests parsed and `git diff --check` passed. The broader verifier passed contracts and compatibility against `origin/main`, documentation, web checks plus 15 Playwright scenarios, mobile analysis plus 185 tests, and tooling. Its backend scope is not green: a concurrent attendance change left `CorrectionReadContractTest` with two failures (detail 404 and unpaged 27-item response), unrelated to these auth/configuration files. Gateway and identity Maven modules completed successfully before that failure. No Caddy/browser live journey, deployment or physical-host check ran.

- Contract: 158 total operations; 8 implemented and 150 pending.
- Implemented: `GET /auth/csrf`, `POST /auth/login`, `POST /auth/refresh`, `GET /auth/me`, `PATCH /auth/profile`, `POST /auth/change-password`, `POST /auth/logout`, and `GET /.well-known/jwks.json`.
- Current proof: all 54 identity unit/HTTP tests pass (including Gate 2 method security, role enforcement, tenant boundary isolation and configured issuer), including tenant-scoped profile and password services, uniform unknown-account login failure, native/body versus browser/cookie refresh/logout selection, refresh-cookie path and password-change authorization/validation. Gateway tests pass 17/17 (including caller identity header sanitization, negative token validator matrix, and RFC 7807 problem details); the web auth adapter passes 4/4 and the mobile local adapter returns the canonical 200 password-change response. The 2026-10-26 Docker-backed Maven verification passed all six identity integration tests, including the profile and password-change PostgreSQL scenarios.
- Postman test suite: 158 operations and 14 categories with dynamic extraction scripts for CSRF, JWT bearer, refresh token rotation, user context and resource IDs.
- Gateway smoke: health, JWKS schema, native and browser login, tenant validation, exact Origin/CSRF enforcement, JWT tamper denial, forged-tenant denial, persisted throttling, refresh rotation/reuse-family revocation and logout pass.
- Client proof: live Nuxt sign-in/sign-out and live Flutter login/restore/logout pass through the gateway.
- Host decision: the user designated a separate Acer Nitro 5—not the development Mac—as the intended self-host, with reported Intel Core i5, 16 GB RAM and 2 TB hard-disk capacity. Sizing budget and IOPS analysis recorded in W02-S24; exact on-device command verification remains pending physical bootstrap.
- Remaining Week 2 blockers: W02-S26 through W02-S35, physical devices, direct Acer host-readiness evidence and external backup media.

### UAT repair — explicitly linked private accounts (2026-10-29)

The service test first failed to compile because the new private setup API did not exist (`/tmp/hrflow-uat-account-setup-red.log`); this is a preimplementation gate, not an executed failing business case. After implementation, all 3 initial PostgreSQL cases pass (`/tmp/hrflow-uat-account-setup-green.log`) under an actual NOSUPERUSER/NOBYPASSRLS runtime login. An explicitly selected employee receives a new login account with exact tenant/employee/role claims from the real authentication service. Foreign/inactive employee, unsupported roles, duplicate links and interrupted setup retain no partial account effects. The owner snapshot fills its already accepted nullable userId; no Avro field/type changes or cross-owner writes are introduced.

Forward V12 adds the unique enabled tenant/employee association, preserving legacy unknown bootstrap links without email/name inference. Setup creates only new accounts, leaves existing accounts untouched and records database-principal setup metadata with no pretend human actor or credential data. Existing role vocabulary and BCrypt provider are reused; no paid service or email integration is added.

All 3 service cases plus 2 packaged command integration cases pass (`/tmp/hrflow-uat-account-command-verify.log`). The actual fat jar was launched through PropertiesLauncher against the migrated disposable database using restricted runtime credentials. A real pseudo-terminal supplied two private prompts; plaintext never appeared in its captured output. The resulting account logs in with the explicitly selected employee and MANAGER roles. Redirected input and password arguments are rejected without account creation. Internal command flags keep migrations/web startup disabled even when environment flags try to enable them. No real accounts, deployment, commit or push occurred. The expanded concurrent-setup case and complete identity reactor are running; no full identity result is presumed.

The full identity reactor exits 0 with 92 owner tests plus 3 shared contracts and 2 actual packaged CLI integration tests, zero failures/errors/skips (`/tmp/hrflow-uat-account-setup-full-verify.log`). The added concurrent setup regression creates exactly one enabled account and one setup audit entry for an employee; the interrupted case also verifies account-driven outbox/audit rollback. Existing organization Kafka publication, audit, authorization, auth/session/profile and tenant regressions ran. V12 is now applied in disposable tests and must only be superseded by a forward migration.

### UAT repair — authoritative notification role facts (2026-10-29)

The new exact-schema compatibility case first failed because accountRoles was missing (`/tmp/hrflow-uat-account-role-contract-red.log`). All 4 shared compatibility cases now pass (`/tmp/hrflow-uat-account-role-contract-green.log`), including both old-reader/new-writer directions against a preserved original v1 fixture. The identity producer emits only documented current roles from the explicitly linked enabled account, retaining the accepted nullable userId and unknown legacy roles as null.

All 8 selected identity cases plus 4 shared contracts pass (`/tmp/hrflow-uat-account-role-producer-test.log`), including actual Schema Registry FULL_TRANSITIVE registration of old then new schema, owner outbox transaction behavior and linked-account login/role facts. An initial fixture broker listener used an invalid URL prefix; corrected to the installed Testcontainers listener API before the accepted run. This adds one nullable/defaulted Avro field without a new role, infrastructure or cross-owner lookup. Notification consumption/routing is a separate next stone; this is not delivery acceptance. No commit/deploy.
