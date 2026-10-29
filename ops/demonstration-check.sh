#!/usr/bin/env bash
set -euo pipefail

# W12-S19-S24: Quality and failure recovery: timed demonstration script and support handover
# Invariants:
# 1. Network failure uses recorded evidence, never fabricated live success.
# 2. Owner/runbook/backup responsibility and residual risks are explicitly accepted as known limits.

mode="${1:-regression}"

temp_dir=$(mktemp -d /tmp/hrflow-demo-check-XXXXXX)
trap 'rm -rf "$temp_dir"' EXIT
umask 077

# 1. Demonstration runner validation
run_demo_stage() {
  local stage_name="$1"
  local network_available="$2" # "ONLINE" or "OFFLINE"
  local allow_fabrication="$3" # "YES" or "NO"

  if [[ "$network_available" == "OFFLINE" ]]; then
    if [[ "$allow_fabrication" == "YES" ]]; then
      # Forbidden: fabricating live success when network is down
      printf 'FAIL: fabricating live success during network failure is forbidden\n' >&2
      return 1
    fi
    # Allowed: Fallback to recorded evidence trail with clear offline marker
    printf '%s\tOFFLINE_RECORDED_EVIDENCE\n' "$stage_name" >> "$temp_dir/demo_log.tsv"
    return 0
  fi

  printf '%s\tLIVE_SUCCESS\n' "$stage_name" >> "$temp_dir/demo_log.tsv"
  return 0
}

# 2. Support handover validation
validate_support_handover() {
  local manifest="$1"

  # Handover must explicitly document known single-host limits and operator assignment
  local required_limits=(
    "SINGLE_HOST_DOWNTIME"
    "POWER_AND_INTERNET_DEPENDENCY"
    "CLOUDFLARE_TUNNEL_DEPENDENCY"
    "DESKTOP_LOGIN_REQUIREMENT"
    "BACKUP_OPERATOR_ASSIGNED"
    "SCRATCH_RESTORE_RUNBOOK"
  )

  for limit in "${required_limits[@]}"; do
    if ! grep -q "^$limit=" "$manifest"; then
      printf 'FAIL: missing required operational limit or handover responsibility: %s\n' "$limit" >&2
      return 1
    fi
  done

  # Reject false claims of high-availability / multi-region clustering
  if grep -qi "HIGH_AVAILABILITY=TRUE" "$manifest" 2>/dev/null; then
    printf 'FAIL: false claim of high availability is forbidden\n' >&2
    return 1
  fi

  return 0
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion 1: Fabricating live success when network fails is forbidden
  if run_demo_stage "leave-conflict-cancel" "OFFLINE" "YES" 2>/dev/null; then
    printf 'NEGATIVE FAIL: fabricated live success was permitted\n' >&2
    exit 1
  fi

  # Negative assertion 2: Handover omitting backup operator or claiming HA is forbidden
  cat <<'EOF' > "$temp_dir/bad_handover.env"
SINGLE_HOST_DOWNTIME=ACCEPTED
POWER_AND_INTERNET_DEPENDENCY=ACCEPTED
CLOUDFLARE_TUNNEL_DEPENDENCY=ACCEPTED
DESKTOP_LOGIN_REQUIREMENT=ACCEPTED
HIGH_AVAILABILITY=TRUE
EOF

  if validate_support_handover "$temp_dir/bad_handover.env" 2>/dev/null; then
    printf 'NEGATIVE FAIL: false HA claim in handover was accepted\n' >&2
    exit 1
  fi

  printf 'NEGATIVE PASS: fabricated live success and unverified HA handover claims strictly rejected\n'
  exit 0
fi

# Regression mode:
demo_manifest="${HRFLOW_DEMO_MANIFEST:-docs/validation/DEMO-MANIFEST.tsv}"
handover_file="${HRFLOW_HANDOVER_FILE:-docs/validation/SUPPORT-HANDOVER.env}"

if [[ ! -f "$demo_manifest" || ! -f "$handover_file" ]]; then
  printf 'NOT RUN: Timed demonstration verification requires existing artifact manifest (%s) and handover file (%s) with real paths, sha256 hashes, dates, and git revisions; offline evidence records must not be manufactured\n' \
    "$demo_manifest" "$handover_file" >&2
  exit 2
fi

# Verify support handover manifest
if ! validate_support_handover "$handover_file"; then
  printf 'REGRESSION FAIL: support handover manifest %s failed validation\n' "$handover_file" >&2
  exit 1
fi

# Verify all 8 demo stages exist in manifest with existing artifact paths, hashes, dates and revisions
stages=(
  "tenant-security"
  "check-in-retry"
  "stopped-worker"
  "leave-conflict-cancel"
  "material-approval"
  "correction"
  "csv-export"
  "restore-evidence"
)

for stage in "${stages[@]}"; do
  stage_entry=$(grep -E "^${stage}[[:space:]]" "$demo_manifest" || true)
  if [[ -z "$stage_entry" ]]; then
    printf 'REGRESSION FAIL: missing demo stage %s in %s\n' "$stage" "$demo_manifest" >&2
    exit 1
  fi

  artifact_path=$(printf '%s' "$stage_entry" | awk '{print $2}')
  expected_hash=$(printf '%s' "$stage_entry" | awk '{print $3}')
  rec_date=$(printf '%s' "$stage_entry" | awk '{print $4}')
  git_rev=$(printf '%s' "$stage_entry" | awk '{print $5}')

  if [[ ! -f "$artifact_path" ]]; then
    printf 'REGRESSION FAIL: demo artifact path %s for stage %s does not exist\n' "$artifact_path" "$stage" >&2
    exit 1
  fi

  if [[ -z "$expected_hash" || -z "$rec_date" || -z "$git_rev" ]]; then
    printf 'REGRESSION FAIL: incomplete metadata (hash/date/rev) for stage %s in manifest\n' "$stage" >&2
    exit 1
  fi

  actual_hash=$(shasum -a 256 "$artifact_path" | awk '{print $1}')
  if [[ "$actual_hash" != "$expected_hash" ]]; then
    printf 'REGRESSION FAIL: hash mismatch for stage %s artifact %s\n' "$stage" "$artifact_path" >&2
    exit 1
  fi
done

printf 'REGRESSION PASS: timed demonstration verified against existing artifact manifest with valid hashes, dates, revisions, and support handover limits\n'
exit 0
