"""Background execution keeps process state, report evidence and cancellation honest."""
from pathlib import Path
import shutil
import sys
import tempfile
import time
import unittest
from unittest.mock import patch

from tools.project_mcp.test_runner import TestRunner, maven_reports
from tools.project_mcp.test_suites import SuiteCommand, suite_command


class TestRunnerBehavior(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.runner = TestRunner(self.root)
        self.addCleanup(self.runner.close)

    def launch(self, code, timeout=5):
        self.runner.timeout = timeout
        def command(root, suite, folder, test_file):
            return SuiteCommand([sys.executable, '-c', code.replace('REPORT_PATH', repr(str(folder / 'report.xml')))],
                                root, folder / 'report.xml', 'junit', {})
        with patch('tools.project_mcp.test_runner.suite_command', side_effect=command):
            run_id = self.runner.start('web_unit')['run_id']
        self.addCleanup(lambda: shutil.rmtree(self.runner.runs[run_id].folder, ignore_errors=True))
        return run_id

    def finished(self, run_id):
        deadline = time.monotonic() + 6
        while time.monotonic() < deadline:
            result = self.runner.result(run_id)
            if result['status'] != 'running':
                return result
            time.sleep(.02)
        self.fail('Test worker did not finish')

    def test_successful_exit_without_report_is_incomplete(self):
        result = self.finished(self.launch('print("All tests passed!")'))
        self.assertEqual('incomplete', result['status'])
        self.assertTrue(result['report_errors'])

    def test_structured_failure_retrievable_without_reading_full_log(self):
        code = '''from pathlib import Path
Path(REPORT_PATH).write_text('<testsuite><testcase name="approval"><failure message="bad transition">leave.ts:42:7</failure></testcase></testsuite>')
raise SystemExit(1)'''
        run_id = self.launch(code)
        result = self.finished(run_id)
        self.assertEqual('failed', result['status'])
        self.assertEqual(1, result['counts']['failed'])
        detail = self.runner.details(run_id, failure_index=0)
        self.assertIn('leave.ts:42:7', detail['failure']['detail'])
        with self.assertRaises(ValueError):
            self.runner.details(run_id, failure_index=100)

    def test_timeout_terminates_process_and_does_not_pass(self):
        run_id = self.launch('import time; time.sleep(30)', timeout=.1)
        self.assertEqual('timed_out', self.finished(run_id)['status'])
        self.assertIsNotNone(self.runner.runs[run_id].process.poll())

    def test_cancel_and_concurrency_limit(self):
        run_id = self.launch('import time; time.sleep(30)')
        with self.assertRaises(ValueError):
            self.runner.start('web_unit')
        self.runner.cancel(run_id)
        self.assertEqual('cancelled', self.finished(run_id)['status'])

    def test_selection_rejects_arbitrary_suites_paths_and_non_test_files(self):
        folder = self.root / 'reports'
        for suite, file in [('shell', ''), ('web_unit', '../outside.ts'),
                            ('mobile', '/etc/passwd'), ('backend', '--help')]:
            with self.subTest(suite=suite), self.assertRaises(ValueError):
                suite_command(self.root, suite, folder, file)
        with self.assertRaises(ValueError):
            self.runner.result('../../outside')

    def test_preexisting_passing_maven_reports_are_not_reused_after_failed_build(self):
        path = self.root / 'services/leave/target/surefire-reports/TEST-old.xml'
        path.parent.mkdir(parents=True)
        path.write_text('<testsuite><testcase name="old passing test"/></testsuite>')
        def command(root, suite, folder, test_file):
            return SuiteCommand([sys.executable, '-c', 'raise SystemExit(1)'],
                                root, folder, 'maven', {})
        with patch('tools.project_mcp.test_runner.suite_command', side_effect=command):
            run_id = self.runner.start('backend')['run_id']
        self.addCleanup(lambda: shutil.rmtree(self.runner.runs[run_id].folder, ignore_errors=True))
        result = self.finished(run_id)
        self.assertEqual('failed', result['status'])
        self.assertEqual(0, result['counts']['passed'])
        self.assertTrue(result['report_errors'])

    def test_maven_snapshot_marks_preexisting_reports_for_freshness_check(self):
        path = self.root / 'services/leave/target/surefire-reports/TEST-old.xml'
        path.parent.mkdir(parents=True)
        path.write_text('<testsuite/>')
        before = maven_reports(self.root)
        self.assertEqual(before, maven_reports(self.root))
        self.assertIn(path, before)
