/* SOV v6.1.58 — Inventar reads the exact same grouped view as Oprema za izlet. */
(function(){
  'use strict';

  const db=window.SOVArmoryDB;
  if(!db || typeof db.loadAllData!=='function' || db.__inventoryExactMemberCatalog) return;

  const originalLoadAllData=db.loadAllData;
  const SELECT='app_id,source_id,legacy_id,catalog_id,display_name,name,main_category,raw_category,subcategory,raw_subcategory,location_name,total_qty,available_qty,unit,status,availability,member_visible,note,detail_summary,quantity_label,available_label,catalog_group_key,search_text';

  function memberCategory(raw){
    const value=String(raw||'Ostalo').trim()||'Ostalo';
    return value==='Osobni SRT komplet'?'Osobna oprema':value;
  }

  async function loadExactGroupedView(){
    const client=window.SOVAuth&&SOVAuth.getClient?SOVAuth.getClient():null;
    if(!client) throw new Error('Supabase client nije dostupan');

    const {data,error}=await client
      .from('sov_equipment_app_catalog_grouped')
      .select(SELECT)
      .eq('member_visible',true)
      .order('priority',{ascending:true})
      .order('display_name',{ascending:true});

    if(error) throw error;

    const items=(data||[]).map((item,index)=>{
      const sourceId=String(item.source_id||item.legacy_id||item.app_id||item.catalog_id||item.catalog_group_key||('GROUP-'+index)).replace(/^item:/i,'').trim();
      const category=memberCategory(item.main_category||item.raw_category);
      const subcategory=String(item.subcategory||item.raw_subcategory||'Ostalo').trim()||'Ostalo';
      const name=String(item.display_name||item.name||'Artikl').trim()||'Artikl';
      const total=Number(item.total_qty)||0;
      const available=Number(item.available_qty)||0;

      return {
        ...item,
        id:item.app_id||item.catalog_group_key||item.catalog_id||sourceId,
        app_id:item.app_id||('item:'+sourceId),
        source_id:sourceId,
        legacy_id:item.legacy_id||sourceId,
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
        loaned:Math.max(0,total-available),
        location:item.location_name,
        source_sheet:'sov_equipment_app_catalog_grouped',
        item_kind:'grouped_member_catalog'
      };
    });

    const categories=[...new Set(items.map(item=>item.main_category).filter(Boolean))]
      .map((name,index)=>({id:'member-cat-'+index,name,description:'Isti katalog kao Oprema za izlet',type:'grouped_member_catalog',sort_order:index}));

    return {
      items,
      ropes:[],
      pieces:[],
      categories,
      raw_app_catalog:[],
      grouped_catalog:items,
      summary:{
        source:'sov_equipment_app_catalog_grouped',
        count_items:items.length,
        count_categories:categories.length,
        inventory_catalog_parity:true
      }
    };
  }

  db.loadAllData=async function(options){
    const isInventory=!!(document.body&&document.body.classList.contains('oruzar-page-inventar'));
    if(!isInventory) return originalLoadAllData.call(this,options);
    try{
      const exact=await loadExactGroupedView();
      window.__SOV_INVENTORY_GROUPED_CATALOG__=exact.items;
      return exact;
    }catch(error){
      console.warn('[inventory exact grouped catalog] fallback to existing loader',error);
      return originalLoadAllData.call(this,options);
    }
  };

  db.__inventoryExactMemberCatalog=true;
})();
