#!/usr/bin/env python3
"""Compare mobile's standalone workflow manifest with the owning business flow list."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main() -> int:
    source = (ROOT / "business-flow.md").read_text()
    source_ids = re.findall(r"^\| (BF-\d{3}) \|", source, re.MULTILINE)
    manifest_path = ROOT / "mobile/docs/validation/workflow-routes.json"
    manifest = json.loads(manifest_path.read_text())
    mobile_ids = [entry["id"] for entry in manifest]
    if len(set(source_ids)) != 40:
        print("Owning business-flow.md must identify 40 distinct workflows")
        return 1
    if len(mobile_ids) != 40 or len(set(mobile_ids)) != 40 or set(mobile_ids) != set(source_ids):
        print("Mobile workflow IDs differ from the owning business-flow.md list")
        return 1
    print("Mobile workflow IDs match all 40 source business flows")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
