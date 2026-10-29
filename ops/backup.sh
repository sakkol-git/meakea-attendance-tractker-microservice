#!/usr/bin/env bash
# shellcheck disable=SC2016 # Quoted scripts expand inside the PostgreSQL container.
set -euo pipefail
# shellcheck source=ops/common.sh
source "$(dirname "$0")/common.sh"
ops_load
ops_external_mount
ops_need sha256sum
ops_need pg_restore
[[ "${HRFLOW_BACKUP_CONFIRM:-}" == "$HRFLOW_TARGET_ID" ]] || ops_die 'set HRFLOW_BACKUP_CONFIRM to the target alias'
root="$HRFLOW_BACKUP_MOUNT/hrflow"
mkdir -p "$root"
stamp=$(date -u +%Y%m%dT%H%M%SZ)
stage=$(mktemp -d "$root/.incomplete-${stamp}-XXXXXX")
trap 'rm -rf "$stage"' EXIT
umask 077
printf '%s\n' "$HRFLOW_DB_BACKUP_PASSWORD" | ops_compose exec -T postgres sh -c 'IFS= read -r password; PGPASSWORD="$password" pg_dump -U "$1" -d "$POSTGRES_DB" --format=custom --no-owner --no-acl' sh "$HRFLOW_DB_BACKUP_USER" > "$stage/database.dump"
ops_compose exec -T postgres sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_dumpall -U "$POSTGRES_USER" --globals-only' > "$stage/globals.sql"
pg_restore --list "$stage/database.dump" > "$stage/database.inventory"
cp "$HRFLOW_SIGNING_KEYS_DIR/public.pem" "$stage/signing-public.pem"
printf 'target=%s\ngit=%s\npostgres_major=18\ncreated_utc=%s\n' "$HRFLOW_TARGET_ID" "$(git rev-parse HEAD)" "$stamp" > "$stage/manifest.txt"
ops_compose images --format json > "$stage/image-inventory.json"
ops_compose exec -T kafka kafka-topics --bootstrap-server kafka:29092 --list > "$stage/kafka-topics.txt"
ops_compose exec -T schema-registry curl -fsS http://127.0.0.1:8081/subjects > "$stage/schema-subjects.json"
[[ -d "${HRFLOW_PRIVATE_FILES_DIR:-}" ]] || ops_die 'private files directory missing'
tar -C "$HRFLOW_PRIVATE_FILES_DIR" -cf "$stage/private-files.tar" .
tar -C "$HRFLOW_SIGNING_KEYS_DIR" -cf "$stage/signing-keys.tar" private.pem public.pem
git bundle create "$stage/source.bundle" HEAD
git bundle verify "$stage/source.bundle" >/dev/null
read -r -a release_images <<< "${HRFLOW_RELEASE_IMAGES:?Set reviewed release image references}"
[[ ${#release_images[@]} -ge 2 ]] || ops_die 'release image list incomplete'
for ref in "${release_images[@]}"; do
  [[ "$ref" =~ ^[a-zA-Z0-9][a-zA-Z0-9._:/@-]*$ ]] || ops_die 'invalid release image reference'
  docker image inspect "$ref" >/dev/null || ops_die "release image unavailable: $ref"
done
docker save -o "$stage/release-images.tar" "${release_images[@]}"
(cd "$stage" && sha256sum database.dump globals.sql database.inventory signing-public.pem private-files.tar signing-keys.tar source.bundle release-images.tar image-inventory.json kafka-topics.txt schema-subjects.json manifest.txt > SHA256SUMS)
(cd "$stage" && sha256sum -c SHA256SUMS >/dev/null)
final="$root/$stamp"
mv "$stage" "$final"
trap - EXIT
printf '%s\n' "$final" > "$root/latest-unverified"
printf 'external copy checksums verified; restore still required: %s\n' "$final"
