(function(){
  'use strict';

  const DEG=Math.PI/180;
  const EARTH_M_PER_DEG=111320;
  const DEFAULT_LAYERS={areas:true,walls:true,details:true,water:true,centerline:true,stations:false,labels:true};

  function num(v,d=0){const n=Number(v);return Number.isFinite(n)?n:d}
  function normStation(v){
    return String(v??'').trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/\s+/g,'');
  }
  function stationAliases(v){
    const raw=normStation(v); if(!raw)return [];
    const out=new Set([raw]);
    if(raw.includes('@')){const p=raw.split('@').filter(Boolean);p.forEach(x=>out.add(x));out.add(p[0]);out.add(p[p.length-1]);}
    if(raw.includes(':')){const p=raw.split(':').filter(Boolean);p.forEach(x=>out.add(x));out.add(p[p.length-1]);}
    return [...out];
  }
  function sqlStationLookup(survey){
    const out=new Map();
    const coords=survey?.sql?.stationCoords||{};
    Object.entries(coords).forEach(([name,c])=>stationAliases(name).forEach(a=>{if(!out.has(a))out.set(a,{name,e:num(c.e),n:num(c.n),z:num(c.z)})}));
    return out;
  }
  function matchStations(survey){
    const lookup=sqlStationLookup(survey); const pairs=[]; const seen=new Set();
    for(const st of survey?.plan?.stations||[]){
      let target=null;
      for(const a of stationAliases(st.name)){if(lookup.has(a)){target=lookup.get(a);break}}
      if(!target)continue;
      const key=target.name+'|'+num(st.x)+'|'+num(st.y);if(seen.has(key))continue;seen.add(key);
      pairs.push({name:String(st.name||target.name),sx:num(st.x)/20,sy:num(st.y)/20,e:target.e,n:target.n});
    }
    return pairs;
  }
  function solveSimilarity(pairs,flipY){
    if(!Array.isArray(pairs)||pairs.length<2)return null;
    const src=pairs.map(p=>({x:p.sx,y:(flipY?-p.sy:p.sy),X:p.e,Y:p.n,name:p.name}));
    const n=src.length;
    const cx=src.reduce((s,p)=>s+p.x,0)/n, cy=src.reduce((s,p)=>s+p.y,0)/n;
    const cX=src.reduce((s,p)=>s+p.X,0)/n, cY=src.reduce((s,p)=>s+p.Y,0)/n;
    let den=0,dot=0,cross=0;
    src.forEach(p=>{const x=p.x-cx,y=p.y-cy,X=p.X-cX,Y=p.Y-cY;den+=x*x+y*y;dot+=x*X+y*Y;cross+=x*Y-y*X;});
    if(den<1e-9)return null;
    const a=dot/den,b=cross/den;
    const tx=cX-(a*cx-b*cy),ty=cY-(b*cx+a*cy);
    const apply=(x,y)=>{const yy=flipY?-num(y):num(y),xx=num(x);return {e:a*xx-b*yy+tx,n:b*xx+a*yy+ty}};
    let err2=0;src.forEach(p=>{const E=a*p.x-b*p.y+tx,N=b*p.x+a*p.y+ty;err2+=(E-p.X)**2+(N-p.Y)**2});
    return {a,b,tx,ty,flipY,scale:Math.hypot(a,b),rotationDeg:Math.atan2(b,a)/DEG,rms:Math.sqrt(err2/n),matched:n,apply};
  }
  function deriveAutoTransform(survey){
    const pairs=matchStations(survey);
    if(pairs.length<2)return {ok:false,pairs,reason:'Premalo zajedničkih mjernih stanica'};
    const a=solveSimilarity(pairs,false),b=solveSimilarity(pairs,true);
    const candidates=[a,b].filter(Boolean).sort((x,y)=>x.rms-y.rms);
    const best=candidates[0];
    if(!best)return {ok:false,pairs,reason:'Transformacija se ne može izračunati'};
    return {ok:true,pairs,best,quality:best.rms<=0.35?'excellent':best.rms<=1.5?'good':best.rms<=5?'review':'poor'};
  }
  function samplePts(points,apply){
    if(!Array.isArray(points)||!points.length)return [];
    const out=[];
    const push=(x,y)=>{const q=apply(num(x)/20,num(y)/20);out.push([q.e,q.n])};
    push(points[0].x,points[0].y);
    for(let i=1;i<points.length;i++){
      const p0=points[i-1],p=points[i];
      if(p.cp){
        const x0=num(p0.x)/20,y0=num(p0.y)/20,x1=num(p.cp.cx1)/20,y1=num(p.cp.cy1)/20,x2=num(p.cp.cx2)/20,y2=num(p.cp.cy2)/20,x3=num(p.x)/20,y3=num(p.y)/20;
        for(let k=1;k<=8;k++){const t=k/8,u=1-t;const x=u*u*u*x0+3*u*u*t*x1+3*u*t*t*x2+t*t*t*x3;const y=u*u*u*y0+3*u*u*t*y1+3*u*t*t*y2+t*t*t*y3;const q=apply(x,y);out.push([q.e,q.n]);}
      }else push(p.x,p.y);
    }
    return out;
  }
  function layerForType(type,kind){
    const t=String(type||'').toLowerCase();
    if(t.includes('water'))return 'water';
    if(kind==='area')return 'areas';
    if(t==='wall'||t==='border'||t==='rock-border')return 'walls';
    if(t==='label')return 'labels';
    return 'details';
  }
  function feature(geometry,properties){return {type:'Feature',geometry,properties}}
  function buildLocalGeometry(survey,transform,anchorStation){
    if(!survey?.plan) return {type:'FeatureCollection',features:[]};
    const t=transform?.ok?transform.best:null;
    const fallback={apply:(x,y)=>({e:x,n:-y}),rms:null,matched:0,rotationDeg:0,scale:1,flipY:true};
    const use=t||fallback;
    const coords=survey?.sql?.stationCoords||{};
    const init=String(anchorStation||survey?.sql?.survey?.initStation||Object.keys(coords)[0]||'');
    const anchor=coords[init]||{e:0,n:0,z:0};
    const relApply=(x,y)=>{const q=use.apply(x,y);return {e:q.e-num(anchor.e),n:q.n-num(anchor.n)}};
    const features=[];
    for(const a of survey.plan.areas||[]){const pts=samplePts(a.pts,relApply);if(pts.length>=3){if(pts[0][0]!==pts[pts.length-1][0]||pts[0][1]!==pts[pts.length-1][1])pts.push([...pts[0]]);features.push(feature({type:'Polygon',coordinates:[pts]},{layer:layerForType(a.type,'area'),kind:'area',tdrType:a.type||'area'}));}}
    for(const l of survey.plan.lines||[]){const pts=samplePts(l.pts,relApply);if(pts.length>=2)features.push(feature({type:'LineString',coordinates:pts},{layer:layerForType(l.type,'line'),kind:'line',tdrType:l.type||'line',closed:!!l.closed}));}
    for(const p of survey.plan.points||[]){const q=relApply(num(p.x)/20,num(p.y)/20);features.push(feature({type:'Point',coordinates:[q.e,q.n]},{layer:layerForType(p.type,'point'),kind:'point',tdrType:p.type||'point',orientation:num(p.orientation),scale:num(p.scale),text:p.text||''}));}
    const tdrByName=new Map();(survey.plan.stations||[]).forEach(s=>stationAliases(s.name).forEach(a=>{if(!tdrByName.has(a))tdrByName.set(a,s)}));
    for(const leg of survey?.sql?.legs||[]){let fs=null,ts=null;for(const a of stationAliases(leg.fStation)){if(tdrByName.has(a)){fs=tdrByName.get(a);break}}for(const a of stationAliases(leg.tStation)){if(tdrByName.has(a)){ts=tdrByName.get(a);break}}if(!fs||!ts)continue;const A=relApply(num(fs.x)/20,num(fs.y)/20),B=relApply(num(ts.x)/20,num(ts.y)/20);features.push(feature({type:'LineString',coordinates:[[A.e,A.n],[B.e,B.n]]},{layer:'centerline',kind:'centerline',from:leg.fStation,to:leg.tStation}));}
    for(const st of survey.plan.stations||[]){const q=relApply(num(st.x)/20,num(st.y)/20);features.push(feature({type:'Point',coordinates:[q.e,q.n]},{layer:'stations',kind:'station',name:String(st.name||'')}));}
    return {type:'FeatureCollection',features};
  }
  function correctLocal(e,n,opts){
    const scale=num(opts?.scale_factor,1),ang=num(opts?.rotation_deg,0)*DEG;const c=Math.cos(ang),s=Math.sin(ang);
    const E=scale*(num(e)*c+num(n)*s)+num(opts?.offset_e_m);const N=scale*(-num(e)*s+num(n)*c)+num(opts?.offset_n_m);return {e:E,n:N};
  }
  function localToLatLng(e,n,anchorLat,anchorLon){
    const lat0=num(anchorLat),lon0=num(anchorLon);const cos=Math.max(0.01,Math.cos(lat0*DEG));
    return [lat0+num(n)/EARTH_M_PER_DEG,lon0+num(e)/(EARTH_M_PER_DEG*cos)];
  }
  function toGeoJSON(record,visibilityOverride){
    const fc=record?.local_geometry||{type:'FeatureCollection',features:[]};const vis=Object.assign({},DEFAULT_LAYERS,record?.layer_visibility||{},visibilityOverride||{});const out=[];
    for(const f of fc.features||[]){const layer=f?.properties?.layer||'details';if(vis[layer]===false)continue;let g=f.geometry;if(!g)continue;
      const conv=p=>{const c=correctLocal(p[0],p[1],record);const ll=localToLatLng(c.e,c.n,record.anchor_lat,record.anchor_lon);return [ll[1],ll[0]]};
      let geometry=null;if(g.type==='Point')geometry={type:'Point',coordinates:conv(g.coordinates)};else if(g.type==='LineString')geometry={type:'LineString',coordinates:(g.coordinates||[]).map(conv)};else if(g.type==='Polygon')geometry={type:'Polygon',coordinates:(g.coordinates||[]).map(r=>(r||[]).map(conv))};if(geometry)out.push(feature(geometry,Object.assign({},f.properties,{georefId:record.id,objectId:record.object_id})));
    }
    return {type:'FeatureCollection',features:out};
  }
  function boundsGeoJSON(fc){let minLat=90,maxLat=-90,minLon=180,maxLon=-180;(fc?.features||[]).forEach(f=>{const walk=c=>{if(!Array.isArray(c))return;if(typeof c[0]==='number'&&typeof c[1]==='number'){minLon=Math.min(minLon,c[0]);maxLon=Math.max(maxLon,c[0]);minLat=Math.min(minLat,c[1]);maxLat=Math.max(maxLat,c[1]);}else c.forEach(walk)};walk(f.geometry?.coordinates)});return minLat<=maxLat?[[minLat,minLon],[maxLat,maxLon]]:null}
  async function surveyKey(survey){
    const legs=(survey?.sql?.legs||[]).map(l=>[l.fStation,l.tStation,num(l.distance),num(l.bearing),num(l.clino)].join('|')).sort();
    const raw=[survey?.name||'',survey?.date||'',...legs].join('\n');
    if(globalThis.crypto?.subtle){const buf=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(raw));return [...new Uint8Array(buf)].map(b=>b.toString(16).padStart(2,'0')).join('');}
    let h=2166136261;for(let i=0;i<raw.length;i++){h^=raw.charCodeAt(i);h=Math.imul(h,16777619)}return 'fnv-'+(h>>>0).toString(16);
  }
  function metersExtent(fc){let minE=Infinity,maxE=-Infinity,minN=Infinity,maxN=-Infinity;const walk=c=>{if(!Array.isArray(c))return;if(typeof c[0]==='number'&&typeof c[1]==='number'){minE=Math.min(minE,c[0]);maxE=Math.max(maxE,c[0]);minN=Math.min(minN,c[1]);maxN=Math.max(maxN,c[1]);}else c.forEach(walk)};(fc?.features||[]).forEach(f=>walk(f.geometry?.coordinates));return Number.isFinite(minE)?{minE,maxE,minN,maxN,width:maxE-minE,height:maxN-minN}:null}

  window.SOVDrawingGeo={DEFAULT_LAYERS,normStation,matchStations,deriveAutoTransform,buildLocalGeometry,correctLocal,localToLatLng,toGeoJSON,boundsGeoJSON,surveyKey,metersExtent};
})();
