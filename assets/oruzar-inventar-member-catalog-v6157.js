/* SOV v6.1.57 — Inventar uses the exact same grouped member catalog as Oprema za izlet. */
(function(){
  'use strict';

  const db=window.SOVArmoryDB;
  if(!db || typeof db.loadAllData!=='function' || db.__inventoryMemberCatalogParity) return;

  const originalLoadAllData=db.loadAllData;

  function memberCategory(raw){
    const value=String(raw||'Ostalo').trim()||'Ostalo';
    return value==='Osobni SRT komplet' ? 'Osobna oprema' : value;
  }

  function groupedInventoryData(data){
    const sourceItems=Array.isArray(data&&data.items)?data.items:[];
    const items=sourceItems
      .filter(item=>item&&item.member_visible!==false)
      .map((item,index)=>{
        const sourceId=String(
          item.source_id||item.legacy_id||item.id||item.catalog_id||('GROUP-'+index)
        ).replace(/^item:/i,'').trim();
        const category=memberCategory(
          item.main_category||item.raw_category||item.xls_category||item.category_name||item.category
        );
        const subcategory=String(item.subcategory||item.raw_subcategory||item.xls_subcategory||'Ostalo').trim()||'Ostalo';
        const name=String(item.display_name||item.name||'Artikl').trim()||'Artikl';
        const total=item.total_qty??item.quantity??0;
        const available=item.available_qty??item.available??0;

        return {
          ...item,
          id:item.id||sourceId,
          app_id:item.app_id||('item:'+sourceId),
          source_id:sourceId,
          legacy_id:sourceId,
          catalog_id:item.catalog_group_key||item.catalog_id||sourceId,
          display_name:name,
          name,
          main_category:category,
          raw_category:category,
          xls_category:category,
          category_name:category,
          category,
          raw_subcategory:subcategory,
          xls_subcategory:subcategory,
          subcategory,
          total_qty:total,
          quantity:total,
          available_qty:available,
          available,
          source_sheet:'sov_equipment_app_catalog_grouped'
        };
      });

    const categories=[...new Set(items.map(item=>item.main_category).filter(Boolean))]
      .map((name,index)=>({id:'member-cat-'+index,name,description:'Isti katalog kao Oprema za izlet',type:'grouped_member_catalog',sort_order:index}));

    return {
      ...data,
      items,
      ropes:[],
      pieces:[],
      categories,
      // Oružar Master previously replaced the grouped catalog with this raw list.
      // Empty it only on Inventar so the shared loader keeps the member-facing articles 1:1.
      raw_app_catalog:[],
      summary:{
        ...(data.summary||{}),
        source:'sov_equipment_app_catalog_grouped',
        count_items:items.length,
        count_categories:categories.length,
        inventory_catalog_parity:true
      }
    };
  }

  db.loadAllData=async function(options){
    const data=await originalLoadAllData.call(this,options);
    const isInventory=!!(document.body&&document.body.classList.contains('oruzar-page-inventar'));
    return isInventory&&data ? groupedInventoryData(data) : data;
  };

  db.__inventoryMemberCatalogParity=true;
})();
