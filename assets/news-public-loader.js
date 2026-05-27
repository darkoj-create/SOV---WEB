(async function(){
  const root=document.getElementById('dynamicNewsFromDb')||document.getElementById('newsList')||document.querySelector('[data-news-list]');
  if(!root || !window.supabase) return;
  const cfg=window.SOV_SUPABASE_CONFIG||window.SUPABASE_CONFIG||{};
  const url=window.SOV_SUPABASE_URL||cfg.url||cfg.SUPABASE_URL, key=window.SOV_SUPABASE_ANON_KEY||cfg.anonKey||cfg.SUPABASE_ANON_KEY;
  if(!url||!key) return;
  const sb=window.supabase.createClient(url,key);
  const {data,error}=await sb.from('sov_news').select('*').eq('published',true).order('pinned',{ascending:false}).order('published_at',{ascending:false}).limit(12);
  if(error || !data?.length) return;
  root.innerHTML = '<div class="section-head"><h2>Vijesti iz urednika</h2></div><div class="news-grid">'+data.map(n=>`<a class="news-card" href="${esc(n.cta_url||n.pdf_url||'#')}"><span class="news-img" style="background-image:url('${esc(n.image_url||'assets/sov-logo.png')}')"></span><span class="news-copy"><span class="news-meta">${n.pinned?'📌 ':''}${date(n.published_at)}</span><strong>${esc(n.title)}</strong><em>${esc(n.summary||n.body||'')}</em></span></a>`).join('')+'</div>';
  function esc(s){return String(s||'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));}
  function date(v){try{return new Date(v||Date.now()).toLocaleDateString('hr-HR')}catch(e){return ''}}
})();
