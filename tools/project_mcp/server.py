"""Local MCP tools for local, bounded HRFlow engineering evidence."""
from pathlib import Path
from typing import Literal

from mcp.server import MCPServer
from mcp.types import ToolAnnotations

from tools.project_mcp.checks import CheckName, run_check
from tools.project_mcp.context import project_context
from tools.project_mcp.workspace import Workspace
from tools.project_mcp.test_runner import TestRunner
from tools.project_mcp.test_suites import SuiteName

ROOT = Path(__file__).resolve().parents[2]
workspace = Workspace(ROOT)
test_runner = TestRunner(ROOT)
server = MCPServer('hrflow-project', instructions=(
    'Use narrow reads and named checks. Every result is bounded evidence, not business acceptance. '
    'Read AGENTS.md and the owning requirement; disclose missing or truncated evidence.'))
READ_ONLY = ToolAnnotations(read_only_hint=True, open_world_hint=False)


@server.tool(annotations=READ_ONLY)
def get_project_context(
    area: Literal['overview', 'leave', 'attendance', 'identity', 'requests', 'infra'] = 'overview',
) -> dict[str, object]:
    """Get fresh Git revisions, bounded dirty state and feature entry points without full-repo dumps."""
    return project_context(ROOT, area)


@server.tool(annotations=READ_ONLY)
def read_source(path: str, start: int = 1, count: int = 80) -> dict[str, object]:
    """Read 1..160 numbered source lines with a content hash; excludes private/generated paths."""
    return workspace.read(path, start, count)


@server.tool(annotations=READ_ONLY)
def search_source(query: str, directory: str = '.', limit: int = 12) -> dict[str, object]:
    """Find a literal phrase in a narrow source directory; return at most 30 located matches."""
    return workspace.search(query, directory, limit)


@server.tool(annotations=READ_ONLY)
def inspect_structure(directory: str = '.') -> dict[str, object]:
    """List up to 20 source files over 300 lines for cohesion review; not an architecture verdict."""
    return workspace.structure(directory)


@server.tool(annotations=ToolAnnotations(
    read_only_hint=False, destructive_hint=False, idempotent_hint=False, open_world_hint=False,
))
def run_project_check(name: CheckName) -> dict[str, object]:
    """Run one allowlisted 30-second local check, returning a short result and full log path."""
    return run_check(ROOT, name)


@server.tool(annotations=ToolAnnotations(
    read_only_hint=False, destructive_hint=False, idempotent_hint=False, open_world_hint=False,
))
def run_tests(suite: SuiteName, test_file: str = '') -> dict[str, object]:
    """Start backend/web_unit/web_e2e/mobile tests in background; optional existing repo-relative file."""
    return test_runner.start(suite, test_file)


@server.tool(annotations=READ_ONLY)
def get_test_result(run_id: str) -> dict[str, object]:
    """Get status, counts, five failure summaries, exact-message groups and compiler diagnostics."""
    return test_runner.result(run_id)


@server.tool(annotations=READ_ONLY)
def get_test_details(run_id: str, failure_index: int | None = None,
                     log_start: int | None = None, lines: int = 60) -> dict[str, object]:
    """Read one failure stack or bounded console excerpt; default excerpt is the log tail."""
    return test_runner.details(run_id, failure_index, log_start, lines)


@server.tool(annotations=ToolAnnotations(
    read_only_hint=False, destructive_hint=False, idempotent_hint=True, open_world_hint=False,
))
def cancel_test_run(run_id: str) -> dict[str, object]:
    """Cancel an active tool-started test run and terminate its local process group."""
    return test_runner.cancel(run_id)


if __name__ == '__main__':
    server.run(transport='stdio')
