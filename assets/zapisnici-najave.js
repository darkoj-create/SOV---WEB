(function(){
  'use strict';
  const els = {
    file: document.getElementById('docxFile'),
    source: document.getElementById('sourceMode'),
    parse: document.getElementById('parseBtn'),
    preview: document.getElementById('previewBtn'),
    refresh: document.getElementById('refreshBtn'),
    status: document.getElementById('importStatus'),
    minutes: document.getElementById('minutesList'),
    anns: document.getElementById('annList'),
    gmailLog: document.getElementById('gmailLog')
  };
  let currentFilter = 'all';
  let state = {minutes:[], announcements:[], gmailLog:[], preview:null};
  const monthByName = {'sijecanj':1,'siječanj':1,'veljaca':2,'veljača':2,'ozujak':3,'ožujak':3,'travanj':4,'svibanj':5,'lipanj':6,'srpanj':7,'kolovoz':8,'rujan':9,'listopad':10,'studeni':11,'prosinac':12};
  function esc(v){return String(v==null?'':v).replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));}
  function slug(s){return String(s||'zapisnik').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-z0-9.]+/g,'-').replace(/-+/g,'-').replace(/^-|-$/g,'').slice(0,120)||'zapisnik';}
  function setStatus(msg, cls){els.status.className='status '+(cls||''); els.status.innerHTML=msg;}
  function iso(y,m,d){if(!y||!m||!d) return null; return `${String(y).padStart(4,'0')}-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')}`;}
  function fmtDate(v){if(!v) return '—'; const s=String(v).slice(0,10); const m=s.match(/^(\d{4})-(\d{2})-(\d{2})$/); return m?`${m[3]}.${m[2]}.${m[1]}.`:s;}
  function fileBytes(n){n=Number(n||0); if(n>=1048576)return(n/1048576).toFixed(1).replace('.0','')+' MB'; if(n>=1024)return Math.round(n/1024)+' KB'; return n+' B';}
  async function sha256(file){
    const buf = await file.arrayBuffer();
    const hash = await crypto.subtle.digest('SHA-256', buf);
    return Array.from(new Uint8Array(hash)).map(b=>b.toString(16).padStart(2,'0')).join('');
  }
  function normalizeText(s){return String(s||'').replace(/\r/g,'').replace(/[\u00a0\t]+/g,' ').replace(/[ ]{2,}/g,' ').replace(/\n[ \t]+/g,'\n').trim();}
  async function extractDocxText(file){
    if(!window.JSZip) throw new Error('JSZip nije učitan.');
    const zip = await JSZip.loadAsync(file);
    const doc = zip.file('word/document.xml');
    if(!doc) throw new Error('DOCX nema word/document.xml.');
    const xml = await doc.async('text');
    const dom = new DOMParser().parseFromString(xml, 'application/xml');
    const paras = Array.from(dom.getElementsByTagName('w:p')).map(p=>{
      const pieces = [];
      Array.from(p.childNodes).forEach(node=>walkNode(node,pieces));
      return pieces.join('').replace(/\s+/g,' ').trim();
    }).filter(Boolean);
    return paras.join('\n');
  }
  function walkNode(node, out){
    if(!node) return;
    const name = node.nodeName;
    if(name === 'w:t') out.push(node.textContent || '');
    else if(name === 'w:tab') out.push(' ');
    else if(name === 'w:br' || name === 'w:cr') out.push('\n');
    else Array.from(node.childNodes||[]).forEach(ch=>walkNode(ch,out));
  }
  function parseMeetingDate(text, filename){
    const hay = `${text}\n${filename||''}`;
    let m = hay.match(/Zapisnik\s+sastanka\s+(\d{1,2})[._\s]*(\d{1,2})[._\s]*(\d{4})/i);
    if(m) return iso(Number(m[3]),Number(m[2]),Number(m[1]));
    m = hay.match(/(\d{4})[-_. ](\d{1,2})[-_. ](\d{1,2})/);
    if(m) return iso(Number(m[1]),Number(m[2]),Number(m[3]));
    m = hay.match(/(\d{1,2})[-_. ](\d{1,2})[-_. ](\d{4})/);
    if(m) return iso(Number(m[3]),Number(m[2]),Number(m[1]));
    return new Date().toISOString().slice(0,10);
  }
  function extractTail(text, label){
    const re = new RegExp(label+'\\s*:?\\s*([^\\n]+)', 'i');
    const m = text.match(re);
    return m ? m[1].trim() : '';
  }
  function extractSection(lines, startTitle, stopTitles){
    let start=-1, end=lines.length;
    for(let i=0;i<lines.length;i++){ if(lines[i].trim().toUpperCase()===startTitle){ start=i+1; break; } }
    if(start<0) return {text:'',lines:[]};
    for(let i=start;i<lines.length;i++){
      const u=lines[i].trim().toUpperCase();
      if(stopTitles.includes(u)){ end=i; break; }
    }
    const out = lines.slice(start,end).map(x=>x.trim()).filter(Boolean);
    return {text:out.join('\n'), lines:out};
  }
  function parseAnnouncements(text, meetingDate){
    const clean = normalizeText(text);
    const lines = clean.split('\n').map(x=>x.trim()).filter(Boolean);
    const section = extractSection(lines,'NAJAVE',['RAZNO','PRIJAVE','SPELEO IZLETI','INI IZLETI','SASTANAK VODIO','SASTANAK VODILA']);
    const year = Number((meetingDate||'').slice(0,4)) || new Date().getFullYear();
    const meetingMonth = Number((meetingDate||'').slice(5,7)) || null;
    const rows = [];
    for(const line of section.lines){
      const row = parseAnnouncementLine(line, year, meetingMonth, meetingDate);
      if(row) rows.push(row);
    }
    return {announcements:rows, announcementsText:section.text};
  }
  function normalizeLeaderName(name){
    const raw = String(name||'').trim().replace(/[.,;:]+$/,'');
    const map = {
      'Daliboru':'Dalibor','Daliborom':'Dalibor','Dalibor':'Dalibor',
      'Petri':'Petra','Petra':'Petra',
      'Dori':'Dora','Dora':'Dora',
      'Veniu':'Venio','Veniju':'Venio','Venio':'Venio',
      'Ličku':'Ličko','Licku':'Ličko','Ličko':'Ličko',
      'Dališi':'Dališa','Dališa':'Dališa','Dalisa':'Dališa',
      'Čedi':'Čedo','Čedo':'Čedo','Cedi':'Čedo','Cedo':'Čedo'
    };
    return map[raw] || raw.replace(/u$/,'').trim();
  }
  function normalizeLeaders(text){
    return String(text||'').split(/\s+i\s+|,|\/|&/i).map(normalizeLeaderName).filter(Boolean).join(' i ');
  }
  function cleanAnnouncementRest(rest){
    return String(rest||'')
      .replace(/^\s*[,.:;–—-]+\s*/,'')
      .replace(/^\([^)]*\)\s*,?\s*/,'')
      .replace(/^\s*(?:je\s+)?/i,'')
      .trim();
  }
  function validDate(y,m,d){ return !!(y && m>=1 && m<=12 && d>=1 && d<=31); }
  function parseDatePrefix(s, defaultYear, defaultMonth){
    const original = String(s||'').replace(/\s+/g,' ').trim();
    const norm = original.replace(/[–—]/g,'-');
    let m, d1, d2, mo1, mo2, y1, y2, rest;

    // 4.-7.6. / 4. - 7. 6. 2026. / 10.-12.-7.
    m = norm.match(/^(\d{1,2})\.\s*-\s*(\d{1,2})\.\s*-?\s*(\d{1,2})\.\s*(\d{4})?\.?\s*[,.:]?\s*(.*)$/);
    if(m){
      d1=Number(m[1]); d2=Number(m[2]); mo1=mo2=Number(m[3]); y1=y2=Number(m[4]||defaultYear); rest=m[5]||'';
      if(validDate(y1,mo1,d1) && validDate(y2,mo2,d2)) return {start:iso(y1,mo1,d1), end:iso(y2,mo2,d2), rest:cleanAnnouncementRest(rest), confidence:0.28, dateWeak:false};
    }

    // 20. - 21. 6. 2026. Burinka
    m = norm.match(/^(\d{1,2})\.\s*-\s*(\d{1,2})\.\s*(\d{1,2})\.\s*(\d{4})\.?\s*[,.:]?\s*(.*)$/);
    if(m){
      d1=Number(m[1]); d2=Number(m[2]); mo1=mo2=Number(m[3]); y1=y2=Number(m[4]); rest=m[5]||'';
      if(validDate(y1,mo1,d1) && validDate(y2,mo2,d2)) return {start:iso(y1,mo1,d1), end:iso(y2,mo2,d2), rest:cleanAnnouncementRest(rest), confidence:0.28, dateWeak:false};
    }

    // 06.06. - 07.06. / 25.07.-09.08. / 16.9. - 20.9.
    m = norm.match(/^(\d{1,2})\.\s*(\d{1,2})\.\s*(\d{4})?\.?\s*(?:-\s*(\d{1,2})\.\s*(?:(\d{1,2})\.)?\s*(\d{4})?\.?)?\s*[,.:]?\s*(.*)$/);
    if(m){
      d1=Number(m[1]); mo1=Number(m[2]); y1=Number(m[3]||defaultYear);
      d2=m[4]?Number(m[4]):null; mo2=m[5]?Number(m[5]):mo1; y2=Number(m[6]||y1); rest=m[7]||'';
      if(validDate(y1,mo1,d1) && (!d2 || validDate(y2,mo2,d2))) return {start:iso(y1,mo1,d1), end:d2?iso(y2,mo2,d2):iso(y1,mo1,d1), rest:cleanAnnouncementRest(rest), confidence:0.30, dateWeak:false};
    }

    // 20. - 21. 2026. (month missing; use meeting month)
    m = norm.match(/^(\d{1,2})\.\s*-\s*(\d{1,2})\.\s*(\d{4})\.?\s*[,.:]?\s*(.*)$/);
    if(m && defaultMonth){
      d1=Number(m[1]); d2=Number(m[2]); mo1=mo2=defaultMonth; y1=y2=Number(m[3]); rest=m[4]||'';
      if(validDate(y1,mo1,d1) && validDate(y2,mo2,d2)) return {start:iso(y1,mo1,d1), end:iso(y2,mo2,d2), rest:cleanAnnouncementRest(rest), confidence:0.18, dateWeak:true};
    }

    return {start:null, end:null, rest:original, confidence:0, dateWeak:true};
  }
  const KNOWN_PLACES = [
    {canon:'Homoljačko polje kod Plitvica', rx:/Homolj[aā]čko\s+polje(?:\s+kod\s+Plitvica)?/i},
    {canon:'Žumberačko gorje', rx:/Žumberačko\s+gorje/i},
    {canon:'Duman i Burinka', rx:/Duman\s+i\s+Burinka/i},
    {canon:'Bijele sige na Medvednici', rx:/Bijele\s+sige(?:\s+na\s+Medvednici)?/i},
    {canon:'Sjeverna Makedonija (BCC)', rx:/Sjeverna\s+Makedonija(?:\s*\(BCC\))?|BCC/i},
    {canon:'Sjeverni Velebit', rx:/Sjeverni\s+Velebit/i},
    {canon:'Munižaba', rx:/Munižab[aiu]?/i},
    {canon:'Ponorac', rx:/Ponor(?:ac|cu|ca)/i},
    {canon:'Burinka', rx:/Burink[aiu]?/i},
    {canon:'Duman', rx:/Duman/i},
    {canon:'Bunovac', rx:/Bunovac/i},
    {canon:'Krasno', rx:/Krasn[ou]/i},
    {canon:'Ratkovo', rx:/Ratkovom?/i},
    {canon:'Žica', rx:/Žic[aiu]?/i}
  ];
  function knownPlaceIn(text){
    const s = String(text||'');
    let best = null;
    for(const p of KNOWN_PLACES){
      const m = s.match(p.rx);
      if(m){ const idx = m.index == null ? 99999 : m.index; if(!best || idx < best.idx) best = {canon:p.canon, match:m[0], idx}; }
    }
    return best;
  }
  function firstMeaningfulChunk(rest){
    const r = String(rest||'').trim();
    const dash = r.search(/\s+[–—-]\s+|:\s+/);
    if(dash >= 0) return r.slice(0,dash).replace(/[:–—-]+$/,'').trim();
    const comma = r.indexOf(',');
    if(comma > 2 && comma < 90) return r.slice(0, comma).trim();
    return r;
  }
  function isGenericChunk(chunk){
    const c = String(chunk||'').trim();
    if(!c || c.length > 90) return true;
    if(/^(od\s+\d|se\s+|bi\s+|je\s+|ako\s+|treba\s+|ide\s+se\s+|slobodno\s+)/i.test(c)) return true;
    if(/\b(vodi|najavljuje|organizira|javite|javiti|okupljamo|mogla|postavljala|seminar|stručni skup|akcija)\b/i.test(c)) return true;
    if(/^[A-ZČĆŽŠĐ][a-zčćžšđ]+$/.test(c)) return true;
    return false;
  }
  function parseAnnouncementLine(line, defaultYear, defaultMonth, meetingDate){
    let s = line.replace(/\s+/g,' ').trim();
    const dp = parseDatePrefix(s, defaultYear, defaultMonth);
    let rest = dp.rest || s;
    let start = dp.start, end = dp.end, dateWeak = dp.dateWeak;
    if(!rest || rest.length < 3) return null;

    let category = detectCategory(rest, '');
    let leader = detectLeader(rest);
    let chunk = firstMeaningfulChunk(rest);
    let place = knownPlaceIn(rest);
    let title = chunk;
    let location = chunk;

    if(isGenericChunk(chunk) && place){ title = place.canon; location = place.canon; }
    else if(place && place.idx <= 25 && chunk.length > place.match.length + 20){ title = place.canon; location = place.canon; }

    const special = refineTitleLocation(rest, title, category, place);
    title = special.title || title;
    location = special.location || location || title;
    category = special.category || category;

    const desc = rest.replace(/^\s*[,.:;–—-]+\s*/,'').trim();
    let confidence = 0.35 + (dp.confidence || 0);
    if(title && !isGenericChunk(title)) confidence += 0.18;
    if(start) confidence += 0.17;
    if(leader) confidence += 0.06;
    if(place) confidence += 0.08;
    if(dateWeak) confidence -= 0.12;
    if(isGenericChunk(title) && !place) confidence -= 0.18;
    confidence = Math.max(0.2, Math.min(0.98, confidence));

    return {raw_text:line, title: title || 'Najava iz zapisnika', location_name:location || title || '', start_date:start, end_date:end||start, leader_name:leader, trip_category:category, description:desc, confidence:Number(confidence.toFixed(2)), status: confidence < .66 ? 'treba_provjeru' : 'novo', meta:{date_weak:dateWeak, place_detected:place ? place.canon : null}};
  }
  function detectCategory(text,title){
    const h=(text+' '+title).toLowerCase();
    if(/predavanje|prezentacij/.test(h)) return 'predavanje';
    if(/seminar|stručni skup|skup/.test(h)) return 'seminar';
    if(/vježb|samospašavanje|žici|žica/.test(h)) return 'vježba';
    if(/ekspedicij|predekspedicij|bcc|camp/.test(h)) return 'ekspedicija';
    if(/akcija|inventur|postavljala bi se žica|postavljanje žice/.test(h)) return 'akcija';
    return 'izlet';
  }
  function refineTitleLocation(rest, title, category, place){
    const clean = String(rest||'').replace(/^\([^)]*\)\s*/,'').trim();
    if(/od\s+\d{1,2}\s+sati.*Žic[aiu]?/i.test(clean)) return {title:'Žica – vježba samospašavanja', location:'Žica', category:'vježba'};
    let m;
    m = clean.match(/odlazak\s+u\s+jamu\s+([^,.;]+?)(?:,|\s+vježbat|$)/i);
    if(m) return {title:m[1].trim(), location:m[1].trim(), category: category || 'izlet'};
    if(/akcija\s+na\s+Ratkovom/i.test(clean)) return {title:'Akcija na Ratkovom', location:'Ratkovo / Mrkopalj', category:'akcija'};
    if(/predekspedicija/i.test(clean)) return {title:'Predekspedicija – postavljanje žice', location:'Sjeverni Velebit', category:'ekspedicija'};
    if(category === 'seminar'){
      m = clean.match(/stručni\s+skup\s+u\s+([^,.;]+?)(?:\s+na\s+koji|\s+ako|$)/i);
      if(m) return {title:'Stručni skup – '+normalizePlaceCase(m[1].trim()), location:normalizePlaceCase(m[1].trim()), category:'seminar'};
      if(place) return {title:'Seminar – '+place.canon, location:place.canon, category:'seminar'};
    }
    if(category === 'predavanje'){
      if(place && place.canon === 'Žica') return {title:'Žica – predavanje prve pomoći', location:'Žica', category:'predavanje'};
      m = clean.match(/predavanje\s+(?:o\s+)?([^,.;]+)/i);
      if(m) return {title:'Predavanje: '+m[1].trim(), location:'Velebit', category:'predavanje'};
    }
    if(place && isGenericChunk(title)) return {title:place.canon, location:place.canon, category};
    return {title, location:title, category};
  }
  function normalizePlaceCase(s){
    const p = knownPlaceIn(s);
    return p ? p.canon : s;
  }
  function detectLeader(text){
    const patterns = [
      /([A-ZČĆŽŠĐ][a-zčćžšđ]+)\s+(?:najavljuje|vodi|organizira|podsjeća)/,
      /javiti\s+se\s+([^.;,]+?)(?:\s+ako|\s+za|[.;,]|$)/i,
      /javite\s+se\s+([^.;,]+?)(?:\s+ako|\s+za|[.;,]|$)/i,
      /neka\s+mu\s+se\s+javi/i
    ];
    for(const p of patterns){
      const m=text.match(p);
      if(m){
        if(p.source.includes('neka')) return 'Dališa';
        return normalizeLeaders(m[1]);
      }
    }
    return '';
  }
  async function parseFile(file){
    const text = normalizeText(await extractDocxText(file));
    const checksum = await sha256(file);
    const meetingDate = parseMeetingDate(text, file.name);
    const year = Number(meetingDate.slice(0,4)), month=Number(meetingDate.slice(5,7)), day=Number(meetingDate.slice(8,10));
    const title = `Zapisnik sastanka ${fmtDate(meetingDate)}`;
    const meetingLeader = extractTail(text,'Sastanak vodio') || extractTail(text,'Sastanak vodila');
    const minutesTaker = extractTail(text,'Zapisnik vodio') || extractTail(text,'Zapisnik vodila');
    const parsed = parseAnnouncements(text, meetingDate);
    const path = `zapisnici/sastanci/${year}/${meetingDate}-${slug(file.name)}`;
    return {file,text,checksum,meetingDate,year,month,day,title,meetingLeader,minutesTaker,announcementsText:parsed.announcementsText,announcements:parsed.announcements,storagePath:path};
  }
  async function previewOnly(){
    const file = els.file.files && els.file.files[0];
    if(!file){ setStatus('Odaberi DOCX zapisnik.', 'bad'); return; }
    try{
      els.preview.disabled=true; setStatus('Čitam DOCX...', '');
      state.preview = await parseFile(file);
      setStatus(`Preview OK: <strong>${esc(state.preview.title)}</strong><br/>Najava pronađeno: <strong>${state.preview.announcements.length}</strong>`, 'ok');
      renderAnnouncements(state.preview.announcements.map((a,i)=>({...a,id:'preview-'+i,source_document_title:state.preview.title,source_meeting_date:state.preview.meetingDate,preview:true})));
    }catch(err){ setStatus('Greška previewa: '+esc(err.message||err), 'bad'); }
    finally{ els.preview.disabled=false; }
  }
  async function importFile(){
    const file = els.file.files && els.file.files[0];
    if(!file){ setStatus('Odaberi DOCX zapisnik.', 'bad'); return; }
    await SOVAuth.ready();
    const sb = SOVAuth.getClient();
    if(!sb){ setStatus('Supabase nije konfiguriran.', 'bad'); return; }
    try{
      els.parse.disabled=true; setStatus('Čitam i parsiram DOCX...', '');
      const p = await parseFile(file);
      let existing = null;
      const check = await sb.from('meeting_minutes').select('*').eq('checksum_sha256', p.checksum).maybeSingle();
      if(check.error) throw check.error;
      if(check.data) existing = check.data;
      let docId = existing && existing.document_id;
      if(!existing){
        setStatus('Uploadam DOCX u arhivu...', '');
        const up = await sb.storage.from('sov-documents').upload(p.storagePath, file, {cacheControl:'3600', upsert:true, contentType:file.type || 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'});
        if(up.error) throw up.error;
        const docPayload = {collection:'zapisnici_sastanaka', title:p.title, document_type:'zapisnik sastanka', document_date:p.meetingDate, year:p.year, month:p.month, day:p.day, original_filename:file.name, storage_bucket:'sov-documents', storage_path:p.storagePath, mime_type:file.type || null, format:'DOCX', size_bytes:file.size, checksum_sha256:p.checksum, source_batch:'minutes-announcements-v6.1.40', tags:['zapisnik','najave'], summary:`Automatski uvoz zapisnika. Najava pronađeno: ${p.announcements.length}.`, ocr_text:p.text, status:'active', visibility:'members'};
        const insDoc = await sb.from('sov_document_archive').insert(docPayload).select('id').single();
        if(insDoc.error) throw insDoc.error;
        docId = insDoc.data.id;
        const mmPayload = {document_id:docId, meeting_date:p.meetingDate, title:p.title, original_filename:file.name, storage_bucket:'sov-documents', storage_path:p.storagePath, checksum_sha256:p.checksum, plain_text:p.text, announcements_text:p.announcementsText, meeting_leader:p.meetingLeader || null, minutes_taker:p.minutesTaker || null, source:els.source.value || 'manual_upload', status:'parsed', parsed_at:new Date().toISOString(), meta:{file_size:file.size, announcements_count:p.announcements.length}};
        const insMm = await sb.from('meeting_minutes').insert(mmPayload).select('*').single();
        if(insMm.error) throw insMm.error;
        existing = insMm.data;
      }else{
        setStatus('Zapisnik već postoji — osvježavam najave koje nisu odobrene...', '');
      }
      await sb.from('trip_announcements_staging').delete().eq('meeting_minutes_id', existing.id).neq('status','odobreno');
      const rows = p.announcements.map(a=>({meeting_minutes_id:existing.id, document_id:docId, source_document_title:p.title, source_meeting_date:p.meetingDate, raw_text:a.raw_text, title:a.title, location_name:a.location_name, start_date:a.start_date, end_date:a.end_date, leader_name:a.leader_name || null, trip_category:a.trip_category, description:a.description, confidence:a.confidence, status:a.status, meta:a.meta || {}}));
      if(rows.length){
        const insA = await sb.from('trip_announcements_staging').insert(rows).select('id');
        if(insA.error) throw insA.error;
        const inserted = insA.data || [];
        for(const r of inserted){
          try{ await sb.rpc('sov_recompute_trip_announcement_dedupe',{p_announcement_id:r.id}); }
          catch(e){ console.warn('dedupe recompute failed', e); }
        }
      }
      setStatus(`Spremljeno: <strong>${esc(p.title)}</strong><br/>Najava u stagingu: <strong>${rows.length}</strong>`, 'ok');
      await loadAll();
    }catch(err){ setStatus('Greška uvoza: '+esc(err.message||err), 'bad'); }
    finally{ els.parse.disabled=false; }
  }
  function renderMinutes(){
    if(!state.minutes.length){ els.minutes.innerHTML='<div class="empty">Još nema zapisnika u novoj arhivi.</div>'; return; }
    els.minutes.innerHTML = state.minutes.map(m=>`<div class="minute-card"><strong>${esc(m.title)}</strong><div class="meta"><span class="pill good">${fmtDate(m.meeting_date)}</span><span class="pill">${esc(m.status)}</span><span class="pill">${esc(m.original_filename||'DOCX')}</span></div>${m.meeting_leader||m.minutes_taker?`<div class="muted" style="margin-top:8px">Vodio/la: ${esc(m.meeting_leader||'—')} · zapisnik: ${esc(m.minutes_taker||'—')}</div>`:''}</div>`).join('');
  }

  function renderGmailLog(){
    if(!els.gmailLog) return;
    const logs = state.gmailLog || [];
    if(!logs.length){ els.gmailLog.innerHTML='<div class="empty">Nema Gmail uvoza u logu.</div>'; return; }
    els.gmailLog.innerHTML = logs.map(l=>`<div class="minute-card"><strong>${esc(l.attachment_name||l.subject||'Gmail zapisnik')}</strong><div class="meta"><span class="pill ${l.status==='imported'?'good':(String(l.status||'').includes('duplicate')?'warn':'')}">${esc(l.status||'received')}</span><span class="pill">${esc((l.created_at||'').slice(0,16).replace('T',' '))}</span></div><div class="muted" style="margin-top:8px">${esc(l.subject||'')}<br/>${esc(l.from_email||'')} ${l.message?'· '+esc(l.message):''}</div></div>`).join('');
  }
  function statusPill(st){
    if(st==='odobreno') return 'good'; if(st==='treba_provjeru') return 'warn'; if(st==='odbijeno'||st==='duplikat') return 'bad'; return '';
  }
  function dedupePanel(a){
    if(!a || a.preview) return '';
    const st = a.dedupe_status || 'new';
    if(st === 'possible_duplicate'){
      const score = a.duplicate_score ? Math.round(Number(a.duplicate_score))+'%' : '—';
      return `<div class="status bad" style="margin-top:10px"><strong>Mogući duplikat</strong><br/>${esc(a.duplicate_reason||'Ova najava sliči postojećoj najavi ili izletu.')}<div class="meta"><span class="pill bad">Sličnost ${esc(score)}</span>${a.matched_announcement_id?'<span class="pill">spojivo sa staging najavom</span>':''}${a.matched_trip_id?'<span class="pill">sliči već objavljenom izletu</span>':''}</div></div>`;
    }
    if(st === 'confirmed_duplicate' || a.status === 'duplikat'){
      return `<div class="status bad" style="margin-top:10px"><strong>Duplikat</strong><br/>Ova najava je označena kao duplikat i neće se pretvarati u novi izlet.</div>`;
    }
    if(st === 'forced_new') return `<div class="status ok" style="margin-top:10px"><strong>Ipak nova najava</strong><br/>Ručno označeno da nije duplikat.</div>`;
    return '';
  }
  function renderAnnouncements(list){
    const data = list || state.announcements.filter(a=>currentFilter==='all' || a.status===currentFilter || (currentFilter==='possible_duplicate' && a.dedupe_status==='possible_duplicate'));
    if(!data.length){ els.anns.innerHTML='<div class="empty">Nema najava za ovaj filter.</div>'; return; }
    els.anns.innerHTML = data.map(a=>`<article class="ann-card" data-id="${esc(a.id)}"><div class="ann-head"><div><h3>${esc(a.title)}</h3><div class="meta"><span class="pill good">${fmtDate(a.start_date)}${a.end_date&&a.end_date!==a.start_date?' – '+fmtDate(a.end_date):''}</span><span class="pill ${statusPill(a.status)}">${esc(a.status)}</span><span class="pill">${esc(a.trip_category)}</span><span class="pill">${Math.round(Number(a.confidence||0)*100)}%</span>${a.dedupe_status&&a.dedupe_status!=='new'?`<span class="pill ${a.dedupe_status==='possible_duplicate'?'bad':'warn'}">${esc(a.dedupe_status)}</span>`:''}</div></div></div>${dedupePanel(a)}<div class="ann-grid"><div class="field"><label>Naziv</label><input data-field="title" value="${esc(a.title)}" ${a.preview?'disabled':''}/></div><div class="field"><label>Lokacija</label><input data-field="location_name" value="${esc(a.location_name||'')}" ${a.preview?'disabled':''}/></div><div class="field"><label>Voditelj</label><input data-field="leader_name" value="${esc(a.leader_name||'')}" ${a.preview?'disabled':''}/></div><div class="field"><label>Od</label><input data-field="start_date" type="date" value="${esc(a.start_date||'')}" ${a.preview?'disabled':''}/></div><div class="field"><label>Do</label><input data-field="end_date" type="date" value="${esc(a.end_date||'')}" ${a.preview?'disabled':''}/></div><div class="field"><label>Kategorija</label><select data-field="trip_category" ${a.preview?'disabled':''}>${['izlet','vježba','seminar','ekspedicija','predavanje','akcija'].map(x=>`<option value="${x}" ${a.trip_category===x?'selected':''}>${x}</option>`).join('')}</select></div></div><div class="field"><label>Opis</label><textarea data-field="description" ${a.preview?'disabled':''}>${esc(a.description||'')}</textarea></div><div class="raw">${esc(a.raw_text)}</div>${a.preview?'':`<div class="ann-actions"><button type="button" data-action="save">Spremi izmjene</button><button class="primary" type="button" data-action="approve" ${a.status==='duplikat'||a.dedupe_status==='confirmed_duplicate'?'disabled':''}>Odobri i stvori izlet</button>${a.dedupe_status==='possible_duplicate'?`<button type="button" data-action="force-new">Ipak novi izlet</button><button type="button" data-action="mark-dup">Spoji / označi duplikat</button>`:''}<button type="button" data-action="review">Treba provjeru</button><button type="button" data-action="reject">Odbij</button><button type="button" data-action="dup">Duplikat</button></div>`}</article>`).join('');
  }
  async function loadAll(){
    await SOVAuth.ready();
    const sb = SOVAuth.getClient();
    if(!sb) return;
    const mins = await sb.from('meeting_minutes').select('*').order('meeting_date',{ascending:false}).limit(50);
    state.minutes = mins.data || [];
    if(mins.error) console.warn(mins.error);
    const anns = await sb.from('trip_announcements_staging').select('*').order('start_date',{ascending:true}).order('created_at',{ascending:false}).limit(200);
    state.announcements = anns.data || [];
    if(anns.error) console.warn(anns.error);
    const gl = await sb.from('sov_gmail_minutes_import_log').select('*').order('created_at',{ascending:false}).limit(20);
    state.gmailLog = gl.data || [];
    if(gl.error) console.warn(gl.error);
    renderMinutes(); renderGmailLog(); renderAnnouncements();
  }
  async function updateAnnouncement(card, action){
    const sb = SOVAuth.getClient();
    const id = card.dataset.id;
    if(action==='approve'){
      if(!confirm('Stvoriti pravi izlet iz ove najave?')) return;
      const res = await sb.rpc('sov_approve_trip_announcement',{p_announcement_id:id});
      if(res.error) alert(res.error.message || res.error); else await loadAll();
      return;
    }
    if(action==='mark-dup' || action==='dup'){
      if(!confirm('Označiti ovu najavu kao duplikat/spojiti je s pronađenom najavom?')) return;
      const res = await sb.rpc('sov_mark_trip_announcement_duplicate',{p_announcement_id:id,p_canonical_announcement_id:null,p_reason:'Ručno označeno kroz web UI'});
      if(res.error) alert(res.error.message || res.error); else await loadAll();
      return;
    }
    if(action==='force-new'){
      const res = await sb.rpc('sov_force_trip_announcement_new',{p_announcement_id:id});
      if(res.error) alert(res.error.message || res.error); else await loadAll();
      return;
    }
    const patch = {updated_at:new Date().toISOString()};
    if(action==='save'){
      card.querySelectorAll('[data-field]').forEach(el=>{ patch[el.dataset.field] = el.value || null; });
    }else if(action==='review') patch.status='treba_provjeru';
    else if(action==='reject'){ patch.status='odbijeno'; patch.rejected_at=new Date().toISOString(); }
    const res = await sb.from('trip_announcements_staging').update(patch).eq('id',id);
    if(res.error) alert(res.error.message || res.error);
    else {
      if(action==='save') await sb.rpc('sov_recompute_trip_announcement_dedupe',{p_announcement_id:id}).catch(console.warn);
      await loadAll();
    }
  }
  document.addEventListener('click', e=>{
    const tab = e.target.closest('.tab');
    if(tab){ document.querySelectorAll('.tab').forEach(x=>x.classList.remove('active')); tab.classList.add('active'); currentFilter=tab.dataset.filter||'all'; renderAnnouncements(); return; }
    const btn = e.target.closest('button[data-action]');
    if(btn){ const card=btn.closest('.ann-card'); if(card) updateAnnouncement(card, btn.dataset.action); }
  });
  els.preview.addEventListener('click', previewOnly);
  els.parse.addEventListener('click', importFile);
  els.refresh.addEventListener('click', loadAll);
  loadAll().catch(console.warn);
})();
