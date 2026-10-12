(() => {
 const original=window.fetch.bind(window);
 window.fetch=(input,options={})=>{
  const url=new URL(typeof input==='string'?input:input.url,location.href);
  const method=(options.method || input.method || 'GET').toUpperCase();
  if(url.origin===location.origin && !['GET','HEAD','OPTIONS'].includes(method)){
   const token=document.querySelector('meta[name="csrf-token"]')?.content;
   if(token){const headers=new Headers(options.headers || input.headers);headers.set('X-XSRF-TOKEN',token);options={...options,headers};}
  }
  return original(input,options);
 };
})();
