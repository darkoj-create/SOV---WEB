/**
 * SOV Drawings Index WebApp v1.1.0
 *
 * Deploy as: Apps Script > Deploy > New deployment > Web app
 * Execute as: Me
 * Who has access: Anyone with the link
 *
 * Android endpoint: ?action=listDrawings
 *
 * Better matching:
 * - reads PDF and image drawing file metadata
 * - optionally extracts text from PDF by converting a temporary Google Doc
 * - parses katastarski broj, broj pločice, lokacija/općina/mjesto and object name
 * - caches extraction in Script Properties so PDF OCR is not repeated every request
 *
 * IMPORTANT for OCR/text extraction:
 * Apps Script editor > Services (+) > add "Drive API" advanced service.
 * Also enable Drive API in the linked Google Cloud project if Google asks.
 * If Drive API is not enabled, script still works, but returns filename-only metadata.
 */
const DRAWINGS_FOLDER_ID = '1vCPsPaznDOgwRMU_XVDiI4aUhu0o8yFB';
const INCLUDE_SUBFOLDERS = true;
const ENABLE_PDF_TEXT_EXTRACTION = true;
const MAX_PDF_TEXT_EXTRACTIONS_PER_RUN = 25;
const MAX_TEXT_CHARS_TO_PARSE = 12000;
const EXTRACT_CACHE_PREFIX = 'drawingmeta_v110_';

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
    drawings.sort(function(a, b) { return String(a.fileName).localeCompare(String(b.fileName), 'hr'); });
    return jsonOutput({
      ok: true,
      version: '1.1.0',
      updatedAt: new Date().toISOString(),
      folderId: DRAWINGS_FOLDER_ID,
      folderName: folder.getName(),
      count: drawings.length,
      extractionCountThisRun: ctx.extractionCount,
      extractionMode: ENABLE_PDF_TEXT_EXTRACTION ? 'pdf_ocr_cached_images_filename' : 'filename_only_images',
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
    const mime = file.getMimeType() || '';
    const isPdf = mime === MimeType.PDF || /\.pdf$/i.test(name);
    const isImage = /^image\//i.test(mime) || /\.(jpe?g|png|tiff?|webp)$/i.test(name);
    if (!isPdf && !isImage) continue;
    const id = file.getId();
    const modifiedTime = safeString_(function() { return file.getLastUpdated().toISOString(); });
    const parsed = isPdf ? getParsedPdfMetadata_(file, path, modifiedTime, ctx) : getParsedImageMetadata_(file, path, modifiedTime);
    out.push({
      fileId: id,
      fileName: name,
      name: name,
      mimeType: mime || guessMimeFromName_(name),
      sizeBytes: safeNumber_(function() { return file.getSize(); }),
      modifiedTime: modifiedTime,
      folderPath: path,
      webViewUrl: file.getUrl(),
      downloadUrl: 'https://drive.google.com/uc?export=download&id=' + encodeURIComponent(id),
      recordId: '',
      katastarId: '',
      objectName: parsed.detectedObjectName || '',
      detectedObjectName: parsed.detectedObjectName || '',
      detectedKatastarNumber: parsed.detectedKatastarNumber || '',
      detectedCadastralNumber: parsed.detectedKatastarNumber || '',
      detectedTile: parsed.detectedTile || '',
      detectedLocation: parsed.detectedLocation || '',
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

function getParsedImageMetadata_(file, path, modifiedTime) {
  const text = file.getName() + '\n' + path;
  const parsed = parseDrawingText_(text);
  return {
    detectedObjectName: parsed.detectedObjectName || inferObjectNameFromFilename_(file.getName()),
    detectedKatastarNumber: parsed.detectedKatastarNumber || '',
    detectedTile: parsed.detectedTile || '',
    detectedLocation: parsed.detectedLocation || '',
    extractionStatus: 'image_filename_only',
    extractedTextPreview: cleanOneLine_(text).slice(0, 600),
    notes: 'image drawing'
  };
}

function guessMimeFromName_(name) {
  const lower = String(name || '').toLowerCase();
  if (/\.pdf$/.test(lower)) return 'application/pdf';
  if (/\.jpe?g$/.test(lower)) return 'image/jpeg';
  if (/\.png$/.test(lower)) return 'image/png';
  if (/\.tiff?$/.test(lower)) return 'image/tiff';
  if (/\.webp$/.test(lower)) return 'image/webp';
  return '';
}

function getParsedPdfMetadata_(file, path, modifiedTime, ctx) {
  const id = file.getId();
  const filenameText = file.getName() + '\n' + path;
  const cache = PropertiesService.getScriptProperties();
  const cacheKey = EXTRACT_CACHE_PREFIX + id + '_' + String(modifiedTime).replace(/[^0-9A-Za-z]/g, '');
  const cached = safeString_(function() { return cache.getProperty(cacheKey); });
  if (cached) {
    try { return JSON.parse(cached); } catch (e) {}
  }

  let extractionStatus = 'filename_only';
  let text = filenameText;
  let notes = '';
  if (ENABLE_PDF_TEXT_EXTRACTION && ctx.extractText && ctx.extractionCount < MAX_PDF_TEXT_EXTRACTIONS_PER_RUN) {
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
    detectedObjectName: parsed.detectedObjectName || inferObjectNameFromFilename_(file.getName()),
    detectedKatastarNumber: parsed.detectedKatastarNumber || '',
    detectedTile: parsed.detectedTile || '',
    detectedLocation: parsed.detectedLocation || '',
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

function parseDrawingText_(rawText) {
  const text = normalizeHrText_(rawText || '');
  return {
    detectedKatastarNumber: firstMatch_(text, [
      /katastarsk(?:i|og)?\s*(?:broj|br\.?|oznaka)\s*[:#\-]?\s*([A-ZČĆŽŠĐ0-9][A-ZČĆŽŠĐ0-9\/._\- ]{0,24})/i,
      /kat\.?\s*br\.?\s*[:#\-]?\s*([A-ZČĆŽŠĐ0-9][A-ZČĆŽŠĐ0-9\/._\- ]{0,24})/i,
      /k\.\s*br\.?\s*[:#\-]?\s*([A-ZČĆŽŠĐ0-9][A-ZČĆŽŠĐ0-9\/._\- ]{0,24})/i
    ]),
    detectedTile: firstMatch_(text, [
      /(?:broj\s*)?plo[cč]ic[aeu]?\s*[:#\-]?\s*([A-ZČĆŽŠĐ0-9][A-ZČĆŽŠĐ0-9\/._\- ]{0,24})/i,
      /(?:list|sekcija)\s*[:#\-]?\s*([A-ZČĆŽŠĐ0-9][A-ZČĆŽŠĐ0-9\/._\- ]{0,24})/i
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

function inferObjectNameFromFilename_(name) {
  return String(name || '')
    .replace(/\.pdf$/i, '')
    .replace(/[_\-–—]+/g, ' ')
    .replace(/\b(nacrt|plan|profil|tlocrt|pdf|final|novo|staro)\b/ig, ' ')
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
