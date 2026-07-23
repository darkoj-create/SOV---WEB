(function(){
  'use strict';
  if(window.__SOV_TRIPS_HUMAN_UI)return;
  window.__SOV_TRIPS_HUMAN_UI=true;

  const $=id=>document.getElementById(id);
  let statusObserver=null;
  let listObserver=null;

  function ensureSyncUi(){
    const toolbar=document.querySelector('.controlTop > .toolbar');
    if(!toolbar||$('tripsSyncState'))return;
    const box=document.createElement('div');
    box.id='tripsSyncState';
    box.className='trips-sync-state';
    box.setAttribute('aria-live','polite');
    box.innerHTML='<span class="trips-sync-dot"></span><span id="tripsSyncText">Automatsko osvježavanje</span>';
    toolbar.insertBefore(box,$('refreshBtn')||null);
  }

  function setSync(mode,label){
    ensureSyncUi();
    const box=$('tripsSyncState');
    const text=$('tripsSyncText');
    if(!box||!text)return;
    box.classList.remove('is-syncing','is-offline','is-error');
    if(mode!=='ok')box.classList.add('is-'+mode);
    if(text.textContent!==label)text.textContent=label;
  }

  function syncFromOriginalStatus(){
    if(navigator.onLine===false){setSync('offline','Offline — spremljeni podaci');return;}
    const value=String($('status')?.textContent||'').trim().toLowerCase();
    if(!value||value.includes('učitavam')||value.includes('osvježavam')){
      setSync('syncing','Učitavam izlete');
      return;
    }
    if(value.includes('grešk')||value.includes('nije dostup')||value.includes('ne odgovara')){
      setSync('error','Prikazujem zadnje podatke');
      return;
    }
    setSync('ok','Automatski osvježeno');
  }

  function cards(){
    document.querySelectorAll('.tripCard').forEach(card=>{
      const open=card.querySelector('[data-open]');
      const mail=card.querySelector('[data-mail]');
      if(open&&open.textContent!=='Otvori')open.textContent='Otvori';
      if(mail&&mail.textContent!=='Najava')mail.textContent='Najava';
      if(card.dataset.humanReady)return;
      card.dataset.humanReady='1';
      card.tabIndex=0;
      card.setAttribute('role','button');
      card.addEventListener('click',event=>{
        if(!event.target.closest('button,a,input,select,textarea')&&open)open.click();
      });
      card.addEventListener('keydown',event=>{
        if((event.key==='Enter'||event.key===' ')&&open){event.preventDefault();open.click();}
      });
    });
  }

  function installDrawer(){
    if(!document.querySelector('.trips-drawer-backdrop')){
      const backdrop=document.createElement('div');
      backdrop.className='trips-drawer-backdrop';
      document.body.appendChild(backdrop);
      backdrop.addEventListener('click',()=>window.openForm&&window.openForm(false));
    }
    const base=typeof window.openForm==='function'?window.openForm:null;
    if(base&&!base.__sovHumanDrawer){
      const wrapped=function(open=true){
        document.body.classList.toggle('trip-form-open',!!open);
        return base(open);
      };
      wrapped.__sovHumanDrawer=true;
      window.openForm=wrapped;
    }
    document.addEventListener('keydown',event=>{
      if(event.key==='Escape'&&document.body.classList.contains('trip-form-open')&&window.openForm)window.openForm(false);
    });
  }

  function label(select,value,text){
    const option=Array.from(select?.options||[]).find(item=>item.value===value);
    if(option&&option.textContent!==text)option.textContent=text;
  }

  function installObservers(){
    const status=$('status');
    if(status&&!statusObserver){
      statusObserver=new MutationObserver(syncFromOriginalStatus);
      statusObserver.observe(status,{childList:true,subtree:true,characterData:true});
    }
    const list=$('listWrap');
    if(list&&!listObserver){
      listObserver=new MutationObserver(cards);
      listObserver.observe(list,{childList:true,subtree:true});
    }
    window.addEventListener('online',syncFromOriginalStatus);
    window.addEventListener('offline',syncFromOriginalStatus);
  }

  function install(){
    document.body.classList.add('sov-trips-human');
    if($('sideAddBtn')&&$('sideAddBtn').textContent!=='+ Novi izlet')$('sideAddBtn').textContent='+ Novi izlet';
    if($('search'))$('search').placeholder='Traži lokaciju, voditelja ili cilj';
    if($('refreshBtn')){
      if($('refreshBtn').textContent!=='Provjeri sad')$('refreshBtn').textContent='Provjeri sad';
      $('refreshBtn').title='Izleti se osvježavaju izvornim mehanizmom stranice.';
    }
    label($('formStatus'),'planned','Planirano');
    label($('formStatus'),'draft','Nacrt');
    label($('formStatus'),'active','Aktivno');
    label($('formStatus'),'done','Završeno');
    label($('formStatus'),'cancelled','Otkazano');
    ensureSyncUi();
    installDrawer();
    cards();
    installObservers();
    syncFromOriginalStatus();
  }

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install,{once:true});
  else install();
})();
