/**
 * SOV Izleti Sheet Web App v1.0.31 RSVP driver fix
 * Target spreadsheet:
 * https://docs.google.com/spreadsheets/d/1g93ZqKOJD2gLcIxZPfHokNcktbDEPivEItp7VRpnfWc/edit
 *
 * Columns from row 2:
 * A Datum | B Voditelj | C Lokacija | D Opis | E Cilj | F Prijavljeni
 */
const SOV_TRIPS_SPREADSHEET_ID = '1g93ZqKOJD2gLcIxZPfHokNcktbDEPivEItp7VRpnfWc';
const SOV_TRIPS_HEADERS = ['Datum', 'Voditelj', 'Lokacija', 'Opis', 'Cilj', 'Prijavljeni'];

function doPost(e) {
  try {
    const p = e && e.parameter ? e.parameter : {};
    const action = String(p.action || '');
    const ss = SpreadsheetApp.openById(SOV_TRIPS_SPREADSHEET_ID);
    const sheet = ss.getSheets()[0];
    ensureHeader_(sheet);

    if (action === 'deleteTrip') return deleteTrip_(sheet, p);
    if (action === 'signupTrip') return signupTrip_(sheet, p);

    const row = [
      p.date || p.tripDate || '',
      p.leader || p.voditelj || '',
      p.location || p.lokacija || '',
      p.description || p.opis || '',
      p.goal || p.cilj || '',
      ''
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
    return json_({ ok: true, service: 'SOV Izleti Sheet Web App v1.0.31 RSVP driver fix' });
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

  const rows = sheet.getRange(2, 1, lastRow - 1, 6).getDisplayValues();
  const trips = [];
  rows.forEach((r, idx) => {
    const hasAny = r.some(v => String(v || '').trim() !== '');
    if (!hasAny) return;
    trips.push({
      rowNumber: idx + 2,
      date: String(r[0] || ''),
      leader: String(r[1] || ''),
      location: String(r[2] || ''),
      description: String(r[3] || ''),
      goal: String(r[4] || ''),
      participants: String(r[5] || '')
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

function json_(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
