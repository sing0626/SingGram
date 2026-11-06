import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'publish_ios_prerelease.sh'


class PublishPrereleaseTests(unittest.TestCase):
    def run_publish(self, existing=False, missing=False, fail_upload=False):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            log = root / 'calls.jsonl'
            fake = root / 'gh'
            fake.write_text(f'#!{sys.executable}\n' + '''import json, os, sys
from pathlib import Path
args = sys.argv[1:]
call = {'args': args}
if '--notes-file' in args:
    call['notes'] = Path(args[args.index('--notes-file') + 1]).read_text()
with open(os.environ['CALLS'], 'a') as out:
    out.write(json.dumps(call) + '\\n')
if args[:2] == ['release', 'view']:
    sys.exit(0 if os.environ['EXISTS'] == '1' else 1)
if args[:2] == ['release', 'upload'] and os.environ['FAIL_UPLOAD'] == '1':
    sys.exit(1)
''')
            fake.chmod(0o755)
            ipa = root / 'SingGram-unsigned.ipa'
            if not missing:
                ipa.write_bytes(b'test fixture only')
            env = dict(os.environ, PATH=f'{root}:{os.environ["PATH"]}', CALLS=str(log),
                       EXISTS=str(int(existing)), FAIL_UPLOAD=str(int(fail_upload)),
                       GITHUB_REPOSITORY='example/repo', GITHUB_SHA='b' * 40,
                       GITHUB_RUN_NUMBER='99', GITHUB_RUN_ATTEMPT='1',
                       IOS_BUILD_SHA='a' * 40, IOS_BUILD_NUMBER='13',
                       IOS_BUILD_ATTEMPT='1', IOS_BUILD_URL='https://github.com/example/repo/actions/runs/123')
            result = subprocess.run(['bash', str(SCRIPT), str(ipa)], env=env, capture_output=True, text=True)
            calls = [json.loads(line) for line in log.read_text().splitlines()] if log.exists() else []
            return result, calls

    def test_new_release_targets_original_build_and_only_uploads_ipa(self):
        result, calls = self.run_publish()
        self.assertEqual(result.returncode, 0, result.stderr)
        create = calls[-1]
        self.assertEqual(create['args'][:3], ['release', 'create', 'ios-13-1'])
        self.assertIn('--prerelease', create['args'])
        self.assertIn('--latest=false', create['args'])
        self.assertEqual(create['args'][create['args'].index('--target') + 1], 'a' * 40)
        self.assertTrue(create['args'][3].endswith('/SingGram-unsigned.ipa'))
        self.assertNotIn('Telegram.DSYMs.zip', ' '.join(create['args']))
        self.assertIn('Source commit: ' + 'a' * 40, create['notes'])

    def test_retry_updates_existing_prerelease(self):
        result, calls = self.run_publish(existing=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual([c['args'][1] for c in calls], ['view', 'upload', 'edit'])
        self.assertIn('--clobber', calls[1]['args'])
        self.assertIn('--prerelease', calls[2]['args'])

    def test_missing_asset_does_not_touch_github(self):
        result, calls = self.run_publish(missing=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(calls, [])

    def test_failed_upload_stops_before_release_metadata_changes(self):
        result, calls = self.run_publish(existing=True, fail_upload=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual([c['args'][1] for c in calls], ['view', 'upload'])


if __name__ == '__main__':
    unittest.main()
