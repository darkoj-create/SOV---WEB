(function(){
  if(window.__SOV_INVENTORY_CATALOG_SYNC_V6157__) return;
  window.__SOV_INVENTORY_CATALOG_SYNC_V6157__=true;

  function isInventoryPage(){
    return !!document.body && document.body.classList.contains('oruzar-page-inventar');
  }

  function install(){
    if(!window.SOVArmoryDB || typeof window.SOVArmoryDB.loadAllData!=='function') return false;
    if(window.SOVArmoryDB.loadAllData.__inventoryCatalogSyncV6157) return true;

    const original=window.SOVArmoryDB.loadAllData.bind(window.SOVArmoryDB);
    const wrapped=async function(options){
      const data=await original(options);
      if(!isInventoryPage() || !data) return data;

      // Inventar mora gledati isti kanonski/grupirani katalog kao dio za posuđivanje.
      // Master renderer inače automatski prelazi na raw_app_catalog, što stvara
      // druge nazive, duplikate i drugačije količine.
      return {
        ...data,
        items:Array.isArray(data.items)?data.items:[],
        ropes:Array.isArray(data.ropes)?data.ropes:[],
        pieces:Array.isArray(data.pieces)?data.pieces:[],
        raw_app_catalog:[]
      };
    };

    wrapped.__inventoryCatalogSyncV6157=true;
    wrapped.__original=original;
    window.SOVArmoryDB.loadAllData=wrapped;
    return true;
  }

  if(!install()){
    document.addEventListener('DOMContentLoaded',install,{once:true});
    setTimeout(install,100);
    setTimeout(install,500);
  }
})();
