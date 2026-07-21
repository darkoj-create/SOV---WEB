#!/usr/bin/env python3
"""SOV web helper: bump cache query version for versioned JS assets.

Usage:
  python bump-version.py 6.1.45ak

Radi samo tekstualne zamjene u statičkom web buildu:
- svim .html datotekama ažurira ?v= na sov-version.js i sov-client-logger.js referencama
- assets/sov-version.js ažurira FALLBACK_VERSION i izvedene build/cache nazive
- na kraju ispiše broj promijenjenih datoteka
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

TARGETS = ("sov-version.js", "sov-client-logger.js")


def update_html(text: str, version: str) -> str:
    out = text
    for asset in TARGETS:
        pattern = re.compile(rf"((?:assets/)?{re.escape(asset)})(?:\?v=[^\"'<>\s]+)?")
        out = pattern.sub(rf"\1?v={version}", out)
    return out


def update_version_js(text: str, version: str) -> str:
    safe = re.sub(r"[^0-9A-Za-z]+", "", version)
    build = f"sov-web-build-v{version}"
    name = f"v{version}"
    out = text
    replacements = {
        "FALLBACK_VERSION": version,
        "FALLBACK_CACHE": safe,
        "FALLBACK_BUILD": build,
        "FALLBACK_NAME": name,
    }
    for key, value in replacements.items():
        out = re.sub(rf"const\s+{key}\s*=\s*['\"][^'\"]*['\"]", f"const {key}='{value}'", out)
    return out


def write_if_changed(path: Path, text: str, changed: list[Path]) -> None:
    old = path.read_text(encoding="utf-8")
    if old != text:
        path.write_text(text, encoding="utf-8")
        changed.append(path)


def main() -> int:
    if len(sys.argv) != 2 or not sys.argv[1].strip():
        print("Usage: python bump-version.py <version>", file=sys.stderr)
        return 2
    version = sys.argv[1].strip()
    root = Path.cwd()
    changed: list[Path] = []

    for html in root.rglob("*.html"):
        if any(part in {"node_modules", ".git", "dist", "build"} for part in html.parts):
            continue
        write_if_changed(html, update_html(html.read_text(encoding="utf-8"), version), changed)

    version_js = root / "assets" / "sov-version.js"
    if version_js.exists():
        write_if_changed(version_js, update_version_js(version_js.read_text(encoding="utf-8"), version), changed)

    print(f"Promijenjeno datoteka: {len(changed)}")
    for path in changed:
        print(path.relative_to(root))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
