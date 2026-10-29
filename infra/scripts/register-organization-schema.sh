#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "$0")/../.." && pwd -P)
schema="$root/libs/hrflow-contracts/events/organization-fact.v1.avsc"
[[ -f "$schema" ]] || { printf 'Organization schema missing\n' >&2; exit 1; }

# The producer disables automatic registration, so install the reviewed value
# subject before enabling the relay. Registry checks its configured compatibility.
payload=$(python3 - "$schema" <<'PY'
import json
import pathlib
import sys

schema = json.loads(pathlib.Path(sys.argv[1]).read_text())
print(json.dumps({"schemaType": "AVRO", "schema": json.dumps(schema)}))
PY
)

for topic in org.employee.v1 org.shift.v1 org.holiday.v1; do
  subject="${topic}-value"
  printf '%s' "$payload" | docker exec -i hrflow-schema-registry \
    curl -fsS -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
      --data-binary @- "http://127.0.0.1:8081/subjects/${subject}/versions" |
    python3 -c 'import json,sys; result=json.load(sys.stdin); print("registered", sys.argv[1], "id", result["id"], "version", result["version"])' "$subject"
done
