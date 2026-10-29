#!/usr/bin/env bash
set -euo pipefail

# W11-S04-S06: Concurrency and race condition invariants
# Invariant: Racing approvals, check-in and balances preserve exactly the DB invariants.

mode="${1:-regression}"

temp_dir=$(mktemp -d /tmp/hrflow-race-check-XXXXXX)
trap 'rm -rf "$temp_dir"' EXIT
umask 077

# Ledger files
balance_file="$temp_dir/leave_balance.txt"
attendance_file="$temp_dir/attendance_sessions.tsv"
printf '1' > "$balance_file" # 1 day remaining
touch "$attendance_file"

# Simulate atomic balance reservation using lock
reserve_leave() {
  local days_requested="$1"
  local lock_dir="$temp_dir/balance.lock"

  if ! mkdir "$lock_dir" 2>/dev/null; then
    # Lock contention / conflict
    return 1
  fi

  local current_balance
  current_balance=$(cat "$balance_file")
  if [[ "$current_balance" -lt "$days_requested" ]]; then
    rm -rf "$lock_dir"
    return 1 # Insufficient balance
  fi

  local new_balance=$((current_balance - days_requested))
  printf '%s' "$new_balance" > "$balance_file"
  rm -rf "$lock_dir"
  return 0
}

# Check-in validation: cannot check in if already checked in without checkout
record_checkin() {
  local employee_id="$1"
  local timestamp="$2"
  local idemp_key="$3"

  local active_session
  active_session=$(grep "^$employee_id	.*	ACTIVE$" "$attendance_file" || true)

  # If identical idempotency key, return reconciled success
  local existing_key
  existing_key=$(grep "^$employee_id	.*	$idemp_key	" "$attendance_file" || true)
  if [[ -n "$existing_key" ]]; then
    return 0 # Idempotent replay
  fi

  if [[ -n "$active_session" ]]; then
    # Interval overlap constraint violation
    return 1
  fi

  printf '%s\t%s\t%s\tACTIVE\n' "$employee_id" "$timestamp" "$idemp_key" >> "$attendance_file"
  return 0
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion 1: Double check-in without check-out must fail with conflict (409)
  record_checkin "emp-01" "2027-01-04T08:00:00Z" "key-01"
  if record_checkin "emp-01" "2027-01-04T08:05:00Z" "key-02"; then
    printf 'NEGATIVE FAIL: second concurrent check-in was accepted without check-out\n' >&2
    exit 1
  fi

  # Negative assertion 2: Overdrawing leave balance (attempting to reserve 2 days when only 1 exists)
  if reserve_leave 2; then
    printf 'NEGATIVE FAIL: leave reservation exceeded remaining balance\n' >&2
    exit 1
  fi

  printf 'POLICY NEGATIVE PASS: sequential policy simulation: check-in collision and balance overdraw rejected (sequential policy logic only, not database concurrency races)\n'
  exit 0
fi

# Regression mode / Sequential policy logic
# 1. First leave reservation takes the remaining 1 day
if ! reserve_leave 1; then
  printf 'POLICY FAIL: valid leave reservation failed\n' >&2
  exit 1
fi

# 2. Parallel second reservation racing for balance now fails with insufficient balance
if reserve_leave 1; then
  printf 'POLICY FAIL: racing second reservation unexpectedly succeeded on depleted balance\n' >&2
  exit 1
fi

# 3. Idempotent check-in retry returns identical success
record_checkin "emp-02" "2027-01-04T08:00:00Z" "key-idem-01"
if ! record_checkin "emp-02" "2027-01-04T08:00:00Z" "key-idem-01"; then
  printf 'POLICY FAIL: idempotent check-in retry failed\n' >&2
  exit 1
fi

# Verify exact row count
count=$(grep -c "emp-02" "$attendance_file" || true)
if [[ "$count" -ne 1 ]]; then
  printf 'POLICY FAIL: duplicate attendance rows created: %s\n' "$count" >&2
  exit 1
fi

printf 'POLICY PASS: sequential policy simulation of leave balance reservation, interval overlap prevention, and idempotent deduplication verified. Live concurrent database race testing remains NOT RUN\n'
exit 0
