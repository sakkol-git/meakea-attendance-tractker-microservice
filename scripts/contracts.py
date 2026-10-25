#!/usr/bin/env python3
"""Validate the shared slice, detect drift, and copy deterministic client bundles."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
PAYLOADS = ('openapi.json', 'fixtures/leave-approval.json', 'check-bundle.py')
METHODS = {'get', 'post', 'put', 'patch', 'delete', 'head', 'options', 'trace'}
# Only editorial fields are excluded. Defaults, required, enums, security and
# response schemas all remain protected. This is deliberately conservative.
EDITORIAL = {'description', 'summary', 'example', 'examples', 'externalDocs'}


def load(path):
    return json.loads(path.read_text())


def semantic(value, named_map=False):
    if isinstance(value, dict):
        maps = {'properties', 'patternProperties', '$defs', 'schemas', 'responses',
                'headers', 'securitySchemes', 'content', 'encoding', 'links',
                'callbacks', 'requestBodies', 'dependentSchemas'}
        return {k: semantic(v, k in maps) for k, v in value.items()
                if named_map or (k not in EDITORIAL and not k.startswith('x-hrflow-'))}
    if isinstance(value, list):
        return [semantic(v) for v in value]
    return value


def compatibility_errors(old, new):
    """Freeze existing wire definitions; permit new paths/components only.

    This is stricter than a semantic breaking-change detector: an additive field
    in an existing schema also needs a separately versioned definition/review.
    The version string alone never bypasses the gate.
    """
    errors = []
    root_keys = (set(old) | set(new)) - {'info', 'paths', 'components', 'tags'}
    for key in sorted(root_keys):
        if key in EDITORIAL or key.startswith('x-hrflow-'):
            continue
        if semantic(old.get(key)) != semantic(new.get(key)):
            errors.append(f'Existing API {key} changed')
    for path, item in old.get('paths', {}).items():
        newer = new.get('paths', {}).get(path, {})
        keys = set(item) | (set(newer) - METHODS)
        for key in sorted(keys):
            if semantic(item.get(key)) != semantic(newer.get(key)):
                errors.append(f'Existing path definition changed: {key.upper()} {path}')
    for category, definitions in old.get('components', {}).items():
        for name, value in definitions.items():
            other = new.get('components', {}).get(category, {}).get(name)
            if semantic(value) != semantic(other):
                errors.append(f'Existing component changed: {category}/{name}')
    return errors


def bundle(root=ROOT):
    content = {name: (root / 'contracts' / name).read_bytes() for name in PAYLOADS}
    hashes = {name: hashlib.sha256(data).hexdigest() for name, data in content.items()}
    receipt = {'source': 'HRFlow backend repository / contracts',
               'version': load(root / 'contracts/openapi.json')['info']['version'],
               'files': hashes}
    content['bundle.json'] = (json.dumps(receipt, indent=2) + '\n').encode()
    return content


def sync_clients(write=False, root=ROOT):
    errors = []
    for client in ('web', 'mobile'):
        if not (root / client).is_dir():
            errors.append(f'{client}: checkout missing; both client repositories are required')
            continue
        for name, content in bundle(root).items():
            path = root / client / 'contracts' / name
            if write:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(content)
            elif not path.is_file() or path.read_bytes() != content:
                errors.append(f'{client}/contracts/{name}: drift; run scripts/contracts.py sync')
    return errors


def validate(root=ROOT):
    from openapi_spec_validator import validate as validate_openapi
    from jsonschema import Draft202012Validator, FormatChecker
    from referencing import Registry, Resource
    from referencing.jsonschema import DRAFT202012

    spec = load(root / 'contracts/openapi.json')
    validate_openapi(spec)
    registry = Registry().with_resource(
        'urn:hrflow:openapi', Resource.from_contents(spec, default_specification=DRAFT202012))
    fixture = load(root / 'contracts/fixtures/leave-approval.json')

    operations = {op['operationId']: (path, method, op)
                  for path, item in spec['paths'].items()
                  for method, op in item.items() if method in METHODS}

    def check_at(parts, value):
        pointer = '/' + '/'.join(part.replace('~', '~0').replace('/', '~1') for part in parts)
        Draft202012Validator({'$ref': 'urn:hrflow:openapi#' + pointer},
                             registry=registry, format_checker=FormatChecker()).validate(value)

    read_path, read_method, _ = operations['getLeaveRequest']
    path, method, operation = operations[fixture['operationId']]
    location = ['paths', path, method]
    check_at(['paths', read_path, read_method, 'responses', '200', 'content',
              'application/json', 'schema'], fixture['before'])
    check_at(location + ['responses', '200', 'content', 'application/json', 'schema'],
             fixture['after'])
    check_at(location + ['responses', '412', 'content', 'application/problem+json', 'schema'],
             fixture['error'])
    for name in ('webBody', 'mobileBody'):
        if fixture[name] is not None:
            check_at(location + ['requestBody', 'content', 'application/json', 'schema'], fixture[name])
        elif operation['requestBody'].get('required', False):
            raise ValueError(f'{name}: consumer omits a required request body')
    if fixture['before']['id'] != fixture['after']['id'] or fixture['after']['id'] != fixture['id']:
        raise ValueError('Fixture ID mismatch')
    if fixture['after']['version'] <= fixture['before']['version']:
        raise ValueError('Fixture must advance version')
    values = {'If-Match': fixture['etag'], 'Idempotency-Key': fixture['idempotencyKey']}
    for parameter in operation['parameters']:
        value = fixture['id'] if parameter['in'] == 'path' else values[parameter['name']]
        Draft202012Validator(parameter['schema'], format_checker=FormatChecker()).validate(value)
    etag_schema = operation['responses']['200']['headers']['ETag']['schema']
    for key in ('etag', 'nextEtag'):
        Draft202012Validator(etag_schema).validate(fixture[key])
    print(f'Validated OpenAPI, {len(operations)} operations and shared leave fixtures')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['validate', 'sync', 'check', 'compat'])
    parser.add_argument('--base', help='Git revision containing the previous contract (required for compat)')
    args = parser.parse_args()
    try:
        if args.command == 'validate':
            validate()
        elif args.command in ('sync', 'check'):
            errors = sync_clients(write=args.command == 'sync')
            if errors:
                raise ValueError('\n'.join(errors))
            print('Both client bundles synchronized' if args.command == 'sync' else 'Both client bundles match')
        else:
            if not args.base:
                parser.error('compat requires --base; use the PR target or merge base')
            # Check the revision first so an invalid ref cannot look like first setup.
            subprocess.run(['git', 'rev-parse', '--verify', args.base + '^{commit}'],
                           cwd=ROOT, check=True, stdout=subprocess.DEVNULL)
            exists = subprocess.run(['git', 'cat-file', '-e', f'{args.base}:contracts/openapi.json'],
                                    cwd=ROOT, capture_output=True)
            if exists.returncode:
                print('Initial contract: baseline has no contracts/openapi.json; compatibility not established')
                return
            previous = subprocess.check_output(
                ['git', 'show', f'{args.base}:contracts/openapi.json'], cwd=ROOT, text=True)
            errors = compatibility_errors(json.loads(previous), load(ROOT / 'contracts/openapi.json'))
            if errors:
                raise ValueError('\n'.join(errors) + '\nPreserve the old wire definition; add a versioned replacement.')
            print('Existing wire definitions preserved')
    except (ValueError, OSError, AssertionError, subprocess.CalledProcessError, ImportError) as error:
        print(f'Contract check failed: {error}', file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
