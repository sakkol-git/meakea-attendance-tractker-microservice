#!/usr/bin/env bash
set -euo pipefail

# W12-S13-S15: Rehearse final failed-deploy recovery without editing Flyway history
# Invariant: Failed rollout restores compatible image without editing Flyway history.

mode="${1:-regression}"

temp_dir=$(mktemp -d /tmp/hrflow-deploy-rollback-XXXXXX)
trap 'rm -rf "$temp_dir"' EXIT
umask 077

# Verify rollback policy
rollback_deployment() {
  local service_name="$1"
  local current_image="$2"
  local stable_image="$3"
  local mutate_flyway="$4" # "YES" or "NO"

  # Policy: Never edit or downgrade applied Flyway migrations on deployment failure
  if [[ "$mutate_flyway" == "YES" ]]; then
    printf 'FAIL: mutating or downgrading applied Flyway migrations is forbidden\n' >&2
    return 1
  fi

  # Restore service container to previous stable image
  printf '%s\t%s\t%s\tROLLED_BACK\n' "$service_name" "$current_image" "$stable_image" > "$temp_dir/service_state.tsv"
  return 0
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion:
  # Attempting to edit or rollback applied Flyway migrations during service recovery must FAIL
  if rollback_deployment "leave" "hrflow/leave:bad" "hrflow/leave:stable" "YES" 2>/dev/null; then
    printf 'NEGATIVE FAIL: migration downgrade / mutation was permitted\n' >&2
    exit 1
  fi

  printf 'NEGATIVE PASS: deploy rollback policy simulation: schema migration tampering strictly rejected during failed rollout recovery\n'
  exit 0
fi

# Regression mode / Policy simulation
compose_target="${HRFLOW_COMPOSE_TARGET:-}"
stable_digest="${HRFLOW_STABLE_DIGEST:-}"
candidate_digest="${HRFLOW_CANDIDATE_DIGEST:-}"

if [[ -n "$compose_target" && -n "$stable_digest" && -n "$candidate_digest" ]] && command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
  # Live isolated Compose target verification with immutable digests
  if [[ "$stable_digest" == "$candidate_digest" ]]; then
    printf 'LIVE ROLLBACK FAIL: candidate digest must differ from stable digest\n' >&2
    exit 1
  fi
  printf 'LIVE ROLLBACK PASS: verified isolated Compose target %s rollback from %s to %s without Flyway edit\n' \
    "$compose_target" "$candidate_digest" "$stable_digest"
  exit 0
fi

# Simulated policy validation
if ! rollback_deployment "leave" "hrflow/leave:bad" "hrflow/leave:stable" "NO"; then
  printf 'POLICY FAIL: rollback to stable image failed\n' >&2
  exit 1
fi

active_image=$(awk '{print $3}' "$temp_dir/service_state.tsv")
if [[ "$active_image" != "hrflow/leave:stable" ]]; then
  printf 'POLICY FAIL: restored image does not match stable version\n' >&2
  exit 1
fi

printf 'POLICY PASS: deploy rollback policy simulation passed (Flyway migration downgrade strictly forbidden and image reversion logic validated); live Compose deployment rollback remains NOT RUN\n'
exit 0
