"""Registration is repeatable and must preserve unrelated local settings."""
import importlib.util
from pathlib import Path
import tempfile
import tomllib
import unittest

path = Path(__file__).resolve().parents[3] / 'scripts/setup-project-mcp.py'
spec = importlib.util.spec_from_file_location('mcp_setup', path)
setup = importlib.util.module_from_spec(spec)
spec.loader.exec_module(setup)


class RegistrationBehavior(unittest.TestCase):
    def test_idempotent_registration_preserves_other_servers(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            config = root / '.codex/config.toml'
            config.parent.mkdir()
            config.write_text('[mcp_servers.existing]\ncommand = "unchanged"\n')
            setup.register(root, root / '.venv-mcp/bin/python')
            content = config.read_text()
            setup.register(root, root / '.venv-mcp/bin/python')
            self.assertEqual(content, config.read_text())
            self.assertEqual('unchanged', tomllib.loads(content)['mcp_servers']['existing']['command'])
            with self.assertRaises(ValueError):
                setup.register(root, root / 'another/python')
            self.assertEqual(content, config.read_text())
