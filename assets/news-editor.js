(function(){
  const cfg = window.SOV_SUPABASE_CONFIG || window.SUPABASE_CONFIG || {};
  const supabase = window.supabase?.createClient(cfg.url || cfg.SUPABASE_URL, cfg.anonKey || cfg.SUPABASE_ANON_KEY);
  const $ = id => document.getElementById(id);
  const status = $('status'), list = $('list'), dialog = $('editor');
  function msg(t){ status.textContent=t||''; }
  function dtLocal(v){ if(!v) return ''; const d=new Date(v); d.setMinutes(d.getMinutes()-d.getTimezoneOffset()); return d.toISOString().slice(0,16); }
  async function load(){
    msg('Učitavam vijesti...');
    const {data,error}=await supabase.from('sov_news').select('*').order('pinned',{ascending:false}).order('published_at',{ascending:false});
    if(error){ msg('Greška: '+error.message); return; }
    list.innerHTML = (data||[]).map(n=>`<article class="news-admin-card"><h3>${n.pinned?'📌 ':''}${escapeHtml(n.title)}</h3><p>${escapeHtml(n.summary||'')}</p><small>${n.published?'Objavljeno':'Skriveno'} · ${new Date(n.published_at||n.created_at).toLocaleString('hr-HR')}</small><br><button data-edit="${n.id}">Uredi</button></article>`).join('') || '<p>Nema vijesti.</p>';
    list.querySelectorAll('[data-edit]').forEach(b=>b.onclick=()=>edit(data.find(x=>x.id===b.dataset.edit)));
    msg('');
  }
  function edit(n){
    $('modalTitle').textContent = n?'Uredi vijest':'Nova vijest';
    ['id','title','summary','body','image_url','pdf_url','cta_label','cta_url'].forEach(k=>$(k).value=n?.[k]||'');
    $('published').checked = n ? !!n.published : true;
    $('pinned').checked = n ? !!n.pinned : false;
    $('published_at').value = dtLocal(n?.published_at || new Date().toISOString());
    dialog.showModal();
  }
  $('newBtn').onclick=()=>edit(null);
  $('saveBtn').onclick=async(ev)=>{
    ev.preventDefault();
    const id=$('id').value;
    const row={title:$('title').value.trim(),summary:$('summary').value.trim(),body:$('body').value.trim(),image_url:$('image_url').value.trim(),pdf_url:$('pdf_url').value.trim(),cta_label:$('cta_label').value.trim(),cta_url:$('cta_url').value.trim(),published:$('published').checked,pinned:$('pinned').checked,published_at:$('published_at').value?new Date($('published_at').value).toISOString():new Date().toISOString()};
    const q = id ? supabase.from('sov_news').update(row).eq('id',id) : supabase.from('sov_news').insert(row);
    const {error}=await q;
    if(error){ msg('Greška: '+error.message); return; }
    dialog.close(); await load();
  };
  function escapeHtml(s){return String(s||'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));}
  load();
})();
