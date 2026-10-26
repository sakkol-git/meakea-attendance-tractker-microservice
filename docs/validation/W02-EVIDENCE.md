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
- Result: analysis reports no issues; 150 automated tests pass with one expected skipped live test in the default run; the separately enabled live test passes login, session restoration and logout through the gateway. No token is persisted in ordinary client storage. **PASS** for automated acceptance.
- Limit: Android/iOS device keychain, biometric and entitlement behavior remains **NOT RUN** and is required before production readiness.

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
- Status: **RECORDED & ASSESSED**; physical Acer live command execution and readiness verification remains **PENDING/BLOCKED** until direct console access is performed. **PASS** for inventory specification recording.

## Authentication slice ledger

- Contract: 158 total operations; 8 implemented and 150 pending.
- Implemented: `GET /auth/csrf`, `POST /auth/login`, `POST /auth/refresh`, `GET /auth/me`, `PATCH /auth/profile`, `POST /auth/change-password`, `POST /auth/logout`, and `GET /.well-known/jwks.json`.
- Current proof: all 53 identity unit/HTTP tests pass (including Gate 2 method security, role enforcement and tenant boundary isolation), including tenant-scoped profile and password services, uniform unknown-account login failure, native/body versus browser/cookie refresh/logout selection, refresh-cookie path and password-change authorization/validation. Gateway tests pass 17/17 (including caller identity header sanitization, negative token validator matrix, and RFC 7807 problem details); the web auth adapter passes 4/4 and the mobile local adapter returns the canonical 200 password-change response. The 2026-10-26 Docker-backed Maven verification passed all six identity integration tests, including the profile and password-change PostgreSQL scenarios.
- Postman test suite: 158 operations and 14 categories with dynamic extraction scripts for CSRF, JWT bearer, refresh token rotation, user context and resource IDs.
- Gateway smoke: health, JWKS schema, native and browser login, tenant validation, exact Origin/CSRF enforcement, JWT tamper denial, forged-tenant denial, persisted throttling, refresh rotation/reuse-family revocation and logout pass.
- Client proof: live Nuxt sign-in/sign-out and live Flutter login/restore/logout pass through the gateway.
- Host decision: the user designated a separate Acer Nitro 5—not the development Mac—as the intended self-host, with reported Intel Core i5, 16 GB RAM and 2 TB hard-disk capacity. Sizing budget and IOPS analysis recorded in W02-S24; exact on-device command verification remains pending physical bootstrap.
- Remaining Week 2 blockers: W02-S25 through W02-S35, physical devices, direct Acer host-readiness evidence and external backup media.
