#!/usr/bin/env bash
set -uo pipefail

# Week 12 Acceptance and Demonstration Runner
# Modes:
#   static (or negative)   - Negative assertion checks across manifest, matrix, restore, rollback, demo, and scorecard
#   regression (or live)   - Full regression aggregation across modules, manifests, restore, rollback, demo, and scorecards

command_mode="${1:-regression}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../ops" && pwd)"

w12_scripts=(
  "release-manifest-check.sh"
  "full-matrix-acceptance-check.sh"
  "release-restore-check.sh"
  "deploy-rollback-check.sh"
  "demonstration-check.sh"
  "freeze-scorecard-check.sh"
)

pass_count=0
fail_count=0
blocked_count=0
not_run_count=0

run_arg=""
case "$command_mode" in
  static|negative)
    run_arg="negative"
    printf '=== Running Week 12 Acceptance Static / Negative Checks ===\n'
    ;;
  regression|live)
    run_arg="regression"
    printf '=== Running Week 12 Acceptance Regression Checks ===\n'
    ;;
  *)
    printf 'Usage: %s [static|regression|negative|live]\n' "$0" >&2
    exit 1
    ;;
esac

for script in "${w12_scripts[@]}"; do
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
printf '\n=== Week 12 Acceptance Summary ===\n'
printf 'Total: %s | PASS: %s | BLOCKED: %s | NOT RUN: %s | FAIL: %s\n' \
  "$total_count" "$pass_count" "$blocked_count" "$not_run_count" "$fail_count"

if [[ $fail_count -gt 0 ]]; then
  printf 'RESULT: FAIL (%s failed checks)\n' "$fail_count" >&2
  exit 1
elif [[ $((blocked_count + not_run_count)) -gt 0 ]]; then
  printf 'RESULT: INCOMPLETE / BLOCKED (%s blocked, %s not run; required release scopes remain unfulfilled)\n' \
    "$blocked_count" "$not_run_count"
  exit 2
else
  printf 'RESULT: PASS (all %s release acceptance checks satisfied)\n' "$pass_count"
  exit 0
fi
