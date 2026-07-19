(() => {
  'use strict';
  if (typeof NacrtParser === 'undefined' || typeof TdrParser === 'undefined') {
    throw new Error('Semantic core traži NacrtParser i TdrParser.');
  }

  const KNOWN = {
    lines: new Set(['wall','wall:presumed','wall:ice','pit','overhang','arrow','border','user','water-flow','stream','river','flow','wood','log','timber','rope','slope','gradient','crack','fissure']),
    areas: new Set(['debris','blocks','sand','clay','mud','water','snow','ice','gravel','pebbles','cobbles','soil','earth','loam','humus','flowstone','calcite','moonmilk','formation','vegetation','grass','plant','bush','shrub','wood','log','timber','root','guano','lake','pool','sump','silt','rubble','scree','boulder','rock']),
    points: new Set(['blocks','rock','boulder','debris','rubble','scree','pebbles','snow','paleo-material','continuation','air-draught','entrance','section','pillar','tree','bush','plant','vegetation','conifer','broadleaf','deciduous','fir','spruce','pine','shrub','label','root','wood','log','timber','water-flow','spring','resurgence','stream','stalactite','stalagmite','column','pillar-formation','crystal','guano','danger','hazard'])
  };
  const esc = v => String(v ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  const meaningful = t => !!t && ((t.lines||[]).length || (t.areas||[]).length || (t.points||[]).length || (t.stations||[]).length);

  function getExtent(tdr,pad=0){
    if(!tdr) return null;
    let xmin=Infinity,xmax=-Infinity,ymin=Infinity,ymax=-Infinity;
    const add=(x,y)=>{if(!Number.isFinite(x)||!Number.isFinite(y))return;xmin=Math.min(xmin,x);xmax=Math.max(xmax,x);ymin=Math.min(ymin,y);ymax=Math.max(ymax,y);};
    for(const line of tdr.lines||[])for(const p of line.pts||[]){add(p.x,p.y);if(p.cp){add(p.cp.cx1,p.cp.cy1);add(p.cp.cx2,p.cp.cy2);}}
    for(const area of tdr.areas||[])for(const p of area.pts||[])add(p.x,p.y);
    for(const point of tdr.points||[])add(point.x,point.y);
    for(const station of tdr.stations||[])add(station.x,station.y);
    return Number.isFinite(xmin)?{xmin:xmin-pad,xmax:xmax+pad,ymin:ymin-pad,ymax:ymax+pad}:null;
  }
  function makeTransform(ext,area,maxScale=Infinity){
    if(!ext||!area)return null;
    const w=Math.max(1,ext.xmax-ext.xmin),h=Math.max(1,ext.ymax-ext.ymin);
    const scale=Math.min(area.width/w,area.height/h,maxScale);
    const ox=area.left+(area.width-w*scale)/2,oy=area.top+(area.height-h*scale)/2;
    const tx=(x,y)=>({x:ox+(x-ext.xmin)*scale,y:oy+(y-ext.ymin)*scale});
    tx.scale=scale;tx.area=area;tx.ext=ext;return tx;
  }
  function pathData(points,tx){
    if(!points||!points.length||!tx)return '';
    let d='';
    points.forEach((point,index)=>{
      const p=tx(point.x,point.y);
      if(index===0)d+=`M${p.x.toFixed(2)},${p.y.toFixed(2)}`;
      else if(point.cp){const c1=tx(point.cp.cx1,point.cp.cy1),c2=tx(point.cp.cx2,point.cp.cy2);d+=` C${c1.x.toFixed(2)},${c1.y.toFixed(2)} ${c2.x.toFixed(2)},${c2.y.toFixed(2)} ${p.x.toFixed(2)},${p.y.toFixed(2)}`;}
      else d+=` L${p.x.toFixed(2)},${p.y.toFixed(2)}`;
    });
    return d;
  }
  function lineTicks(points,tx,{spacing=12,length=5,side=1}={}){
    if(!points||points.length<2||!tx)return '';
    const parts=[];
    for(let i=1;i<points.length;i++){
      const a=tx(points[i-1].x,points[i-1].y),b=tx(points[i].x,points[i].y),dx=b.x-a.x,dy=b.y-a.y,dist=Math.hypot(dx,dy);
      if(dist<spacing*.55)continue;
      const nx=-dy/dist*side,ny=dx/dist*side,count=Math.max(1,Math.floor(dist/spacing));
      for(let j=1;j<=count;j++){const t=j/(count+1),x=a.x+dx*t,y=a.y+dy*t;parts.push(`M${x.toFixed(1)},${y.toFixed(1)} L${(x+nx*length).toFixed(1)},${(y+ny*length).toFixed(1)}`);}
    }
    return parts.join(' ');
  }
  function arrowHead(points,tx,size=7){
    if(!points||points.length<2||!tx)return '';
    const a=tx(points.at(-2).x,points.at(-2).y),b=tx(points.at(-1).x,points.at(-1).y),ang=Math.atan2(b.y-a.y,b.x-a.x);
    const l={x:b.x-Math.cos(ang-.55)*size,y:b.y-Math.sin(ang-.55)*size},r={x:b.x-Math.cos(ang+.55)*size,y:b.y-Math.sin(ang+.55)*size};
    return `${b.x.toFixed(1)},${b.y.toFixed(1)} ${l.x.toFixed(1)},${l.y.toFixed(1)} ${r.x.toFixed(1)},${r.y.toFixed(1)}`;
  }
  function diagnostics(scraps){
    const u={unknownLines:new Set(),unknownAreas:new Set(),unknownPoints:new Set()};
    for(const tdr of scraps){
      for(const x of tdr.lines||[]){const t=String(x.type||'').toLowerCase();if(t&&!KNOWN.lines.has(t))u.unknownLines.add(t);}
      for(const x of tdr.areas||[]){const t=String(x.type||'').toLowerCase();if(t&&!KNOWN.areas.has(t))u.unknownAreas.add(t);}
      for(const x of tdr.points||[]){const t=String(x.type||'').toLowerCase();if(t&&!KNOWN.points.has(t))u.unknownPoints.add(t);}
    }
    return Object.fromEntries(Object.entries(u).map(([k,v])=>[k,[...v].sort()]));
  }

  const oldParse=NacrtParser.parseZip.bind(NacrtParser);
  NacrtParser.parseZip=async file=>{
    const survey=await oldParse(file),all=[];
    try{
      const zip=await JSZip.loadAsync(file);
      for(const [path,entry] of Object.entries(zip.files)){
        if(entry.dir||!/\.tdr$/i.test(path))continue;
        try{const t=TdrParser.parse(await entry.async('arraybuffer'),path);t.filename=path;if(meaningful(t))all.push(t);}catch(e){console.warn('Preskačem TDR scrap',path,e);}
      }
    }catch(e){console.warn('Dodatno učitavanje scrapova nije uspjelo',e);}
    if(all.length){
      survey.allScraps=all;survey.plans=all.filter(t=>t.plotType===1);survey.profiles=all.filter(t=>t.plotType===2);survey.sections=all.filter(t=>t.plotType===0);
      survey.plan=survey.plans[0]||survey.plan||null;survey.profile=survey.profiles[0]||survey.profile||null;survey.semanticDiagnostics=diagnostics(all);
      const d=survey.semanticDiagnostics;if(d.unknownLines.length||d.unknownAreas.length||d.unknownPoints.length)console.info('Nacrt semantic fallback:',d);
    }
    return survey;
  };

  window.SOVSemanticCore={version:'6.0',KNOWN,esc,meaningful,getExtent,makeTransform,pathData,lineTicks,arrowHead};
})();
