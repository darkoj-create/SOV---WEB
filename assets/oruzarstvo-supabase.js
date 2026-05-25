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
    const profile=await SOVAuth.getProfile();
    if(!profile || !profile.id) throw new Error('Nisi prijavljen.');
    const {data,error}=await client.from('equipment_requests').insert({
      requester_id:profile.id,
      requester_name:req.user || profile.full_name || profile.email,
      requester_email:req.email || profile.email,
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

  async function importStaticData(data){
    if(!configured()) throw new Error('Supabase nije konfiguriran.');
    if(!(await SOVAuth.can('armory'))) throw new Error('Import može raditi samo admin ili oružar.');
    const cats=dedupeByKey((data.categories||[])
      .filter(c => String(c.name||'').trim())
      .map((c,idx)=>({legacy_id:String(c.id||idx+1),name:String(c.name||'').trim(),description:c.description||null,type:c.type||null,sort_order:idx})),
      c => String(c.name||'').trim().toLowerCase()
    );
    const locs=(data.locations||[]).map((l,idx)=>({legacy_id:String(l.id||idx+1),name:l.name,description:l.description||null,type:l.type||null}));
    const items=(data.items||[]).map(i=>({legacy_id:i.id,catalog_id:String(i.catalog_id||''),name:i.name,category_name:i.category||null,subcategory:i.subcategory||null,unit:i.unit||'kom',tracking_type:i.tracking_type||'po vrsti',quantity:Number(i.quantity)||0,loaned:Number(i.loaned)||0,available:Number(i.available)||0,minimum:i.minimum===''?null:Number(i.minimum)||null,status:i.status||'aktivno',availability:i.availability||'dostupno',member_visible:i.member_visible!==false,internal_note:i.internal_note||null,source_sheet:i.source_sheet||null,item_kind:i.item_kind||'quantity_article',code_required:false,physical_code_note:i.physical_code_note||'Nema pojedinačnih kodova; vodi se količina po artiklu.'}));
    const pieces=(data.pieces||[]).map(p=>({legacy_id:p.id,catalog_legacy_id:String(p.catalog_id||''),name:p.name,sku:p.sku||null,manufacturer:p.manufacturer||null,model:p.model||null,purchase_date:toDate(p.purchase_date),location_name:p.location||null,status:p.status||'U društvu',next_service:toDate(p.next_service),note:p.note||null}));
    const ropes=(data.ropes||[]).map(r=>({legacy_id:r.id,sku:normalizedSku(r.sku||r.id),name:r.name,diameter_mm:String(r.diameter_mm||'').replace(',','.')||null,length_m:Number(r.length_m)||null,manufacturer:r.manufacturer||null,model:r.model||null,standard:r.standard||null,production_year:Number(r.year)||null,in_use_since:toDate(r.in_use_since),color:r.color||null,supplier:r.supplier||null,location_name:r.location||null,status:r.status||'U društvu',note:r.note||null,item_kind:'individual_rope',code_required:true}));
    const procurement=(data.procurement||[]).map(p=>({legacy_id:p.id,equipment_legacy_id:String(p.catalog_id||''),item_name:p.name,quantity:Number(p.quantity)||null,unit_price:Number(p.unit_price)||null,total_price:Number(p.total_price)||null,supplier:p.supplier||null,status:p.status||'Zaprimljeno',purchase_date:toDate(p.date),requested_by:p.person||null,note:p.note||null}));
    const disposals=[...(data.disposed||[]),...(data.lost||[])].map(d=>({legacy_id:d.id,disposal_date:toDate(d.date),disposal_type:d.type||'Rashod',equipment_legacy_id:String(d.catalog_id||''),item_name:d.name,quantity:Number(d.quantity)||null,reason:d.reason||null,location_name:d.location||null,person_name:d.person||null,note:d.note||null}));
    const field=(data.field||[]).map(f=>({legacy_id:f.id,recorded_at:toDate(f.date),equipment_legacy_id:String(f.catalog_id||''),item_name:f.name,quantity:Number(f.quantity)||null,field_location:f.location||f.reason||null,responsible_person:f.person||null,status:'na terenu',note:f.note||null}));
    const inventories=(data.inventories||[]).map(i=>({legacy_id:i.id,name:i.name,inventory_date:toDate(i.date)||new Date().toISOString().slice(0,10),owner_name:i.owner||null,status:i.status||'Završena',note:i.note||null}));
    const result={};
    result.categories=await upsertRows('equipment_categories',cats,'name');
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
      category_name:row.category_name || row.category || null,
      subcategory:row.subcategory || null,
      unit:row.unit || 'kom',
      tracking_type:row.tracking_type || 'po vrsti',
      quantity:Number(row.quantity)||1,
      loaned:0,
      available:Number(row.available)||Number(row.quantity)||1,
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
      diameter_mm:String(row.diameter_mm||'').replace(',','.') || null,
      length_m:Number(row.length_m)||null,
      manufacturer:row.manufacturer || null,
      model:row.model || null,
      standard:row.standard || null,
      production_year:Number(row.production_year)||null,
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

  window.SOVArmoryDB={configured,loadRequests,createRequest,updateRequestStatus,importStaticData,createEquipmentItem,createEquipmentPiece,createEquipmentPieces,createRope,updateEquipmentStatus};
})();
