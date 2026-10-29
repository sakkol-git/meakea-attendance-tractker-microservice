#!/usr/bin/env python3
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
target_script = ROOT / "docs/roadmap/tools/docs-lint.py"
paths = sys.argv[1:] if len(sys.argv) > 1 else [
    "docs", "README.md", "AGENTS.md", "contracts/README.md", "tools/project_mcp/README.md"
]

res = subprocess.run([sys.executable, str(target_script), *paths], cwd=ROOT)
sys.exit(res.returncode)
