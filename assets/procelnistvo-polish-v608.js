
(function(){
  const entries=[...document.querySelectorAll('.timeline .entry[data-year]')];
  if(!entries.length)return;
  const host=document.querySelector('.timeline');
  const tools=document.createElement('div');tools.className='procelnistvo-tools-v608';
  tools.innerHTML='<input id="prSearch608" type="search" placeholder="Pretraži osobu ili funkciju…"><select id="prYear608"><option value="">Sve godine</option></select><button id="prClear608" type="button">Reset</button>';
  const decades=document.createElement('div');decades.className='decade-strip-v608';
  const count=document.createElement('div');count.className='procelnistvo-count-v608';
  host.parentNode.insertBefore(tools,host);host.parentNode.insertBefore(decades,host);host.parentNode.insertBefore(count,host);
  const years=entries.map(e=>+e.dataset.year).filter(Boolean).sort((a,b)=>b-a);
  const sel=tools.querySelector('#prYear608');years.forEach(y=>{const o=document.createElement('option');o.value=y;o.textContent=y;sel.appendChild(o)});
  const decs=[...new Set(years.map(y=>Math.floor(y/10)*10))].sort((a,b)=>b-a);
  const all=document.createElement('button');all.textContent='Sve dekade';all.dataset.decade='';all.className='active';decades.appendChild(all);
  decs.forEach(d=>{const b=document.createElement('button');b.textContent=d+'-e';b.dataset.decade=d;decades.appendChild(b)});
  let activeDecade='';
  function apply(){const q=tools.querySelector('#prSearch608').value.trim().toLowerCase();const y=sel.value;let shown=0;entries.forEach(e=>{const ey=e.dataset.year;const txt=e.textContent.toLowerCase();const okQ=!q||txt.includes(q);const okY=!y||ey===y;const okD=!activeDecade||String(Math.floor(+ey/10)*10)===String(activeDecade);const ok=okQ&&okY&&okD;e.classList.toggle('is-hidden-v608',!ok);if(ok)shown++});count.textContent='Prikazano: '+shown+' od '+entries.length+' godina';}
  tools.addEventListener('input',apply);sel.addEventListener('change',apply);tools.querySelector('#prClear608').addEventListener('click',()=>{tools.querySelector('#prSearch608').value='';sel.value='';activeDecade='';decades.querySelectorAll('button').forEach(b=>b.classList.toggle('active',!b.dataset.decade));apply()});
  decades.addEventListener('click',e=>{const b=e.target.closest('button');if(!b)return;activeDecade=b.dataset.decade;decades.querySelectorAll('button').forEach(x=>x.classList.toggle('active',x===b));apply()});
  apply();
})();
