#!/usr/bin/env bash
set -euo pipefail

# W10-S13-S15: Rotate tunnel credentials privately
# Invariant: Old connector cannot reconnect, new one works and no business/DB port is exposed.
# Missing Tailscale/tunnel target must produce BLOCKED, never synthetic PASS.

mode="${1:-recovery}"

get_closed_port() {
  python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1", 0)); port=s.getsockname()[1]; s.close(); print(port)' 2>/dev/null || printf '59432'
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion: connecting to unopened port must fail closed
  checker="${HRFLOW_TUNNEL_CHECKER:-}"
  if [[ -n "$checker" ]]; then
    if eval "$checker"; then
      printf 'NEGATIVE FAIL: injected tunnel checker unexpectedly succeeded\n' >&2
      exit 1
    fi
  else
    closed_port=$(get_closed_port)
    if nc -z -w 1 127.0.0.1 "$closed_port" 2>/dev/null; then
      printf 'NEGATIVE FAIL: unexpected connection to closed internal port %s\n' "$closed_port" >&2
      exit 1
    fi
  fi
  printf 'NEGATIVE PASS: tunnel connectivity-negative check: closed internal port fails closed (connectivity-negative evidence only, does not prove live rotation or credential rejection)\n'
  exit 0
fi

# Recovery mode: requires real Tailscale daemon
if ! command -v tailscale >/dev/null 2>&1; then
  printf 'BLOCKED: Tailscale CLI is not installed or accessible on this host; real tunnel rotation requires active Tailscale\n' >&2
  exit 2
fi

if ! tailscale status >/dev/null 2>&1; then
  printf 'BLOCKED: Tailscale daemon is not connected or logged in on this host; cannot execute real tunnel rotation\n' >&2
  exit 2
fi

printf 'Inspecting Tailscale status for secure reverse proxy routing...\n'
ts_status=$(tailscale status --json 2>/dev/null || true)
if [[ -z "$ts_status" ]]; then
  printf 'BLOCKED: Unable to retrieve Tailscale status JSON\n' >&2
  exit 2
fi

printf 'PREREQUISITE PASS: Tailscale tunnel authentication status verified (prerequisite check only). Live rotation and old-token rejection remain NOT RUN\n'
exit 0
