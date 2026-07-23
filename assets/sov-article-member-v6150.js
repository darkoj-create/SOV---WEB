// SOV member article draft protection v6.1.50.
(function(){
  'use strict';
  if(window.__SOV_ARTICLE_MEMBER_V6150)return;
  window.__SOV_ARTICLE_MEMBER_V6150=true;
  const KEY='sov_member_article_draft_v6150';
  const ids=['title','author','category','summary','imageUrl','body'];
  const $=id=>document.getElementById(id);
  let timer=null;

  function read(){try{return JSON.parse(localStorage.getItem(KEY)||'null')}catch(e){return null}}
  function snapshot(){const out={savedAt:Date.now()};ids.forEach(id=>{const el=$(id);if(el)out[id]=el.value||''});return out}
  function useful(d){return !!(d&&(d.title||d.summary||d.imageUrl||d.body))}
  function state(text){let el=$('articleDraftState');if(!el){el=document.createElement('span');el.id='articleDraftState';el.className='status';const status=$('status');if(status&&status.parentNode)status.parentNode.insertBefore(el,status)}el.textContent=text||''}
  function save(){const d=snapshot();if(useful(d)){try{localStorage.setItem(KEY,JSON.stringify(d));state('Skica spremljena.')}catch(e){}}else{try{localStorage.removeItem(KEY)}catch(e){}state('')}}
  function queue(){clearTimeout(timer);timer=setTimeout(save,350)}
  function restore(){const d=read();if(!useful(d))return;ids.forEach(id=>{const el=$(id);if(el&&!el.value&&d[id]!=null)el.value=d[id]});const image=$('imageUrl');if(image)image.dispatchEvent(new Event('input',{bubbles:true}));const when=d.savedAt?new Date(d.savedAt).toLocaleString('hr-HR',{day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'}):'';state('Vraćena spremljena skica'+(when?' · '+when:''));}
  function clear(){try{localStorage.removeItem(KEY)}catch(e){}state('')}
  function install(){
    if(!$('articleForm'))return;
    restore();
    ids.forEach(id=>{const el=$(id);if(el){el.addEventListener('input',queue);el.addEventListener('change',queue)}});
    const clearBtn=$('clearBtn');if(clearBtn)clearBtn.addEventListener('click',clear);
    const cover=$('coverFile');if(cover)cover.addEventListener('change',()=>{if(cover.files&&cover.files[0])$('uploadBtn')?.click()});
    const status=$('status');if(status)new MutationObserver(()=>{if(String(status.textContent||'').includes('Predano uredniku'))clear()}).observe(status,{childList:true,characterData:true,subtree:true});
    window.addEventListener('pagehide',save);
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(install,80),{once:true});else setTimeout(install,80);
})();
