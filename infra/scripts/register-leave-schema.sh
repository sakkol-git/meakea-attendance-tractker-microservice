#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "$0")/../.." && pwd -P)
schema="$root/libs/hrflow-contracts/events/leave-request-fact.v1.avsc"
[[ -f "$schema" ]] || { printf 'Leave schema missing\n' >&2; exit 1; }
python3 - "$schema" <<'PY' | docker exec -i hrflow-schema-registry \
  curl -fsS -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
    --data-binary @- 'http://127.0.0.1:8081/subjects/leave.request.v1-value/versions'
import json
import pathlib
import sys

schema = json.loads(pathlib.Path(sys.argv[1]).read_text())
print(json.dumps({"schemaType": "AVRO", "schema": json.dumps(schema)}))
PY
printf '\n'
