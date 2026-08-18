(function(){
'use strict';
const q=(s,r=document)=>r.querySelector(s),qa=(s,r=document)=>[...r.querySelectorAll(s)];
function name(){return(String(document.getElementById('fTitle')?.value||'nacrt').trim()||'nacrt').replace(/[\\/:*?"<>|]+/g,'_')}
function clean(){
 const live=q('#preview svg');if(!live)return null;const c=live.cloneNode(true),vis=window.SOVNacrtEditor?.getLayerVisibility?.()||{};
 qa('[data-sov-editor-ui],#sov-geometry-preview',c).forEach(e=>e.remove());
 qa('[data-sov-layer]',c).forEach(e=>{if(vis[e.dataset.sovLayer]===false)e.remove();else e.style.display=e.style.display==='none'?'none':''});
 qa('[data-sov-generated-layer]',c).forEach(e=>e.removeAttribute('data-sov-generated-layer'));
 qa('.sov-geometry-selected',c).forEach(e=>e.classList.remove('sov-geometry-selected'));
 return c;
}
function text(){const c=clean();return c?new XMLSerializer().serializeToString(c):''}
function download(blob,filename){const u=URL.createObjectURL(blob),a=document.createElement('a');a.href=u;a.download=filename;document.body.append(a);a.click();setTimeout(()=>{URL.revokeObjectURL(u);a.remove()},200)}
function svg(){const s=text();if(s)download(new Blob([s],{type:'image/svg+xml'}),name()+'.svg')}
function png(){const s=text();if(!s)return;const u=URL.createObjectURL(new Blob([s],{type:'image/svg+xml'})),im=new Image();im.onload=()=>{const live=q('#preview svg'),vb=live?.viewBox?.baseVal,w=vb?.width||1240,h=vb?.height||1754,c=document.createElement('canvas');c.width=w*2;c.height=h*2;const x=c.getContext('2d');x.fillStyle='#fff';x.fillRect(0,0,c.width,c.height);x.drawImage(im,0,0,c.width,c.height);c.toBlob(b=>{if(b)download(b,name()+'.png');URL.revokeObjectURL(u)},'image/png')};im.onerror=()=>URL.revokeObjectURL(u);im.src=u}
async function pdf(){
 const c=clean();if(!c)return;
 try{
  if(!window.jspdf?.jsPDF||typeof window.jspdf.jsPDF.API.svg!=='function')throw Error('PDF modul nije učitan');
  const {jsPDF}=window.jspdf,doc=new jsPDF({orientation:'portrait',unit:'pt',format:'a4',compress:true,putOnlyUsedFonts:true}),W=doc.internal.pageSize.getWidth(),H=doc.internal.pageSize.getHeight();
  await doc.svg(c,{x:0,y:0,width:W,height:H});doc.setProperties({title:name(),subject:'Speleološki nacrt — SOV Velebit',creator:'SOV Nacrt Generator'});doc.save(name()+'.pdf');
 }catch(e){console.error('Geometry PDF export',e)}
}
function click(e){const id=e.target?.closest?.('button')?.id;if(!['btnSvg','btnPng','btnPdf'].includes(id)||!q('#preview svg'))return;e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();if(id==='btnSvg')svg();else if(id==='btnPng')png();else pdf()}
function boot(){document.addEventListener('click',click,true);let n=0,t=setInterval(()=>{const E=window.SOVNacrtEditor;if(E){E.serializeSvg=text;clearInterval(t)}else if(++n>60)clearInterval(t)},100)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot);else boot();
window.SOVNacrtExportGuard={version:'6.1.60',serialize:text};
})();