/**
 * SOV Izleti Sheet Web App v1.0.89 shared trip coordinates + raspored url + weather city
 *
 * Paste this over the existing Apps Script project used by the shared Izleti /exec URL,
 * then Deploy > Manage deployments > Edit existing web app deployment > New version.
 * Keeping the same deployment keeps the Android app FIXED_WEBAPP_URL valid.
 *
 * Columns from row 2:
 * A Datum | B Voditelj | C Lokacija | D Opis | E Cilj | F Prijavljeni |
 * G CenterLat | H CenterLon | I MinLat | J MaxLat | K MinLon | L MaxLon | M RasporedUrl | N WeatherCity
 */
const SOV_TRIPS_SPREADSHEET_ID = '1g93ZqKOJD2gLcIxZPfHokNcktbDEPivEItp7VRpnfWc';
const SOV_TRIPS_HEADERS = [
  'Datum', 'Voditelj', 'Lokacija', 'Opis', 'Cilj', 'Prijavljeni',
  'CenterLat', 'CenterLon', 'MinLat', 'MaxLat', 'MinLon', 'MaxLon', 'RasporedUrl', 'WeatherCity'
];

function doPost(e) {
  try {
    const p = e && e.parameter ? e.parameter : {};
    const action = String(p.action || '');
    const ss = SpreadsheetApp.openById(SOV_TRIPS_SPREADSHEET_ID);
    const sheet = ss.getSheets()[0];
    ensureHeader_(sheet);

    if (action === 'deleteTrip') return deleteTrip_(sheet, p);
    if (action === 'signupTrip') return signupTrip_(sheet, p);
    if (action === 'updateRasporedUrl') return updateRasporedUrl_(sheet, p);

    const row = [
      p.date || p.tripDate || '',
      p.leader || p.voditelj || '',
      p.location || p.lokacija || '',
      p.description || p.opis || '',
      p.goal || p.cilj || '',
      '',
      numberOrBlank_(p.centerLat),
      numberOrBlank_(p.centerLon),
      numberOrBlank_(p.minLat),
      numberOrBlank_(p.maxLat),
      numberOrBlank_(p.minLon),
      numberOrBlank_(p.maxLon),
      p.rasporedUrl || '',
      p.weatherCity || ''
    ];

    const nextRow = Math.max(sheet.getLastRow() + 1, 2);
    sheet.getRange(nextRow, 1, 1, row.length).setValues([row]);

    return json_({ ok: true, row: nextRow });
  } catch (err) {
    return json_({ ok: false, error: String(err) });
  }
}

function doGet(e) {
  try {
    const action = e && e.parameter ? String(e.parameter.action || '') : '';
    if (action === 'listTrips') return listTrips_();
    return json_({ ok: true, service: 'SOV Izleti Sheet Web App v1.0.89 shared trip coordinates + raspored url + weather city' });
  } catch (err) {
    return json_({ ok: false, error: String(err) });
  }
}

function listTrips_() {
  const ss = SpreadsheetApp.openById(SOV_TRIPS_SPREADSHEET_ID);
  const sheet = ss.getSheets()[0];
  ensureHeader_(sheet);
  const lastRow = sheet.getLastRow();
  if (lastRow < 2) return json_({ ok: true, trips: [] });

  const rows = sheet.getRange(2, 1, lastRow - 1, SOV_TRIPS_HEADERS.length).getValues();
  const trips = [];
  rows.forEach((r, idx) => {
    const hasAny = r.some(v => String(v || '').trim() !== '');
    if (!hasAny) return;
    trips.push({
      rowNumber: idx + 2,
      date: displayDate_(r[0]),
      leader: String(r[1] || ''),
      location: String(r[2] || ''),
      description: String(r[3] || ''),
      goal: String(r[4] || ''),
      participants: String(r[5] || ''),
      centerLat: nullableNumber_(r[6]),
      centerLon: nullableNumber_(r[7]),
      minLat: nullableNumber_(r[8]),
      maxLat: nullableNumber_(r[9]),
      minLon: nullableNumber_(r[10]),
      maxLon: nullableNumber_(r[11]),
      rasporedUrl: String(r[12] || ''),
      weatherCity: String(r[13] || '')
    });
  });
  return json_({ ok: true, trips });
}

function signupTrip_(sheet, p) {
  const rowNumber = Number(p.rowNumber || 0);
  const name = String(p.name || '').trim();
  const driving = String(p.driving || '').toUpperCase() === 'TRUE';
  if (!rowNumber || rowNumber < 2 || !name) return json_({ ok: false, error: 'Missing rowNumber/name' });

  const cell = sheet.getRange(rowNumber, 6);
  const current = String(cell.getDisplayValue() || '').trim();
  const cleanName = name.replace(/[;|\n\r]+/g, ' ').trim();
  const entry = cleanName + (driving ? ' 🚗' : '');

  const parts = current
    ? current.split(/[,;]+/).map(v => String(v || '').trim()).filter(Boolean)
    : [];
  const normalizedName = cleanName.toLowerCase();
  const withoutSame = parts.filter(v => v.replace('🚗', '').trim().toLowerCase() !== normalizedName);
  const next = withoutSame.concat([entry]).join(', ');
  cell.setValue(next);
  return json_({ ok: true, participants: next });
}

function updateRasporedUrl_(sheet, p) {
  const rowNumber = Number(p.rowNumber || 0);
  const rasporedUrl = String(p.rasporedUrl || '').trim();
  if (!rowNumber || rowNumber < 2 || !rasporedUrl) return json_({ ok: false, error: 'Missing rowNumber/rasporedUrl' });
  sheet.getRange(rowNumber, 13).setValue(rasporedUrl);
  return json_({ ok: true });
}

function deleteTrip_(sheet, p) {
  const rowNumber = Number(p.rowNumber || 0);
  if (!rowNumber || rowNumber < 2) return json_({ ok: false, error: 'Missing rowNumber' });
  sheet.deleteRow(rowNumber);
  return json_({ ok: true });
}

function ensureHeader_(sheet) {
  const existing = sheet.getRange(1, 1, 1, SOV_TRIPS_HEADERS.length).getValues()[0];
  const empty = existing.every(v => String(v || '').trim() === '');
  if (empty) {
    sheet.getRange(1, 1, 1, SOV_TRIPS_HEADERS.length).setValues([SOV_TRIPS_HEADERS]);
    sheet.setFrozenRows(1);
    return;
  }
  SOV_TRIPS_HEADERS.forEach((header, idx) => {
    if (String(existing[idx] || '').trim() === '') sheet.getRange(1, idx + 1).setValue(header);
  });
}

function numberOrBlank_(value) {
  const n = nullableNumber_(value);
  return n === null ? '' : n;
}

function nullableNumber_(value) {
  if (value === null || value === undefined) return null;
  const clean = String(value).trim().replace(',', '.');
  if (!clean) return null;
  const n = Number(clean);
  return Number.isFinite(n) ? n : null;
}

function displayDate_(value) {
  if (Object.prototype.toString.call(value) === '[object Date]' && !isNaN(value.getTime())) {
    return Utilities.formatDate(value, Session.getScriptTimeZone(), 'dd.MM.yyyy.');
  }
  return String(value || '');
}

function json_(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
