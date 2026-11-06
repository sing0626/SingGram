"""Expose compiler diagnostics for tracked Swift source as CI annotations.

Do not publish command lines, generated files or raw build logs, which can contain
build-time configuration. The build step preserves its original exit code.
"""
import argparse
from pathlib import Path
import re
import subprocess

ANSI = re.compile(r'\x1b\[[0-9;]*[A-Za-z]')
DIAGNOSTIC = re.compile(r'^(.+\.swift):(\d+):(\d+):\s+error:\s+(.+)$')


def escape(value):
    return value.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')


def annotations(log_path, repo):
    repo = Path(repo).resolve()
    tracked = set(subprocess.check_output(['git', '-C', str(repo), 'ls-files'], text=True).splitlines())
    seen = set()
    with open(log_path, errors='replace') as log:
        for line in log:
            match = DIAGNOSTIC.match(ANSI.sub('', line.strip()))
            if not match:
                continue
            filename, row, column, message = match.groups()
            path = Path(filename)
            candidates = [path] if path.is_absolute() else [repo / path, repo / 'official-ios' / path]
            for candidate in candidates:
                try:
                    relative = candidate.resolve().relative_to(repo).as_posix()
                except ValueError:
                    continue
                if relative not in tracked or not relative.startswith('official-ios/'):
                    continue
                result = (relative, row, column, message)
                if result not in seen:
                    seen.add(result)
                    yield f'::error file={escape(relative)},line={row},col={column}::{escape(message)}'
                break
            if len(seen) >= 25:
                break


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('log')
    parser.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    for annotation in annotations(args.log, args.repo):
        print(annotation)
