(function(){
  const REGISTERED_PAGES = new Set([
    'dashboard.html','baza.html','pregled-baze.html','izleti.html','kalendar-izleta.html',
    'dokumentacija.html','pregled-zapisnika.html','zapisnici-skupstine.html','novi-zapisnik.html',
    'speleo-zapisnik.html','topodroid.html','napisi-clanak.html','oruzarstvo.html','admin-users.html'
  ]);
  const ROLE_LABELS = {admin:'Admin',editor:'Urednik',oruzar:'Oružar',user:'Član'};
  const ADMIN_ROLES = ['admin'];
  const EDITOR_ROLES = ['admin','editor'];
  const ARMORY_ROLES = ['admin','oruzar'];
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
    const sb = getClient();
    if(!sb) return {session:null,user:null};
    const {data} = await sb.auth.getSession();
    return {session:data.session,user:data.session && data.session.user};
  }
  async function getProfile(force=false){
    if(profileCache && !force) return profileCache;
    const sb = getClient();
    if(!sb) return null;
    const {user} = await getSession();
    if(!user) return null;
    const {data,error} = await sb.from('profiles').select('*').eq('id',user.id).maybeSingle();
    if(error){ console.error(error); return null; }
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
  async function logout(){ const sb=getClient(); if(sb) await sb.auth.signOut(); profileCache=null; location.href='login.html'; }

  async function can(ability){
    const u = await getProfile();
    if(!u || u.status !== 'approved') return false;
    if(ability === 'admin') return ADMIN_ROLES.includes(u.role);
    if(ability === 'editor') return EDITOR_ROLES.includes(u.role);
    if(ability === 'armory') return ARMORY_ROLES.includes(u.role);
    return true;
  }
  async function requireApproved(){
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
  async function requireAdmin(){
    const ok = await requireApproved(); if(!ok) return false;
    if(!(await can('admin'))){ location.href='dashboard.html?denied=admin'; return false; }
    return true;
  }
  async function requireEditor(){
    const ok = await requireApproved(); if(!ok) return false;
    if(!(await can('editor'))){ location.href='dashboard.html?denied=editor'; return false; }
    return true;
  }
  async function requireArmory(){
    const ok = await requireApproved(); if(!ok) return false;
    if(!(await can('armory'))){ location.href='dashboard.html?denied=armory'; return false; }
    return true;
  }

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

  async function renderUserBadge(){
    const u = await getProfile();
    document.querySelectorAll('[data-user-name]').forEach(el=>{el.textContent = u ? (u.full_name || u.email) : 'Gost';});
    document.querySelectorAll('[data-user-role]').forEach(el=>{el.textContent = u ? roleText(u.role) : 'Gost';});
    document.querySelectorAll('[data-auth-status]').forEach(el=>{el.textContent = u ? statusText(u.status) : 'Nije prijavljen';});
    document.querySelectorAll('[data-logout]').forEach(el=>{el.addEventListener('click',e=>{e.preventDefault();logout();});});
    document.querySelectorAll('[data-role-admin]').forEach(el=>{el.style.display = u && u.role === 'admin' ? '' : 'none';});
    document.querySelectorAll('[data-role-editor]').forEach(el=>{el.style.display = u && EDITOR_ROLES.includes(u.role) ? '' : 'none';});
    document.querySelectorAll('[data-role-armory]').forEach(el=>{el.style.display = u && ARMORY_ROLES.includes(u.role) ? '' : 'none';});
    document.body.classList.toggle('role-admin', !!(u && u.role==='admin'));
    document.body.classList.toggle('role-editor', !!(u && u.role==='editor'));
    document.body.classList.toggle('role-oruzar', !!(u && u.role==='oruzar'));
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
    else await requireApproved();
    await renderUserBadge();
  }
  readyPromise = new Promise(resolve=>{
    document.addEventListener('DOMContentLoaded', async()=>{ await autoProtect(); resolve(true); });
  });

  window.SOVAuth = {isConfigured,getClient,getSession,getProfile,currentUser,register,login,logout,can,requireApproved,requireAdmin,requireEditor,requireArmory,loadUsers,approve,reject,setRole,renderUserBadge,statusText,roleText,ready:()=>readyPromise};
})();
