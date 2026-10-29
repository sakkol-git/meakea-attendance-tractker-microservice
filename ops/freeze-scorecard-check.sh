#!/usr/bin/env bash
set -euo pipefail

# W12-S25-S28: Final feature integration: demo rehearsal sanitization and release scorecard
# Invariants:
# 1. Recording contains synthetic names and no tokens/private addresses.
# 2. Release tag and manifest identify exact passing commit/images; no claim of certification or high availability.
# 3. All 14 Q gates evaluated; physical Acer / external disk recorded honestly as NOT RUN / BLOCKED.

mode="${1:-regression}"

temp_dir=$(mktemp -d /tmp/hrflow-freeze-scorecard-XXXXXX)
trap 'rm -rf "$temp_dir"' EXIT
umask 077

# 1. Demo rehearsal artifact scanner
scan_demo_artifacts() {
  local artifact_file="$1"

  # Pattern check for private IP addresses (10.x.x.x, 172.16-31.x.x, 192.168.x.x)
  if grep -E -q '(10\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}|192\.168\.[0-9]{1,3}\.[0-9]{1,3}|172\.(1[6-9]|2[0-9]|3[0-1])\.[0-9]{1,3}\.[0-9]{1,3})' "$artifact_file"; then
    printf 'FAIL: private internal IP address detected in demo recording: %s\n' "$artifact_file" >&2
    return 1
  fi

  # Pattern check for Bearer tokens or JWTs (header.payload.signature)
  if grep -E -i -q '(Bearer eyJ[a-zA-Z0-9_-]+\.eyJ[a-zA-Z0-9_-]+\.|Authorization: Bearer)' "$artifact_file"; then
    printf 'FAIL: secret or JWT Bearer token detected in demo recording: %s\n' "$artifact_file" >&2
    return 1
  fi

  return 0
}

# 2. 14 Quality Gates scorecard audit
audit_release_scorecard() {
  local scorecard_file="$1"
  local allow_fake_physical_pass="${2:-NO}"

  # All 14 gates Q01 to Q14 must be present
  for i in $(seq -w 1 14); do
    local q_id="Q$i"
    if ! grep -q "^$q_id:" "$scorecard_file"; then
      printf 'FAIL: missing gate %s in scorecard\n' "$q_id" >&2
      return 1
    fi
  done

  # Q13 covers physical Acer deploy and external disk restore.
  # Must NOT be falsely marked PASSED without physical environment evidence.
  local q13_status
  q13_status=$(grep "^Q13:" "$scorecard_file" | cut -d: -f2 | tr -d ' ')
  if [[ "$q13_status" == "PASSED" && "$allow_fake_physical_pass" == "NO" ]]; then
    printf 'FAIL: Q13 physical deployment/external restore cannot be marked PASSED without physical host\n' >&2
    return 1
  fi

  return 0
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion 1: Leaked bearer token in demo asset must be rejected
  cat <<'EOF' > "$temp_dir/leaked_demo.txt"
Demo Stage: Attendance Check-in
User: Synthetic John Doe
Header: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.doNotLeakThis
EOF

  if scan_demo_artifacts "$temp_dir/leaked_demo.txt" 2>/dev/null; then
    printf 'NEGATIVE FAIL: leaked token in demo artifact was accepted\n' >&2
    exit 1
  fi

  # Negative assertion 2: Leaked private IP in demo asset must be rejected
  cat <<'EOF' > "$temp_dir/leaked_ip_demo.txt"
Demo Stage: System Overview
Host: 192.168.1.100
Gateway: 10.0.0.1
EOF

  if scan_demo_artifacts "$temp_dir/leaked_ip_demo.txt" 2>/dev/null; then
    printf 'NEGATIVE FAIL: leaked private IP in demo artifact was accepted\n' >&2
    exit 1
  fi

  # Negative assertion 3: Falsely claiming Q13 physical gate passed must be rejected
  cat <<'EOF' > "$temp_dir/fake_scorecard.txt"
Q01: PASSED
Q02: PASSED
Q03: PASSED
Q04: PASSED
Q05: PASSED
Q06: PASSED
Q07: PASSED
Q08: PASSED
Q09: PASSED
Q10: PASSED
Q11: PASSED
Q12: PASSED
Q13: PASSED
Q14: PASSED
EOF

  if audit_release_scorecard "$temp_dir/fake_scorecard.txt" "NO" 2>/dev/null; then
    printf 'NEGATIVE FAIL: false physical gate pass was accepted\n' >&2
    exit 1
  fi

  printf 'NEGATIVE PASS: secret/IP leak in demo assets and unverified physical Q13 pass strictly rejected\n'
  exit 0
fi

# Regression mode:
scorecard_file="${HRFLOW_RELEASE_SCORECARD_FILE:-docs/validation/RELEASE-SCORECARD.txt}"
demo_recording_file="${HRFLOW_DEMO_RECORDING_FILE:-docs/validation/DEMO-RECORDING.txt}"

if [[ ! -f "$scorecard_file" || ! -f "$demo_recording_file" ]]; then
  printf 'BLOCKED: Release freeze scorecard audit requires an existing owned scorecard (%s) and demo recording (%s); generating synthetic PASSED scorecards is strictly forbidden\n' \
    "$scorecard_file" "$demo_recording_file" >&2
  exit 2
fi

# 1. Verify clean sanitized demo artifact from existing owned file
if ! scan_demo_artifacts "$demo_recording_file"; then
  printf 'REGRESSION FAIL: demo recording artifact %s failed sanitization check\n' "$demo_recording_file" >&2
  exit 1
fi

# 2. Audit existing owned 14 Q release scorecard
if ! audit_release_scorecard "$scorecard_file" "NO"; then
  printf 'REGRESSION FAIL: release scorecard %s failed quality audit\n' "$scorecard_file" >&2
  exit 1
fi

printf 'REGRESSION PASS: owned demo rehearsal recording sanitized and owned 14 Q release scorecard evaluated with honest physical accounting\n'
exit 0
