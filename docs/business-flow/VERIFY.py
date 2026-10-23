"""Read-only business-flow citation/coverage check; no server, DB or secret access."""
import ast
import collections
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'docs/business-flow'
FILES = [ROOT / 'business-flow.md'] + sorted(BASE.glob('*.md'))
TEXT = '\n'.join(p.read_text() for p in FILES)

citations = sorted({(p, s.rstrip('.')) for p, s in re.findall(
    r'((?:[\w.-]+/)+[\w.-]+\.(?:java|py|avsc))#([A-Za-z_][\w.]*)', TEXT)})
for path, symbol in citations:
    p = ROOT / path
    assert p.is_file(), ('missing citation file', path)
    if p.suffix == '.py':
        names = set()
        def visit(nodes, prefix=''):
            for n in nodes:
                if isinstance(n, (ast.ClassDef, ast.FunctionDef, ast.AsyncFunctionDef)):
                    names.add(prefix + n.name)
                    if isinstance(n, ast.ClassDef):
                        visit(n.body, prefix + n.name + '.')
                elif isinstance(n, (ast.Assign, ast.AnnAssign)):
                    for target in n.targets if isinstance(n, ast.Assign) else [n.target]:
                        if isinstance(target, ast.Name):
                            names.add(prefix + target.id)
        visit(ast.parse(p.read_text()).body)
        assert symbol in names, (path, symbol)
    elif p.suffix == '.java':
        source = p.read_text()
        for part in symbol.split('.'):
            assert re.search(r'\b' + re.escape(part) + r'\s*(?:\(|\{|implements|extends)', source), (path, symbol)
    else:
        assert json.loads(p.read_text()).get('name') == symbol, (path, symbol)
print('Source#symbol citations resolve:', len(citations))

bodies = '\n'.join(p.read_text() for p in FILES if re.search(r'^### BF-', p.read_text(), re.M))
definitions = re.findall(r'^### (BF-\d{3}).*?Status: ([A-Z-]+)', bodies, re.M)
assert len(definitions) == 40 and len({x for x, _ in definitions}) == 40
assert set(re.findall(r'BF-\d{3}', TEXT)) <= {x for x, _ in definitions}
fields = ['Purpose (business)', 'Trigger / entry point', 'Actors and permissions',
          'Preconditions and inputs', 'Main flow', 'Business rules and decisions',
          'Alternative and failure flows', 'State transitions', 'Data effects',
          'Events and integrations', 'Idempotency and concurrency', 'Security',
          'QA scenarios', 'Findings']
for block in re.split(r'(?=^### BF-)', bodies, flags=re.M):
    if block.startswith('### BF-'):
        for field in fields:
            assert len(re.findall(r'^- ' + re.escape(field) + ':', block, re.M)) == 1, (block[:60], field)
qas = set(re.findall(r'^\| (QA-\d{3}) /', (BASE / 'QA.md').read_text(), re.M))
assert len(qas) == 100 and set(re.findall(r'QA-\d{3}', TEXT)) <= qas
print('Workflow templates and references:', len(definitions), 'QA cases:', len(qas))

for p in FILES:
    assert len(p.read_text().splitlines()) <= 500, p
    for link in re.findall(r'\[[^\]]+\]\(([^)]+)\)', p.read_text()):
        assert (p.parent / link).is_file(), (p, link)
print('All relative links resolve; every Markdown file is within 500 lines')

# Keep the existing linter unchanged. Only the two names explicitly required by
# this task depart from its uppercase filename policy; reject every other error.
result = subprocess.run(['python3', str(ROOT / 'docs/roadmap/tools/docs-lint.py'),
                         str(ROOT / 'docs'), str(ROOT / 'README.md'),
                         str(ROOT / 'business-flow.md')], capture_output=True, text=True)
issues = [line for line in result.stdout.splitlines() if re.search(r':\d+: ', line)]
assert len(issues) == 2, result.stdout
assert all(':1: filename: use UPPER-KEBAB-CASE.md' in x for x in issues), issues
assert any('/business-flow.md:' in x for x in issues), issues
assert any('/business-flow/_inventory.md:' in x for x in issues), issues
print('Existing strict linter: only the two explicitly requested filename exceptions; no other errors')

for topic in re.findall(r'\b(?:org|attendance|leave|request|time)\.[a-z-]+\.v1\b',
                        (ROOT / 'infra/scripts/create-topic.sh').read_text()):
    assert topic in (BASE / 'EVENTS-DATA.md').read_text(), topic
assert 'AttendanceCheckedIn' in (BASE / 'EVENTS-DATA.md').read_text()
assert all(f'E{i:02}' in (BASE / 'API-MAP.md').read_text() for i in range(1, 8))
assert len(re.findall(r'^\| J\d{2}[HLR] \|', (BASE / 'API-MAP.md').read_text(), re.M)) == 21
assert len(re.findall(r'^\| API-\d{3} \|', (BASE / 'API-MAP.md').read_text(), re.M)) == 112
print('Seven observed route objects, ten scripted topics, one Avro record, and 112 historical candidates accounted for')

tracked = subprocess.check_output(['git', 'diff', '--name-only', 'HEAD'], cwd=ROOT, text=True).splitlines()
assert all(x == 'business-flow.md' or x.startswith('docs/business-flow/') for x in tracked), tracked
print('Tracked application/source/configuration and existing docs remain unchanged')
print('Status counts:', dict(collections.Counter(s for _, s in definitions)))
