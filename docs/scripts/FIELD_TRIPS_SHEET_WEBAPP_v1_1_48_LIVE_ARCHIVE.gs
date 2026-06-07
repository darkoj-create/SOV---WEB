/**
 * SOV Izleti Sheet Web App v1.1.48 — live + archive friendly
 *
 * Purpose:
 * - Android app action=listTrips must always return all trips from the shared table.
 * - App then separates current/upcoming trips from past trips.
 * - Supports both the normal SOV trip sheet schema and the older DOCX-import schema.
 *
 * Normal SOV schema:
 *   A Datum | B Voditelj | C Lokacija | D Opis | E Cilj | F Sudionici | G Vozači | H Raspored URL | I Weather city
 *
 * Legacy DOCX-import schema also supported:
 *   A Datum | B Lokacija | C Ljudi
 */
const SOV_TRIPS_SPREADSHEET_ID = '1g93ZqKOJD2gLcIxZPfHokNcktbDEPivEItp7VRpnfWc';
const SOV_TRIPS_SHEET_NAME = ''; // '' = first tab

// Optional fallback: current DOCX importer sheet seen in older script.
// Keep true for safety while migrating; set false later if not needed.
const INCLUDE_LEGACY_DOCX_TRIPS_SHEET = true;
const LEGACY_DOCX_TRIPS_SPREADSHEET_ID = '1Z6phSE_j4WvKnPrxI-axxI1nitEa8uNtybyF2b4-9Zk';
const LEGACY_DOCX_TRIPS_SHEET_NAME = '';

const SOV_TRIPS_HEADERS = [
  'Datum', 'Voditelj', 'Lokacija', 'Opis', 'Cilj',
  'Sudionici', 'Vozači', 'Raspored URL', 'Weather city',
  'Package ID', 'Object count', 'Point count', 'Track count',
  'Center lat', 'Center lon', 'Min lat', 'Max lat', 'Min lon', 'Max lon',
  'Created at', 'Source'
];

function doGet(e) {
  try {
    const action = e && e.parameter ? String(e.parameter.action || '') : '';
    if (action === 'listTrips') return listTrips_();
    return json_({ ok: true, service: 'SOV Izleti Sheet Web App v1.1.48', actions: ['listTrips', 'addTripV2', 'deleteTrip', 'signupTrip', 'updateRasporedUrl'] });
  } catch (err) {
    return json_({ ok: false, error: String(err && err.message ? err.message : err) });
  }
}

function doPost(e) {
  try {
    const p = e && e.parameter ? e.parameter : {};
    const action = String(p.action || 'addTripV2');
    if (action === 'addTripV2' || action === 'addTrip') return addTrip_(p);
    if (action === 'deleteTrip') return deleteTrip_(p);
    if (action === 'signupTrip') return signupTrip_(p);
    if (action === 'updateRasporedUrl') return updateRasporedUrl_(p);
    return json_({ ok: false, error: 'Unknown action: ' + action });
  } catch (err) {
    return json_({ ok: false, error: String(err && err.message ? err.message : err) });
  }
}

function listTrips_() {
  const primary = readTripsFromSpreadsheet_(SOV_TRIPS_SPREADSHEET_ID, SOV_TRIPS_SHEET_NAME, true, 'shared');
  let trips = primary.trips;

  if (INCLUDE_LEGACY_DOCX_TRIPS_SHEET && LEGACY_DOCX_TRIPS_SPREADSHEET_ID && LEGACY_DOCX_TRIPS_SPREADSHEET_ID !== SOV_TRIPS_SPREADSHEET_ID) {
    const legacy = readTripsFromSpreadsheet_(LEGACY_DOCX_TRIPS_SPREADSHEET_ID, LEGACY_DOCX_TRIPS_SHEET_NAME, false, 'docx_archive');
    trips = dedupeTrips_(trips.concat(legacy.trips));
  }

  trips.sort(function(a, b) {
    return parseDateForSort_(a.date) - parseDateForSort_(b.date) || String(a.location).localeCompare(String(b.location), 'hr');
  });

  return json_({ ok: true, version: '1.1.48', updatedAt: new Date().toISOString(), count: trips.length, trips: trips });
}

function addTrip_(p) {
  const sh = openSheet_(SOV_TRIPS_SPREADSHEET_ID, SOV_TRIPS_SHEET_NAME);
  ensureHeader_(sh);
  const nextRow = Math.max(sh.getLastRow() + 1, 2);
  const row = [
    p.date || p.tripDate || '',
    p.leader || p.voditelj || '',
    p.location || p.lokacija || '',
    p.description || p.opis || '',
    p.goal || p.cilj || '',
    p.participants || '',
    p.drivers || '',
    p.rasporedUrl || '',
    p.weatherCity || '',
    p.packageId || '',
    p.objectCount || '',
    p.pointCount || '',
    p.trackCount || '',
    p.centerLat || '',
    p.centerLon || '',
    p.minLat || '',
    p.maxLat || '',
    p.minLon || '',
    p.maxLon || '',
    p.createdAt || '',
    p.source || 'SOV Android'
  ];
  sh.getRange(nextRow, 1, 1, row.length).setValues([row]);
  return json_({ ok: true, row: nextRow });
}

function deleteTrip_(p) {
  const rowNumber = Number(p.rowNumber || 0);
  if (!rowNumber || rowNumber < 2) return json_({ ok: false, error: 'Bad rowNumber' });
  const sh = openSheet_(SOV_TRIPS_SPREADSHEET_ID, SOV_TRIPS_SHEET_NAME);
  if (rowNumber > sh.getLastRow()) return json_({ ok: false, error: 'Row not found' });
  sh.deleteRow(rowNumber);
  return json_({ ok: true });
}

function signupTrip_(p) {
  const rowNumber = Number(p.rowNumber || 0);
  const name = String(p.name || '').trim();
  if (!rowNumber || rowNumber < 2 || !name) return json_({ ok: false, error: 'Missing rowNumber/name' });
  const sh = openSheet_(SOV_TRIPS_SPREADSHEET_ID, SOV_TRIPS_SHEET_NAME);
  ensureHeader_(sh);
  if (rowNumber > sh.getLastRow()) return json_({ ok: false, error: 'Row not found' });

  const participantCol = 6; // F
  const driversCol = 7; // G
  appendCellList_(sh, rowNumber, participantCol, name);
  if (String(p.vozim || p.driving || '').toLowerCase() === 'da' || String(p.vozim || p.driving || '').toLowerCase() === 'true') {
    appendCellList_(sh, rowNumber, driversCol, name);
  }
  return json_({ ok: true });
}

function updateRasporedUrl_(p) {
  const rowNumber = Number(p.rowNumber || 0);
  const url = String(p.rasporedUrl || '').trim();
  if (!rowNumber || rowNumber < 2 || !url) return json_({ ok: false, error: 'Missing rowNumber/rasporedUrl' });
  const sh = openSheet_(SOV_TRIPS_SPREADSHEET_ID, SOV_TRIPS_SHEET_NAME);
  ensureHeader_(sh);
  if (rowNumber > sh.getLastRow()) return json_({ ok: false, error: 'Row not found' });
  sh.getRange(rowNumber, 8).setValue(url);
  return json_({ ok: true });
}

function readTripsFromSpreadsheet_(spreadsheetId, sheetName, createHeaderIfEmpty, source) {
  try {
    const sh = openSheet_(spreadsheetId, sheetName);
    if (createHeaderIfEmpty) ensureHeader_(sh);
    const lastRow = sh.getLastRow();
    const lastCol = Math.max(sh.getLastColumn(), 3);
    if (lastRow < 2) return { trips: [] };

    const header = sh.getRange(1, 1, 1, lastCol).getDisplayValues()[0].map(String);
    const rows = sh.getRange(2, 1, lastRow - 1, lastCol).getDisplayValues();
    const idx = buildHeaderIndex_(header);
    const looksLegacy = hasHeader_(idx, ['lokacija']) && !hasHeader_(idx, ['voditelj', 'leader']) && hasHeader_(idx, ['ljudi']);

    const trips = [];
    rows.forEach(function(r, i) {
      const hasAny = r.some(function(v) { return String(v || '').trim() !== ''; });
      if (!hasAny) return;
      const rowNumber = i + 2;
      const date = cellByHeaders_(r, idx, ['datum', 'date'], 0);
      let leader = cellByHeaders_(r, idx, ['voditelj', 'leader'], 1);
      let location = cellByHeaders_(r, idx, ['lokacija', 'location'], looksLegacy ? 1 : 2);
      const participants = cellByHeaders_(r, idx, ['sudionici', 'participants', 'ljudi', 'ekipa'], looksLegacy ? 2 : 5);
      if (looksLegacy) leader = '';
      trips.push({
        rowNumber: rowNumber,
        date: String(date || ''),
        leader: String(leader || ''),
        location: String(location || ''),
        description: String(cellByHeaders_(r, idx, ['opis', 'description'], 3) || ''),
        goal: String(cellByHeaders_(r, idx, ['cilj', 'goal'], 4) || ''),
        participants: String(participants || ''),
        drivers: String(cellByHeaders_(r, idx, ['vozaci', 'vozači', 'drivers'], 6) || ''),
        rasporedUrl: String(cellByHeaders_(r, idx, ['raspored url', 'rasporedurl', 'raspored'], 7) || ''),
        weatherCity: String(cellByHeaders_(r, idx, ['weather city', 'weathercity', 'vrijeme grad'], 8) || ''),
        centerLat: numberOrNull_(cellByHeaders_(r, idx, ['center lat', 'centerlat'], 13)),
        centerLon: numberOrNull_(cellByHeaders_(r, idx, ['center lon', 'centerlon'], 14)),
        minLat: numberOrNull_(cellByHeaders_(r, idx, ['min lat', 'minlat'], 15)),
        maxLat: numberOrNull_(cellByHeaders_(r, idx, ['max lat', 'maxlat'], 16)),
        minLon: numberOrNull_(cellByHeaders_(r, idx, ['min lon', 'minlon'], 17)),
        maxLon: numberOrNull_(cellByHeaders_(r, idx, ['max lon', 'maxlon'], 18)),
        source: source
      });
    });
    return { trips: trips };
  } catch (err) {
    return { trips: [], error: String(err && err.message ? err.message : err) };
  }
}

function openSheet_(spreadsheetId, sheetName) {
  const ss = SpreadsheetApp.openById(spreadsheetId);
  const sh = sheetName ? ss.getSheetByName(sheetName) : ss.getSheets()[0];
  if (!sh) throw new Error('Sheet not found: ' + spreadsheetId + ' / ' + sheetName);
  return sh;
}

function ensureHeader_(sheet) {
  const existing = sheet.getRange(1, 1, 1, SOV_TRIPS_HEADERS.length).getValues()[0];
  const empty = existing.every(function(v) { return String(v || '').trim() === ''; });
  if (empty) {
    sheet.getRange(1, 1, 1, SOV_TRIPS_HEADERS.length).setValues([SOV_TRIPS_HEADERS]);
    sheet.setFrozenRows(1);
  }
}

function buildHeaderIndex_(header) {
  const idx = {};
  header.forEach(function(h, i) {
    const key = normalizeHeader_(h);
    if (key && idx[key] == null) idx[key] = i;
  });
  return idx;
}

function normalizeHeader_(value) {
  return String(value || '').toLowerCase()
    .replace(/[čć]/g, 'c').replace(/š/g, 's').replace(/đ/g, 'd').replace(/ž/g, 'z')
    .replace(/[_\-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function hasHeader_(idx, names) {
  return names.some(function(n) { return idx[normalizeHeader_(n)] != null; });
}

function cellByHeaders_(row, idx, names, fallbackIndex) {
  for (let i = 0; i < names.length; i++) {
    const pos = idx[normalizeHeader_(names[i])];
    if (pos != null && pos < row.length) return row[pos];
  }
  return fallbackIndex != null && fallbackIndex < row.length ? row[fallbackIndex] : '';
}

function appendCellList_(sh, row, col, value) {
  const cell = sh.getRange(row, col);
  const old = String(cell.getValue() || '').trim();
  if (!old) {
    cell.setValue(value);
  } else if (old.split(/,|;/).map(function(x) { return x.trim().toLowerCase(); }).indexOf(value.toLowerCase()) === -1) {
    cell.setValue(old + ', ' + value);
  }
}

function dedupeTrips_(trips) {
  const seen = {};
  const out = [];
  trips.forEach(function(t) {
    const key = [t.date, t.location, t.leader, t.participants].map(function(v) { return String(v || '').toLowerCase().replace(/\s+/g, ' ').trim(); }).join('|');
    if (!seen[key]) {
      seen[key] = true;
      out.push(t);
    }
  });
  return out;
}

function parseDateForSort_(value) {
  const nums = String(value || '').match(/\d+/g) || [];
  if (nums.length < 2) return 9999999999999;
  const nowYear = new Date().getFullYear();
  const day = Number(nums[0]);
  const month = Number(nums[1]);
  const year = nums.find(function(n) { return Number(n) >= 1900; });
  const y = year ? Number(year) : nowYear;
  return new Date(y, month - 1, day).getTime();
}

function numberOrNull_(value) {
  const n = Number(String(value || '').replace(',', '.'));
  return Number.isFinite(n) ? n : null;
}

function json_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
