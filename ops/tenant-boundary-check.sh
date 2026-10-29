#!/usr/bin/env bash
set -euo pipefail

# W11-S01-S03: Two-tenant boundary verification and forged tenant hardening
# Invariant: Forged tenant body/header cannot expose foreign lists/counts/files/exports/jobs.

mode="${1:-regression}"

temp_dir=$(mktemp -d /tmp/hrflow-tenant-hardening-XXXXXX)
trap 'rm -rf "$temp_dir"' EXIT
umask 077

tenant_a="11111111-1111-1111-1111-111111111111"
tenant_b="22222222-2222-2222-2222-222222222222"

# Multi-tenant simulated storage
db="$temp_dir/records.tsv"
cat <<EOF > "$db"
$tenant_a	attendance	att-a-01	2027-01-04T08:00:00Z	PRESENT
$tenant_a	leave	leave-a-01	ANNUAL	APPROVED
$tenant_a	request	req-a-01	EQUIPMENT	PENDING
$tenant_a	export	exp-a-01	ATTENDANCE_SUMMARY	READY
$tenant_b	attendance	att-b-01	2027-01-04T08:15:00Z	PRESENT
$tenant_b	leave	leave-b-01	SICK	PENDING
$tenant_b	request	req-b-01	CERTIFICATE	APPROVED
$tenant_b	export	exp-b-01	LEAVE_SUMMARY	READY
EOF

# Server route resolver: derives tenant strictly from verified token, ignoring client-provided headers/body
query_records() {
  local token_tenant="$1"
  local _forged_header_tenant="$2"
  local _forged_body_tenant="$3"
  local category="$4"

  # Server security rule: caller-provided header or body tenant is NEVER trusted
  local effective_tenant="$token_tenant"

  grep "^$effective_tenant	$category	" "$db" || true
}

access_single_record() {
  local token_tenant="$1"
  local target_record_id="$2"

  local record
  record=$(grep "	$target_record_id	" "$db" || true)
  if [[ -z "$record" ]]; then
    printf '404\n'
    return 0
  fi

  local record_owner_tenant
  record_owner_tenant=$(printf '%s' "$record" | awk '{print $1}')
  if [[ "$record_owner_tenant" != "$token_tenant" ]]; then
    printf '403\n'
    return 0
  fi

  printf '200\n'
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion 1: Tenant A actor attempts to forge Tenant B in HTTP header
  forged_header_results=$(query_records "$tenant_a" "$tenant_b" "" "attendance")
  if echo "$forged_header_results" | grep -q "$tenant_b"; then
    printf 'NEGATIVE FAIL: forged header leaked Tenant B attendance data\n' >&2
    exit 1
  fi

  # Negative assertion 2: Tenant A actor attempts to forge Tenant B in JSON body
  forged_body_results=$(query_records "$tenant_a" "" "$tenant_b" "export")
  if echo "$forged_body_results" | grep -q "$tenant_b"; then
    printf 'NEGATIVE FAIL: forged body leaked Tenant B export data\n' >&2
    exit 1
  fi

  # Negative assertion 3: Tenant A directly requesting Tenant B export file ID exp-b-01 must get 403 Forbidden
  export_status=$(access_single_record "$tenant_a" "exp-b-01")
  if [[ "$export_status" != 403 ]]; then
    printf 'NEGATIVE FAIL: Tenant A expected 403 for Tenant B export file, got %s\n' "$export_status" >&2
    exit 1
  fi

  # Negative assertion 4: Tenant A requesting Tenant B leave request ID req-b-01 must get 403 Forbidden
  request_status=$(access_single_record "$tenant_a" "req-b-01")
  if [[ "$request_status" != 403 ]]; then
    printf 'NEGATIVE FAIL: Tenant A expected 403 for Tenant B request, got %s\n' "$request_status" >&2
    exit 1
  fi

  printf 'POLICY NEGATIVE PASS: tenant boundary simulation: client-supplied headers/body ignored in favor of derived token tenant; cross-tenant access rejected (policy logic only, not live server authorization)\n'
  exit 0
fi

# Regression mode / Policy logic harness
# 1. Query Tenant A records
records_a=$(query_records "$tenant_a" "" "" "attendance")
if ! echo "$records_a" | grep -q "att-a-01"; then
  printf 'POLICY FAIL: Tenant A attendance query returned no data\n' >&2
  exit 1
fi
if echo "$records_a" | grep -q "$tenant_b"; then
  printf 'POLICY FAIL: Tenant A attendance query contained Tenant B data\n' >&2
  exit 1
fi

# 2. Query Tenant B records
records_b=$(query_records "$tenant_b" "" "" "leave")
if ! echo "$records_b" | grep -q "leave-b-01"; then
  printf 'POLICY FAIL: Tenant B leave query returned no data\n' >&2
  exit 1
fi
if echo "$records_b" | grep -q "$tenant_a"; then
  printf 'POLICY FAIL: Tenant B leave query contained Tenant A data\n' >&2
  exit 1
fi

printf 'POLICY PASS: tenant isolation policy simulation verified against simulated records. Real JWT, gateway, and service tenant isolation remain NOT RUN\n'
exit 0
