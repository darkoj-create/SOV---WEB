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
  function setMeta(row){
    document.title=(row.title||'Vijest')+' — SOV Velebit';
    const desc=(row.summary||row.body||'').replace(/\s+/g,' ').trim().slice(0,220);
    const description=document.querySelector('meta[name="description"]');
    if(description&&desc) description.setAttribute('content',desc);
    const ogTitle=document.querySelector('meta[property="og:title"]');
    if(ogTitle) ogTitle.setAttribute('content',row.title||'SOV Novosti');
    const ogImage=document.querySelector('meta[property="og:image"]');
    if(ogImage&&row.image_url) ogImage.setAttribute('content',row.image_url);
  }

  async function load(){
    const slug=qs('slug');
    if(!slug){ root.innerHTML='<div class="empty">Nedostaje oznaka vijesti.</div>'; return; }
    const sb=client();
    if(!sb){ root.innerHTML='<div class="empty">Vijest se trenutno ne može učitati.</div>'; return; }

    let row=null;
    try{
      const rpc=await sb.rpc('sov_news_public_detail',{p_slug:slug});
      if(!rpc.error&&rpc.data&&rpc.data.length) row=rpc.data[0];
    }catch(e){}
    if(!row){
      const {data,error}=await sb.from('sov_news').select('*').eq('slug',slug).eq('published',true).maybeSingle();
      if(error){ root.innerHTML='<div class="empty">Vijest se trenutno ne može učitati.</div>'; return; }
      row=data;
    }
    if(!row){ root.innerHTML='<div class="empty">Vijest nije pronađena ili nije objavljena.</div>'; return; }

    setMeta(row);
    const gallery=Array.isArray(row.gallery_urls)?row.gallery_urls.filter(Boolean):[];
    const attachments=Array.isArray(row.attachment_urls)?row.attachment_urls.filter(Boolean):[];
    const html=row.content_html||textToHtml(row.body);
    const meta=[row.category||'Novosti',date(row.published_at),row.author_name||''].filter(Boolean).map(esc).join(' · ');
    const hasImage=!!row.image_url;

    root.innerHTML=`
      <article class="sov-news-article">
        <section class="sov-news-hero ${hasImage?'':'no-image'}">
          ${hasImage?`<img class="sov-news-hero-img" src="${esc(row.image_url)}" alt="${esc(row.image_alt||row.title||'')}" loading="eager">`:''}
          <div class="sov-news-hero-inner">
            <a class="sov-news-back" href="vijesti.html">← Sve objave</a>
            <div class="sov-news-meta">${meta}</div>
            <h1 class="sov-news-title">${esc(row.title||'Bez naslova')}</h1>
            ${row.summary?`<p class="sov-news-lead">${esc(row.summary)}</p>`:''}
          </div>
        </section>
        <section class="sov-news-paper">
          <div class="sov-news-copy">
            <div class="sov-news-main">
              <div class="article-body">${html}</div>
              ${gallery.length?`<section class="sov-news-gallery"><h2>Galerija</h2><div class="sov-news-gallery-grid">${gallery.map(u=>`<a href="${esc(u)}" target="_blank" rel="noopener"><img src="${esc(u)}" alt="" loading="lazy"></a>`).join('')}</div></section>`:''}
              ${(row.pdf_url||attachments.length||row.cta_url)?`<section class="sov-news-actions">${row.pdf_url?`<a class="btn" href="${esc(row.pdf_url)}" target="_blank" rel="noopener">Dokument</a>`:''}${attachments.map((u,i)=>`<a class="btn secondary" href="${esc(u)}" target="_blank" rel="noopener">Privitak ${i+1}</a>`).join('')}${row.cta_url&&!String(row.cta_url).includes('vijest.html?')?`<a class="btn secondary" href="${esc(row.cta_url)}" target="_blank" rel="noopener">${esc(row.cta_label||'Otvori')}</a>`:''}</section>`:''}
              <a class="sov-news-edit" href="news-editor.html?edit=${encodeURIComponent(row.slug)}">Uredi vijest</a>
            </div>
            <aside class="sov-news-side" aria-label="O objavi">
              <strong>SOV Velebit</strong>
              <div>${meta}</div>
              <a href="vijesti.html">Sve objave</a>
              <a href="index.html">Naslovnica</a>
              <a href="speleoskola.html">Speleoškola</a>
            </aside>
          </div>
        </section>
      </article>`;
  }
  load();
})();
