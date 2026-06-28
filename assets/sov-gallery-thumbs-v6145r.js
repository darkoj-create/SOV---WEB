/* SOV v6.1.45r — fill empty gallery/archive thumbnails with existing article images. */
(function(){
  const fallbackByHref={
    'otvorene-prijave-za-54-speleolosku-skolu':'assets/legacy-wordpress-thumbs/2024/03/5854-osvrt-na-prvi-izlet-54-skole.webp',
    'speleoloska-ekspedicija-nedam-2020':'assets/legacy-wordpress-thumbs/2020/08/4044-novosti-iz-jame-nedam.webp',
    'odgadaju-se-sastanci-i-50-skola':'assets/legacy-wordpress-thumbs/2020/09/4086-pocinje-50-speleoskola.webp',
    'nedam-na-740':'assets/legacy-wordpress-thumbs/2020/07/3965-jama-nedam-ide-dalje.webp',
    'vodic-kroz-namibiju-za-velebitange':'assets/legacy-wordpress-thumbs/2017/06/850-maroko-u-mom-oku.webp',
    'uskrsnja-kita':'assets/legacy-wordpress-thumbs/2017/12/1704-128-istrazivanje-kite-iliti-34-282-m.webp'
  };
  const pool=[
    'assets/legacy-wordpress-thumbs/2026/04/6806-6806.webp',
    'assets/legacy-wordpress-thumbs/2025/05/6433-suze-u-munizabi.webp',
    'assets/legacy-wordpress-thumbs/2025/07/6504-bez-soma-nema-doma.webp',
    'assets/legacy-wordpress-thumbs/2023/09/5635-speleoloska-ekspedicija-sjeverni-velebit-2023.webp',
    'assets/legacy-wordpress-thumbs/2021/04/4385-munizaba-pregled-istrazivanja.webp',
    'assets/legacy-wordpress-thumbs/2019/05/2443-izlet-speleoskole-na-ponorac.webp'
  ];
  function hasRealImage(el){
    const s=(el.getAttribute('style')||'').toLowerCase();
    return s.includes('background-image') && !s.includes('undefined') && !s.includes("url('')") && !s.includes('url("")') && !s.includes('placeholder');
  }
  function hydrate(){
    let i=0;
    document.querySelectorAll('a.archive-card,a.news-card,a.news-side-card,a.news-featured').forEach(card=>{
      const img=card.querySelector('.archive-img,.news-img');
      if(!img || hasRealImage(img)) return;
      const href=(card.getAttribute('href')||'').toLowerCase();
      const key=Object.keys(fallbackByHref).find(k=>href.includes(k));
      const src=key ? fallbackByHref[key] : pool[i++ % pool.length];
      img.classList.remove('sov-thumb-fallback');
      img.style.backgroundImage=`url('${src}')`;
      img.setAttribute('data-thumb-source','article-fallback');
    });
  }
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',hydrate); else hydrate();
})();
