"""Launch the actual stdio server and verify discovery, evidence, errors and checks."""
from pathlib import Path
import sys
import unittest

from mcp import Client
from mcp.client.stdio import StdioServerParameters

ROOT = Path(__file__).resolve().parents[3]


class ProtocolBehavior(unittest.IsolatedAsyncioTestCase):
    async def test_real_client_can_discover_and_call_bounded_tools(self):
        process = StdioServerParameters(command=sys.executable,
                                       args=['-m', 'tools.project_mcp.server'], cwd=ROOT)
        async with Client(process) as client:
            listed = await client.list_tools()
            self.assertEqual({'get_project_context', 'read_source', 'search_source',
                              'inspect_structure', 'run_project_check', 'run_tests', 'get_test_result',
                              'get_test_details', 'cancel_test_run'},
                             {tool.name for tool in listed.tools})
            context = await client.call_tool('get_project_context', {'area': 'leave'})
            self.assertFalse(context.is_error)
            self.assertIn('repositories', context.structured_content)
            read = await client.call_tool('read_source', {'path': 'AGENTS.md', 'count': 3})
            self.assertFalse(read.is_error)
            self.assertEqual('AGENTS.md', read.structured_content['path'])
            search = await client.call_tool('search_source', {
                'query': 'class Workspace', 'directory': 'tools/project_mcp', 'limit': 2})
            self.assertFalse(search.is_error)
            self.assertTrue(search.structured_content['matches'])
            structure = await client.call_tool('inspect_structure', {'directory': 'tools/project_mcp'})
            self.assertFalse(structure.is_error)
            self.assertEqual([], structure.structured_content['review_files'])
            forbidden = await client.call_tool('read_source', {'path': '../outside.md'})
            self.assertTrue(forbidden.is_error)
            invalid = await client.call_tool('run_project_check', {'name': 'arbitrary-shell'})
            self.assertTrue(invalid.is_error)
            invalid_suite = await client.call_tool('run_tests', {'suite': 'arbitrary-shell'})
            self.assertTrue(invalid_suite.is_error)
            missing_run = await client.call_tool('get_test_result', {'run_id': 'missing'})
            self.assertTrue(missing_run.is_error)
            checked = await client.call_tool('run_project_check', {'name': 'contract_schema'})
            self.assertFalse(checked.is_error)
            self.assertTrue(checked.structured_content['passed'])
