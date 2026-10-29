#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=ops/common.sh
source "$(dirname "$0")/common.sh"
ops_load
ops_external_mount
: "${HRFLOW_BACKUP_MOUNT:?}"
receipt="$HRFLOW_BACKUP_MOUNT/hrflow/latest-verified"
[[ -f "$receipt" && ! -L "$receipt" ]] || ops_die 'no verified external backup receipt'
verified_backup=$(cat "$receipt")
[[ "$verified_backup" == "$HRFLOW_BACKUP_MOUNT/hrflow/"* && -d "$verified_backup" && ! -L "$verified_backup" ]] || ops_die 'verified backup path outside encrypted mount'
(cd "$verified_backup" && sha256sum -c SHA256SUMS >/dev/null) || ops_die 'verified backup checksum failed'
[[ -f "$verified_backup/restore-report.txt" ]] || ops_die 'reviewed restore report missing'
[[ "${HRFLOW_DEPLOY_CONFIRM:-}" == "$HRFLOW_TARGET_ID" ]] || ops_die 'set HRFLOW_DEPLOY_CONFIRM to the target alias'
ops_compose config --quiet
release=$(git rev-parse HEAD)
[[ -z "$(git status --porcelain --untracked-files=no)" ]] || ops_die 'reviewed release checkout must be clean'
mkdir -p "$HRFLOW_BACKUP_MOUNT/hrflow/deployments"
previous="$HRFLOW_BACKUP_MOUNT/hrflow/deployments/previous-$(date -u +%Y%m%dT%H%M%SZ).txt"
{ printf 'target=%s\nprevious_git=%s\ncandidate_git=%s\n' "$HRFLOW_TARGET_ID" "${HRFLOW_PREVIOUS_GIT_REVISION:-unknown}" "$release"
  ops_compose images --format json
} > "$previous"
chmod 600 "$previous"
ops_compose build api-gateway identity-org web
ops_compose up -d --no-build postgres kafka schema-registry identity-org api-gateway web caddy
for ((attempt = 0; attempt < 24; attempt++)); do
  if [[ "$(ops_compose ps --status running --services | sort | tr '\n' ' ')" == *'api-gateway'* ]] &&
     ops_compose exec -T api-gateway wget -q -T 5 -O - http://127.0.0.1:9080/actuator/health/readiness | grep -q '"status":"UP"' &&
     ops_compose exec -T identity-org wget -q -T 5 -O - http://127.0.0.1:8101/actuator/health/readiness | grep -q '"status":"UP"' &&
     ops_compose exec -T web wget -q -T 5 -O /dev/null http://127.0.0.1:3000/login; then
    printf 'private readiness passed; target=%s candidate=%s\n' "$HRFLOW_TARGET_ID" "$release"
    exit 0
  fi
  sleep 5
done
ops_die "readiness timed out; prior image IDs saved at $previous; follow rollback runbook"
