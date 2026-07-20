#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
path=ROOT/'update.json'
data=json.loads(path.read_text(encoding='utf-8'))
data.update({
  'version':'6.1.45ax',
  'versionName':'v6.1.45ax-gmail-trips-status-visual',
  'build':'sov-web-build-v6.1.45ax-gmail-trips-status-visual',
  'createdAt':'2026-07-20T23:15:00+02:00',
  'cacheBust':'6145ax-gmail-trips-status-visual',
  'base':'sov-web-build-v6.1.45aw-trips-force-refresh',
  'requiresSql':True,
  'sqlFiles':[
    'sql/sov_release_v6145ax.sql',
    'sql/sov_trip_assets_cleanup_safe_v6145ax.sql',
    'sql/sov_ecosystem_manifest_v6145ax.sql',
  ],
  'releaseType':'stability-and-ui-cleanup',
  'changedFiles':[
    'system-status.html',
    'assets/sov-system-status-v6145ax.js',
    'assets/sov-client-logger.js',
    'izleti-cloud.html',
    'assets/sov-trips-cloud.js',
    'zapisnici-native.html',
    'zapisnici-najave.html',
    'assets/zapisnici-native-v6144e.js',
    'assets/zapisnici-najave-v6143a.js',
    'dashboard.html',
    'tools/visual_layout_audit.mjs',
    'tools/fix_v6145ax.py',
    'tools/finalize_v6145ax_manifest.py',
    '.github/workflows/pre-release-audit.yml',
    'sql/sov_release_v6145ax.sql',
    'sql/sov_trip_assets_cleanup_safe_v6145ax.sql',
    'sql/sov_ecosystem_manifest_v6145ax.sql',
    'VERSION.txt',
    'BUILD_VERSION.txt',
    'assets/sov-version.js',
    'README.md',
    'update.json',
  ],
  'notes':'Automatic Trips refresh and effective past-trip completion, hourly Gmail minutes sync, factual System Status v3, safe Android asset cleanup compatibility, quieter telemetry and professional Croatian UI copy.'
})
rendered=json.dumps(data,ensure_ascii=False,indent=2)+'\n'
if path.read_text(encoding='utf-8')!=rendered:
  path.write_text(rendered,encoding='utf-8')
  print('update.json finalized')
else:
  print('update.json already final')
