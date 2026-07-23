(function(){
  'use strict';
  const $=id=>document.getElementById(id);
  const LAST_SYNC='sov_trips_last_sync_at_v6146';
  let lastSync=0;
  try{lastSync=Number(localStorage.getItem(LAST_SYNC)||0)||0;}catch(e){}
  let loadInFlight=null,loadQueued=false,autoTimer=null;

  function ensureSyncUi(){
    const toolbar=document.querySelector('.controlTop > .toolbar');
    if(!toolbar||$('tripsSyncState'))return;
    const box=document.createElement('div');
    box.id='tripsSyncState';box.className='trips-sync-state';box.setAttribute('aria-live','polite');
    box.innerHTML='<span class="trips-sync-dot"></span><span id="tripsSyncText">Automatsko osvježavanje</span><span class="trips-sync-age" id="tripsSyncAge"></span>';
    toolbar.insertBefore(box,$('refreshBtn')||null);
  }
  function age(){if(!lastSync)return'';const s=Math.floor((Date.now()-lastSync)/1000);if(s<10)return'upravo sada';if(s<60)return`prije ${s} s`;const m=Math.floor(s/60);return m<60?`prije ${m} min`:new Date(lastSync).toLocaleTimeString('hr-HR',{hour:'2-digit',minute:'2-digit'});}
  function syncUi(mode,label){
    ensureSyncUi();const box=$('tripsSyncState'),text=$('tripsSyncText'),ago=$('tripsSyncAge');if(!box)return;
    box.classList.remove('is-syncing','is-offline','is-error');if(mode!=='ok')box.classList.add('is-'+mode);
    text.textContent=label||({ok:'Automatski osvježeno',syncing:'Osvježavam',offline:'Offline — spremljeni podaci',error:'Prikazujem zadnje podatke'}[mode]);
    ago.textContent=mode==='ok'&&lastSync?'· '+age():'';
  }
  function markOk(){lastSync=Date.now();try{localStorage.setItem(LAST_SYNC,String(lastSync))}catch(e){}syncUi('ok');}

  function installLoadQueue(){
    if(typeof window.loadTrips!=='function'||window.loadTrips.__sovHumanQueue)return;
    const original=window.loadTrips;
    const wrapped=function(opts={}){
      if(loadInFlight){if(opts.force)loadQueued=true;return loadInFlight;}
      if(navigator.onLine===false){syncUi('offline');return Promise.resolve();}
      syncUi('syncing');
      loadInFlight=Promise.resolve(original(opts)).then(result=>{
        const status=String($('status')?.textContent||'').toLowerCase();
        if(status.includes('nisu dostupni')||status.includes('ne odgovara'))syncUi('error');else markOk();
        return result;
      }).catch(err=>{console.warn('[SOV trips] load queue',err);syncUi('error');}).finally(()=>{
        loadInFlight=null;if(loadQueued){loadQueued=false;setTimeout(()=>wrapped({force:true,silent:true}),120);}
      });
      return loadInFlight;
    };
    wrapped.__sovHumanQueue=true;window.loadTrips=wrapped;
  }

  async function refresh(source='auto'){
    const manual=source!=='auto';
    if(navigator.onLine===false){syncUi('offline');if(manual&&typeof window.toast==='function')window.toast('Nema interneta. Prikazujem spremljene izlete.');return;}
    installLoadQueue();const btn=$('refreshBtn');
    if(manual&&btn){btn.disabled=true;btn.setAttribute('aria-busy','true');}
    try{await window.loadTrips({force:true,silent:!manual});if(manual&&typeof window.toast==='function')window.toast('Izleti su provjereni.');}
    catch(e){syncUi('error');if(manual&&typeof window.toast==='function')window.toast('Osvježavanje nije uspjelo.');}
    finally{if(manual&&btn){btn.disabled=false;btn.removeAttribute('aria-busy');}}
  }

  function cards(){
    document.querySelectorAll('.tripCard').forEach(card=>{
      const open=card.querySelector('[data-open]'),mail=card.querySelector('[data-mail]');
      if(open)open.textContent='Otvori';if(mail)mail.textContent='Najava';
      if(card.dataset.humanReady)return;card.dataset.humanReady='1';card.tabIndex=0;card.setAttribute('role','button');
      card.addEventListener('click',e=>{if(!e.target.closest('button,a,input,select,textarea')&&open)open.click();});
      card.addEventListener('keydown',e=>{if((e.key==='Enter'||e.key===' ')&&open){e.preventDefault();open.click();}});
    });
  }
  function installDrawer(){
    if(document.querySelector('.trips-drawer-backdrop'))return;
    const back=document.createElement('div');back.className='trips-drawer-backdrop';document.body.appendChild(back);
    const base=typeof window.openForm==='function'?window.openForm:null;
    if(base){window.openForm=function(open=true){document.body.classList.toggle('trip-form-open',!!open);base(open);};}
    back.onclick=()=>window.openForm&&window.openForm(false);
    document.addEventListener('keydown',e=>{if(e.key==='Escape'&&document.body.classList.contains('trip-form-open'))window.openForm&&window.openForm(false);});
  }
  function label(select,value,text){const o=Array.from(select?.options||[]).find(x=>x.value===value);if(o)o.textContent=text;}
  function friendlyUi(){
    document.body.classList.add('sov-trips-human');
    const hero=document.querySelector('.heroText');if(hero&&!hero.querySelector('.trips-hero-subtitle')){const p=document.createElement('p');p.className='trips-hero-subtitle';p.textContent='Nadolazeći izleti, prijave, prijevoz i priprema za teren na jednom mjestu.';hero.appendChild(p);}
    if($('sideAddBtn'))$('sideAddBtn').textContent='+ Novi izlet';
    if($('search'))$('search').placeholder='Traži lokaciju, voditelja ili cilj';
    if($('refreshBtn')){$('refreshBtn').textContent='Provjeri sad';$('refreshBtn').title='Izleti se inače osvježavaju automatski.';}
    label($('formStatus'),'planned','Planirano');label($('formStatus'),'draft','Nacrt');label($('formStatus'),'active','Aktivno');label($('formStatus'),'done','Završeno');label($('formStatus'),'cancelled','Otkazano');
    ensureSyncUi();
    const statusText=String($('status')?.textContent||'').toLowerCase();
    if(navigator.onLine===false)syncUi('offline');
    else if(lastSync)syncUi('ok');
    else if(statusText&&!statusText.includes('učitavam')&&!statusText.includes('nisu dostupni'))markOk();
    else syncUi('syncing','Učitavam izlete');
    installDrawer();cards();const list=$('listWrap');if(list)new MutationObserver(cards).observe(list,{childList:true,subtree:true});
  }
  function autoSync(){
    if(autoTimer)return;
    autoTimer=setInterval(()=>{if(lastSync&&navigator.onLine!==false&&!document.querySelector('.trips-sync-state.is-syncing'))syncUi('ok');},15000);
    window.addEventListener('online',()=>syncUi('syncing','Veza vraćena — osvježavam'));
    window.addEventListener('offline',()=>syncUi('offline'));
  }
  function install(){friendlyUi();installLoadQueue();if($('refreshBtn'))$('refreshBtn').onclick=()=>refresh('button');autoSync();}
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install);else install();
})();
