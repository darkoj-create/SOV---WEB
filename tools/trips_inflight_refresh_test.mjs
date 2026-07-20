#!/usr/bin/env node
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source=fs.readFileSync(new URL('../assets/sov-trips-cloud.js',import.meta.url),'utf8');
const storage=new Map();
let rpcCalls=0;
let resolveStale;

const client={
  rpc(name){
    assert.equal(name,'sov_list_trips_feed');
    rpcCalls+=1;
    if(rpcCalls===1){
      return new Promise(resolve=>{
        resolveStale=()=>resolve({data:[{id:'stale',start_date:'2026-07-01'}],error:null});
      });
    }
    return Promise.resolve({data:[{id:'fresh',start_date:'2026-07-02'}],error:null});
  },
  from(){
    throw new Error('Fallback table must not be called in this regression test.');
  },
};

const context={
  console,
  setTimeout,
  clearTimeout,
  Date,
  JSON,
  Promise,
  localStorage:{
    getItem:key=>storage.get(key)??null,
    setItem:(key,value)=>storage.set(key,String(value)),
    removeItem:key=>storage.delete(key),
  },
};
context.window={
  SOVAuth:{
    getClient:()=>client,
    requireApproved:async()=>true,
  },
};
context.globalThis=context;

vm.runInNewContext(source,context,{filename:'assets/sov-trips-cloud.js'});
const api=context.window.SOVTripsCloud;
assert.ok(api&&typeof api.listTrips==='function','SOVTripsCloud.listTrips missing');

const stalePromise=api.listTrips();
await new Promise(resolve=>setImmediate(resolve));
assert.equal(rpcCalls,1,'initial request was not started');

const freshRows=await api.listTrips({force:true});
assert.equal(rpcCalls,2,'force refresh reused the old in-flight request');
assert.equal(freshRows[0]?.id,'fresh','force refresh did not return fresh rows');

resolveStale();
const staleRows=await stalePromise;
assert.equal(staleRows[0]?.id,'stale','initial request fixture did not resolve as stale');

const cacheEntries=[...storage.entries()].filter(([key])=>key.includes('sov_trips_cloud_cache'));
assert.ok(cacheEntries.length,'Trips cache was not written');
const newest=JSON.parse(cacheEntries.find(([key])=>key.includes('v6_1_45aw'))?.[1]||'{}');
assert.equal(newest.rows?.[0]?.id,'fresh','older in-flight response overwrote the fresh cache');

console.log('SOV TRIPS IN-FLIGHT REFRESH TEST PASSED: force request bypassed stale in-flight work and stale response could not overwrite cache.');
