"""Named local checks with bounded output. No arbitrary commands or deployment actions."""
from pathlib import Path
import subprocess
import sys
import tempfile
import time
from typing import Literal

CheckName = Literal['contract_schema', 'contract_drift', 'foundation_tests', 'foundation_docs']


def check_command(name: CheckName) -> list[str]:
    checks = {
        'contract_schema': ['scripts/contracts.py', 'validate'],
        'contract_drift': ['scripts/contracts.py', 'check'],
        'foundation_tests': ['-m', 'unittest', 'discover', '-s', 'scripts/tests', '-v'],
        'foundation_docs': ['docs/roadmap/tools/docs-lint.py', 'docs/development',
                            'docs/SOURCE-OF-TRUTH.md', 'docs/DOCUMENTATION-CATALOG.md',
                            'docs/roadmap/ENGINEERING-STANDARDS.md', 'README.md',
                            'AGENTS.md', 'contracts/README.md', 'tools/project_mcp/README.md'],
    }
    if name not in checks:
        raise ValueError('Unknown check; choose ' + ', '.join(checks))
    return [sys.executable, *checks[name]]


def run_check(root: Path, name: CheckName) -> dict:
    command = check_command(name)
    started = time.monotonic()
    with tempfile.NamedTemporaryFile(prefix='hrflow-check-', suffix='.log', delete=False) as log:
        try:
            result = subprocess.run(command, cwd=root, stdout=log, stderr=subprocess.STDOUT,
                                    timeout=30, check=False)
            code, timed_out = result.returncode, False
        except subprocess.TimeoutExpired:
            code, timed_out = None, True
        log.flush()
        size = log.tell()
        limit = 4000 if code != 0 else 1000
        with open(log.name, 'rb') as source:
            source.seek(max(0, size - limit))
            tail = source.read(limit).decode('utf-8', errors='replace')
        return {'check': name, 'command': command[1:], 'exit_code': code,
                'passed': code == 0, 'timed_out': timed_out,
                'seconds': round(time.monotonic() - started, 2), 'output_tail': tail,
                'truncated': size > limit, 'full_log': log.name,
                'limitation': 'Only this named check ran; no full-system or provider acceptance claim.'}
