#!/usr/bin/env bash
set -euo pipefail

ops_die() { printf 'HRFlow ops: %s\n' "$*" >&2; exit 1; }
ops_need() { command -v "$1" >/dev/null || ops_die "missing command: $1"; }

ops_load() {
  : "${HRFLOW_OPS_CONFIG:?Set HRFLOW_OPS_CONFIG to an operator-owned file outside Git}"
  [[ -f "$HRFLOW_OPS_CONFIG" && ! -L "$HRFLOW_OPS_CONFIG" ]] || ops_die 'config must be a regular file'
  local mode owner
  mode=$(stat -c %a "$HRFLOW_OPS_CONFIG")
  owner=$(stat -c %u "$HRFLOW_OPS_CONFIG")
  [[ "$mode" =~ ^[0-7]+$ && $((8#$mode & 8#077)) -eq 0 ]] || ops_die 'config must not be group/world accessible'
  [[ "$owner" == "$(id -u)" ]] || ops_die 'config must be owned by the operator'
  # This file is trusted operator shell configuration, never a downloaded artifact.
  set -a
  # shellcheck disable=SC1090 # Operator-owned path is validated above.
  source "$HRFLOW_OPS_CONFIG"
  set +a
  : "${HRFLOW_ROOT:?}" "${HRFLOW_TARGET_ID:?}" "${HRFLOW_MODE:?}" "${COMPOSE_PROJECT_NAME:?}"
  [[ "$(cd "$HRFLOW_ROOT" && pwd -P)" == "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)" ]] || ops_die 'HRFLOW_ROOT is not this checkout'
  [[ "$HRFLOW_MODE" == production ]] || ops_die 'Week 2 scripts require production mode'
  [[ "$HRFLOW_TARGET_ID" =~ ^[a-z][a-z0-9-]{2,40}$ ]] || ops_die 'invalid target alias'
  [[ "$COMPOSE_PROJECT_NAME" == hrflow-selfhost ]] || ops_die 'unexpected Compose project'
  [[ "${HRFLOW_EDGE_BIND_IP:-127.0.0.1}" == 127.0.0.1 ]] || ops_die 'Week 2 candidate must keep edge on loopback'
  [[ "${HRFLOW_BROWSER_ORIGIN:-https://hrflow.test}" == https://hrflow.test ]] || ops_die 'browser origin must match the reviewed Caddy site'
  [[ -d "$HRFLOW_ROOT/web" && -f "$HRFLOW_ROOT/web/package-lock.json" ]] || ops_die 'matching web checkout missing'
  [[ -n "${POSTGRES_PASSWORD:-}" && -n "${HRFLOW_DB_RUNTIME_PASSWORD:-}" && -n "${HRFLOW_DB_MIGRATION_PASSWORD:-}" && -n "${HRFLOW_DB_BACKUP_PASSWORD:-}" ]] || ops_die 'database credentials missing'
  [[ "${HRFLOW_DB_RUNTIME_USER:-}" =~ ^[a-z_][a-z0-9_]*$ && "${HRFLOW_DB_MIGRATION_USER:-}" =~ ^[a-z_][a-z0-9_]*$ && "${HRFLOW_DB_BACKUP_USER:-}" =~ ^[a-z_][a-z0-9_]*$ ]] || ops_die 'database role names invalid'
  [[ "$HRFLOW_DB_RUNTIME_USER" != "$HRFLOW_DB_MIGRATION_USER" && "$HRFLOW_DB_RUNTIME_USER" != "$HRFLOW_DB_BACKUP_USER" && "$HRFLOW_DB_MIGRATION_USER" != "$HRFLOW_DB_BACKUP_USER" && "$HRFLOW_DB_RUNTIME_USER" != hrflow && "$HRFLOW_DB_MIGRATION_USER" != hrflow && "$HRFLOW_DB_BACKUP_USER" != hrflow ]] || ops_die 'runtime, migration, backup and owner roles must differ'
  if [[ -n "${HRFLOW_DB_JOB_USER:-}" || -n "${HRFLOW_DB_JOB_PASSWORD:-}" ]]; then
    [[ "${HRFLOW_DB_JOB_USER:-}" =~ ^[a-z_][a-z0-9_]*$ && -n "${HRFLOW_DB_JOB_PASSWORD:-}" ]] || ops_die 'job role name/password must be supplied together'
    [[ "$HRFLOW_DB_JOB_USER" != "$HRFLOW_DB_RUNTIME_USER" && "$HRFLOW_DB_JOB_USER" != "$HRFLOW_DB_MIGRATION_USER" && "$HRFLOW_DB_JOB_USER" != "$HRFLOW_DB_BACKUP_USER" && "$HRFLOW_DB_JOB_USER" != hrflow ]] || ops_die 'job role must differ from runtime, migration, backup and owner roles'
  fi
  [[ -d "${HRFLOW_SIGNING_KEYS_DIR:-}" && -f "$HRFLOW_SIGNING_KEYS_DIR/private.pem" && -f "$HRFLOW_SIGNING_KEYS_DIR/public.pem" ]] || ops_die 'mounted signing key pair missing'
  ops_need docker
  docker compose version >/dev/null || ops_die 'Docker Compose unavailable'
  cd "$HRFLOW_ROOT"
  export HRFLOW_COMPOSE_FILE="$HRFLOW_ROOT/infra/selfhost/compose.yaml"
}

ops_compose() { docker compose -f "$HRFLOW_COMPOSE_FILE" "$@"; }

ops_external_mount() {
  : "${HRFLOW_BACKUP_MOUNT:?}" "${HRFLOW_BACKUP_MAPPER:?}"
  [[ "$HRFLOW_BACKUP_MOUNT" == /* && "$HRFLOW_BACKUP_MOUNT" != / ]] || ops_die 'invalid external mount'
  ops_need findmnt
  ops_need cryptsetup
  [[ "$(findmnt -n -o SOURCE --target "$HRFLOW_BACKUP_MOUNT")" == "/dev/mapper/$HRFLOW_BACKUP_MAPPER" ]] || ops_die 'backup target is not the named encrypted mount'
  cryptsetup status "$HRFLOW_BACKUP_MAPPER" >/dev/null || ops_die 'encrypted backup mapper unavailable'
  [[ -w "$HRFLOW_BACKUP_MOUNT" ]] || ops_die 'backup mount not writable'
}
