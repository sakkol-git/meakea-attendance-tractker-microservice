#!/usr/bin/env bash
set -euo pipefail

# W10-S22-S24: Verify mobile/phone recovery path
# Invariant: Restored synthetic phone journey sends no live push/email.
# Missing mock notification sink or running service must produce BLOCKED, never synthetic PASS.

mode="${1:-recovery}"
target_url="${HRFLOW_NOTIFICATION_MOCK_URL:-http://127.0.0.1:8025/api/v1}"

get_closed_port() {
  python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1", 0)); port=s.getsockname()[1]; s.close(); print(port)' 2>/dev/null || printf '59998'
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion:
  # Sending notifications to a controlled unavailable sink must fail closed
  checker="${HRFLOW_SINK_CHECKER:-}"
  if [[ -n "$checker" ]]; then
    if eval "$checker"; then
      printf 'NEGATIVE FAIL: injected notification sink checker unexpectedly succeeded\n' >&2
      exit 1
    fi
  else
    closed_port=$(get_closed_port)
    if curl -s -f --connect-timeout 1 "http://127.0.0.1:$closed_port/api/v1" >/dev/null 2>&1; then
      printf 'NEGATIVE FAIL: unexpected connection to unopened notification port %s\n' "$closed_port" >&2
      exit 1
    fi
  fi
  printf 'NEGATIVE PASS: notification-sink connectivity-negative check: unreachable sink fails closed (connectivity-negative evidence only, does not prove phone restoration or notification suppression)\n'
  exit 0
fi

# Recovery mode: requires running isolated notification sink
if ! curl -s -f -o /dev/null --connect-timeout 2 "$target_url" 2>/dev/null; then
  printf 'BLOCKED: Isolated notification sink (%s) is unreachable; real restore verification requires running mock sink\n' "$target_url" >&2
  exit 2
fi

printf 'Connected to isolated notification sink at %s.\n' "$target_url"
printf 'PREREQUISITE REACHABLE: Isolated notification sink is reachable. Synthetic journey notification suppression drill NOT RUN\n'
exit 0
