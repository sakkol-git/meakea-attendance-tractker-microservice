#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
ops_load
ops_external_mount
: "${HRFLOW_SCRATCH_DB:?Set a unique scratch database name}"
[[ "$HRFLOW_SCRATCH_DB" =~ ^hrflow_restore_[a-z0-9_]{8,48}$ ]] || ops_die 'scratch database name must be unique and prefixed hrflow_restore_'
[[ "$HRFLOW_SCRATCH_DB" != "${POSTGRES_DB:-hrflow}" ]] || ops_die 'production database refused'
[[ "${HRFLOW_RESTORE_CONFIRM:-}" == "$HRFLOW_SCRATCH_DB" ]] || ops_die 'set HRFLOW_RESTORE_CONFIRM to the exact scratch database'
root="$HRFLOW_BACKUP_MOUNT/hrflow"
[[ -f "$root/latest-unverified" ]] || ops_die 'no backup candidate'
backup=$(cat "$root/latest-unverified")
[[ "$backup" == "$root/"* && -d "$backup" && ! -L "$backup" ]] || ops_die 'backup path outside external mount'
(cd "$backup" && sha256sum -c SHA256SUMS >/dev/null)
pg_major=$(sed -n 's/^postgres_major=//p' "$backup/manifest.txt")
[[ "$pg_major" == 18 ]] || ops_die 'PostgreSQL major mismatch'
pg_restore --list "$backup/database.dump" >/dev/null
git bundle verify "$backup/source.bundle" >/dev/null
existing=$(ops_compose exec -T postgres sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname = '\''$1'\''"' sh "$HRFLOW_SCRATCH_DB")
[[ "$existing" != 1 ]] || ops_die 'scratch database already exists'
ops_compose exec -T postgres sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" createdb -U "$POSTGRES_USER" "$1"' sh "$HRFLOW_SCRATCH_DB"
ops_compose exec -T postgres sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_restore --exit-on-error --no-owner --no-acl -U "$POSTGRES_USER" -d "$1"' sh "$HRFLOW_SCRATCH_DB" < "$backup/database.dump"
ops_compose exec -T postgres sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$1" -Atc "SELECT count(*) FROM identity_org.auth_user"' sh "$HRFLOW_SCRATCH_DB" >/dev/null
printf 'scratch database restored: %s; manual auth, file, grant and replay checks remain required\n' "$HRFLOW_SCRATCH_DB"
