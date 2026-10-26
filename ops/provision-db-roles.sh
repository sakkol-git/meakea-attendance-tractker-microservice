#!/usr/bin/env bash
# shellcheck disable=SC2016 # Quoted script expands inside the PostgreSQL container.
set -euo pipefail
# shellcheck source=ops/common.sh
source "$(dirname "$0")/common.sh"
ops_load
[[ "${HRFLOW_ROLE_CONFIRM:-}" == "$HRFLOW_TARGET_ID" ]] || ops_die 'set HRFLOW_ROLE_CONFIRM to the target alias'
ops_compose up -d postgres
sql_literal() { local escaped=${1//\'/\'\'}; printf '%s' "$escaped"; }
runtime_password=$(sql_literal "$HRFLOW_DB_RUNTIME_PASSWORD")
migration_password=$(sql_literal "$HRFLOW_DB_MIGRATION_PASSWORD")
backup_password=$(sql_literal "$HRFLOW_DB_BACKUP_PASSWORD")
# Secrets travel only through the container's private stdin, never process arguments or logs.
{
  printf "SELECT format('CREATE ROLE %%I LOGIN PASSWORD %%L', '%s', '%s') WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '%s')\\gexec\n" "$HRFLOW_DB_MIGRATION_USER" "$migration_password" "$HRFLOW_DB_MIGRATION_USER"
  printf "SELECT format('CREATE ROLE %%I LOGIN PASSWORD %%L', '%s', '%s') WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '%s')\\gexec\n" "$HRFLOW_DB_RUNTIME_USER" "$runtime_password" "$HRFLOW_DB_RUNTIME_USER"
  printf "SELECT format('CREATE ROLE %%I LOGIN PASSWORD %%L', '%s', '%s') WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '%s')\\gexec\n" "$HRFLOW_DB_BACKUP_USER" "$backup_password" "$HRFLOW_DB_BACKUP_USER"
  printf "ALTER ROLE %s PASSWORD '%s';\n" "$HRFLOW_DB_MIGRATION_USER" "$migration_password"
  printf "ALTER ROLE %s PASSWORD '%s';\n" "$HRFLOW_DB_RUNTIME_USER" "$runtime_password"
  printf "ALTER ROLE %s PASSWORD '%s';\n" "$HRFLOW_DB_BACKUP_USER" "$backup_password"
  printf 'GRANT pg_read_all_data TO %s;\n' "$HRFLOW_DB_BACKUP_USER"
  printf 'GRANT USAGE, CREATE ON SCHEMA identity_org TO %s;\n' "$HRFLOW_DB_MIGRATION_USER"
  printf 'GRANT USAGE ON SCHEMA identity_org TO %s;\n' "$HRFLOW_DB_RUNTIME_USER"
  printf 'GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA identity_org TO %s;\n' "$HRFLOW_DB_RUNTIME_USER"
  printf 'GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA identity_org TO %s;\n' "$HRFLOW_DB_RUNTIME_USER"
  printf 'ALTER DEFAULT PRIVILEGES FOR ROLE %s IN SCHEMA identity_org GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %s;\n' "$HRFLOW_DB_MIGRATION_USER" "$HRFLOW_DB_RUNTIME_USER"
  printf 'ALTER DEFAULT PRIVILEGES FOR ROLE %s IN SCHEMA identity_org GRANT USAGE, SELECT ON SEQUENCES TO %s;\n' "$HRFLOW_DB_MIGRATION_USER" "$HRFLOW_DB_RUNTIME_USER"
} | ops_compose exec -T postgres sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -q' >/dev/null
unset runtime_password migration_password backup_password
printf 'identity database roles provisioned for target=%s\n' "$HRFLOW_TARGET_ID"
