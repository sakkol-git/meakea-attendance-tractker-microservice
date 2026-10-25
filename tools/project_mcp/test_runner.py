"""One background test run at a time, with inspectable reports and process cleanup."""
import atexit
from collections import deque
from dataclasses import dataclass, field
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import tempfile
import threading
import time
import uuid

from tools.project_mcp.context import repository_state
from tools.project_mcp.test_reports import diagnostic_excerpt, parse_flutter, parse_junit, summarize
from tools.project_mcp.test_suites import SuiteName, suite_command


def maven_reports(root: Path) -> dict[Path, int]:
    return {path: path.stat().st_mtime_ns for group in ('services', 'libs')
            for path in (root / group).glob('*/target/surefire-reports/TEST-*.xml')}


def stop_process(process: subprocess.Popen | None):
    if process is None or process.poll() is not None:
        return
    try:
        if os.name == 'posix':
            os.killpg(process.pid, signal.SIGTERM)
        else:
            process.terminate()
        try:
            process.wait(timeout=2)
        except subprocess.TimeoutExpired:
            if os.name == 'posix':
                os.killpg(process.pid, signal.SIGKILL)
            else:
                process.kill()
            process.wait(timeout=2)
    except ProcessLookupError:
        pass


@dataclass
class TestRun:
    suite: str
    folder: Path
    command: list[str]
    started: float = field(default_factory=time.monotonic)
    process: subprocess.Popen | None = None
    cancelled: bool = False
    result: dict | None = None
    report: dict | None = None
    provenance: dict = field(default_factory=dict)


class TestRunner:
    def __init__(self, root: Path, timeout_seconds: int = 600):
        self.root = root
        self.timeout = timeout_seconds
        self.runs: dict[str, TestRun] = {}
        self.lock = threading.Lock()
        atexit.register(self.close)

    def start(self, suite: SuiteName, test_file: str = '') -> dict:
        with self.lock:
            if any(run.result is None for run in self.runs.values()):
                raise ValueError('A test run is active; inspect or cancel it before starting another')
            folder = Path(tempfile.mkdtemp(prefix='hrflow-tests-'))
            try:
                selected = suite_command(self.root, suite, folder, test_file)
            except Exception:
                folder.rmdir()
                raise
            run_id = uuid.uuid4().hex[:16]
            run = TestRun(suite, folder, selected.command)
            run.provenance = {name: repository_state(self.root / sub)
                              for name, sub in [('backend', '.'), ('web', 'web'), ('mobile', 'mobile')]}
            self.runs[run_id] = run
            if len(self.runs) > 20:
                del self.runs[next(iter(self.runs))]
            threading.Thread(target=self._execute, args=(run, selected), daemon=True).start()
        return {'run_id': run_id, 'status': 'running', 'suite': suite,
                'test_file': test_file or None, 'poll_after_seconds': 5}

    def _execute(self, run: TestRun, selected):
        log = run.folder / 'console.log'
        timed_out, code = False, None
        try:
            before = maven_reports(self.root) if selected.format == 'maven' else {}
            with log.open('wb') as output:
                process = subprocess.Popen(selected.command, cwd=selected.cwd, stdout=output,
                                           stderr=subprocess.STDOUT, env=os.environ | selected.environment,
                                           start_new_session=os.name == 'posix')
                with self.lock:
                    run.process = process
                    cancel_requested = run.cancelled
                if cancel_requested:
                    stop_process(process)
                try:
                    code = process.wait(timeout=self.timeout)
                except subprocess.TimeoutExpired:
                    timed_out = True
                    stop_process(process)
            if selected.format == 'maven':
                current = maven_reports(self.root)
                paths = []
                for path, modified in current.items():
                    if before.get(path) != modified:
                        copy = run.folder / str(path.relative_to(self.root)).replace('/', '_')
                        shutil.copy2(path, copy)
                        paths.append(copy)
                report = parse_junit(paths)
            elif selected.format == 'flutter':
                report = parse_flutter(selected.report)
            else:
                report = parse_junit([selected.report])
            result = summarize(report, code, timed_out)
        except Exception as error:
            report = {'counts': {'passed': 0, 'failed': 0, 'skipped': 0},
                      'failures': [], 'report_errors': [f'{type(error).__name__}: {str(error)[:500]}']}
            result = summarize(report, code, timed_out)
            result['status'] = 'error'
        result.update(seconds=round(time.monotonic() - run.started, 2), suite=run.suite,
                      command=run.command, artifacts=str(run.folder), full_log=str(log),
                      diagnostics=diagnostic_excerpt(log) if result['status'] != 'passed' else [])
        with self.lock:
            if run.cancelled:
                result['status'] = 'cancelled'
            run.report = report
            run.result = result
            (run.folder / 'summary.json').write_text(json.dumps(result, indent=2))

    def _get(self, run_id: str) -> TestRun:
        if run_id not in self.runs:
            raise ValueError('Unknown run_id; use a run returned by run_tests in this server session')
        return self.runs[run_id]

    def result(self, run_id: str) -> dict:
        with self.lock:
            run = self._get(run_id)
            if run.result is None:
                return {'run_id': run_id, 'status': 'running', 'suite': run.suite,
                        'seconds': round(time.monotonic() - run.started, 2), 'poll_after_seconds': 5}
            return {'run_id': run_id, **run.result}

    def details(self, run_id: str, failure_index: int | None = None,
                log_start: int | None = None, lines: int = 60) -> dict:
        if not 1 <= lines <= 120 or (log_start is not None and log_start < 1):
            raise ValueError('lines must be 1..120 and log_start must be positive')
        with self.lock:
            run = self._get(run_id)
            if failure_index is not None:
                failures = run.report['failures'] if run.report else []
                if not 0 <= failure_index < len(failures):
                    raise ValueError('Failure index is unavailable; inspect get_test_result first')
                return {'run_id': run_id, 'failure': failures[failure_index],
                        'provenance_at_start': run.provenance}
            log = run.folder / 'console.log'
        selected = deque(maxlen=lines)
        line_truncated = False
        if log.is_file():
            with log.open(errors='replace') as stream:
                for number, text in enumerate(stream, 1):
                    if log_start is None or log_start <= number < log_start + lines:
                        numbered = f'{number}: {text.rstrip()}'
                        line_truncated = line_truncated or len(numbered) > 1000
                        selected.append(numbered[:1000])
                    if log_start is not None and number >= log_start + lines:
                        break
        text = '\n'.join(selected)
        return {'run_id': run_id, 'log_excerpt': text[:8000], 'full_log': str(log),
                'excerpt_only': True, 'text_truncated': len(text) > 8000 or line_truncated,
                'provenance_at_start': run.provenance}

    def cancel(self, run_id: str) -> dict:
        with self.lock:
            run = self._get(run_id)
            if run.result is not None:
                return {'run_id': run_id, 'status': run.result['status']}
            run.cancelled = True
            process = run.process
        stop_process(process)
        return {'run_id': run_id, 'status': 'cancellation_requested'}

    def close(self):
        for run in list(self.runs.values()):
            if run.result is None:
                run.cancelled = True
                stop_process(run.process)
