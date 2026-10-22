#!/usr/bin/env python3
"""Compatibility entry point: the roadmap linter replaces the old fixed-file validator."""
from pathlib import Path
import subprocess
import sys
root = Path(__file__).resolve().parents[2]
cmd = [sys.executable, str(root / "docs/roadmap/tools/docs-lint.py"), str(root / "docs")]
raise SystemExit(subprocess.call(cmd))
