/**
 * SOV v4.92 — Drive nacrti sync endpoint
 * Namjena: admin/arhivar jednom klikne Sync u webu, svi useri poslije vide thumbnail/full view.
 * Deploy: Apps Script > Deploy > New deployment > Web app
 * Execute as: Me
 * Who has access: Anyone with the link
 * Zalijepi /exec URL u assets/supabase-config.js kao window.SOV_DRAWINGS_SYNC_ENDPOINT.
 */
const SOV_NACRTI_FOLDER_ID = '1vCPsPaznDOgwRMU_XVDiI4aUhu0o8yFB';

function doGet(e) {
  const folder = DriveApp.getFolderById(SOV_NACRTI_FOLDER_ID);
  const files = [];
  collectFiles_(folder, files);
  return ContentService
    .createTextOutput(JSON.stringify({ ok: true, count: files.length, files }))
    .setMimeType(ContentService.MimeType.JSON);
}

function collectFiles_(folder, out) {
  const fileIt = folder.getFiles();
  while (fileIt.hasNext()) {
    const f = fileIt.next();
    out.push({
      id: f.getId(),
      name: f.getName(),
      mimeType: f.getMimeType(),
      size: f.getSize(),
      modifiedTime: f.getLastUpdated() ? f.getLastUpdated().toISOString() : null
    });
  }
  const folderIt = folder.getFolders();
  while (folderIt.hasNext()) collectFiles_(folderIt.next(), out);
}
