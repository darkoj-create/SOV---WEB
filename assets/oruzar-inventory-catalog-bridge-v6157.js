/* SOV v6.1.57 — Inventory and borrowing must use one catalog source. */
(function(){
  function install(){
    if(!document.body || !document.body.classList.contains('oruzar-page-inventar')) return;
    const db=window.SOVArmoryDB;
    if(!db || typeof db.loadAllData!=='function' || db.__inventorySingleSourceInstalled) return;
    const original=db.loadAllData.bind(db);
    db.loadAllData=async function(options){
      const data=await original(options);
      if(!data) return data;
      // Master historically preferred raw_app_catalog whenever present.
      // Inventory must instead use the exact same grouped catalog as borrowing.
      return {...data, raw_app_catalog:[]};
    };
    db.__inventorySingleSourceInstalled=true;
  }
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',install,{once:true});
  else install();
})();
