"""Allowlisted test commands and safe optional file selection; no shell strings."""
from dataclasses import dataclass
from pathlib import Path
import re
import shutil
from typing import Literal

SuiteName = Literal['backend', 'web_unit', 'web_e2e', 'mobile']


@dataclass
class SuiteCommand:
    command: list[str]
    cwd: Path
    report: Path
    format: str
    environment: dict[str, str]


def suite_command(root: Path, suite: SuiteName, folder: Path, test_file: str = '') -> SuiteCommand:
    if suite not in ('backend', 'web_unit', 'web_e2e', 'mobile'):
        raise ValueError('Choose backend, web_unit, web_e2e or mobile')
    relative = Path(test_file)
    if test_file:
        if relative.is_absolute() or '..' in relative.parts or any(part.startswith('-') for part in relative.parts):
            raise ValueError('test_file must be an existing repository-relative test file')
        expected = root / ('mobile' if suite == 'mobile' else 'web' if suite.startswith('web') else '.')
        resolved = (expected / relative).resolve()
        if not resolved.is_relative_to(expected.resolve()) or not resolved.is_file():
            raise ValueError('Test file missing or outside its repository')
    report = folder / ('events.jsonl' if suite == 'mobile' else 'report.xml')
    if suite == 'backend':
        command = [str(root / 'mvnw'), '--batch-mode', '--no-transfer-progress', 'test']
        if test_file:
            if not re.fullmatch(r'services/[\w-]+/src/test/java/(?:\w+/)*\w+\.java', test_file):
                raise ValueError('Backend selection requires services/<service>/src/test/java/...Test.java')
            module = '/'.join(relative.parts[:2])
            class_name = '.'.join(relative.parts[5:])[:-5]
            command += ['-pl', module, '-am', '-Dtest=' + class_name,
                        '-Dsurefire.failIfNoSpecifiedTests=false']
        return SuiteCommand(command, root, folder, 'maven', {})
    if suite == 'mobile':
        if test_file and (relative.parts[0] != 'test' or not test_file.endswith('_test.dart')):
            raise ValueError('Mobile selection requires test/..._test.dart')
        flutter = root / 'mobile/.fvm/flutter_sdk/bin/flutter'
        binary = str(flutter) if flutter.is_file() else shutil.which('flutter') or 'flutter'
        command = [binary, '--suppress-analytics', 'test', '--no-pub', '--reporter=expanded',
                   '--file-reporter=json:' + str(report)]
    else:
        directory = 'unit' if suite == 'web_unit' else 'e2e'
        if test_file and (relative.parts[:2] != ('tests', directory) or not test_file.endswith('.ts')):
            raise ValueError(f'Web selection requires tests/{directory}/...ts')
        executable = 'vitest' if suite == 'web_unit' else 'playwright'
        command = [str(root / 'web/node_modules/.bin' / executable)]
        if suite == 'web_unit':
            command += ['run', '--reporter=junit', '--outputFile=' + str(report)]
        else:
            command += ['test', '--reporter=junit', '--fail-on-flaky-tests',
                        '--output=' + str(folder / 'browser-artifacts')]
    if test_file:
        command.append(re.escape(test_file) + '$' if suite == 'web_e2e' else test_file)
    env = {'PLAYWRIGHT_JUNIT_OUTPUT_FILE': str(report)} if suite == 'web_e2e' else {}
    return SuiteCommand(command, root / ('mobile' if suite == 'mobile' else 'web'),
                        report, 'flutter' if suite == 'mobile' else 'junit', env)
