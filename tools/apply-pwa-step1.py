#!/usr/bin/env python3
"""SOV PWA Korak 1 helper.

Pokreni iz root foldera SOV web repoa:
  python tools/apply-pwa-step1.py 6.1.46a

Skripta dodaje PWA tagove u <head> svih .html stranica i bumpa reference
na assets/pwa-register.js. Idempotentna je: postojeće PWA tagove prvo ukloni.
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

VERSION = sys.argv[1] if len(sys.argv) > 1 else "6.1.46a"
ROOT = Path.cwd()
SKIP_DIRS = {".git", "node_modules", "dist", "build", ".vercel"}
THEME = "#0b0f14"
PWA_BLOCK = f'''<link rel="manifest" href="/manifest.webmanifest">
<meta name="theme-color" content="{THEME}">
<link rel="apple-touch-icon" href="/assets/icons/sov-icon-192.png">
<script defer src="/assets/pwa-register.js?v={VERSION}"></script>'''

TAG_PATTERNS = [
    re.compile(r'\s*<link\s+[^>]*rel=["\']manifest["\'][^>]*>\s*', re.I),
    re.compile(r'\s*<meta\s+[^>]*name=["\']theme-color["\'][^>]*>\s*', re.I),
    re.compile(r'\s*<link\s+[^>]*rel=["\']apple-touch-icon["\'][^>]*>\s*', re.I),
    re.compile(r'\s*<script\s+[^>]*src=["\'][^"\']*assets/pwa-register\.js[^"\']*["\'][^>]*>\s*</script>\s*', re.I),
]

def should_skip(path: Path) -> bool:
    return any(part in SKIP_DIRS for part in path.parts)

def patch_html(path: Path) -> bool:
    raw = path.read_text(encoding="utf-8", errors="ignore")
    if "</head>" not in raw.lower():
        return False
    text = raw
    for pat in TAG_PATTERNS:
        text = pat.sub("\n", text)
    text = re.sub(
        r'(<script\s+[^>]*src=["\'][^"\']*assets/(?:sov-version|sov-client-logger)\.js)(?:\?v=[^"\']*)?(["\'][^>]*>\s*</script>)',
        rf'\1?v={VERSION}\2',
        text,
        flags=re.I,
    )
    text = re.sub(r'</head>', PWA_BLOCK + "\n</head>", text, count=1, flags=re.I)
    if text != raw:
        path.write_text(text, encoding="utf-8")
        return True
    return False

def main() -> int:
    changed = []
    for path in ROOT.rglob("*.html"):
        if should_skip(path):
            continue
        if patch_html(path):
            changed.append(path)
    print(f"SOV PWA: promijenjeno HTML datoteka: {len(changed)}")
    for p in changed:
        print(" -", p.relative_to(ROOT))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
