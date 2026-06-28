(function(){
const PAGES={
 'arhivar-dashboard.html':'Predaje',
 'arhivar-predane-jame.html':'Predaje',
 'arhivar.html':'Arhiva',
 'arhivar-izvoz.html':'Izvoz',
 'arhivar-zahvati.html':'Zahvati',
 'topodroid.html':'Nacrti'
};
const TAB_TARGETS=[['Predaje','arhivar-dashboard.html#predaje'],['Arhiva','arhivar-dashboard.html#arhiva'],['Izvoz','arhivar-dashboard.html#izvoz']];
function page(){return (location.pathname.split('/').pop()||'').toLowerCase()||'arhivar-dashboard.html'}
function isArhivar(){return Object.prototype.hasOwnProperty.call(PAGES,page())}
function el(html){const t=document.createElement('template');t.innerHTML=html.trim();return t.content.firstElementChild}
function nav(){
 if(!isArhivar())return;
 const embedded=new URLSearchParams(location.search).has('embedded');
 if(embedded){document.body.classList.add('sov-arhivar-embedded');return;}
 const p=page();
 const n=el(`<nav class="sov-arhivar-nav" aria-label="Arhivar navigacija"><a class="brand" href="arhivar-dashboard.html"><span>SOV Arhivar</span><small>jedan radni ekran</small></a><a data-tab="Predaje" href="arhivar-dashboard.html#predaje">Predaje</a><a data-tab="Arhiva" href="arhivar-dashboard.html#arhiva">Arhiva</a><a data-tab="Izvoz" href="arhivar-dashboard.html#izvoz">Izvoz</a><a href="arhivar-zahvati.html">Zahvati</a><a href="topodroid.html">Nacrti</a><a href="karta.html">Karta objekata</a><span class="spacer"></span><span class="muted">osnovno odmah · napredno po potrebi</span><a href="dashboard.html">Dashboard</a><a data-logout href="login.html">Odjava</a></nav>`);
 const current=PAGES[p];
 n.querySelectorAll('a').forEach(a=>{ if(a.dataset.tab===current || (p==='arhivar-zahvati.html'&&a.textContent.trim()==='Zahvati') || (p==='topodroid.html'&&a.textContent.trim()==='Nacrti')) a.classList.add('active'); });
 const first=document.body.firstElementChild;
 if(first) document.body.insertBefore(n,first); else document.body.appendChild(n);
}
function cleanLegacyHeaders(){
 if(!isArhivar())return;
 const embedded=document.body.classList.contains('sov-arhivar-embedded');
 if(embedded)return;
 document.querySelectorAll('header.as-top,header.aw-top,header.az-top').forEach(h=>h.style.display='none');
 document.querySelectorAll('a[href="Karta.html"]').forEach(a=>a.setAttribute('href','karta.html'));
 document.querySelectorAll('a').forEach(a=>{const t=(a.textContent||'').trim(); if(['Speleo baza','SOV Karta','Baza'].includes(t)) a.textContent='Karta objekata';});
 document.querySelectorAll('.as-icon').forEach(x=>x.remove());
}
function collapseStats(){
 const stats=document.querySelector('#stats.aw-stats'); if(!stats||stats.dataset.sovReady)return; stats.dataset.sovReady='1';
 const btn=document.createElement('button');btn.type='button';btn.className='sov-more-stats';btn.textContent='Prikaži više statistika';
 const set=()=>{if(stats.children.length>4){stats.classList.add('sov-collapsed'); if(!btn.isConnected) stats.insertAdjacentElement('afterend',btn);}else btn.remove();};
 btn.addEventListener('click',()=>{stats.classList.toggle('sov-collapsed');btn.textContent=stats.classList.contains('sov-collapsed')?'Prikaži više statistika':'Sakrij dodatne statistike';});
 new MutationObserver(set).observe(stats,{childList:true}); set();
}
function exportRedirect(){
 const box=document.querySelector('.aw-export-box'); if(!box||box.dataset.sovDone)return; box.dataset.sovDone='1';box.classList.add('sov-export-redirect');
 box.innerHTML='<h3>Izvoz je premješten na jedno mjesto</h3><div class="sov-export-muted">Odaberi što izvoziš i format u zajedničkom modulu. Brzi export odabranog objekta ostaje dostupan u detalju.</div><a class="sov-export-link" href="arhivar-dashboard.html#izvoz">Otvori jedinstveni izvoz</a>';
}
function guardActions(){
 document.addEventListener('click',e=>{
   const btn=e.target.closest('#needsBtn,#rejectBtn,#approveBtn'); if(!btn)return;
   const title=document.querySelector('#detailPanel h2')?.textContent?.trim()||'odabrani zapis';
   if(btn.id==='needsBtn' && !confirm(`Sigurno označiti što fali za "${title}"?`)){e.preventDefault();e.stopImmediatePropagation();}
   if(btn.id==='rejectBtn' && !confirm(`Sigurno odbiti "${title}"?`)){e.preventDefault();e.stopImmediatePropagation();}
 },true);
}
function toast(msg){let t=document.querySelector('.sov-toast'); if(!t){t=document.createElement('div');t.className='sov-toast';document.body.appendChild(t);} t.textContent=msg; clearTimeout(t._sovT); t._sovT=setTimeout(()=>t.remove(),3600)}
function observeDetailSelection(){
 const listSel=['#objectList','#submissionList'];
 listSel.forEach(sel=>{const box=document.querySelector(sel); if(!box)return; new MutationObserver(()=>{const active=box.querySelector('.active'); const visible=box.querySelector('[data-id]'); const detail=document.querySelector('#detailPanel'); if(!active&&visible){visible.click();} else if(!visible&&detail){detail.innerHTML='<div class="as-empty aw-empty">Odaberi objekt.</div>';}}).observe(box,{childList:true,subtree:false});});
}
function boot(){nav();cleanLegacyHeaders();collapseStats();exportRedirect();guardActions();observeDetailSelection();}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot); else boot();
window.SOVSimpleToast=toast;
})();
