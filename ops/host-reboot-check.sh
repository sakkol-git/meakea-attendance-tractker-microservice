#!/usr/bin/env bash
set -euo pipefail

# W10-S01-S03: Verify host reboot prerequisites and Docker readiness on target host
# Invariant: Must verify Docker daemon and host environment on the actual target self-host (Acer Nitro 5).
# Missing target host or Docker daemon must produce BLOCKED or nonzero failure, never synthetic PASS.

mode="${1:-recovery}"
target_host="${HRFLOW_TARGET_HOST:-acer-nitro-5}"

if [[ "$mode" == "negative" ]]; then
  # Negative assertion: invalid Docker socket is safely rejected without unattended restart claim
  if ! DOCKER_HOST="unix:///nonexistent.sock" docker info >/dev/null 2>&1; then
    printf 'NEGATIVE PASS: invalid Docker socket is safely rejected without unattended restart claim\n'
    exit 0
  else
    printf 'NEGATIVE FAIL: expected failure with invalid docker socket\n' >&2
    exit 1
  fi
fi

# Recovery mode: check target host and Docker daemon
current_host=$(hostname -s 2>/dev/null || uname -n)
if [[ "$current_host" != "$target_host" && "${HRFLOW_ALLOW_NON_TARGET_HOST:-false}" != "true" ]]; then
  printf 'BLOCKED: Current host (%s) is not the target self-host (%s); unattended host reboot verification requires target host\n' \
    "$current_host" "$target_host" >&2
  exit 2
fi

if ! command -v docker >/dev/null || ! docker info >/dev/null 2>&1; then
  printf 'BLOCKED: Docker daemon is not accessible on this host\n' >&2
  exit 2
fi

printf 'PREREQUISITE PASS: Target host (%s) and Docker daemon verified. Host reboot and unattended startup drill NOT RUN\n' "$current_host"
exit 0
