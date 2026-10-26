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
- Coordinated client revisions for this local candidate: web `a8a351f` and mobile `b52ae98`, tested with the backend working tree before this evidence commit. The root repository ignores both client trees; both revisions were pushed to their own `main` branches. No remote cross-repository integration run has completed for this exact triple yet.
- W02-S26 remains **BLOCKED**: current identity/gateway code has no demonstrated bounded key overlap/retirement, and the web dependency audit fails. Root backend/contract CI and all three secret-scan workflows passed on the pushed candidate. The first mobile CI run failed because its workflow test read `../business-flow.md`, unavailable in the standalone checkout; the test now checks the local handover IDs, while the parent full verifier checks parity with the owning source. Focused mobile test and analysis pass; rerun CI is pending. W02-S24 and W02-S27 through W02-S35 remain **BLOCKED/NOT RUN** on direct Acer access, the separate encrypted external disk, physical client devices and real drills. User explicitly chose to leave the host gate blocked while local work is completed. No `w02-s31-green` or `w02-s35-green` tag is justified.
- Boundary review: self-host configuration and operator scripts changed; no HTTP contract, DTO, business rule, database migration, event schema or client adapter changed in this preparation. Runtime role grants and backup completeness require host execution. Existing web/mobile changes are tracked separately and do not prove this gate.

## Authentication slice ledger

- Contract: 158 total operations; 8 implemented and 150 pending.
- Implemented: `GET /auth/csrf`, `POST /auth/login`, `POST /auth/refresh`, `GET /auth/me`, `PATCH /auth/profile`, `POST /auth/change-password`, `POST /auth/logout`, and `GET /.well-known/jwks.json`.
- Current proof: all 54 identity unit/HTTP tests pass (including Gate 2 method security, role enforcement, tenant boundary isolation and configured issuer), including tenant-scoped profile and password services, uniform unknown-account login failure, native/body versus browser/cookie refresh/logout selection, refresh-cookie path and password-change authorization/validation. Gateway tests pass 17/17 (including caller identity header sanitization, negative token validator matrix, and RFC 7807 problem details); the web auth adapter passes 4/4 and the mobile local adapter returns the canonical 200 password-change response. The 2026-10-26 Docker-backed Maven verification passed all six identity integration tests, including the profile and password-change PostgreSQL scenarios.
- Postman test suite: 158 operations and 14 categories with dynamic extraction scripts for CSRF, JWT bearer, refresh token rotation, user context and resource IDs.
- Gateway smoke: health, JWKS schema, native and browser login, tenant validation, exact Origin/CSRF enforcement, JWT tamper denial, forged-tenant denial, persisted throttling, refresh rotation/reuse-family revocation and logout pass.
- Client proof: live Nuxt sign-in/sign-out and live Flutter login/restore/logout pass through the gateway.
- Host decision: the user designated a separate Acer Nitro 5—not the development Mac—as the intended self-host, with reported Intel Core i5, 16 GB RAM and 2 TB hard-disk capacity. Sizing budget and IOPS analysis recorded in W02-S24; exact on-device command verification remains pending physical bootstrap.
- Remaining Week 2 blockers: W02-S26 through W02-S35, physical devices, direct Acer host-readiness evidence and external backup media.
