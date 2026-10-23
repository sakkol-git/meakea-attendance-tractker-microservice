# Browser cookies and CSRF

## Ten-minute concept

A browser automatically sends matching cookies. A hostile site can try to trigger a request that carries those cookies; CSRF means cross-site request forgery. HttpOnly prevents JavaScript from reading a cookie, but does not alone prove the request came from your application.

## HRFlow choice

Identity's browser transport keeps refresh in an HttpOnly, Secure, SameSite cookie with a narrow auth path. Access tokens stay in Nuxt memory. The adapter checks an exact configured Origin and a server-issued CSRF token echoed in a request header for cookie-authenticated refresh/logout. Do not allow a wildcard credentialed origin, accept a caller-selected tenant or return the refresh token in the browser JSON body.

Mobile uses its existing explicit JSON refresh and platform secure storage. Choosing browser/mobile transport changes delivery only; it never grants a role or tenant. The static Nuxt client calls the Java adapter through the same approved edge; no additional backend-for-frontend service is introduced.

## One request fixture

```text
POST /api/v1/auth/refresh
Origin: configured-application-origin
Cookie: protected refresh and CSRF cookies managed by the browser
X-CSRF-Token: matching server-issued token
```

The literal origin/token above are fixture descriptions, not production values. Store real secrets only in protected host/test configuration. The server compares the token using the reviewed security helper and rotates the refresh family only after all boundary checks pass.

## Minimum failure tests

- [ ] Valid allowed-origin/token request rotates exactly once.
- [ ] Missing/mismatched CSRF header or hostile Origin returns safe 403 and changes no refresh state.
- [ ] Browser response never includes refresh plaintext in JSON; cookie flags/path are asserted.
- [ ] Mobile explicit flow still rejects reused/expired/foreign-family credentials.
- [ ] Nuxt single-flight refresh cannot loop or persist refresh in web storage.

## Common mistake

> **Pitfall:** Treating SameSite or CORS headers as the complete CSRF implementation, or retrying a denied security request until it succeeds.

[Security baseline](../SECURITY-BASELINE.md), [auth primer](JWT-REFRESH.md), and [golden slice](../GOLDEN-SLICE.md).

[Spring Security CSRF for single-page applications](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) documents the version-specific token/request-handler integration. Use that reviewed helper, not a custom transport header to bypass the filter. The CSRF token must be available to the same-origin client through its deliberate non-HttpOnly token cookie or response fixture; the refresh cookie alone stays HttpOnly with its narrow path. Missing/mismatched tokens fail closed.
