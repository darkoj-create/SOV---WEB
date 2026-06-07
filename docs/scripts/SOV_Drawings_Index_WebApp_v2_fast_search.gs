/**
 * SOV Drawings Index WebApp v2.0 FAST SEARCH
 *
 * Problem solved:
 * - Android must NOT request the full Drive drawing archive when there are 1000+ objects.
 * - Build a Drive-side JSON index once, then app calls searchDrawings per object.
 *
 * Deploy as Web app:
 * - Execute as: Me
 * - Who has access: Anyone with the link
 *
 * Required app actions:
 *   ?action=stats
 *   ?action=searchDrawings&objectName=PT%206&limit=30
 *
 * Admin/manual actions:
 *   ?action=rebuildDrawingsIndex
 *   ?action=listDrawings   (reads cached index, does NOT rescan Drive)
 */
const DRAWINGS_FOLDER_ID = '1vCPsPaznDOgwRMU_XVDiI4aUhu0o8yFB';
const INCLUDE_SUBFOLDERS = true;
const INDEX_FILE_NAME = '_sov_drawings_index_v2.json';
const SUPPORTED_IMAGE_EXT = ['jpg', 'jpeg', 'png', 'tif', 'tiff', 'webp'];
const SUPPORTED_PDF_EXT = ['pdf'];
const PDF_DRAWING_WORDS = ['nacrt', 'nacrti', 'tlocrt', 'presjek', 'profil', 'skica', 'plan'];

function doGet(e) {
  const p = (e && e.parameter) || {};
  const action = String(p.action || 'searchDrawings');
  try {
    if (action === 'rebuildDrawingsIndex' || action === 'rebuildIndex') return rebuildDrawingsIndex_();
    if (action === 'stats') return stats_();
    if (action === 'listDrawings') return listDrawings_(p);
    if (action === 'searchDrawings') return searchDrawings_(p);
    return json_({ ok: false, error: 'Unknown action: ' + action });
  } catch (err) {
    return json_({ ok: false, error: String(err && err.message ? err.message : err) });
  }
}

function rebuildDrawingsIndex_() {
  const root = DriveApp.getFolderById(DRAWINGS_FOLDER_ID);
  const rows = [];
  collectDrawings_(root, rows, root.getName());
  rows.sort((a, b) => String(a.folderPath + '/' + a.fileName).localeCompare(String(b.folderPath + '/' + b.fileName), 'hr'));
  const payload = {
    ok: true,
    version: '2.0-fast-search',
    folderId: DRAWINGS_FOLDER_ID,
    folderName: root.getName(),
    indexBuiltAt: new Date().toISOString(),
    count: rows.length,
    drawings: rows
  };
  saveIndexFile_(root, JSON.stringify(payload));
  return json_({ ok: true, version: payload.version, folderId: payload.folderId, count: rows.length, indexBuiltAt: payload.indexBuiltAt });
}

function stats_() {
  const idx = loadIndex_();
  if (!idx) return json_({ ok: false, error: 'Index nacrta nije izgrađen. Pokreni ?action=rebuildDrawingsIndex.' });
  return json_({ ok: true, version: idx.version || '2.0-fast-search', count: Number(idx.count || (idx.drawings || []).length), totalCount: Number(idx.count || (idx.drawings || []).length), indexBuiltAt: idx.indexBuiltAt || '' });
}

function listDrawings_(p) {
  const idx = loadIndex_();
  if (!idx) return json_({ ok: false, error: 'Index nacrta nije izgrađen. Pokreni ?action=rebuildDrawingsIndex.', drawings: [] });
  const limit = Math.max(1, Math.min(Number(p.limit || 200), 2000));
  const drawings = (idx.drawings || []).slice(0, limit);
  return json_({ ok: true, version: idx.version || '2.0-fast-search', count: drawings.length, totalCount: Number(idx.count || (idx.drawings || []).length), indexBuiltAt: idx.indexBuiltAt || '', drawings });
}

function searchDrawings_(p) {
  const idx = loadIndex_();
  if (!idx) return json_({ ok: false, error: 'Index nacrta nije izgrađen. Pokreni ?action=rebuildDrawingsIndex.', drawings: [] });
  const objectName = String(p.objectName || p.q || '').trim();
  const plate = String(p.plate || '').trim();
  const limit = Math.max(1, Math.min(Number(p.limit || 30), 80));
  if (!objectName && !plate) return json_({ ok: true, drawings: [], count: 0, totalCount: Number(idx.count || (idx.drawings || []).length), indexBuiltAt: idx.indexBuiltAt || '' });

  const queryNorm = normalize_(objectName);
  const queryCompact = compact_(objectName);
  const queryTokens = tokens_(objectName);
  const plateCompact = compact_(plate);

  const scored = [];
  (idx.drawings || []).forEach(d => {
    const hay = [d.objectName, d.detectedObjectName, d.fileName, d.folderPath].join(' ');
    const hayNorm = normalize_(hay);
    const hayCompact = compact_(hay);
    const hayTokens = tokens_(hay);
    let score = 0;
    if (queryNorm && hayNorm === queryNorm) score = Math.max(score, 1.0);
    if (queryNorm && hayNorm.indexOf(queryNorm) !== -1) score = Math.max(score, 0.92);
    if (queryCompact && hayCompact.indexOf(queryCompact) !== -1) score = Math.max(score, 0.88);
    if (plateCompact && hayCompact.indexOf(plateCompact) !== -1) score = Math.max(score, 0.86);
    if (queryTokens.length) {
      const overlap = queryTokens.filter(t => hayTokens.indexOf(t) !== -1).length / queryTokens.length;
      if (overlap >= 0.5) score = Math.max(score, 0.55 + overlap * 0.25);
    }
    if (score > 0) scored.push({ score, drawing: d });
  });
  scored.sort((a, b) => b.score - a.score || String(a.drawing.fileName).localeCompare(String(b.drawing.fileName), 'hr'));
  const drawings = scored.slice(0, limit).map(x => {
    const d = x.drawing;
    d.matchStatus = x.score >= 0.86 ? 'verified' : 'possible';
    d.notes = (d.notes || '') + (d.notes ? ' | ' : '') + 'serverScore=' + x.score.toFixed(2);
    return d;
  });
  return json_({ ok: true, version: idx.version || '2.0-fast-search', count: drawings.length, totalCount: Number(idx.count || (idx.drawings || []).length), indexBuiltAt: idx.indexBuiltAt || '', drawings });
}

function collectDrawings_(folder, out, path) {
  const files = folder.getFiles();
  while (files.hasNext()) {
    const file = files.next();
    const name = file.getName() || '';
    const ext = extension_(name);
    const lowerPath = (path + '/' + name).toLowerCase();
    const isImage = SUPPORTED_IMAGE_EXT.indexOf(ext) !== -1;
    const isPdfDrawing = SUPPORTED_PDF_EXT.indexOf(ext) !== -1 && PDF_DRAWING_WORDS.some(w => lowerPath.indexOf(w) !== -1);
    if (!isImage && !isPdfDrawing) continue;
    const id = file.getId();
    const inferred = inferRegionObjectFromPath_(path);
    out.push({
      fileId: id,
      fileName: name,
      name: name,
      mimeType: mimeForExt_(ext, file.getMimeType() || ''),
      sizeBytes: safeNumber_(() => file.getSize()),
      modifiedTime: safeString_(() => file.getLastUpdated().toISOString()),
      folderPath: path,
      region: inferred.region,
      objectName: inferred.objectName,
      detectedObjectName: inferred.objectName,
      webViewUrl: file.getUrl(),
      downloadUrl: 'https://drive.google.com/uc?export=download&id=' + encodeURIComponent(id),
      matchStatus: '',
      notes: isPdfDrawing ? 'PDF nacrt' : 'slika nacrta'
    });
  }
  if (!INCLUDE_SUBFOLDERS) return;
  const folders = folder.getFolders();
  while (folders.hasNext()) {
    const child = folders.next();
    collectDrawings_(child, out, path + '/' + child.getName());
  }
}

function inferRegionObjectFromPath_(path) {
  const parts = String(path || '').split('/').filter(Boolean);
  if (parts.length < 2) return { region: '', objectName: '' };
  let last = parts[parts.length - 1];
  let objectName = last;
  let region = parts.length >= 2 ? parts[parts.length - 2] : '';
  if (/^nacrti?$|^nacrt$/i.test(last) && parts.length >= 3) {
    objectName = parts[parts.length - 2];
    region = parts[parts.length - 3];
  }
  return { region, objectName };
}

function loadIndex_() {
  const root = DriveApp.getFolderById(DRAWINGS_FOLDER_ID);
  const files = root.getFilesByName(INDEX_FILE_NAME);
  if (!files.hasNext()) return null;
  const file = files.next();
  return JSON.parse(file.getBlob().getDataAsString('UTF-8'));
}

function saveIndexFile_(root, content) {
  const files = root.getFilesByName(INDEX_FILE_NAME);
  if (files.hasNext()) {
    const file = files.next();
    file.setContent(content);
    while (files.hasNext()) files.next().setTrashed(true);
  } else {
    root.createFile(INDEX_FILE_NAME, content, MimeType.PLAIN_TEXT);
  }
}

function extension_(name) { return String(name || '').split('.').pop().toLowerCase(); }
function mimeForExt_(ext, fallback) {
  if (ext === 'pdf') return 'application/pdf';
  if (ext === 'jpg' || ext === 'jpeg') return 'image/jpeg';
  if (ext === 'png') return 'image/png';
  if (ext === 'tif' || ext === 'tiff') return 'image/tiff';
  if (ext === 'webp') return 'image/webp';
  return fallback || 'application/octet-stream';
}
function normalize_(v) {
  return String(v || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/gi, 'dj').toLowerCase().replace(/[_\-–—/.,;:()\[\]{}]+/g, ' ').replace(/[^a-z0-9 ]+/g, ' ').replace(/\s+/g, ' ').trim();
}
function compact_(v) { return normalize_(v).replace(/[^a-z0-9]+/g, ''); }
function tokens_(v) { return normalize_(v).split(' ').filter(t => t.length >= 2 && ['nacrt','nacrti','jama','spilja','pdf','jpg','png','tif','tiff'].indexOf(t) === -1); }
function safeNumber_(fn) { try { return Number(fn()) || 0; } catch(e) { return 0; } }
function safeString_(fn) { try { return String(fn() || ''); } catch(e) { return ''; } }
function json_(obj) { return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON); }
