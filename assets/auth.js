
(function(){
 const KEY='sov_users_v13'; const SESSION='sov_session_v13';
 function users(){return JSON.parse(localStorage.getItem(KEY)||'[]')}
 function saveUsers(u){localStorage.setItem(KEY,JSON.stringify(u))}
 if(!users().some(u=>u.email==='admin@sov.local')) saveUsers([{name:'Admin',email:'admin@sov.local',pass:'admin',status:'approved',role:'admin'}]);
 window.SOVAuth={
  current(){let e=localStorage.getItem(SESSION);return users().find(u=>u.email===e)},
  require(){let u=this.current(); if(!u||u.status!=='approved') location.href='login.html'; return u},
  login(email,pass){let u=users().find(x=>x.email===email&&x.pass===pass); if(!u) return {ok:false,msg:'Krivi email ili lozinka.'}; if(u.status!=='approved') return {ok:false,msg:'Registracija čeka admin odobrenje.'}; localStorage.setItem(SESSION,u.email); return {ok:true}},
  register(name,email,pass,reason){let u=users(); if(u.some(x=>x.email===email)) return {ok:false,msg:'Korisnik već postoji.'}; u.push({name,email,pass,reason,status:'pending',role:'member'}); saveUsers(u); return {ok:true,msg:'Zahtjev je poslan. Admin ga treba odobriti.'}},
  logout(){localStorage.removeItem(SESSION); location.href='index.html'},
  list(){return users()}, approve(email){let u=users(); u.forEach(x=>{if(x.email===email)x.status='approved'}); saveUsers(u)}, reject(email){saveUsers(users().filter(x=>x.email!==email))}
 }
})();
