"""Exercise smoke-test success/failure decisions without a preview cluster."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'logstash-manual-requeue-smoke.sh'


class LogstashSmokeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not shutil.which('jq'):
            raise RuntimeError('jq is required to run the Logstash smoke-script tests')

    def run_smoke(self, mode='recovery', response='delayed', missing=None,
                  namespace='pr-test', context='cft-preview-00-aks'):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            kubectl = root / 'kubectl'
            kubectl.write_text('''#!/usr/bin/env python3
import json, os, pathlib, sys
args = sys.argv[1:]
if args[0] == 'config':
    print(os.environ['MOCK_KUBE_CONTEXT'])
elif args[0] == 'get':
    print('preview-pod')
elif 'psql' in args:
    sql = args[-1]
    if 'flyway_schema_history' in sql: print(3)
    elif "SearchCriteria" in sql: print(1 if os.environ['LOGSTASH_SMOKE_MODE'] == 'cutover' else 0)
    elif 'SELECT id FROM case_data WHERE' in sql: print(42)
    elif 'INSERT INTO' in sql: print(10000000001)
    else: print(0)
elif '-X' in args:
    print(200 if os.environ['MOCK_RESPONSE'] == 'accepted-stale' else 409)
else:
    counter = pathlib.Path('reads')
    count = int(counter.read_text()) + 1 if counter.exists() else 1
    counter.write_text(str(count))
    response = os.environ['MOCK_RESPONSE']
    stale = response == 'stale' or (response == 'delayed' and count == 1)
    data = {'HMCTSServiceId': 'BBA3', 'orgs_assigned_users': {'OrgA': 22}}
    if response == 'wrong-data': data['orgs_assigned_users']['OrgA'] = 21
    if '/global_search/' in args[-1]: data = {'HMCTSServiceId': 'BBA3'}
    print(json.dumps({'found': True, '_version': 7 if stale else 10000000001,
                      '_source': {'supplementary_data': data}}))
''')
            kubectl.chmod(0o755)
            sleep = root / 'sleep'
            sleep.write_text('#!/bin/sh\nexit 0\n')
            sleep.chmod(0o755)
            env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ['PATH'],
                       BRANCH_NAME='PR-1', TEAM_NAMESPACE=namespace, MOCK_KUBE_CONTEXT=context,
                       LOGSTASH_SMOKE_MODE=mode, MOCK_RESPONSE=response,
                       LOGSTASH_MANUAL_REQUEUE_CASE_REFERENCE='1234567890123456',
                       LOGSTASH_MANUAL_REQUEUE_CASE_TYPE='AAT_PRIVATE',
                       LOGSTASH_MANUAL_REQUEUE_EXPECTED_SUPPLEMENTARY_DATA=
                       '{"HMCTSServiceId":"BBA3","orgs_assigned_users":{"OrgA":22}}',
                       LOGSTASH_MANUAL_REQUEUE_OUTAGE_EVIDENCE='https://evidence/outage',
                       LOGSTASH_CUTOVER_EVIDENCE='https://evidence/cutover',
                       LOGSTASH_CUTOVER_QUEUE_ID='10000000001')
            if missing:
                env.pop(missing)
            result = subprocess.run(['bash', str(SCRIPT)], cwd=root, env=env,
                                    capture_output=True, text=True, timeout=45)
            evidence = root / 'Logstash Manual Requeue Smoke/evidence.md'
            return result, evidence.read_text() if evidence.exists() else ''

    def test_waits_for_requeued_version(self):
        result, evidence = self.run_smoke()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('version equals queue ID', evidence)

    def test_allows_shared_ccd_namespace_on_preview_cluster(self):
        result, _ = self.run_smoke(namespace='ccd', response='current')
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_rejects_shared_ccd_namespace_outside_preview(self):
        result, evidence = self.run_smoke(namespace='ccd', context='cft-prod-00-aks')
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(evidence, '')

    def test_rejects_old_document_with_matching_data(self):
        result, evidence = self.run_smoke(response='stale')
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(evidence, '')

    def test_rejects_new_version_with_wrong_data(self):
        result, _ = self.run_smoke(response='wrong-data')
        self.assertNotEqual(result.returncode, 0)

    def test_requires_outage_evidence(self):
        result, _ = self.run_smoke(missing='LOGSTASH_MANUAL_REQUEUE_OUTAGE_EVIDENCE')
        self.assertNotEqual(result.returncode, 0)

    def test_cutover_rejects_accepted_stale_write(self):
        result, _ = self.run_smoke(mode='cutover', response='accepted-stale')
        self.assertNotEqual(result.returncode, 0)

    def test_requires_case_reference(self):
        result, _ = self.run_smoke(missing='LOGSTASH_MANUAL_REQUEUE_CASE_REFERENCE')
        self.assertNotEqual(result.returncode, 0)

    def test_cutover_checks_both_destinations(self):
        result, evidence = self.run_smoke(mode='cutover', response='current')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('aat_private_cases global_search', evidence)


if __name__ == '__main__':
    unittest.main()
