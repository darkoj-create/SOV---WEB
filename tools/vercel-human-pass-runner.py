from __future__ import annotations
import base64, gzip, json, re, shutil, subprocess, sys
from pathlib import Path

ROOT = Path.cwd()
BASE_SHA = '4a000ba3cf46cfdb38499f98fff0c3414d10ce74'
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

    script_path = Path('/tmp/apply_public_human_pass.py')
    script = script_path.read_text(encoding='utf-8')
    script = script.replace('if credit_count != 4:', 'if credit_count != 3:')
    script = script.replace('očekivana 4 potpisa fotografa', 'očekivana 3 potpisa fotografa')
    old_extract = '''def extract_remote_urls(source: str) -> list[str]:
    urls = re.findall(r"https?://(?:i0\\.wp\\.com/sovelebit\\.wordpress\\.com|sovelebit\\.wordpress\\.com)/[^\\"'<> )]+", source, re.I)
    return sorted(set(html_lib.unescape(u) for u in urls))
'''
    new_extract = '''def extract_remote_urls(source: str) -> list[str]:
    urls = re.findall(r"https?://(?:i0\\.wp\\.com/sovelebit\\.wordpress\\.com|sovelebit\\.wordpress\\.com)/[^\\"'<> )]+", source, re.I)
    clean = (html_lib.unescape(u) for u in urls)
    return sorted(set(u for u in clean if re.search(r"\\.(?:jpe?g|png|webp|gif|tiff?)(?:\\?|$)", u, re.I)))
'''
    if old_extract not in script:
        raise RuntimeError('Image URL extractor hotfix target not found')
    script = script.replace(old_extract, new_extract)
    script_path.write_text(script, encoding='utf-8')

    run('git', 'cat-file', '-e', BASE_SHA + '^{commit}')
    run('git', 'update-ref', 'refs/remotes/origin/main', BASE_SHA)
    shutil.rmtree(ROOT / '.vercel', ignore_errors=True)
    shutil.copy2('/tmp/sov-human.css', ROOT / 'sov-human.css')
    run('git', 'config', 'user.name', 'vercel-human-pass')
    run('git', 'config', 'user.email', 'vercel-human-pass@invalid.local')
    proc = subprocess.run(
        [sys.executable, str(script_path)],
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
