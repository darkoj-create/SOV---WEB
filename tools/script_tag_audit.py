#!/usr/bin/env python3
"""Fail when an external <script src> tag contains inline source code.

Browsers ignore inline contents of script tags that also have src, so a missing
closing tag can silently swallow an entire fix without producing a syntax error.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKIP = {'.git', 'node_modules', '.vercel', 'dist', 'build', 'coverage'}
PATTERN = re.compile(
    r'<script\b(?=[^>]*\bsrc\s*=)[^>]*>(.*?)</script\s*>',
    re.IGNORECASE | re.DOTALL,
)

failures: list[str] = []
for path in ROOT.rglob('*.htm*'):
    if any(part in SKIP for part in path.parts):
        continue
    text = path.read_text(encoding='utf-8', errors='replace')
    for match in PATTERN.finditer(text):
        body = match.group(1)
        if not body.strip():
            continue
        line = text.count('\n', 0, match.start()) + 1
        preview = ' '.join(body.strip().split())[:180]
        failures.append(f'{path.relative_to(ROOT)}:{line}: external script tag contains inline code: {preview}')

if failures:
    print('MALFORMED EXTERNAL SCRIPT TAGS')
    for failure in failures:
        print('- ' + failure)
    sys.exit(1)

print('External script tags are clean.')
