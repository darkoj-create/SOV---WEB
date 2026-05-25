
(function(){
  const LS_NOTES='sov_global_notifications';
  const LS_READ='sov_global_notifications_read';
  const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]));
  const nowIso=()=>new Date().toISOString();
  let state={open:false,tab:'inbox',profile:null,items:[],read:new Set(),canWrite:false,source:'local'};
  function sb(){return window.SOVAuth&&SOVAuth.getClient?SOVAuth.getClient():null}
  function configured(){return !!(window.SOVAuth&&SOVAuth.isConfigured&&SOVAuth.isConfigured()&&sb())}
  function role(){return (state.profile&&state.profile.role)||'user'}
  function userId(){return (state.profile&&state.profile.id)||null}
  function userLabel(){return (state.profile&&(state.profile.full_name||state.profile.email))||'Korisnik'}
  function localItems(){try{return JSON.parse(localStorage.getItem(LS_NOTES)||'[]')}catch(e){return []}}
  function saveLocal(items){localStorage.setItem(LS_NOTES,JSON.stringify(items||[]))}
  function loadRead(){try{return new Set(JSON.parse(localStorage.getItem(LS_READ)||'[]'))}catch(e){return new Set()}}
  function saveRead(){localStorage.setItem(LS_READ,JSON.stringify([...state.read]))}
  function targetOk(n){
    const t=String(n.target||n.target_role||'all').toLowerCase();
    if(t==='all'||t==='svi'||!t)return true;
    if(t==='armory'||t==='oruzar')return ['admin','oruzar'].includes(role());
    if(t==='admin')return role()==='admin';
    if(t==='user'||t==='users'||t==='clan'||t==='član')return role()==='user';
    if(n.target_user_id && userId())return String(n.target_user_id)===String(userId());
    return t===String(role()).toLowerCase();
  }
  function normalize(n){return {id:String(n.id||('N-'+Date.now())),title:n.title||'Obavijest',body:n.body||n.message||'',priority:n.priority||'normal',target:n.target_role||n.target||'all',author:n.author_name||n.created_by_name||n.author||'SOV',created_at:n.created_at||nowIso(),expires_at:n.expires_at||null,status:n.status||'active',target_user_id:n.target_user_id||null}}
  async function loadRemote(){
    if(!configured())return null;
    try{
      const client=sb();
      const {data,error}=await client.from('sov_notifications').select('*').eq('status','active').order('created_at',{ascending:false}).limit(100);
      if(error)throw error;
      state.source='supabase';
      return (data||[]).map(normalize);
    }catch(e){console.warn('[SOV inbox] remote load failed',e.message||e);return null}
  }
  async function saveRemote(n){
    if(!configured())return null;
    const client=sb();
    const payload={title:n.title,body:n.body,priority:n.priority,target_role:n.target,author_id:userId(),author_name:userLabel(),status:'active',created_at:nowIso(),updated_at:nowIso()};
    const {data,error}=await client.from('sov_notifications').insert(payload).select('*').single();
    if(error)throw error;
    return normalize(data);
  }
  async function markReadRemote(id){
    if(!configured()||!userId())return;
    try{await sb().from('sov_notification_reads').upsert({notification_id:id,user_id:userId(),read_at:nowIso()},{onConflict:'notification_id,user_id'});}catch(e){console.warn('[SOV inbox] mark read skipped',e.message||e)}
  }
  async function loadReadRemote(){
    if(!configured()||!userId())return;
    try{const {data,error}=await sb().from('sov_notification_reads').select('notification_id').eq('user_id',userId()); if(!error)(data||[]).forEach(r=>state.read.add(String(r.notification_id)));}catch(e){}
  }
  async function refresh(){
    try{state.profile=window.SOVAuth&&SOVAuth.getProfile?await SOVAuth.getProfile():null}catch(e){state.profile=null}
    try{state.canWrite=!!(window.SOVAuth&&SOVAuth.can&&(await SOVAuth.can('armory')))}catch(e){state.canWrite=false}
    state.read=loadRead(); await loadReadRemote();
    const remote=await loadRemote();
    state.items=(remote||localItems()).map(normalize).filter(targetOk).sort((a,b)=>String(b.created_at).localeCompare(String(a.created_at)));
    render();
  }
  function unreadCount(){return state.items.filter(n=>!state.read.has(String(n.id))).length}
  function priorityClass(p){p=String(p||'normal').toLowerCase();return p==='hitno'||p==='urgent'||p==='high'?'bad':(p==='važno'||p==='vazno'||p==='medium'?'warn':'')}
  function niceDate(s){if(!s)return ''; const d=new Date(s); if(isNaN(d))return String(s).slice(0,16); return d.toLocaleString('hr-HR',{day:'2-digit',month:'2-digit',year:'numeric',hour:'2-digit',minute:'2-digit'});}
  function shell(){
    if(document.getElementById('sovInboxFab'))return;
    document.body.insertAdjacentHTML('beforeend',`<button id="sovInboxFab" class="sov-inbox-fab" title="Inbox / obavijesti">✉️<span id="sovInboxBadge" class="sov-inbox-badge" style="display:none">0</span></button><div id="sovInboxBackdrop" class="sov-inbox-backdrop"></div><aside id="sovInboxPanel" class="sov-inbox-panel" aria-label="SOV inbox"></aside><div id="sovInboxToast" class="sov-inbox-toast"></div>`);
    document.getElementById('sovInboxFab').addEventListener('click',open);
    document.getElementById('sovInboxBackdrop').addEventListener('click',close);
  }
  function toast(m){const t=document.getElementById('sovInboxToast'); if(!t)return; t.textContent=m;t.classList.add('show');clearTimeout(t._to);t._to=setTimeout(()=>t.classList.remove('show'),2200)}
  function open(){state.open=true; const p=document.getElementById('sovInboxPanel'),b=document.getElementById('sovInboxBackdrop'); if(p)p.classList.add('open'); if(b)b.classList.add('open'); render();}
  function close(){state.open=false; const p=document.getElementById('sovInboxPanel'),b=document.getElementById('sovInboxBackdrop'); if(p)p.classList.remove('open'); if(b)b.classList.remove('open');}
  function render(){
    shell();
    const badge=document.getElementById('sovInboxBadge'); const u=unreadCount(); if(badge){badge.style.display=u?'grid':'none';badge.textContent=u>99?'99+':String(u)}
    const panel=document.getElementById('sovInboxPanel'); if(!panel)return;
    const can=state.canWrite;
    panel.innerHTML=`<div class="sov-inbox-head"><div><h2>✉️ Inbox</h2><p>Obavijesti za članove · ${esc(state.source)}</p></div><button class="sov-inbox-close" onclick="SOVInbox.close()">×</button></div><div class="sov-inbox-tabs"><button class="sov-inbox-tab ${state.tab==='inbox'?'active':''}" onclick="SOVInbox.tab('inbox')">Primljeno</button>${can?`<button class="sov-inbox-tab ${state.tab==='compose'?'active':''}" onclick="SOVInbox.tab('compose')">+ Nova obavijest</button>`:''}</div><div class="sov-inbox-body">${state.tab==='compose'&&can?composeHtml():inboxHtml()}</div>`;
  }
  function inboxHtml(){
    if(!state.items.length)return `<div class="sov-inbox-empty">Nema obavijesti.</div>`;
    return state.items.map(n=>`<article class="sov-msg ${state.read.has(String(n.id))?'':'unread'}"><div class="sov-msg-top"><div><h3>${esc(n.title)}</h3><small>${esc(n.author)} · ${esc(niceDate(n.created_at))}</small></div><span class="sov-pill ${priorityClass(n.priority)}">${esc(n.priority||'normal')}</span></div><p>${esc(n.body)}</p><div class="sov-msg-meta"><span class="sov-pill">Za: ${esc(n.target||'all')}</span>${state.read.has(String(n.id))?'<span class="sov-pill">pročitano</span>':`<button class="sov-inbox-btn" onclick="SOVInbox.markRead('${esc(n.id)}')">Označi pročitano</button>`}</div></article>`).join('')
  }
  function composeHtml(){return `<form class="sov-inbox-form" onsubmit="SOVInbox.send(event)"><input id="sovMsgTitle" class="sov-inbox-input" placeholder="Naslov obavijesti" required><textarea id="sovMsgBody" class="sov-inbox-textarea" placeholder="Poruka članovima..." required></textarea><div class="sov-inbox-actions"><select id="sovMsgTarget" class="sov-inbox-select"><option value="all">Svi korisnici</option><option value="user">Samo članovi</option><option value="armory">Admin + oružar</option><option value="admin">Samo admin</option></select><select id="sovMsgPriority" class="sov-inbox-select"><option value="normal">Normalno</option><option value="važno">Važno</option><option value="hitno">Hitno</option></select></div><div class="sov-inbox-actions"><button class="sov-inbox-btn primary">Pošalji obavijest</button><button type="button" class="sov-inbox-btn" onclick="SOVInbox.tab('inbox')">Odustani</button></div></form>`}
  async function send(ev){
    ev.preventDefault();
    if(!state.canWrite){toast('Samo admin ili oružar mogu slati obavijesti.');return;}
    const n=normalize({id:'N-'+Date.now(),title:document.getElementById('sovMsgTitle').value.trim(),body:document.getElementById('sovMsgBody').value.trim(),target:document.getElementById('sovMsgTarget').value,priority:document.getElementById('sovMsgPriority').value,author:userLabel(),created_at:nowIso()});
    let saved=null; try{saved=await saveRemote(n)}catch(e){console.warn('[SOV inbox] remote save failed',e.message||e);}
    if(!saved){const l=localItems(); l.unshift(n); saveLocal(l); state.source='local';}
    state.tab='inbox'; await refresh(); open(); toast(saved?'Obavijest poslana.':'Obavijest spremljena lokalno.');
  }
  async function markRead(id){state.read.add(String(id));saveRead();await markReadRemote(id);render();}
  function setTab(t){state.tab=t;render();}
  async function init(){if(window.SOVAuth&&SOVAuth.ready){try{await SOVAuth.ready()}catch(e){}} shell(); await refresh();}
  window.SOVInbox={init,refresh,open,close,tab:setTab,send,markRead};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();
