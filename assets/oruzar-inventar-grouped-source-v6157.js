(function(){
  'use strict';
  if(!document.body || !document.body.classList.contains('oruzar-page-inventar')) return;
  const SELECT='app_id,source_id,legacy_id,catalog_id,display_name,name,main_category,raw_category,subcategory,raw_subcategory,location_name,total_qty,available_qty,unit,status,availability,member_visible,note,detail_summary,quantity_label,available_label,catalog_group_key,search_text';
  function patch(){
    if(!window.SOVArmoryDB || window.SOVArmoryDB.__inventoryGroupedPatched) return false;
    const original=window.SOVArmoryDB.loadAllData;
    window.SOVArmoryDB.loadAllData=async function(){
      const client=window.SOVAuth&&SOVAuth.getClient?SOVAuth.getClient():null;
      if(!client) return typeof original==='function'?original.apply(this,arguments):{items:[],ropes:[],pieces:[],categories:[]};
      const {data,error}=await client
        .from('sov_equipment_app_catalog_grouped')
        .select(SELECT)
        .eq('member_visible',true)
        .order('priority',{ascending:true})
        .order('display_name',{ascending:true});
      if(error) throw error;
      const rows=(data||[]).map(r=>({
        ...r,
        category_name:r.main_category||r.raw_category||'Ostalo',
        category:r.main_category||r.raw_category||'Ostalo',
        xls_category:r.main_category||r.raw_category||'Ostalo',
        xls_subcategory:r.subcategory||r.raw_subcategory||'Ostalo',
        quantity:r.total_qty,
        available:r.available_qty,
        loaned:Math.max(0,(Number(r.total_qty)||0)-(Number(r.available_qty)||0)),
        location:r.location_name||'',
        minimum:0,
        variant_count:1
      }));
      return {items:rows,ropes:[],pieces:[],categories:[],raw_app_catalog:[]};
    };
    window.SOVArmoryDB.__inventoryGroupedPatched=true;
    return true;
  }
  if(!patch()){
    let tries=0;
    const timer=setInterval(()=>{tries++; if(patch()||tries>40)clearInterval(timer);},25);
  }
})();