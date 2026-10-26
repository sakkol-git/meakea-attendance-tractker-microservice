# ADR 010 — Dual JWT validation at gateway and services

## Status and evidence

**Accepted:** 2026-10-26 by explicit user instruction.

**Partially superseded:** ADR 011 replaces the custom `JwtAuthenticationFilter` implementation mechanism with Spring Security OAuth2 Resource Server. The dual-gate boundary and verification requirements in this ADR remain accepted.

This decision supersedes only ADR 008's gateway-injected identity-header trust model and the matching decision-summary row in the approved architecture reference. It does not change the seven-service topology, token issuer, refresh-token policy, service ownership, private-network requirement or method/resource authorization rules. Existing implementation and test evidence predating this ADR does not prove the new boundary.

## Decision

Every protected request passes two authentication gates:

1. The API gateway validates the bearer access JWT before routing.
1. The destination Java service independently validates the same bearer access JWT before constructing its security principal or entering business logic.

The gateway forwards the original `Authorization: Bearer <access-token>` credential to the destination service. It strips caller-supplied identity headers and does not replace the JWT with trusted tenant, employee or role headers. Each gate validates signature and approved algorithm, issuer, audience, lifetime and required identity claims using the accepted issuer/JWKS configuration. Each service derives tenant, employee and roles only from its own validated token.

Explicit public authentication, health and approved control-plane endpoints remain narrowly allowlisted. A route accepted by the gateway but rejected by the service fails closed. Direct access to a private service with a valid token still undergoes service authentication and authorization; network isolation remains required defense in depth rather than the source of caller identity.

## Rationale and tradeoffs

Independent service validation limits the effect of gateway bypass, routing mistakes and forged internal identity headers. It adds a security filter and consistent issuer/JWKS configuration to every Java service, duplicates a small amount of token-validation work and requires key-rotation tests across all validators. Public-key validation keeps the signing key in identity-org while allowing the gateway and services to verify tokens.

## Historical implementation mechanism and retained verification impact

- At acceptance, this ADR proposed custom reactive and servlet `JwtAuthenticationFilter` implementations. ADR 011 supersedes that mechanism; current work must use Spring Security OAuth2 Resource Server JWT support instead.
- Shared configuration may centralize non-business validation conventions, but each service must execute validation locally; a shared library or gateway success is not evidence that a service gate ran.
- Negative tests cover missing, malformed, expired, not-yet-valid, wrong-issuer, wrong-audience, disallowed-algorithm and invalid-signature tokens at both gates. Tests also prove forged identity headers are ignored, the bearer token reaches the owner service, and a gateway-accepted request is denied when the service validator rejects it.
- Key rotation must prove bounded overlap and retirement at the gateway and all six services.

This ADR changes planning and acceptance criteria only. It does not implement a filter, configuration or route.
