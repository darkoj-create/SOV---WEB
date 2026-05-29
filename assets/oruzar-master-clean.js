(function(){
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]));
const norm=s=>String(s||'').trim();
const lower=s=>norm(s).toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'');
const synonyms={krol:'croll',crol:'croll',croll:'croll',busilica:'bušilica',busilice:'bušilica',pupak:'pupčano uže',pupcano:'pupčano uže',spit:'spit',anker:'sidrište'};
function qnorm(s){let x=lower(s); Object.entries(synonyms).forEach(([a,b])=>{x=x.replaceAll(a,lower(b));}); return x;}

function countInt(v, fallback=0){
  // Armory quantities are whole pieces. Old imports may contain strings, commas,
  // Excel/date fragments or decimals such as 201.2 / 854.05. Never display those.
  if(v === null || v === undefined || v === '') v = fallback;
  let raw = String(v).trim().replace(',', '.');
  // Values that look like month.year/date are not quantities. Fall back instead.
  if(/^\d{1,2}[./]\d{4}$/.test(raw) || /^\d{1,2}[./]\d{1,2}[./]\d{2,4}$/.test(raw)) raw = String(fallback ?? 0);
  let n = Number(raw);
  if(!Number.isFinite(n)) n = Number(fallback);
  if(!Number.isFinite(n)) return 0;
  n = Math.round(n);
  return n < 0 ? 0 : n;
}
function sumQty(rows, key){ return rows.reduce((s,r)=>s+countInt(r[key]),0); }
function fmtQty(v){ return String(countInt(v)); }

function canonicalCategory(raw,text){
  const r=lower(raw), t=lower([raw,text].filter(Boolean).join(' '));
  if((/descender|\bstop\b|rig|maestro|id['’]?s|croll|krol|crol|bloker|zumar|ascender|pojas|sjedal|pedal|stremen|prsni|pupak|pupcano/.test(t))&&!/penjack|penjac|alpinist|climbing/.test(t)) return 'Osobna oprema - komplet';
    if(/osob|kacig|helmet|kombinezon|odijel|rukavic|cizm|obuc/.test(t)) return 'Osobna oprema';
  if(/uzad|uzetna|\buze\b|rope|prusik|gurt|traka|kolotur|transportna vreca/.test(t) && !/busil|bater|punjac|svrd/.test(t)) return 'Užad i užetna oprema';
  if(/busil|baterija bosch|bosch.*bater|punjac|svrd|gbh18|gbh180|boschhammer/.test(t)) return 'Bušilice i baterije';
  if(/postavlj|spit|sidrist|ploc|ring|anker|bolt|karabiner|matica|hms/.test(t) && !/descender|croll|bloker|pojas/.test(t)) return 'Oprema za postavljanje';
  if(/crtan|mjeren|disto|kompas|topodroid|dokumentac|nacrt|skic/.test(t)) return 'Oprema za crtanje';
  if(/elektro|foto|kamera|video|rasvjet|svjetl|lampa|ceona|ceo/.test(t)) return 'Elektro i foto oprema';
  if(/medicin|medicina|prva pomoc|prva pom/.test(t)) return 'Medicinska oprema';
  if(/ronil|ronjenje|neopren|maska|peraj|boca/.test(t)) return 'Ronilačka oprema';
  if(/alpinist|alpin|penjack|penjac/.test(t)) return 'Alpinistička oprema';
  if(/cisto podzemlje|ciscenje|cistoc|otpad/.test(t)) return 'Čisto podzemlje';
  if(/prosir|prosirivanje|klin|cekic|macol|dlijet|stem/.test(t)) return 'Oprema za proširivanje';
  if(/logor|kamp|ekspedic|sator|kuhal|plin|podlog|vreca za spavanje/.test(t)) return 'Oprema za logor';
  if(/alat|kljuc|odvijac|klijest|toolbox/.test(t)) return 'Ostali alat';
  if(/ostalo|razno/.test(r)) return 'Ostalo';
  return norm(raw)||'Ostalo';
}

function normalizeArticleName(name){
  let x=lower(name).replace(/[()\[\]{}]/g,' ').replace(/[+_\/\\,;:]+/g,' ').replace(/\s+/g,' ').trim();
  x=x.replace(/\b(krol|crol|croll)\b/g,'croll').replace(/\bpupak\b|\bpupcano\s+u?ze\b/g,'pupcano uze');
  if(/\bcroll\b/.test(x)){ if(/\b(s|small|velicina s)\b/.test(x))return 'croll s'; if(/\b(l|large|velicina l)\b/.test(x))return 'croll l'; return 'croll'; }
  if(/\bstremen\b|\bpedala\b/.test(x)) return 'stremen';
  if(/\bpupcano uze\b/.test(x)) return 'pupcano uze';
  if(/\bprusik\b/.test(x)) return 'prusik';
  if(/\bbloker\b|\bascender\b|\bjumar\b|\bzumar\b/.test(x)) return 'bloker';
  if(/\bstop\b/.test(x)) return 'stop descender';
  return x;
}
function articleMergeKey(r){return lower(r.category)+'|'+lower(r.subcategory||'Ostalo')+'|'+normalizeArticleName(r.name)}
function mergeDisplayRows(rows){
  const out=[]; const map=new Map();
  for(const r of rows){
    if(r.type==='rope'){out.push(r); continue;}
    const key=articleMergeKey(r);
    if(!map.has(key)){map.set(key,{...r}); out.push(map.get(key));}
    else{const cur=map.get(key); cur.qty=countInt(cur.qty)+countInt(r.qty); cur.av=countInt(cur.av)+countInt(r.av); cur.loan=countInt(cur.loan)+countInt(r.loan); if(!cur.location&&r.location)cur.location=r.location; cur.search=qnorm([cur.search,r.search].join(' '));}
  }
  return out;
}
function toast(m){let t=document.getElementById('cmToast'); if(!t){t=document.createElement('div');t.id='cmToast';t.className='cm-toast';document.body.appendChild(t)} t.textContent=m;t.classList.add('show');clearTimeout(t._to);t._to=setTimeout(()=>t.classList.remove('show'),2300)}
function iconFor(t){t=lower(t); if(t.includes('osobna oprema - komplet'))return '🧗'; if(t.includes('osobna'))return '🧑‍🚒'; if(t.includes('už')||t.includes('uz'))return '🪢'; if(t.includes('postav')||t.includes('spit')||t.includes('sidri'))return '⚓'; if(t.includes('crtan')||t.includes('mjer'))return '📐'; if(t.includes('bus')||t.includes('buš')||t.includes('bater'))return '🔋'; if(t.includes('elektro')||t.includes('foto'))return '📷'; if(t.includes('alpin'))return '⛰️'; if(t.includes('med'))return '🧰'; if(t.includes('ronil'))return '🤿'; if(t.includes('kamp')||t.includes('logor')||t.includes('eksp'))return '⛺'; if(t.includes('čisto')||t.includes('cisto'))return '🧹'; if(t.includes('proš')||t.includes('pros'))return '🔨'; if(t.includes('alat'))return '🧰'; return '📦'}
let STATE={data:null,rows:[],cat:null,sub:null,query:'',requests:[]};
async function loadData(){
  if(STATE.data) return STATE.data;
  let d=null, source='fallback';
  try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.loadAllData){ d=await SOVArmoryDB.loadAllData(); if(d) source='supabase'; }}catch(e){console.warn('[clean master] supabase load failed',e)}
  if(!d){ try{ d=await fetch('data/oruzarstvo-data.json',{cache:'no-store'}).then(r=>r.json()); }catch(e){console.warn('[clean master] json load failed',e); d={items:[],ropes:[],pieces:[],categories:[]};}}
  STATE.data=d||{}; try{const extra=JSON.parse(localStorage.getItem('sov_armory_items_override')||'[]'); if(extra.length){STATE.data.items=[...(STATE.data.items||[]),...extra];}}catch(e){} STATE.source=source; STATE.rows=makeRows(STATE.data); console.info('[SOV armory] clean master v4.76 dedupe articles loaded', STATE.source, STATE.rows.length); return STATE.data;
}
function makeRows(d){const arr=[]; (d.items||[]).forEach((r,i)=>arr.push(row(r,'item',i))); (d.ropes||[]).forEach((r,i)=>arr.push(row(r,'rope',i))); (d.pieces||[]).forEach((r,i)=>arr.push(row(r,'piece',i))); return mergeDisplayRows(arr.filter(r=>r.name));}
function row(r,type,i){
  let qty=countInt(r.quantity ?? r.total ?? 1, 0);
  let av=countInt(r.available ?? r.available_qty ?? qty, qty);
  let loan=countInt(r.loaned ?? r.loaned_qty ?? Math.max(0,qty-av), 0);
  if(type==='rope'||type==='piece'){qty=1; av=/posu|vani|rashod|otpis|izgubl/i.test(String(r.status||''))?0:1; loan=/posu|vani/i.test(String(r.status||''))?1:0}
  // Counts in armory are always whole pieces. Decimal garbage from old imports is normalized here for display only.
  qty=countInt(qty); av=countInt(av); loan=countInt(loan);
return {raw:r,type,id:String(r.legacy_id||r.catalog_id||r.sku||r.id||`${type}-${i}`),name:norm(r.name||r.item_name||r.model||r.sku||'Artikl'),category:canonicalCategory(r.category||r.category_name||(type==='rope'?'Užad i užetna oprema':'Ostalo'), [r.name,r.item_name,r.model,r.subcategory,r.note,r.internal_note].join(' ')),subcategory:norm(r.subcategory||r.group||'Ostalo'),qty,av,loan,location:norm(r.location||r.location_name||''),status:norm(r.status||r.availability||'aktivno'),minimum:countInt(r.minimum ?? r.threshold ?? r.min_quantity ?? 0,0),search:qnorm([r.name,r.item_name,r.category,r.category_name,r.subcategory,r.sku,r.model,r.manufacturer,r.search_text,r.internal_note,r.note].join(' ')), variants:countInt(r.variant_count||1,1)};}
function filtered(){let rows=STATE.rows; if(STATE.query){const q=qnorm(STATE.query); rows=rows.filter(r=>r.search.includes(q)||q.split(/\s+/).every(p=>r.search.includes(p)));} return rows;}
function categories(rows=filtered()){const m=new Map(); rows.forEach(r=>{const c=r.category||'Ostalo'; if(!m.has(c))m.set(c,[]); m.get(c).push(r)}); return [...m.entries()].sort((a,b)=>a[0].localeCompare(b[0],'hr'));}
function subcategories(cat){const m=new Map(); filtered().filter(r=>r.category===cat).forEach(r=>{const s=r.subcategory||'Ostalo'; if(!m.has(s))m.set(s,[]); m.get(s).push(r)}); return [...m.entries()].sort((a,b)=>a[0].localeCompare(b[0],'hr'));}
function kpis(){const rows=STATE.rows; const cats=new Set(rows.map(r=>r.category)).size; const total=sumQty(rows,'qty'); const av=sumQty(rows,'av'); const loan=sumQty(rows,'loan'); return {cats,total,av,loan}}
function renderKpis(){const el=document.getElementById('cmKpis'); if(el){el.innerHTML=''; el.style.display='none';}}
function bindSearch(cb){const q=document.getElementById('cmSearch'); if(q){q.value=STATE.query; q.oninput=()=>{STATE.query=q.value;STATE.cat=null;STATE.sub=null;cb();}}}
function renderInventory(){const root=document.getElementById('inventoryRoot'); if(!root)return; renderKpis(); bindSearch(renderInventory); const cat=STATE.cat, sub=STATE.sub; let html=''; if(!cat){html=`<div class="cat-grid">${categories().map(([c,rs])=>`<button class="cat-tile" onclick="CleanArmory.pickCat('${esc(c)}')"><span class="ico">${iconFor(c)}</span><b>${esc(c)}</b><small>Otvori kategoriju</small></button>`).join('')}</div>`}
else if(!sub){html=`<div class="cm-breadcrumb"><button onclick="CleanArmory.pickCat('')">Sve kategorije</button><span>${esc(cat)}</span></div><div class="cat-grid">${subcategories(cat).map(([s,rs])=>`<button class="cat-tile" onclick="CleanArmory.pickSub('${esc(s)}')"><span class="ico">${iconFor(s)}</span><b>${esc(s)}</b><small>Otvori podkategoriju</small></button>`).join('')}</div>`}
else {const rows=filtered().filter(r=>r.category===cat&&r.subcategory===sub); html=`<div class="cm-breadcrumb"><button onclick="CleanArmory.pickCat('')">Sve kategorije</button><button onclick="CleanArmory.pickCat('${esc(cat)}')">${esc(cat)}</button><span>${esc(sub)}</span></div><div class="cm-tools"><button class="cm-btn primary" onclick="CleanArmory.newItem()">+ Dodaj artikl</button><button class="cm-btn" onclick="CleanArmory.pickSub('')">← Podkategorije</button></div><div class="item-grid">${rows.map(itemCard).join('')}</div>`}
 root.innerHTML=html||'<div class="empty">Nema artikala za prikaz.</div>';}
function itemCard(r){const low=r.minimum&&r.av<=r.minimum;return `<article class="item-card ${low?'low-stock':''}"><h3>${esc(r.name)}</h3><div class="muted">${esc(r.category)} · ${esc(r.subcategory)}</div><div class="badgetray"><span class="badge ${low?'bad':(r.av>0?'ok':'bad')}">${low?'ispod praga':esc(r.status)}</span><span class="badge">${esc(r.location||'bez lokacije')}</span>${r.variants&&r.variants>1?`<span class="badge">${esc(r.variants)} varijanti</span>`:''}${r.minimum?`<span class="badge warn">prag ${esc(r.minimum)}</span>`:''}${r.type==='rope'?'<span class="badge warn">kodirano uže</span>':''}</div><div class="cm-tools"><button class="cm-btn" onclick="CleanArmory.editItem('${esc(r.id)}')">Uredi artikl</button><button class="cm-btn bad" onclick="CleanArmory.removeItem('${esc(r.id)}')">Makni</button></div></article>`}
function renderMaster(){const root=document.getElementById('masterRoot'); if(!root)return; renderKpis(); root.innerHTML=`<div class="cm-grid"><a class="cm-card" href="oruzar-master-inventar.html"><span class="ico">📦</span><h2>Inventar</h2><p>Kategorije, podkategorije, artikli i lokacije. Brojevi se uređuju tek u formi artikla.</p><span>Otvori →</span></a><a class="cm-card" href="oruzar-master-inventura.html"><span class="ico">✅</span><h2>Inventura</h2><p>Nova inventura, datum, kategorije i potvrda stvarnog broja artikala.</p><span>Otvori →</span></a><a class="cm-card" href="oruzar-master-posudbe.html"><span class="ico">↔️</span><h2>Posudba</h2><p>Zatražene posudbe i izdano vani. Vraćeno nestaje iz aktivnog pregleda.</p><span>Otvori →</span></a><a class="cm-card" href="oruzar-master-notes.html"><span class="ico">📝</span><h2>Notes & Reminders</h2><p>Bilješke, podsjetnici i što treba nabaviti/obaviti.</p><span>Otvori →</span></a></div>`}
async function loadRequests(){let req=null; try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.loadRequests) req=await SOVArmoryDB.loadRequests(); }catch(e){console.warn('[clean master] load requests failed',e)} if(Array.isArray(req)){STATE.requests=req;STATE.reqSource='supabase';return req} try{STATE.requests=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]');STATE.reqSource='local';return STATE.requests}catch(e){STATE.requests=[];return []}}
function itemsLine(r){return (r.items||[]).map(i=>`<span class="badge">${esc(i.name||i.item_name||'Artikl')} × ${esc(i.quantity||1)}</span>`).join('')||'<span class="badge">bez stavki</span>'}
async function renderLoans(){const root=document.getElementById('loansRoot'); if(!root)return; const reqs=await loadRequests(); const pending=reqs.filter(r=>!/issued|izdano|prepared|partial|returned|vrac|vrać|cancel|odbij/i.test(String(r.status||'pending'))); const issued=reqs.filter(r=>/issued|izdano|prepared|partial/i.test(String(r.status||''))); root.innerHTML=`<div class="loan-grid"><section class="cm-panel"><h2>📝 Zatražene posudbe</h2><p class="muted">Web zahtjevi + ručni unos. Klik na izdano prebacuje u aktivno posuđeno.</p><div class="cm-tools"><button class="cm-btn" onclick="CleanArmory.renderLoans()">Osvježi</button></div><div class="loan-list">${pending.length?pending.map(r=>loanRow(r,'pending')).join(''):'<div class="empty">Nema novih zahtjeva.</div>'}</div><hr style="border-color:rgba(255,255,255,.08);margin:16px 0"><form class="cm-form" onsubmit="CleanArmory.manualLoan(event)"><h3>+ Ručni unos zahtjeva</h3><div class="cm-form-grid"><input class="cm-input" id="mUser" placeholder="Tko traži"><input class="cm-input" id="mItem" placeholder="Artikl"><input class="cm-input" id="mQty" type="number" min="1" value="1"></div><div class="cm-tools"><input class="cm-input" id="mNote" placeholder="Izlet / napomena"><button class="cm-btn primary">Dodaj zahtjev</button></div></form></section><section class="cm-panel"><h2>📦 Izdano vani</h2><p class="muted">Ovdje ostaje samo aktivno vani. Vraćeno se miče iz pregleda.</p><div class="loan-list">${issued.length?issued.map(r=>loanRow(r,'issued')).join(''):'<div class="empty">Ništa trenutno nije vani.</div>'}</div></section></div><div class="debug">Izvor zahtjeva: ${esc(STATE.reqSource||'nepoznato')}. SQL se ne refresh-a ručno za svaku akciju; treba samo jednom postaviti tablice/RLS.</div>`}
function loanRow(r,mode){return `<div class="loan-row"><div class="loan-row-top"><div><b>${esc(r.user||r.member_name||r.requester_name||'Član')}</b><div class="muted">${esc(r.trip||r.note||'Zahtjev')} · ${esc((r.created_at||'').slice(0,10))}</div></div><span class="badge ${mode==='issued'?'ok':'warn'}">${mode==='issued'?'vani':'za izdati'}</span></div><div class="loan-items">${itemsLine(r)}</div><div class="cm-tools">${mode==='pending'?`<button class="cm-btn primary" onclick="CleanArmory.issueLoan('${esc(r.id)}')">Označi izdano</button><button class="cm-btn" onclick="CleanArmory.setStatus('${esc(r.id)}','cancelled')">Zatvori</button>`:`<button class="cm-btn primary" onclick="CleanArmory.openReturn('${esc(r.id)}')">Označi vraćeno</button>`}</div></div>`}
async function setStatus(id,status){try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.updateRequestStatus) await SOVArmoryDB.updateRequestStatus(id,status); }catch(e){console.warn('[clean master] remote status failed',e); toast('Supabase update nije prošao; spremam lokalno.')} try{const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]'); const r=l.find(x=>String(x.id)===String(id)); if(r)r.status=status; localStorage.setItem('sov_equipment_requests',JSON.stringify(l));}catch(e){} await renderLoans(); toast(status==='issued'?'Označeno izdano':status==='returned'?'Označeno vraćeno':'Status promijenjen')}
async function issueLoan(id){
  const r=(STATE.requests||[]).find(x=>String(x.id)===String(id));
  try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.issueRequest) await SOVArmoryDB.issueRequest(id,r); else await setStatus(id,'issued'); }
  catch(e){console.warn('[clean master] issue failed',e); await setStatus(id,'issued');}
  try{const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]'); const x=l.find(y=>String(y.id)===String(id)); if(x)x.status='issued'; localStorage.setItem('sov_equipment_requests',JSON.stringify(l));}catch(e){}
  await renderLoans(); toast('Označeno izdano');
}
function openReturn(id){
  const r=(STATE.requests||[]).find(x=>String(x.id)===String(id));
  if(!r){toast('Ne mogu naći posudbu.');return;}
  const items=(r.items||[]);
  const html=`<div class="cm-modal-backdrop" id="returnModal"><div class="cm-modal"><div class="cm-modal-head"><div><h2>Potvrdi povrat</h2><p class="muted">Za svaki artikl upiši koliko je stvarno vraćeno. Ostatak ostaje gdje ga označiš.</p></div><button class="cm-icon-btn" onclick="CleanArmory.closeReturn()">×</button></div><div class="return-list">${items.map((it,idx)=>{const q=countInt(it.quantity,1);return `<div class="return-row" data-idx="${idx}" data-id="${esc(it.id||'')}" data-name="${esc(it.name||it.item_name||'Artikl')}" data-qty="${q}"><div><b>${esc(it.name||it.item_name||'Artikl')}</b><div class="muted">Izdano: ${q}</div></div><label>Vraćeno<input class="cm-input return-qty" type="number" min="0" max="${q}" value="${q}"></label><label>Gdje ide vraćeno<select class="cm-input return-dest"><option>Oružarstvo</option><option>U jami / teren</option><option>Kod nekoga drugoga</option><option>Rashod</option></select></label><label>Ako nije sve vraćeno, ostatak je<select class="cm-input remain-dest"><option>Kod posuđivača</option><option>U jami / teren</option><option>Kod nekoga drugoga</option><option>Izgubljeno</option><option>Rashod</option></select></label></div>`}).join('')}</div><textarea class="cm-input" id="returnNote" placeholder="Napomena, npr. 2 karabinera ostala kod Marka / dio ostao u jami"></textarea><div class="cm-tools"><button class="cm-btn" onclick="CleanArmory.closeReturn()">Odustani</button><button class="cm-btn primary" onclick="CleanArmory.confirmReturn('${esc(id)}')">Spremi povrat</button></div></div></div>`;
  document.body.insertAdjacentHTML('beforeend',html);
}
function closeReturn(){const m=document.getElementById('returnModal'); if(m)m.remove();}
async function confirmReturn(id){
  const r=(STATE.requests||[]).find(x=>String(x.id)===String(id));
  const rows=[...document.querySelectorAll('#returnModal .return-row')].map(row=>{const issued=countInt(row.dataset.qty,0); const returned=countInt(row.querySelector('.return-qty').value,0); return {id:row.dataset.id||null,name:row.dataset.name,issued_quantity:issued,returned_quantity:Math.min(Math.max(returned,0),issued),missing_quantity:Math.max(issued-Math.min(Math.max(returned,0),issued),0),return_location:row.querySelector('.return-dest').value,remaining_location:row.querySelector('.remain-dest').value};});
  const full=rows.every(x=>x.missing_quantity===0); const status=full?'returned':'partial_return'; const note=document.getElementById('returnNote')?.value||'';
  try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.returnRequestItems) await SOVArmoryDB.returnRequestItems(id,rows,note,r); else await setStatus(id,status); }
  catch(e){console.warn('[clean master] return failed',e); toast('Supabase povrat nije prošao; spremam lokalno.');}
  try{const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]'); const x=l.find(y=>String(y.id)===String(id)); if(x){x.status=status;x.return_items=rows;x.return_note=note;} localStorage.setItem('sov_equipment_requests',JSON.stringify(l));}catch(e){}
  closeReturn(); await renderLoans(); toast(full?'Sve vraćeno u evidenciju':'Djelomični povrat spremljen');
}
async function manualLoan(ev){ev.preventDefault(); const req={id:'REQ-'+Date.now(),created_at:new Date().toISOString(),user:document.getElementById('mUser').value||'Član',trip:document.getElementById('mNote').value||'Ručni unos',status:'pending',items:[{id:'manual',name:document.getElementById('mItem').value||'Artikl',quantity:Number(document.getElementById('mQty').value)||1}]}; try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.createRequest){const saved=await SOVArmoryDB.createRequest(req); req.id=saved.id||req.id; }}catch(e){console.warn(e)} const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]'); l.unshift(req); localStorage.setItem('sov_equipment_requests',JSON.stringify(l)); await renderLoans(); toast('Zahtjev dodan')}
function getRow(id){return (STATE.rows||[]).find(r=>String(r.id)===String(id));}
function openItemModal(r){
  const isNew=!r;
  r=r||{id:'NEW-'+Date.now(),name:'',category:STATE.cat||'Ostalo',subcategory:STATE.sub||'Ostalo',qty:0,av:0,loan:0,location:'Oružarstvo',minimum:0,status:'aktivno',type:'item'};
  const html=`<div class="cm-modal-backdrop" id="itemModal"><div class="cm-modal"><div class="cm-modal-head"><div><h2>${isNew?'Dodaj artikl':'Uredi artikl'}</h2><p class="muted">Ovdje se uređuje lokacija, broj komada i prag za crveno. Nema više stare threshold logike kroz Postavke.</p></div><button class="cm-icon-btn" onclick="CleanArmory.closeItemModal()">×</button></div><form class="cm-form" onsubmit="CleanArmory.saveItem(event,'${esc(r.id)}',${isNew})"><div class="cm-form-grid"><input class="cm-input" id="itemName" placeholder="Naziv artikla" value="${esc(r.name)}"><input class="cm-input" id="itemCat" placeholder="Kategorija" value="${esc(r.category)}"><input class="cm-input" id="itemSub" placeholder="Podkategorija" value="${esc(r.subcategory)}"></div><div class="cm-form-grid"><input class="cm-input" id="itemQty" type="number" min="0" placeholder="Ukupno" value="${esc(r.qty)}"><input class="cm-input" id="itemAv" type="number" min="0" placeholder="Dostupno" value="${esc(r.av)}"><input class="cm-input" id="itemMin" type="number" min="0" placeholder="Crveni prag" value="${esc(r.minimum||0)}"></div><div class="cm-form-grid"><input class="cm-input" id="itemLoc" placeholder="Lokacija" value="${esc(r.location||'Oružarstvo')}"><input class="cm-input" id="itemCode" placeholder="Opcionalni kod / napomena" value="${esc(r.raw&&r.raw.physical_code_note||'')}"><input class="cm-input" id="itemStatus" placeholder="Status" value="${esc(r.status||'aktivno')}"></div><div class="cm-tools"><button class="cm-btn" type="button" onclick="CleanArmory.closeItemModal()">Odustani</button><button class="cm-btn primary">Spremi</button></div></form></div></div>`;
  document.body.insertAdjacentHTML('beforeend',html);
}
function closeItemModal(){const m=document.getElementById('itemModal'); if(m)m.remove();}
async function saveItem(ev,id,isNew){
  ev.preventDefault();
  const row=getRow(id)||{};
  const payload={legacy_id:isNew?('ART-'+Date.now()):(row.raw&&row.raw.legacy_id)||id,catalog_id:isNew?('ART-'+Date.now()):(row.raw&&row.raw.catalog_id)||id,name:document.getElementById('itemName').value.trim()||'Artikl',category_name:document.getElementById('itemCat').value.trim()||'Ostalo',category:document.getElementById('itemCat').value.trim()||'Ostalo',subcategory:document.getElementById('itemSub').value.trim()||'Ostalo',quantity:countInt(document.getElementById('itemQty').value,0),available:countInt(document.getElementById('itemAv').value,0),loaned:Math.max(0,countInt(document.getElementById('itemQty').value,0)-countInt(document.getElementById('itemAv').value,0)),minimum:countInt(document.getElementById('itemMin').value,0),location_name:document.getElementById('itemLoc').value.trim()||'Oružarstvo',status:document.getElementById('itemStatus').value.trim()||'aktivno',physical_code_note:document.getElementById('itemCode').value.trim()||null,item_kind:'quantity_article',code_required:false,member_visible:true};
  try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.upsertSimpleItem) await SOVArmoryDB.upsertSimpleItem(payload); else throw new Error('no supabase helper'); }
  catch(e){ console.warn('[clean master] save item local fallback',e); const local=JSON.parse(localStorage.getItem('sov_armory_items_override')||'[]'); const i=local.findIndex(x=>String(x.legacy_id)===String(payload.legacy_id)); if(i>=0)local[i]=payload; else local.unshift(payload); localStorage.setItem('sov_armory_items_override',JSON.stringify(local)); }
  closeItemModal(); STATE.data=null; await loadData(); renderInventory(); toast('Artikl spremljen');
}
function newItem(){openItemModal(null)}
function editItem(id){const r=getRow(id); if(!r){toast('Ne mogu naći artikl.');return;} openItemModal(r)}
async function removeItem(id){const r=getRow(id); if(!r){toast('Ne mogu naći artikl.');return;} if(!confirm('Maknuti/rashodovati artikl iz aktivnog inventara?'))return; try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.retireSimpleItem) await SOVArmoryDB.retireSimpleItem((r.raw&&r.raw.legacy_id)||id,r.name); }catch(e){console.warn(e)} r.status='rashod'; r.av=0; renderInventory(); toast('Artikl maknut iz aktivnog inventara')}


function safeSheetName(name){
  let x=String(name||'Kategorija').replace(/[\\\/?*\[\]:]/g,' ').replace(/\s+/g,' ').trim();
  if(!x) x='Kategorija';
  return x.slice(0,31);
}
function xmlEsc(s){return String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]));}
function xlsCell(v, cls=''){return `<td class="${cls}">${xmlEsc(v)}</td>`;}
function xlsWorkbook(filename, sheets){
  const worksheetNames=sheets.map(s=>safeSheetName(s.name));
  const tabs=worksheetNames.map(n=>`<x:ExcelWorksheet><x:Name>${xmlEsc(n)}</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet>`).join('');
  const body=sheets.map((sheet,idx)=>`<div style="mso-element:worksheet" id="${xmlEsc(worksheetNames[idx])}"><table>${sheet.html}</table></div>`).join('\n');
  const html=`<!doctype html><html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40"><head><meta charset="utf-8"><!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets>${tabs}</x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]--><style>table{border-collapse:collapse;font-family:Arial,sans-serif;font-size:12px}td,th{border:1px solid #999;padding:6px;mso-number-format:"\\@"}th{background:#d9ead3;font-weight:bold}.num{mso-number-format:"0"}.warn{background:#fff2cc}.bad{background:#f4cccc}.head{background:#073b32;color:#fff;font-size:16px;font-weight:bold}</style></head><body>${body}</body></html>`;
  const blob=new Blob([html],{type:'application/vnd.ms-excel;charset=utf-8'});
  const a=document.createElement('a'); a.href=URL.createObjectURL(blob); a.download=filename; document.body.appendChild(a); a.click(); setTimeout(()=>{URL.revokeObjectURL(a.href); a.remove();},1500);
}
function rowsByCategory(){
  const rows=(STATE.rows||[]).filter(r=>r.name && !/rashod|otpis|deleted/i.test(String(r.status||'')));
  const m=new Map();
  rows.forEach(r=>{const c=r.category||'Ostalo'; if(!m.has(c))m.set(c,[]); m.get(c).push(r);});
  return [...m.entries()].sort((a,b)=>a[0].localeCompare(b[0],'hr'));
}
async function exportInventoryXls(){
  await loadData();
  const date=new Date().toISOString().slice(0,10);
  const sheets=rowsByCategory().map(([cat,rows])=>{
    const header=`<tr><th colspan="9" class="head">Inventar — ${xmlEsc(cat)} — ${xmlEsc(date)}</th></tr><tr><th>Podkategorija</th><th>Artikl</th><th>Ukupno</th><th>Dostupno</th><th>Posuđeno</th><th>Lokacija</th><th>Status</th><th>Prag</th><th>Napomena / kod</th></tr>`;
    const body=rows.sort((a,b)=>(a.subcategory+a.name).localeCompare(b.subcategory+b.name,'hr')).map(r=>`<tr>${xlsCell(r.subcategory)}${xlsCell(r.name)}${xlsCell(r.qty,'num')}${xlsCell(r.av,'num')}${xlsCell(r.loan,'num')}${xlsCell(r.location||'Oružarstvo')}${xlsCell(r.status||'aktivno')}${xlsCell(r.minimum||'', 'num')}${xlsCell((r.raw&& (r.raw.physical_code_note||r.raw.note||r.raw.internal_note||r.raw.sku))||'')}</tr>`).join('');
    return {name:cat,html:header+body};
  });
  if(!sheets.length){toast('Nema inventara za export.');return;}
  xlsWorkbook(`SOV_inventar_${date}.xls`,sheets);
  toast('Inventar exportiran u XLS');
}
async function exportInventuraXls(){
  await loadData();
  const date=(document.querySelector('input[type="date"]')?.value)||new Date().toISOString().slice(0,10);
  const note=(document.querySelector('input[placeholder*="Napomena"]')?.value)||'';
  const sheets=rowsByCategory().map(([cat,rows])=>{
    const header=`<tr><th colspan="9" class="head">Inventura — ${xmlEsc(cat)} — ${xmlEsc(date)} ${note?(' — '+xmlEsc(note)):''}</th></tr><tr><th>Podkategorija</th><th>Artikl</th><th>Broj u bazi</th><th>Stvarno prebrojano</th><th>Razlika</th><th>Lokacija</th><th>Status</th><th>Za rashod?</th><th>Napomena</th></tr>`;
    const body=rows.sort((a,b)=>(a.subcategory+a.name).localeCompare(b.subcategory+b.name,'hr')).map(r=>`<tr>${xlsCell(r.subcategory)}${xlsCell(r.name)}${xlsCell(r.qty,'num')}${xlsCell('')}${xlsCell('')}${xlsCell(r.location||'Oružarstvo')}${xlsCell(r.status||'aktivno')}${xlsCell('')}${xlsCell('')}</tr>`).join('');
    return {name:cat,html:header+body};
  });
  if(!sheets.length){toast('Nema inventara za inventuru export.');return;}
  xlsWorkbook(`SOV_inventura_${date}.xls`,sheets);
  toast('Inventura exportirana u XLS');
}


async function loadNotes(){
  try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.loadArmoryNotes){const n=await SOVArmoryDB.loadArmoryNotes(); if(Array.isArray(n))return n;} }catch(e){console.warn(e)}
  try{return JSON.parse(localStorage.getItem('sov_armory_notes')||'[]')}catch(e){return []}
}
async function renderNotes(){const root=document.getElementById('notesRoot'); if(!root)return; const notes=await loadNotes(); const open=notes.filter(n=>!/done|closed|obavljeno/i.test(String(n.status||'open'))); root.innerHTML=`<div class="loan-grid"><section class="cm-panel"><h2>+ Nova bilješka / reminder</h2><form class="cm-form" onsubmit="CleanArmory.saveNote(event)"><input class="cm-input" id="noteTitle" placeholder="Naslov, npr. Nabaviti spitove"><textarea class="cm-input" id="noteBody" placeholder="Detalji / napomena"></textarea><div class="cm-form-grid"><input class="cm-input" id="noteDue" type="date"><select class="cm-input" id="noteType"><option value="todo">Obaviti</option><option value="buy">Nabaviti</option><option value="check">Provjeriti</option></select><select class="cm-input" id="notePriority"><option value="normal">Normalno</option><option value="high">Hitno</option><option value="low">Nisko</option></select></div><button class="cm-btn primary">Spremi reminder</button></form></section><section class="cm-panel"><h2>Notes & reminders</h2><div class="loan-list">${open.length?open.map(n=>`<div class="loan-row"><div class="loan-row-top"><div><b>${esc(n.title||'Bilješka')}</b><div class="muted">${esc(n.due_date||'bez datuma')} · ${esc(n.note_type||n.type||'todo')} · ${esc(n.priority||'normal')}</div></div><span class="badge ${n.priority==='high'?'bad':'warn'}">${esc(n.status||'open')}</span></div><p>${esc(n.body||n.note||'')}</p><button class="cm-btn primary" onclick="CleanArmory.doneNote('${esc(n.id)}')">Označi obavljeno</button></div>`).join(''):'<div class="empty">Nema otvorenih podsjetnika.</div>'}</div></section></div>`}
async function saveNote(ev){ev.preventDefault(); const n={id:'NOTE-'+Date.now(),title:document.getElementById('noteTitle').value||'Bilješka',body:document.getElementById('noteBody').value||'',due_date:document.getElementById('noteDue').value||null,note_type:document.getElementById('noteType').value,priority:document.getElementById('notePriority').value,status:'open',created_at:new Date().toISOString()}; try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.saveArmoryNote){await SOVArmoryDB.saveArmoryNote(n);} }catch(e){console.warn(e)} const l=JSON.parse(localStorage.getItem('sov_armory_notes')||'[]'); l.unshift(n); localStorage.setItem('sov_armory_notes',JSON.stringify(l)); await renderNotes(); toast('Reminder spremljen')}
async function doneNote(id){try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.doneArmoryNote) await SOVArmoryDB.doneArmoryNote(id);}catch(e){console.warn(e)} const l=JSON.parse(localStorage.getItem('sov_armory_notes')||'[]'); const n=l.find(x=>String(x.id)===String(id)); if(n)n.status='done'; localStorage.setItem('sov_armory_notes',JSON.stringify(l)); await renderNotes(); toast('Označeno obavljeno')}

async function init(){await loadData(); renderKpis(); renderMaster(); renderInventory(); await renderLoans(); await renderNotes();}
window.CleanArmory={init,pickCat(c){STATE.cat=c||null;STATE.sub=null;renderInventory()},pickSub(s){STATE.sub=s||null;renderInventory()},renderLoans,setStatus,manualLoan,newItem,editItem,removeItem,exportInventoryXls,exportInventuraXls,openReturn,closeReturn,confirmReturn,closeItemModal,saveItem,renderNotes,saveNote,doneNote};
document.addEventListener('DOMContentLoaded',init);
})();
