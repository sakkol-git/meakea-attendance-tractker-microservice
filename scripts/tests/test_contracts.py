"""Negative checks for drift and compatibility enforcement, not application mocks."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

MODULE = Path(__file__).resolve().parents[1] / 'contracts.py'
spec = importlib.util.spec_from_file_location('contracts', MODULE)
contracts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(contracts)


class ContractChecks(unittest.TestCase):
    def setUp(self):
        self.current = contracts.load(contracts.ROOT / 'contracts/openapi.json')

    def test_existing_consumers_protected_against_type_security_and_route_changes(self):
        mutations = [lambda s: s['paths'].clear(),
                     lambda s: s.update(security=[]),
                     lambda s: s['paths']['/leave/requests/{id}'].update(parameters=[{'name': 'newRequired', 'in': 'query', 'required': True}]),
                     lambda s: s.update(jsonSchemaDialect='https://example.test/dialect'),
                     lambda s: s['components']['schemas']['LeaveRecord']['properties']['version'].update(type='string'),
                     lambda s: s['paths']['/leave/requests/{id}/approve']['post']['parameters'].pop(),
                     lambda s: s['components']['schemas']['LeaveRecord']['properties'].update(description={'type': 'integer'})]
        for mutate in mutations:
            with self.subTest(mutation=mutate):
                changed = copy.deepcopy(self.current)
                mutate(changed)
                self.assertTrue(contracts.compatibility_errors(self.current, changed))

    def test_new_path_and_editorial_updates_allowed(self):
        changed = copy.deepcopy(self.current)
        changed['paths']['/new-resource'] = {'get': {'responses': {'204': {'description': 'New'}}}}
        changed['components']['schemas']['LeaveRecord']['description'] = 'Clarified wording'
        self.assertEqual([], contracts.compatibility_errors(self.current, changed))

    def test_operation_response_shape_is_checked_not_only_component(self):
        from jsonschema.exceptions import ValidationError
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in contracts.PAYLOADS:
                target = root / 'contracts' / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes((contracts.ROOT / 'contracts' / name).read_bytes())
            target = root / 'contracts/openapi.json'
            contract = json.loads(target.read_text())
            operation = contract['paths']['/leave/requests/{id}/approve']['post']
            operation['responses']['200']['content']['application/json']['schema'] = {'type': 'string'}
            target.write_text(json.dumps(contract))
            with self.assertRaises(ValidationError):
                contracts.validate(root)

    def test_missing_stale_and_tampered_client_bundles_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name, data in contracts.bundle().items():
                if name != 'bundle.json':
                    target = root / 'contracts' / name
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_bytes(data)
            self.assertEqual(2, len(contracts.sync_clients(root=root)))
            for client in ('web', 'mobile'):
                (root / client).mkdir()
            self.assertEqual([], contracts.sync_clients(write=True, root=root))
            self.assertEqual([], contracts.sync_clients(root=root))
            (root / 'web/contracts/openapi.json').write_text('{}')
            (root / 'mobile/contracts/bundle.json').unlink()
            self.assertEqual(2, len(contracts.sync_clients(root=root)))

    def test_invalid_fixture_fails_real_schema_validation(self):
        from jsonschema.exceptions import ValidationError
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in contracts.PAYLOADS:
                target = root / 'contracts' / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes((contracts.ROOT / 'contracts' / name).read_bytes())
            target = root / 'contracts/fixtures/leave-approval.json'
            fixture = json.loads(target.read_text())
            fixture['after']['version'] = '2'
            target.write_text(json.dumps(fixture))
            with self.assertRaises(ValidationError):
                contracts.validate(root)

    def test_only_verified_auth_slice_is_marked_implemented(self):
        expected = {
            'bootstrapBrowserCsrf', 'login', 'refreshToken',
            'getCurrentUser', 'patchUserProfile', 'changePassword',
            'logout', 'getJwks'
        }
        implemented = {
            operation['operationId']
            for path in self.current['paths'].values()
            for operation in path.values()
            if operation.get('x-hrflow-implementation') == 'implemented'
        }
        self.assertEqual(expected, implemented)


if __name__ == '__main__':
    unittest.main()
