(function(){
  const $ = id => document.getElementById(id);
  const els = {
    list:$('list'), status:$('status'), form:$('form'), formTitle:$('formTitle'),
    id:$('id'), title:$('title'), slug:$('slug'), category:$('category'), author_name:$('author_name'), published_at:$('published_at'),
    summary:$('summary'), image_url:$('image_url'), image_alt:$('image_alt'), coverPreview:$('coverPreview'), body:$('body'),
    gallery_urls:$('gallery_urls'), pdf_url:$('pdf_url'), cta_label:$('cta_label'), cta_url:$('cta_url'),
    published:$('published'), pinned:$('pinned'), featured:$('featured'), search:$('search'), filter:$('filter'),
    coverFile:$('coverFile'), galleryFiles:$('galleryFiles'), openPublicBtn:$('openPublicBtn')
  };
  let sb=null, rows=[], selected=null, dirtySlug=false;

  function esc(s){return String(s||'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));}
  function msg(t){ els.status.textContent=t||''; }
  function norm(s){return String(s||'').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'');}
  function slugify(s){return norm(s).replace(/đ/g,'d').replace(/[^a-z0-9]+/g,'-').replace(/^-+|-+$/g,'').slice(0,90);}
  function dtLocal(v){ if(!v) return ''; const d=new Date(v); if(Number.isNaN(d.getTime())) return ''; d.setMinutes(d.getMinutes()-d.getTimezoneOffset()); return d.toISOString().slice(0,16); }
  function isoFromLocal(v){ return v ? new Date(v).toISOString() : new Date().toISOString(); }
  function dateText(v){ const d=new Date(v||Date.now()); return Number.isNaN(d.getTime())?'bez datuma':d.toLocaleDateString('hr-HR'); }
  function linesToArray(v){return String(v||'').split('\n').map(x=>x.trim()).filter(Boolean);}
  function arrayToLines(a){return Array.isArray(a)?a.filter(Boolean).join('\n'):'';}
  function bodyToHtml(txt){return String(txt||'').split(/\n{2,}/).map(p=>p.trim()).filter(Boolean).map(p=>'<p>'+esc(p).replace(/\n/g,'<br>')+'</p>').join('\n');}
  function getClient(){
    if(sb) return sb;
    if(window.SOVAuth && SOVAuth.getClient) sb=SOVAuth.getClient();
    if(!sb && window.supabase){
      const cfg=window.SOV_SUPABASE_CONFIG||window.SUPABASE_CONFIG||{};
      const url=window.SOV_SUPABASE_URL||cfg.url||cfg.SUPABASE_URL;
      const key=window.SOV_SUPABASE_ANON_KEY||cfg.anonKey||cfg.SUPABASE_ANON_KEY;
      if(url&&key) sb=window.supabase.createClient(url,key,{auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:true}});
    }
    return sb;
  }
  async function boot(){
    if(window.SOVAuth && SOVAuth.ready){ try{await SOVAuth.ready();}catch(e){} }
    sb=getClient();
    if(!sb){ els.list.innerHTML='<div class="empty">Supabase nije konfiguriran.</div>'; return; }
    bind(); await load();
    const qs=new URLSearchParams(location.search); const edit=qs.get('edit')||qs.get('slug');
    if(edit){ const row=rows.find(r=>r.slug===edit || String(r.id)===edit); if(row) open(row); }
  }
  function bind(){
    $('newBtn').onclick=()=>open(null);
    $('refreshBtn').onclick=load;
    $('clearBtn').onclick=()=>open(null);
    $('deleteBtn').onclick=removeSelected;
    $('duplicateBtn').onclick=duplicateSelected;
    $('uploadCoverBtn').onclick=uploadCover;
    $('uploadGalleryBtn').onclick=uploadGallery;
    els.form.onsubmit=save;
    els.search.oninput=renderList;
    els.filter.onchange=renderList;
    els.image_url.oninput=updateCover;
    els.title.oninput=()=>{ if(!dirtySlug && !els.id.value) els.slug.value=slugify(els.title.value); renderOpenLink(); };
    els.slug.oninput=()=>{ dirtySlug=true; renderOpenLink(); };
  }
  async function load(){
    msg('Učitavam vijesti iz baze...');
    const {data,error}=await sb.from('sov_news').select('*').order('pinned',{ascending:false}).order('featured',{ascending:false}).order('published_at',{ascending:false}).order('created_at',{ascending:false});
    if(error){ els.list.innerHTML='<div class="empty">Ne mogu učitati vijesti. Provjeri prijavu, rolu urednika i Supabase pristup.<br>'+esc(error.message)+'</div>'; msg('Greška učitavanja.'); return; }
    rows=data||[]; renderList(); msg(rows.length?`Učitano ${rows.length} vijesti.`:'Nema vijesti u bazi. Dodaj prvu vijest.');
  }
  function renderList(){
    const q=norm(els.search.value), f=els.filter.value;
    const view=rows.filter(n=>{
      if(f==='published'&&!n.published) return false;
      if(f==='hidden'&&n.published) return false;
      if(f==='pinned'&&!n.pinned) return false;
      if(f==='featured'&&!n.featured) return false;
      if(f==='submitted'&&!String(n.source||'').includes('user-submission')) return false;
      if(q&&!norm([n.title,n.summary,n.body,n.category,n.slug].join(' ')).includes(q)) return false;
      return true;
    });
    els.list.innerHTML=view.map(n=>`<button class="news-row ${selected&&selected.id===n.id?'active':''}" data-id="${esc(n.id)}"><span class="thumb" style="background-image:url('${esc(n.image_url||'assets/sov-logo.png')}')"></span><span><strong>${esc(n.title||'Bez naslova')}</strong><span class="meta">${esc(n.category||'Novosti')} · ${dateText(n.published_at||n.created_at)}</span><span class="badges"><span class="badge ${n.published?'live':'hidden'}">${n.published?'objavljeno':'skriveno'}</span>${n.pinned?'<span class="badge">📌 vrh</span>':''}${n.featured?'<span class="badge">featured</span>':''}${String(n.source||'').includes('user-submission')?'<span class="badge">predao član</span>':''}</span></span></button>`).join('') || '<div class="empty">Nema rezultata.</div>';
    els.list.querySelectorAll('[data-id]').forEach(b=>b.onclick=()=>open(rows.find(r=>String(r.id)===String(b.dataset.id))));
  }
  function open(n){
    selected=n||null; dirtySlug=!!n;
    els.formTitle.textContent=n?'Uredi vijest':'Nova vijest';
    els.id.value=n?.id||''; els.title.value=n?.title||''; els.slug.value=n?.slug||''; els.category.value=n?.category||'Novosti'; els.author_name.value=n?.author_name||'';
    els.published_at.value=dtLocal(n?.published_at||new Date().toISOString()); els.summary.value=n?.summary||''; els.image_url.value=n?.image_url||''; els.image_alt.value=n?.image_alt||'';
    els.body.value=n?.body||htmlToPlain(n?.content_html)||''; els.gallery_urls.value=arrayToLines(n?.gallery_urls); els.pdf_url.value=n?.pdf_url||''; els.cta_label.value=n?.cta_label||''; els.cta_url.value=n?.cta_url||'';
    els.published.checked=n?!!n.published:true; els.pinned.checked=n?!!n.pinned:false; els.featured.checked=n?!!n.featured:false;
    updateCover(); renderOpenLink(); renderList(); msg(n?'Vijest otvorena za uređivanje.':'Nova vijest.');
  }
  function htmlToPlain(html){
    if(!html) return '';
    const d=document.createElement('div'); d.innerHTML=html;
    d.querySelectorAll('br').forEach(br=>br.replaceWith('\n'));
    d.querySelectorAll('p,div,h2,h3,li').forEach(el=>el.appendChild(document.createTextNode('\n\n')));
    return d.textContent.replace(/\n{3,}/g,'\n\n').trim();
  }
  function rowFromForm(){
    const slug=els.slug.value.trim()||slugify(els.title.value);
    const body=els.body.value.trim();
    return {
      title:els.title.value.trim(), slug, category:els.category.value.trim()||'Novosti', author_name:els.author_name.value.trim(),
      published_at:isoFromLocal(els.published_at.value), summary:els.summary.value.trim(), image_url:els.image_url.value.trim(), image_alt:els.image_alt.value.trim(),
      body, content_html:body ? bodyToHtml(body) : (selected?.content_html||''), gallery_urls:linesToArray(els.gallery_urls.value), pdf_url:els.pdf_url.value.trim(),
      cta_label:els.cta_label.value.trim(), cta_url:els.cta_url.value.trim() || ('vijest.html?slug='+encodeURIComponent(slug)),
      published:els.published.checked, pinned:els.pinned.checked, featured:els.featured.checked
    };
  }
  async function save(ev){
    ev.preventDefault();
    const row=rowFromForm();
    if(!row.title){ msg('Naslov je obavezan.'); return; }
    const id=els.id.value || null;
    msg(id?'Spremam izmjene...':'Spremam novu vijest...');

    // v5.58.9: save preko RPC-a da izbjegnemo Supabase/PostgREST .single() grešku
    // "Cannot coerce the result to a single JSON object". RPC uvijek vraća jedan JSON objekt
    // ili jasnu grešku. Ako SQL još nije pokrenut, postoji fallback bez .single().
    let saved=null;
    try{
      const rpc=await sb.rpc('sov_news_save',{p_id:id,p_payload:row});
      if(rpc.error) throw rpc.error;
      saved=rpc.data || null;
    }catch(e){
      const msgText=String(e && e.message || e || '');
      const rpcMissing=/sov_news_save|function .* does not exist|Could not find the function/i.test(msgText);
      if(!rpcMissing){ msg('Greška spremanja: '+msgText); return; }

      // Fallback za slučaj da web ode gore prije SQL-a: nikad ne koristimo .single().
      const q=id
        ? await sb.from('sov_news').update(row).eq('id',id).select().limit(1)
        : await sb.from('sov_news').insert(row).select().limit(1);
      if(q.error){ msg('Greška spremanja: '+q.error.message); return; }
      saved=Array.isArray(q.data) ? q.data[0] : q.data;
      if(!saved){
        msg(id
          ? 'Spremanje nije vratilo vijest. Provjeri prava za urednika i osvježi listu.'
          : 'Nova vijest je možda spremljena, ali je baza nije vratila. Osvježi listu i provjeri prava za urednika.');
        await load();
        return;
      }
    }

    selected=saved;
    await load();
    open(rows.find(r=>String(r.id)===String(saved.id))||saved);
    msg('Spremljeno.');
  }
  async function uploadFile(file,folder){
    if(!file) throw new Error('Nema datoteke.');
    const safe=file.name.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-z0-9.]+/g,'-').replace(/^-+|-+$/g,'');
    const path=`${folder}/${Date.now()}-${Math.random().toString(16).slice(2)}-${safe}`;
    const {error}=await sb.storage.from('sov-news').upload(path,file,{cacheControl:'3600',upsert:true,contentType:file.type||undefined});
    if(error) throw error;
    const {data}=sb.storage.from('sov-news').getPublicUrl(path);
    return data.publicUrl;
  }
  async function uploadCover(){
    const file=els.coverFile.files && els.coverFile.files[0];
    if(!file){ msg('Odaberi naslovnu fotografiju.'); return; }
    try{ msg('Uploadam naslovnu fotku...'); const url=await uploadFile(file,'covers'); els.image_url.value=url; updateCover(); msg('Fotka uploadana. Ne zaboravi spremiti vijest.'); }
    catch(e){ msg('Greška uploada: '+e.message+' — provjeri prijavu, rolu urednika i pristup sov-news bucketu.'); }
  }
  async function uploadGallery(){
    const files=[...(els.galleryFiles.files||[])];
    if(!files.length){ msg('Odaberi jednu ili više fotki za galeriju.'); return; }
    try{
      msg('Uploadam galeriju...'); const urls=[];
      for(const file of files) urls.push(await uploadFile(file,'gallery'));
      const current=linesToArray(els.gallery_urls.value); els.gallery_urls.value=[...current,...urls].join('\n'); msg(`Uploadano ${urls.length} fotki. Ne zaboravi spremiti vijest.`);
    }catch(e){ msg('Greška uploada galerije: '+e.message+' — provjeri prijavu, rolu urednika i pristup sov-news bucketu.'); }
  }
  function updateCover(){ const v=els.image_url.value.trim(); els.coverPreview.style.backgroundImage=v?`url('${v.replace(/'/g,'%27')}')`:''; els.coverPreview.innerHTML='<span>'+esc(v?'Preview naslovne fotografije':'Nema naslovne fotografije')+'</span>'; }
  function renderOpenLink(){ const slug=els.slug.value.trim()||slugify(els.title.value); els.openPublicBtn.href=slug?'vijest.html?slug='+encodeURIComponent(slug):'vijest.html'; }
  async function duplicateSelected(){
    if(!selected){ msg('Prvo odaberi vijest.'); return; }
    const copy={...selected,title:(selected.title||'Vijest')+' — kopija',slug:(selected.slug||slugify(selected.title||'vijest'))+'-kopija',published:false,pinned:false,featured:false};
    delete copy.id; delete copy.created_at; delete copy.updated_at; delete copy.created_by; delete copy.updated_by;
    msg('Dupliciram...');
    const rpc=await sb.rpc('sov_news_save',{p_id:null,p_payload:copy});
    if(rpc.error){ msg('Greška dupliciranja: '+rpc.error.message); return; }
    await load(); open(rows.find(r=>String(r.id)===String(rpc.data && rpc.data.id))||rpc.data||null); msg('Duplicirano kao skrivena kopija.');
  }
  async function removeSelected(){
    if(!selected){ msg('Prvo odaberi vijest.'); return; }
    if(!confirm('Obrisati vijest "'+(selected.title||'')+'"?')) return;
    msg('Brišem...');
    try{
      const rpc=await sb.rpc('sov_news_delete',{p_id:selected.id});
      if(rpc.error) throw rpc.error;
    }catch(e){
      const msgText=String(e && e.message || e || '');
      const rpcMissing=/sov_news_delete|function .* does not exist|Could not find the function/i.test(msgText);
      if(!rpcMissing){ msg('Greška brisanja: '+msgText); return; }
      const {error}=await sb.from('sov_news').delete().eq('id',selected.id);
      if(error){ msg('Greška brisanja: '+error.message); return; }
    }
    selected=null; await load(); open(null); msg('Obrisano.');
  }
  document.addEventListener('DOMContentLoaded',boot);
})();
