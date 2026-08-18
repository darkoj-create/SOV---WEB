(function(){
'use strict';
function safeName(s){return (String(s||'nacrt').normalize('NFC').replace(/[\\/:*?"<>|]+/g,'_').trim()||'nacrt')+'.pdf'}
function status(msg,type){const el=document.getElementById('status');if(!el)return;el.className='status '+(type||'info');el.textContent=msg}
async function exportPdf(){
 const svg=document.querySelector('#preview svg');if(!svg){status('Prvo izradi nacrt.','error');return}
 const btn=document.getElementById('btnPdf');if(btn){btn.disabled=true;btn.textContent='PDF…'}
 try{
  if(!window.jspdf?.jsPDF||typeof window.jspdf.jsPDF.API.svg!=='function')throw new Error('PDF modul nije učitan');
  const {jsPDF}=window.jspdf;const doc=new jsPDF({orientation:'portrait',unit:'pt',format:'a4',compress:true,putOnlyUsedFonts:true});
  const W=doc.internal.pageSize.getWidth(),H=doc.internal.pageSize.getHeight();
  await doc.svg(svg,{x:0,y:0,width:W,height:H});
  const title=document.getElementById('fTitle')?.value||'nacrt';
  doc.setProperties({title,subject:'Speleološki nacrt — SOV Velebit',creator:'SOV Nacrt Generator'});
  doc.save(safeName(title));status('PDF je izvezen kao vektorski dokument.','ok');
 }catch(err){console.error('PDF export',err);status('PDF export nije uspio; otvoren je ispis kao sigurni fallback.','error');printFallback(svg)}finally{if(btn){btn.disabled=false;btn.textContent='PDF'}}
}
function printFallback(svg){
 const w=window.open('','_blank','noopener,noreferrer');if(!w)return;
 w.document.write('<!doctype html><html><head><title>Nacrt PDF</title><style>@page{size:A4 portrait;margin:0}html,body{margin:0;width:100%;height:100%}svg{display:block;width:210mm!important;height:297mm!important}</style></head><body>'+svg.outerHTML+'<script>onload=()=>setTimeout(()=>print(),80)<\/script></body></html>');w.document.close();
}
function inject(){if(document.getElementById('btnPdf'))return;const row=document.querySelector('.btn-row');if(!row)return;const b=document.createElement('button');b.type='button';b.id='btnPdf';b.className='btn btn-secondary';b.textContent='PDF';b.title='Vektorski PDF za arhivu i ispis';b.addEventListener('click',exportPdf);row.appendChild(b)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',inject);else inject();
window.SOVNacrtPdf={export:exportPdf};
})();
