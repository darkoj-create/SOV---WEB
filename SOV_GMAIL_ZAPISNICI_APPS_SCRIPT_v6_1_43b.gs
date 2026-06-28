/** SOV Gmail zapisnici v6.1.43b — weekly + manual 28-day sync. */
const SOV_GMAIL_CFG = {
  SUPABASE_URL: 'https://ncomefzkuixyfixisrhi.supabase.co',
  SUPABASE_ANON_KEY: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJIUzI1NiIsInJlZiI6Im5jb21lZnprdWl4eWZpeGlzcmhpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk1ODQwOTYsImV4cCI6MjA5NTE2MDA5Nn0.WFSiENYXv48Npaz7vFcY-ksYvg_Ja40iNGsEqb1nUDk',
  INGEST_KEY: 'SOV_GMAIL_ZAPISNICI_2026_CHANGE_ME',
  LABEL_QUEUE: 'SOV/Zapisnici za obradu',
  LABEL_DONE: 'SOV/Zapisnici obrađeno',
  LABEL_ERROR: 'SOV/Zapisnici greška'
};

function doGet(e) {
  const callback = String((e && e.parameter.callback) || '').replace(/[^a-zA-Z0-9_$]/g, '');
  let result;
  try {
    if (!e || e.parameter.action !== 'sync') throw new Error('Unknown action');
    const days = Math.max(1, Math.min(28, Number(e.parameter.days || 28)));
    result = processSovGmailZapisnici(days);
  } catch (err) {
    result = {ok:false,error:String(err && err.message || err)};
  }
  const json = JSON.stringify(result);
  return ContentService.createTextOutput(callback ? callback + '(' + json + ');' : json)
    .setMimeType(callback ? ContentService.MimeType.JAVASCRIPT : ContentService.MimeType.JSON);
}

function processSovGmailZapisnici(days) {
  days = (typeof days === 'number' && isFinite(days)) ? days : 28;
  days = Math.max(1, Math.min(28, days));
  const queue = getOrCreateLabel_(SOV_GMAIL_CFG.LABEL_QUEUE);
  const done = getOrCreateLabel_(SOV_GMAIL_CFG.LABEL_DONE);
  const failedLabel = getOrCreateLabel_(SOV_GMAIL_CFG.LABEL_ERROR);
  const query = 'newer_than:' + days + 'd has:attachment filename:docx {label:"' + SOV_GMAIL_CFG.LABEL_QUEUE + '" subject:zapisnik}';
  const threads = GmailApp.search(query, 0, 100);
  const out = {ok:true,days:days,threads:threads.length,messages:0,attachments:0,imported:0,skipped:0,failed:0};
  threads.forEach(function(thread) {
    let threadFailed = false, threadProcessed = false;
    thread.getMessages().forEach(function(message) {
      if (message.getDate().getTime() < Date.now() - days * 86400000) return;
      out.messages++;
      message.getAttachments({includeInlineImages:false,includeAttachments:true}).forEach(function(att) {
        if (!/\.docx$/i.test(att.getName()) && att.getContentType() !== 'application/vnd.openxmlformats-officedocument.wordprocessingml.document') return;
        out.attachments++;
        try {
          const text = docxText_(att.copyBlob());
          const checksum = sha256Hex_(att.getBytes());
          const response = ingest_(message, att, checksum, text);
          if (response.status === 'imported') out.imported++; else out.skipped++;
          threadProcessed = true;
        } catch (err) {
          out.failed++; threadFailed = true;
          console.error(message.getId() + ' / ' + att.getName() + ': ' + err);
        }
      });
    });
    if (threadFailed) thread.addLabel(failedLabel);
    if (threadProcessed && !threadFailed) { thread.addLabel(done); thread.removeLabel(queue); thread.removeLabel(failedLabel); }
  });
  return out;
}

function installSovGmailZapisniciTrigger() {
  ScriptApp.getProjectTriggers().forEach(function(t) {
    if (t.getHandlerFunction() === 'processSovGmailZapisnici') ScriptApp.deleteTrigger(t);
  });
  ScriptApp.newTrigger('processSovGmailZapisnici').timeBased().onWeekDay(ScriptApp.WeekDay.WEDNESDAY).atHour(23).nearMinute(50).everyWeeks(1).create();
}

function ingest_(message, attachment, checksum, text) {
  const payload = {
    p_ingest_key:SOV_GMAIL_CFG.INGEST_KEY,
    p_gmail_message_id:message.getId(),
    p_gmail_thread_id:message.getThread().getId(),
    p_subject:message.getSubject(),
    p_from_email:message.getFrom(),
    p_attachment_name:attachment.getName(),
    p_checksum_sha256:checksum,
    p_plain_text:text,
    p_storage_path:'gmail/' + message.getId() + '/' + attachment.getName()
  };
  const res = UrlFetchApp.fetch(SOV_GMAIL_CFG.SUPABASE_URL + '/rest/v1/rpc/sov_ingest_meeting_minutes_from_gmail', {
    method:'post', contentType:'application/json', muteHttpExceptions:true,
    headers:{apikey:SOV_GMAIL_CFG.SUPABASE_ANON_KEY,Authorization:'Bearer ' + SOV_GMAIL_CFG.SUPABASE_ANON_KEY},
    payload:JSON.stringify(payload)
  });
  if (res.getResponseCode() < 200 || res.getResponseCode() >= 300) throw new Error('Supabase ' + res.getResponseCode() + ': ' + res.getContentText());
  return JSON.parse(res.getContentText() || '{}');
}

function docxText_(blob) {
  const files = Utilities.unzip(blob.setContentType('application/zip'));
  const documentXml = files.filter(function(f){ return f.getName() === 'word/document.xml'; })[0];
  if (!documentXml) throw new Error('DOCX nema word/document.xml');
  return documentXml.getDataAsString('UTF-8')
    .replace(/<w:tab[^>]*\/>/g, '\t').replace(/<w:br[^>]*\/>/g, '\n')
    .replace(/<\/w:p>/g, '\n').replace(/<[^>]+>/g, '')
    .replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'")
    .replace(/\r/g, '').replace(/[ \t]+\n/g, '\n').replace(/\n{3,}/g, '\n\n').trim();
}

function sha256Hex_(bytes) {
  return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, bytes).map(function(b){ const v=(b<0?b+256:b).toString(16); return v.length===1?'0'+v:v; }).join('');
}
function getOrCreateLabel_(name) { return GmailApp.getUserLabelByName(name) || GmailApp.createLabel(name); }
