#!/usr/bin/env python3
"""Apply the v6.1.45aw Trips force-refresh hotfix deterministically."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

changed_files: list[str] = []

# Data layer: a forced read must start a distinct request even while an older
# request is still running. A generation guard prevents that older response
# from overwriting the fresh cache after it eventually resolves.
cloud_path = ROOT / 'assets' / 'sov-trips-cloud.js'
cloud = cloud_path.read_text(encoding='utf-8')
original = cloud
cloud = cloud.replace(
    "  const CACHE_KEY='sov_trips_cloud_cache_v6_1_25';\n  const LEGACY_CACHE_KEYS=['sov_trips_cloud_cache_v5_56'];",
    "  const CACHE_KEY='sov_trips_cloud_cache_v6_1_45aw';\n  const LEGACY_CACHE_KEYS=['sov_trips_cloud_cache_v6_1_25','sov_trips_cloud_cache_v5_56'];",
    1,
)
if 'let listTripsGeneration=0;' not in cloud:
    cloud = cloud.replace(
        '  let listTripsInFlight=null;',
        '  let listTripsInFlight=null;\n  let listTripsGeneration=0;',
        1,
    )

old_start = """  async function listTrips(){
    if(listTripsInFlight) return listTripsInFlight;
    listTripsInFlight=(async()=>{
"""
new_start = """  async function listTrips(options={}){
    const force=!!(options&&options.force);
    if(listTripsInFlight && !force) return listTripsInFlight;
    const generation=++listTripsGeneration;
    const saveFresh=rows=>{if(generation===listTripsGeneration) saveCache(rows);};
    const request=(async()=>{
"""
if new_start not in cloud:
    if old_start not in cloud:
        raise SystemExit('Trips listTrips start marker not found.')
    cloud = cloud.replace(old_start, new_start, 1)

start = cloud.index(new_start)
body_start = start + len(new_start)
old_end = """    })();
    try{return await listTripsInFlight;}
    finally{listTripsInFlight=null;}
  }
"""
new_end = """    })();
    listTripsInFlight=request;
    try{return await request;}
    finally{if(listTripsInFlight===request) listTripsInFlight=null;}
  }
"""
end = cloud.index(old_end, body_start) if old_end in cloud[body_start:] else -1
if end >= 0:
    body = cloud[body_start:end]
    body = body.replace('saveCache(rows);', 'saveFresh(rows);')
    body = body.replace('saveCache(res.data||[]);', 'saveFresh(res.data||[]);')
    cloud = cloud[:body_start] + body + cloud[end:].replace(old_end, new_end, 1)
elif new_end not in cloud[body_start:]:
    raise SystemExit('Trips listTrips end marker not found.')

required_cloud_markers = [
    'async function listTrips(options={})',
    'const generation=++listTripsGeneration;',
    'if(listTripsInFlight && !force)',
    'const saveFresh=rows=>{if(generation===listTripsGeneration) saveCache(rows);};',
    'if(listTripsInFlight===request) listTripsInFlight=null',
]
for marker in required_cloud_markers:
    if marker not in cloud:
        raise SystemExit(f'Missing repaired data-layer marker: {marker}')
if cloud != original:
    cloud_path.write_text(cloud, encoding='utf-8')
    changed_files.append(str(cloud_path.relative_to(ROOT)))

# Page layer: pass the force flag into the data layer and cache-bust the hotfix.
page_path = ROOT / 'izleti-cloud.html'
page = page_path.read_text(encoding='utf-8')
original = page
page = page.replace(
    'window.SOVTripsCloud.listTrips(),\n      new Promise',
    'window.SOVTripsCloud.listTrips({force:!!opts.force}),\n      new Promise',
    1,
)
page = re.sub(
    r'assets/sov-trips-cloud\.js\?v=[^"\']+',
    'assets/sov-trips-cloud.js?v=6.1.45aw',
    page,
    count=1,
)
if 'window.SOVTripsCloud.listTrips({force:!!opts.force})' not in page:
    raise SystemExit('Trips page does not pass force to data layer.')
if 'assets/sov-trips-cloud.js?v=6.1.45aw' not in page:
    raise SystemExit('Trips data-layer cache bust was not updated.')
if page != original:
    page_path.write_text(page, encoding='utf-8')
    changed_files.append(str(page_path.relative_to(ROOT)))

# Release contract.
version_file = ROOT / 'VERSION.txt'
if version_file.read_text(encoding='utf-8').strip() != '6.1.45aw':
    version_file.write_text('6.1.45aw\n', encoding='utf-8')
    changed_files.append('VERSION.txt')
build_file = ROOT / 'BUILD_VERSION.txt'
if build_file.read_text(encoding='utf-8').strip() != 'sov-web-build-v6.1.45aw-trips-force-refresh':
    build_file.write_text('sov-web-build-v6.1.45aw-trips-force-refresh\n', encoding='utf-8')
    changed_files.append('BUILD_VERSION.txt')

version_path = ROOT / 'assets' / 'sov-version.js'
version = version_path.read_text(encoding='utf-8')
new_version = re.sub(r"const FALLBACK_VERSION='[^']+';", "const FALLBACK_VERSION='6.1.45aw';", version, count=1)
new_version = re.sub(r"const FALLBACK_CACHE='[^']+';", "const FALLBACK_CACHE='6145aw-trips-force-refresh';", new_version, count=1)
new_version = re.sub(r"const FALLBACK_BUILD='[^']+';", "const FALLBACK_BUILD='sov-web-build-v6.1.45aw-trips-force-refresh';", new_version, count=1)
new_version = re.sub(r"const FALLBACK_NAME='[^']+';", "const FALLBACK_NAME='v6.1.45aw-trips-force-refresh';", new_version, count=1)
if new_version != version:
    version_path.write_text(new_version, encoding='utf-8')
    changed_files.append(str(version_path.relative_to(ROOT)))

manifest_path = ROOT / 'update.json'
manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
new_manifest = dict(manifest)
new_manifest.update({
    'version': '6.1.45aw',
    'versionName': 'v6.1.45aw-trips-force-refresh',
    'build': 'sov-web-build-v6.1.45aw-trips-force-refresh',
    'createdAt': '2026-07-20T22:20:00+02:00',
    'cacheBust': '6145aw-trips-force-refresh',
    'base': 'sov-web-build-v6.1.45av-pre-release-audit',
    'requiresSql': False,
    'sqlFiles': [],
    'releaseType': 'trips-force-refresh-hotfix',
    'changedFiles': [
        'assets/sov-trips-cloud.js',
        'izleti-cloud.html',
        'tools/fix_trips_force_refresh.py',
        'tools/trips_inflight_refresh_test.mjs',
        '.github/workflows/pre-release-audit.yml',
        'VERSION.txt',
        'BUILD_VERSION.txt',
        'assets/sov-version.js',
        'README.md',
        'update.json',
    ],
    'notes': 'True Trips force refresh now starts a separate database request even while an older request is still in flight. A generation guard prevents the older response from overwriting the fresh cache. The production page forwards force=true into the data layer and uses a new cache-busted asset URL.',
})
rendered_manifest = json.dumps(new_manifest, ensure_ascii=False, indent=2) + '\n'
if manifest_path.read_text(encoding='utf-8') != rendered_manifest:
    manifest_path.write_text(rendered_manifest, encoding='utf-8')
    changed_files.append('update.json')

readme_path = ROOT / 'README.md'
readme = readme_path.read_text(encoding='utf-8')
new_readme = re.sub(r'^# SOV web build v[^\n]+', '# SOV web build v6.1.45aw', readme, count=1)
section = """
## v6.1.45aw — Trips force-refresh hotfix

- `Osvježi` i mobilno povlačenje sada pokreću potpuno novi Supabase zahtjev čak i ako prethodni poziv još traje.
- Stariji odgovor više ne može prepisati cache nakon što je stigao noviji rezultat.
- Dodan je izolirani regresijski test koji namjerno drži stari zahtjev otvorenim i potvrđuje da force refresh vraća novi rezultat.
- Nema SQL promjena.

"""
if '## v6.1.45aw — Trips force-refresh hotfix' not in new_readme:
    first_break = new_readme.find('\n') + 1
    new_readme = new_readme[:first_break] + section + new_readme[first_break:]
if new_readme != readme:
    readme_path.write_text(new_readme, encoding='utf-8')
    changed_files.append('README.md')

print('Trips force-refresh hotfix ready.')
for name in sorted(set(changed_files)):
    print(name)
