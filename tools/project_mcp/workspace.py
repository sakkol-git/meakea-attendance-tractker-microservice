"""Bounded, source-backed reads of one HRFlow workspace; no persistent index."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

SOURCE_SUFFIXES = {'.py', '.java', '.dart', '.ts', '.vue', '.md', '.json',
                   '.yaml', '.yml', '.xml', '.sql', '.avsc', '.toml', '.sh', '.css'}
EXCLUDED = {'.git', '.codex', '.venv', '.venv-checks', '.venv-mcp', '.fvm',
            'node_modules', 'target', 'build', '.dart_tool', '.nuxt', '.output',
            '__pycache__', '.pytest_cache', '.cache', 'test-results',
            'playwright-report', 'coverage', 'datasets', 'runs', '.idea', '.vscode'}
MAX_BYTES = 1_000_000
MAX_FILES = 2000
MAX_TEXT = 8000


class Workspace:
    def __init__(self, root: Path):
        self.root = root.resolve()

    def resolve_source(self, relative: str) -> Path:
        candidate = Path(relative)
        if candidate.is_absolute() or '..' in candidate.parts:
            raise ValueError('Use a workspace-relative source path without traversal')
        if any(part in EXCLUDED or part.startswith('.env') for part in candidate.parts):
            raise ValueError('Private or generated paths are excluded')
        path = (self.root / candidate).resolve()
        if not path.is_relative_to(self.root):
            raise ValueError('Path leaves the workspace')
        if any(part in EXCLUDED or part.startswith('.env')
               for part in path.relative_to(self.root).parts):
            raise ValueError('Resolved path is private or generated')
        if path.is_file() and (path.suffix not in SOURCE_SUFFIXES or 'lock' in path.name):
            raise ValueError('Only supported source and documentation files are available')
        return path

    def read(self, relative: str, start: int = 1, count: int = 80) -> dict:
        if not 1 <= count <= 160 or start < 1:
            raise ValueError('start must be positive; count must be 1..160')
        path = self.resolve_source(relative)
        if not path.is_file() or path.stat().st_size > MAX_BYTES:
            raise ValueError('Source file missing or exceeds 1 MB')
        data = path.read_bytes()
        lines = data.decode('utf-8').splitlines()
        if start > max(1, len(lines)):
            raise ValueError('start is beyond the file')
        excerpt = '\n'.join(f'{i + start}: {line}'
                            for i, line in enumerate(lines[start - 1:start - 1 + count]))
        return {'path': path.relative_to(self.root).as_posix(),
                'sha256': hashlib.sha256(data).hexdigest(), 'total_lines': len(lines),
                'start': start, 'text': excerpt[:MAX_TEXT],
                'truncated': start > 1 or start - 1 + count < len(lines) or len(excerpt) > MAX_TEXT}

    def search(self, query: str, directory: str = '.', limit: int = 12) -> dict:
        if not query or len(query) > 200 or '\n' in query or not 1 <= limit <= 30:
            raise ValueError('Use a single literal query up to 200 characters and limit 1..30')
        path = self.resolve_source(directory)
        if not path.is_dir():
            raise ValueError('Search scope must be a source directory')
        command = ['rg', '--json', '--hidden', '--no-ignore', '--fixed-strings',
                   '--max-count', '10', '--max-filesize', '1M']
        for suffix in sorted(SOURCE_SUFFIXES):
            command += ['--glob', '*' + suffix]
        for excluded in sorted(EXCLUDED):
            command += ['--glob', f'!**/{excluded}/**']
        command += ['--glob', '!**/.env*', '--glob', '!*lock*', '-e', query, '--', str(path)]
        matches = []
        # A temporary file keeps potentially verbose rg output out of process memory.
        with tempfile.TemporaryFile() as output:
            result = subprocess.run(command, stdout=output, stderr=subprocess.PIPE,
                                    timeout=15, text=True)
            if result.returncode not in (0, 1):
                raise ValueError('Source search failed; confirm rg is installed and scope is readable')
            output.seek(0)
            for line in output:
                event = json.loads(line)
                if event['type'] != 'match':
                    continue
                data = event['data']
                match_path = Path(data['path']['text']).relative_to(self.root).as_posix()
                matches.append({'path': match_path, 'line': data['line_number'],
                                'text': data['lines']['text'].rstrip()[:240]})
                if len(matches) > limit:
                    break
        return {'matches': matches[:limit], 'truncated': len(matches) > limit,
                'scope': directory, 'limitation': 'Source types only; excluded generated/private files, '
                'files over 1 MB and more than ten matches per file are not searched exhaustively.'}

    def structure(self, directory: str = '.', review_lines: int = 300) -> dict:
        path = self.resolve_source(directory)
        if not path.is_dir():
            raise ValueError('Structure scope must be a source directory')
        review = []
        inspected = 0
        for current, directories, files in os.walk(path, followlinks=False):
            directories[:] = sorted(d for d in directories if d not in EXCLUDED
                                    and not (Path(current) / d).is_symlink())
            for name in sorted(files):
                source = Path(current) / name
                if source.is_symlink() or source.suffix not in SOURCE_SUFFIXES or 'lock' in name:
                    continue
                if source.stat().st_size > MAX_BYTES:
                    continue
                inspected += 1
                if inspected > MAX_FILES:
                    break
                try:
                    count = len(source.read_text().splitlines())
                except (UnicodeError, OSError):
                    continue
                if count > review_lines:
                    review.append({'path': source.relative_to(self.root).as_posix(), 'lines': count})
            if inspected > MAX_FILES:
                break
        review.sort(key=lambda row: (-row['lines'], row['path']))
        return {'files_inspected': min(inspected, MAX_FILES), 'review_threshold': review_lines,
                'review_files': review[:20], 'review_count': len(review),
                'truncated': inspected > MAX_FILES or len(review) > 20,
                'limitation': 'File size prompts responsibility review; it does not prove SOLID, '
                'clean architecture or business correctness. Generated files may need an exception.'}
