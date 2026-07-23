import { chromium } from 'playwright';
import fs from 'node:fs/promises';

const baseUrl=process.env.BASE_URL||'http://127.0.0.1:4173';
const outDir='artifacts/oruzar-master-home';
await fs.mkdir(outDir,{recursive:true});

const catalog=Array.from({length:28},(_,i)=>({
  id:`item-${i+1}`,
  display_name:['Karabiner ovalni','Pojas speleološki','Croll prsni','Bušilica Bosch','Uže 10 mm'][i%5]+` ${i+1}`,
  category_name:['Osobna oprema','Osobna oprema','Osobna oprema','Oprema za proširivanje','Užeta'][i%5],
  subcategory:['Karabineri','Pojasevi','Sprave','Bušilice','Statička užeta'][i%5],
  quantity:4,
  available:3,
  loaned:1,
  status:'aktivno',
  location_name:'Oružarstvo Klaićeva'
}));
const requests=[
  ...Array.from({length:3},(_,i)=>({id:`req-${i}`,status:'requested',user:`Član ${i+1}`})),
  ...Array.from({length:7},(_,i)=>({id:`loan-${i}`,status:'issued',user:`Član ${i+4}`}))
];
const dbMock=`
window.SOVArmoryDB={
  configured:()=>true,
  loadAllData:async()=>({items:${JSON.stringify(catalog)},ropes:[],pieces:[],categories:[]}),
  loadRequests:async()=>${JSON.stringify(requests)}
};`;

async function capture(name,viewport){
  const browser=await chromium.launch({headless:true});
  const page=await browser.newPage({viewport,deviceScaleFactor:1});
  await page.route('https://cdn.jsdelivr.net/**',route=>route.fulfill({status:200,contentType:'application/javascript',body:'window.supabase={createClient:()=>({})};'}));
  await page.route('**/assets/supabase-config.js*',route=>route.fulfill({status:200,contentType:'application/javascript',body:'window.SOV_SUPABASE_URL="";window.SOV_SUPABASE_ANON_KEY="";'}));
  await page.route('**/assets/auth.js*',route=>route.fulfill({status:200,contentType:'application/javascript',body:'window.SOVAuth={};'}));
  await page.route('**/assets/oruzarstvo-supabase.js*',route=>route.fulfill({status:200,contentType:'application/javascript',body:dbMock}));
  await page.goto(`${baseUrl}/oruzar-master.html`,{waitUntil:'networkidle',timeout:60000});
  await page.waitForSelector('.cm-grid-dashboard .cm-card-posudbe',{timeout:30000});
  await page.screenshot({path:`${outDir}/${name}.png`,fullPage:true});
  const report=await page.evaluate(()=>({
    title:document.title,
    bodyBackground:getComputedStyle(document.body).backgroundColor,
    horizontalOverflow:Math.max(0,document.documentElement.scrollWidth-document.documentElement.clientWidth),
    cards:[...document.querySelectorAll('.cm-grid-dashboard .cm-card')].filter(el=>getComputedStyle(el).display!=='none').map(el=>({className:el.className,title:el.querySelector('h2')?.innerText||'',height:Math.round(el.getBoundingClientRect().height)})),
    heroHeight:Math.round(document.querySelector('.cm-hero')?.getBoundingClientRect().height||0)
  }));
  await fs.writeFile(`${outDir}/${name}.json`,JSON.stringify(report,null,2));
  await browser.close();
}

await capture('desktop',{width:1440,height:1000});
await capture('mobile',{width:390,height:844});
