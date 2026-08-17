(function(){
  try{
    const q=new URLSearchParams(location.search||'');
    const h=new URLSearchParams((location.hash||'').replace(/^#/,''));
    const recoveryType=(q.get('type')||h.get('type')||'').toLowerCase()==='recovery';
    const hasRecoveryToken=!!(h.get('access_token')&&recoveryType);
    let pending=false;
    try{pending=localStorage.getItem('sov_password_recovery_pending')==='1'}catch(e){}
    const hasRecoveryCode=!!q.get('code')&&(recoveryType||pending);
    const hasRecoveryError=recoveryType&&(q.get('error')||q.get('error_code')||h.get('error')||h.get('error_code'));
    if(hasRecoveryToken||hasRecoveryCode||hasRecoveryError){
      document.documentElement.style.visibility='hidden';
      const target='reset-password.html'+(location.search||'')+(location.hash||'');
      location.replace(target);
    }
  }catch(e){console.warn('Recovery landing redirect skipped',e)}
})();
