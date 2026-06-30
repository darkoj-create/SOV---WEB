/**
 * SOV Drawings Index WebApp v1.1.155
 *
 * Deploy as: Apps Script > Deploy > New deployment > Web app
 * Execute as: Me
 * Who has access: Anyone with the link
 *
 * Android endpoint: ?action=listDrawings
 *
 * Purpose:
 * - indexes ordinary SOV cave/object drawings only
 * - supports JPG/JPEG, PNG, TIF/TIFF, WEBP and selected PDF drawings
 * - ignores non-drawing documents as much as possible
 *
 * Important:
 * - We do NOT index TopoDroid exports, GPX/KML, ZIP/TDR, Word/Excel or ordinary zapisnici.
 * - PDF is included only when it clearly looks like a drawing, or when it is inside a folder named Nacrt/Nacrti.
 * - Images are treated as nacrti because the cleaned archive should contain only drawing images.
 *
 * Optional PDF text extraction:
 * - only runs for PDF drawings
 * - requires Apps Script Advanced Service: Drive API
 * - if Drive API is not enabled, script still works with filename/folder metadata
 */
const DRAWINGS_FOLDER_ID = '1vCPsPaznDOgwRMU_XVDiI4aUhu0o8yFB';
const INCLUDE_SUBFOLDERS = true;
const ENABLE_PDF_TEXT_EXTRACTION = true;
const MAX_PDF_TEXT_EXTRACTIONS_PER_RUN = 25;
const MAX_TEXT_CHARS_TO_PARSE = 12000;
const EXTRACT_CACHE_PREFIX = 'drawingmeta_v155_';

const DRAWING_EXTENSIONS = ['jpg', 'jpeg', 'png', 'tif', 'tiff', 'webp', 'pdf'];
const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'tif', 'tiff', 'webp'];
const BLOCKED_EXTENSIONS = ['doc', 'docx', 'xls', 'xlsx', 'csv', 'txt', 'rtf', 'gpx', 'kml', 'kmz', 'zip', 'tdr', 'th2'];

function doGet(e) {
  const params = (e && e.parameter) || {};
  const action = String(params.action || 'listDrawings');
  if (action !== 'listDrawings') {
    return jsonOutput({ ok: false, error: 'Unknown action: ' + action });
  }
  try {
    const folder = DriveApp.getFolderById(DRAWINGS_FOLDER_ID);
    const ctx = {
      startedAt: Date.now(),
      extractionCount: 0,
      extractText: String(params.extractText || '').toLowerCase() !== '0'
    };
    const drawings = [];
    collectDrawingFiles_(folder, drawings, folder.getName(), ctx);
    drawings.sort(function(a, b) {
      const pa = String(a.folderPath || '') + '/' + String(a.fileName || '');
      const pb = String(b.folderPath || '') + '/' + String(b.fileName || '');
      return pa.localeCompare(pb, 'hr');
    });
    return jsonOutput({
      ok: true,
      version: '1.1.155',
      updatedAt: new Date().toISOString(),
      folderId: DRAWINGS_FOLDER_ID,
      folderName: folder.getName(),
      count: drawings.length,
      supportedExtensions: DRAWING_EXTENSIONS,
      extractionCountThisRun: ctx.extractionCount,
      extractionMode: ENABLE_PDF_TEXT_EXTRACTION ? 'pdf_ocr_cached_for_pdf_drawings_only' : 'filename_only',
      drawings: drawings
    });
  } catch (err) {
    return jsonOutput({ ok: false, error: String(err && err.message ? err.message : err) });
  }
}

function collectDrawingFiles_(folder, out, path, ctx) {
  const files = folder.getFiles();
  while (files.hasNext()) {
    const file = files.next();
    const name = file.getName() || '';
    const ext = getExtension_(name);
    if (!shouldIndexAsDrawing_(file, path, ext)) continue;

    const id = file.getId();
    const mime = normalizeMimeType_(file.getMimeType() || '', ext);
    const modifiedTime = safeString_(function() { return file.getLastUpdated().toISOString(); });
    const parsed = getParsedDrawingMetadata_(file, path, modifiedTime, ctx, ext, mime);
    const drawingKind = classifyDrawingKind_(name, path, ext, mime);

    out.push({
      fileId: id,
      fileName: name,
      name: name,
      extension: ext,
      mimeType: mime,
      drawingKind: drawingKind,
      drawingType: drawingKind,
      sizeBytes: safeNumber_(function() { return file.getSize(); }),
      modifiedTime: modifiedTime,
      folderPath: path,
      webViewUrl: file.getUrl(),
      downloadUrl: 'https://drive.google.com/uc?export=download&id=' + encodeURIComponent(id),
      recordId: '',
      katastarId: '',
      objectName: parsed.detectedObjectName || inferObjectNameFromPath_(path) || inferObjectNameFromFilename_(name),
      detectedObjectName: parsed.detectedObjectName || inferObjectNameFromPath_(path) || inferObjectNameFromFilename_(name),
      detectedKatastarNumber: parsed.detectedKatastarNumber || inferKatastarFromFilename_(name),
      detectedCadastralNumber: parsed.detectedKatastarNumber || inferKatastarFromFilename_(name),
      detectedTile: parsed.detectedTile || '',
      detectedLocation: parsed.detectedLocation || inferRegionFromPath_(path),
      extractionStatus: parsed.extractionStatus || '',
      extractedTextPreview: parsed.extractedTextPreview || '',
      matchStatus: '',
      notes: parsed.notes || ''
    });
  }

  if (!INCLUDE_SUBFOLDERS) return;
  const folders = folder.getFolders();
  while (folders.hasNext()) {
    const child = folders.next();
    collectDrawingFiles_(child, out, path + '/' + child.getName(), ctx);
  }
}

function shouldIndexAsDrawing_(file, path, ext) {
  const name = file.getName() || '';
  const mime = file.getMimeType() || '';
  if (!ext) return false;
  if (BLOCKED_EXTENSIONS.indexOf(ext) !== -1) return false;
  if (IMAGE_EXTENSIONS.indexOf(ext) !== -1) return true;

  // PDF is included only if it clearly looks like a drawing or sits inside a Nacrt/Nacrti folder.
  if (ext === 'pdf' || mime === MimeType.PDF) {
    return looksLikeDrawingPdf_(name, path);
  }

  return false;
}

function looksLikeDrawingPdf_(name, path) {
  const combined = normalizeForMatch_(String(path || '') + '/' + String(name || ''));
  return /(^|[\s_\-\/])(nacrt|nacrti|tlocrt|presjek|profil|skica|plan|drawing|survey)([\s_\-.\/]|$)/i.test(combined);
}

function getParsedDrawingMetadata_(file, path, modifiedTime, ctx, ext, mime) {
  const id = file.getId();
  const filenameText = file.getName() + '\n' + path;
  const cache = PropertiesService.getScriptProperties();
  const cacheKey = EXTRACT_CACHE_PREFIX + id + '_' + String(modifiedTime).replace(/[^0-9A-Za-z]/g, '');
  const cached = safeString_(function() { return cache.getProperty(cacheKey); });
  if (cached) {
    try { return JSON.parse(cached); } catch (e) {}
  }

  let extractionStatus = IMAGE_EXTENSIONS.indexOf(ext) !== -1 ? 'image_filename_only' : 'filename_only';
  let text = filenameText;
  let notes = '';

  if ((ext === 'pdf' || mime === 'application/pdf') && ENABLE_PDF_TEXT_EXTRACTION && ctx.extractText && ctx.extractionCount < MAX_PDF_TEXT_EXTRACTIONS_PER_RUN) {
    const extracted = extractPdfText_(file);
    ctx.extractionCount++;
    if (extracted.ok && extracted.text) {
      text = filenameText + '\n' + extracted.text;
      extractionStatus = 'pdf_text_extracted';
    } else {
      extractionStatus = 'pdf_text_unavailable';
      notes = extracted.error || '';
    }
  }

  const parsed = parseDrawingText_(text);
  const result = {
    detectedObjectName: parsed.detectedObjectName || inferObjectNameFromPath_(path) || inferObjectNameFromFilename_(file.getName()),
    detectedKatastarNumber: parsed.detectedKatastarNumber || inferKatastarFromFilename_(file.getName()),
    detectedTile: parsed.detectedTile || '',
    detectedLocation: parsed.detectedLocation || inferRegionFromPath_(path),
    extractionStatus: extractionStatus,
    extractedTextPreview: cleanOneLine_(text).slice(0, 600),
    notes: notes
  };
  cache.setProperty(cacheKey, JSON.stringify(result));
  return result;
}

function extractPdfText_(file) {
  let tempId = '';
  try {
    if (typeof Drive === 'undefined' || !Drive.Files || !Drive.Files.copy) {
      return { ok: false, error: 'Drive advanced service nije uključen.' };
    }
    const resource = {
      title: 'SOV_OCR_TMP_' + file.getId(),
      mimeType: MimeType.GOOGLE_DOCS
    };
    const copied = Drive.Files.copy(resource, file.getId(), {
      ocr: true,
      ocrLanguage: 'hr'
    });
    tempId = copied.id;
    const doc = DocumentApp.openById(tempId);
    const text = doc.getBody().getText() || '';
    return { ok: true, text: text.slice(0, MAX_TEXT_CHARS_TO_PARSE) };
  } catch (err) {
    return { ok: false, error: String(err && err.message ? err.message : err) };
  } finally {
    if (tempId) {
      try { DriveApp.getFileById(tempId).setTrashed(true); } catch (e) {}
    }
  }
}

function classifyDrawingKind_(name, path, ext, mime) {
  const combined = normalizeForMatch_(String(path || '') + '/' + String(name || ''));
  if (ext === 'pdf' || mime === 'application/pdf') return 'nacrt_pdf';
  if (/skica/i.test(combined)) return 'nacrt_skica';
  if (/presjek|profil/i.test(combined)) return 'nacrt_presjek';
  if (/tlocrt|plan/i.test(combined)) return 'nacrt_tlocrt';
  if (IMAGE_EXTENSIONS.indexOf(ext) !== -1) return 'nacrt_slika';
  return 'nacrt';
}

function normalizeMimeType_(mime, ext) {
  const m = String(mime || '').toLowerCase();
  if (m && m !== 'application/octet-stream') return m;
  if (ext === 'jpg' || ext === 'jpeg') return 'image/jpeg';
  if (ext === 'png') return 'image/png';
  if (ext === 'tif' || ext === 'tiff') return 'image/tiff';
  if (ext === 'webp') return 'image/webp';
  if (ext === 'pdf') return 'application/pdf';
  return m || 'application/octet-stream';
}

function getExtension_(name) {
  const m = String(name || '').toLowerCase().match(/\.([a-z0-9]+)$/);
  return m ? m[1] : '';
}

function parseDrawingText_(rawText) {
  const text = normalizeHrText_(rawText || '');
  return {
    detectedKatastarNumber: firstMatch_(text, [
      /katastarsk(?:i|og)?\s*(?:broj|br\.?|oznaka)\s*[:#\-]?\s*([A-ZČĆŽŠĐ0-9][A-ZČĆŽŠĐ0-9\/[\]._\- ]{0,24})/i,
      /kat\.?\s*br\.?\s*[:#\-]?\s*([A-ZČĆŽŠĐ0-9][A-ZČĆŽŠĐ0-9\/[\]._\- ]{0,24})/i,
      /k\.\s*br\.?\s*[:#\-]?\s*([A-ZČĆŽŠĐ0-9][A-ZČĆŽŠĐ0-9\/[\]._\- ]{0,24})/i,
      /\b(\d{2}[\-_ ]?\d{3,4})\b/i
    ]),
    detectedTile: firstMatch_(text, [
      /(?:broj\s*)?plo[cč]ic[aeu]?\s*[:#\-]?\s*([A-ZČĆŽŠĐ0-9][A-ZČĆŽŠĐ0-9\/[\]._\- ]{0,24})/i,
      /(?:list|sekcija)\s*[:#\-]?\s*([A-ZČĆŽŠĐ0-9][A-ZČĆŽŠĐ0-9\/[\]._\- ]{0,24})/i
    ]),
    detectedLocation: firstMatch_(text, [
      /(?:lokacija|polo[zž]aj|mjesto|naselje|op[cć]ina|podru[cč]je)\s*[:#\-]?\s*([^\n\r]{2,70})/i
    ]),
    detectedObjectName: firstMatch_(text, [
      /(?:naziv\s*(?:objekta)?|ime\s*(?:objekta)?|objekt)\s*[:#\-]?\s*([^\n\r]{2,90})/i,
      /(?:jama|[sš]pilja|pe[cć]ina)\s+([A-ZČĆŽŠĐ][^\n\r]{2,80})/i
    ])
  };
}

function firstMatch_(text, patterns) {
  for (let i = 0; i < patterns.length; i++) {
    const m = text.match(patterns[i]);
    if (m && m[1]) return cleanCaptured_(m[1]);
  }
  return '';
}

function cleanCaptured_(value) {
  return String(value || '')
    .replace(/[|;]+.*$/g, '')
    .replace(/\s+/g, ' ')
    .replace(/^[:#\-\s]+|[:#\-\s]+$/g, '')
    .trim()
    .slice(0, 90);
}

function inferObjectNameFromPath_(path) {
  const parts = String(path || '').split('/').filter(Boolean);
  if (!parts.length) return '';
  let candidate = parts[parts.length - 1] || '';
  if (/^nacrti?$/i.test(normalizeForMatch_(candidate)) && parts.length >= 2) {
    candidate = parts[parts.length - 2] || '';
  }
  return cleanupObjectName_(candidate);
}

function inferRegionFromPath_(path) {
  const parts = String(path || '').split('/').filter(Boolean);
  if (parts.length >= 2) return parts[1] || '';
  return '';
}

function inferObjectNameFromFilename_(name) {
  return cleanupObjectName_(String(name || '')
    .replace(/\.[^.]+$/i, '')
    .replace(/^\d{2}[\-_ ]?\d{3,4}[\-_ ]*/i, ''));
}

function inferKatastarFromFilename_(name) {
  const m = String(name || '').match(/\b(\d{2}[\-_ ]?\d{3,4})\b/);
  return m && m[1] ? m[1].replace(/_/g, '-') : '';
}

function cleanupObjectName_(value) {
  return String(value || '')
    .replace(/\.[^.]+$/i, '')
    .replace(/[_\-–—]+/g, ' ')
    .replace(/\b(nacrt|nacrti|plan|profil|presjek|tlocrt|skica|pdf|jpg|jpeg|png|tif|tiff|webp|final|fin|novo|staro)\b/ig, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function normalizeForMatch_(value) {
  return String(value || '')
    .replace(/\u00a0/g, ' ')
    .replace(/[\t\r\n]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function normalizeHrText_(value) {
  return String(value || '')
    .replace(/\u00a0/g, ' ')
    .replace(/[ \t]+/g, ' ')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n');
}

function cleanOneLine_(value) {
  return String(value || '').replace(/\s+/g, ' ').trim();
}

function safeNumber_(fn) {
  try { return Number(fn()) || 0; } catch (e) { return 0; }
}

function safeString_(fn) {
  try { return String(fn() || ''); } catch (e) { return ''; }
}

function jsonOutput(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
