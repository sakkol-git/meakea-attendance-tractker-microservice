#!/usr/bin/env python3
"""Run explicit verification scopes; full mode requires both client checkouts."""
import argparse
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scope', choices=['all', 'foundation', 'backend', 'web', 'mobile', 'tooling'], default='all')
    parser.add_argument('--base', help='Git base revision for the conservative contract compatibility gate')
    args = parser.parse_args()
    scopes = ['foundation', 'backend', 'web', 'mobile', 'tooling'] if args.scope == 'all' else [args.scope]
    tasks = []
    checks_python = ROOT / '.venv-checks/bin/python'
    py = str(checks_python) if checks_python.is_file() else sys.executable
    if 'foundation' in scopes:
        tasks += [(ROOT, [py, 'scripts/contracts.py', 'validate']),
                  (ROOT, [py, '-m', 'unittest', 'discover', '-s', 'scripts/tests', '-v']),
                  (ROOT, [py, 'docs/roadmap/tools/docs-lint.py', 'docs', 'README.md', 'AGENTS.md', 'contracts/README.md', 'tools/project_mcp/README.md']),
                  (ROOT, ['git', 'diff', '--check'])]
        if args.scope == 'all':
            tasks += [(ROOT, [py, 'scripts/contracts.py', 'check']),
                      (ROOT, [py, 'scripts/sync-client-demo-data.py', '--check'])]
    if args.base:
        tasks.append((ROOT, [py, 'scripts/contracts.py', 'compat', '--base', args.base]))
    if 'backend' in scopes:
        tasks += [(ROOT, ['./mvnw', '--batch-mode', 'verify']),
                  (ROOT, ['docker', 'compose', '-f', 'infra/docker/docker-compose.yaml', 'config', '--quiet'])]
    if 'web' in scopes:
        tasks += [(ROOT / 'web', [py, 'contracts/check-bundle.py']),
                  (ROOT / 'web', ['npm', 'run', 'check']),
                  (ROOT / 'web', ['npm', 'run', 'test:e2e']),
                  (ROOT / 'web', ['git', 'diff', '--check'])]
    if 'mobile' in scopes:
        local = ROOT / 'mobile/.fvm/flutter_sdk/bin/flutter'
        flutter = str(local) if local.is_file() else shutil.which('flutter') or 'flutter'
        tasks += [(ROOT / 'mobile', [py, 'contracts/check-bundle.py']),
                  (ROOT / 'mobile', [flutter, 'analyze']),
                  (ROOT / 'mobile', [flutter, 'test']),
                  (ROOT / 'mobile', ['git', 'diff', '--check'])]
    if 'tooling' in scopes:
        local_tools = ROOT / '.venv-mcp/bin/python'
        tool_python = str(local_tools) if local_tools.is_file() else py
        tasks += [(ROOT, [tool_python, '-m', 'unittest', 'discover',
                          '-s', 'tools/project_mcp/tests', '-v'])]
    failures = []
    for directory, command in tasks:
        print(f'\n[{directory.name}] {" ".join(command)}', flush=True)
        try:
            subprocess.run(command, cwd=directory, check=True)
        except (OSError, subprocess.CalledProcessError) as error:
            failures.append(f'{directory.name}: {" ".join(command)}: {error}')
    print('\nExecuted scopes:', ', '.join(scopes))
    if args.scope != 'all':
        print('Other scopes were not run; this is not a complete integration check.')
    if not args.base:
        print('Compatibility against a Git baseline was not run; pass --base for a change review.')
    if failures:
        print('\nFailed checks:\n' + '\n'.join(failures), file=sys.stderr)
        return 1
    print('Selected checks passed. Live backend business acceptance remains a separate release gate.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
