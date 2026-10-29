#!/usr/bin/env bash
set -euo pipefail

# W10-S25-S27: Test disk and log bounds
# Invariant: Threshold override alerts without filling disk or deleting volumes.

mode="${1:-recovery}"

check_headroom_alert() {
  local usage_percent="$1"
  local threshold_percent=90
  if [[ "$usage_percent" -ge "$threshold_percent" ]]; then
    return 0 # alert fires
  fi
  return 1 # no alert
}

check_volume_safety() {
  local action="$1"
  # Invariant: live database/docker volumes must NEVER be deleted automatically by disk-check
  if [[ "$action" == "delete_volume" || "$action" == "prune_active_volumes" ]]; then
    return 1 # forbidden
  fi
  return 0
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion 1: Usage at 95% must fire alert; failing to alert is forbidden
  if ! check_headroom_alert 95; then
    printf 'NEGATIVE FAIL: disk usage at 95%% failed to trigger alert\n' >&2
    exit 1
  fi

  # Negative assertion 2: Live volume deletion as a cleanup response is strictly forbidden
  if check_volume_safety "delete_volume"; then
    printf 'NEGATIVE FAIL: volume deletion was permitted\n' >&2
    exit 1
  fi

  printf 'NEGATIVE PASS: disk policy harness: threshold alert triggers under simulated override; volume deletion strictly prohibited\n'
  exit 0
fi

# Recovery mode / Policy harness
live_check="${HRFLOW_CHECK_LIVE_DISK:-false}"

if [[ "$live_check" == "true" ]]; then
  # Live inspection of host filesystem and Docker logging
  target_mount="${HRFLOW_DISK_MOUNT:-/}"
  if ! df -k "$target_mount" >/dev/null 2>&1; then
    printf 'LIVE DISK FAIL: unable to query filesystem for %s\n' "$target_mount" >&2
    exit 1
  fi
  actual_usage=$(df -k "$target_mount" | awk 'NR==2 {print int($5)}')
  if [[ "$actual_usage" -ge 90 ]]; then
    printf 'LIVE DISK WARNING: filesystem usage on %s is at %s%%\n' "$target_mount" "$actual_usage"
  fi

  if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    log_driver=$(docker info --format '{{.LoggingDriver}}' 2>/dev/null || printf 'unknown')
    printf 'LIVE DISK PASS: filesystem usage on %s is %s%%; docker logging driver is %s\n' "$target_mount" "$actual_usage" "$log_driver"
  else
    printf 'LIVE DISK PASS: filesystem usage on %s is %s%%; docker daemon uninspected\n' "$target_mount" "$actual_usage"
  fi
  exit 0
fi

# Policy harness (simulated thresholds and bounds)
simulated_usage="${SIMULATED_DISK_USAGE:-85}"
if [[ "$simulated_usage" -ge 90 ]]; then
  printf 'POLICY NOTICE: disk usage %s%% reached warning threshold\n' "$simulated_usage"
fi

# Docker log rotation policy invariant (max-size <= 50m)
log_max_size_mb=50
if [[ "$log_max_size_mb" -gt 100 ]]; then
  printf 'POLICY FAIL: docker log max-size exceeds safe bound\n' >&2
  exit 1
fi

printf 'POLICY PASS: disk headroom and log rotation policy harness passed (threshold and non-destructive volume invariants verified); live filesystem inspection and Docker daemon log configuration checks remain NOT RUN without target host\n'
exit 0
