/**
 * SOV Nacrt clean branding + symbol layer.
 * Loaded after nacrt-v2.js.
 * Stable, vector-only post-processing: no canvas, no per-pixel work.
 */
(() => {
  'use strict';
  if (typeof NacrtRenderer === 'undefined' || typeof NacrtRenderer.render !== 'function') {
    throw new Error('Nacrt branding traži učitan nacrt-v2.js');
  }

  const renderV2 = NacrtRenderer.render.bind(NacrtRenderer);
  let SOV_LOGO = 'assets/nacrt-sov-logo.png';
  let PDS_LOGO = 'assets/nacrt-pds-velebit-logo.png';
  const blobToDataUrl = blob => new Promise((resolve,reject)=>{const r=new FileReader();r.onload=()=>resolve(String(r.result||''));r.onerror=reject;r.readAsDataURL(blob);});
  async function preloadOfficialLogos(){
    try{
      const [sovRes,pdsRes]=await Promise.all([fetch(SOV_LOGO,{cache:'force-cache'}),fetch(PDS_LOGO,{cache:'force-cache'})]);
      if(sovRes.ok) SOV_LOGO=await blobToDataUrl(await sovRes.blob());
      if(pdsRes.ok) PDS_LOGO=await blobToDataUrl(await pdsRes.blob());
    }catch(e){ console.warn('Official Nacrt logos preload skipped',e); }
  }
  preloadOfficialLogos();
  const LAYOUT = {
    profile: { left:105, top:352, width:640, height:1300, pad:18 },
    section: { left:835, top:500, width:270, height:300, pad:12 },
    plan: { left:760, top:810, width:430, height:500, pad:18 }
  };

  function getExtent(tdr, pad=0){
    if(!tdr) return null;
    let xmin=Infinity,xmax=-Infinity,ymin=Infinity,ymax=-Infinity;
    const add=(x,y)=>{ if(!Number.isFinite(x)||!Number.isFinite(y)) return; xmin=Math.min(xmin,x);xmax=Math.max(xmax,x);ymin=Math.min(ymin,y);ymax=Math.max(ymax,y); };
    for(const line of tdr.lines||[]) for(const p of line.pts||[]){ add(p.x,p.y); if(p.cp){add(p.cp.cx1,p.cp.cy1);add(p.cp.cx2,p.cp.cy2);} }
    for(const area of tdr.areas||[]) for(const p of area.pts||[]) add(p.x,p.y);
    for(const point of tdr.points||[]) add(point.x,point.y);
    for(const station of tdr.stations||[]) add(station.x,station.y);
    return Number.isFinite(xmin)?{xmin:xmin-pad,xmax:xmax+pad,ymin:ymin-pad,ymax:ymax+pad}:null;
  }

  function makeTransform(ext,area){
    if(!ext||!area) return null;
    const w=Math.max(1,ext.xmax-ext.xmin),h=Math.max(1,ext.ymax-ext.ymin);
    const scale=Math.min(area.width/w,area.height/h);
    const ox=area.left+(area.width-w*scale)/2,oy=area.top+(area.height-h*scale)/2;
    const tx=(x,y)=>({x:ox+(x-ext.xmin)*scale,y:oy+(y-ext.ymin)*scale});
    tx.scale=scale;tx.area=area;return tx;
  }

  function pathData(points,tx){
    if(!points||!points.length||!tx) return '';
    let d='';
    points.forEach((point,index)=>{
      const p=tx(point.x,point.y);
      if(index===0) d+=`M${p.x.toFixed(2)},${p.y.toFixed(2)}`;
      else if(point.cp){
        const c1=tx(point.cp.cx1,point.cp.cy1),c2=tx(point.cp.cx2,point.cp.cy2);
        d+=` C${c1.x.toFixed(2)},${c1.y.toFixed(2)} ${c2.x.toFixed(2)},${c2.y.toFixed(2)} ${p.x.toFixed(2)},${p.y.toFixed(2)}`;
      } else d+=` L${p.x.toFixed(2)},${p.y.toFixed(2)}`;
    });
    return d;
  }

  function patternFor(type){
    const t=String(type||'').toLowerCase();
    if(t.includes('debris')||t.includes('pebble')||t.includes('rubble')) return 'sov-pattern-debris';
    if(t.includes('block')||t.includes('rock')) return 'sov-pattern-blocks';
    if(t.includes('sand')) return 'sov-pattern-sand';
    if(t.includes('clay')||t.includes('mud')) return 'sov-pattern-clay';
    if(t.includes('water')) return 'sov-pattern-water';
    return '';
  }

  function materialDefs(){
    return `<defs id="sov-clean-symbol-defs">
      <pattern id="sov-pattern-debris" width="18" height="16" patternUnits="userSpaceOnUse">
        <path d="M2 12 L6 6 L10 12 Z M11 5 L14 2 L17 7 Z" fill="#eee" stroke="#666" stroke-width="0.8"/>
        <circle cx="3" cy="3" r="1" fill="#777"/><circle cx="14" cy="13" r="1.2" fill="#777"/>
      </pattern>
      <pattern id="sov-pattern-blocks" width="24" height="22" patternUnits="userSpaceOnUse">
        <path d="M2 8 L8 2 L14 5 L12 13 L5 15 Z M14 12 L20 7 L23 14 L18 20 L12 18 Z" fill="#f4f4f4" stroke="#555" stroke-width="1"/>
      </pattern>
      <pattern id="sov-pattern-sand" width="10" height="10" patternUnits="userSpaceOnUse">
        <circle cx="2" cy="2" r="0.8" fill="#555"/><circle cx="7" cy="5" r="0.7" fill="#777"/><circle cx="4" cy="9" r="0.6" fill="#666"/>
      </pattern>
      <pattern id="sov-pattern-clay" width="12" height="12" patternUnits="userSpaceOnUse">
        <path d="M0 6 Q3 3 6 6 T12 6" fill="none" stroke="#777" stroke-width="0.8"/>
      </pattern>
      <pattern id="sov-pattern-water" width="16" height="12" patternUnits="userSpaceOnUse">
        <path d="M0 4 Q4 1 8 4 T16 4 M0 9 Q4 6 8 9 T16 9" fill="none" stroke="#555" stroke-width="0.8"/>
      </pattern>
    </defs>`;
  }

  function renderAreas(tdr,tx,clipId){
    if(!tdr||!tx) return '';
    const items=[];
    for(const area of tdr.areas||[]){
      const pattern=patternFor(area.type);
      if(!pattern) continue;
      const d=pathData(area.pts,tx);
      if(d) items.push(`<path d="${d} Z" fill="url(#${pattern})" stroke="#555" stroke-width="0.8"/>`);
    }
    return items.length?`<g class="sov-clean-materials" clip-path="url(#${clipId})">${items.join('')}</g>`:'';
  }

  function replaceHeaderLogos(svg){
    svg=svg.replace(/<g transform="translate\([^>]+\)"><circle r="64"[\s\S]*?<text x="0" y="82"[\s\S]*?>SOV VELEBIT<\/text><\/g>/,
      `<image href="${SOV_LOGO}" x="50" y="32" width="154" height="154" preserveAspectRatio="xMidYMid meet"/>`);
    svg=svg.replace(/<g transform="translate\([^>]+\)"><path d="M0,-56[\s\S]*?<text x="0" y="83"[\s\S]*?>PDS VELEBIT<\/text><\/g>/,
      `<image href="${PDS_LOGO}" x="1042" y="22" width="132" height="166" preserveAspectRatio="xMidYMid meet"/>`);
    return svg;
  }

  function cleanBase(svg){
    return svg
      .replace(/fill="#d9c19d" fill-opacity="0\.55"/g,'fill="none"')
      .replace(/stroke="#b88a4b" stroke-opacity="0\.14" stroke-width="10"/g,'stroke="none" stroke-width="0"')
      .replace(/fill="#2b8d47" stroke="#1d6231"/g,'fill="#fff" stroke="#111"')
      .replace(/fill="#6b655f"/g,'fill="#555"')
      .replace(/<text x="34" y="1732"[^>]*>SOV Nacrt Generator v2<\/text>/g,'')
      .replace(/<text x="1194" y="1732"[^>]*>TopoDroid → SVG \/ PNG<\/text>/g,'');
  }

  NacrtRenderer.render=function renderCleanBranded(survey,options={}){
    let svg=renderV2(survey,options);
    svg=cleanBase(svg);
    svg=replaceHeaderLogos(svg);

    const profileTx=survey.profile?makeTransform(getExtent(survey.profile,LAYOUT.profile.pad),LAYOUT.profile):null;
    const planTx=survey.plan?makeTransform(getExtent(survey.plan,LAYOUT.plan.pad),LAYOUT.plan):null;
    const section=Array.isArray(survey.sections)?survey.sections[0]:null;
    const sectionTx=section?makeTransform(getExtent(section,LAYOUT.section.pad),LAYOUT.section):null;
    const overlays=[
      renderAreas(survey.profile,profileTx,'profile-clip'),
      renderAreas(section,sectionTx,'section-clip'),
      renderAreas(survey.plan,planTx,'plan-clip')
    ].join('');
    svg=svg.replace('</style>',`</style>${materialDefs()}${overlays}`);
    return svg;
  };

  if(typeof window!=='undefined') window.SOV_NACRT_CLEAN={version:'1.0'};
})();
