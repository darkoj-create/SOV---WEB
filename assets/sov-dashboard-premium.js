(function(){
  const $=(s,r=document)=>r.querySelector(s);
  const $$=(s,r=document)=>Array.from(r.querySelectorAll(s));
  function text(sel,v){const el=$(sel); if(el) el.textContent=v;}
  function fmtDate(v){try{return v?new Date(v).toLocaleString('hr-HR'):'—'}catch(e){return '—'}}
  function escapeHtml(s){return String(s||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
  async function safeCount(sb, table){try{const {count,error}=await sb.from(table).select('*',{count:'exact',head:true}); if(error) throw error; return count||0;}catch(e){return '—';}}
  async function safeRows(sb, table, limit){try{const {data,error}=await sb.from(table).select('*').limit(limit||5); if(error) throw error; return data||[];}catch(e){return [];}}
  function accessSummary(role){
    const map={admin:'Puni nadzor nad sustavom, korisnicima, SQL alatima i svim modulima.',oruzar:'Operativni pristup Oružarstvu, katalogu opreme i zahtjevima.',arhivar:'Arhiva, nacrti, TopoDroid i provjera metapodataka.',editor:'Vijesti, članci i javni sadržaj.',user:'Terenski pregled, izleti, karta i zahtjevi prema internim modulima.'};
    return map[role]||map.user;
  }
  async function init(){
    if(!window.SOVAuth) return;
    await SOVAuth.ready();
    const u=await SOVAuth.getProfile();
    text('[data-dash-name]', u ? (u.full_name||u.email||'Član') : 'Gost');
    text('[data-dash-role]', u ? SOVAuth.roleText(u.role) : 'Gost');
    text('[data-access-summary]', accessSummary(u&&u.role));
    const sb=SOVAuth.getClient && SOVAuth.getClient();
    if(!sb){ text('[data-kpi-sync]','offline'); return; }
    const [profiles, devices, audits, drawings] = await Promise.all([
      safeCount(sb,'profiles'), safeCount(sb,'sov_user_devices'), safeCount(sb,'sov_audit_log'), safeCount(sb,'speleo_object_drawings')
    ]);
    text('[data-kpi-users]', profiles); text('[data-kpi-devices]', devices); text('[data-kpi-audit]', audits); text('[data-kpi-drawings]', drawings);
    const auditRows = await safeRows(sb,'sov_audit_log_recent',5);
    const auditBox=$('[data-recent-audit]');
    if(auditBox){
      auditBox.innerHTML = auditRows.length ? auditRows.map(r=>`<div class="sov-list-item"><div><b>${escapeHtml(r.action||'Akcija')}</b><small>${escapeHtml(r.actor_email||r.user_email||'korisnik')} · ${escapeHtml(r.entity_type||'sustav')}</small></div><span class="sov-soft">${escapeHtml(fmtDate(r.created_at))}</span></div>`).join('') : '<div class="sov-empty">Još nema audit zapisa ili view nije dostupan. Kad modul krene raditi promjene, ovdje će se vidjeti zadnje akcije.</div>';
    }
    const devRows = await safeRows(sb,'sov_user_devices_recent',5);
    const devBox=$('[data-device-health]');
    if(devBox){
      devBox.innerHTML = devRows.length ? devRows.map(r=>`<div class="sov-list-item"><div><b>${escapeHtml(r.device_name||r.device_id||'Uređaj')}</b><small>${escapeHtml(r.platform||'platforma')} · ${escapeHtml(r.app_version||r.app_build||'app')}</small></div><span class="sov-soft">${escapeHtml(fmtDate(r.last_seen_at))}</span></div>`).join('') : '<div class="sov-empty">Nema registriranih uređaja ili APK još ne šalje device telemetry. To je OK za trenutnu fazu.</div>';
    }
  }
  document.addEventListener('DOMContentLoaded',init);
})();
