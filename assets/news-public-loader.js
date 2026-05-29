(function(){
  function esc(s){return String(s||'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));}
  function date(v){try{return new Date(v||Date.now()).toLocaleDateString('hr-HR')}catch(e){return ''}}
  function client(){
    if(!window.supabase) return null;
    const cfg=window.SOV_SUPABASE_CONFIG||window.SUPABASE_CONFIG||{};
    const url=window.SOV_SUPABASE_URL||cfg.url||cfg.SUPABASE_URL;
    const key=window.SOV_SUPABASE_ANON_KEY||cfg.anonKey||cfg.SUPABASE_ANON_KEY;
    return (url&&key)?window.supabase.createClient(url,key):null;
  }
  function linkFor(n){return 'vijest.html?slug='+encodeURIComponent(n.slug||n.id||'');}
  function category(n){return [n.category||'Novosti', date(n.published_at)].filter(Boolean).join(' · ');}
  async function fetchNews(sb){
    try{
      const rpc=await sb.rpc('sov_news_public_list',{p_limit:80});
      if(!rpc.error && rpc.data && rpc.data.length) return rpc.data;
    }catch(e){}
    const q=await sb.from('sov_news').select('id,slug,title,summary,category,image_url,image_alt,cta_url,published_at,pinned,featured').eq('published',true).order('pinned',{ascending:false}).order('featured',{ascending:false}).order('published_at',{ascending:false}).limit(80);
    if(q.error) throw q.error;
    return q.data||[];
  }
  function renderNewsSection(root, rows){
    if(!rows.length) return;
    const featured = rows.find(n=>n.featured) || rows[0];
    const side = rows.filter(n=>n!==featured).slice(0,2);
    const rest = rows.filter(n=>n!==featured && !side.includes(n)).slice(0,12);
    root.innerHTML = `
      <div class="section-head"><h2>Najnovije</h2><a class="news-edit-inline" href="news-editor.html" title="Uredi vijesti">✏️ Uredi vijesti</a></div>
      <div class="news-edit-entry"><a href="news-editor.html">✏️ Uredi / dodaj vijest</a></div>
      <div class="news-lead">
        ${card(featured,'featured')}
        <div class="news-stack">${side.map(n=>card(n,'side')).join('')}</div>
      </div>
      <div class="news-grid">${rest.map(n=>card(n,'grid')).join('')}</div>
    `;
  }
  function card(n,type){
    const cls=type==='featured'?'news-featured':(type==='side'?'news-side-card':'news-card');
    const img=n.image_url||'assets/sov-logo.png';
    return `<a class="${cls}" href="${esc(linkFor(n))}"><span class="news-img" style="background-image:url('${esc(img)}')"></span><span class="news-copy"><span class="news-meta">${esc((n.pinned?'📌 ':'')+category(n))}</span><strong>${esc(n.title||'Bez naslova')}</strong><em>${esc(n.summary||'')}</em></span></a>`;
  }
  async function init(){
    const root=document.querySelector('main.news-section')||document.getElementById('dynamicNewsFromDb')||document.getElementById('dynamicNewsHomepage')||document.querySelector('[data-news-list]');
    if(!root) return;
    const sb=client(); if(!sb) return;
    try{ const rows=await fetchNews(sb); if(rows.length) renderNewsSection(root,rows); }
    catch(e){ console.warn('SOV news public loader fallback to static content:',e); }
  }
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',init); else init();
})();
