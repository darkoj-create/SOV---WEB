/* SOV v6.1.57 — Inventar uses the exact same grouped catalog as member borrowing. */
(function(){
  if(!document.body || !document.body.classList.contains('oruzar-page-inventar')) return;
  if(!window.SOVArmoryDB || !window.SOVAuth || !SOVAuth.getClient) return;

  const originalLoadAllData = SOVArmoryDB.loadAllData && SOVArmoryDB.loadAllData.bind(SOVArmoryDB);
  const SELECT = 'app_id,source_id,legacy_id,catalog_id,display_name,name,main_category,raw_category,subcategory,raw_subcategory,location_name,total_qty,available_qty,unit,status,availability,member_visible,note,detail_summary,quantity_label,available_label,catalog_group_key,search_text,priority';

  async function loadGroupedCatalog(){
    const client = SOVAuth.getClient();
    if(!client) throw new Error('Supabase client nije dostupan');
    const {data,error}=await client
      .from('sov_equipment_app_catalog_grouped')
      .select(SELECT)
      .eq('member_visible',true)
      .order('priority',{ascending:true})
      .order('display_name',{ascending:true});
    if(error) throw error;
    return (data||[]).map(row=>({
      ...row,
      id:row.app_id||row.catalog_group_key||row.catalog_id||row.legacy_id||row.source_id,
      category_name:row.main_category||row.raw_category||'Ostalo',
      category:row.main_category||row.raw_category||'Ostalo',
      xls_category:row.main_category||row.raw_category||'Ostalo',
      xls_subcategory:row.subcategory||row.raw_subcategory||'Ostalo',
      quantity:row.total_qty,
      available:row.available_qty,
      loaned:Math.max(0,(Number(row.total_qty)||0)-(Number(row.available_qty)||0)),
      location:row.location_name,
      item_kind:'grouped_catalog_article',
      variants:1
    }));
  }

  SOVArmoryDB.loadAllData = async function(options){
    try{
      const grouped=await loadGroupedCatalog();
      window.__SOV_INVENTORY_GROUPED_CATALOG__=grouped;
      return {
        items:grouped,
        ropes:[],
        pieces:[],
        categories:[],
        raw_app_catalog:grouped,
        grouped_catalog:grouped,
        inventory_catalog_source:'sov_equipment_app_catalog_grouped'
      };
    }catch(error){
      console.warn('[inventory grouped catalog] fallback to existing loader',error);
      if(originalLoadAllData) return originalLoadAllData(options);
      throw error;
    }
  };
})();
