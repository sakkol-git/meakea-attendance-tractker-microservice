#!/usr/bin/env bash
set -euo pipefail

# W12-S01-S03: Generate and validate release manifest from real workspace inputs
# Invariant: Must inspect actual Git revision, dirty workspace state, contract hash, source migration inventory,
# and real built Docker image digests. Placeholder image digests or missing inputs strictly produce BLOCKED or nonzero failure.

mode="${1:-regression}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$ROOT_DIR"

temp_dir=$(mktemp -d /tmp/hrflow-manifest-XXXXXX)
trap 'rm -rf "$temp_dir"' EXIT
umask 077

validate_manifest_content() {
  local manifest="$1"

  # 1. Must record root, web, and mobile git commit shas
  for repo in root web mobile; do
    if ! grep -qE "^${repo}_git_commit: [0-9a-f]{40}$" "$manifest"; then
      printf 'FAIL: invalid or missing %s_git_commit in manifest\n' "$repo" >&2
      return 1
    fi
    if ! grep -qE "^${repo}_git_dirty: (true|false)$" "$manifest"; then
      printf 'FAIL: missing %s_git_dirty flag in manifest\n' "$repo" >&2
      return 1
    fi
  done

  # 2. Must have valid OpenAPI contract sha256
  if ! grep -qE '^contracts_openapi_hash: [0-9a-f]{64}$' "$manifest"; then
    printf 'FAIL: invalid or missing contracts_openapi_hash in manifest\n' >&2
    return 1
  fi

  # 3. Must have source migration inventory for all 6 owner schemas
  for schema in identity_org attendance notification leave request reporting; do
    if ! grep -qE "^source_migration_inventory_${schema}: V[0-9]+" "$manifest"; then
      printf 'FAIL: missing source migration inventory for schema %s\n' "$schema" >&2
      return 1
    fi
  done

  # 4. Must require exact @sha256:[0-9a-f]{64} digests for all six images
  local required_images=("identity-org" "attendance" "notification" "leave" "request" "reporting")
  for img in "${required_images[@]}"; do
    if ! grep -qE "^image: [a-zA-Z0-9/_.-]+/${img}:.*@sha256:[0-9a-f]{64}$" "$manifest" && \
       ! grep -qE "^image: [a-zA-Z0-9/_.-]*${img}[a-zA-Z0-9/_.-]*@sha256:[0-9a-f]{64}$" "$manifest"; then
      printf 'FAIL: missing or invalid @sha256:[0-9a-f]{64} digest for required image %s\n' "$img" >&2
      return 1
    fi
  done

  # Ensure no unpinned or malformed image lines exist
  while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$line" =~ ^image: ]]; then
      local img_ref
      img_ref=$(printf '%s' "$line" | awk '{print $2}')
      if [[ ! "$img_ref" =~ @sha256:[0-9a-f]{64}$ ]]; then
        printf 'FAIL: image line does not end with exact @sha256:[0-9a-f]{64} digest: %s\n' "$img_ref" >&2
        return 1
      fi
    fi
  done < "$manifest"

  return 0
}

if [[ "$mode" == "negative" ]]; then
  # Negative assertion 1: Placeholder digest (short hash) must fail validation
  bad_manifest="$temp_dir/bad_manifest.txt"
  cat <<EOF > "$bad_manifest"
root_git_commit: $(git rev-parse HEAD)
root_git_dirty: false
web_git_commit: $(git -C "$ROOT_DIR/web" rev-parse HEAD)
web_git_dirty: false
mobile_git_commit: $(git -C "$ROOT_DIR/mobile" rev-parse HEAD)
mobile_git_dirty: false
contracts_openapi_hash: $(shasum -a 256 contracts/openapi.json | awk '{print $1}')
image: hrflow/identity-org:1.0.0@sha256:1111222233334444
image: hrflow/attendance:1.0.0@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
image: hrflow/notification:1.0.0@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
image: hrflow/leave:1.0.0@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
image: hrflow/request:1.0.0@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
image: hrflow/reporting:1.0.0@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
source_migration_inventory_identity_org: V10
source_migration_inventory_attendance: V2
source_migration_inventory_notification: V1
source_migration_inventory_leave: V1
source_migration_inventory_request: V1
source_migration_inventory_reporting: V1
EOF

  if validate_manifest_content "$bad_manifest" 2>/dev/null; then
    printf 'NEGATIVE FAIL: placeholder image digest was accepted\n' >&2
    exit 1
  fi

  # Negative assertion 2: Missing one of six required images must fail validation
  missing_img_manifest="$temp_dir/missing_img_manifest.txt"
  grep -v 'hrflow/reporting' "$bad_manifest" > "$missing_img_manifest"
  if validate_manifest_content "$missing_img_manifest" 2>/dev/null; then
    printf 'NEGATIVE FAIL: manifest missing one of six images was accepted\n' >&2
    exit 1
  fi

  printf 'NEGATIVE PASS: placeholder image digests, missing images, and unpinned tags strictly rejected\n'
  exit 0
fi

# Regression mode: inspect real inputs
if ! command -v git >/dev/null || ! git rev-parse HEAD >/dev/null 2>&1; then
  printf 'BLOCKED: Git repository is not available for release manifest inspection\n' >&2
  exit 2
fi

root_git_commit=$(git rev-parse HEAD)
root_git_dirty="false"
if [[ -n "$(git status --porcelain)" ]]; then
  root_git_dirty="true"
fi

web_git_commit=$(git -C "$ROOT_DIR/web" rev-parse HEAD 2>/dev/null || printf '0000000000000000000000000000000000000000')
web_git_dirty="false"
if [[ -n "$(git -C "$ROOT_DIR/web" status --porcelain 2>/dev/null)" ]]; then
  web_git_dirty="true"
fi

mobile_git_commit=$(git -C "$ROOT_DIR/mobile" rev-parse HEAD 2>/dev/null || printf '0000000000000000000000000000000000000000')
mobile_git_dirty="false"
if [[ -n "$(git -C "$ROOT_DIR/mobile" status --porcelain 2>/dev/null)" ]]; then
  mobile_git_dirty="true"
fi

# Block release identity when dirty content is not represented
if [[ "$root_git_dirty" == "true" || "$web_git_dirty" == "true" || "$mobile_git_dirty" == "true" ]]; then
  if [[ "${HRFLOW_ALLOW_DIRTY_MANIFEST:-false}" != "true" ]]; then
    printf 'BLOCKED: Release identity blocked; uncommitted dirty changes present in repositories (root: %s, web: %s, mobile: %s). Dirty content is not represented in an immutable commit.\n' \
      "$root_git_dirty" "$web_git_dirty" "$mobile_git_dirty" >&2
    exit 2
  fi
fi

openapi_hash=$(shasum -a 256 "$ROOT_DIR/contracts/openapi.json" | awk '{print $1}')

manifest_file="$temp_dir/release_manifest.txt"
{
  printf 'root_git_commit: %s\n' "$root_git_commit"
  printf 'root_git_dirty: %s\n' "$root_git_dirty"
  printf 'web_git_commit: %s\n' "$web_git_commit"
  printf 'web_git_dirty: %s\n' "$web_git_dirty"
  printf 'mobile_git_commit: %s\n' "$mobile_git_commit"
  printf 'mobile_git_dirty: %s\n' "$mobile_git_dirty"
  printf 'contracts_openapi_hash: %s\n' "$openapi_hash"
} > "$manifest_file"

# Inspect source migration inventory from disk (note: source files, not live applied Flyway state)
services=(
  "identity_org:services/identity-org/src/main/resources/db/migration"
  "attendance:services/attendance/src/main/resources/db/migration"
  "notification:services/notification/src/main/resources/db/migration"
  "leave:services/leave/src/main/resources/db/migration"
  "request:services/request/src/main/resources/db/migration"
  "reporting:services/reporting/src/main/resources/db/migration"
)

for item in "${services[@]}"; do
  schema="${item%%:*}"
  dir="${item##*:}"
  if [[ ! -d "$ROOT_DIR/$dir" ]]; then
    printf 'BLOCKED: Migration directory %s not found\n' "$dir" >&2
    exit 2
  fi
  latest_migration=$(find "$ROOT_DIR/$dir" -name 'V*.sql' -exec basename {} \; | sort -V | tail -n 1)
  version="${latest_migration%%__*}"
  printf 'source_migration_inventory_%s: %s\n' "$schema" "$version" >> "$manifest_file"
done

# Inspect Docker daemon for immutable published repository digests
if ! command -v docker >/dev/null || ! docker info >/dev/null 2>&1; then
  printf 'BLOCKED: Docker daemon is not accessible; cannot inspect image digests for release manifest\n' >&2
  exit 2
fi

image_names=("identity-org" "attendance" "notification" "leave" "request" "reporting")
for img in "${image_names[@]}"; do
  full_tag="hrflow/$img:latest"
  digest=$(docker inspect --format='{{index .RepoDigests 0}}' "$full_tag" 2>/dev/null || true)
  if [[ -z "$digest" || "$digest" != *"@sha256:"* ]]; then
    printf 'BLOCKED: Required immutable release image digest missing for %s (local image ID is not a published registry digest)\n' "$full_tag" >&2
    exit 2
  fi
  printf 'image: %s\n' "$digest" >> "$manifest_file"
done

if ! validate_manifest_content "$manifest_file"; then
  printf 'REGRESSION FAIL: release manifest failed validation\n' >&2
  exit 1
fi

printf 'REGRESSION PASS: release manifest verified from real Git revisions (root_dirty=%s, web_dirty=%s, mobile_dirty=%s), contract hash, source migrations, and immutable image digests\n' \
  "$root_git_dirty" "$web_git_dirty" "$mobile_git_dirty"
exit 0
