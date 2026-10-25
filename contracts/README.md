# Shared HTTP contract baseline

Canonical source: `openapi.json`, OpenAPI 3.1 JSON. Status: provisional shared consumer requirements. The current working copy inventories 101 paths and marks their business operations `x-hrflow-implementation: pending`; it is broader than its older leave-slice metadata. Only the shared leave detail/approval fixture currently has cross-client adapter evidence. No Java HR business endpoint is asserted implemented, and an inventoried operation is not an accepted provider contract.

Run the commands in [project context](../docs/development/PROJECT-CONTEXT.md). `scripts/contracts.py validate` uses OpenAPI validation and JSON Schema validation of the shared synthetic fixture. `sync` creates versioned client copies plus a SHA-256 receipt; `check` rejects drift or missing clients. Standalone `check-bundle.py` is copied with the bundle and verifies integrity, not upstream freshness. Existing Avro contracts remain in `libs/hrflow-contracts/events/`.

Client tests exercise the actual web API wrapper and mobile feature repository with this fixture, including ETag preservation, request routes, command identity and failure propagation. They are consumer checks using simulated transport. Real provider tests, business transactions, security, browser CSRF/session negotiation and deployed-version compatibility remain required before integration acceptance.

The compatibility checker preserves existing wire definitions conservatively and permits new paths/components and editorial edits. It is stricter than a semantic breaking-change tool and does not prove business compatibility. See the context guide for limits and versioning procedure.
