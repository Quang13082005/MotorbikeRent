(() => {
 'use strict';
 const message=document.getElementById('cw-message');
 const tell=text=>{if(message){message.textContent=text;message.scrollIntoView({block:'nearest',behavior:'smooth'});}};
 async function api(url,options={}) {
  const headers=new Headers(options.headers);
  const csrf=document.querySelector('meta[name="csrf-token"]')?.content;
  if(csrf)headers.set('X-XSRF-TOKEN',csrf);
  const res=await fetch(url,{credentials:'same-origin',...options,headers});
  const data=await res.json().catch(()=>({message:'Máy chủ trả về dữ liệu không hợp lệ'}));
  if(!res.ok)throw new Error(data.message || data.error || 'Không thể xử lý yêu cầu');return data;
 }
 async function busy(button,action) {button.disabled=true;try{await action();}catch(e){tell(e.message);}finally{button.disabled=false;}}
 const json=body=>({headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
 document.querySelectorAll('[data-action]').forEach(button=>button.addEventListener('click',()=>busy(button,async()=>{
  const id=button.dataset.id,action=button.dataset.action;
  if(action==='pay-extension'){const data=await api('/api/payment/extension/'+id,{method:'POST'});location.assign(data.checkoutUrl);}
  if(action==='pay'){const data=await api('/api/payment/create/'+id,{method:'POST'});location.assign(data.checkoutUrl);}
  if(action==='early-return'){
   const reason=prompt('Nhập lý do trả xe sớm (hoặc để trống):');
   if(reason!==null){
    await api('/api/rental-orders/'+id+'/early-return',{method:'POST',...json({reason:reason.trim()})});
    tell('Đã gửi yêu cầu trả xe sớm. Vui lòng liên hệ hotline cửa hàng để xác nhận việc trả xe sớm và chính sách hoàn tiền.');
    setTimeout(()=>location.reload(),1000);
   }
  }
  if(action==='cancel' && confirm('Hủy đơn thuê này? Khoản đã thanh toán sẽ được cửa hàng đối soát.')){await api('/api/rental-orders/'+id+'/cancel',{method:'POST'});location.reload();}
  if(action==='reconcile'){await api('/api/payment/reconcile/'+id,{method:'POST'});location.reload();}
 })));
 const forms={
  'account-form':async(f,data)=>{data.gender=data.gender===''?null:data.gender==='true';data.birthDate=data.birthDate||null;await api('/api/account',{method:'PUT',...json(data)});tell('Đã lưu thông tin cá nhân.');},
  'password-form':async(f,data)=>{const response=await api('/api/account/password',{method:'POST',...json(data)});tell(response.message);localStorage.removeItem('token');document.cookie='access_token=; Max-Age=0; path=/';setTimeout(()=>location.assign('/auth/login'),1200);},
  'avatar-form':async(f)=>{await api('/api/account/avatar',{method:'POST',body:new FormData(f)});location.reload();},
  'extension-form':async(f,data)=>{await api('/api/rental-orders/'+f.dataset.id+'/extensions',{method:'POST',...json(data)});location.assign('/customer/rental-detail/'+f.dataset.id);},
  'review-form':async(f,data)=>{data.rating=Number(data.rating);data.orderId=Number(f.dataset.id);await api('/api/reviews',{method:'POST',...json(data)});location.assign('/customer/rental-detail/'+f.dataset.id);}
 };
 Object.entries(forms).forEach(([id,handler])=>{const form=document.getElementById(id);form?.addEventListener('submit',e=>{e.preventDefault();busy(form.querySelector('button'),()=>handler(form,Object.fromEntries(new FormData(form))));});});
 document.querySelectorAll('[data-gallery]').forEach(b=>b.addEventListener('click',()=>{document.getElementById('bike-image').src=b.querySelector('img').src;}));
 document.querySelectorAll('[data-preserve-query]').forEach(a=>{const url=new URL(a.href);for(const [k,v] of new URLSearchParams(location.search))url.searchParams.set(k,v);a.href=url;});
})();
