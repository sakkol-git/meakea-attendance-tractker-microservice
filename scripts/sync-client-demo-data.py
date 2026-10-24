"""Copy the canonical synthetic seed into client bundles; --check detects drift."""
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
source = (root / 'client-design/data/demo-seed.json').read_bytes()
targets = ['web/app/data/demo-seed.json', 'mobile/assets/data/demo-seed.json']
for name in targets:
    target = root / name
    if '--check' in sys.argv:
        if not target.exists() or target.read_bytes() != source:
            raise SystemExit(f'Demo seed needs syncing: {name}')
    else:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(source)
