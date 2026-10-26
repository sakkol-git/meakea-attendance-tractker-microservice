# Security baseline

## Checklist by risk

These are engineering safeguards aligned to common web application risks, not an OWASP certification or compliance claim.

- [ ] Q01/Q02, Week 1: validate contract/error shape; no internal stack traces or unbounded responses.
- [ ] Q03/Q07, Week 3: typed DTOs reject unknown server fields; SQL uses parameters; exact balances and constraints reject corrupt rows.
- [ ] Q04, Weeks 2–3: service-validated token-derived tenant, scoped lists/counts/objects/files/jobs, tenant index and non-owner RLS; test pool reuse and missing context.
- [ ] Q05, Weeks 2–3: signed JWT algorithm/signature/issuer/audience/time/required-claim validation at the gateway and every Java service, bearer forwarding without trusted identity headers, generic login failure, password hashing, refresh reuse detection, method/resource checks and login throttling.
- [ ] Q06/Q12, Weeks 1–2: secrets stay out of Git/logs; test-only keys; pinned tools; secret/dependency/image scans block unwaived findings.
- [ ] Q08, Weeks 4–5: idempotency scope/body checks, bounded requests and I/O, transactional intent/receipt, graceful shutdown and safe replay.
- [ ] Q10, Week 3: architecture tests deny cross-module internals and dependency cycles; zero-import test fails.
- [ ] Q11, Weeks 2–9: protected logs/metrics, no names/coordinates/tokens; Caddy does not expose management/debug paths.
- [ ] Q13, Weeks 2/10/12: HTTPS/auth/firewall/secrets/external verified backup before public access; restore/rotation/rollback drills and operator.
- [ ] Q14, every stone: review scope/roles/migrations and pattern admission before tag.

## File and export safety

Private files are never authorized by filename secrecy. Recheck requester/current role/tenant at download, cap sizes, validate content, prevent path traversal, escape CSV formula-leading cells, expire download grants and clean artifacts. If optional attachment UI is deferred, it creates no public upload route. Export file security is Must.

## RLS caveat and auth bootstrap

Transaction-local tenant is established from the destination service's independently verified token or durable job. Gateway validation is the first gate and cannot supply tenant authority to the service through identity headers. The runtime role cannot bypass policy. A constrained pre-auth credential lookup is a documented exception and must not grant arbitrary tenant business access. A malicious SQL injection could set a tenant variable; RLS complements rather than replaces parameterization/query/object checks.

## No unsafe fallback

Unknown/expired identity fails closed; missing reference data does not become ABSENT. Scanner outage is blocked evidence. A failed restore or cross-tenant assertion blocks public launch even when a happy-path demo succeeds.

[ADR 007](../adr/007-QUALITY-AND-TENANT-DEFENSE.md), [ADR 010](../adr/010-DUAL-JWT-VALIDATION.md), [ADR 011](../adr/011-SPRING-RESOURCE-SERVER-JWT.md) and [quality coverage](QUALITY-COVERAGE.md).
