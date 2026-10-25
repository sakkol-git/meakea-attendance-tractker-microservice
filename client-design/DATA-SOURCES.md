# Local demonstration and remote API data sources

Status: mobile application implemented; web data-source foundation implemented · 24 October 2026.

Both clients select local or remote at composition. Remote never falls back to synthetic success. A base URL switch works only if the API implements the accepted wire contract and business rules; an arbitrary backend cannot be integrated by URL alone.

## Mobile

The Flutter starter has been replaced with an employee/manager/HR workspace. Screens use application providers and repository ports. The mobile simulator adds distinct fictional personas, persistent snapshots, idempotent command receipts, leave accounting, two-stage material approval, correction effects, effective-dated shift assignments, files, reports, catalogs and audit activity. Production tokens use native secure storage, and remote reads use an account-scoped encrypted cache with bounded memory fallback.

```sh
cd mobile
fvm flutter run
fvm flutter run --dart-define=DATA_SOURCE=remote --dart-define=API_BASE_URL=https://hr.example.com/api/v1
```

Dart defines are public build-time settings. Use the same defines in release builds. Mobile documentation is authoritative for the current implementation:

- [Run and included experiences](../mobile/README.md)
- [Architecture, session and cache policy](../mobile/docs/ARCHITECTURE.md)
- [Consumed API adapter contract](../mobile/docs/API-INTEGRATION.md)
- [Workflow coverage and explicit gaps](../mobile/docs/WORKFLOW-COVERAGE.md)
- [Validation record](../mobile/docs/VALIDATION.md)

Mobile uses feature-owned `lib/features/<feature>/{domain,application,data,presentation}/` modules. `lib/app/composition.dart` injects independent feature repositories over the selected client transport; `lib/core/data/` supplies shared HTTP/cache/storage and `core/demo/local_service.dart` composes feature-owned demo handlers. No all-feature repository ships in production. `main.dart` calls the composition root.

Local snapshots use preferences and contain fictional data only. Remote tokens/cache keys use secure storage; production response caching uses encrypted Hive or memory if encrypted persistence is unavailable. No background mutation queue is claimed. Device authentication, sharing and keychain/keystore still need native acceptance.

## Web

The Nuxt client now includes workspace pages, typed API wrappers, runtime response schemas, forms, role guards, reporting and workflow adapters. Inspect the current [architecture](../web/docs/ARCHITECTURE.md), [integration contract](../web/docs/INTEGRATION.md) and [feature coverage](../web/docs/FEATURE-COVERAGE.md) rather than the earlier starter checkpoint. Demo and mock-server browser journeys are separate from live backend acceptance.

```dotenv
NUXT_PUBLIC_DATA_SOURCE=remote
NUXT_PUBLIC_API_BASE_URL=https://hr.example.com/api/v1
```

See `web/.env.example`. `web/app/api/workspace.ts` owns core route mappings and `web/app/schemas/contracts.ts` owns current runtime response validation. Advanced workflows use a typed adapter boundary that rejects unagreed remote operations. Authentication uses an in-memory access token and a server-owned refresh cookie with CSRF checks; bootstrap, cookie/CORS policy and the actual provider still require agreement. There is no demo fallback for remote failures.

The current dependency audit and release blockers live in the web integration guide. Earlier counts and starter-only results are historical evidence and must not be presented as current application state.

## Shared synthetic seed

The canonical seed is [data/demo-seed.json](data/demo-seed.json). Bundle copies are synchronized with:

```sh
python3 scripts/sync-client-demo-data.py
python3 scripts/sync-client-demo-data.py --check
```

The seed contains fictional identities and simplified transitions. Mobile extends it with runtime fixtures/rules; web currently uses the earlier subset. Mutations are not shared between clients. Demo role checks are not production authorization evidence.

## Wire conventions

Domain paths are relative to `/api/v1`; query values are encoded separately. Base URLs require HTTPS except loopback HTTP, reject embedded credentials and do not follow redirects. Failures expose status/code/detail/trace ID. Timeout/network failures can leave mutation outcomes unknown. Preserve a command key and exact payload across retries; preserve quoted ETags on versioned commands. Do not log tokens or personal payloads.

Mobile excludes login/refresh from command idempotency and handles one replay after token refresh. Web uses its separate cookie/CSRF authentication adapter; the two refresh protocols are not reconciled by the shared leave contract. Browser deployment requires accepted CORS/cookie/CSRF policies and exposed ETag headers.

Route names in historical [API-MAP.md](../docs/business-flow/API-MAP.md) are planned references. [REST-API-DATABASE-DESIGN.md](../docs/REST-API-DATABASE-DESIGN.md) owns intended wire conventions. The canonical `contracts/openapi.json` now models a provisional shared leave detail/approval slice with synchronized fixtures and adapter tests; other routes and DTOs remain unreconciled. See [project context](../docs/development/PROJECT-CONTEXT.md). The current mobile adapter document names deliberate provisional DTOs/extensions; it must be reconciled with the actual backend rather than advertised as a published server schema.

## Repository boundaries

Root, `mobile/` and `web/` are separate Git repositories. Root commits do not save either client. This work leaves reviewable changes without committing, publishing or deploying them.
