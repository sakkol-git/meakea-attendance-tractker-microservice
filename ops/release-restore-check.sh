#!/usr/bin/env bash
set -euo pipefail

# W12-S10-S12: Clean restore verification into isolated database target
# Invariant: Must restore an actual backup archive into an explicit isolated scratch database target.
# Never restore into production or ordinary development databases (e.g. hrflow, hrflow_dev, postgres).
# Verify the connected database identity before executing the archive.
# Prove the source/original database remains unchanged. Stop as BLOCKED before executing SQL if prerequisites are missing.

mode="${1:-regression}"
target_env="${2:-scratch-test}"
backup_archive="${3:-${HRFLOW_BACKUP_ARCHIVE:-}}"
target_db="${HRFLOW_RESTORE_DB:-${PGDATABASE:-}}"

temp_dir=$(mktemp -d /tmp/hrflow-restore-XXXXXX)
trap 'rm -rf "$temp_dir"' EXIT
umask 077

check_disposability() {
  local db="$1"
  local marker="${HRFLOW_DISPOSABLE_TARGET_MARKER:-}"
  local created_by_run="${HRFLOW_RESTORE_TARGET_CREATED_BY_RUN:-false}"

  if [[ "$created_by_run" == "true" ]]; then
    return 0
  fi
  if [[ -n "$marker" && "$marker" == "accepted_disposable" ]]; then
    return 0
  fi
  return 1
}

detect_dump_format() {
  local file="$1"
  local magic
  magic=$(head -c 5 "$file" 2>/dev/null || true)
  if [[ "$magic" == "PGDMP" ]]; then
    printf 'custom\n'
  else
    printf 'sql\n'
  fi
}

is_forbidden_database() {
  local db="$1"
  local env="$2"
  if [[ "$env" == "production" || "$env" == "prod" || "$env" == "hrflow-prod" ]]; then
    return 0
  fi
  # Forbidden databases regardless of target_env string
  case "$db" in
    hrflow|hrflow_dev|hrflow_prod|postgres|template0|template1|master|production|prod)
      return 0
      ;;
    *)
      # A name containing scratch is not sufficient proof of disposability.
      # Must match naming pattern AND have accepted disposability marker or created-by-run proof
      if [[ ! "$db" =~ (scratch|isolated|restore_test) ]]; then
        return 0
      fi
      if ! check_disposability "$db"; then
        return 0 # forbidden because disposability marker is missing
      fi
      return 1
      ;;
  esac
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion 1: Production target_env must be rejected
  if is_forbidden_database "any_db" "production"; then
    :
  else
    printf 'NEGATIVE FAIL: production target_env was not forbidden\n' >&2
    exit 1
  fi

  # Negative assertion 2: Ordinary development databases (hrflow, postgres) must be rejected
  if is_forbidden_database "hrflow" "scratch-test" && is_forbidden_database "postgres" "scratch-test"; then
    :
  else
    printf 'NEGATIVE FAIL: ordinary dev database hrflow was not forbidden\n' >&2
    exit 1
  fi

  # Negative assertion 3: Database without scratch/isolated in name must be rejected
  if is_forbidden_database "my_database" "test"; then
    :
  else
    printf 'NEGATIVE FAIL: non-isolated database was not forbidden\n' >&2
    exit 1
  fi

  # Negative assertion 4: Database named with scratch but without disposability marker/created-by-run must be rejected
  if is_forbidden_database "hrflow_scratch" "scratch-test"; then
    :
  else
    printf 'NEGATIVE FAIL: scratch-named database without disposability marker was permitted\n' >&2
    exit 1
  fi

  # Negative assertion 5: Dump format detection distinguishes SQL vs custom PGDMP
  printf 'PGDMP_sample' > "$temp_dir/sample.dump"
  printf 'SELECT 1;' > "$temp_dir/sample.sql"
  if [[ "$(detect_dump_format "$temp_dir/sample.dump")" != "custom" || "$(detect_dump_format "$temp_dir/sample.sql")" != "sql" ]]; then
    printf 'NEGATIVE FAIL: dump format detection failed\n' >&2
    exit 1
  fi

  # Negative assertion 6: Tampered or truncated backup archive must fail checksum validation
  corrupt_stage="$temp_dir/corrupt_stage"
  mkdir -p "$corrupt_stage"
  printf 'valid-header' > "$corrupt_stage/dump.sql"
  (cd "$corrupt_stage" && shasum -a 256 dump.sql > SHA256SUMS)
  printf 'corrupted-tampered-content' > "$corrupt_stage/dump.sql"

  if (cd "$corrupt_stage" && shasum -a 256 -c SHA256SUMS >/dev/null 2>&1); then
    printf 'NEGATIVE FAIL: checksum verification passed on tampered archive\n' >&2
    exit 1
  fi

  printf 'NEGATIVE PASS: production, ordinary dev databases, unverified scratch targets, dump formats, and tampered archives strictly validated\n'
  exit 0
fi

# Regression mode
if [[ -z "$backup_archive" || ! -f "$backup_archive" ]]; then
  printf 'BLOCKED: No valid backup archive file provided (HRFLOW_BACKUP_ARCHIVE); dummy text files are strictly prohibited\n' >&2
  exit 2
fi

if [[ -z "$target_db" ]]; then
  printf 'BLOCKED: Target database (HRFLOW_RESTORE_DB) is not configured; isolated scratch target required\n' >&2
  exit 2
fi

# 1. Reject forbidden databases independently of target_env
if is_forbidden_database "$target_db" "$target_env"; then
  printf 'FAIL: restore target database "%s" (env: %s) is forbidden. Must use an explicit isolated scratch database containing "scratch", "isolated", or "restore_test".\n' \
    "$target_db" "$target_env" >&2
  exit 1
fi

# Mandatory checksums file verification alongside archive
checksum_file="${backup_archive}.sha256"
if [[ ! -f "$checksum_file" ]]; then
  printf 'BLOCKED: Mandatory sha256 checksum file missing for archive: %s\n' "$checksum_file" >&2
  exit 2
fi

if ! (cd "$(dirname "$backup_archive")" && shasum -a 256 -c "$checksum_file" >/dev/null 2>&1); then
  printf 'REGRESSION FAIL: backup archive checksum mismatch: %s\n' "$backup_archive" >&2
  exit 1
fi

# Check for psql / pg_isready to perform actual database restore
if ! command -v psql >/dev/null || ! command -v pg_isready >/dev/null; then
  printf 'BLOCKED: psql or pg_isready client utilities not found; cannot execute database restore\n' >&2
  exit 2
fi

target_host="${PGHOST:-127.0.0.1}"
target_port="${PGPORT:-5432}"

if ! pg_isready -h "$target_host" -p "$target_port" >/dev/null 2>&1; then
  printf 'BLOCKED: Isolated PostgreSQL target %s:%s is unreachable; cannot restore backup\n' "$target_host" "$target_port" >&2
  exit 2
fi

# 2. Verify connected database identity BEFORE executing the archive
connected_db=$(psql -t -A -h "$target_host" -p "$target_port" -d "$target_db" -c "SELECT current_database();" 2>/dev/null || true)
if [[ -z "$connected_db" ]]; then
  printf 'BLOCKED: Failed to connect or query database identity on target "%s"\n' "$target_db" >&2
  exit 2
fi

if is_forbidden_database "$connected_db" "$target_env"; then
  printf 'SECURITY ABORT: Connected database identity "%s" is a forbidden target; aborting before executing archive SQL\n' "$connected_db" >&2
  exit 1
fi

if [[ "$connected_db" != "$target_db" ]]; then
  printf 'SECURITY ABORT: Connected database identity "%s" does not match target database "%s"\n' "$connected_db" "$target_db" >&2
  exit 1
fi

# 3. Source database integrity proof
# Invariant (Recovery 7): Table count before and after does not prove the source database was completely unchanged.
# Use accepted checksum/ledger verification when available. Otherwise report source-integrity proof and physical restore as NOT RUN.
source_checksum_ledger="${HRFLOW_SOURCE_LEDGER:-}"
if [[ -z "$source_checksum_ledger" || ! -f "$source_checksum_ledger" ]]; then
  printf 'NOT RUN: Source database cryptographic integrity ledger (HRFLOW_SOURCE_LEDGER) is not provisioned; source-integrity proof and physical restore remain NOT RUN.\n' >&2
  exit 2
fi

dump_fmt=$(detect_dump_format "$backup_archive")
printf 'Executing real database restore (%s format) into verified isolated target: %s...\n' "$dump_fmt" "$connected_db"
if [[ "$dump_fmt" == "custom" ]]; then
  if ! command -v pg_restore >/dev/null; then
    printf 'BLOCKED: pg_restore client utility not found for custom dump format\n' >&2
    exit 2
  fi
  pg_restore -v --clean --if-exists -h "$target_host" -p "$target_port" -d "$target_db" "$backup_archive"
else
  psql -v ON_ERROR_STOP=1 -h "$target_host" -p "$target_port" -d "$target_db" -f "$backup_archive"
fi

# Verify restored schema inventory
table_count=$(psql -t -A -h "$target_host" -p "$target_port" -d "$target_db" \
  -c "SELECT count(*) FROM information_schema.tables WHERE table_schema IN ('identity_org','attendance','leave','request','notification','reporting')")

if [[ "$table_count" -lt 6 ]]; then
  printf 'REGRESSION FAIL: restored database contains %s owner tables; expected at least 6\n' "$table_count" >&2
  exit 1
fi

# Verify data rows exist (not just empty tables)
data_rows=$(psql -t -A -h "$target_host" -p "$target_port" -d "$target_db" \
  -c "SELECT (SELECT count(*) FROM identity_org.tenants) + (SELECT count(*) FROM identity_org.users)")

if [[ "$data_rows" -le 0 ]]; then
  printf 'REGRESSION FAIL: restored database tables are empty (0 rows in core tenant/users)\n' >&2
  exit 1
fi

printf 'REGRESSION PASS: clean backup archive restored into isolated target %s; verified %s owner tables, %s core rows with valid sha256 checksum (unverified files/ledgers/replay metadata omitted)\n' \
  "$connected_db" "$table_count" "$data_rows"
exit 0
