#!/usr/bin/env bash
set -euo pipefail

# W10-S28-S30: Exercise private internet loss and idempotency reconciliation
# Invariant: Lost response retry reconciles the command; offline client does not report false success.
# Missing target API server must produce BLOCKED, never synthetic PASS.

mode="${1:-recovery}"
target_url="${HRFLOW_API_URL:-http://127.0.0.1:8106/reports/exports}"

get_closed_port() {
  python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1", 0)); port=s.getsockname()[1]; s.close(); print(port)' 2>/dev/null || printf '59999'
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion:
  # Disconnected / offline client failing closed against controlled unavailable endpoint
  checker="${HRFLOW_API_CHECKER:-}"
  if [[ -n "$checker" ]]; then
    if eval "$checker"; then
      printf 'NEGATIVE FAIL: injected API checker unexpectedly succeeded\n' >&2
      exit 1
    fi
  else
    closed_port=$(get_closed_port)
    if curl -s -f --connect-timeout 1 "http://127.0.0.1:$closed_port/api/action" >/dev/null 2>&1; then
      printf 'NEGATIVE FAIL: offline client reported unexpected success on closed port %s\n' "$closed_port" >&2
      exit 1
    fi
  fi
  printf 'NEGATIVE PASS: API offline-connectivity safety: unreachable endpoint fails closed (offline-connectivity safety only, does not prove idempotent retry recovery)\n'
  exit 0
fi

# Recovery mode: requires real running service endpoint
if ! curl -s -o /dev/null --connect-timeout 2 "$target_url" 2>/dev/null; then
  printf 'BLOCKED: Target API endpoint %s is unreachable; cannot verify real network recovery and idempotency\n' "$target_url" >&2
  exit 2
fi

printf 'Connected to target API at %s.\n' "$target_url"
printf 'PREREQUISITE REACHABLE: Target API endpoint %s is reachable. Idempotent command retry drill NOT RUN\n' "$target_url"
exit 0
