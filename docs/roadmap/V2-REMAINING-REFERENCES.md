# V2 architecture reference review

## Final disposition

The initial inventory identified 41 candidate files with C/React/local-dispatch drift. Current weekly execution, root navigation, standards, examples, runbooks and authoritative maps now follow ADR 008. Historical ADRs/V1 audit/progress/reference material remain as explicitly labeled history; quoted rejected alternatives are not executable requirements.

| Reference class | Disposition | Evidence |
| --- | --- | --- |
| Weekly hrflow-app/React commands | Replaced with existing owner roots, Nuxt/client targets and inspected future adapters | Twelve BUILD guide sets and step maps |
| Local event worker/absence of Kafka | Replaced with owner outbox, broker ack, inbox/offset and one-active-relay recovery | Weeks 3/5/9/10/11 and ADR 008 |
| JPA-only teaching | Explicit comparative history; selected implementation is JDBC | Spring JDBC primer, package/dependency standards |
| JUnit 5 assumption | Corrected to actual Boot-managed Jupiter 6.0.3 and Testcontainers 2.0.5 | Toolchain reference/cached Boot 4.1.1 BOM |
| Old five-stone/ending tags | New eight-stone tables and mapped unchanged old tags | Milestone/global checklist/ID maps |
| Cloud within twelve weeks | Latest user deferral applied; production design retained as future | Architecture index/detail banners/ADR 008 |
| Unknown start date | Updated to next-week Monday 2026-10-26 | Dashboard/roadmap/calendar |

## What remains intentionally unverified

Actual implementation behavior, host capacity, private/public connectivity, native viewer import, security/CI/restore/load reports and HR policy approval remain execution checks, all honestly Not run or unknown. The current guide does not describe them as complete. Re-run the linter and review each new architecture change through an ADR; never replace code names based only on a keyword search.
