"""Fresh Git provenance plus a small navigation map; never invent implementation status."""
from datetime import datetime, timezone
from pathlib import Path
import subprocess

ENTRY_POINTS = {
    'overview': ['AGENTS.md', 'docs/SOURCE-OF-TRUTH.md', 'docs/Architecture.md',
                 'docs/development/ENGINEERING-RULES.md', 'docs/roadmap/YOU-ARE-HERE.md'],
    'leave': ['contracts/openapi.json', 'web/app/api/workspace.ts',
              'mobile/lib/features/leave/data/repository.dart',
              'services/leave/pom.xml', 'docs/business-flow/LEAVE.md'],
    'attendance': ['docs/business-flow/ATTENDANCE.md',
                   'mobile/lib/features/attendance/data/repository.dart', 'services/attendance/pom.xml'],
    'identity': ['docs/business-flow/IDENTITY.md', 'web/docs/INTEGRATION.md',
                 'mobile/docs/API-INTEGRATION.md', 'services/identity-org/pom.xml'],
    'requests': ['docs/business-flow/REQUEST.md', 'web/app/api/workspace.ts',
                 'mobile/lib/features/requests/data/repository.dart', 'services/request/pom.xml'],
    'infra': ['infra/docker/docker-compose.yaml', 'docs/SERVICE-CONFIGURATION.md',
              'docs/business-flow/INFRASTRUCTURE.md'],
}


def repository_state(directory: Path) -> dict:
    def git(*args):
        return subprocess.check_output(['git', '-C', str(directory), *args],
                                       stderr=subprocess.DEVNULL, timeout=5).decode('utf-8')
    try:
        if Path(git('rev-parse', '--show-toplevel').strip()).resolve() != directory.resolve():
            raise ValueError('Not an independent checkout')
        # NUL-separated porcelain handles whitespace/newlines in filenames.
        changed = git('status', '--porcelain=v1', '-z', '--untracked-files=normal').split('\0')
        rows = []
        index = 0
        while index < len(changed):
            row = changed[index]
            if row:
                rows.append({'status': row[:2], 'path': row[3:][:240]})
                if 'R' in row[:2] or 'C' in row[:2]:
                    index += 1
            index += 1
        return {'revision': git('rev-parse', 'HEAD').strip(), 'dirty': bool(rows),
                'changed_count': len(rows), 'changes': rows[:12], 'truncated': len(rows) > 12}
    except (OSError, ValueError, subprocess.SubprocessError):
        return {'available': False, 'reason': 'Missing or unavailable independent Git checkout'}


def project_context(root: Path, area: str = 'overview') -> dict:
    if area not in ENTRY_POINTS:
        raise ValueError('Unknown area; choose ' + ', '.join(ENTRY_POINTS))
    return {'generated_at': datetime.now(timezone.utc).isoformat(), 'area': area,
            'repositories': {name: repository_state(root / folder)
                             for name, folder in [('backend', '.'), ('web', 'web'), ('mobile', 'mobile')]},
            'entry_points': [{'path': path, 'exists': (root / path).is_file()}
                             for path in ENTRY_POINTS[area]],
            'limitation': 'Navigation and Git provenance only. Read owning requirements and source; '
            'no inferred dependency graph, implementation certification or cached acceptance claim.'}
