# HRFlow local engineering MCP tools

## Purpose

Nine tools expose small, current evidence from this checkout over local stdio using the official Python MCP SDK. They help with repeated repository navigation and verification without a hosted service, API key, persistent index or model call. They do not replace business requirements, review, full CI or live provider tests.

| Tool | Use | Output limit |
| --- | --- | --- |
| get_project_context | Fresh revisions, dirty state and selected feature entry points | Twelve changed paths per repository |
| read_source | Exact numbered source excerpt and whole-file SHA-256 | 160 lines and 8,000 characters; files up to 1 MB |
| search_source | Literal scoped search using rg | Thirty matches; 240 characters per match |
| inspect_structure | Review large files for mixed responsibilities | Twenty review candidates; scan up to 2,000 files |
| run_project_check | One named existing project check | Thirty-second timeout and bounded output tail |
| run_tests | Start a whole suite or selected test file in the background | One active run; ten-minute timeout |
| get_test_result | Counts, failed tests, identical-message groups and compiler diagnostics | Five failures and five groups |
| get_test_details | One stack trace or numbered log excerpt | 12,000 characters per failure; 8,000 per log excerpt |
| cancel_test_run | Stop a run started by this server | Cancels its process group on macOS/Linux |

Supported context areas are overview, leave, attendance, identity, requests and infra. Entry points are navigation hints with existence flags, not inferred completeness. Reads/searches exclude conventional secret/configuration and generated directories. These exclusions are not a general secret scanner; never place real secrets in source. Symlinks leaving the workspace or resolving into excluded locations are rejected. Source types, file sizes and search limits mean a negative search is not proof of absence across the entire system.

Named checks are contract_schema, contract_drift, foundation_tests and foundation_docs. No arbitrary command argument, shell evaluation, database mutation, synchronization write or deployment tool is exposed. Checks can create temporary artifacts; they are not advertised as read-only. Full logs are written to temporary local files; the result includes the log path, exit status, timeout state and truncation flag. Remove old temporary logs using normal local file maintenance when no longer needed.

## Install and use

Requirements: Python 3.11 or newer, Git and ripgrep. Run from the HRFlow root:

```bash
python3 scripts/setup-project-mcp.py
.venv-mcp/bin/python -m unittest discover -s tools/project_mcp/tests -v
```

The installer creates ignored `.venv-mcp/` and a machine-specific ignored `.codex/config.toml` registration for this checkout. It preserves other configuration and refuses to overwrite a different existing `hrflow_project` entry. Rerun setup after moving the checkout or update the registration deliberately. Use `--register-only` when the environment is already installed.

Project-local MCP configuration is supported in trusted Codex projects. Reload the trusted project/session to discover the server; creating the registration does not dynamically inject tools into an already running chat. See the [official Codex MCP documentation](https://developers.openai.com/codex/mcp/). This setup edits no global Codex configuration, permissions or project trust setting.

Start with get_project_context, search a narrow feature directory, and read only the relevant lines. Run a named check after changing its inputs; do not rerun identical successful checks just to fill a report. When MCP is unavailable, use the existing CLI scripts and rg directly. Open the parent HRFlow checkout for tools spanning all three repositories; standalone clients retain their own agent rules and CI.

## Structured test runs

Use run_tests with suite backend, web_unit, web_e2e or mobile. The optional test_file is an existing file relative to the selected repository; backend paths are relative to the root. Examples are `tests/unit/shared-contract.test.ts`, `tests/e2e/remote.spec.ts`, `test/shared_contract_test.dart` and `services/leave/src/test/java/com/hrflow/leave/config/EnvironmentConfigurationTests.java`. Omit test_file for the full selected suite. Backend selections run the owning service and necessary Maven dependencies. Web unit uses Vitest; the separate Node data tests, lint, type checks and builds are still covered by the full verifier, not this unit suite.

The start call immediately returns run_id. Poll get_test_result after the suggested delay while continuing useful independent work. The default result reports process status, current-run counts, at most five failures with observed locations, exact-message error groups, reporter errors, compiler diagnostics and artifact paths. It does not send the whole console log to the model. Use get_test_details with failure_index for one stack, or log_start/lines for an exact numbered excerpt. Without an index/start it returns a bounded log tail. Failure details include the initial Git revision/dirty-state snapshot; this is provenance, not a snapshot of every uncommitted source byte.

Vitest and Playwright write per-run JUnit reports. Flutter writes its machine-readable JSON event report. Maven reports are copied only when their modification timestamp changed during this run, so an earlier passing report cannot establish success after a failed build. Do not run another Maven invocation against the same checkout concurrently outside this server. Each run has a separate temporary artifact folder. Playwright writes screenshots/traces there; Flutter dependencies must already be installed because the tool uses no-pub. Tests retain their normal local side effects; this tool does not sandbox test code or establish live provider safety.

A nonzero process exit always prevents success. Missing/malformed reports, zero executed tests, timeouts and cancellations remain explicit. Similar messages are grouped exactly; the tool does not invent root causes or remediation. Failing cases can still need source inspection, a focused rerun or a larger test scope. The bounded report is an aid to investigation, not a replacement for it.

The server keeps the last twenty run handles in memory and limits tool-started execution to one active run. Logs and machine reports remain in local temporary folders until removed through normal file maintenance. A server restart loses run handles; artifact paths still identify saved logs/reports. Cancel a long run with cancel_test_run. Normal server exit attempts to stop active child processes; ten minutes is the run limit. macOS/Linux use process-group termination, while other systems terminate the direct child only.

## Small module responsibilities

The server module declares the tools. The workspace module handles bounded file reads/searches and structure review. The context module handles Git evidence and navigation. The checks module wraps existing allowlisted commands. Test suite selection, background process lifecycle and report parsing have separate small modules. Tests cover evidence boundaries, failure behavior, configuration preservation and a real stdio client/server exchange. Runtime behavior comes from the official SDK rather than a custom MCP protocol implementation.

## Validation checkpoint

The initial workspace acceptance tests were run before implementation and failed because the workspace module did not exist. Subsequent unit and stdio protocol checks are recorded in the project verification guide. Success here does not clear the earlier mobile, Compose or legacy documentation blockers. No claim of measured token savings or guaranteed hallucination prevention is made.
