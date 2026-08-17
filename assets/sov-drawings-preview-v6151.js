(function(){
  'use strict';
  const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const attr=s=>esc(s).replace(/`/g,'&#96;');
  function nameOf(d){return String(d&& (d.fileName||d.drive_file_name||d.name||d.title||d.drawing_title)||'Nacrt');}
  function mimeOf(d){return String(d&& (d.mimeType||d.mime_type||d.type)||'').toLowerCase();}
  function idFromUrl(v){const s=String(v||'');let m=s.match(/\/d\/([a-zA-Z0-9_-]{10,})/);if(m)return m[1];m=s.match(/[?&]id=([a-zA-Z0-9_-]{10,})/);return m?m[1]:'';}
  function fileId(d){
    if(!d)return '';
    const direct=d.drive_file_id||d.driveFileId||d.fileId||d.file_id||d.googleDriveId||d.google_drive_id||d.id;
    if(direct && /^[a-zA-Z0-9_-]{10,}$/.test(String(direct)))return String(direct);
    for(const k of ['webViewUrl','webViewLink','web_view_link','previewUrl','preview_url','downloadUrl','download_url','thumbnailUrl','thumbnail_url','thumbnailLink','url']){
      const id=idFromUrl(d[k]);if(id)return id;
    }
    return '';
  }
  function extOf(d){const n=nameOf(d).toLowerCase();const m=n.match(/\.([a-z0-9]{2,5})$/);return m?m[1]:'';}
  function isImage(d){return /^(jpg|jpeg|png|webp|gif|svg)$/.test(extOf(d))||mimeOf(d).startsWith('image/');}
  function downloadOf(d){return String(d&& (d.downloadUrl||d.download_url||d.webContentLink||d.web_content_link)||'');}
  function directThumb(d){return String(d&& (d.thumbnailUrl||d.thumbnail_url||d.thumbnailLink||d.thumbUrl||d.thumb_url)||'');}
  function directPreview(d){return String(d&& (d.previewUrl||d.preview_url||d.webViewUrl||d.webViewLink||d.web_view_link||d.downloadUrl||d.download_url||d.webContentLink||d.url)||'');}
  function driveThumb(id,size){return id?'https://drive.google.com/thumbnail?id='+encodeURIComponent(id)+'&sz='+size:'';}
  function thumbUrl(d,large){
    const dl=downloadOf(d);
    if(isImage(d)&&dl)return dl;
    const t=directThumb(d);if(t)return t;
    const id=fileId(d);
    if(id)return driveThumb(id,large?'w2000':'w900');
    if(isImage(d))return directPreview(d);
    return '';
  }
  function previewUrl(d){
    const id=fileId(d),dl=downloadOf(d);
    if(isImage(d)&&dl)return {url:dl,kind:'image'};
    if(id){
      if(isImage(d))return {url:driveThumb(id,'w2400'),kind:'image'};
      return {url:'https://drive.google.com/file/d/'+encodeURIComponent(id)+'/preview',kind:'frame'};
    }
    const p=directPreview(d);
    if(!p)return {url:'',kind:'none'};
    return {url:p,kind:isImage(d)?'image':'frame'};
  }
  function typeLabel(d){const n=nameOf(d).toLowerCase(),m=n.match(/\.([a-z0-9]{2,5})$/);if(m)return m[1].toUpperCase();const mt=mimeOf(d);if(mt.includes('/'))return mt.split('/').pop().toUpperCase();return 'NACRT';}

  window.renderDrawings=function(arr){
    if(!Array.isArray(arr)||!arr.length)return '';
    return '<div class="sectionTitle">Nacrti</div><div class="drawingList">'+arr.slice(0,12).map((d,i)=>{
      const n=nameOf(d), th=thumbUrl(d,false), type=typeLabel(d);
      const visual=th
        ? '<img src="'+attr(th)+'" loading="lazy" alt="'+attr(n)+'" onerror="this.style.display=\'none\';this.nextElementSibling.style.display=\'grid\'">'
          +'<span class="drawingVisualFallback" style="display:none">'+esc(type)+'</span>'
        : '<span class="drawingVisualFallback">'+esc(type)+'</span>';
      return '<button type="button" class="drawingVisual" onclick="openPreviewForSelected('+i+')">'
        +'<span class="drawingVisualFrame">'+visual+'<span class="drawingVisualType">'+esc(type)+'</span></span>'
        +'<span class="drawingVisualMeta"><b>'+esc(n)+'</b><small>Klikni za veliki pregled</small></span>'
        +'</button>';
    }).join('')+'</div>';
  };

  window.openPreviewForSelected=function(i){
    try{
      const arr=selected?drawingsForObject(selected):[];
      const d=arr&&arr[i];if(!d)return;
      const title=nameOf(d), p=previewUrl(d);
      const titleEl=document.getElementById('previewTitle'),body=document.getElementById('previewBody'),overlay=document.getElementById('previewOverlay');
      if(titleEl)titleEl.textContent=title;
      if(body){
        if(!p.url)body.innerHTML='<p class="meta">Pregled nije dostupan za ovu datoteku.</p>';
        else if(p.kind==='image')body.innerHTML='<img src="'+attr(p.url)+'" alt="'+attr(title)+'">';
        else body.innerHTML='<iframe src="'+attr(p.url)+'" title="'+attr(title)+'" allow="fullscreen"></iframe>';
      }
      if(overlay)overlay.classList.add('show');
    }catch(e){console.warn('[SOV nacrti] preview nije otvoren',e);}
  };

  window.SOVDrawingPreview={fileId,thumbUrl,previewUrl,nameOf,typeLabel};
})();
