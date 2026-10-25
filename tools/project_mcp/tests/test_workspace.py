"""Acceptance tests for bounded evidence and local workspace boundaries."""
from pathlib import Path
import tempfile
import unittest

from tools.project_mcp.workspace import Workspace


class WorkspaceBehavior(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        (self.root / 'services').mkdir()
        (self.root / 'services/sample.py').write_text('first\nsecond\nthird\n')
        self.workspace = Workspace(self.root)

    def test_read_returns_exact_numbered_lines_and_content_digest(self):
        result = self.workspace.read('services/sample.py', start=2, count=1)
        self.assertEqual('2: second', result['text'])
        self.assertEqual(3, result['total_lines'])
        self.assertEqual(64, len(result['sha256']))
        self.assertTrue(result['truncated'])

    def test_traversal_environment_generated_and_external_symlink_are_rejected(self):
        with tempfile.TemporaryDirectory() as outside:
            secret = Path(outside) / 'secret.py'
            secret.write_text('private')
            (self.root / 'services/link.py').symlink_to(secret)
            (self.root / '.codex').mkdir()
            (self.root / '.codex/local.toml').write_text('private = true')
            (self.root / 'services/internal.toml').symlink_to(self.root / '.codex/local.toml')
            for path in ['services/internal.toml', '../secret.py', '/etc/passwd', '.env',
                         'web/node_modules/dependency/index.ts', 'services/link.py']:
                with self.subTest(path=path), self.assertRaises(ValueError):
                    self.workspace.resolve_source(path)

    def test_read_rejects_large_requests_and_marks_long_line_truncation(self):
        with self.assertRaises(ValueError):
            self.workspace.read('services/sample.py', count=10000)
        (self.root / 'services/long.py').write_text('x' * 20000)
        result = self.workspace.read('services/long.py')
        self.assertLessEqual(len(result['text']), 8000)
        self.assertTrue(result['truncated'])

    def test_structure_reports_large_source_without_claiming_architecture_correct(self):
        (self.root / 'services/large.py').write_text('pass\n' * 301)
        result = self.workspace.structure('services')
        self.assertEqual(1, len(result['review_files']))
        self.assertEqual('services/large.py', result['review_files'][0]['path'])
        self.assertIn('does not prove', result['limitation'])

    def test_literal_search_is_bounded_and_has_source_locations(self):
        result = self.workspace.search('second', 'services', limit=1)
        self.assertEqual('services/sample.py', result['matches'][0]['path'])
        self.assertEqual(2, result['matches'][0]['line'])
        self.assertEqual('second', result['matches'][0]['text'])
        self.assertEqual([], self.workspace.search('.*', 'services')['matches'])


if __name__ == '__main__':
    unittest.main()
