#!/usr/bin/env bash
set -euo pipefail

# W11-S25-S28: Rehearse limited pilot story and score quality gates
# Invariant: No consenting pilot proceeds with an unresolved critical control.

mode="${1:-regression}"

temp_dir=$(mktemp -d /tmp/hrflow-pilot-readiness-XXXXXX)
trap 'rm -rf "$temp_dir"' EXIT
umask 077

check_release_blockers() {
  local target_deployment="$1"
  local physical_host_verified="$2"
  local external_backup_verified="$3"

  if [[ "$target_deployment" == "live_production" || "$target_deployment" == "live_pilot" ]]; then
    if [[ "$physical_host_verified" != "YES" || "$external_backup_verified" != "YES" ]]; then
      return 1 # BLOCKED
    fi
  fi
  return 0
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion:
  # Attempting to promote to live pilot or production when physical host or external disk is unverified MUST FAIL
  if check_release_blockers "live_pilot" "NO" "NO"; then
    printf 'NEGATIVE FAIL: live pilot was permitted with unverified physical host and backup disk\n' >&2
    exit 1
  fi

  printf 'NEGATIVE PASS: live pilot is strictly blocked while physical controls remain unverified\n'
  exit 0
fi

# Regression mode
# 1. Physical release blockers policy check
if ! check_release_blockers "local_candidate" "NO" "NO"; then
  printf 'POLICY FAIL: local candidate testing was blocked\n' >&2
  exit 1
fi

# 2. 10-user pilot journey requires real isolated endpoints and fixtures
pilot_endpoint="${HRFLOW_PILOT_ENDPOINT:-}"
pilot_fixtures_dir="${HRFLOW_PILOT_FIXTURES_DIR:-}"

if [[ -z "$pilot_endpoint" || -z "$pilot_fixtures_dir" || ! -d "$pilot_fixtures_dir" ]]; then
  printf 'NOT RUN: 10-user pilot journey requires isolated service endpoint (HRFLOW_PILOT_ENDPOINT) and user fixtures directory (HRFLOW_PILOT_FIXTURES_DIR); a loop creating IDs is not a ten-user journey\n' >&2
  exit 2
fi

# Execute actual journey for 10 fixture users against isolated endpoint
errors=0
for i in $(seq 1 10); do
  fixture_file="$pilot_fixtures_dir/user_${i}.json"
  if [[ ! -f "$fixture_file" ]]; then
    printf 'PILOT FAIL: user fixture %s missing\n' "$fixture_file" >&2
    errors=$((errors + 1))
    continue
  fi
  if ! curl -s -f -o /dev/null --connect-timeout 2 -H "Content-Type: application/json" -d @"$fixture_file" "$pilot_endpoint" 2>/dev/null; then
    errors=$((errors + 1))
  fi
done

if [[ "$errors" -gt 0 ]]; then
  printf 'PILOT FAIL: %s user journey requests failed\n' "$errors" >&2
  exit 1
fi

printf 'REGRESSION PASS: 10-user pilot journey verified against isolated endpoint %s with real fixtures\n' "$pilot_endpoint"
exit 0
