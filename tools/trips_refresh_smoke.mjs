#!/usr/bin/env node
import { chromium } from 'playwright';

const BASE=process.env.SOV_AUDIT_BASE||'http://127.0.0.1:4173';
const browser=await chromium.launch({headless:true});
const context=await browser.newContext({
  viewport:{width:390,height:844},
  isMobile:true,
  hasTouch:true,
});
const page=await context.newPage();
const failures=[];
page.on('pageerror',error=>failures.push('pageerror: '+String(error?.message||error)));
await page.goto(`${BASE}/tools/fixtures/trips-refresh-harness.html`,{waitUntil:'domcontentloaded',timeout:15000});
await page.waitForFunction(()=>window.SOVTripAssetsManager&&typeof window.SOVTripAssetsManager.requestTripsRefresh==='function');

// Button regression: must bypass stale normal listTrips and call the fresh RPC.
await page.click('#refreshBtn');
await page.waitForFunction(()=>window.__loadCalls===1&&window.__rpcCalls===1&&window.__lastRows?.[0]?.id==='fresh');
let state=await page.evaluate(()=>({
  loadCalls:window.__loadCalls,
  rpcCalls:window.__rpcCalls,
  lastRows:window.__lastRows,
  disabled:document.getElementById('refreshBtn').disabled,
  busy:document.getElementById('refreshBtn').getAttribute('aria-busy'),
  toasts:window.__toastMessages,
  savedRows:window.__savedRows,
}));
if(state.loadCalls!==1)failures.push(`button loadCalls=${state.loadCalls}`);
if(state.rpcCalls!==1)failures.push(`button rpcCalls=${state.rpcCalls}`);
if(state.lastRows?.[0]?.id!=='fresh')failures.push('button returned stale rows');
if(state.disabled||state.busy)failures.push('button remained busy/disabled');
if(!state.toasts.some(x=>x.includes('Izleti osvježeni')))failures.push('button success toast missing');
if(state.savedRows?.[0]?.id!=='fresh')failures.push('fresh rows were not cached');

// Pull-to-refresh regression: a top-of-page pull over the threshold must trigger
// the same hard-refresh path and must not reuse a stale request.
await page.evaluate(()=>{
  window.__lastRows=[];
  const makeTouch=y=>new Touch({identifier:7,target:document.body,clientX:120,clientY:y,pageX:120,pageY:y,screenX:120,screenY:y,radiusX:2,radiusY:2,rotationAngle:0,force:0.5});
  const start=makeTouch(20);
  document.dispatchEvent(new TouchEvent('touchstart',{touches:[start],targetTouches:[start],changedTouches:[start],bubbles:true,cancelable:true}));
  const move=makeTouch(130);
  document.dispatchEvent(new TouchEvent('touchmove',{touches:[move],targetTouches:[move],changedTouches:[move],bubbles:true,cancelable:true}));
  document.dispatchEvent(new TouchEvent('touchend',{touches:[],targetTouches:[],changedTouches:[move],bubbles:true,cancelable:true}));
});
await page.waitForFunction(()=>window.__loadCalls===2&&window.__rpcCalls===2&&window.__lastRows?.[0]?.id==='fresh',{timeout:5000});
state=await page.evaluate(()=>({
  loadCalls:window.__loadCalls,
  rpcCalls:window.__rpcCalls,
  lastRows:window.__lastRows,
  indicator:document.getElementById('sovTripsPullIndicator')?.textContent||'',
  toasts:window.__toastMessages,
}));
if(state.loadCalls!==2)failures.push(`pull loadCalls=${state.loadCalls}`);
if(state.rpcCalls!==2)failures.push(`pull rpcCalls=${state.rpcCalls}`);
if(state.lastRows?.[0]?.id!=='fresh')failures.push('pull returned stale rows');
if(!state.toasts.some(x=>x.includes('povlačenjem')))failures.push('pull success toast missing');

await browser.close();
if(failures.length){
  console.error('SOV TRIPS REFRESH SMOKE FAILED');
  failures.forEach(x=>console.error('- '+x));
  process.exit(1);
}
console.log('SOV TRIPS REFRESH SMOKE PASSED: button and pull-to-refresh both fetched fresh RPC rows.');
