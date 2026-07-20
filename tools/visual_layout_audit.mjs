#!/usr/bin/env node
import {chromium} from 'playwright';
import fs from 'node:fs';
import path from 'node:path';

const BASE=process.env.SOV_AUDIT_BASE||'http://127.0.0.1:4173';
const pages=['index.html','dashboard.html','izleti-cloud.html','zapisnici-native.html','zapisnici-najave.html','system-status.html','dokumenti.html'];
const viewports=[
  {name:'desktop',width:1440,height:1000},
  {name:'mobile',width:390,height:844},
];
const banned=[/Živi zapisnici/i,/Live admin dashboard/i,/observability v\d/i,/crash reporti/i];
const outDir=path.resolve('VISUAL_AUDIT');
fs.mkdirSync(outDir,{recursive:true});
const failures=[];
const results=[];
const browser=await chromium.launch({headless:true});

for(const viewport of viewports){
  const context=await browser.newContext({viewport:{width:viewport.width,height:viewport.height},javaScriptEnabled:false});
  for(const file of pages){
    const page=await context.newPage();
    const response=await page.goto(`${BASE}/${file}`,{waitUntil:'domcontentloaded',timeout:20000});
    const status=response?.status()||0;
    const source=await page.content();
    const text=(await page.locator('body').innerText().catch(()=>''))||'';
    const layout=await page.evaluate(()=>{
      const root=document.documentElement;
      const body=document.body;
      const bad=[];
      for(const el of document.querySelectorAll('body *')){
        const style=getComputedStyle(el);
        if(style.display==='none'||style.visibility==='hidden'||Number(style.opacity)===0)continue;
        const r=el.getBoundingClientRect();
        if(r.width<2||r.height<2)continue;
        if(r.right>innerWidth+4||r.left<-4){
          const tag=el.tagName.toLowerCase();
          const id=el.id?`#${el.id}`:'';
          const cls=el.classList?.length?'.'+[...el.classList].slice(0,2).join('.'):'';
          bad.push(`${tag}${id}${cls} [${Math.round(r.left)},${Math.round(r.right)}]`);
          if(bad.length>=12)break;
        }
      }
      return{scrollWidth:Math.max(root.scrollWidth,body?.scrollWidth||0),innerWidth,overflow:bad};
    });
    const name=`${file.replace(/\.html?$/,'')}-${viewport.name}`;
    await page.screenshot({path:path.join(outDir,`${name}.png`),fullPage:true});
    const item={file,viewport:viewport.name,status,...layout};
    results.push(item);
    if(status>=400)failures.push(`${name}: HTTP ${status}`);
    if(layout.scrollWidth>layout.innerWidth+4)failures.push(`${name}: horizontal overflow ${layout.scrollWidth}px > ${layout.innerWidth}px; ${layout.overflow.join(', ')}`);
    if(source.includes('�')||text.includes('�'))failures.push(`${name}: replacement character detected`);
    for(const pattern of banned){if(pattern.test(text)||pattern.test(source))failures.push(`${name}: banned copy ${pattern}`)}
    await page.close();
  }
  await context.close();
}
await browser.close();
fs.writeFileSync(path.join(outDir,'report.json'),JSON.stringify({generatedAt:new Date().toISOString(),results,failures},null,2));
fs.writeFileSync(path.join(outDir,'report.md'),`# SOV visual layout audit\n\nPages: ${pages.length}\nViewports: ${viewports.length}\nFailures: ${failures.length}\n\n${failures.length?failures.map(x=>`- ${x}`).join('\n'):'All key pages passed desktop/mobile overflow, broken-character and copy checks.'}\n`);
if(failures.length){
  console.error('SOV VISUAL AUDIT FAILED');
  failures.forEach(x=>console.error('- '+x));
  process.exit(1);
}
console.log(`SOV VISUAL AUDIT PASSED: ${pages.length} pages × ${viewports.length} viewports.`);
