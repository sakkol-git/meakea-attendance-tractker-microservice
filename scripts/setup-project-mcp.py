#!/usr/bin/env python3
"""Install isolated local tools and register this checkout without editing global settings."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import tomllib
import venv

ROOT = Path(__file__).resolve().parents[1]


def register(root: Path, interpreter: Path) -> Path:
    config = root / '.codex/config.toml'
    expected = {'command': str(interpreter), 'args': ['-m', 'tools.project_mcp.server'],
                'cwd': str(root), 'startup_timeout_sec': 15, 'tool_timeout_sec': 45}
    original = config.read_text() if config.exists() else ''
    existing = tomllib.loads(original).get('mcp_servers', {}).get('hrflow_project')
    if existing is not None:
        if existing != expected:
            raise ValueError('Existing hrflow_project settings differ; preserve them and reconcile explicitly')
        return config
    lines = ['[mcp_servers.hrflow_project]']
    for name, value in expected.items():
        lines.append(f'{name} = {json.dumps(value)}')
    config.parent.mkdir(parents=True, exist_ok=True)
    config.write_text(original.rstrip() + '\n\n' + '\n'.join(lines) + '\n')
    return config


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--register-only', action='store_true', help='Use an already installed .venv-mcp')
    args = parser.parse_args()
    environment = ROOT / '.venv-mcp'
    interpreter = environment / ('Scripts/python.exe' if sys.platform == 'win32' else 'bin/python')
    if not args.register_only:
        venv.EnvBuilder(with_pip=True).create(environment)
        subprocess.run([str(interpreter), '-m', 'pip', 'install', '-r',
                        str(ROOT / 'tools/project_mcp/requirements.txt')], check=True)
    if not interpreter.is_file():
        parser.error('Install .venv-mcp first; omit --register-only')
    config = register(ROOT, interpreter)
    print(f'Project registration: {config}')
    print('Reload the trusted project to discover the tools. Global configuration was not edited.')


if __name__ == '__main__':
    main()
