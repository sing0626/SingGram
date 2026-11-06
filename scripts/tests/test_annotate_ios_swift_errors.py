import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('annotator', ROOT / 'scripts/annotate_ios_swift_errors.py')
annotator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(annotator)
SOURCE = 'submodules/TelegramUI/Sources/AppDelegate.swift'


class AnnotationTests(unittest.TestCase):
    def parse(self, text):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'build.log'
            path.write_text(text)
            return list(annotator.annotations(path, ROOT))

    def test_accepts_relative_and_absolute_tracked_swift_paths_once(self):
        relative = f'{SOURCE}:12:4: error: missing type\n'
        absolute = f'{ROOT}/official-ios/{SOURCE}:12:4: error: missing type\n'
        result = self.parse(relative + absolute)
        self.assertEqual(result, [f'::error file=official-ios/{SOURCE},line=12,col=4::missing type'])

    def test_skips_generated_files_command_lines_and_non_swift_diagnostics(self):
        result = self.parse('clang -DPRIVATE_VALUE=placeholder\n'
                            'build-input/config.swift:1:1: error: generated configuration\n'
                            '/tmp/unknown.swift:1:1: error: external source\n'
                            'submodules/BuildConfig/Sources/BuildConfig.m:1:1: error: generated flags\n')
        self.assertEqual(result, [])

    def test_strips_ansi_and_escapes_workflow_command_characters(self):
        result = self.parse(f'\x1b[31m{SOURCE}:12:4: error: value%0Aexample\x1b[0m\n')
        self.assertEqual(result, [f'::error file=official-ios/{SOURCE},line=12,col=4::value%250Aexample'])


if __name__ == '__main__':
    unittest.main()
