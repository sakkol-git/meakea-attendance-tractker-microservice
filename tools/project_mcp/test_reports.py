"""Parse test-runner evidence and produce compact, non-speculative summaries."""
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

LOCATION = re.compile(r'[\w./\\-]+\.(?:dart|ts|vue|java|py):\d+(?::\d+)?')
ANSI = re.compile(r'\x1b\[[0-9;]*[A-Za-z]')


def empty_report() -> dict:
    return {'counts': {'passed': 0, 'failed': 0, 'skipped': 0},
            'failures': [], 'report_errors': []}


def failure(name: str, message: str, detail: str, source: str = '') -> dict:
    text = ANSI.sub('', message + '\n' + detail)
    return {'name': name[:300], 'message': ANSI.sub('', message)[:600],
            'detail': text[:12000], 'detail_truncated': len(text) > 12000,
            'locations': list(dict.fromkeys(LOCATION.findall(text)))[:8], 'source': source}


def parse_junit(paths: list[Path]) -> dict:
    report = empty_report()
    if not paths:
        report['report_errors'].append('No current-run JUnit report was produced')
    for path in paths:
        try:
            if path.stat().st_size > 20_000_000:
                raise ValueError('Report exceeds 20 MB')
            root = ET.parse(path).getroot()
            for case in root.iter():
                if case.tag.rsplit('}', 1)[-1] != 'testcase':
                    continue
                children = {child.tag.rsplit('}', 1)[-1]: child for child in case}
                problem = children.get('failure')
                if problem is None:
                    problem = children.get('error')
                outcome = 'failed' if problem is not None else 'skipped' if 'skipped' in children else 'passed'
                report['counts'][outcome] += 1
                if problem is not None:
                    detail = ''.join(problem.itertext())
                    message = problem.get('message') or next(iter(detail.splitlines()), 'Test failed')
                    report['failures'].append(failure(case.get('name', 'Unnamed test'), message,
                                                      detail, case.get('file') or case.get('classname', '')))
        except (OSError, ET.ParseError, ValueError) as error:
            report['report_errors'].append(f'{path.name}: {error}')
    return report


def parse_flutter(path: Path) -> dict:
    report = empty_report()
    names, outcomes, errors = {}, {}, {}
    complete = False
    try:
        if path.stat().st_size > 20_000_000:
            raise ValueError('Report exceeds 20 MB')
        with path.open() as stream:
            for line in stream:
                event = json.loads(line)
                kind, test_id = event.get('type'), event.get('testID', 'global')
                if kind == 'testStart':
                    test = event['test']
                    names[test['id']] = (test['name'], test.get('url', ''))
                elif kind == 'error':
                    errors.setdefault(test_id, []).append((event.get('error', 'Test error'),
                                                            event.get('stackTrace', '')))
                    outcomes[test_id] = 'failed'
                elif kind == 'testDone':
                    if test_id in errors or event['result'] in ('failure', 'error'):
                        outcomes[test_id] = 'failed'
                    elif event.get('skipped'):
                        outcomes[test_id] = 'skipped'
                    elif not event.get('hidden'):
                        outcomes[test_id] = 'passed'
                elif kind == 'done':
                    complete = True
                    if not event.get('success') and not any(value == 'failed' for value in outcomes.values()):
                        report['report_errors'].append('Runner reported unsuccessful completion without test failure detail')
    except (OSError, ValueError, KeyError) as error:
        report['report_errors'].append(f'Flutter report: {error}')
    for test_id, outcome in outcomes.items():
        report['counts'][outcome] += 1
        if outcome == 'failed':
            name, source = names.get(test_id, (str(test_id), ''))
            messages = errors.get(test_id, [('Test failed without error detail', '')])
            report['failures'].append(failure(name, messages[0][0],
                                              '\n'.join(a + '\n' + b for a, b in messages), source))
    if not complete:
        report['report_errors'].append('Flutter report has no terminal done event')
    return report


def summarize(report: dict, exit_code: int | None, timed_out: bool = False) -> dict:
    counts = report['counts']
    status = 'passed'
    if timed_out:
        status = 'timed_out'
    elif exit_code != 0 or counts['failed']:
        status = 'failed'
    elif report['report_errors'] or not counts['passed']:
        status = 'incomplete'
    groups = {}
    for index, item in enumerate(report['failures']):
        key = item['message']
        group = groups.setdefault(key, {'message': key[:400], 'affected_tests': 0,
                                        'example_failure_index': index})
        group['affected_tests'] += 1
    return {'status': status, 'exit_code': exit_code, 'counts': dict(counts),
            'failures': [{'index': index, 'name': item['name'], 'message': item['message'][:400],
                          'locations': item['locations'][:3]}
                         for index, item in enumerate(report['failures'][:5])],
            'failure_count': len(report['failures']), 'failure_groups': list(groups.values())[:5],
            'report_errors': report['report_errors'][:5],
            'truncated': len(report['failures']) > 5 or len(report['report_errors']) > 5
            or any(len(item['message']) > 400 or len(item['locations']) > 3
                   for item in report['failures'][:5]),
            'limitation': 'Observed test/report evidence only. Similar messages are grouped exactly; '
            'root cause and business correctness still need investigation.'}


def diagnostic_excerpt(log: Path) -> list[str]:
    """Pick compiler/harness error lines only; do not infer causes from them."""
    messages = []
    if log.is_file():
        with log.open(errors='replace') as stream:
            for line in stream:
                text = ANSI.sub('', line).strip()
                if re.search(r'\[ERROR\]|\bError:|\berror [•-]|Compilation failed|Cannot find|command not found', text):
                    if text[:400] not in messages:
                        messages.append(text[:400])
                    if len(messages) == 6:
                        break
    return messages
