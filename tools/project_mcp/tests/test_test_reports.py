"""Machine reports, not a successful-looking console tail, establish test outcomes."""
import json
from pathlib import Path
import tempfile
import unittest

from tools.project_mcp.test_reports import parse_junit, parse_flutter, summarize


class TestReportBehavior(unittest.TestCase):
    def test_junit_counts_cases_once_and_preserves_failure_locations(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / 'result.xml'
            report.write_text('''<testsuites tests="99"><testsuite tests="99">
              <testcase name="ok"/>
              <testcase name="bad" classname="LeaveTest"><failure message="expected 2 got 1">at leave.ts:42:7</failure></testcase>
              <testcase name="skip"><skipped/></testcase>
            </testsuite></testsuites>''')
            data = parse_junit([report])
        self.assertEqual({'passed': 1, 'failed': 1, 'skipped': 1}, data['counts'])
        self.assertIn('leave.ts:42:7', data['failures'][0]['locations'])
        self.assertEqual('bad', data['failures'][0]['name'])

    def test_empty_malformed_or_absent_reports_cannot_establish_success(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / 'broken.xml'
            report.write_text('<broken')
            data = parse_junit([report])
            self.assertTrue(data['report_errors'])
            self.assertEqual('incomplete', summarize(data, exit_code=0)['status'])
        self.assertEqual('incomplete', summarize(parse_junit([]), exit_code=0)['status'])

    def test_process_failure_overrides_passing_report_and_timeout_overrides_partial_report(self):
        data = {'counts': {'passed': 2, 'failed': 0, 'skipped': 0}, 'failures': [], 'report_errors': []}
        self.assertEqual('failed', summarize(data, exit_code=1)['status'])
        self.assertEqual('timed_out', summarize(data, exit_code=None, timed_out=True)['status'])

    def test_flutter_ignores_hidden_loader_success_but_retains_compile_failure(self):
        events = [
            {'type': 'testStart', 'test': {'id': 1, 'name': 'loading sample', 'url': 'file:///tmp/sample.dart'}},
            {'type': 'testDone', 'testID': 1, 'result': 'success', 'hidden': True, 'skipped': False},
            {'type': 'testStart', 'test': {'id': 2, 'name': 'approval'}},
            {'type': 'error', 'testID': 2, 'error': 'expected APPROVED', 'stackTrace': 'sample.dart:25:3'},
            {'type': 'testDone', 'testID': 2, 'result': 'failure', 'hidden': False, 'skipped': False},
            {'type': 'done', 'success': False},
        ]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'events.jsonl'
            path.write_text('\n'.join(json.dumps(event) for event in events))
            data = parse_flutter(path)
        self.assertEqual({'passed': 0, 'failed': 1, 'skipped': 0}, data['counts'])
        self.assertEqual('approval', data['failures'][0]['name'])
        self.assertEqual('failed', summarize(data, exit_code=1)['status'])

    def test_summary_is_bounded_and_groups_repeated_error_messages(self):
        failures = [{'name': str(i), 'message': 'same compiler error', 'detail': 'x' * 20000,
                     'locations': []} for i in range(20)]
        data = {'counts': {'passed': 0, 'failed': 20, 'skipped': 0},
                'failures': failures, 'report_errors': []}
        result = summarize(data, exit_code=1)
        self.assertEqual(5, len(result['failures']))
        self.assertEqual(20, result['failure_groups'][0]['affected_tests'])
        self.assertTrue(result['truncated'])
        self.assertNotIn('detail', result['failures'][0])
