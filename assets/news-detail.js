(function(){
  const root=document.getElementById('newsDetail');
  if(!root) return;
  function esc(s){return String(s||'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));}
  function date(v){try{return new Date(v||Date.now()).toLocaleDateString('hr-HR',{day:'2-digit',month:'2-digit',year:'numeric'})}catch(e){return ''}}
  function qs(k){return new URLSearchParams(location.search).get(k)||'';}
  function client(){
    if(!window.supabase) return null;
    const cfg=window.SOV_SUPABASE_CONFIG||window.SUPABASE_CONFIG||{};
    const url=window.SOV_SUPABASE_URL||cfg.url||cfg.SUPABASE_URL;
    const key=window.SOV_SUPABASE_ANON_KEY||cfg.anonKey||cfg.SUPABASE_ANON_KEY;
    return (url&&key)?window.supabase.createClient(url,key):null;
  }
  function textToHtml(txt){return String(txt||'').split(/\n{2,}/).map(p=>p.trim()).filter(Boolean).map(p=>'<p>'+esc(p).replace(/\n/g,'<br>')+'</p>').join('\n');}
  async function load(){
    const slug=qs('slug');
    if(!slug){ root.innerHTML='<div class="empty">Nedostaje slug vijesti.</div>'; return; }
    const sb=client(); if(!sb){ root.innerHTML='<div class="empty">Supabase nije konfiguriran.</div>'; return; }
    let row=null;
    try{
      const rpc=await sb.rpc('sov_news_public_detail',{p_slug:slug});
      if(!rpc.error && rpc.data && rpc.data.length) row=rpc.data[0];
    }catch(e){}
    if(!row){
      const {data,error}=await sb.from('sov_news').select('*').eq('slug',slug).eq('published',true).maybeSingle();
      if(error){ root.innerHTML='<div class="empty">Greška učitavanja vijesti: '+esc(error.message)+'</div>'; return; }
      row=data;
    }
    if(!row){ root.innerHTML='<div class="empty">Vijest nije pronađena ili nije objavljena.</div>'; return; }
    document.title=(row.title||'Vijest')+' — SOV Novosti';
    const gallery=Array.isArray(row.gallery_urls)?row.gallery_urls.filter(Boolean):[];
    const attachments=Array.isArray(row.attachment_urls)?row.attachment_urls.filter(Boolean):[];
    const html=row.content_html || textToHtml(row.body);
    root.innerHTML=`
      <article class="detail-card">
        <a class="back" href="index.html">← Sve vijesti</a>
        <div class="detail-meta">${esc(row.category||'Novosti')} · ${date(row.published_at)}${row.author_name?' · '+esc(row.author_name):''}</div>
        <h1>${esc(row.title||'Bez naslova')}</h1>
        ${row.summary?`<p class="lead">${esc(row.summary)}</p>`:''}
        ${row.image_url?`<img class="hero-img" src="${esc(row.image_url)}" alt="${esc(row.image_alt||row.title||'')}" loading="eager">`:''}
        <div class="article-body">${html}</div>
        ${gallery.length?`<section class="gallery"><h2>Galerija</h2><div>${gallery.map(u=>`<a href="${esc(u)}" target="_blank" rel="noopener"><img src="${esc(u)}" alt="Galerija" loading="lazy"></a>`).join('')}</div></section>`:''}
        ${(row.pdf_url||attachments.length||row.cta_url)?`<section class="detail-actions">${row.pdf_url?`<a class="btn" href="${esc(row.pdf_url)}" target="_blank" rel="noopener">PDF / dokument</a>`:''}${attachments.map((u,i)=>`<a class="btn secondary" href="${esc(u)}" target="_blank" rel="noopener">Privitak ${i+1}</a>`).join('')}${row.cta_url && !String(row.cta_url).includes('vijest.html?')?`<a class="btn secondary" href="${esc(row.cta_url)}" target="_blank" rel="noopener">${esc(row.cta_label||'Otvori link')}</a>`:''}</section>`:''}
        <a class="edit-link" href="news-editor.html?edit=${encodeURIComponent(row.slug)}">✏️ Uredi ovu vijest</a>
      </article>`;
  }
  load();
})();
