/* ============================================================================
   SOV Web — WOW Layer v6.0 engine
   Orchestrates: scroll progress · pointer aurora · staggered page-load ·
   scroll-reveal (mutation-aware for Supabase-injected news) · magnetic CTAs ·
   count-up hero stats · premium news-loading skeleton.
   Safe: feature-detected, reduced-motion aware, never throws, never removes data hooks.
   ============================================================================ */
(function(){
  "use strict";
  var REDUCED = window.matchMedia && window.matchMedia("(prefers-reduced-motion:reduce)").matches;
  var FINE    = window.matchMedia && window.matchMedia("(hover:hover) and (pointer:fine)").matches;
  var PAGE    = (location.pathname.split("/").pop()||"index.html").toLowerCase();
  var IS_HOME = PAGE==="index.html" || PAGE==="";
  var body    = document.body;

  function ready(fn){ if(document.readyState!=="loading") fn(); else document.addEventListener("DOMContentLoaded",fn); }

  /* ---------- 1. scroll progress bar ---------- */
  function progressBar(){
    if(REDUCED) return;
    var bar=document.createElement("div");
    bar.className="wow-progress";
    document.documentElement.appendChild(bar);
    var raf;
    function upd(){
      var h=document.documentElement;
      var max=(h.scrollHeight-h.clientHeight)||1;
      var p=Math.min(1,Math.max(0,(h.scrollTop||window.pageYOffset)/max));
      bar.style.width=(p*100).toFixed(2)+"%";
      raf=null;
    }
    window.addEventListener("scroll",function(){ if(!raf) raf=requestAnimationFrame(upd); },{passive:true});
    window.addEventListener("resize",upd,{passive:true}); upd();
  }

  /* ---------- 2. pointer aurora on heroes ---------- */
  function aurora(){
    if(REDUCED||!FINE) return;
    var heroes=document.querySelectorAll(".portal-hero,.about-hero-polish,.about-hero");
    heroes.forEach(function(hero){
      var a=document.createElement("div"); a.className="wow-aurora"; hero.prepend(a);
      hero.addEventListener("pointermove",function(e){
        var r=hero.getBoundingClientRect();
        a.style.setProperty("--wow-x",((e.clientX-r.left)/r.width*100)+"%");
        a.style.setProperty("--wow-y",((e.clientY-r.top)/r.height*100)+"%");
        a.classList.add("is-live");
      });
      hero.addEventListener("pointerleave",function(){ a.classList.remove("is-live"); });
    });
  }

  /* ---------- 3. page-load orchestration ---------- */
  function pageReady(){ requestAnimationFrame(function(){ body.classList.add("wow-ready"); }); }

  /* ---------- 4. scroll reveal (mutation-aware) ---------- */
  var io=null;
  function ensureObserver(){
    if(io||!("IntersectionObserver" in window)) return io;
    io=new IntersectionObserver(function(entries){
      entries.forEach(function(en){
        if(en.isIntersecting){ en.target.classList.add("is-in"); io.unobserve(en.target); }
      });
    },{threshold:.12,rootMargin:"0px 0px -8% 0px"});
    return io;
  }
  function tag(el,type){
    if(!el||el.hasAttribute("data-reveal")) return;
    el.setAttribute("data-reveal",type||"");
    if(REDUCED||!("IntersectionObserver" in window)){ el.classList.add("is-in"); return; }
    ensureObserver().observe(el);
  }
  var REVEAL_SELECTORS=[
    ".expedition-banner-section",".section-head",
    ".news-featured",".news-side-card",".news-card",
    ".photo-tile",".video-band .youtube-channel",".video-mini",
    ".contact-card",".link-card",".quick-inner a",
    ".about-block",".about-feature-grid article",".about-route-grid a",
    ".about-switchboard a",".glass-card",".doc-card",
    "body.sov-cloud-page .sov-module","body.sov-cloud-page .sov-hero"
  ];
  function applyReveals(scope){
    (scope||document).querySelectorAll(REVEAL_SELECTORS.join(",")).forEach(function(el){ tag(el,""); });
  }
  function watchNews(){
    var root=document.querySelector("main.news-section")||document.querySelector("[data-news-list]");
    if(!root||!("MutationObserver" in window)) return;
    var mo=new MutationObserver(function(muts){
      muts.forEach(function(m){ if(m.addedNodes && m.addedNodes.length) applyReveals(root); });
      // home: cards arrive already in view, so reveal them immediately
      if(IS_HOME) root.querySelectorAll("[data-reveal]").forEach(function(el){
        var r=el.getBoundingClientRect(); if(r.top<window.innerHeight*1.1) el.classList.add("is-in");
      });
    });
    mo.observe(root,{childList:true,subtree:true});
  }

  /* ---------- 5. magnetic CTAs ---------- */
  function magnetic(){
    if(REDUCED||!FINE) return;
    document.querySelectorAll(".hero-join,.about-hero-actions a:first-child,.expedition-banner-cta").forEach(function(btn){
      btn.addEventListener("pointermove",function(e){
        var r=btn.getBoundingClientRect();
        var x=(e.clientX-r.left-r.width/2)/r.width;
        var y=(e.clientY-r.top-r.height/2)/r.height;
        btn.style.transform="translate("+(x*10).toFixed(1)+"px,"+(y*8).toFixed(1)+"px)";
      });
      btn.addEventListener("pointerleave",function(){ btn.style.transform=""; });
    });
  }

  /* ---------- 6. count-up hero stats (homepage) ---------- */
  function heroStats(){
    if(!IS_HOME) return;
    var inner=document.querySelector(".portal-hero .portal-hero-inner");
    if(!inner||inner.querySelector(".hero-stats")) return;
    // Edit these to your real club figures — labels are Croatian to match the site.
    var STATS=[
      {n:1954,label:"prva speleo sekcija u Velebitu",raw:true},
      {n:56,label:"zagrebačkih speleoškola"},
      {n:1500,suffix:"+",label:"istraženih objekata u bazi"},
      {n:70,suffix:" god.",label:"terena, ekspedicija i podzemlja"}
    ];
    var wrap=document.createElement("div"); wrap.className="hero-stats";
    STATS.forEach(function(s){
      var d=document.createElement("div"); d.className="hero-stat";
      d.innerHTML="<b data-target='"+s.n+"' data-suffix='"+(s.suffix||"")+"' data-raw='"+(s.raw?1:0)+"'>0</b><span>"+s.label+"</span>";
      wrap.appendChild(d);
    });
    var actions=inner.querySelector(".hero-actions");
    if(actions) actions.after(wrap); else inner.appendChild(wrap);
    runCounts(wrap);
  }
  function runCounts(wrap){
    wrap.querySelectorAll("b[data-target]").forEach(function(el){
      var target=parseInt(el.getAttribute("data-target"),10)||0;
      var suffix=el.getAttribute("data-suffix")||"";
      var raw=el.getAttribute("data-raw")==="1"; // raw years not formatted with thousands
      if(REDUCED){ el.textContent=fmt(target,raw)+suffix; return; }
      var start=null, dur=1500;
      function step(t){
        if(!start) start=t;
        var p=Math.min(1,(t-start)/dur);
        var eased=1-Math.pow(1-p,3);
        el.textContent=fmt(Math.round(target*eased),raw)+suffix;
        if(p<1) requestAnimationFrame(step); else el.textContent=fmt(target,raw)+suffix;
      }
      requestAnimationFrame(step);
    });
  }
  function fmt(v,raw){ return raw ? String(v) : v.toLocaleString("hr-HR"); }

  /* ---------- 7. premium news skeleton while Supabase loads ---------- */
  function newsSkeleton(){
    if(!IS_HOME) return;
    var root=document.querySelector("main.news-section");
    if(!root) return;
    // only show skeleton if news will be replaced (supabase available) and not yet loaded
    if(!window.supabase) return;
    var hasReal=root.querySelector(".news-featured,.news-card");
    // Keep existing static content as the graceful fallback; just add a shimmer overlay
    // hint at the top so the swap feels intentional, removed once loader runs.
    var sk=document.createElement("div"); sk.className="wow-skeleton"; sk.setAttribute("aria-hidden","true");
    sk.style.cssText="margin:18px auto 0;width:min(1180px,calc(100% - 36px))";
    sk.innerHTML="<div class='sk-lead'><div class='sk-box'></div><div class='sk-box'></div></div>";
    // We do NOT remove static content (it is the fallback). Skeleton only appears
    // if the section is genuinely empty (e.g. JS-rendered empty shell).
    if(!hasReal) root.prepend(sk);
  }

  /* ---------- boot ---------- */
  ready(function(){
    try{ progressBar(); }catch(e){}
    try{ aurora(); }catch(e){}
    try{ heroStats(); }catch(e){}
    try{ applyReveals(document); }catch(e){}
    try{ watchNews(); }catch(e){}
    try{ magnetic(); }catch(e){}
    try{ newsSkeleton(); }catch(e){}
    try{ pageReady(); }catch(e){}
  });
})();
