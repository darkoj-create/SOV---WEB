/**
 * FUTURE-PROOF: SPELEO IZLETI (DOCX attachment) → Google Sheet
 * Upis:
 *   A2↓ Datum
 *   B2↓ Lokacija
 *   C2↓ Ljudi
 *
 * Izvor:
 *   - Gmail: uzima NAJNOVIJI mail čiji subject SADRŽI "Zapisnik" (bilo gdje)
 *   - Attachment: .docx čije ime POČINJE s "Zapisnik"
 *
 * Robusno:
 *   - radi s jednim datumom (4.2.) ili rasponom (31.01.-01.02.)
 *   - tolerira bullets i različite crte (- – —) i prelomljene linije
 *   - ljude traži po više patterna: "Ljudi:", "Bili su", "Sudjelovali", "Ekipa", "(...)" itd.
 *   - NE reže na prvoj zarezi (jer popis imena ide zarezima)
 *   - opis reže na keyworde ("u okviru", "projekt", "postavljali"...), ne na zarez
 *   - ako ne prepozna ljude, upiše "(nije prepoznato)"
 *   - status/debug u D1 (poruka) + E1 (vrijeme)
 *
 * VAŽNO:
 *   - U Apps Scriptu uključi Advanced Google service: Drive API (v2)
 *     Services → + → Drive API
 */

const SPREADSHEET_ID = "1Z6phSE_j4WvKnPrxI-axxI1nitEa8uNtybyF2b4-9Zk";
const SHEET_NAME = ""; // "" = prvi tab

const GMAIL_QUERY = ["newer_than:365d", "in:inbox", "subject:Zapisnik"].join(" ");
const SUBJECT_MUST_CONTAIN = "Zapisnik";

const ATTACH_PREFIX = "Zapisnik"; // attachment name starts with this
const SECTION_START = "SPELEO IZLETI";
const SECTION_END = "INI IZLETI";

const TRASH_CONVERTED_DOC = true;

/* ===================== MAIN ===================== */

function importSpeleoTrips_toColumnsABC() {
  const { sh } = openTargetSheet_();
  const props = PropertiesService.getScriptProperties();
  const lastProcessedId = props.getProperty("LAST_MSG_ID") || "";

  try {
    setStatus_(sh, "START");

    // Ultra-low Gmail calls: only latest matching thread
    const threads = GmailApp.search(GMAIL_QUERY, 0, 1);
    if (!threads.length) {
      setStatus_(sh, "NEMA THREADOVA za query: " + GMAIL_QUERY);
      return;
    }

    const msg = threads[0].getMessages().slice(-1)[0];
    const messageId = msg.getId();
    const subj = (msg.getSubject() || "").trim();

    setStatus_(sh, `NAĐEN MAIL: "${subj}" | id=${messageId}`);

    if (messageId === lastProcessedId) {
      setStatus_(sh, `STOP: isti messageId kao LAST_MSG_ID (${messageId})`);
      return;
    }

    if (!containsIgnoreCase_(subj, SUBJECT_MUST_CONTAIN)) {
      props.setProperty("LAST_MSG_ID", messageId);
      setStatus_(sh, `SKIP: subject ne sadrži "${SUBJECT_MUST_CONTAIN}"`);
      return;
    }

    const docxAtt = pickDocxAttachmentStrict_(msg, ATTACH_PREFIX);
    if (!docxAtt) {
      props.setProperty("LAST_MSG_ID", messageId);
      setStatus_(sh, `SKIP: nema .docx attachmenta koji počinje s "${ATTACH_PREFIX}"`);
      return;
    }
    setStatus_(sh, `ATTACH OK: ${docxAtt.getName()}`);

    const text = convertDocxAttachmentToText_(docxAtt, TRASH_CONVERTED_DOC);
    if (!text) {
      props.setProperty("LAST_MSG_ID", messageId);
      setStatus_(sh, "FAIL: DOCX nije pretvoren u tekst (Drive API / permissions?)");
      return;
    }

    const section = extractSectionFromTextRobust_(text, SECTION_START, SECTION_END);
    if (!section) {
      props.setProperty("LAST_MSG_ID", messageId);
      setStatus_(sh, `SKIP: sekcija "${SECTION_START}" nije nađena ili je prazna`);
      return;
    }

    const year = extractYear_(subj, text);
    const trips = parseSpeleoTripsFutureProof_(section, year);

    if (!trips.length) {
      props.setProperty("LAST_MSG_ID", messageId);
      setStatus_(sh, "SKIP: sekcija postoji, ali nije pronađen nijedan datum/stavka");
      return;
    }

    writeTripsToABC_(sh, trips);

    props.setProperty("LAST_MSG_ID", messageId);

    const unknownPeopleCount = trips.filter(t => t.people === "(nije prepoznato)").length;
    if (unknownPeopleCount) {
      setStatus_(sh, `OK: upisano ${trips.length} izleta (ljudi neprepoznati u ${unknownPeopleCount})`);
    } else {
      setStatus_(sh, `OK: upisano ${trips.length} izleta`);
    }
  } catch (e) {
    setStatus_(sh, "ERROR: " + (e && e.message ? e.message : String(e)));
    throw e;
  }
}

function resetLastMsgId() {
  PropertiesService.getScriptProperties().deleteProperty("LAST_MSG_ID");
  const { sh } = openTargetSheet_();
  setStatus_(sh, "RESET: LAST_MSG_ID obrisan");
}

/* ===================== SHEET ===================== */

function openTargetSheet_() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sh = SHEET_NAME ? ss.getSheetByName(SHEET_NAME) : ss.getSheets()[0];
  if (!sh) throw new Error("Sheet not found (provjeri SHEET_NAME).");
  return { sh };
}

function setStatus_(sh, msg) {
  sh.getRange("D1").setValue(msg);
  sh.getRange("E1").setValue(new Date());
}

function writeTripsToABC_(sh, trips) {
  const startRow = Math.max(2, sh.getLastRow() + 1);
  const values = trips.map(t => [t.date, t.location, t.people]); // A,B,C
  sh.getRange(startRow, 1, values.length, 3).setValues(values);
}

/* ===================== SECTION EXTRACTION (ROBUST) ===================== */

function extractSectionFromTextRobust_(text, startHeader, endHeader) {
  const raw = String(text || "")
    .replace(/\u00A0/g, " ")
    .replace(/\r/g, "\n")
    .replace(/[ \t]+/g, " ")
    .replace(/\n{3,}/g, "\n\n")
    .trim();

  // Try line-based exact headers
  const lines = raw.split("\n").map(l => l.trim());
  const startIdx = lines.findIndex(l => l.toUpperCase() === String(startHeader).toUpperCase());
  if (startIdx !== -1) {
    const after = lines.slice(startIdx + 1);
    const endRel = after.findIndex(l => l.toUpperCase() === String(endHeader).toUpperCase());
    const sectionLines = (endRel === -1) ? after : after.slice(0, endRel);
    const section = sectionLines.join("\n").trim();
    if (section) return section;
  }

  // Fallback slice
  const upper = raw.toUpperCase();
  const s = upper.indexOf(String(startHeader).toUpperCase());
  if (s === -1) return "";
  const afterStart = raw.slice(s + startHeader.length);
  const e = afterStart.toUpperCase().indexOf(String(endHeader).toUpperCase());
  if (e === -1) return "";
  return afterStart.slice(0, e).trim();
}

/* ===================== FUTURE-PROOF PARSER ===================== */

function parseSpeleoTripsFutureProof_(sectionText, year) {
  let s = String(sectionText || "")
    .replace(/\u00A0/g, " ")
    .replace(/\r/g, "\n")
    .replace(/[ \t]+/g, " ")
    .trim();

  s = s.replace(/\n{2,}/g, "\n");

  // Date markers
  const rangeRe = /(^|\n|\s)(\d{1,2})\.(\d{1,2})\.?\s*[–—-]\s*(\d{1,2})\.(\d{1,2})\.?(?:\s*[–—-]?\s*(20\d{2}|\d{2}))?\.?/g;
  const singleRe = /(^|\n|\s)(\d{1,2})\.(\d{1,2})\.?(?:\s*(20\d{2}|\d{2}))?\.?/g;

  const markers = [];

  let m;
  while ((m = rangeRe.exec(s)) !== null) {
    const leadLen = m[1] ? m[1].length : 0;
    markers.push({
      idx: m.index + leadLen,
      kind: "range",
      d1: Number(m[2]),
      mo1: Number(m[3]),
      d2: Number(m[4]),
      mo2: Number(m[5]),
      y: m[6] ? normalizeYear_(m[6]) : null
    });
  }

  let m2;
  while ((m2 = singleRe.exec(s)) !== null) {
    const leadLen = m2[1] ? m2[1].length : 0;
    markers.push({
      idx: m2.index + leadLen,
      kind: "single",
      d1: Number(m2[2]),
      mo1: Number(m2[3]),
      d2: null,
      mo2: null,
      y: m2[4] ? normalizeYear_(m2[4]) : null
    });
  }

  if (!markers.length) return [];

  markers.sort((a, b) => a.idx - b.idx);

  // dedupe by idx, prefer range
  const deduped = [];
  for (const mk of markers) {
    if (!deduped.length || deduped[deduped.length - 1].idx !== mk.idx) {
      deduped.push(mk);
    } else {
      const last = deduped[deduped.length - 1];
      if (last.kind === "single" && mk.kind === "range") deduped[deduped.length - 1] = mk;
    }
  }

  const trips = [];

  for (let i = 0; i < deduped.length; i++) {
    const cur = deduped[i];
    const start = cur.idx;
    const end = (i + 1 < deduped.length) ? deduped[i + 1].idx : s.length;

    let chunk = s.slice(start, end).trim();

    // remove leading date marker
    if (cur.kind === "range") {
      chunk = chunk.replace(/^\d{1,2}\.\d{1,2}\.?\s*[–—-]\s*\d{1,2}\.\d{1,2}\.?(?:\s*[–—-]?\s*(?:20\d{2}|\d{2}))?\.?/, "").trim();
    } else {
      chunk = chunk.replace(/^\d{1,2}\.\d{1,2}\.?(?:\s*(?:20\d{2}|\d{2}))?\.?/, "").trim();
    }

    chunk = chunk.replace(/^[\s•*\-–—:]+/, "").trim();

    const location = extractLocation_(chunk);
    if (!location) continue;

    const people = extractPeopleFutureProof_(chunk) || "(nije prepoznato)";

    const yFinal = cur.y || year || null;
    const dateStr = formatDateMarker_(cur, yFinal);

    trips.push({
      date: dateStr,
      location,
      people
    });
  }

  return trips;
}

function extractLocation_(chunk) {
  const c = String(chunk || "").trim();
  if (!c) return "";

  const dashParts = c.split(/\s*[–—-]\s*/);
  let loc = (dashParts[0] || "").trim();

  if (!loc || loc === c) {
    loc = c.split(",")[0].split(".")[0].trim();
  }

  loc = loc.replace(/^(bio|bila|bili|bile)\s+/i, "").trim();
  loc = loc.replace(/:$/, "").trim();

  if (loc.length < 2) return "";
  // Avoid obvious non-location starts
  if (/^(bili|bili su|ljudi|ekipa|sudjelovali)/i.test(loc)) return "";

  return loc;
}

/**
 * People extraction:
 * - label patterns: Ljudi/Ekipa/Sudjelovali/Prisustvovali/Bili su/Išli su ...
 * - parentheses: (...)
 * - fallback: after first dash
 *
 * IMPORTANT:
 * - we DO NOT cut at first comma (commas are used to separate names)
 * - we cut at "description keywords" (projekt, u okviru, instrumenti, ...)
 */
function extractPeopleFutureProof_(chunk) {
  const str = String(chunk || "");

  // 1) Label patterns (best)
  const labelRe = /\b(Ljudi|Ekipa|Sudjelovali|Sudjelovali su|Prisustvovali|Prisustvovali su|Bili su|Bili|Išli su|Išli|Isli su|Isli)\b\s*:?\s*([^.\n]+)(?:[.\n]|$)/i;
  const m = str.match(labelRe);
  if (m) {
    let peoplePart = (m[2] || "").trim();
    peoplePart = cutAtDescriptionKeywords_(peoplePart);
    peoplePart = cleanupPeopleText_(peoplePart);
    const list = splitPeopleList_(peoplePart);
    if (list.length) return list.join(", ");
  }

  // 2) Parentheses "(Marko, Ana, Ivo)"
  const paren = str.match(/\(([^)]+)\)/);
  if (paren) {
    let inside = cutAtDescriptionKeywords_(paren[1]);
    inside = cleanupPeopleText_(inside);
    const list = splitPeopleList_(inside);
    if (list.length) return list.join(", ");
  }

  // 3) Fallback: after first dash
  const dashSplit = str.split(/\s*[–—-]\s*/);
  if (dashSplit.length >= 2) {
    let peoplePart = dashSplit.slice(1).join(" - ").trim();
    // remove anything after "(" (notes)
    peoplePart = peoplePart.split("(")[0].trim();
    peoplePart = cutAtDescriptionKeywords_(peoplePart);
    peoplePart = peoplePart.replace(/^(bio|bila|bili|bile)\s+/i, "").trim();
    peoplePart = cleanupPeopleText_(peoplePart);
    const list = splitPeopleList_(peoplePart);
    if (list.length) return list.join(", ");
  }

  return "";
}

function cutAtDescriptionKeywords_(text) {
  let t = String(text || "").trim();
  if (!t) return "";

  // keywords that typically start "description" part (not people list)
  const kw = [
    "u okviru", "u sklopu", "u svrhu",
    "projekt", "postavlj", "instrument", "opcija", "nuditi",
    "traj", "godin", "pa će", "pa ce",
    "obilaz", "tko nije", "ko nije", "koji nije", "koji nisu",
    "u planu", "plan", "izvješt", "izvjest",
    "odrad", "radili", "radilo", "radili su"
  ];

  const lower = t.toLowerCase();
  let cutPos = -1;

  for (const k of kw) {
    const p = lower.indexOf(k);
    if (p !== -1 && (cutPos === -1 || p < cutPos)) cutPos = p;
  }

  if (cutPos !== -1) {
    t = t.slice(0, cutPos).replace(/[,\s]+$/g, "").trim();
  }

  // remove leading "bio/bili"
  t = t.replace(/^(bio|bila|bili|bile)\s+/i, "").trim();

  return t;
}

function cleanupPeopleText_(s) {
  let t = String(s || "")
    .replace(/\u00A0/g, " ")
    .replace(/\s{2,}/g, " ")
    .trim();

  // normalize " i " -> ", "
  t = t.replace(/\s+i\s+/g, ", ");

  return t;
}

function splitPeopleList_(s) {
  const parts = String(s || "")
    .split(/,|;/)
    .map(x => x.trim())
    .filter(Boolean);

  // Filter obvious non-people tokens (keep conservative)
  const badRe = /\b(projekt|instrument|opcije|nuditi|obilaz|u okviru|u sklopu|traje|godin|samograd|provala)\b/i;

  return parts.filter(p => p.length >= 2 && !badRe.test(p));
}

function normalizeYear_(y) {
  const yy = Number(y);
  if (!yy) return null;
  if (yy < 100) return 2000 + yy;
  return yy;
}

function formatDateMarker_(mk, year) {
  if (mk.kind === "range") {
    const left = `${String(mk.d1).padStart(2, "0")}.${String(mk.mo1).padStart(2, "0")}.`;
    const right = `${String(mk.d2).padStart(2, "0")}.${String(mk.mo2).padStart(2, "0")}.`;
    return year ? `${left}-${right}${year}` : `${left}-${right}`;
  }
  const d = `${String(mk.d1).padStart(2, "0")}.${String(mk.mo1).padStart(2, "0")}.`;
  return year ? `${d}${year}` : d;
}

function extractYear_(subject, text) {
  const m = (String(subject || "") + "\n" + String(text || "")).match(/\b(20\d{2})\b/);
  return m ? Number(m[1]) : null;
}

/* ===================== GMAIL / ATTACHMENT ===================== */

function pickDocxAttachmentStrict_(msg, namePrefix) {
  const atts = msg.getAttachments({ includeInlineImages: false, includeAttachments: true });
  return atts.find(a => {
    const name = (a.getName() || "").trim();
    return startsWithIgnoreCase_(name, namePrefix) && /\.docx$/i.test(name);
  }) || null;
}

/* ===================== DOCX → TEXT (Drive API v2) ===================== */

function convertDocxAttachmentToText_(attachment, trashAfter) {
  const blob = attachment.copyBlob();

  const resource = {
    title: attachment.getName(),
    mimeType: MimeType.GOOGLE_DOCS
  };

  const file = Drive.Files.insert(resource, blob, { convert: true });

  const doc = DocumentApp.openById(file.id);
  const text = (doc.getBody().getText() || "").replace(/\u00A0/g, " ").trim();

  if (trashAfter) {
    DriveApp.getFileById(file.id).setTrashed(true);
  }
  return text;
}

/* ===================== UTIL ===================== */

function startsWithIgnoreCase_(s, prefix) {
  return String(s || "").toLowerCase().startsWith(String(prefix || "").toLowerCase());
}

function containsIgnoreCase_(s, part) {
  return String(s || "").toLowerCase().includes(String(part || "").toLowerCase());
}
