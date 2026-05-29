/**
 * SOV Drawings Index WebApp v2.0.2 FAST SEARCH
 *
 * Purpose:
 * - DO NOT return the full Drive archive to the Android app.
 * - Build a Drive-side JSON index once.
 * - Android app calls:
 *     ?action=stats
 *     ?action=searchDrawings&objectName=PT%206&limit=30
 *
 * Admin/manual actions:
 *     ?action=rebuildDrawingsIndex
 *     ?action=listDrawings&limit=200   // cached index preview only, no Drive rescan
 *
 * Deploy as:
 * - Apps Script > Deploy > Manage deployments > Edit existing Web app deployment
 * - Version: New version
 * - Execute as: Me
 * - Who has access: Anyone with the link
 *
 * After deploy, run once:
 *     DEPLOY_URL?action=rebuildDrawingsIndex
 */

const DRAWINGS_FOLDER_ID = '1vCPsPaznDOgwRMU_XVDiI4aUhu0o8yFB';

const INCLUDE_SUBFOLDERS = true;
const INDEX_FILE_NAME = '_sov_drawings_index_v2.json';

const SUPPORTED_IMAGE_EXT = ['jpg', 'jpeg', 'png', 'tif', 'tiff', 'webp'];
const SUPPORTED_PDF_EXT = ['pdf'];

// PDF is kept only when file/path clearly looks like a drawing.
const PDF_DRAWING_WORDS = [
  'nacrt', 'nacrti', 'tlocrt', 'presjek', 'profil', 'skica', 'plan',
  'sken', 'skenirani', 'digitalizirani'
];

function doGet(e) {
  const p = (e && e.parameter) || {};
  const rawAction = String(p.action || 'stats').trim();
  const action = normalizeAction_(rawAction);

  try {
    if (action === 'debug') return debug_(p, rawAction, action);
    if (action === 'rebuildDrawingsIndex') return rebuildDrawingsIndex_();
    if (action === 'stats') return stats_();
    if (action === 'listDrawings') return listDrawings_(p);
    if (action === 'searchDrawings') return searchDrawings_(p);

    return json_({
      ok: false,
      error: 'Unknown action: ' + rawAction,
      normalizedAction: action,
      supportedActions: ['debug', 'stats', 'rebuildDrawingsIndex', 'listDrawings', 'searchDrawings'],
      version: '2.0.2-fast-search'
    });
  } catch (err) {
    return json_({
      ok: false,
      error: String(err && err.message ? err.message : err),
      action: rawAction,
      normalizedAction: action,
      version: '2.0.2-fast-search'
    });
  }
}

function doPost(e) {
  return doGet(e);
}

function normalizeAction_(action) {
  const a = String(action || '').trim();
  const compact = a.toLowerCase().replace(/[^a-z0-9]/g, '');

  if (
    compact === 'searchdrawings' ||
    compact === 'searchdrawing' ||
    compact === 'seasrchdrawings' ||
    compact === 'serachdrawings' ||
    compact === 'finddrawings' ||
    compact === 'finddrawing'
  ) return 'searchDrawings';

  if (compact === 'listdrawings' || compact === 'listdrawing' || compact === 'list') return 'listDrawings';
  if (compact === 'stats' || compact === 'stat' || compact === 'status') return 'stats';
  if (
    compact === 'rebuilddrawingsindex' ||
    compact === 'rebuildindex' ||
    compact === 'rebuilddrawings' ||
    compact === 'buildindex'
  ) return 'rebuildDrawingsIndex';
  if (compact === 'debug' || compact === 'test') return 'debug';

  return a;
}

/* ===================== ACTIONS ===================== */

function debug_(p, rawAction, action) {
  const root = DriveApp.getFolderById(DRAWINGS_FOLDER_ID);
  const idx = loadIndex_();
  return json_({
    ok: true,
    version: '2.0.2-fast-search',
    marker: 'SOV_NACRTI_FAST_SEARCH_DEPLOYED',
    rawAction: rawAction,
    normalizedAction: action,
    folderId: DRAWINGS_FOLDER_ID,
    folderName: safeString_(function() { return root.getName(); }),
    indexExists: !!idx,
    indexCount: idx ? Number(idx.count || (idx.drawings || []).length) : 0,
    indexBuiltAt: idx ? String(idx.indexBuiltAt || '') : '',
    time: new Date().toISOString()
  });
}

function rebuildDrawingsIndex_() {
  const root = DriveApp.getFolderById(DRAWINGS_FOLDER_ID);
  const rows = [];
  collectDrawings_(root, rows, root.getName());

  rows.sort(function(a, b) {
    return String(a.folderPath + '/' + a.fileName).localeCompare(String(b.folderPath + '/' + b.fileName), 'hr');
  });

  const payload = {
    ok: true,
    version: '2.0.2-fast-search',
    folderId: DRAWINGS_FOLDER_ID,
    folderName: root.getName(),
    indexBuiltAt: new Date().toISOString(),
    count: rows.length,
    drawings: rows
  };

  saveIndexFile_(root, JSON.stringify(payload));
  return json_({
    ok: true,
    version: payload.version,
    folderId: payload.folderId,
    folderName: payload.folderName,
    count: rows.length,
    indexBuiltAt: payload.indexBuiltAt
  });
}

function stats_() {
  const idx = loadIndex_();
  if (!idx) {
    return json_({
      ok: false,
      version: '2.0.2-fast-search',
      error: 'Index nacrta nije izgrađen. Pokreni ?action=rebuildDrawingsIndex.',
      count: 0,
      totalCount: 0,
      drawings: []
    });
  }

  const total = Number(idx.count || (idx.drawings || []).length || 0);
  return json_({
    ok: true,
    version: idx.version || '2.0.2-fast-search',
    count: total,
    totalCount: total,
    indexBuiltAt: idx.indexBuiltAt || '',
    folderId: idx.folderId || DRAWINGS_FOLDER_ID,
    folderName: idx.folderName || ''
  });
}

function listDrawings_(p) {
  const idx = loadIndex_();
  if (!idx) {
    return json_({
      ok: false,
      version: '2.0.2-fast-search',
      error: 'Index nacrta nije izgrađen. Pokreni ?action=rebuildDrawingsIndex.',
      count: 0,
      totalCount: 0,
      drawings: []
    });
  }

  const total = Number(idx.count || (idx.drawings || []).length || 0);
  const limit = clampInt_(p.limit, 200, 1, 2000);
  const offset = clampInt_(p.offset, 0, 0, Math.max(total, 0));
  const drawings = (idx.drawings || []).slice(offset, offset + limit);

  return json_({
    ok: true,
    version: idx.version || '2.0.2-fast-search',
    count: drawings.length,
    totalCount: total,
    offset: offset,
    limit: limit,
    indexBuiltAt: idx.indexBuiltAt || '',
    drawings: drawings
  });
}

function searchDrawings_(p) {
  const idx = loadIndex_();
  if (!idx) {
    return json_({
      ok: false,
      version: '2.0.2-fast-search',
      error: 'Index nacrta nije izgrađen. Pokreni ?action=rebuildDrawingsIndex.',
      count: 0,
      totalCount: 0,
      drawings: []
    });
  }

  const objectName = String(p.objectName || p.object || p.q || p.query || '').trim();
  const plate = String(p.plate || p.tile || p.katastar || p.cadastral || '').trim();
  const limit = clampInt_(p.limit, 30, 1, 80);

  const total = Number(idx.count || (idx.drawings || []).length || 0);
  if (!objectName && !plate) {
    return json_({
      ok: true,
      version: idx.version || '2.0.2-fast-search',
      count: 0,
      totalCount: total,
      indexBuiltAt: idx.indexBuiltAt || '',
      drawings: [],
      query: { objectName: objectName, plate: plate }
    });
  }

  const queryNorm = normalize_(objectName);
  const queryCompact = compact_(objectName);
  const queryTokens = tokens_(objectName);
  const plateCompact = compact_(plate);

  const scored = [];

  (idx.drawings || []).forEach(function(d) {
    const hay = [
      d.objectName,
      d.detectedObjectName,
      d.fileName,
      d.name,
      d.folderPath,
      d.region,
      d.plate,
      d.detectedPlate
    ].join(' ');

    const hayNorm = normalize_(hay);
    const hayCompact = compact_(hay);
    const hayTokens = tokens_(hay);

    let score = 0;

    if (queryNorm && hayNorm === queryNorm) score = Math.max(score, 1.0);
    if (queryNorm && hayNorm.indexOf(queryNorm) !== -1) score = Math.max(score, 0.94);
    if (queryCompact && hayCompact.indexOf(queryCompact) !== -1) score = Math.max(score, 0.90);
    if (plateCompact && hayCompact.indexOf(plateCompact) !== -1) score = Math.max(score, 0.88);

    if (queryTokens.length) {
      const overlapCount = queryTokens.filter(function(t) { return hayTokens.indexOf(t) !== -1; }).length;
      const overlap = overlapCount / queryTokens.length;
      if (overlap >= 0.5) score = Math.max(score, 0.55 + overlap * 0.28);
    }

    // Special tolerance: PT 6 / PT6 / PT-6 / PT_6
    const compactObj = compact_(objectName);
    if (compactObj && compactObj.length >= 2 && hayCompact.indexOf(compactObj) !== -1) {
      score = Math.max(score, 0.91);
    }

    if (score > 0) scored.push({ score: score, drawing: d });
  });

  scored.sort(function(a, b) {
    return b.score - a.score || String(a.drawing.fileName).localeCompare(String(b.drawing.fileName), 'hr');
  });

  const drawings = scored.slice(0, limit).map(function(x) {
    const d = clone_(x.drawing);
    d.matchStatus = x.score >= 0.86 ? 'verified' : 'possible';
    d.notes = cleanOneLine_((d.notes || '') + ((d.notes || '') ? ' | ' : '') + 'serverScore=' + x.score.toFixed(2));
    return d;
  });

  return json_({
    ok: true,
    version: idx.version || '2.0.2-fast-search',
    count: drawings.length,
    totalCount: total,
    indexBuiltAt: idx.indexBuiltAt || '',
    query: {
      objectName: objectName,
      plate: plate,
      normalized: queryNorm,
      compact: queryCompact
    },
    drawings: drawings
  });
}

/* ===================== INDEX BUILDING ===================== */

function collectDrawings_(folder, out, path) {
  const files = folder.getFiles();

  while (files.hasNext()) {
    const file = files.next();
    const name = file.getName() || '';

    // Do not index the index itself.
    if (name === INDEX_FILE_NAME) continue;

    const ext = extension_(name);
    const lowerPath = (path + '/' + name).toLowerCase();

    const isImage = SUPPORTED_IMAGE_EXT.indexOf(ext) !== -1;
    const isPdfDrawing = SUPPORTED_PDF_EXT.indexOf(ext) !== -1 &&
      PDF_DRAWING_WORDS.some(function(w) { return lowerPath.indexOf(w) !== -1; });

    if (!isImage && !isPdfDrawing) continue;

    const id = file.getId();
    const inferred = inferRegionObjectFromPath_(path, name);
    const plate = inferPlate_(name + ' ' + path);

    out.push({
      fileId: id,
      id: id,
      fileName: name,
      name: name,
      extension: ext,
      mimeType: mimeForExt_(ext, file.getMimeType() || ''),
      sizeBytes: safeNumber_(function() { return file.getSize(); }),
      modifiedTime: safeString_(function() { return file.getLastUpdated().toISOString(); }),
      folderPath: path,
      region: inferred.region,
      objectName: inferred.objectName,
      detectedObjectName: inferred.objectName,
      plate: plate,
      detectedPlate: plate,
      webViewUrl: file.getUrl(),
      downloadUrl: 'https://drive.google.com/uc?export=download&id=' + encodeURIComponent(id),
      matchStatus: '',
      notes: isPdfDrawing ? 'PDF nacrt' : drawingNoteForExt_(ext)
    });
  }

  if (!INCLUDE_SUBFOLDERS) return;

  const folders = folder.getFolders();
  while (folders.hasNext()) {
    const child = folders.next();
    collectDrawings_(child, out, path + '/' + child.getName());
  }
}

function inferRegionObjectFromPath_(path, fileName) {
  const parts = String(path || '').split('/').filter(Boolean);

  if (parts.length >= 2) {
    let objectName = parts[parts.length - 1];
    let region = parts[parts.length - 2];

    // If file is inside "Nacrt/Nacrti" subfolder, object is one level up.
    if (/^nacrti?$|^nacrt$|^drawings?$/i.test(objectName) && parts.length >= 3) {
      objectName = parts[parts.length - 2];
      region = parts[parts.length - 3];
    }

    return {
      region: region,
      objectName: cleanObjectName_(objectName)
    };
  }

  // Flat folder fallback: infer from filename.
  return {
    region: parts.length ? parts[0] : '',
    objectName: cleanObjectName_(inferObjectNameFromFilename_(fileName))
  };
}

function inferObjectNameFromFilename_(name) {
  return String(name || '')
    .replace(/\.[^.]+$/g, '')
    .replace(/\b\d{1,3}[-_ ]\d{1,4}\b/g, ' ')
    .replace(/\b05[-_ ]?\d{1,4}\b/ig, ' ')
    .replace(/\b0?29[-_ ]?\d{1,4}\b/ig, ' ')
    .replace(/\b(nacrt|nacrti|skenirani|sken|digitalizirani|strana|page|jpg|jpeg|png|tif|tiff|webp|pdf|final|fin|radni|uređeni|uredjeni|staro|novo|master)\b/ig, ' ')
    .replace(/[_\-–—]+/g, ' ')
    .replace(/\s+\d+$/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

function cleanObjectName_(name) {
  return String(name || '')
    .replace(/\.[^.]+$/g, '')
    .replace(/[_]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function inferPlate_(value) {
  const s = String(value || '');
  const patterns = [
    /\b(\d{2,3}[-_ ]\d{1,4})\b/,
    /\b(05[-_ ]?\d{1,4})\b/i,
    /\b(029[-_ ]?\d{1,4})\b/i
  ];

  for (let i = 0; i < patterns.length; i++) {
    const m = s.match(patterns[i]);
    if (m && m[1]) return m[1].replace(/[_ ]+/g, '-');
  }

  return '';
}

/* ===================== INDEX FILE ===================== */

function loadIndex_() {
  const root = DriveApp.getFolderById(DRAWINGS_FOLDER_ID);
  const files = root.getFilesByName(INDEX_FILE_NAME);
  if (!files.hasNext()) return null;

  const file = files.next();
  const text = file.getBlob().getDataAsString('UTF-8');
  return JSON.parse(text);
}

function saveIndexFile_(root, content) {
  const files = root.getFilesByName(INDEX_FILE_NAME);
  if (files.hasNext()) {
    const file = files.next();
    file.setContent(content);
    while (files.hasNext()) {
      try { files.next().setTrashed(true); } catch (e) {}
    }
  } else {
    root.createFile(INDEX_FILE_NAME, content, MimeType.PLAIN_TEXT);
  }
}

/* ===================== HELPERS ===================== */

function extension_(name) {
  const s = String(name || '');
  const i = s.lastIndexOf('.');
  return i === -1 ? '' : s.slice(i + 1).toLowerCase();
}

function mimeForExt_(ext, fallback) {
  if (ext === 'pdf') return 'application/pdf';
  if (ext === 'jpg' || ext === 'jpeg') return 'image/jpeg';
  if (ext === 'png') return 'image/png';
  if (ext === 'tif' || ext === 'tiff') return 'image/tiff';
  if (ext === 'webp') return 'image/webp';
  return fallback || 'application/octet-stream';
}

function drawingNoteForExt_(ext) {
  if (ext === 'tif' || ext === 'tiff') return 'TIFF nacrt';
  if (ext === 'pdf') return 'PDF nacrt';
  if (ext === 'png') return 'PNG nacrt';
  if (ext === 'webp') return 'WEBP nacrt';
  return 'slika nacrta';
}

function normalize_(v) {
  return String(v || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/gi, 'dj')
    .toLowerCase()
    .replace(/[_\-–—/.,;:()\[\]{}]+/g, ' ')
    .replace(/[^a-z0-9 ]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function compact_(v) {
  return normalize_(v).replace(/[^a-z0-9]+/g, '');
}

function tokens_(v) {
  const stop = [
    'nacrt', 'nacrti', 'sken', 'skenirani', 'digitalizirani',
    'jama', 'spilja', 'pecina', 'pdf', 'jpg', 'jpeg', 'png',
    'tif', 'tiff', 'webp', 'strana', 'page'
  ];
  return normalize_(v)
    .split(' ')
    .filter(function(t) { return t.length >= 2 && stop.indexOf(t) === -1; });
}

function clampInt_(value, def, min, max) {
  const n = Number(value);
  if (!Number.isFinite(n)) return def;
  return Math.max(min, Math.min(Math.floor(n), max));
}

function clone_(obj) {
  return JSON.parse(JSON.stringify(obj || {}));
}

function safeNumber_(fn) {
  try { return Number(fn()) || 0; } catch (e) { return 0; }
}

function safeString_(fn) {
  try { return String(fn() || ''); } catch (e) { return ''; }
}

function cleanOneLine_(value) {
  return String(value || '').replace(/\s+/g, ' ').trim();
}

function json_(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
