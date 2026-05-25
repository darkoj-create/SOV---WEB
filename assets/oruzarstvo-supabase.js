(function(){
  function sb(){ return window.SOVAuth && SOVAuth.getClient ? SOVAuth.getClient() : null; }
  function configured(){ return !!(window.SOVAuth && SOVAuth.isConfigured && SOVAuth.isConfigured() && sb()); }
  function toDate(v){
    if(v===null || v===undefined) return null;
    if(v instanceof Date && !isNaN(v)) return v.toISOString().slice(0,10);
    let s=String(v).trim();
    if(!s || s==='-' || s==='—' || /^n\/?a$/i.test(s)) return null;
    if(/^\d{4}-\d{2}-\d{2}$/.test(s)) return s;
    let m=s.match(/^(\d{4})-(\d{1,2})$/);
    if(m) return `${m[1]}-${String(m[2]).padStart(2,'0')}-01`;
    m=s.match(/^(\d{1,2})\/(\d{4})$/);
    if(m) return `${m[2]}-${String(m[1]).padStart(2,'0')}-01`;
    m=s.match(/^(\d{1,2})\.(\d{4})\.?$/);
    if(m) return `${m[2]}-${String(m[1]).padStart(2,'0')}-01`;
    m=s.match(/^(\d{1,2})\.(\d{1,2})\.(\d{4})\.?$/);
    if(m) return `${m[3]}-${String(m[2]).padStart(2,'0')}-${String(m[1]).padStart(2,'0')}`;
    m=s.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    if(m) return `${m[3]}-${String(m[2]).padStart(2,'0')}-${String(m[1]).padStart(2,'0')}`;
    m=s.match(/^(\d{1,2})-(\d{1,2})-(\d{4})$/);
    if(m) return `${m[3]}-${String(m[2]).padStart(2,'0')}-${String(m[1]).padStart(2,'0')}`;
    m=s.match(/^(\d{1,2})\.\/(\d{1,2})\.(\d{1,2})\.(\d{4})\.?$/);
    if(m) return `${m[4]}-${String(m[3]).padStart(2,'0')}-${String(m[1]).padStart(2,'0')}`;
    m=s.match(/^(\d{1,2})\.\/(\d{1,2})\.(\d{4})\.?$/);
    if(m) return `${m[3]}-${String(m[2]).padStart(2,'0')}-${String(m[1]).padStart(2,'0')}`;
    m=s.match(/^(\d{1,2})\.(\d{1,2})\.?$/);
    if(m) return null;
    if(/^\d+(\.0)?$/.test(s)){
      const n=Number(s);
      if(n>25000 && n<80000){
        const d=new Date(Date.UTC(1899,11,30)+n*86400000);
        return d.toISOString().slice(0,10);
      }
    }
    return null;
  }

  function safeNumber(v){
    if(v===null || v===undefined) return null;
    if(typeof v==='number') return Number.isFinite(v) ? v : null;
    let s=String(v).trim();
    if(!s || s==='-' || s==='—' || /^n\/?a$/i.test(s) || s==='/' ) return null;
    // Values like 2.2024 or 02.2024 are month/year dates, not numeric quantities.
    if(/^\d{1,2}[.\/]\d{4}\.?$/.test(s)) return null;
    s=s.replace(',', '.');
    if(!/^-?\d+(\.\d+)?$/.test(s)) return null;
    const n=Number(s);
    return Number.isFinite(n) ? n : null;
  }
  function safeInt(v){
    const n=safeNumber(v);
    return n===null ? null : Math.trunc(n);
  }
  function safeQuantity(v){
    // Armory quantities are whole pieces. Do not import dates, fractions, labels like 'hrpa', or Excel garbage as stock counts.
    if(v===null || v===undefined) return null;
    if(typeof v==='number'){
      if(!Number.isFinite(v)) return null;
      if(Math.abs(v - Math.round(v)) < 1e-9) return Math.max(0, Math.trunc(v));
      return null;
    }
    let s=String(v).trim();
    if(!s || s==='-' || s==='—' || s==='/' || /^n\/?a$/i.test(s)) return null;
    if(/^\d{1,2}[.\/]\d{4}\.?$/.test(s)) return null;
    s=s.replace(',', '.');
    if(!/^-?\d+(\.0+)?$/.test(s)) return null;
    const n=Number(s);
    return Number.isFinite(n) ? Math.max(0, Math.trunc(n)) : null;
  }
  function safeYear(v){
    if(v===null || v===undefined) return null;
    if(typeof v==='number' && Number.isFinite(v)){
      const y=Math.trunc(v);
      return y>=1900 && y<=2100 ? y : null;
    }
    const s=String(v).trim();
    if(!s || s==='-' || s==='—') return null;
    let m=s.match(/^(\d{4})$/);
    if(m){ const y=Number(m[1]); return y>=1900 && y<=2100 ? y : null; }
    m=s.match(/^\d{1,2}[.\/](\d{4})\.?$/);
    if(m){ const y=Number(m[1]); return y>=1900 && y<=2100 ? y : null; }
    m=s.match(/^\d{1,2}[.\/]\d{1,2}[.\/](\d{4})\.?$/);
    if(m){ const y=Number(m[1]); return y>=1900 && y<=2100 ? y : null; }
    m=s.match(/(19\d{2}|20\d{2}|2100)/);
    if(m){ const y=Number(m[1]); return y>=1900 && y<=2100 ? y : null; }
    return null;
  }

  function stripDiacritics(s){ return String(s||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim(); }
  function canonicalArmoryCategory(raw, text){
    const r=stripDiacritics(raw);
    const t=stripDiacritics([raw,text].filter(Boolean).join(' '));
    if(/descender|\bstop\b|rig|maestro|id['’]?s|croll|krol|crol|bloker|zumar|pojas|sjedal|pedal|stremen|prsni|pupak|pupcano/.test(t)) return 'Osobna oprema';
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
    return raw && String(raw).trim() ? String(raw).trim() : 'Ostalo';
  }


  function normalizeArticleName(name){
    let x=stripDiacritics(name);
    x=x.replace(/[()\[\]{}]/g,' ').replace(/[+_\/\\,;:]+/g,' ').replace(/\s+/g,' ').trim();
    x=x.replace(/\b(krol|crol|croll)\b/g,'croll');
    x=x.replace(/\bpupak\b|\bpupcano\s+u?ze\b/g,'pupcano uze');
    // Osnovni speleo artikli bez stvarnog modela se vode kao jedan artikl.
    if(/\bcroll\b/.test(x)){
      if(/\b(s|small|velicina s)\b/.test(x)) return 'croll s';
      if(/\b(l|large|velicina l)\b/.test(x)) return 'croll l';
      return 'croll';
    }
    if(/\bstremen\b|\bpedala\b/.test(x)) return 'stremen';
    if(/\bpupcano uze\b/.test(x)) return 'pupcano uze';
    if(/\bprusik\b/.test(x)) return 'prusik';
    if(/\bbloker\b|\bascender\b|\bjumar\b|\bzumar\b/.test(x)) return 'bloker';
    if(/\bstop\b/.test(x)) return 'stop descender';
    return x;
  }
  function articleMergeKey(row){
    const cat=canonicalArmoryCategory(row.category_name || row.category || 'Ostalo',[row.name,row.model,row.subcategory,row.internal_note,row.note].join(' '));
    const sub=String(row.subcategory||'Ostalo').trim() || 'Ostalo';
    const nm=normalizeArticleName(row.name||row.item_name||row.model||'Artikl');
    return stripDiacritics(cat)+'|'+stripDiacritics(sub)+'|'+nm;
  }
  function stableArticleId(row){
    const key=articleMergeKey(row).replace(/[^a-z0-9]+/g,'-').replace(/^-+|-+$/g,'').slice(0,90);
    return 'ART-'+(key||String(row.legacy_id||row.id||Date.now()));
  }
  function mergeQuantityArticles(rows){
    const map=new Map();
    for(const row of rows || []){
      const key=articleMergeKey(row);
      const qty=safeQuantity(row.quantity); const loan=safeQuantity(row.loaned); const av=safeQuantity(row.available);
      if(!map.has(key)){
        const first={...row};
        first.legacy_id=stableArticleId(row);
        first.catalog_id=first.legacy_id;
        first.category_name=canonicalArmoryCategory(row.category_name || row.category || 'Ostalo',[row.name,row.model,row.subcategory,row.internal_note,row.note].join(' '));
        first.category=first.category_name;
        first.name=String(row.name||row.item_name||row.model||'Artikl').trim();
        first.quantity=qty===null?0:qty;
        first.loaned=loan===null?0:loan;
        first.available=av===null?(qty===null?0:qty):av;
        first.minimum=safeQuantity(row.minimum);
        first.internal_note=[row.internal_note,row.note].filter(Boolean).join(' | ') || null;
        first.source_sheet=row.source_sheet||null;
        first.item_kind='quantity_article'; first.code_required=false;
        map.set(key, first);
      }else{
        const cur=map.get(key);
        cur.quantity=(safeQuantity(cur.quantity)||0)+(qty===null?0:qty);
        cur.loaned=(safeQuantity(cur.loaned)||0)+(loan===null?0:loan);
        cur.available=(safeQuantity(cur.available)||0)+(av===null?(qty===null?0:qty):av);
        if(cur.minimum===null || cur.minimum===undefined || cur.minimum==='') cur.minimum=safeQuantity(row.minimum);
        if(!cur.internal_note && (row.internal_note||row.note)) cur.internal_note=row.internal_note||row.note;
        if(!cur.source_sheet && row.source_sheet) cur.source_sheet=row.source_sheet;
      }
    }
    return Array.from(map.values());
  }

  function requestToUi(r, items){
    return {
      id:r.id,
      created_at:r.created_at,
      user:r.requester_name || '',
      email:r.requester_email || '',
      trip:r.trip_name || '',
      from:r.date_from || '',
      to:r.date_to || '',
      note:r.note || '',
      status:r.status || 'pending',
      items:(items||[]).filter(i=>i.request_id===r.id).map(i=>({id:i.equipment_legacy_id||i.equipment_item_id||i.id,name:i.item_name,quantity:Number(i.quantity)||1,note:i.note||''}))
    };
  }
  async function loadRequests(){
    if(!configured()) return null;
    const client=sb();
    const {data:reqs,error}=await client.from('equipment_requests').select('*').order('created_at',{ascending:false});
    if(error){ console.warn('Oružarstvo requests Supabase fallback:',error.message); return null; }
    const ids=(reqs||[]).map(r=>r.id);
    let items=[];
    if(ids.length){
      const res=await client.from('equipment_request_items').select('*').in('request_id',ids);
      if(!res.error) items=res.data||[];
    }
    return (reqs||[]).map(r=>requestToUi(r,items));
  }
  async function createRequest(req){
    if(!configured()) return null;
    const client=sb();
    let profile=null;
    try{ profile=await SOVAuth.getProfile(); }catch(e){ profile=null; }
    // v4.70: Zahtjev mora doći do oružara i kad user view nema kompletan profil.
    // Ako je korisnik prijavljen, vežemo requester_id. Ako nije, SQL policy dopušta nullable requester_id
    // i oružar ipak vidi zahtjev.
    const {data,error}=await client.from('equipment_requests').insert({
      requester_id:(profile&&profile.id)||null,
      requester_name:req.user || (profile&&profile.full_name) || (profile&&profile.email) || 'Član',
      requester_email:req.email || (profile&&profile.email) || '',
      trip_name:req.trip || null,
      date_from:toDate(req.from),
      date_to:toDate(req.to),
      note:req.note || null,
      status:'pending'
    }).select('id,created_at,status').single();
    if(error) throw error;
    const rows=(req.items||[]).map(i=>({
      request_id:data.id,
      equipment_legacy_id:i.id || null,
      item_name:i.name,
      quantity:Number(i.quantity)||1,
      note:i.note || null
    }));
    if(rows.length){
      const res=await client.from('equipment_request_items').insert(rows);
      if(res.error) throw res.error;
    }
    return {...req,id:data.id,created_at:data.created_at,status:data.status};
  }
  async function updateRequestStatus(id,status){
    if(!configured()) return false;
    const client=sb();
    const patch={status,updated_at:new Date().toISOString()};
    if(['approved','rejected','prepared','issued','returned'].includes(status)){
      const profile=await SOVAuth.getProfile();
      patch.decided_by=profile && profile.id;
      patch.decided_at=new Date().toISOString();
    }
    const {error}=await client.from('equipment_requests').update(patch).eq('id',id);
    if(error) throw error;
    return true;
  }
  async function upsertRows(table, rows, conflict){
    if(!rows || !rows.length) return 0;
    const client=sb();
    let total=0;
    for(let i=0;i<rows.length;i+=500){
      const chunk=rows.slice(i,i+500);
      const {error}=await client.from(table).upsert(chunk,{onConflict:conflict});
      if(error) throw error;
      total += chunk.length;
    }
    return total;
  }

  function normalizedSku(v){
    const s = (v === null || v === undefined) ? '' : String(v).trim();
    return s || null;
  }
  function dedupeByKey(rows, keyFn){
    const map = new Map();
    for(const row of rows || []){
      const key = keyFn(row);
      if(!key){
        map.set(`__no_key__:${map.size}`, row);
      }else{
        map.set(key, row);
      }
    }
    return Array.from(map.values());
  }
  async function upsertRopes(rows){
    if(!rows || !rows.length) return 0;

    // equipment_ropes has TWO unique keys: legacy_id and sku.
    // Old import used legacy_id as the conflict key, so a repeated/renamed rope with
    // the same SKU crashed with: duplicate key value violates constraint equipment_ropes_sku_key.
    // SKU is the real physical rope identifier, so rows with SKU are deduped and upserted by sku.
    const withSku = dedupeByKey(rows.filter(r => normalizedSku(r.sku)), r => normalizedSku(r.sku));
    const withoutSku = dedupeByKey(rows.filter(r => !normalizedSku(r.sku)), r => r.legacy_id ? String(r.legacy_id) : null);

    let total = 0;
    total += await upsertRows('equipment_ropes', withSku, 'sku');
    total += await upsertRows('equipment_ropes', withoutSku, 'legacy_id');
    return total;
  }


  async function upsertCategoriesSafe(rows){
    if(!rows || !rows.length) return 0;
    const client=sb();
    const normalized=dedupeByKey(rows
      .filter(r => String(r.name||'').trim())
      .map((r,idx)=>({
        name:String(r.name||'').trim(),
        description:r.description||null,
        type:r.type||null,
        sort_order:Number.isFinite(Number(r.sort_order)) ? Number(r.sort_order) : idx
      })),
      r => String(r.name||'').trim().toLowerCase()
    );
    let total=0;
    for(const row of normalized){
      const {data:existing,error:selectError}=await client
        .from('equipment_categories')
        .select('id,name')
        .eq('name',row.name)
        .maybeSingle();
      if(selectError) throw selectError;
      if(existing && existing.id){
        const {error:updateError}=await client
          .from('equipment_categories')
          .update({description:row.description,type:row.type,sort_order:row.sort_order,updated_at:new Date().toISOString()})
          .eq('id',existing.id);
        if(updateError) throw updateError;
      }else{
        const {error:insertError}=await client
          .from('equipment_categories')
          .insert(row);
        if(insertError){
          if(String(insertError.message||'').includes('equipment_categories_name_key') || String(insertError.code||'') === '23505'){
            const {data:again,error:againError}=await client
              .from('equipment_categories')
              .select('id')
              .eq('name',row.name)
              .maybeSingle();
            if(againError) throw againError;
            if(again && again.id){
              const {error:updateAgainError}=await client
                .from('equipment_categories')
                .update({description:row.description,type:row.type,sort_order:row.sort_order,updated_at:new Date().toISOString()})
                .eq('id',again.id);
              if(updateAgainError) throw updateAgainError;
            }else{
              throw insertError;
            }
          }else{
            throw insertError;
          }
        }
      }
      total += 1;
    }
    return total;
  }

  function normalizeStaticData(data){
    const empty={summary:{},categories:[],items:[],pieces:[],ropes:[],loans:[],inventories:[],inventory_items:[],procurement:[],services:[],disposed:[],lost:[],field:[],locations:[],members:[],status_options:[],request_status_options:[],tasks:[],inventory_rules:[],audit_notes:[]};
    if(!data || typeof data !== 'object') return empty;
    for(const k of Object.keys(empty)){
      if(Array.isArray(empty[k])) empty[k]=Array.isArray(data[k]) ? data[k] : [];
      else empty[k]=(data[k] && typeof data[k]==='object') ? data[k] : {};
    }
    return empty;
  }

  async function importStaticData(data){
    if(!configured()) throw new Error('Supabase nije konfiguriran.');
    if(!(await SOVAuth.can('armory'))) throw new Error('Import može raditi samo admin ili oružar.');
    data=normalizeStaticData(data);
    const cats=dedupeByKey((data.categories||[])
      .filter(c => String(c.name||'').trim())
      .map((c,idx)=>({legacy_id:String(c.id||idx+1),name:canonicalArmoryCategory(String(c.name||'').trim(), c.description||''),description:c.description||null,type:c.type||null,sort_order:idx})),
      c => String(c.name||'').trim().toLowerCase()
    );
    const locs=(data.locations||[]).map((l,idx)=>({legacy_id:String(l.id||idx+1),name:l.name,description:l.description||null,type:l.type||null}));
    const rawItems=(data.items||[]).map(i=>({legacy_id:i.id,catalog_id:String(i.catalog_id||''),name:i.name,category_name:canonicalArmoryCategory(i.category||i.category_name||null,[i.name,i.model,i.subcategory,i.internal_note].join(' ')),subcategory:i.subcategory||null,unit:i.unit||'kom',tracking_type:i.tracking_type||'po vrsti',quantity:safeQuantity(i.quantity)||0,loaned:safeQuantity(i.loaned)||0,available:safeQuantity(i.available)||0,minimum:i.minimum===''?null:safeQuantity(i.minimum),status:i.status||'aktivno',availability:i.availability||'dostupno',member_visible:i.member_visible!==false,internal_note:i.internal_note||null,source_sheet:i.source_sheet||null,item_kind:i.item_kind||'quantity_article',code_required:false,physical_code_note:i.physical_code_note||'Nema pojedinačnih kodova; vodi se količina po artiklu.'}));
    const items=mergeQuantityArticles(rawItems);
    const pieces=(data.pieces||[]).map(p=>({legacy_id:p.id,catalog_legacy_id:String(p.catalog_id||''),name:p.name,sku:p.sku||null,manufacturer:p.manufacturer||null,model:p.model||null,purchase_date:toDate(p.purchase_date),location_name:p.location||null,status:p.status||'U društvu',next_service:toDate(p.next_service),note:p.note||null}));
    const ropes=(data.ropes||[]).map(r=>({legacy_id:r.id,sku:normalizedSku(r.sku||r.id),name:r.name,diameter_mm:safeNumber(r.diameter_mm),length_m:safeNumber(r.length_m),manufacturer:r.manufacturer||null,model:r.model||null,standard:r.standard||null,production_year:safeYear(r.year),in_use_since:toDate(r.in_use_since),color:r.color||null,supplier:r.supplier||null,location_name:r.location||null,status:r.status||'U društvu',note:r.note||null,item_kind:'individual_rope',code_required:true}));
    const procurement=(data.procurement||[]).map(p=>({legacy_id:p.id,equipment_legacy_id:String(p.catalog_id||''),item_name:p.name,quantity:safeNumber(p.quantity),unit_price:safeNumber(p.unit_price),total_price:safeNumber(p.total_price),supplier:p.supplier||null,status:p.status||'Zaprimljeno',purchase_date:toDate(p.date),requested_by:p.person||null,note:p.note||null}));
    const disposals=[...(data.disposed||[]),...(data.lost||[])].map(d=>({legacy_id:d.id,disposal_date:toDate(d.date),disposal_type:d.type||'Rashod',equipment_legacy_id:String(d.catalog_id||''),item_name:d.name,quantity:safeNumber(d.quantity),reason:d.reason||null,location_name:d.location||null,person_name:d.person||null,note:d.note||null}));
    const field=(data.field||[]).map(f=>({legacy_id:f.id,recorded_at:toDate(f.date),equipment_legacy_id:String(f.catalog_id||''),item_name:f.name,quantity:safeNumber(f.quantity),field_location:f.location||f.reason||null,responsible_person:f.person||null,status:'na terenu',note:f.note||null}));
    const inventories=(data.inventories||[]).map(i=>({legacy_id:i.id,name:i.name,inventory_date:toDate(i.date)||new Date().toISOString().slice(0,10),owner_name:i.owner||null,status:i.status||'Završena',note:i.note||null}));
    const result={};
    result.categories=await upsertCategoriesSafe(cats);
    result.locations=await upsertRows('equipment_locations',locs,'legacy_id');
    result.items=await upsertRows('equipment_items',items,'legacy_id');
    result.pieces=await upsertRows('equipment_pieces',pieces,'legacy_id');
    result.ropes=await upsertRopes(ropes);
    result.procurement=await upsertRows('procurement_plan',procurement,'legacy_id');
    result.disposals=await upsertRows('equipment_disposals',disposals,'legacy_id');
    result.field=await upsertRows('equipment_field_items',field,'legacy_id');
    result.inventories=await upsertRows('inventory_sessions',inventories,'legacy_id');
    return result;
  }

  async function requireArmory(){
    if(!configured()) throw new Error('Supabase nije konfiguriran.');
    if(!(await SOVAuth.can('armory'))) throw new Error('Samo admin ili oružar.');
    return sb();
  }
  async function createEquipmentItem(row){
    const client=await requireArmory();
    const payload={
      legacy_id:row.legacy_id || row.sku || ('manual-'+Date.now()),
      catalog_id:row.catalog_id || row.sku || null,
      name:row.name,
      category_name:canonicalArmoryCategory(row.category_name || row.category || null,[row.name,row.model,row.subcategory,row.note,row.internal_note].join(' ')),
      subcategory:row.subcategory || null,
      unit:row.unit || 'kom',
      tracking_type:row.tracking_type || 'po vrsti',
      quantity:safeNumber(row.quantity)||1,
      loaned:0,
      available:safeNumber(row.available)||safeNumber(row.quantity)||1,
      minimum:row.minimum || null,
      status:row.status || 'aktivno',
      availability:row.availability || 'dostupno',
      member_visible:row.member_visible !== false,
      internal_note:row.note || row.internal_note || null,
      source_sheet:'manual-web'
    };
    const {data,error}=await client.from('equipment_items').upsert(payload,{onConflict:'legacy_id'}).select('*').single();
    if(error) throw error;
    return data;
  }
  async function createEquipmentPiece(row){
    const client=await requireArmory();
    const payload={
      legacy_id:row.legacy_id || row.sku || ('piece-'+Date.now()),
      catalog_legacy_id:row.catalog_legacy_id || null,
      name:row.name,
      sku:row.sku || row.legacy_id || null,
      manufacturer:row.manufacturer || null,
      model:row.model || null,
      purchase_date:toDate(row.purchase_date),
      location_name:row.location_name || row.location || null,
      status:row.status || 'U društvu',
      next_service:toDate(row.next_service),
      note:row.note || null
    };
    const {data,error}=await client.from('equipment_pieces').upsert(payload,{onConflict:'legacy_id'}).select('*').single();
    if(error) throw error;
    return data;
  }
  async function createEquipmentPieces(rows){
    if(!rows || !rows.length) return 0;
    const client=await requireArmory();
    const payload=rows.map(row=>({legacy_id:row.legacy_id||row.sku,name:row.name,sku:row.sku||row.legacy_id||null,location_name:row.location_name||row.location||null,status:row.status||'U društvu',note:row.note||null}));
    const {error}=await client.from('equipment_pieces').upsert(payload,{onConflict:'legacy_id'});
    if(error) throw error;
    return payload.length;
  }
  async function createRope(row){
    const client=await requireArmory();
    const payload={
      legacy_id:row.legacy_id || row.sku || ('rope-'+Date.now()),
      sku:normalizedSku(row.sku || row.legacy_id),
      name:row.name,
      diameter_mm:safeNumber(row.diameter_mm),
      length_m:safeNumber(row.length_m),
      manufacturer:row.manufacturer || null,
      model:row.model || null,
      standard:row.standard || null,
      production_year:safeYear(row.production_year),
      in_use_since:toDate(row.in_use_since),
      color:row.color || null,
      supplier:row.supplier || null,
      location_name:row.location_name || row.location || null,
      status:row.status || 'U društvu',
      note:row.note || null
    };
    const {data,error}=await client.from('equipment_ropes').upsert(payload,{onConflict:'sku'}).select('*').single();
    if(error) throw error;
    return data;
  }
  async function updateEquipmentStatus(kind,id,status,note){
    const client=await requireArmory();
    const table=kind==='rope'?'equipment_ropes':(kind==='piece'?'equipment_pieces':'equipment_items');
    const patch={status,updated_at:new Date().toISOString()};
    if(table==='equipment_items') patch.availability='nedostupno';
    if(note) patch.note=note;
    let q=client.from(table).update(patch);
    if(kind==='rope') q=q.or(`id.eq.${id},sku.eq.${id},legacy_id.eq.${id}`);
    else q=q.or(`id.eq.${id},legacy_id.eq.${id}`);
    const {error}=await q;
    if(error) throw error;
    return true;
  }


  async function loadAllData(){
    if(!configured()) return null;
    const client=sb();
    const out={summary:{source:'Supabase live'},categories:[],items:[],pieces:[],ropes:[],loans:[],inventories:[],inventory_items:[],procurement:[],services:[],disposed:[],lost:[],field:[],locations:[]};
    async function safe(table, cols='*'){
      try{const {data,error}=await client.from(table).select(cols); if(error){console.warn('SOVArmoryDB load '+table,error.message); return [];} return data||[];}catch(e){console.warn('SOVArmoryDB load '+table,e.message||e); return [];}
    }
    const cats=await safe('equipment_categories','*');
    const items=await safe('equipment_items','*');
    const ropes=await safe('equipment_ropes','*');
    const pieces=await safe('equipment_pieces','*');
    const locs=await safe('equipment_locations','*');
    const loans=await safe('equipment_loans','*');
    const inv=await safe('inventory_sessions','*');
    const proc=await safe('procurement_plan','*');
    out.categories=(cats||[]).map((c,idx)=>({id:c.legacy_id||c.id||String(idx+1),name:canonicalArmoryCategory(c.name,c.description||''),description:c.description||'',type:c.type||'',sort_order:c.sort_order||idx})).filter(c=>c.name);
    out.items=mergeQuantityArticles((items||[]).map((i,idx)=>({
      id:i.legacy_id||i.catalog_id||i.id||('DB-ITEM-'+idx), legacy_id:i.legacy_id||i.id, catalog_id:i.catalog_id||i.legacy_id||i.id,
      name:i.name||i.item_name||'Artikl', category:canonicalArmoryCategory(i.category_name||i.category||'Ostalo',[i.name,i.subcategory,i.internal_note].join(' ')), category_name:canonicalArmoryCategory(i.category_name||i.category||'Ostalo',[i.name,i.subcategory,i.internal_note].join(' ')), subcategory:i.subcategory||'Ostalo',
      unit:i.unit||'kom', tracking_type:i.tracking_type||'po vrsti', quantity:safeQuantity(i.quantity)||0, quantity_label:String(i.quantity??''),
      available:safeQuantity(i.available ?? i.quantity ?? 0)||0, available_label:String(i.available ?? i.quantity ?? ''), loaned:safeQuantity(i.loaned)||0,
      minimum:i.minimum ?? '', status:i.status||'aktivno', availability:i.availability||'dostupno', member_visible:i.member_visible!==false,
      internal_note:i.internal_note||i.note||'', source_sheet:i.source_sheet||'Supabase', location:i.location_name||i.location||'', location_name:i.location_name||i.location||''
    })).filter(i=>i.name));
    out.ropes=(ropes||[]).map((r,idx)=>({
      id:r.legacy_id||r.sku||r.id||('DB-ROPE-'+idx), legacy_id:r.legacy_id||r.id, sku:r.sku||r.legacy_id||'', name:r.name||r.sku||'Uže',
      category:'Užad i užetna oprema', category_name:'Užad i užetna oprema', subcategory:r.subcategory||'Užad', quantity:1, available:/posu|vani|otpis|rashod|izgubl/i.test(String(r.status||''))?0:1, loaned:/posu|vani/i.test(String(r.status||''))?1:0,
      diameter_mm:r.diameter_mm, length_m:r.length_m, manufacturer:r.manufacturer||'', model:r.model||'', production_year:r.production_year, in_use_since:r.in_use_since,
      color:r.color||'', location:r.location_name||'', location_name:r.location_name||'', status:r.status||'U društvu', note:r.note||'', member_visible:true
    })).filter(r=>r.name);
    out.pieces=(pieces||[]).map((x,idx)=>({
      id:x.legacy_id||x.sku||x.id||('DB-PIECE-'+idx), legacy_id:x.legacy_id||x.id, sku:x.sku||'', name:x.name||x.model||x.sku||'Komad opreme',
      category:canonicalArmoryCategory(x.category_name||x.category||'Ostalo',[x.name,x.model,x.subcategory,x.note].join(' ')), category_name:canonicalArmoryCategory(x.category_name||x.category||'Ostalo',[x.name,x.model,x.subcategory,x.note].join(' ')), subcategory:x.subcategory||'Ostalo', quantity:1, available:/posu|vani|otpis|rashod|izgubl/i.test(String(x.status||''))?0:1, loaned:/posu|vani/i.test(String(x.status||''))?1:0,
      manufacturer:x.manufacturer||'', model:x.model||'', location:x.location_name||'', location_name:x.location_name||'', status:x.status||'U društvu', note:x.note||'', member_visible:true
    })).filter(x=>x.name);
    out.locations=(locs||[]).map(l=>({id:l.legacy_id||l.id,name:l.name,description:l.description||'',type:l.type||''})).filter(l=>l.name);
    out.loans=(loans||[]).map(l=>({id:l.id,member_name:l.member_name||l.user_name||l.borrower_name||'',item_name:l.item_name||l.name||'',quantity:safeQuantity(l.quantity)||1,due_date:l.due_date||l.to||'',status:l.status||'vani',note:l.note||''}));
    out.inventories=(inv||[]).map(i=>({id:i.legacy_id||i.id,name:i.name||'Inventura',date:i.inventory_date||i.date,owner:i.owner_name||i.owner,status:i.status,note:i.note}));
    out.procurement=(proc||[]).map(p=>({id:p.legacy_id||p.id,name:p.item_name||p.name,quantity:p.quantity,status:p.status,date:p.purchase_date,note:p.note}));
    out.summary.count_items=out.items.length; out.summary.count_ropes=out.ropes.length; out.summary.count_categories=out.categories.length;
    return out;
  }

  window.SOVArmoryDB={configured,loadRequests,createRequest,updateRequestStatus,importStaticData,loadAllData,createEquipmentItem,createEquipmentPiece,createEquipmentPieces,createRope,updateEquipmentStatus};
})();
