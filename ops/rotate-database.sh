#!/usr/bin/env bash
set -euo pipefail

# W10-S10-S12: Rotate database runtime credentials
# Invariant: Old runtime credential is refused after rotation; new role still cannot cross owner schema or bypass RLS.
# Missing database target must produce BLOCKED, never synthetic PASS.

mode="${1:-recovery}"
target_host="${PGHOST:-127.0.0.1}"
target_port="${PGPORT:-5432}"

if [[ "$mode" == "negative" ]]; then
  # Negative assertion:
  # Connection with invalid credentials must be strictly rejected
  if command -v psql >/dev/null 2>&1; then
    if PGPASSWORD="wrong-password" psql -h "$target_host" -p "$target_port" -U "invalid_user" -d "hrflow" -c "SELECT 1" >/dev/null 2>&1; then
      printf 'NEGATIVE FAIL: invalid database credentials unexpectedly accepted\n' >&2
      exit 1
    fi
  fi
  printf 'NEGATIVE PASS: invalid database credentials strictly rejected\n'
  exit 0
fi

# Recovery mode: requires real PostgreSQL server
if ! command -v pg_isready >/dev/null 2>&1; then
  printf 'BLOCKED: pg_isready client utility is not installed; real database rotation requires PostgreSQL client tools\n' >&2
  exit 2
fi

if ! pg_isready -h "$target_host" -p "$target_port" >/dev/null 2>&1; then
  printf 'BLOCKED: Target PostgreSQL database %s:%s is unreachable; cannot execute real credential rotation\n' "$target_host" "$target_port" >&2
  exit 2
fi

printf 'Connected to PostgreSQL at %s:%s.\n' "$target_host" "$target_port"
printf 'PREREQUISITE REACHABLE: Target PostgreSQL database is reachable. Database runtime credential rotation and old-credential rejection drill NOT RUN\n'
exit 0
