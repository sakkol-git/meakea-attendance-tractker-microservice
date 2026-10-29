#!/usr/bin/env bash
set -euo pipefail

# W10-S34-S36: Restore broker schema and replay boundaries
# Invariant: A deliberate retention gap stops replay and selects the verified snapshot path; no Kafka high-availability claim.
# Missing Kafka broker must produce BLOCKED, never synthetic PASS.

mode="${1:-recovery}"
kafka_host="${KAFKA_HOST:-127.0.0.1}"
kafka_port="${KAFKA_PORT:-9092}"

get_closed_port() {
  python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1", 0)); port=s.getsockname()[1]; s.close(); print(port)' 2>/dev/null || printf '59092'
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion: controlled unavailable broker connection fails closed
  checker="${HRFLOW_BROKER_CHECKER:-}"
  if [[ -n "$checker" ]]; then
    if eval "$checker"; then
      printf 'NEGATIVE FAIL: injected broker checker unexpectedly succeeded\n' >&2
      exit 1
    fi
  else
    closed_port=$(get_closed_port)
    if nc -z -w 1 127.0.0.1 "$closed_port" 2>/dev/null; then
      printf 'NEGATIVE FAIL: unexpected connection to closed broker port %s\n' "$closed_port" >&2
      exit 1
    fi
  fi
  printf 'NEGATIVE PASS: broker connectivity-negative check: unreachable broker fails closed (connectivity-negative evidence only, does not prove replay or retention boundaries)\n'
  exit 0
fi

# Recovery mode: requires real Kafka broker
if ! nc -z "$kafka_host" "$kafka_port" 2>/dev/null; then
  printf 'BLOCKED: Kafka broker %s:%s is unreachable; cannot verify replay boundaries without running broker\n' "$kafka_host" "$kafka_port" >&2
  exit 2
fi

printf 'Connected to Kafka broker at %s:%s.\n' "$kafka_host" "$kafka_port"
printf 'PREREQUISITE REACHABLE: Kafka broker is reachable. Bounded stream replay and retention recovery drill NOT RUN\n'
exit 0
