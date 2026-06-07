/**
 * Darko OS Track Export Web App v1.0
 *
 * Stavi ovu skriptu u Apps Script vezan uz Darko OS Google Sheet:
 * https://docs.google.com/spreadsheets/d/1GLc4W-3DPEsX_XYLEHok9-Q83XT3Y8xIZ2Y85En3Dxo/edit
 *
 * Deploy kao Web app:
 * - Execute as: Me
 * - Who has access: Anyone with the link / Anyone
 *
 * Zatim /exec URL zalijepi u SOV Admin:
 * app/src/main/java/com/darko/speleov1/DarkoOsTrackSyncClient.kt
 * DARKO_OS_WEBAPP_URL
 */

const DARKO_OS_SPREADSHEET_ID = '1GLc4W-3DPEsX_XYLEHok9-Q83XT3Y8xIZ2Y85En3Dxo';
const TRACK_EXPORT_SHEET_NAME = 'SOV_Track_Exports';
const DRIVE_FOLDER_NAME = 'Darko OS - SOV GPX Track Exports';

const TRACK_EXPORT_HEADERS = [
  'LoggedAt',
  'Source',
  'TrackId',
  'Name',
  'Description',
  'CreatedAt',
  'StartTime',
  'EndTime',
  'DurationMin',
  'DistanceKm',
  'AscentM',
  'DescentM',
  'PointCount',
  'StartLat',
  'StartLon',
  'EndLat',
  'EndLon',
  'MinLat',
  'MaxLat',
  'MinLon',
  'MaxLon',
  'GpxUrl',
  'GpxFileId'
];

function doGet(e) {
  return json_({ ok: true, service: 'Darko OS Track Export Web App v1.0', actions: ['appendTrack'] });
}

function doPost(e) {
  try {
    const p = e && e.parameter ? e.parameter : {};
    const action = String(p.action || '');
    if (action !== 'appendTrack') {
      return json_({ ok: false, error: 'Unknown action: ' + action });
    }
    return appendTrack_(p);
  } catch (err) {
    return json_({ ok: false, error: String(err), stack: err && err.stack ? String(err.stack) : '' });
  }
}

function appendTrack_(p) {
  const ss = SpreadsheetApp.openById(DARKO_OS_SPREADSHEET_ID);
  const sh = getOrCreateSheet_(ss, TRACK_EXPORT_SHEET_NAME, TRACK_EXPORT_HEADERS);

  const trackId = clean_(p.trackId);
  const name = clean_(p.name) || 'SOV track';
  const gpx = String(p.gpx || '');

  if (!trackId) return json_({ ok: false, error: 'Missing trackId' });
  if (!gpx) return json_({ ok: false, error: 'Missing GPX payload' });

  const existingRow = findExistingTrackRow_(sh, trackId);
  if (existingRow > 0) {
    return json_({ ok: true, row: existingRow, duplicate: true, message: 'Track already exported' });
  }

  const file = saveGpxFile_(name, trackId, gpx);

  const row = [
    new Date(),
    clean_(p.source) || 'SOV_ADMIN',
    trackId,
    name,
    clean_(p.description),
    clean_(p.createdAt),
    clean_(p.startTime),
    clean_(p.endTime),
    numOrBlank_(p.durationMin),
    numOrBlank_(p.distanceKm),
    numOrBlank_(p.ascentM),
    numOrBlank_(p.descentM),
    numOrBlank_(p.pointCount),
    numOrBlank_(p.startLat),
    numOrBlank_(p.startLon),
    numOrBlank_(p.endLat),
    numOrBlank_(p.endLon),
    numOrBlank_(p.minLat),
    numOrBlank_(p.maxLat),
    numOrBlank_(p.minLon),
    numOrBlank_(p.maxLon),
    file.getUrl(),
    file.getId()
  ];

  const nextRow = sh.getLastRow() + 1;
  sh.getRange(nextRow, 1, 1, row.length).setValues([row]);
  sh.autoResizeColumns(1, TRACK_EXPORT_HEADERS.length);

  return json_({ ok: true, row: nextRow, url: file.getUrl(), fileId: file.getId() });
}

function getOrCreateSheet_(ss, name, headers) {
  let sh = ss.getSheetByName(name);
  if (!sh) sh = ss.insertSheet(name);

  const existing = sh.getRange(1, 1, 1, headers.length).getValues()[0];
  const empty = existing.every(v => String(v || '').trim() === '');
  if (empty) {
    sh.getRange(1, 1, 1, headers.length).setValues([headers]);
    sh.setFrozenRows(1);
  } else {
    headers.forEach((h, i) => {
      if (String(existing[i] || '').trim() === '') sh.getRange(1, i + 1).setValue(h);
    });
  }
  return sh;
}

function findExistingTrackRow_(sh, trackId) {
  const lastRow = sh.getLastRow();
  if (lastRow < 2) return 0;
  const values = sh.getRange(2, 3, lastRow - 1, 1).getValues();
  for (let i = 0; i < values.length; i++) {
    if (String(values[i][0] || '').trim() === trackId) return i + 2;
  }
  return 0;
}

function saveGpxFile_(name, trackId, gpx) {
  const folder = getOrCreateFolder_(DRIVE_FOLDER_NAME);
  const safeName = sanitizeFileName_(name || 'sov_track');
  const stamp = Utilities.formatDate(new Date(), Session.getScriptTimeZone(), 'yyyyMMdd_HHmmss');
  const filename = `${stamp}_${safeName}_${trackId}.gpx`;
  return folder.createFile(filename, gpx, MimeType.PLAIN_TEXT);
}

function getOrCreateFolder_(name) {
  const it = DriveApp.getFoldersByName(name);
  if (it.hasNext()) return it.next();
  return DriveApp.createFolder(name);
}

function sanitizeFileName_(value) {
  return String(value || 'track')
    .replace(/[\\/:*?"<>|]+/g, '_')
    .replace(/\s+/g, '_')
    .substring(0, 80);
}

function clean_(v) {
  return v == null ? '' : String(v).trim();
}

function numOrBlank_(v) {
  const s = clean_(v).replace(',', '.');
  if (!s) return '';
  const n = Number(s);
  return Number.isFinite(n) ? n : '';
}

function json_(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
