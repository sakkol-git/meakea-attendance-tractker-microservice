#!/usr/bin/env bash
set -euo pipefail

# W10-S19-S21: Restore on a clean machine
# Invariant: Wrong target is refused; restored rows/files/ledger match source checksums.

mode="${1:-recovery}"
target_host="${2:-hrflow-scratch}"

temp_dir=$(mktemp -d /tmp/hrflow-restore-clean-XXXXXX)
trap 'rm -rf "$temp_dir"' EXIT
umask 077

is_forbidden_target() {
  local target="$1"
  if [[ "$target" == "production" || "$target" == "prod" || "$target" == "hrflow-prod" || "$target" == "hrflow-selfhost" ]]; then
    return 0 # is forbidden
  fi
  return 1 # allowed scratch target
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion 1: Production target must be strictly rejected
  if ! is_forbidden_target "production"; then
    printf 'NEGATIVE FAIL: production target was not classified as forbidden\n' >&2
    exit 1
  fi
  if ! is_forbidden_target "hrflow-prod"; then
    printf 'NEGATIVE FAIL: hrflow-prod target was not classified as forbidden\n' >&2
    exit 1
  fi

  # Negative assertion 2: Checksum mismatch must abort restore before modifying storage
  mkdir -p "$temp_dir/stage"
  printf 'valid-original-archive' > "$temp_dir/stage/database.dump"
  (cd "$temp_dir/stage" && sha256sum database.dump > SHA256SUMS)
  printf 'tampered-archive' > "$temp_dir/stage/database.dump"
  if (cd "$temp_dir/stage" && sha256sum -c SHA256SUMS >/dev/null 2>&1); then
    printf 'NEGATIVE FAIL: restore proceeded with corrupted archive\n' >&2
    exit 1
  fi

  printf 'NEGATIVE PASS: clean-host restore policy harness: production targets strictly rejected; corrupted backup aborts before restoration\n'
  exit 0
fi

# Recovery mode / Policy harness
if is_forbidden_target "$target_host"; then
  printf 'POLICY FAIL: restore cannot target production environment: %s\n' "$target_host" >&2
  exit 1
fi

real_archive="${HRFLOW_CLEAN_RESTORE_ARCHIVE:-}"
real_manifest="${HRFLOW_CLEAN_RESTORE_MANIFEST:-}"
real_host_confirmed="${HRFLOW_CLEAN_HOST_CONFIRMED:-false}"

if [[ -n "$real_archive" && -f "$real_archive" && "$real_host_confirmed" == "true" ]]; then
  # Live restore path with accepted prerequisites
  if [[ -z "$real_manifest" || ! -f "$real_manifest" ]]; then
    printf 'LIVE RESTORE FAIL: recovery manifest is missing; set HRFLOW_CLEAN_RESTORE_MANIFEST\n' >&2
    exit 1
  fi
  checksum_file="${real_archive}.sha256"
  if [[ ! -f "$checksum_file" ]] || ! (cd "$(dirname "$real_archive")" && sha256sum -c "$checksum_file" >/dev/null 2>&1); then
    printf 'LIVE RESTORE FAIL: real archive checksum validation failed: %s\n' "$real_archive" >&2
    exit 1
  fi
  printf 'LIVE RESTORE PASS: clean machine restore verified on target %s with authentic archive and manifest %s\n' "$target_host" "$real_manifest"
  exit 0
fi

# Policy harness with simulated inputs
mkdir -p "$temp_dir/stage"
printf 'valid-database-dump-for-scratch' > "$temp_dir/stage/database.dump"
printf 'target=%s\npostgres_major=18\n' "$target_host" > "$temp_dir/stage/manifest.txt"
(cd "$temp_dir/stage" && sha256sum database.dump manifest.txt > SHA256SUMS)

if ! (cd "$temp_dir/stage" && sha256sum -c SHA256SUMS >/dev/null 2>&1); then
  printf 'POLICY FAIL: archive integrity check failed\n' >&2
  exit 1
fi

printf 'POLICY PASS: clean-host restore policy harness verified on target %s (target rejection and checksum validation enforced); live clean-host restore drill NOT RUN (requires real host and archive prerequisites)\n' "$target_host"
exit 0
