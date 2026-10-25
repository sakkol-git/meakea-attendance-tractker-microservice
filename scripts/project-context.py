#!/usr/bin/env python3
"""Print fresh entry points and Git provenance without indexing private payloads."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def git(path, *args):
    return subprocess.check_output(['git', '-C', str(path), *args], text=True).strip()


def main():
    print('HRFlow context — inspect source; this is navigation, not implementation certification.')
    for label, directory in [('backend', ROOT), ('web', ROOT / 'web'), ('mobile', ROOT / 'mobile')]:
        print(f'\n{label}: {directory}')
        try:
            if Path(git(directory, 'rev-parse', '--show-toplevel')).resolve() != directory.resolve():
                raise ValueError('not an independent checkout')
            print('revision:', git(directory, 'rev-parse', 'HEAD'))
            print(git(directory, 'status', '--short') or 'working tree clean')
        except (OSError, ValueError, subprocess.CalledProcessError):
            print('UNAVAILABLE: checkout missing or not an independent repository')
    print('\nRead:')
    for entry in ['docs/SOURCE-OF-TRUTH.md', 'docs/Architecture.md',
                  'docs/roadmap/YOU-ARE-HERE.md', 'docs/development/PROJECT-CONTEXT.md',
                  'docs/development/CHANGE-CHECKLIST.md', 'contracts/openapi.json',
                  'web/docs/INTEGRATION.md', 'mobile/docs/API-INTEGRATION.md']:
        print(f'- {entry}' + ('' if (ROOT / entry).is_file() else ' [missing]'))
    print('\nRun scripts/verify-project.py for checks. Consumer mocks do not verify a live provider.')


if __name__ == '__main__':
    main()
