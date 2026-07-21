/**
 * SOV Apps Script shared-key guard — v1.4.40a
 *
 * Setup in EVERY target Apps Script deployment:
 * 1) Project Settings → Script Properties
 * 2) Add property: SOV_APPS_SCRIPT_KEY = same secret as Android local.properties
 * 3) In doGet/doPost call sovRequireKey_(e) before any real work.
 *
 * Android sends the key both as:
 * - query/form parameter: X-SOV-KEY
 * - HTTP header: X-SOV-KEY
 * Apps Script webapps reliably expose query/form params via e.parameter.
 */
const SOV_KEY_PARAM = 'X-SOV-KEY';
const SOV_KEY_PROPERTY = 'SOV_APPS_SCRIPT_KEY';

function sovJson_(obj, code) {
  const payload = Object.assign({ statusCode: code || 200 }, obj || {});
  return ContentService
    .createTextOutput(JSON.stringify(payload))
    .setMimeType(ContentService.MimeType.JSON);
}

function sovExpectedKey_() {
  return String(PropertiesService.getScriptProperties().getProperty(SOV_KEY_PROPERTY) || '').trim();
}

function sovProvidedKey_(e) {
  const p = (e && e.parameter) ? e.parameter : {};
  return String(
    p[SOV_KEY_PARAM] ||
    p['x-sov-key'] ||
    p['sov_key'] ||
    p['key'] ||
    ''
  ).trim();
}

function sovIsAuthorized_(e) {
  const expected = sovExpectedKey_();
  if (!expected) {
    console.error('Missing Script Property: ' + SOV_KEY_PROPERTY);
    return false;
  }
  return sovProvidedKey_(e) === expected;
}

function sovRequireKey_(e) {
  if (sovIsAuthorized_(e)) return null;
  return sovJson_({ ok: false, error: 'Forbidden' }, 403);
}

/**
 * Pattern for GET endpoint:
 * Rename your old doGet body to doGetCore_, then keep this wrapper.
 */
function doGet(e) {
  const denied = sovRequireKey_(e);
  if (denied) return denied;
  return doGetCore_(e);
}

/**
 * Pattern for POST endpoint:
 * Rename your old doPost body to doPostCore_, then keep this wrapper.
 */
function doPost(e) {
  const denied = sovRequireKey_(e);
  if (denied) return denied;
  return doPostCore_(e);
}

/*
Example conversion:

OLD:
function doGet(e) {
  const action = e.parameter.action;
  ...
}

NEW:
function doGetCore_(e) {
  const action = e.parameter.action;
  ...
}
function doGet(e) {
  const denied = sovRequireKey_(e);
  if (denied) return denied;
  return doGetCore_(e);
}
*/
