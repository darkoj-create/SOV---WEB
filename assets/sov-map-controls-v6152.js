/* SOV v6.1.52 — independent Karta toggle: only objects with drawings. */
(function(){
  'use strict';
  let drawingsOnly=false;
  function hasDrawing(o){
    try{return typeof drawingsForObject==='function' && drawingsForObject(o).length>0}catch(e){return false}
  }
  function install(){
    try{
      if(typeof matchesFilter!=='function'||typeof render!=='function')return;
      if(window.__SOV_DRAWINGS_FILTER_INSTALLED)return;
      window.__SOV_DRAWINGS_FILTER_INSTALLED=true;
      const nativeMatches=matchesFilter;
      matchesFilter=function(o){return nativeMatches(o)&&(!drawingsOnly||hasDrawing(o));};
      const chips=document.querySelector('.searchCard .chips');
      if(!chips)return;
      const btn=document.createElement('button');
      btn.type='button';
      btn.className='chip sov-drawings-only';
      btn.id='drawingsOnlyChip';
      btn.textContent='Samo s nacrtima';
      btn.setAttribute('aria-pressed','false');
      const syncButton=()=>{
        btn.classList.toggle('active',drawingsOnly);
        btn.setAttribute('aria-pressed',String(drawingsOnly));
      };
      btn.addEventListener('click',()=>{drawingsOnly=!drawingsOnly;syncButton();render();});
      chips.appendChild(btn);
      if(typeof setFilter==='function'){
        const nativeSetFilter=setFilter;
        setFilter=function(f){nativeSetFilter(f);syncButton();};
      }
      window.SOVMapDrawingsOnly={get active(){return drawingsOnly},set active(v){drawingsOnly=!!v;syncButton();render();}};
    }catch(e){console.warn('[SOV Karta] drawings-only filter skipped',e)}
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install,{once:true});else install();
})();
