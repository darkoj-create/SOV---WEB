(function(){
  'use strict';

  const TABLE='sov_arhivar_worklist';
  const FULL_LIST_LIMIT=5000;
  const wrappedClients=new WeakMap();

  function patchTableBuilder(builder){
    if(!builder || typeof builder.select!=='function' || builder.__sovArhivarListPatched) return builder;

    const originalSelect=builder.select.bind(builder);
    builder.select=function(){
      const filter=originalSelect.apply(builder,arguments);
      if(filter && typeof filter.limit==='function' && !filter.__sovArhivarLimitPatched){
        const originalLimit=filter.limit.bind(filter);
        filter.limit=function(count,options){
          const requested=Number(count);
          const effective=Number.isFinite(requested) && requested===1500 ? FULL_LIST_LIMIT : count;
          return originalLimit(effective,options);
        };
        Object.defineProperty(filter,'__sovArhivarLimitPatched',{value:true});
      }
      return filter;
    };

    Object.defineProperty(builder,'__sovArhivarListPatched',{value:true});
    return builder;
  }

  function wrapClient(client){
    if(!client || typeof client.from!=='function') return client;
    if(wrappedClients.has(client)) return wrappedClients.get(client);

    const proxy=new Proxy(client,{
      get(target,prop,receiver){
        if(prop==='from'){
          return function(table){
            const builder=target.from(table);
            return table===TABLE ? patchTableBuilder(builder) : builder;
          };
        }
        const value=Reflect.get(target,prop,receiver);
        return typeof value==='function' ? value.bind(target) : value;
      }
    });

    wrappedClients.set(client,proxy);
    return proxy;
  }

  function install(){
    if(!window.SOVAuth || typeof window.SOVAuth.getClient!=='function') return false;
    if(window.SOVAuth.__arhivarFullListInstalled) return true;

    const originalGetClient=window.SOVAuth.getClient.bind(window.SOVAuth);
    window.SOVAuth.getClient=function(){
      return wrapClient(originalGetClient());
    };

    Object.defineProperty(window.SOVAuth,'__arhivarFullListInstalled',{value:true});
    return true;
  }

  if(!install()) document.addEventListener('DOMContentLoaded',install,{once:true});
})();
