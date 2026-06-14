/**
 * SOV Gmail -> Zapisnici sastanaka auto-import v6.1.41
 *
 * Kako koristiti:
 * 1) Gmail: labeliraj mailove za obradu labelom: SOV/Zapisnici za obradu
 * 2) Apps Script: zalijepi ovaj file u novi Google Apps Script projekt.
 * 3) Run: installSovGmailZapisniciTrigger()
 * 4) Dopusti Gmail/Drive/UrlFetch dozvole.
 *
 * Sigurnost:
 * - koristi Supabase anon key + zasebni ingest key.
 * - ingest key provjerava RPC public.sov_ingest_meeting_minutes_from_gmail().
 * - default key treba kasnije promijeniti i u SQL-u i ovdje.
 */

const SOV_GMAIL_CONFIG = {
  SUPABASE_URL: 'https://ncomefzkuixyfixisrhi.supabase.co',
  SUPABASE_ANON_KEY: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5jb21lZnprdWl4eWZpeGlzcmhpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk1ODQwOTYsImV4cCI6MjA5NTE2MDA5Nn0.WFSiENYXv48Npaz7vFcY-ksYvg_Ja40iNGsEqb1nUDk',
  INGEST_KEY: 'SOV_GMAIL_ZAPISNICI_2026_CHANGE_ME',
  SOURCE_LABEL: 'SOV/Zapisnici za obradu',
  DONE_LABEL: 'SOV/Zapisnici obrađeno',
  ERROR_LABEL: 'SOV/Zapisnici greška',
  DRIVE_FOLDER_NAME: 'SOV zapisnici sastanaka - Gmail arhiva',
  MAX_THREADS_PER_RUN: 20
};

function installSovGmailZapisniciTrigger() {
  ensureSovGmailLabels_();
  ScriptApp.getProjectTriggers()
    .filter(t => t.getHandlerFunction() === 'processSovGmailZapisnici')
    .forEach(t => ScriptApp.deleteTrigger(t));
  ScriptApp.newTrigger('processSovGmailZapisnici')
    .timeBased()
    .everyMinutes(15)
    .create();
  Logger.log('SOV Gmail zapisnici trigger installed: every 15 minutes.');
}

function processSovGmailZapisnici() {
  const labels = ensureSovGmailLabels_();
  const folder = ensureSovDriveFolder_();
  const threads = labels.source.getThreads(0, SOV_GMAIL_CONFIG.MAX_THREADS_PER_RUN);
  let imported = 0;
  let errors = 0;

  threads.forEach(thread => {
    let threadHadError = false;
    const messages = thread.getMessages();

    messages.forEach(msg => {
      const atts = msg.getAttachments({includeInlineImages: false, includeAttachments: true}) || [];
      atts
        .filter(att => /\.docx$/i.test(att.getName()))
        .forEach(att => {
          try {
            const saved = folder.createFile(att.copyBlob()).setName(att.getName());
            const text = extractDocxPlainText_(att.copyBlob());
            const checksum = sha256Hex_(att.getBytes());
            const payload = {
              p_ingest_key: SOV_GMAIL_CONFIG.INGEST_KEY,
              p_gmail_message_id: msg.getId(),
              p_gmail_thread_id: thread.getId(),
              p_subject: msg.getSubject(),
              p_from_email: msg.getFrom(),
              p_attachment_name: att.getName(),
              p_checksum_sha256: checksum,
              p_plain_text: text,
              p_storage_path: saved.getUrl()
            };
            const res = callSupabaseRpc_('sov_ingest_meeting_minutes_from_gmail', payload);
            Logger.log('Imported %s -> %s', att.getName(), JSON.stringify(res));
            imported++;
          } catch (err) {
            threadHadError = true;
            errors++;
            Logger.log('ERROR importing %s: %s', att.getName(), err && err.stack ? err.stack : err);
          }
        });
    });

    if (threadHadError) {
      thread.addLabel(labels.error);
    } else {
      thread.addLabel(labels.done);
      thread.removeLabel(labels.source);
    }
  });

  Logger.log('SOV Gmail zapisnici run done. Imported=%s errors=%s', imported, errors);
}

function ensureSovGmailLabels_() {
  return {
    source: getOrCreateLabel_(SOV_GMAIL_CONFIG.SOURCE_LABEL),
    done: getOrCreateLabel_(SOV_GMAIL_CONFIG.DONE_LABEL),
    error: getOrCreateLabel_(SOV_GMAIL_CONFIG.ERROR_LABEL)
  };
}

function getOrCreateLabel_(name) {
  return GmailApp.getUserLabelByName(name) || GmailApp.createLabel(name);
}

function ensureSovDriveFolder_() {
  const it = DriveApp.getFoldersByName(SOV_GMAIL_CONFIG.DRIVE_FOLDER_NAME);
  return it.hasNext() ? it.next() : DriveApp.createFolder(SOV_GMAIL_CONFIG.DRIVE_FOLDER_NAME);
}

function callSupabaseRpc_(fnName, payload) {
  const url = SOV_GMAIL_CONFIG.SUPABASE_URL.replace(/\/$/, '') + '/rest/v1/rpc/' + fnName;
  const response = UrlFetchApp.fetch(url, {
    method: 'post',
    contentType: 'application/json',
    muteHttpExceptions: true,
    headers: {
      apikey: SOV_GMAIL_CONFIG.SUPABASE_ANON_KEY,
      Authorization: 'Bearer ' + SOV_GMAIL_CONFIG.SUPABASE_ANON_KEY,
      Prefer: 'return=representation'
    },
    payload: JSON.stringify(payload)
  });
  const code = response.getResponseCode();
  const body = response.getContentText();
  if (code < 200 || code >= 300) {
    throw new Error('Supabase RPC failed ' + code + ': ' + body);
  }
  try { return JSON.parse(body); } catch (e) { return body; }
}

function extractDocxPlainText_(blob) {
  const files = Utilities.unzip(blob);
  const doc = files.find(f => f.getName() === 'word/document.xml');
  if (!doc) throw new Error('DOCX nema word/document.xml');
  let xml = doc.getDataAsString('UTF-8');
  xml = xml
    .replace(/<w:tab\/>/g, ' ')
    .replace(/<w:br\/>/g, '\n')
    .replace(/<\/w:p>/g, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/\r/g, '')
    .replace(/[ \t]+/g, ' ')
    .replace(/\n[ \t]+/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
  return xml;
}

function sha256Hex_(bytes) {
  const digest = Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, bytes);
  return digest.map(b => {
    const v = (b < 0 ? b + 256 : b).toString(16);
    return v.length === 1 ? '0' + v : v;
  }).join('');
}

function testSovGmailDocxParserOnly() {
  const labels = ensureSovGmailLabels_();
  const threads = labels.source.getThreads(0, 1);
  if (!threads.length) throw new Error('Nema mailova s labelom ' + SOV_GMAIL_CONFIG.SOURCE_LABEL);
  const msg = threads[0].getMessages()[0];
  const att = msg.getAttachments().find(a => /\.docx$/i.test(a.getName()));
  if (!att) throw new Error('Prvi mail nema DOCX attachment.');
  Logger.log(extractDocxPlainText_(att.copyBlob()).slice(0, 2000));
}
