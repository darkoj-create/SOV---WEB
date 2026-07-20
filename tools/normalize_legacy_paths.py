#!/usr/bin/env python3
"""Normalize legacy nested paths and alias pages in the SOV static site.

This script is intentionally deterministic and idempotent. Run on an audit
branch, review the diff, then keep it as a maintenance tool for imported pages.
"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: Path) -> str:
    return path.read_text(encoding='utf-8', errors='replace')


def write_if_changed(path: Path, text: str) -> bool:
    old = read(path) if path.exists() else None
    if old == text:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8')
    print(path.relative_to(ROOT))
    return True


def redirect_page(title: str, destination: str, description: str) -> str:
    dest = destination.replace("'", "%27")
    return f'''<!DOCTYPE html>
<html lang="hr">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>{title}</title>
<meta name="robots" content="noindex,nofollow,noarchive"/>
<script>location.replace('{dest}')</script>
<style>
:root{{--bg:#050707;--text:#f4f7f6;--muted:#9cadad;--line:rgba(255,255,255,.12);--accent:#d7f66f}}
*{{box-sizing:border-box}}body{{margin:0;min-height:100vh;display:grid;place-items:center;background:linear-gradient(180deg,#020303,#071012 55%,#040606);color:var(--text);font-family:Inter,system-ui,sans-serif}}
main{{width:min(720px,calc(100% - 32px));border:1px solid var(--line);border-radius:28px;background:rgba(255,255,255,.045);padding:28px}}
h1{{margin:0 0 10px}}p{{color:var(--muted)}}a{{display:inline-flex;margin-top:12px;border-radius:999px;background:var(--accent);color:#111;text-decoration:none;font-weight:900;padding:12px 16px}}
</style>
</head>
<body><main><h1>{title}</h1><p>{description}</p><a href="{destination}">Otvori →</a></main></body>
</html>
'''


changed = 0

# Imported news pages live one directory below the root. Their generated
# relative asset paths were valid only before they were moved into /novosti/.
news_dir = ROOT / 'novosti'
if news_dir.exists():
    for path in sorted(news_dir.glob('*.html')):
        text = read(path)
        new = text
        for quote in ('"', "'"):
            new = new.replace(f'href={quote}assets/', f'href={quote}/assets/')
            new = new.replace(f'src={quote}assets/', f'src={quote}/assets/')
            new = new.replace(f'poster={quote}assets/', f'poster={quote}/assets/')
        # WordPress exports sometimes refer to a local emoji directory that was
        # never shipped. Preserve the visible emoji from alt instead of 404ing.
        new = re.sub(
            r'<img\b(?=[^>]*\bclass=["\'][^"\']*\bemoji\b[^"\']*["\'])(?=[^>]*\balt=["\']([^"\']*)["\'])[^>]*>',
            lambda m: m.group(1),
            new,
            flags=re.I,
        )
        if write_if_changed(path, new):
            changed += 1

# Shared version helper must fetch the root manifest even from nested pages.
version_path = ROOT / 'assets' / 'sov-version.js'
if version_path.exists():
    text = read(version_path)
    new = text.replace("fetch('update.json?cb='", "fetch('/update.json?cb='")
    if write_if_changed(version_path, new):
        changed += 1

# Old nested aliases should never carry duplicated full page bundles.
aliases = {
    'login/index.html': ('Prijava — SOV Velebit', '/login.html', 'Otvaram prijavu.'),
    'prijava/index.html': ('Prijava — SOV Velebit', '/login.html', 'Otvaram prijavu.'),
    'clanska-zona/index.html': ('Članska zona — SOV Velebit', '/login.html', 'Otvaram člansku prijavu.'),
    'status/index.html': ('Status sustava — SOV Velebit', '/system-status.html', 'Otvaram status sustava.'),
    'system-status/index.html': ('Status sustava — SOV Velebit', '/system-status.html', 'Otvaram status sustava.'),
    'sov-system-status/index.html': ('Status sustava — SOV Velebit', '/system-status.html', 'Otvaram status sustava.'),
    'baza.html': ('Karta — SOV Velebit', '/karta.html', 'Otvaram kartu i bazu objekata.'),
    'inventura.html': ('Inventura — SOV Velebit', '/oruzarstvo.html#inventory', 'Otvaram inventuru opreme.'),
    'pregled-zapisnika.html': ('Zapisnici — SOV Velebit', '/zapisnici-native.html', 'Otvaram arhivu zapisnika.'),
    'zapisnici-aktualni-2026.html': ('Zapisnici — SOV Velebit', '/zapisnici-native.html', 'Otvaram arhivu zapisnika.'),
    'zapisnici-arhiva-2017-2022.html': ('Zapisnici — SOV Velebit', '/zapisnici-native.html', 'Otvaram arhivu zapisnika.'),
    'zapisnici-cijela-arhiva.html': ('Zapisnici — SOV Velebit', '/zapisnici-native.html', 'Otvaram arhivu zapisnika.'),
}
for rel, (title, dest, description) in aliases.items():
    path = ROOT / rel
    if path.exists() and write_if_changed(path, redirect_page(title, dest, description)):
        changed += 1

# Duplicate style/script IDs are invalid HTML and break querySelector semantics.
armory_import = ROOT / 'oruzarstvo-import.html'
if armory_import.exists():
    text = read(armory_import)
    new = text.replace('<style id="v469-real-icon-system">', '<style id="v469-real-icon-system-style">', 1)
    if write_if_changed(armory_import, new):
        changed += 1

# CSS URLs are resolved relative to the CSS file. Imported rules that include
# "assets/brand" inside an /assets stylesheet accidentally request
# /assets/assets/brand/....
for path in (ROOT / 'assets').rglob('*.css'):
    text = read(path)
    new = text.replace('assets/brand/munizaba-hero.jpg', 'brand/munizaba-hero.jpg')
    if write_if_changed(path, new):
        changed += 1

# Speleoškola referenced four local WordPress export folders that were never
# included in the repository. Use the matching, stable WordPress media URLs
# already used by the corresponding article cards on the public home page.
school = ROOT / 'speleoskola.html'
if school.exists():
    text = read(school)
    replacements = {
        'novosti/Sve što je lijepo kratko traje, osim puta do Velebitaškog duha – Speleološki odsjek PDS Velebit_files/autorica-teksta-na-izlazu-iz-dvojame_paula-skelin.jpg':
            'https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2026/05/demonstracija-tehnickog-penjanja_gorana-peric.jpg?fit=1200%2C676&ssl=1',
        'novosti/Pa po užetu dol’ pa po užetu gor’! – Speleološki odsjek PDS Velebit_files/1f60a.svg':
            'https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2026/05/jutarnje-zagrijavanje_gorana-peric.jpg?fit=1200%2C676&ssl=1',
        'novosti/Spust u jamu – Speleološki odsjek PDS Velebit_files/grupno-crtanje_klara-krsticevic.jpg':
            'https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2026/04/vjezbanje-uzlova_jelena-babic.jpg?fit=1200%2C900&ssl=1',
        'novosti/Hej, haj Terihaj (i Gorsko) – Speleološki odsjek PDS Velebit_files/gorsko-zrcalo-na-stijeni_petra-jagodic.jpg':
            'https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2026/04/terihaj-uspon_ines-sasic.jpg?fit=1200%2C900&ssl=1',
    }
    new = text
    for old, replacement in replacements.items():
        new = new.replace(old, replacement)
    if write_if_changed(school, new):
        changed += 1

# This protected import page used auth.js without first loading supabase-js.
topodroid_import = ROOT / 'topodroid-import.html'
if topodroid_import.exists():
    text = read(topodroid_import)
    marker = '<script src="assets/supabase-config.js"></script><script src="assets/auth.js"></script>'
    replacement = '<script src="https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2"></script><script src="assets/supabase-config.js"></script><script src="assets/auth.js"></script>'
    new = text.replace(marker, replacement, 1)
    if write_if_changed(topodroid_import, new):
        changed += 1

# Vercel destinations may include URL fragments; the static file check must
# validate only the path component.
audit_tool = ROOT / 'tools' / 'pre_release_audit.py'
if audit_tool.exists():
    text = read(audit_tool)
    new = text.replace(
        'dest.lstrip("/").split("?", 1)[0]',
        'dest.lstrip("/").split("#", 1)[0].split("?", 1)[0]',
    )
    if write_if_changed(audit_tool, new):
        changed += 1

print(f'Normalized {changed} file(s).')
