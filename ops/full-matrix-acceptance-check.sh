#!/usr/bin/env bash
set -euo pipefail

# W12 Backend Services Acceptance Runner
# Invariant: Every required service suite is actually executed from clean sources/fixtures; no synthetic echo passes.
# Note: Executing Java service unit/integration test suites does not constitute full end-to-end matrix acceptance.
# Client, contract sync, broker transport, and e2e scopes are explicitly reported as omitted.

mode="${1:-regression}"
target_service="${2:-all}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$ROOT_DIR"

run_suite() {
  local service="$1"
  printf 'Executing test suite for service: %s...\n' "$service"
  TESTCONTAINERS_TRANSPORT_TYPE=httpclient5 ./mvnw --batch-mode clean test -pl "services/$service"
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion:
  # Any failing or nonexistent service suite must strictly return nonzero and abort.
  printf 'Verifying negative failure path on non-existent or failing suite...\n'
  if ./mvnw --batch-mode clean test -pl "services/reporting" -Dtest="NonExistentFailingTestTarget" >/dev/null 2>&1; then
    printf 'NEGATIVE FAIL: acceptance passed despite failing/missing target\n' >&2
    exit 1
  fi
  printf 'NEGATIVE PASS: failing suite strictly produces nonzero exit; synthetic pass rejected\n'
  exit 0
fi

# Regression mode
services=("identity-org" "attendance" "leave" "request" "notification" "reporting")

if [[ "$target_service" != "all" ]]; then
  services=("$target_service")
fi

for s in "${services[@]}"; do
  if [[ ! -d "services/$s" ]]; then
    printf 'BLOCKED: Service directory services/%s does not exist\n' "$s" >&2
    exit 1
  fi
  if ! run_suite "$s"; then
    printf 'REGRESSION FAIL: Service suite services/%s failed\n' "$s" >&2
    exit 1
  fi
  printf 'SERVICE PASS: %s verified with executed tests\n' "$s"
done

printf 'BACKEND MODULE TESTS PASS across: %s\n' "${services[*]}"
printf 'OMITTED SCOPES (NOT RUN): web (Nuxt), mobile (Flutter), contract sync, live Kafka broker transport, end-to-end matrix\n'
exit 0
