#!/usr/bin/env bash
set -euo pipefail

# W11-S19-S24: Review unresolved policy and incidents, gate pilot consent
# Invariant: Missing HR consent/security/restore blocks real pilot; synthetic demo remains possible.

mode="${1:-regression}"

temp_dir=$(mktemp -d /tmp/hrflow-pilot-consent-XXXXXX)
trap 'rm -rf "$temp_dir"' EXIT
umask 077

check_pilot_readiness() {
  local target_mode="$1" # "demo" or "pilot"
  local hr_consent="$2" # "APPROVED" or "PENDING"
  local backup_verified="$3" # "YES" or "NO"
  local tls_verified="$4" # "YES" or "NO"

  if [[ "$target_mode" == "demo" ]]; then
    # Synthetic demo mode is always permitted with simulated data
    return 0
  fi

  # Live pilot requirements: HR consent, verified restore, and TLS
  if [[ "$hr_consent" != "APPROVED" ]]; then
    printf 'BLOCKED: tenant HR consent is missing\n' >&2
    return 1
  fi

  if [[ "$backup_verified" != "YES" ]]; then
    printf 'BLOCKED: verified clean-host backup restore is missing\n' >&2
    return 1
  fi

  if [[ "$tls_verified" != "YES" ]]; then
    printf 'BLOCKED: external TLS configuration is unverified\n' >&2
    return 1
  fi

  return 0
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion 1: Attempting to launch real pilot without HR consent must be BLOCKED
  if check_pilot_readiness "pilot" "PENDING" "YES" "YES" 2>/dev/null; then
    printf 'NEGATIVE FAIL: real pilot permitted without HR consent\n' >&2
    exit 1
  fi

  # Negative assertion 2: Attempting to launch real pilot without verified restore must be BLOCKED
  if check_pilot_readiness "pilot" "APPROVED" "NO" "YES" 2>/dev/null; then
    printf 'NEGATIVE FAIL: real pilot permitted without verified restore\n' >&2
    exit 1
  fi

  printf 'NEGATIVE PASS: real pilot strictly blocked when prerequisites are missing; fail-closed policy enforced\n'
  exit 0
fi

# Regression mode
# 1. Synthetic demo mode succeeds without live prerequisites
if ! check_pilot_readiness "demo" "PENDING" "NO" "NO"; then
  printf 'POLICY FAIL: synthetic demo mode blocked\n' >&2
  exit 1
fi
printf 'DEMO POLICY PASS: synthetic demo mode allowed without live consent prerequisites\n'

# 2. Live pilot consent check requires real owned evidence inputs
consent_file="${HRFLOW_PILOT_CONSENT_FILE:-}"
backup_record="${HRFLOW_BACKUP_VERIFIED_RECORD:-}"
tls_record="${HRFLOW_TLS_VERIFIED_RECORD:-}"

if [[ -z "$consent_file" || ! -f "$consent_file" || \
      -z "$backup_record" || ! -f "$backup_record" || \
      -z "$tls_record" || ! -f "$tls_record" ]]; then
  printf 'BLOCKED: Live pilot consent requires authentic owned evidence inputs (HRFLOW_PILOT_CONSENT_FILE, HRFLOW_BACKUP_VERIFIED_RECORD, HRFLOW_TLS_VERIFIED_RECORD); caller-provided APPROVED/YES strings are not consent evidence\n' >&2
  exit 2
fi

hr_consent="PENDING"
if grep -qE '^TENANT_HR_CONSENT=(APPROVED|ACCEPTED)$' "$consent_file"; then
  hr_consent="APPROVED"
fi

backup_verified="NO"
if grep -qE '^BACKUP_RESTORE_VERIFIED=(YES|TRUE)$' "$backup_record"; then
  backup_verified="YES"
fi

tls_verified="NO"
if grep -qE '^TLS_VERIFIED=(YES|TRUE)$' "$tls_record"; then
  tls_verified="YES"
fi

if ! check_pilot_readiness "pilot" "$hr_consent" "$backup_verified" "$tls_verified"; then
  printf 'BLOCKED: Live pilot prerequisites unfulfilled in owned evidence inputs\n' >&2
  exit 2
fi

printf 'REGRESSION PASS: live pilot consent verified against authentic owned evidence files\n'
exit 0
