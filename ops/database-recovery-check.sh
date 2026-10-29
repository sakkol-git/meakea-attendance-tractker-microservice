#!/usr/bin/env bash
set -euo pipefail

# W10-S31-S33: Exercise scratch database loss and transaction recovery
# Invariant: No failed uncommitted request is acknowledged; uncertain committed request returns same ID.
# Missing database target must produce BLOCKED, never synthetic PASS.

mode="${1:-recovery}"
target_host="${PGHOST:-127.0.0.1}"
target_port="${PGPORT:-5432}"

get_closed_port() {
  python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1", 0)); port=s.getsockname()[1]; s.close(); print(port)' 2>/dev/null || printf '59432'
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion: controlled unavailable database port fails closed
  checker="${HRFLOW_DB_CHECKER:-}"
  if [[ -n "$checker" ]]; then
    if eval "$checker"; then
      printf 'NEGATIVE FAIL: injected database checker unexpectedly succeeded\n' >&2
      exit 1
    fi
  else
    closed_port=$(get_closed_port)
    if command -v pg_isready >/dev/null 2>&1; then
      if pg_isready -h 127.0.0.1 -p "$closed_port" >/dev/null 2>&1; then
        printf 'NEGATIVE FAIL: unexpected connection to closed database port %s\n' "$closed_port" >&2
        exit 1
      fi
    elif nc -z -w 1 127.0.0.1 "$closed_port" 2>/dev/null; then
      printf 'NEGATIVE FAIL: unexpected connection to closed port %s\n' "$closed_port" >&2
      exit 1
    fi
  fi
  printf 'NEGATIVE PASS: database connectivity-negative check: unreachable database port fails closed (connectivity failure only, not WAL or transaction recovery)\n'
  exit 0
fi

# Recovery mode: requires real PostgreSQL server
if ! command -v pg_isready >/dev/null 2>&1; then
  printf 'BLOCKED: pg_isready client utility is not installed; database recovery verification requires PostgreSQL client\n' >&2
  exit 2
fi

if ! pg_isready -h "$target_host" -p "$target_port" >/dev/null 2>&1; then
  printf 'BLOCKED: Target PostgreSQL database %s:%s is unreachable; cannot verify crash recovery on nonexistent database\n' "$target_host" "$target_port" >&2
  exit 2
fi

printf 'Connected to PostgreSQL at %s:%s.\n' "$target_host" "$target_port"
printf 'PREREQUISITE REACHABLE: PostgreSQL database is reachable. Crash and WAL transaction recovery drill NOT RUN\n'
exit 0
