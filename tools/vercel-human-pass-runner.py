from __future__ import annotations
import base64, gzip, json, re, shutil, subprocess, sys
from pathlib import Path

ROOT = Path.cwd()
EMBEDDED = ROOT / '.github/workflows/public-human-pass.yml'
REPORT = ROOT / 'human-pass-build-report.txt'


def run(*args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(args, cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=True)


def main() -> None:
    text = EMBEDDED.read_text(encoding='utf-8')
    matches = re.findall(r"printf '%s' '([^']+)' \| base64 -d \| gzip -d > (/tmp/[^\n]+)", text)
    if len(matches) != 2:
        raise RuntimeError(f'Expected 2 embedded payloads, found {len(matches)}')
    for payload, target in matches:
        Path(target).write_bytes(gzip.decompress(base64.b64decode(payload)))

    run('git', 'fetch', 'origin', 'main:refs/remotes/origin/main')
    shutil.copy2('/tmp/sov-human.css', ROOT / 'sov-human.css')
    run('git', 'config', 'user.name', 'vercel-human-pass')
    run('git', 'config', 'user.email', 'vercel-human-pass@invalid.local')
    proc = subprocess.run(
        [sys.executable, '/tmp/apply_public_human_pass.py'],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    REPORT.write_text(proc.stdout, encoding='utf-8')
    if proc.returncode:
        print(proc.stdout)
        raise SystemExit(proc.returncode)

    changed = run('git', 'diff', '--name-only', 'origin/main...HEAD').stdout.splitlines()
    ignored = {
        '.github/workflows/public-human-pass-pr.yml',
        'tools/vercel-human-pass-runner.py',
        'vercel.json',
        'human-pass-build-report.txt',
    }
    allowed = [p for p in changed if p not in ignored and not p.startswith('.github/workflows/')]
    (ROOT / 'human-pass-changed-files.json').write_text(
        json.dumps({'count': len(allowed), 'files': allowed}, ensure_ascii=False, indent=2),
        encoding='utf-8',
    )
    print(proc.stdout)
    print(f'VERCEL HUMAN PASS READY: {len(allowed)} changed files')


if __name__ == '__main__':
    main()
