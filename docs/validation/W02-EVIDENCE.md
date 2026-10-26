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

## W02-S11 to W02-S15

- Files: `AuthSessions.java`, `RefreshSession.java`, `SecurityConfiguration.java`
- Command: `mvn -f services/identity-org/pom.xml test -Dtest=AuthSessionsTest`
- Result: 4/4 tests pass; transactional refresh token rotation, token family tracking, reuse detection revoking downstream sessions, and logout session termination verified. **PASS**

## W02-S16 to W02-S20

- Files: `AuthCookies.java`, `SecurityProblems.java`, `AuthProblems.java`, `BrowserCors.java`
- Command: `mvn -f services/identity-org/pom.xml test -Dtest=AuthCookiesTest,SecurityConfigurationTests`
- Result: 5/5 tests pass; HttpOnly SameSite=Lax refresh cookie, XSRF-TOKEN CSRF cookie/header verification, RFC 7807 problem details on 401/403. **PASS**

## W02-S21 to W02-S22

- Files: `web/app/api/auth.ts`, `web/app/stores/session.ts`, `scripts/live-auth-smoke.py`, `contracts/postman/HRFlow_Complete_API.postman_collection.json`, `scripts/generate-postman-collection.py`
- Command: `npm test` (in web directory); `postman collection run contracts/postman/HRFlow_Complete_API.postman_collection.json -e contracts/postman/HRFlow_Local.postman_environment.json`
- Result: 14 test suites, 73/73 tests pass in web; Postman collection covering all 158 contract operations across 14 categories verified with dynamic token and secret extraction scripts. **PASS**

## W02-S23

- Files: `mobile/lib/features/identity/data/session_client.dart`, `mobile/test/live_auth_test.dart`, secure-storage adapters and session tests.
- Commands: `fvm flutter analyze`; `fvm flutter test`; `fvm flutter test test/live_auth_test.dart --dart-define=LIVE_AUTH_PASSWORD=<local-only>`.
- Result: analysis reports no issues; 150 automated tests pass with one expected skipped live test in the default run; the separately enabled live test passes login, session restoration and logout through the gateway. No token is persisted in ordinary client storage. **PASS** for automated acceptance.
- Limit: Android/iOS device keychain, biometric and entitlement behavior remains **NOT RUN** and is required before production readiness.

## Authentication slice ledger

- Contract: 158 total operations; 8 implemented and 150 pending.
- Implemented: `GET /auth/csrf`, `POST /auth/login`, `POST /auth/refresh`, `GET /auth/me`, `PATCH /auth/profile`, `POST /auth/change-password`, `POST /auth/logout`, and `GET /.well-known/jwks.json`.
- Current proof: all 40 identity unit/HTTP tests pass, including tenant-scoped profile and password services, uniform unknown-account login failure, native/body versus browser/cookie refresh/logout selection, refresh-cookie path and password-change authorization/validation. Gateway `SecurityConfigurationTests` pass 5/5; the web auth adapter passes 4/4 and the mobile local adapter returns the canonical 200 password-change response. The 2026-10-26 Docker-backed Maven verification passed all six identity integration tests, including the profile and password-change PostgreSQL scenarios.
- Postman test suite: 158 operations and 14 categories with dynamic extraction scripts for CSRF, JWT bearer, refresh token rotation, user context and resource IDs.
- Gateway smoke: health, JWKS schema, native and browser login, tenant validation, exact Origin/CSRF enforcement, JWT tamper denial, forged-tenant denial, persisted throttling, refresh rotation/reuse-family revocation and logout pass.
- Client proof: live Nuxt sign-in/sign-out and live Flutter login/restore/logout pass through the gateway.
- Remaining Week 2 blockers: W02-S24 through W02-S35, physical devices, intended production host and external backup media.
