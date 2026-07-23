(function(){
  'use strict';
  if(window.__SOV_TRIPS_HUMAN_UI)return;
  window.__SOV_TRIPS_HUMAN_UI=true;
  const $=id=>document.getElementById(id);

  function cards(){
    document.querySelectorAll('.tripCard').forEach(card=>{
      const open=card.querySelector('[data-open]');
      const mail=card.querySelector('[data-mail]');
      if(open)open.textContent='Otvori';
      if(mail)mail.textContent='Najava';
      if(card.dataset.humanReady)return;
      card.dataset.humanReady='1';
      card.tabIndex=0;
      card.setAttribute('role','button');
      card.addEventListener('click',e=>{
        if(!e.target.closest('button,a,input,select,textarea')&&open)open.click();
      });
      card.addEventListener('keydown',e=>{
        if((e.key==='Enter'||e.key===' ')&&open){e.preventDefault();open.click();}
      });
    });
  }

  function installDrawer(){
    if(document.querySelector('.trips-drawer-backdrop'))return;
    const back=document.createElement('div');
    back.className='trips-drawer-backdrop';
    document.body.appendChild(back);
    const original=typeof window.openForm==='function'?window.openForm:null;
    if(original){
      window.openForm=function(open=true){
        document.body.classList.toggle('trip-form-open',!!open);
        return original(open);
      };
    }
    back.addEventListener('click',()=>window.openForm&&window.openForm(false));
    document.addEventListener('keydown',e=>{
      if(e.key==='Escape'&&document.body.classList.contains('trip-form-open'))window.openForm&&window.openForm(false);
    });
  }

  function label(select,value,text){
    const option=Array.from(select?.options||[]).find(x=>x.value===value);
    if(option)option.textContent=text;
  }

  function install(){
    document.body.classList.add('sov-trips-human');
    if($('sideAddBtn'))$('sideAddBtn').textContent='+ Novi izlet';
    if($('search'))$('search').placeholder='Traži lokaciju, voditelja ili cilj';
    if($('refreshBtn')){
      $('refreshBtn').textContent='Provjeri sad';
      $('refreshBtn').title='Ručno provjeri ima li novih podataka.';
    }
    label($('formStatus'),'planned','Planirano');
    label($('formStatus'),'draft','Nacrt');
    label($('formStatus'),'active','Aktivno');
    label($('formStatus'),'done','Završeno');
    label($('formStatus'),'cancelled','Otkazano');
    installDrawer();
    cards();
    const list=$('listWrap');
    if(list)new MutationObserver(cards).observe(list,{childList:true,subtree:true});
  }

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install,{once:true});
  else install();
})();
