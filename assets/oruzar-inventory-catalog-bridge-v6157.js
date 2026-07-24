/* SOV v6.1.57 — Inventory and borrowing must use one catalog source. */
(function(){
  if(!document.body || !document.body.classList.contains('oruzar-page-inventar')) return;
  const db=window.SOVArmoryDB;
  if(!db || typeof db.loadAllData!=='function') return;
  const original=db.loadAllData.bind(db);
  db.loadAllData=async function(options){
    const data=await original(options);
    if(!data) return data;
    // oruzar-master-clean.js historically prefers raw_app_catalog whenever present.
    // On Inventory that created a parallel article list different from borrowing.
    // Hide only that alternate representation so both screens use data.items,
    // which comes from sov_equipment_app_catalog_grouped.
    return {...data, raw_app_catalog:[]};
  };
})();
