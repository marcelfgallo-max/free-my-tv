#!/usr/bin/env python3
"""Compare a JDK 17 build with the pre-token build. Stores hashes only, never bytecode or config."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
BASELINE = ROOT / 'design-review' / 'phase1-baseline.json'
REPORT = ROOT / 'design-review' / 'phase1-verification.json'
MIGRATED = ('HomeView', 'SettingsActivity', 'Ui')


def sha(data):
    return hashlib.sha256(data).hexdigest()


def snapshot():
    java_home = os.environ.get('JAVA_HOME')
    if not java_home:
        raise SystemExit('Set JAVA_HOME to JDK 17 first.')
    javap = str(Path(java_home) / 'bin' / 'javap')
    version = subprocess.check_output([javap, '-version'], text=True).strip()
    if not version.startswith('17.'):
        raise SystemExit('JDK 17 required, found ' + version)
    classes = {}
    for path in sorted((ROOT / 'out/classes').rglob('*.class')):
        name = path.relative_to(ROOT / 'out/classes').as_posix()
        entry = {'class_sha256': sha(path.read_bytes())}
        if any(path.stem == prefix or path.stem.startswith(prefix + '$') for prefix in MIGRATED):
            code = subprocess.check_output([javap, '-c', '-p', str(path)], text=True)
            # Constant-pool indices can move when Java adds InnerClasses metadata for Theme.Legacy.
            # Keep every instruction, jump address, descriptor and resolved constant/reference.
            code = re.sub(r'#\d+[ \t]*', '# ', code)
            entry['instructions_sha256'] = sha(code.encode())
        classes[name] = entry
    if not classes:
        raise SystemExit('Build first: out/classes is empty.')
    assets = {p.relative_to(ROOT).as_posix(): sha(p.read_bytes())
              for base in ('app/assets', 'app/res') for p in sorted((ROOT / base).rglob('*')) if p.is_file()}
    return {'jdk': version, 'classes': classes, 'assets': assets}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline', action='store_true')
    args = parser.parse_args()
    current = snapshot()
    if args.baseline:
        if BASELINE.exists():
            raise SystemExit('Baseline exists; refusing to overwrite.')
        current['base_commit'] = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
        BASELINE.write_text(json.dumps(current, indent=2) + '\n')
        print('Baseline recorded:', len(current['classes']), 'classes,', len(current['assets']), 'assets/resources.')
        return
    baseline = json.loads(BASELINE.read_text())
    failures, identical, equivalent = [], [], []
    for name, before in baseline['classes'].items():
        after = current['classes'].get(name)
        if after is None:
            failures.append(name + ': missing')
        elif before['class_sha256'] == after['class_sha256']:
            identical.append(name)
        elif before.get('instructions_sha256') and before['instructions_sha256'] == after.get('instructions_sha256'):
            equivalent.append(name)
        else:
            failures.append(name + ': bytecode changed')
    additions = sorted(set(current['classes']) - set(baseline['classes']))
    for name in additions:
        if not re.fullmatch(r'io/freemytv/Theme(?:\$[\w$]+)?\.class', name):
            failures.append(name + ': unexpected new class')
    if baseline['assets'] != current['assets']:
        failures.append('assets/resources changed')
    if baseline['jdk'] != current['jdk']:
        failures.append('JDK changed')
    report = {'base_commit': baseline['base_commit'], 'jdk': current['jdk'],
              'identical_class_count': len(identical), 'equivalent_instructions': equivalent,
              'new_theme_classes': additions, 'assets_unchanged': baseline['assets'] == current['assets'],
              'failures': failures, 'visual_validation': 'pending real TV before/after screenshots'}
    REPORT.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))
    raise SystemExit(bool(failures))


if __name__ == '__main__':
    main()
