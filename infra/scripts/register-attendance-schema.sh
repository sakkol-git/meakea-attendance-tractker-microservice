#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "$0")/../.." && pwd -P)
for kind in session correction daily-status shift-cutoff; do
  file="$kind"
  [[ "$kind" == daily-status ]] && file=daily
  prefix=attendance
  [[ "$kind" == shift-cutoff ]] && prefix="shift"
  [[ "$kind" == shift-cutoff ]] && file=cutoff
  topic="attendance.$kind.v1"
  [[ "$kind" == shift-cutoff ]] && topic=time.shift-cutoff.v1
  schema="$root/libs/hrflow-contracts/events/$prefix-$file-fact.v1.avsc"
  [[ -f "$schema" ]] || { printf 'Attendance schema missing\n' >&2; exit 1; }
# Registry enforces its configured compatibility policy. Producer auto-registration stays disabled.
python3 - "$schema" <<'PY' | docker exec -i hrflow-schema-registry \
  curl -fsS -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
    --data-binary @- "http://127.0.0.1:8081/subjects/$topic-value/versions"
import json
import pathlib
import sys

schema = json.loads(pathlib.Path(sys.argv[1]).read_text())
print(json.dumps({"schemaType": "AVRO", "schema": json.dumps(schema)}))
PY
printf '\n'
done
