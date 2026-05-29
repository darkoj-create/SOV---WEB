(function(){
  // v5.58.12: Webmaster super-role + operational Admin split. Preview role switcher is opt-in with ?preview=1 or localStorage SOV_OPEN_PREVIEW_MODE=true.
  const SOV_OPEN_PREVIEW_MODE = (()=>{
    try{
      const q = new URLSearchParams(location.search);
      if(q.get('preview') === '1'){ localStorage.setItem('SOV_OPEN_PREVIEW_MODE','true'); return true; }
      if(q.get('preview') === '0'){ localStorage.removeItem('SOV_OPEN_PREVIEW_MODE'); return false; }
      return localStorage.getItem('SOV_OPEN_PREVIEW_MODE') === 'true';
    }catch(e){ return false; }
  })();
  const OPEN_PREVIEW_PROFILE_BASE = {id:null,email:'preview@sov.local',full_name:'Preview korisnik',role:'webmaster',status:'approved',open_preview:true};
  const PREVIEW_ROLES = ['user','oruzar','arhivar','editor','admin','webmaster'];
  function getPreviewRole(){
    try{
      const r = localStorage.getItem('SOV_PREVIEW_ROLE') || 'admin';
      return PREVIEW_ROLES.includes(r) ? r : 'webmaster';
    }catch(e){ return 'admin'; }
  }
  function getOpenPreviewProfile(){
    const role = getPreviewRole();
    return {...OPEN_PREVIEW_PROFILE_BASE, role, full_name:'Preview '+(ROLE_LABELS[role]||role)};
  }
  const REGISTERED_PAGES = new Set([
    'dashboard.html','karta.html','pregled-baze.html','izleti.html','izleti-cloud.html','kalendar-izleta.html',
    'dokumentacija.html','pregled-zapisnika.html','zapisnici-skupstine.html','novi-zapisnik.html',
    'speleo-zapisnik.html','topodroid.html','napisi-clanak.html','arhivar-dashboard.html','arhivar.html','arhivar-zahvati.html','arhivar-predane-jame.html','arhivar-izvoz.html','speleo-sql-safe.html','speleo-sql-edit-sandbox.html','speleo-sql-compare.html','speleo-sql-object-hub.html','speleo-sql-promote.html','speleo-sql-go-live.html','oruzarstvo.html','oruzarstvo-import.html','oruzar-master-notes.html','oruzar-master-inventura.html','oruzar-master-inventar.html','oruzar-master-posudbe.html','oruzar-master.html','admin-users.html','admin-notifications.html','role-manager.html','news-editor.html','sync-status.html'
  ]);
  const ROLE_LABELS = {webmaster:'Webmaster',admin:'Admin',editor:'Urednik',oruzar:'Oružar',arhivar:'Arhivar',user:'Član'};
  const ADMIN_ROLES = ['webmaster','admin'];
  const WEBMASTER_ROLES = ['webmaster'];
  const EDITOR_ROLES = ['webmaster','admin','editor'];
  const ARCHIVE_ROLES = ['webmaster','admin','arhivar'];
  const ARMORY_ROLES = ['webmaster','admin','oruzar'];
  const PERMISSION_FALLBACK = {
    user:{label:'Član',can_view_sov_base:true,can_view_katastar:false,can_edit_objects:false,can_upload_drawings:true,can_verify_drawings:false,can_manage_trips:false,can_manage_equipment:false,can_edit_news:false,can_use_sql_tools:false,can_manage_users:false,can_send_notifications:false},
    editor:{label:'Urednik',can_view_sov_base:true,can_view_katastar:false,can_edit_objects:true,can_upload_drawings:true,can_verify_drawings:false,can_manage_trips:true,can_manage_equipment:false,can_edit_news:true,can_use_sql_tools:false,can_manage_users:false,can_send_notifications:false},
    arhivar:{label:'Arhivar',can_view_sov_base:true,can_view_katastar:false,can_edit_objects:true,can_upload_drawings:true,can_verify_drawings:true,can_manage_trips:true,can_manage_equipment:false,can_edit_news:false,can_use_sql_tools:false,can_manage_users:false,can_send_notifications:false},
    oruzar:{label:'Oružar',can_view_sov_base:true,can_view_katastar:false,can_edit_objects:false,can_upload_drawings:true,can_verify_drawings:false,can_manage_trips:false,can_manage_equipment:true,can_edit_news:false,can_use_sql_tools:false,can_manage_users:false,can_send_notifications:false},
    admin:{label:'Admin',can_view_sov_base:true,can_view_katastar:true,can_edit_objects:true,can_upload_drawings:true,can_verify_drawings:true,can_manage_trips:true,can_manage_equipment:true,can_edit_news:true,can_use_sql_tools:false,can_manage_users:true,can_send_notifications:true},
    webmaster:{label:'Webmaster',can_view_sov_base:true,can_view_katastar:true,can_edit_objects:true,can_upload_drawings:true,can_verify_drawings:true,can_manage_trips:true,can_manage_equipment:true,can_edit_news:true,can_use_sql_tools:true,can_manage_users:true,can_send_notifications:true}
  };
  const ABILITY_TO_PERMISSION = {
    admin:'can_manage_users', webmaster:'can_use_sql_tools', editor:'can_edit_news', armory:'can_manage_equipment', notification:'can_send_notifications',
    speleo_edit:'can_edit_objects', archive:'can_verify_drawings', sql:'can_use_sql_tools',
    trips:'can_manage_trips', drawings:'can_upload_drawings', katastar:'can_view_katastar', sov_base:'can_view_sov_base'
  };
  const BOOTSTRAP_WEBMASTER_EMAILS = ['darko.jeras@gmail.com'];
  function isBootstrapWebmasterEmail(email){ return BOOTSTRAP_WEBMASTER_EMAILS.includes(String(email||'').trim().toLowerCase()); }
  function bootstrapWebmasterProfile(user){
    return {id:user.id,email:user.email,full_name:'Darko Jeras',role:'webmaster',status:'approved',bootstrap_webmaster:true};
  }
  let client = null;
  let profileCache = null;
  let permissionCache = null;
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
  function fallbackPermissionsFor(role){ return {...(PERMISSION_FALLBACK[role] || PERMISSION_FALLBACK.user)}; }
  async function loadCurrentPermissions(force=false){
    if(permissionCache && !force) return permissionCache;
    if(SOV_OPEN_PREVIEW_MODE){
      const p = getOpenPreviewProfile();
      permissionCache = {...fallbackPermissionsFor(p.role), role:p.role, status:'approved', label:ROLE_LABELS[p.role]||p.role};
      return permissionCache;
    }
    const sb = getClient();
    if(!sb) return null;
    try{
      const {data,error} = await sb.from('sov_current_user_permissions').select('*').maybeSingle();
      if(error) throw error;
      permissionCache = data || null;
      return permissionCache;
    }catch(e){
      console.warn('SOV role permissions view nije dostupan, koristim fallback role map.', e);
      return null;
    }
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
      const perms = await loadCurrentPermissions(force);
      const effectiveRole = isBootstrapWebmasterEmail(user.email) ? 'webmaster' : String((perms && perms.role) || data.role || 'user');
      profileCache = {...data,email:user.email,role:effectiveRole,permissions:perms || fallbackPermissionsFor(effectiveRole)};
      return profileCache;
    }
    if(isBootstrapWebmasterEmail(user.email)){
      profileCache = data ? {...data,email:user.email,role:'webmaster',status:'approved',bootstrap_webmaster:true,permissions:fallbackPermissionsFor('webmaster')} : {...bootstrapWebmasterProfile(user),permissions:fallbackPermissionsFor('webmaster')};
      try{
        await sb.from('profiles').upsert({
          id:user.id, email:user.email, full_name:profileCache.full_name || 'Darko Jeras',
          role:'webmaster', status:'approved', approved_at:new Date().toISOString(), approved_by:user.id
        },{onConflict:'id'});
      }catch(e){ console.warn('Bootstrap webmaster profile sync skipped; client-side bootstrap remains active.', e); }
      return profileCache;
    }
    profileCache = data ? {...data,email:user.email,permissions:fallbackPermissionsFor(String(data.role||'user'))} : {id:user.id,email:user.email,full_name:user.email,role:'user',status:'pending',permissions:fallbackPermissionsFor('user')};
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
    profileCache = null; permissionCache = null;
    const profile = await getProfile(true);
    if(!profile || profile.status !== 'approved'){
      await sb.auth.signOut();
      profileCache = null;
      return {ok:false,msg:'Račun još nije odobren. Pričekaj admin approval.'};
    }
    return {ok:true,user:profile};
  }
  async function logout(){ const sb=getClient(); if(sb) await sb.auth.signOut(); profileCache=null; permissionCache=null; try{localStorage.removeItem('SOV_OPEN_PREVIEW_MODE');}catch(e){} location.href='index.html'; }

  async function can(ability){
    const u = await getProfile();
    if(!u || u.status !== 'approved') return false;
    const permKey = ABILITY_TO_PERMISSION[ability];
    if(!permKey){ return true; }
    if(u.permissions && Object.prototype.hasOwnProperty.call(u.permissions, permKey)) return !!u.permissions[permKey];
    const livePerms = await loadCurrentPermissions();
    if(livePerms && Object.prototype.hasOwnProperty.call(livePerms, permKey)) return !!livePerms[permKey];
    const fallback = fallbackPermissionsFor(String(u.role||'user'));
    return !!fallback[permKey];
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
  async function requireAdmin(){ const ok = await requireApproved(); if(!ok) return false; if(!(await can('admin'))){ location.href='dashboard.html?denied=admin'; return false; } return true; }
  async function requireWebmaster(){ const ok = await requireApproved(); if(!ok) return false; if(!(await can('webmaster'))){ location.href='dashboard.html?denied=webmaster'; return false; } return true; }
  async function requireEditor(){ const ok = await requireApproved(); if(!ok) return false; if(!(await can('editor'))){ location.href='dashboard.html?denied=editor'; return false; } return true; }
  async function requireArmory(){ const ok = await requireApproved(); if(!ok) return false; if(!(await can('armory'))){ location.href='dashboard.html?denied=armory'; return false; } return true; }
  async function requireArchive(){ const ok = await requireApproved(); if(!ok) return false; if(!(await can('archive'))){ location.href='dashboard.html?denied=archive'; return false; } return true; }

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
      ['admin','🛡️','Admin'],
      ['webmaster','🧑‍💻','Webmaster']
    ];
    if(!document.getElementById('sov-preview-role-style')){
      const st=document.createElement('style');
      st.id='sov-preview-role-style';
      st.textContent=`
        #sov-preview-role-switcher{position:fixed;left:14px;bottom:14px;z-index:999999;display:flex;gap:6px;align-items:center;flex-wrap:wrap;padding:8px;border:1px solid rgba(255,255,255,.14);border-radius:999px;background:rgba(4,10,11,.86);backdrop-filter:blur(14px);box-shadow:0 16px 50px rgba(0,0,0,.38);font-family:Inter,system-ui,sans-serif}
        #sov-preview-role-switcher .label{color:#b8c9c3;font-size:11px;font-weight:900;padding:0 5px;letter-spacing:.05em;text-transform:uppercase}
        #sov-preview-role-switcher button{border:1px solid rgba(255,255,255,.12);background:rgba(255,255,255,.06);color:#eef8f2;border-radius:999px;padding:7px 10px;font-weight:950;font-size:12px;cursor:pointer;display:inline-flex;align-items:center;gap:5px}
        #sov-preview-role-switcher button.active{background:linear-gradient(135deg,#d7f66f,#83e6c2);color:#111;border-color:transparent;box-shadow:0 8px 24px rgba(215,246,111,.18)}
        @media(max-width:720px){#sov-preview-role-switcher{left:8px;right:8px;bottom:8px;justify-content:center;border-radius:20px}#sov-preview-role-switcher .label{display:none}#sov-preview-role-switcher button{padding:8px 9px;font-size:11px}}
      `;
      document.head.appendChild(st);
    }
    if(!box){
      box=document.createElement('div');
      box.id='sov-preview-role-switcher';
      document.body.appendChild(box);
    }
    const current = (u && u.role) || getPreviewRole();
    box.innerHTML='<span class="label">Preview</span>'+roles.map(([r,ico,label])=>`<button type="button" data-preview-role="${r}" class="${r===current?'active':''}">${ico}<span>${label}</span></button>`).join('')+'<button type="button" data-preview-close>×</button>';
    const closeBtn = box.querySelector('[data-preview-close]');
    if(closeBtn){ closeBtn.title='Isključi preview'; closeBtn.onclick=()=>{ try{localStorage.removeItem('SOV_OPEN_PREVIEW_MODE')}catch(e){} location.href=location.pathname; }; }
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
    const canAdmin = await can('admin');
    const canEditor = await can('editor');
    const canArmory = await can('armory');
    const canArchive = await can('archive');
    const canSql = await can('sql');
    const canWebmaster = await can('webmaster');
    document.querySelectorAll('[data-role-admin]').forEach(el=>{el.style.display = canAdmin ? '' : 'none';});
    document.querySelectorAll('[data-role-editor]').forEach(el=>{el.style.display = canEditor ? '' : 'none';});
    document.querySelectorAll('[data-role-armory]').forEach(el=>{el.style.display = canArmory ? '' : 'none';});
    document.querySelectorAll('[data-role-archive]').forEach(el=>{el.style.display = canArchive ? '' : 'none';});
    document.querySelectorAll('[data-role-sql]').forEach(el=>{el.style.display = canSql ? '' : 'none';});
    document.querySelectorAll('[data-role-webmaster]').forEach(el=>{el.style.display = canWebmaster ? '' : 'none';});
    document.body.classList.remove('role-webmaster','role-admin','role-editor','role-oruzar','role-arhivar','role-user');
    document.body.classList.toggle('role-webmaster', !!(u && u.role==='webmaster'));
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
    if(!REGISTERED_PAGES.has(p)){ await renderUserBadge(); return; }
    if(p === 'admin-users.html' || p === 'admin-notifications.html') await requireAdmin();
    else if(p === 'role-manager.html' || p === 'sync-status.html' || p === 'audit-status.html' || p.startsWith('speleo-sql-')) await requireWebmaster();
    else if(p === 'news-editor.html') await requireEditor();
    else if(p === 'napisi-clanak.html') await requireApproved();
    else if(p === 'oruzarstvo.html' || p === 'oruzarstvo-import.html' || p.startsWith('oruzar-master') || p === 'inventura.html') await requireArmory();
    else if(p === 'arhivar-dashboard.html' || p === 'arhivar.html' || p === 'arhivar-zahvati.html' || p === 'arhivar-predane-jame.html' || p === 'arhivar-izvoz.html') await requireArchive();
    else await requireApproved();
    await renderUserBadge();
  }
  readyPromise = new Promise(resolve=>{
    document.addEventListener('DOMContentLoaded', async()=>{ await autoProtect(); resolve(true); });
  });

  window.SOVAuth = {isConfigured,getClient,getSession,getProfile,currentUser,register,login,logout,can,requireApproved,requireAdmin,requireWebmaster,requireEditor,requireArmory,requireArchive,loadUsers,approve,reject,setRole,loadCurrentPermissions,renderUserBadge,statusText,roleText,ready:()=>readyPromise};
})();
