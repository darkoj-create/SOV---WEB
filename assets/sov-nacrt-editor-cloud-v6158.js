(function(){
'use strict';
const $=id=>document.getElementById(id);
const E=()=>window.SOVNacrtEditor;
const P=()=>window.SOVNacrtPlacement;
const SB=()=>window.SOVAuth?.getClient?.();
let currentSurveyToken='';
let installed=false;

const DEFAULT_LAYERS={
  traverse:{visible:true,locked:true},
  walls:{visible:true,locked:true},
  formations:{visible:true,locked:false},
  water:{visible:true,locked:false},
  vegetation:{visible:true,locked:false},
  annotations:{visible:true,locked:false},
  text:{visible:true,locked:false},
  manual:{visible:true,locked:false}
};

function clone(v){return JSON.parse(JSON.stringify(v));}
function slug(v){return String(v||'radni').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'')||'radni';}
function surveyToken(survey,file){return [survey?.name,survey?.date,file?.name].filter(Boolean).join('|')||String(survey?.sql?.survey?.name||'');}
function record(){try{return P()?.getRecord?.()||null}catch(_){return null;}}
function stableKey(r=record()){return `sov:nacrt-editor:cloud-v1:${slug(r?.survey_key||r?.survey_name||$('fTitle')?.value)}`;}
function titleKey(r=record()){return `sov:nacrt-editor:v1:${slug(r?.survey_name||$('fCadNum')?.value||$('fTitle')?.value)}`;}
function blank(){return {schema:'sov-nacrt-editor',version:1,layers:clone(DEFAULT_LAYERS),objects:[]};}
function readLocalDraft(r=record()){
  try{
    for(const key of [stableKey(r),titleKey(r)]){
      const raw=localStorage.getItem(key);
      if(!raw)continue;
      const state=JSON.parse(raw);
      if(state?.schema==='sov-nacrt-editor'&&Array.isArray(state.objects))return state;
    }
  }catch(_){}
  return null;
}
function saveLocalStable(state,r=record()){
  try{localStorage.setItem(stableKey(r),JSON.stringify(state))}catch(_){}
}
function resetEditorForSurvey(survey,file){
  const token=surveyToken(survey,file);
  if(token&&token===currentSurveyToken)return;
  currentSurveyToken=token;
  E()?.setState?.(blank());
}
function wrapSetSurvey(){
  const p=P();
  if(!p?.setSurvey)return false;
  if(p.__sovEditorCloudWrapped)return true;
  const original=p.setSurvey;
  p.setSurvey=function(survey,file){
    resetEditorForSurvey(survey,file);
    return original.apply(this,arguments);
  };
  p.__sovEditorCloudWrapped=true;
  return true;
}
async function cloudSave(){
  try{
    const r=record(),sb=SB(),editor=E();
    if(!r?.object_id||!r?.survey_key||!sb||!editor?.getState)return;
    const state=editor.getState();
    saveLocalStable(state,r);
    const {data:existing,error:readError}=await sb.from('speleo_drawing_georefs').select('metadata').eq('object_id',r.object_id).eq('survey_key',r.survey_key).maybeSingle();
    if(readError)throw readError;
    const metadata={...(existing?.metadata||{}),...(r.metadata||{}),editor_state:state,editor_version:'6.1.58',editor_saved_at:new Date().toISOString()};
    const {error}=await sb.from('speleo_drawing_georefs').update({metadata}).eq('object_id',r.object_id).eq('survey_key',r.survey_key);
    if(error)throw error;
    const st=$('geoStatus');
    if(st&&st.textContent.includes('Spremljeno.'))st.innerHTML='<b>Spremljeno.</b> Položaj i dorade nacrta spremljeni su u cloud.';
  }catch(e){
    console.warn('SOV editor cloud save',e);
    const st=$('geoStatus');
    if(st&&!st.querySelector('.sov-editor-cloud-error'))st.insertAdjacentHTML('beforeend',' <span class="geo-error sov-editor-cloud-error">Dorade nacrta nisu spremljene u cloud.</span>');
  }
}
async function cloudLoad(){
  try{
    const r=record(),sb=SB(),editor=E();
    if(!r?.object_id||!r?.survey_key||!sb||!editor?.getState||!editor?.setState)return;
    const current=editor.getState();
    if((current?.objects?.length||0)>0)return;
    const local=readLocalDraft(r);
    if(local){editor.setState(local);saveLocalStable(local,r);return;}
    const {data,error}=await sb.from('speleo_drawing_georefs').select('metadata').eq('object_id',r.object_id).eq('survey_key',r.survey_key).maybeSingle();
    if(error)throw error;
    const state=data?.metadata?.editor_state;
    if(state?.schema!=='sov-nacrt-editor'||!Array.isArray(state.objects))return;
    editor.setState(state);
    saveLocalStable(state,r);
  }catch(e){console.warn('SOV editor cloud load',e);}
}
function watchPlacement(){
  const save=$('geoSave'),status=$('geoStatus');
  if(!save||!status)return false;
  if(status.dataset.sovEditorCloudWatch==='1')return true;
  status.dataset.sovEditorCloudWatch='1';
  let saveRequested=false,loading=false;
  save.addEventListener('click',()=>{saveRequested=true;});
  new MutationObserver(()=>{
    const text=status.textContent||'';
    if(saveRequested&&text.includes('Spremljeno.')){saveRequested=false;cloudSave();}
    if(!loading&&text.includes('Postojeći položaj je učitan.')){loading=true;cloudLoad().finally(()=>{loading=false;});}
  }).observe(status,{childList:true,subtree:true,characterData:true});
  return true;
}
function install(){
  if(installed)return;
  if(!window.SOVNacrtEditor||!window.SOVNacrtPlacement||!window.SOVAuth)return;
  const wrapped=wrapSetSurvey(),watched=watchPlacement();
  if(wrapped&&watched)installed=true;
}
function boot(){
  install();
  if(!installed){let n=0;const timer=setInterval(()=>{install();if(installed||++n>80)clearInterval(timer);},100);}
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot);else boot();
window.SOVNacrtEditorCloud={version:'6.1.58',save:cloudSave,load:cloudLoad};
})();
