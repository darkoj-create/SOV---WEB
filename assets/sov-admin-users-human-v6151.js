(function(){
  'use strict';
  const esc=s=>String(s||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const niceDate=s=>{if(!s)return '';const d=new Date(s);return isNaN(d)?'':d.toLocaleDateString('hr-HR',{day:'2-digit',month:'2-digit',year:'numeric'});};
  let users=[];
  let current=null;
  let busy=false;
  let timer=null;

  function roleOptions(canWebmaster){
    return '<option value="user">Član</option><option value="editor">Urednik</option><option value="oruzar">Oružar</option><option value="arhivar">Arhivar</option><option value="admin">Upravljanje</option>'+(canWebmaster?'<option value="webmaster">Sustav</option>':'');
  }
  function row(u,kind){
    const canWebmaster=!!(current&&current.role==='webmaster');
    const date=kind==='approved'?(u.approved_at||u.created_at):u.created_at;
    const mainActions=kind==='pending'
      ? '<button class="btn ok" data-approve="'+esc(u.id)+'">Aktiviraj</button><button class="btn bad" data-reject="'+esc(u.id)+'">Odbij</button>'
      : kind==='rejected' ? '<button class="btn ok" data-approve="'+esc(u.id)+'">Aktiviraj</button>' : '';
    return '<div class="user" data-user-id="'+esc(u.id)+'">'
      +'<div><b>'+esc(u.full_name||u.email)+'</b><span class="pill">'+esc(SOVAuth.roleText(u.role))+'</span>'
      +'<p class="muted">'+esc(u.email)+(u.note?'<br>'+esc(u.note):'')+(date?'<br>'+(kind==='approved'?'Aktiviran: ':'Zahtjev: ')+esc(niceDate(date)):'')+'</p></div>'
      +'<div class="actions"><select class="roleSelect" data-role="'+esc(u.id)+'">'+roleOptions(canWebmaster)+'</select>'+mainActions+'</div></div>';
  }
  function section(title,arr,kind,open){
    const list=arr.length?arr.map(u=>row(u,kind)).join(''):'<div class="sov-users-empty">'+(kind==='pending'?'Nema novih zahtjeva za aktivaciju.':'Nema korisnika u ovoj listi.')+'</div>';
    if(kind==='pending') return '<section class="sov-user-section"><div class="sov-user-section-head"><h2>'+title+'</h2><span class="sov-user-count">'+arr.length+'</span></div><div class="sov-user-list">'+list+'</div></section>';
    const search=kind==='approved'&&arr.length?'<div class="sov-user-search"><input id="activatedUserSearch" placeholder="Traži aktiviranog korisnika…" autocomplete="off"></div>':'';
    return '<details class="sov-user-section" '+(open?'open':'')+'><summary class="sov-user-summary"><h2>'+title+'</h2><span class="sov-user-count">'+arr.length+'</span></summary>'+search+'<div class="sov-user-list" data-list="'+kind+'">'+list+'</div></details>';
  }
  function bind(){
    document.querySelectorAll('[data-role]').forEach(s=>{
      const u=users.find(x=>String(x.id)===String(s.dataset.role));
      if(u)s.value=u.role||'user';
      s.onchange=async()=>{try{await SOVAuth.setRole(s.dataset.role,s.value);await refresh(true);}catch(e){console.error(e);}};
    });
    document.querySelectorAll('[data-approve]').forEach(b=>b.onclick=async()=>{
      if(busy)return;busy=true;b.disabled=true;b.textContent='Aktiviram…';
      try{await SOVAuth.approve(b.dataset.approve);await refresh(true);}catch(e){console.error(e);b.disabled=false;b.textContent='Aktiviraj';}finally{busy=false;}
    });
    document.querySelectorAll('[data-reject]').forEach(b=>b.onclick=async()=>{
      if(busy)return;busy=true;b.disabled=true;
      try{await SOVAuth.reject(b.dataset.reject);await refresh(true);}catch(e){console.error(e);b.disabled=false;}finally{busy=false;}
    });
    const search=document.getElementById('activatedUserSearch');
    if(search){search.oninput=()=>{const q=search.value.trim().toLowerCase();document.querySelectorAll('[data-list="approved"] .user').forEach(el=>{const u=users.find(x=>String(x.id)===String(el.dataset.userId));const hay=[u&&u.full_name,u&&u.email,u&&u.role].join(' ').toLowerCase();el.style.display=!q||hay.includes(q)?'grid':'none';});};}
  }
  function paint(){
    const box=document.getElementById('users');if(!box)return;
    const pending=users.filter(u=>u.status==='pending').sort((a,b)=>String(b.created_at||'').localeCompare(String(a.created_at||'')));
    const approved=users.filter(u=>u.status==='approved').sort((a,b)=>String(b.approved_at||b.created_at||'').localeCompare(String(a.approved_at||a.created_at||'')));
    const rejected=users.filter(u=>u.status==='rejected').sort((a,b)=>String(b.created_at||'').localeCompare(String(a.created_at||'')));
    box.innerHTML=section('Za aktivaciju',pending,'pending',true)+section('Aktivirani korisnici',approved,'approved',false)+(rejected.length?section('Odbijeni zahtjevi',rejected,'rejected',false):'');
    bind();
  }
  async function refresh(force){
    try{
      if(!window.SOVAuth)return;
      if(SOVAuth.ready)await SOVAuth.ready();
      current=await SOVAuth.currentUser();
      users=await SOVAuth.loadUsers();
      paint();
      if(force&&window.SOVInbox&&SOVInbox.refresh)SOVInbox.refresh();
    }catch(e){console.error('[SOV users]',e);const box=document.getElementById('users');if(box)box.innerHTML='<div class="sov-users-empty">Korisnike trenutno nije moguće učitati.</div>';}
  }
  function start(){
    const h=document.querySelector('.admin>h1');if(h)h.textContent='Korisnici';
    document.title='Korisnici — SOV Velebit';
    window.render=()=>refresh(true);
    refresh(false);
    timer=setInterval(()=>{if(document.visibilityState==='visible'&&!busy)refresh(false);},20000);
    document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible')refresh(false);});
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',start);else start();
})();
