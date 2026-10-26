# ADR 011 — Spring Security resource-server JWT authentication

## Status and evidence

**Accepted:** 2026-10-26 by explicit user instruction.

This decision supersedes only ADR 010's planned custom `JwtAuthenticationFilter` implementation mechanism. ADR 010's two independent authentication gates, bearer-token forwarding, claim-validation requirements, identity-header rejection and service-local authorization boundaries remain accepted.

The current gateway and identity-org security chains already call Spring Security's `oauth2ResourceServer(...jwt...)` DSL. That source observation establishes alignment for those configurations, not complete acceptance of issuer, audience, algorithm, rotation or routed two-gate behavior. The other five owner services remain planned work.

## Decision

The reactive API gateway and every servlet Java owner service use Spring Security OAuth2 Resource Server with JWT bearer-token support. Do not create or maintain an application-owned `JwtAuthenticationFilter` for access-token authentication.

- The gateway configures a reactive `SecurityWebFilterChain`, a `ReactiveJwtDecoder` and explicit validators.
- Each owner service configures a servlet `SecurityFilterChain`, a `JwtDecoder` and the same required validation policy.
- Configuration validates signature, allowed algorithm, issuer, audience, lifetime and required identity claims. Tenant, employee and roles are derived only after Spring Security has authenticated the JWT locally.
- The gateway forwards the original bearer credential and strips caller-supplied identity headers. Each service repeats authentication and then applies method/resource authorization.
- Custom components are permitted only at supported extension points when required, such as an audience/claim validator, authorities converter, authentication entry point or access-denied handler. They must not duplicate bearer-token parsing or reimplement the resource-server filter chain.

## Rationale and tradeoffs

Spring Security already supplies the reactive and servlet bearer-token authentication chains, JWT decoding, security-context integration and standard failure handling. Using the framework mechanism removes duplicated security-sensitive parsing and keeps gateway/service implementations consistent. The tradeoff is reliance on Spring Security configuration and extension points, so tests must verify the configured issuer, audience, algorithms, claims and key-rotation behavior rather than assuming framework defaults satisfy HRFlow policy.

The official Spring Security references describe JWT resource-server configuration for both [reactive applications](https://docs.spring.io/spring-security/reference/reactive/oauth2/resource-server/jwt.html) and [servlet applications](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

## Implementation and verification impact

- Replace roadmap work that names `JwtAuthenticationFilter` with the appropriate resource-server `SecurityWebFilterChain` or `SecurityFilterChain`, decoder and validator configuration.
- Keep ADR 010's negative matrix at both gates: missing, malformed, expired, not-yet-valid, wrong-issuer, wrong-audience, disallowed-algorithm and invalid-signature tokens.
- Add configuration tests proving the expected decoder/validators are active; do not test a custom filter class.
- Retain routed proof that a token accepted by the gateway can still be rejected by the destination service and that forged identity headers never supply authority.
- Verify JWKS rotation overlap and retirement independently at the gateway and every owner service.

This ADR changes the selected implementation mechanism and synchronized plan. It does not claim the remaining five service gates or the complete two-gate acceptance matrix are implemented.
