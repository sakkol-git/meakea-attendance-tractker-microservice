"""Check execution must fail honestly, preserve diagnostics and reject arbitrary commands."""
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

from tools.project_mcp.checks import run_check


class CheckBehavior(unittest.TestCase):
    def test_unknown_command_cannot_execute(self):
        with patch('tools.project_mcp.checks.subprocess.run') as execute:
            with self.assertRaises(ValueError):
                run_check(Path('.'), 'rm -rf /')
            execute.assert_not_called()

    def test_nonzero_check_is_failure_and_large_output_is_bounded(self):
        def failure(command, **kwargs):
            kwargs['stdout'].write(b'x' * 10000 + b'\nFAILED: synthetic fixture\n')
            return subprocess.CompletedProcess(command, 1)
        with patch('tools.project_mcp.checks.subprocess.run', side_effect=failure):
            result = run_check(Path('.'), 'foundation_tests')
        self.addCleanup(Path(result['full_log']).unlink)
        self.assertFalse(result['passed'])
        self.assertEqual(1, result['exit_code'])
        self.assertTrue(result['truncated'])
        self.assertLessEqual(len(result['output_tail']), 4000)
        self.assertIn('FAILED: synthetic fixture', result['output_tail'])

    def test_timeout_never_looks_like_success(self):
        with patch('tools.project_mcp.checks.subprocess.run',
                   side_effect=subprocess.TimeoutExpired('check', 30)):
            result = run_check(Path('.'), 'foundation_tests')
        self.addCleanup(Path(result['full_log']).unlink)
        self.assertTrue(result['timed_out'])
        self.assertFalse(result['passed'])
        self.assertIsNone(result['exit_code'])
