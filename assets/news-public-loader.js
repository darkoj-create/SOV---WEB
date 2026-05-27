// Drop-in public news loader. Include on public/vijesti page where #newsList exists.
(async function(){
  const root=document.getElementById('newsList')||document.querySelector('[data-news-list]');
  if(!root || !window.supabase) return;
  const cfg=window.SOV_SUPABASE_CONFIG||window.SUPABASE_CONFIG||{};
  const sb=window.supabase.createClient(cfg.url||cfg.SUPABASE_URL,cfg.anonKey||cfg.SUPABASE_ANON_KEY);
  const {data,error}=await sb.from('sov_news').select('*').eq('published',true).order('pinned',{ascending:false}).order('published_at',{ascending:false});
  if(error || !data?.length) return;
  root.innerHTML=data.map(n=>`<article class="news-card ${n.pinned?'pinned':''}">${n.image_url?`<img class="news-hero" src="${n.image_url}" alt="">`:''}<div><h2>${esc(n.title)}</h2><p>${esc(n.summary||'')}</p>${n.body?`<p>${esc(n.body)}</p>`:''}<div class="news-actions">${n.cta_url?`<a class="btn" href="${n.cta_url}" target="_blank" rel="noopener">${esc(n.cta_label||'Otvori')}</a>`:''}${n.pdf_url?`<a class="btn ghost" href="${n.pdf_url}" target="_blank" rel="noopener">PDF</a>`:''}</div></div></article>`).join('');
  function esc(s){return String(s||'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));}
})();
