/* SOV v6.1.57 — Inventar koristi isti grupirani katalog kao posuđivanje. */
(function(){
  const db=window.SOVArmoryDB;
  if(!db || typeof db.loadAllData!=='function') return;
  const originalLoadAllData=db.loadAllData.bind(db);
  db.loadAllData=async function(options){
    const data=await originalLoadAllData(options);
    if(!data || !Array.isArray(data.items) || !data.items.length) return data;
    // Master kod inače zamijeni grupirani katalog raw redovima. Samo na Inventaru
    // uklanjamo taj alternativni izvor kako bi popis bio 1:1 isti kao kod posuđivanja.
    return {...data,raw_app_catalog:[]};
  };
})();
