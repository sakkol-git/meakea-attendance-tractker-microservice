# JWT authentication and rotating refresh tokens

> **Note:** Modern stateless API authentication pairs short-lived JSON Web Tokens (JWT) for immediate request authorization with cryptographically rotating refresh tokens for long-term session maintenance.

## Purpose and core concept

Stateless authentication allows backend services to verify user identity and permissions without querying a centralized session cache on every single HTTP request.

The security architecture combines two complementary tokens:

- Access token: A cryptographically signed, short-lived JWT (typically 15 minutes) carrying the user ID, tenant ID, and role claims.
- Refresh token: A high-entropy random string with longer validity (e.g., 7 to 30 days) stored securely as a salted hash in the database, used exclusively to request new access tokens.

## Problem being solved

Relying solely on either traditional server sessions or standalone long-lived JWTs introduces severe vulnerabilities:

- Long-lived standalone JWTs: Once issued, a standard JWT cannot be revoked before its expiration without maintaining a global revocation list, which destroys statelessness. If an access token is intercepted, the attacker maintains access until it expires.
- Traditional server sessions: Storing active sessions in memory or Redis introduces stateful clustering dependencies and single points of failure that complicate self-hosting.
- Stolen refresh tokens: If a refresh token is static, an attacker with physical or network access to the token can mint access tokens indefinitely.

## Simple mental model

An access token is like a paper concert wristband stamped with the current date and your ticket tier. Security guards at each stage glance at your wristband and grant entry immediately without calling the box office. However, the wristband expires at the end of the set.

A refresh token is your receipt and photo ID stored safely in your wallet. When your wristband expires, you return to the box office to show your ID. The box office invalidates your old receipt, hands you a new receipt, and stamps a fresh wristband on your wrist.

## How it works

Refresh token rotation enforces single-use token lifecycle:

1. Initial login: The user posts valid credentials. The auth service generates a 15-minute access JWT and an opaque refresh token UUID.
1. Database storage: The SHA-256 hash of the refresh token is inserted into `identity_org.refresh_tokens` alongside a family identifier (`family_id`) and expiration timestamp.
1. Authenticated requests: The client attaches the access JWT to API requests. Spring Security validates the signature and expiration locally.
1. Token expiration: After 15 minutes, API requests fail with `401 Unauthorized`.
1. Token refresh exchange: The client sends a refresh request with the old refresh token.
1. Rotation execution: The server verifies the token hash in the database, marks the old refresh token as revoked, generates a new refresh token, updates the database, and issues both a new access JWT and the new refresh token.
1. Breach detection: If an attacker and a legitimate client both attempt to use the same old refresh token, the server detects reuse, invalidates the entire token family, and forces all sessions to re-authenticate immediately.

## Why HRFlow needs it

In HRFlow, employee mobile devices and web browsers must maintain active sessions across work days without compromising security:

- Secure self-hosting: Eliminates the requirement to host and manage an external Redis cluster for session storage.
- Immediate tenant bounding: The access JWT embeds the tenant identifier (`tenant_id`), ensuring every service call carries verified tenant context.
- Instant threat neutralization: If an employee leaves the company or reports a lost phone, revoking their refresh token family immediately stops access token renewal.

## Minimal example

Refresh token family table schema in PostgreSQL:

```sql
CREATE TABLE identity_org.refresh_tokens (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    family_id UUID NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_family
ON identity_org.refresh_tokens (family_id);
```

## Application-specific implementation

Refresh rotation and reuse detection in Java:

```java
@Service
public class TokenRotationService {

    private final RefreshTokenRepository tokenRepo;
    private final JwtTokenProvider jwtProvider;

    public TokenRotationService(RefreshTokenRepository tokenRepo,
                                JwtTokenProvider jwtProvider) {
        this.tokenRepo = tokenRepo;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public TokenPair rotateToken(String rawRefreshToken) {
        String tokenHash = Sha256.hash(rawRefreshToken);
        RefreshToken token = tokenRepo.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (token.isRevoked() || token.isExpired()) {
            tokenRepo.revokeEntireFamily(token.getFamilyId());
            throw new SecurityBreachException("Refresh token reuse detected; family revoked");
        }

        token.setRevoked(true);
        tokenRepo.save(token);

        String newRawToken = UUID.randomUUID().toString();
        RefreshToken newToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .tenantId(token.getTenantId())
                .userId(token.getUserId())
                .tokenHash(Sha256.hash(newRawToken))
                .familyId(token.getFamilyId())
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        tokenRepo.save(newToken);

        String newJwt = jwtProvider.createAccessToken(token.getTenantId(), token.getUserId());
        return new TokenPair(newJwt, newRawToken);
    }
}
```

## Common mistakes and pitfalls

Avoid these critical security flaws:

> **Pitfall:** Storing plaintext refresh tokens in the database. If an attacker gains read access to the database or a backup snapshot, plaintext tokens grant immediate account takeover. Always store cryptographically hashed tokens (`SHA-256`).

- Placing sensitive data in JWT claims: JWT payloads are Base64Url-encoded, not encrypted. Never store passwords, PINs, or confidential personal information in JWT claims.
- Missing signature validation: Libraries that parse JWTs without explicitly enforcing the cryptographic algorithm (`alg: HS256` or `RS256`) are vulnerable to "alg: none" signature bypass attacks.
- Client-side token storage in localStorage: Storing JWTs in browser `localStorage` exposes them to Cross-Site Scripting (XSS) extraction. Use `HttpOnly`, `Secure`, `SameSite=Strict` cookies for web clients where possible.

## Testing and debugging

Validation test matrix:

- Normal refresh rotation test: Submit valid credentials, capture refresh token. Exchange refresh token for new pair. Verify that access token works and new refresh token is stored.
- Reuse detection test: Re-submit the old refresh token from the previous step. Verify that the server returns `401 Unauthorized` and marks all tokens in the `family_id` as revoked.
- Expiration test: Issue a JWT with an expired `exp` timestamp and assert that Spring Security rejects requests with `401 Unauthorized`.

## Trade-offs and alternatives

Comparing authentication patterns:

- Short JWT + rotating refresh (HRFlow choice): High security, stateless API verification, instant revocation on family reuse; requires database query once every 15 minutes during token refresh.
- Pure stateful sessions: Simplest revocation; requires centralized database or Redis lookup on every single API request.
- Long-lived JWT without refresh: Zero server lookups; impossible to revoke stolen credentials before expiration.

## When not to use it

Do not use JWT and refresh token rotation for:

- Machine-to-machine internal service communication: Use mutual TLS (mTLS) or private network authentication between trusted backend components.
- Short-lived single-action links: Password reset links and email verification tokens require dedicated one-time tokens with immediate invalidation upon use.

## Related documentation

- [Multi-tenancy primer](MULTI-TENANCY.md)
- [Reverse proxy and HTTPS primer](REVERSE-PROXY-HTTPS.md)
- [Week 02 identity and host build guide](../02-IDENTITY-AND-HOST/BUILD-GUIDE.md)
- [Architecture decision record 006](../../adr/006-FINISH-LINE-MODULAR-MONOLITH.md)
