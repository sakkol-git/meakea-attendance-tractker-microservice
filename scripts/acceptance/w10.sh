#!/usr/bin/env bash
set -uo pipefail

# Week 10 Operations Acceptance Runner
# Modes:
#   static (or negative)       - Negative policy enforcement and static bounds checks
#   prerequisite (or recovery) - Infrastructure prerequisites and policy harness checks
#   live                       - Live infrastructure drills (host reboot, container rollback, physical external restore)

command_mode="${1:-prerequisite}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../ops" && pwd)"

w10_scripts=(
  "host-reboot-check.sh"
  "rollback-check.sh"
  "rotate-jwt.sh"
  "rotate-database.sh"
  "rotate-tunnel.sh"
  "backup-check.sh"
  "restore-clean.sh"
  "restore-client-check.sh"
  "disk-check.sh"
  "recovery-check.sh"
  "database-recovery-check.sh"
  "broker-recovery-check.sh"
)

pass_count=0
fail_count=0
blocked_count=0
not_run_count=0

run_arg=""
case "$command_mode" in
  static|negative)
    run_arg="negative"
    printf '=== Running Week 10 Operations Static / Negative Checks ===\n'
    ;;
  prerequisite|recovery)
    run_arg="recovery"
    printf '=== Running Week 10 Operations Prerequisite / Harness Checks ===\n'
    ;;
  live)
    run_arg="recovery"
    export HRFLOW_CHECK_LIVE_DISK="true"
    printf '=== Running Week 10 Operations Live Infrastructure Drills ===\n'
    ;;
  *)
    printf 'Usage: %s [static|prerequisite|live|negative|recovery]\n' "$0" >&2
    exit 1
    ;;
esac

for script in "${w10_scripts[@]}"; do
  script_path="$script_dir/$script"
  if [[ ! -f "$script_path" ]]; then
    printf 'MISSING: %s not found\n' "$script" >&2
    fail_count=$((fail_count + 1))
    continue
  fi

  out=$(bash "$script_path" "$run_arg" 2>&1)
  status=$?

  first_line=$(echo "$out" | head -n 1)
  last_line=$(echo "$out" | tail -n 1)

  if [[ $status -eq 0 ]]; then
    pass_count=$((pass_count + 1))
    printf '[PASS] %s: %s\n' "$script" "$last_line"
  elif [[ $status -eq 2 ]]; then
    if echo "$out" | grep -q "NOT RUN"; then
      not_run_count=$((not_run_count + 1))
      printf '[NOT RUN] %s: %s\n' "$script" "$last_line"
    else
      blocked_count=$((blocked_count + 1))
      printf '[BLOCKED] %s: %s\n' "$script" "$last_line"
    fi
  else
    fail_count=$((fail_count + 1))
    printf '[FAIL] %s (exit %s): %s\n' "$script" "$status" "$last_line"
  fi
done

total_count=$((pass_count + fail_count + blocked_count + not_run_count))
printf '\n=== Week 10 Operations Summary ===\n'
printf 'Total: %s | PASS: %s | BLOCKED: %s | NOT RUN: %s | FAIL: %s\n' \
  "$total_count" "$pass_count" "$blocked_count" "$not_run_count" "$fail_count"

if [[ $fail_count -gt 0 ]]; then
  printf 'RESULT: FAIL (%s failed checks)\n' "$fail_count" >&2
  exit 1
elif [[ $((blocked_count + not_run_count)) -gt 0 ]]; then
  printf 'RESULT: INCOMPLETE / BLOCKED (%s blocked, %s not run; never converted to success)\n' "$blocked_count" "$not_run_count"
  exit 2
else
  printf 'RESULT: PASS (all %s checks satisfied)\n' "$pass_count"
  exit 0
fi
