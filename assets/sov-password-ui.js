(function(){
  const STYLE_ID='sov-password-ui-style';
  function ensureStyle(){
    if(document.getElementById(STYLE_ID)) return;
    const style=document.createElement('style');
    style.id=STYLE_ID;
    style.textContent=`
      .sov-password-wrap{position:relative;display:block;width:100%}
      .sov-password-wrap>input{padding-right:52px!important}
      .sov-password-toggle{position:absolute;right:8px;top:50%;transform:translateY(-50%);width:38px;height:38px;border:0;border-radius:999px;background:transparent;color:inherit;display:grid;place-items:center;cursor:pointer;opacity:.78;padding:0;z-index:2}
      .sov-password-toggle:hover,.sov-password-toggle:focus-visible{opacity:1;background:rgba(255,255,255,.08);outline:1px solid rgba(255,255,255,.18)}
      .sov-password-toggle svg{width:20px;height:20px;display:block;pointer-events:none}
    `;
    document.head.appendChild(style);
  }
  const eyeOpen='<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg>';
  const eyeClosed='<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 3 18 18"/><path d="M10.6 10.6a2 2 0 0 0 2.8 2.8"/><path d="M9.9 5.1A11 11 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-2.1 3.1"/><path d="M6.6 6.6C3.6 8.4 2 12 2 12s3.5 7 10 7a10 10 0 0 0 4.1-.9"/></svg>';
  function enhance(input){
    if(!input || input.dataset.sovPasswordUi==='1') return;
    input.dataset.sovPasswordUi='1';
    ensureStyle();
    const wrap=document.createElement('span');
    wrap.className='sov-password-wrap';
    input.parentNode.insertBefore(wrap,input);
    wrap.appendChild(input);
    const btn=document.createElement('button');
    btn.type='button';
    btn.className='sov-password-toggle';
    btn.setAttribute('aria-label','Prikaži lozinku');
    btn.setAttribute('title','Prikaži lozinku');
    btn.innerHTML=eyeOpen;
    btn.addEventListener('click',()=>{
      const show=input.type==='password';
      input.type=show?'text':'password';
      btn.setAttribute('aria-label',show?'Sakrij lozinku':'Prikaži lozinku');
      btn.setAttribute('title',show?'Sakrij lozinku':'Prikaži lozinku');
      btn.innerHTML=show?eyeClosed:eyeOpen;
      input.focus({preventScroll:true});
      try{input.setSelectionRange(input.value.length,input.value.length)}catch(e){}
    });
    wrap.appendChild(btn);
  }
  function scan(root=document){
    if(root.matches && root.matches('input[type="password"]')) enhance(root);
    if(root.querySelectorAll) root.querySelectorAll('input[type="password"]').forEach(enhance);
  }
  function boot(){
    scan(document);
    new MutationObserver(mutations=>mutations.forEach(m=>m.addedNodes.forEach(n=>{if(n.nodeType===1)scan(n)}))).observe(document.documentElement,{childList:true,subtree:true});
  }
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',boot,{once:true}); else boot();
})();
