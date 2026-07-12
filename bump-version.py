#!/usr/bin/env python3
"""SOV web version bump helper.

Bumpa:
- ?v= parametar na assets/sov-version.js, assets/sov-client-logger.js i assets/pwa-register.js u svim HTML-ovima
- fallback verzije u assets/sov-version.js
- SW_VERSION u sw.js

Upotreba:
  python bump-version.py 6.1.46a
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

VERSION = sys.argv[1] if len(sys.argv) > 1 else "6.1.46a"
VERSION_NAME = f"v{VERSION}-pwa-step1"
BUILD = f"sov-web-build-v{VERSION}-pwa-step1"
CACHE = VERSION.replace('.', '').replace('-', '') + "-pwa-step1"
ROOT = Path.cwd()
SKIP_DIRS = {".git", "node_modules", "dist", "build", ".vercel"}

def skip(path: Path) -> bool:
    return any(part in SKIP_DIRS for part in path.parts)

def write_if_changed(path: Path, text: str, changed: list[Path]) -> None:
    raw = path.read_text(encoding="utf-8", errors="ignore") if path.exists() else ""
    if text != raw:
        path.write_text(text, encoding="utf-8")
        changed.append(path)

def main() -> int:
    changed: list[Path] = []
    for path in ROOT.rglob("*.html"):
        if skip(path):
            continue
        raw = path.read_text(encoding="utf-8", errors="ignore")
        text = re.sub(
            r'(assets/(?:sov-version|sov-client-logger|pwa-register)\.js)(?:\?v=[^"\']*)?',
            rf'\1?v={VERSION}',
            raw,
            flags=re.I,
        )
        write_if_changed(path, text, changed)

    sv = ROOT / "assets" / "sov-version.js"
    if sv.exists():
        raw = sv.read_text(encoding="utf-8", errors="ignore")
        text = raw
        text = re.sub(r"const\s+FALLBACK_VERSION\s*=\s*['\"][^'\"]+['\"]", f"const FALLBACK_VERSION='{VERSION}'", text)
        text = re.sub(r"const\s+FALLBACK_CACHE\s*=\s*['\"][^'\"]+['\"]", f"const FALLBACK_CACHE='{CACHE}'", text)
        text = re.sub(r"const\s+FALLBACK_BUILD\s*=\s*['\"][^'\"]+['\"]", f"const FALLBACK_BUILD='{BUILD}'", text)
        text = re.sub(r"const\s+FALLBACK_NAME\s*=\s*['\"][^'\"]+['\"]", f"const FALLBACK_NAME='{VERSION_NAME}'", text)
        write_if_changed(sv, text, changed)

    sw = ROOT / "sw.js"
    if sw.exists():
        raw = sw.read_text(encoding="utf-8", errors="ignore")
        text = re.sub(r"const\s+SW_VERSION\s*=\s*['\"][^'\"]+['\"]", f"const SW_VERSION = '{VERSION}-pwa-step1'", raw)
        write_if_changed(sw, text, changed)

    print(f"SOV bump-version: promijenjeno datoteka: {len(changed)}")
    for p in changed:
        print(" -", p.relative_to(ROOT))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
