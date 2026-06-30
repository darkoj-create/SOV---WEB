/**
 * SOV Drawings Index WebApp v1.0.149
 * Deploy as: Apps Script > Deploy > New deployment > Web app
 * Execute as: Me
 * Who has access: Anyone with the link
 *
 * The Android app reads this endpoint with ?action=listDrawings.
 */
const DRAWINGS_FOLDER_ID = '1vCPsPaznDOgwRMU_XVDiI4aUhu0o8yFB';
const INCLUDE_SUBFOLDERS = true;

function doGet(e) {
  const action = String((e && e.parameter && e.parameter.action) || 'listDrawings');
  if (action !== 'listDrawings') {
    return jsonOutput({ ok: false, error: 'Unknown action: ' + action });
  }
  try {
    const folder = DriveApp.getFolderById(DRAWINGS_FOLDER_ID);
    const drawings = [];
    collectPdfFiles_(folder, drawings, folder.getName());
    drawings.sort(function(a, b) { return String(a.fileName).localeCompare(String(b.fileName), 'hr'); });
    return jsonOutput({
      ok: true,
      updatedAt: new Date().toISOString(),
      folderId: DRAWINGS_FOLDER_ID,
      folderName: folder.getName(),
      count: drawings.length,
      drawings: drawings
    });
  } catch (err) {
    return jsonOutput({ ok: false, error: String(err && err.message ? err.message : err) });
  }
}

function collectPdfFiles_(folder, out, path) {
  const files = folder.getFiles();
  while (files.hasNext()) {
    const file = files.next();
    const name = file.getName() || '';
    const mime = file.getMimeType() || '';
    const isPdf = mime === MimeType.PDF || /\.pdf$/i.test(name);
    if (!isPdf) continue;
    const id = file.getId();
    out.push({
      fileId: id,
      fileName: name,
      name: name,
      mimeType: mime,
      sizeBytes: safeNumber_(function() { return file.getSize(); }),
      modifiedTime: safeString_(function() { return file.getLastUpdated().toISOString(); }),
      folderPath: path,
      webViewUrl: file.getUrl(),
      downloadUrl: 'https://drive.google.com/uc?export=download&id=' + encodeURIComponent(id),
      recordId: '',
      katastarId: '',
      objectName: '',
      matchStatus: '',
      notes: ''
    });
  }

  if (!INCLUDE_SUBFOLDERS) return;
  const folders = folder.getFolders();
  while (folders.hasNext()) {
    const child = folders.next();
    collectPdfFiles_(child, out, path + '/' + child.getName());
  }
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
