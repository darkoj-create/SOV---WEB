/**
 * SOV Izleti Sheet Web App v1.7.46
 * Target spreadsheet:
 * https://docs.google.com/spreadsheets/d/1g93ZqKOJD2gLcIxZPfHokNcktbDEPivEItp7VRpnfWc/edit
 *
 * Columns from row 2:
 * A Datum | B Voditelj | C Lokacija | D Opis | E Cilj
 */
const SOV_TRIPS_SPREADSHEET_ID = '1g93ZqKOJD2gLcIxZPfHokNcktbDEPivEItp7VRpnfWc';
const SOV_TRIPS_HEADERS = ['Datum', 'Voditelj', 'Lokacija', 'Opis', 'Cilj'];

function doPost(e) {
  try {
    const p = e && e.parameter ? e.parameter : {};
    const ss = SpreadsheetApp.openById(SOV_TRIPS_SPREADSHEET_ID);
    const sheet = ss.getSheets()[0];
    ensureHeader_(sheet);

    const row = [
      p.date || p.tripDate || '',
      p.leader || p.voditelj || '',
      p.location || p.lokacija || '',
      p.description || p.opis || '',
      p.goal || p.cilj || ''
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
    if (action === 'listTrips') {
      return listTrips_();
    }
    return json_({ ok: true, service: 'SOV Izleti Sheet Web App v1.7.46' });
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

  const rows = sheet.getRange(2, 1, lastRow - 1, 5).getDisplayValues();
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
      goal: String(r[4] || '')
    });
  });
  return json_({ ok: true, trips });
}

function ensureHeader_(sheet) {
  const existing = sheet.getRange(1, 1, 1, SOV_TRIPS_HEADERS.length).getValues()[0];
  const empty = existing.every(v => String(v || '').trim() === '');
  if (empty) {
    sheet.getRange(1, 1, 1, SOV_TRIPS_HEADERS.length).setValues([SOV_TRIPS_HEADERS]);
    sheet.setFrozenRows(1);
  }
}

function json_(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
