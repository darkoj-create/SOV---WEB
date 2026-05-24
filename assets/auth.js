(function(){
  const USERS_KEY='sovCloudUsers';
  const SESSION_KEY='sovCloudSession';
  const defaultAdmin={
    id:'admin-local', name:'SOV Admin', email:'admin@sov.local', password:'sovadmin', role:'admin', status:'approved', createdAt:'seed'
  };
  function loadUsers(){
    let users=[];
    try{users=JSON.parse(localStorage.getItem(USERS_KEY)||'[]')}catch(e){users=[]}
    if(!users.some(u=>u.email===defaultAdmin.email)){
      users.unshift(defaultAdmin);
      localStorage.setItem(USERS_KEY,JSON.stringify(users));
    }
    return users;
  }
  function saveUsers(users){localStorage.setItem(USERS_KEY,JSON.stringify(users))}
  function setSession(user){
    localStorage.setItem(SESSION_KEY,JSON.stringify({email:user.email,name:user.name,role:user.role,status:user.status,ts:Date.now()}));
  }
  function currentUser(){
    try{return JSON.parse(localStorage.getItem(SESSION_KEY)||'null')}catch(e){return null}
  }
  function logout(){localStorage.removeItem(SESSION_KEY); location.href='login.html'}
  function register(payload){
    const users=loadUsers();
    const email=(payload.email||'').trim().toLowerCase();
    if(!email||!payload.password||!payload.name) return {ok:false,msg:'Unesi ime, email i lozinku.'};
    if(users.some(u=>u.email===email)) return {ok:false,msg:'Korisnik s tim emailom već postoji.'};
    users.push({id:'u-'+Date.now(),name:payload.name.trim(),email,password:payload.password,role:'member',status:'pending',note:payload.note||'',createdAt:new Date().toISOString()});
    saveUsers(users);
    return {ok:true,msg:'Zahtjev je poslan. Čeka admin odobrenje.'};
  }
  function login(email,password){
    const users=loadUsers();
    const user=users.find(u=>u.email===(email||'').trim().toLowerCase()&&u.password===password);
    if(!user) return {ok:false,msg:'Krivi email ili lozinka.'};
    if(user.status!=='approved') return {ok:false,msg:'Račun još nije odobren. Pričekaj admin approval.'};
    setSession(user); return {ok:true,user};
  }
  function approve(email){const users=loadUsers(); const u=users.find(x=>x.email===email); if(u){u.status='approved'; saveUsers(users); return true} return false}
  function reject(email){const users=loadUsers(); const u=users.find(x=>x.email===email); if(u){u.status='rejected'; saveUsers(users); return true} return false}
  function requireApproved(){const u=currentUser(); if(!u||u.status!=='approved'){location.href='login.html?next='+encodeURIComponent(location.pathname.split('/').pop()||'dashboard.html')}}
  function requireAdmin(){const u=currentUser(); if(!u||u.role!=='admin'){location.href='dashboard.html'}}
  function renderUserBadge(){
    const u=currentUser();
    document.querySelectorAll('[data-user-name]').forEach(el=>{el.textContent=u?u.name:'Gost'});
    document.querySelectorAll('[data-user-role]').forEach(el=>{el.textContent=u?u.role:'guest'});
    document.querySelectorAll('[data-logout]').forEach(el=>{el.addEventListener('click',logout)});
  }
  window.SOVAuth={loadUsers,saveUsers,currentUser,logout,register,login,approve,reject,requireApproved,requireAdmin,renderUserBadge};
  loadUsers();
})();

/* DEV PREVIEW v2.1: registered pages are open until Supabase is connected. */
if (window.SOVAuth) { SOVAuth.requireApproved = function(){ return true; }; SOVAuth.requireAdmin = function(){ return true; }; }
