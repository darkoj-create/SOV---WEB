(function(){
  // v4.77 TEMP OPEN MODE: full preview without login. Remove this build when done testing.
  const SOV_OPEN_PREVIEW_MODE = true;
  const OPEN_PREVIEW_PROFILE_BASE = {id:null,email:'preview@sov.local',full_name:'Preview korisnik',role:'admin',status:'approved',open_preview:true};
  const PREVIEW_ROLES = ['user','oruzar','arhivar','editor','admin'];
  function getPreviewRole(){
    try{
      const r = localStorage.getItem('SOV_PREVIEW_ROLE') || 'admin';
      return PREVIEW_ROLES.includes(r) ? r : 'admin';
    }catch(e){ return 'admin'; }
  }
  function getOpenPreviewProfile(){
    const role = getPreviewRole();
    return {...OPEN_PREVIEW_PROFILE_BASE, role, full_name:'Preview '+(ROLE_LABELS[role]||role)};
  }
  const REGISTERED_PAGES = new Set([
    'dashboard.html','baza.html','pregled-baze.html','izleti.html','kalendar-izleta.html',
    'dokumentacija.html','pregled-zapisnika.html','zapisnici-skupstine.html','novi-zapisnik.html',
    'speleo-zapisnik.html','topodroid.html','napisi-clanak.html','arhivar-zahvati.html','speleo-sql-safe.html','oruzarstvo.html','oruzarstvo-import.html','admin-users.html'
  ]);
  const ROLE_LABELS = {admin:'Admin',editor:'Urednik',oruzar:'Oružar',arhivar:'Arhivar',user:'Član'};
  const ADMIN_ROLES = ['admin'];
  const EDITOR_ROLES = ['admin','editor'];
  const ARCHIVE_ROLES = ['admin','arhivar'];
  const ARMORY_ROLES = ['admin','oruzar'];
  const BOOTSTRAP_ADMIN_EMAILS = ['darko.jeras@gmail.com'];
  function isBootstrapAdminEmail(email){ return BOOTSTRAP_ADMIN_EMAILS.includes(String(email||'').trim().toLowerCase()); }
  function bootstrapAdminProfile(user){
    return {id:user.id,email:user.email,full_name:'Darko Jeras',role:'admin',status:'approved',bootstrap_admin:true};
  }
  let client = null;
  let profileCache = null;
  let readyPromise = null;

  function pageName(){ return (location.pathname.split('/').pop() || 'index.html').toLowerCase(); }
  function isConfigured(){
    return !!(window.SOV_SUPABASE_URL && window.SOV_SUPABASE_ANON_KEY &&
      !String(window.SOV_SUPABASE_URL).includes('PASTE_') && !String(window.SOV_SUPABASE_ANON_KEY).includes('PASTE_'));
  }
  function getClient(){
    if(client) return client;
    if(!isConfigured()) return null;
    if(!window.supabase || !window.supabase.createClient){ console.error('Supabase JS nije učitan.'); return null; }
    client = window.supabase.createClient(window.SOV_SUPABASE_URL, window.SOV_SUPABASE_ANON_KEY, {
      auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:true}
    });
    return client;
  }
  function statusText(status){
    if(status === 'approved') return 'Odobren';
    if(status === 'pending') return 'Čeka odobrenje';
    if(status === 'rejected') return 'Odbijen';
    return status || 'Nepoznato';
  }
  function roleText(role){ return ROLE_LABELS[role] || role || 'Član'; }
  function escapeHtml(str){ return String(str||'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c])); }

  async function getSession(){
    if(SOV_OPEN_PREVIEW_MODE) return {session:null,user:getOpenPreviewProfile()};
    const sb = getClient();
    if(!sb) return {session:null,user:null};
    const {data} = await sb.auth.getSession();
    return {session:data.session,user:data.session && data.session.user};
  }
  async function getProfile(force=false){
    if(SOV_OPEN_PREVIEW_MODE){ profileCache = getOpenPreviewProfile(); return profileCache; }
    if(profileCache && !force) return profileCache;
    const sb = getClient();
    if(!sb) return null;
    const {user} = await getSession();
    if(!user) return null;
    const {data,error} = await sb.from('profiles').select('*').eq('id',user.id).maybeSingle();
    if(error){ console.error(error); }
    if(data && data.status === 'approved'){
      profileCache = {...data,email:user.email};
      return profileCache;
    }
    if(isBootstrapAdminEmail(user.email)){
      profileCache = data ? {...data,email:user.email,role:'admin',status:'approved',bootstrap_admin:true} : bootstrapAdminProfile(user);
      try{
        await sb.from('profiles').upsert({
          id:user.id, email:user.email, full_name:profileCache.full_name || 'Darko Jeras',
          role:'admin', status:'approved', approved_at:new Date().toISOString(), approved_by:user.id
        },{onConflict:'id'});
      }catch(e){ console.warn('Bootstrap admin profile sync skipped.', e); }
      return profileCache;
    }
    profileCache = data ? {...data,email:user.email} : {id:user.id,email:user.email,full_name:user.email,role:'user',status:'pending'};
    return profileCache;
  }
  async function currentUser(){ return await getProfile(); }

  async function register(payload){
    const sb = getClient();
    if(!sb) return {ok:false,msg:'Supabase još nije konfiguriran. Upisi URL i anon key u assets/supabase-config.js.'};
    const email = (payload.email||'').trim().toLowerCase();
    const password = payload.password || '';
    const full_name = (payload.name||'').trim();
    if(!email || !password || !full_name) return {ok:false,msg:'Unesi ime, email i lozinku.'};
    const {data,error} = await sb.auth.signUp({
      email,password,
      options:{data:{full_name, requested_role:'user'}}
    });
    if(error) return {ok:false,msg:error.message};
    if(data.user){
      await sb.from('profiles').upsert({
        id:data.user.id,
        email,
        full_name,
        role:'user',
        status:'pending',
        note:payload.note || '',
        created_at:new Date().toISOString()
      },{onConflict:'id'});
    }
    return {ok:true,msg:'Zahtjev je poslan. Račun čeka admin odobrenje.'};
  }

  async function login(email,password){
    const sb = getClient();
    if(!sb) return {ok:false,msg:'Supabase nije konfiguriran. Upisi URL i anon key u assets/supabase-config.js.'};
    const {data,error} = await sb.auth.signInWithPassword({email:(email||'').trim().toLowerCase(),password});
    if(error) return {ok:false,msg:'Krivi email/lozinka ili račun nije potvrđen.'};
    profileCache = null;
    const profile = await getProfile(true);
    if(!profile || profile.status !== 'approved'){
      await sb.auth.signOut();
      profileCache = null;
      return {ok:false,msg:'Račun još nije odobren. Pričekaj admin approval.'};
    }
    return {ok:true,user:profile};
  }
  async function logout(){ const sb=getClient(); if(sb) await sb.auth.signOut(); profileCache=null; location.href='index.html'; }

  async function can(ability){
    const u = await getProfile();
    if(SOV_OPEN_PREVIEW_MODE){
      if(!u) return false;
      if(ability === 'admin') return ADMIN_ROLES.includes(u.role);
      if(ability === 'editor') return EDITOR_ROLES.includes(u.role);
      if(ability === 'armory') return ARMORY_ROLES.includes(u.role);
      if(ability === 'speleo_edit' || ability === 'archive') return ARCHIVE_ROLES.includes(u.role);
      return true;
    }
    if(!u || u.status !== 'approved') return false;
    if(ability === 'admin') return ADMIN_ROLES.includes(u.role);
    if(ability === 'editor') return EDITOR_ROLES.includes(u.role);
    if(ability === 'armory') return ARMORY_ROLES.includes(u.role);
    if(ability === 'speleo_edit' || ability === 'archive') return ARCHIVE_ROLES.includes(u.role);
    return true;
  }
  async function requireApproved(){
    if(SOV_OPEN_PREVIEW_MODE){ await renderUserBadge(); return true; }
    if(!isConfigured()){ showAuthWarning('Supabase nije konfiguriran. Upisi ključeve u assets/supabase-config.js.'); return false; }
    const {user} = await getSession();
    const profile = await getProfile();
    if(!user || !profile || profile.status !== 'approved'){
      const next = encodeURIComponent(pageName() || 'dashboard.html');
      location.href = 'login.html?next=' + next;
      return false;
    }
    return true;
  }
  async function requireAdmin(){ if(SOV_OPEN_PREVIEW_MODE){ await renderUserBadge(); return true; } const ok = await requireApproved(); if(!ok) return false; if(!(await can('admin'))){ location.href='dashboard.html?denied=admin'; return false; } return true; }
  async function requireEditor(){ if(SOV_OPEN_PREVIEW_MODE){ await renderUserBadge(); return true; } const ok = await requireApproved(); if(!ok) return false; if(!(await can('editor'))){ location.href='dashboard.html?denied=editor'; return false; } return true; }
  async function requireArmory(){ if(SOV_OPEN_PREVIEW_MODE){ await renderUserBadge(); return true; } const ok = await requireApproved(); if(!ok) return false; if(!(await can('armory'))){ location.href='dashboard.html?denied=armory'; return false; } return true; }
  async function requireArchive(){ if(SOV_OPEN_PREVIEW_MODE){ await renderUserBadge(); return true; } const ok = await requireApproved(); if(!ok) return false; if(!(await can('speleo_edit'))){ location.href='dashboard.html?denied=archive'; return false; } return true; }

  async function updateProfile(id, patch){
    const sb = getClient();
    const {error} = await sb.from('profiles').update(patch).eq('id',id);
    if(error) throw error;
    return true;
  }
  async function loadUsers(){
    const sb = getClient(); if(!sb) return [];
    const {data,error} = await sb.from('profiles').select('id,email,full_name,role,status,note,created_at,approved_at').order('created_at',{ascending:false});
    if(error){ console.error(error); return []; }
    return data || [];
  }
  async function approve(id){ return updateProfile(id,{status:'approved',approved_at:new Date().toISOString()}); }
  async function reject(id){ return updateProfile(id,{status:'rejected'}); }
  async function setRole(id, role){ return updateProfile(id,{role}); }

  function renderPreviewRoleSwitcher(u){
    if(!SOV_OPEN_PREVIEW_MODE) return;
    let box = document.getElementById('sov-preview-role-switcher');
    const roles = [
      ['user','👤','User'],
      ['oruzar','🛠️','Oružar'],
      ['arhivar','🗂️','Arhivar'],
      ['editor','✏️','Urednik'],
      ['admin','🛡️','Admin']
    ];
    if(!document.getElementById('sov-preview-role-style')){
      const st=document.createElement('style');
      st.id='sov-preview-role-style';
      st.textContent=`
        #sov-preview-role-switcher{position:fixed;top:12px;right:12px;z-index:999999;display:flex;gap:6px;align-items:center;flex-wrap:wrap;padding:8px;border:1px solid rgba(255,255,255,.14);border-radius:999px;background:rgba(4,10,11,.86);backdrop-filter:blur(14px);box-shadow:0 16px 50px rgba(0,0,0,.38);font-family:Inter,system-ui,sans-serif}
        #sov-preview-role-switcher .label{color:#b8c9c3;font-size:11px;font-weight:900;padding:0 5px;letter-spacing:.05em;text-transform:uppercase}
        #sov-preview-role-switcher button{border:1px solid rgba(255,255,255,.12);background:rgba(255,255,255,.06);color:#eef8f2;border-radius:999px;padding:7px 10px;font-weight:950;font-size:12px;cursor:pointer;display:inline-flex;align-items:center;gap:5px}
        #sov-preview-role-switcher button.active{background:linear-gradient(135deg,#d7f66f,#83e6c2);color:#111;border-color:transparent;box-shadow:0 8px 24px rgba(215,246,111,.18)}
        @media(max-width:720px){#sov-preview-role-switcher{left:8px;right:8px;top:auto;bottom:8px;justify-content:center;border-radius:20px}#sov-preview-role-switcher .label{display:none}#sov-preview-role-switcher button{padding:8px 9px;font-size:11px}}
      `;
      document.head.appendChild(st);
    }
    if(!box){
      box=document.createElement('div');
      box.id='sov-preview-role-switcher';
      document.body.appendChild(box);
    }
    const current = (u && u.role) || getPreviewRole();
    box.innerHTML='<span class="label">Preview view</span>'+roles.map(([r,ico,label])=>`<button type="button" data-preview-role="${r}" class="${r===current?'active':''}">${ico}<span>${label}</span></button>`).join('');
    box.querySelectorAll('button[data-preview-role]').forEach(btn=>{
      btn.onclick=()=>{
        try{localStorage.setItem('SOV_PREVIEW_ROLE', btn.dataset.previewRole)}catch(e){}
        location.reload();
      };
    });
  }

  async function renderUserBadge(){
    const u = await getProfile();
    renderPreviewRoleSwitcher(u);
    document.querySelectorAll('[data-user-name]').forEach(el=>{el.textContent = u ? (u.full_name || u.email) : 'Gost';});
    document.querySelectorAll('[data-user-role]').forEach(el=>{el.textContent = u ? roleText(u.role) : 'Gost';});
    document.querySelectorAll('[data-auth-status]').forEach(el=>{el.textContent = SOV_OPEN_PREVIEW_MODE ? 'Otvoreni preview' : (u ? statusText(u.status) : 'Nije prijavljen');});
    document.querySelectorAll('[data-logout]').forEach(el=>{el.addEventListener('click',e=>{e.preventDefault();logout();});});
    document.querySelectorAll('[data-role-admin]').forEach(el=>{el.style.display = (u && u.role === 'admin') ? '' : 'none';});
    document.querySelectorAll('[data-role-editor]').forEach(el=>{el.style.display = (u && EDITOR_ROLES.includes(u.role)) ? '' : 'none';});
    document.querySelectorAll('[data-role-armory]').forEach(el=>{el.style.display = (u && ARMORY_ROLES.includes(u.role)) ? '' : 'none';});
    document.querySelectorAll('[data-role-archive]').forEach(el=>{el.style.display = (u && ARCHIVE_ROLES.includes(u.role)) ? '' : 'none';});
    document.body.classList.remove('role-admin','role-editor','role-oruzar','role-arhivar','role-user');
    document.body.classList.toggle('role-admin', !!(u && u.role==='admin'));
    document.body.classList.toggle('role-editor', !!(u && u.role==='editor'));
    document.body.classList.toggle('role-oruzar', !!(u && u.role==='oruzar'));
    document.body.classList.toggle('role-arhivar', !!(u && u.role==='arhivar'));
    document.body.classList.toggle('role-user', !!(u && u.role==='user'));
  }
  function showAuthWarning(msg){
    if(document.getElementById('sov-auth-warning')) return;
    const d=document.createElement('div');
    d.id='sov-auth-warning';
    d.style.cssText='position:fixed;left:16px;right:16px;bottom:16px;z-index:999999;background:#12170f;color:#e9ffd6;border:1px solid rgba(215,246,111,.4);border-radius:18px;padding:14px 16px;font:800 13px Inter,system-ui,sans-serif;box-shadow:0 18px 50px rgba(0,0,0,.45)';
    d.innerHTML=escapeHtml(msg);
    document.body.appendChild(d);
  }

  async function autoProtect(){
    const p = pageName();
    if(!REGISTERED_PAGES.has(p)) return;
    if(p === 'admin-users.html') await requireAdmin();
    else if(p === 'oruzarstvo-import.html') await requireArmory();
    else await requireApproved();
    await renderUserBadge();
  }
  readyPromise = new Promise(resolve=>{
    document.addEventListener('DOMContentLoaded', async()=>{ await autoProtect(); resolve(true); });
  });

  window.SOVAuth = {isConfigured,getClient,getSession,getProfile,currentUser,register,login,logout,can,requireApproved,requireAdmin,requireEditor,requireArmory,requireArchive,loadUsers,approve,reject,setRole,renderUserBadge,statusText,roleText,ready:()=>readyPromise};
})();
