# HRFlow glossary

## Terms used by the guide

**Tenant:** An isolated company scope derived from a verified token.

**Principal:** Authenticated user/service identity, distinct from employee record.

**JWT:** Signed short-lived access token; decoding is not signature validation.

**Refresh family:** Related rotating refresh secrets, stored hashed and revoked on reuse.

**Migration:** Versioned database change applied once.

**Invariant:** A condition that must remain true under retries and concurrency.

**Idempotency:** Same command identity yields one intended effect.

**Outbox:** Durable event intent committed with domain data.

**Inbox:** Receipt committed with a handler effect to prevent duplicates.

**Projection:** Derived read model with source revision and visible freshness.

**Aggregate revision:** Monotonic owner state version, not wall-clock order.

**Lease:** Time-limited worker claim recoverable after a crash.

**CGNAT:** ISP translation that prevents ordinary inbound router forwarding.

**Reverse proxy:** Gateway forwarding approved requests to private application.

**TLS:** Encrypted authenticated network transport.

**RPO:** Maximum agreed data-loss interval, not a promise from a backup filename.

**RTO:** Agreed recovery duration measured by drills.

**Green tag:** Git label for tested app startup and a stone win.

**Mock server:** Responses generated from OpenAPI before live implementation.

**Testcontainers:** Real disposable dependency containers started by tests.

**CI:** Automated build/tests on pushes, with secrets protected.

**Cron:** Host scheduler; sleeping/unavailable host can miss jobs.

**Uptime Kuma:** Private availability monitoring; cannot alert externally when its entire host is down.

**Caddy:** Reverse proxy; public automatic TLS requires reachable validation or configured DNS.

**Cloudflare Tunnel:** Outbound connector; remote access still depends on ISP/provider.
