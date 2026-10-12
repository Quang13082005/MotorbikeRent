(() => {
 const keys=['pickup','returnAt','location','deliveryAddress','deliveryLat','deliveryLng','returnLocation','returnAddress','returnLat','returnLng'];
 const storageKey='motorent.booking-context.v1';
 const read=()=>{try{return JSON.parse(sessionStorage.getItem(storageKey)||'{}');}catch{return {};}};
 const save=values=>{
  const state={...read()};for(const k of keys)if(k in values)state[k]=values[k];
  try{sessionStorage.setItem(storageKey,JSON.stringify(state));}catch{}
  const url=new URL(window.location.href);
  for(const k of keys)if(state[k])url.searchParams.set(k,state[k]);else url.searchParams.delete(k);
  history.replaceState(history.state,'',url);
  return state;
 };
 const query=new URLSearchParams(location.search),explicit=Object.fromEntries([...query].filter(([k])=>keys.includes(k)));
 if(Object.keys(explicit).length)save(explicit);
 const get=()=>read();
 const now=()=>new Intl.DateTimeFormat('sv-SE',{timeZone:'Asia/Ho_Chi_Minh',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hourCycle:'h23'}).format(new Date()).replace(' ','T');
 const valid=(pickup,end,required=false)=>{
  if(!pickup&&!end&&!required)return '';
  if(!pickup||!end)return 'Vui lòng chọn đủ ngày giờ nhận và trả xe.';
  if(pickup<now())return 'Ngày nhận xe không được ở quá khứ.';
  if(end<=pickup)return 'Ngày trả xe phải sau ngày nhận xe.';
  return '';
 };
 window.BookingContext={get,save,now,valid,keys};
 document.addEventListener('click',e=>{
  const a=e.target.closest('[data-preserve-query]');if(!a)return;
  const url=new URL(a.href),context=get();for(const k of keys)if(context[k])url.searchParams.set(k,context[k]);a.href=url;
 },true);
})();
