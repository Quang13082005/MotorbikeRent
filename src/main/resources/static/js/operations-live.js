(() => {
 'use strict';
 const $=s=>document.querySelector(s),error=$('#ops-error');
 const labels={expiresAt:"Hạn thanh toán",approvedAt:"Ngày duyệt",direction:'Thu / chi',processedBy:'Người ghi nhận',processedAt:'Thời điểm ghi nhận',note:'Ghi chú',imageCredit:'Tác giả / ghi chú ảnh',imageSourceUrl:'Nguồn ảnh (HTTPS)',imageLicenseUrl:'Giấy phép ảnh (HTTPS)',lat:'Vĩ độ',lng:'Kinh độ',area:'Khu vực',category:'Nhóm địa điểm',mapsUrl:'Liên kết bản đồ',published:'Hiển thị cho khách',id:'Mã',name:'Tên',licensePlate:'Biển số',brandId:'Hãng',categoryId:'Loại xe',year:'Năm',cc:'Dung tích',pricePerDay:'Giá ngày',depositAmount:'Bảo đảm tài sản',status:'Trạng thái',odoKm:'Công tơ mét',description:'Mô tả',imageUrl:'Ảnh',username:'Tên đăng nhập',firstName:'Tên',lastName:'Họ và tên đệm',email:'Email',phone:'Điện thoại',address:'Địa chỉ',orderCode:'Mã đơn',customer:'Khách hàng',motorbike:'Xe',pickupDate:'Ngày nhận',returnDate:'Ngày trả',totalPrice:'Tổng tiền',paid:'Đã thu',staff:'Người xử lý',motorbikeId:'Xe',type:'Hạng mục',cost:'Chi phí',reportedBy:'Người lập',startedAt:'Bắt đầu',completedAt:'Hoàn tất',code:'Mã ưu đãi',discountType:'Loại giảm',discountValue:'Giá trị giảm',minOrderAmount:'Đơn tối thiểu',maxDiscount:'Giảm tối đa',startDate:'Bắt đầu',endDate:'Kết thúc',usageLimit:'Giới hạn lượt',usedCount:'Đã dùng',amount:'Số tiền',receivedAmount:'Thực nhận',purpose:'Khoản thu',method:'Hình thức',paidAt:'Ngày nhận',reference:'Tham chiếu',newReturnDate:'Ngày trả mới',extraAmount:'Phí thêm',reason:'Lý do',rating:'Số sao',comment:'Nội dung',staffReply:'Phản hồi',createdAt:'Ngày tạo',password:'Mật khẩu (để trống nếu giữ nguyên)'};
 const states={WAITING_FOR_PAYMENT:"Chờ thanh toán gia hạn",EXPIRED:"Hết hạn thanh toán",EXTENSION_PAYMENT:"Thanh toán gia hạn",BOOKING_PAYMENT:'Thanh toán giữ xe',BOOKING_REFUND:'Hoàn tiền đặt xe',BOOKING_ADJUSTMENT_REFUND:'Hoàn tiền chênh lệch',IN:'Thu',OUT:'Chi',OTHER:'Khác',true:'Đang hiển thị',false:'Đang ẩn',PENDING_PAYMENT:'Chờ cọc',PENDING:'Chờ duyệt',CONFIRMED:'Đã xác nhận',READY_FOR_HANDOVER:'Sẵn sàng giao',HANDED_OVER:'Đang thuê',COMPLETED:'Đã hoàn thành',CANCELLED:'Đã hủy',REJECTED:'Từ chối',EXTENDED:'Đã gia hạn',OVERDUE:'Quá hạn',AVAILABLE:'Sẵn sàng',UNAVAILABLE:'Tạm ngưng',RENTED:'Đang thuê',MAINTENANCE:'Bảo dưỡng',ACTIVE:'Hoạt động',BLOCKED:'Đã khóa',INACTIVE:'Ngừng áp dụng',IN_PROGRESS:'Đang bảo dưỡng',PAID:'Đã nhận tiền',REVIEW_REQUIRED:'Cần đối soát',REFUNDED:'Đã hoàn tiền',APPROVED:'Đã duyệt',DEPOSIT:'Cọc thuê',BALANCE:'Tiền thuê còn lại',SECURITY:'Bảo đảm tài sản',SECURITY_REFUND:'Hoàn bảo đảm',DEPOSIT_REFUND:'Hoàn cọc',LATE_REFUND:'Hoàn tiền đến muộn',BANK_TRANSFER:'Chuyển khoản ngân hàng',CASH:'Tiền mặt',PAYOS:'PayOS',PERCENTAGE:'Phần trăm',FIXED:'Số tiền'};
 const money=n=>new Intl.NumberFormat('vi-VN').format(n||0)+' ₫';
 const safe=x=>String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const show=e=>{if(error)error.textContent=e.message||e;};
 async function api(url,options={}){const headers=new Headers(options.headers);const csrf=document.querySelector('meta[name="csrf-token"]')?.content;if(csrf)headers.set('X-XSRF-TOKEN',csrf);const r=await fetch(url,{credentials:'same-origin',...options,headers});const d=await r.json().catch(()=>({message:'Máy chủ không trả về dữ liệu hợp lệ'}));if(!r.ok)throw new Error(d.message||'Không thực hiện được yêu cầu');return d;}
 const json=data=>({headers:{'Content-Type':'application/json'},body:JSON.stringify(data)});
 function download(filename,columns,rows){const cell=v=>'"'+String(v??'').replace(/^[=+@-]/,"'$&").replace(/"/g,'""')+'"';const blob=new Blob(['\ufeff'+[columns.map(k=>labels[k]||k),...rows.map(r=>columns.map(k=>r[k]))].map(r=>r.map(cell).join(',')).join('\r\n')],{type:'text/csv;charset=utf-8'});const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=filename;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
 function format(k,v){if(v==null || v==='')return '—';if(['pricePerDay','depositAmount','totalPrice','paid','cost','amount','receivedAmount','extraAmount','minOrderAmount','maxDiscount'].includes(k))return money(v);if(/Date$|At$/.test(k))return new Date(v).toLocaleString('vi-VN');return states[v]||v;}
 const numberKeys=['year','cc','pricePerDay','depositAmount','odoKm','cost','discountValue','minOrderAmount','maxDiscount','usageLimit'];
 const configs={
  'travel-places':{fields:['name','area','category','address','description','phone','imageUrl','imageCredit','imageSourceUrl','imageLicenseUrl','mapsUrl','lat','lng','published'],columns:['name','area','address','published'],select:{published:['false','true']}},
  motorbikes:{fields:['name','licensePlate','brandId','categoryId','year','cc','pricePerDay','depositAmount','odoKm','description','imageUrl','status'],columns:['name','licensePlate','pricePerDay','status','odoKm'],select:{brandId:'brands',categoryId:'categories',status:['AVAILABLE','UNAVAILABLE']}},
  customers:{fields:['username','firstName','lastName','email','phone','address','password','status'],columns:['username','firstName','lastName','phone','email','status'],select:{status:['ACTIVE','BLOCKED']}},
  staffs:{fields:['username','firstName','lastName','email','phone','address','password','status'],columns:['username','firstName','lastName','phone','email','status'],select:{status:['ACTIVE','BLOCKED']}},
  maintenance:{fields:['motorbikeId','type','description','cost','status'],columns:['motorbike','type','cost','status','reportedBy'],select:{motorbikeId:'bikes',status:['IN_PROGRESS','COMPLETED']}},
  'rental-orders':{columns:['orderCode','customer','phone','motorbike','pickupDate','returnDate','totalPrice','paid','status']},
  payments:{columns:['orderCode','amount','receivedAmount','purpose','method','status','paidAt','direction','processedBy','processedAt','reference']},
  extensions:{columns:['orderCode','customer','newReturnDate','extraAmount','reason','status','expiresAt']},
  reviews:{fields:['staffReply'],columns:['motorbike','customer','rating','comment','staffReply','createdAt'],noCreate:true}
 };
 let module=$('[data-live-module]')?.dataset.liveModule,rows=[],lookups={},editing=null,placePage=0,placeTotal=0,placePages=0;
 const visible=()=>module==='travel-places'?rows:rows.filter(r=>Object.values(r).join(' ').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().includes(($('#live-search')?.value||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase()));
 const btn=(action,label,id)=>`<button type="button" class="ops-btn" data-live-action="${action}" data-id="${safe(id)}">${label}</button>`;
 function actions(r){
  if(configs[module].fields)return btn('edit','Chỉnh sửa',r.id);
  if(module==='extensions' && r.status==='PENDING')return btn('approve','Duyệt',r.id)+btn('deny','Từ chối',r.id);
  if(module==='payments')return btn('settlement','Thu chi của đơn',r.orderId)+(r.status==='REVIEW_REQUIRED'?btn('refund','Ghi nhận đã hoàn tiền',r.id):'');
  if(module==='rental-orders'){
   let out=`<a class="ops-btn" href="/customer/rental-detail/${encodeURIComponent(r.id)}">Chi tiết</a>`;
   const next={PENDING:'confirm',CONFIRMED:'hand-over',READY_FOR_HANDOVER:'hand-over',HANDED_OVER:'complete',EXTENDED:'complete',OVERDUE:'complete',RETURN_REQUESTED:'complete',RETURNED:'complete'}[r.status];
   if(next)out+=btn(next,{confirm:'Xác nhận','hand-over':'Đã giao xe',complete:'Hoàn tất đơn'}[next],r.id);
   if(['PENDING','CONFIRMED','READY_FOR_HANDOVER'].includes(r.status))out+=btn('vehicle-change','Đổi xe / hoàn tiền',r.id);
   if(['HANDED_OVER','EXTENDED','OVERDUE'].includes(r.status))out+=btn('during-change','Đổi xe / thuê tiếp',r.id);
   return out;
  }return '';
 }
 function render(){const columns=configs[module].columns;$('#live-head').innerHTML='<tr>'+columns.map(k=>'<th>'+safe(labels[k]||k)+'</th>').join('')+'<th>Thao tác</th></tr>';const data=visible();$('#live-body').innerHTML=data.map(r=>'<tr>'+columns.map(k=>'<td>'+safe(format(k,r[k]))+'</td>').join('')+'<td><div class="cw-actions">'+actions(r)+'</div></td></tr>').join('');$('#live-count').textContent=data.length+' / '+rows.length+' bản ghi';}
 async function load(){if(error)error.textContent='';const result=await api('/api/operations/data/'+module+(module==='travel-places'?'?'+new URLSearchParams({query:$('#live-search').value,page:placePage}):''));if(module==='travel-places'){rows=result.items;placeTotal=result.total;placePages=result.pages;}else rows=result;render();if(module==='travel-places'){$('#live-count').textContent=placeTotal+' địa điểm · Trang '+(placePage+1)+' / '+Math.max(1,placePages);$('#place-prev').disabled=placePage===0;$('#place-next').disabled=placePage+1>=placePages;}}
 function edit(r){editing=r;const config=configs[module];$('#live-title').textContent=r?'Cập nhật thông tin':'Thêm mới';$('#live-form-error').textContent='';$('#live-fields').replaceChildren();
  config.fields.forEach(k=>{const label=document.createElement('label');label.className='cw-field';label.textContent=labels[k]||k;let input;const choices=config.select?.[k];
   if(choices){input=document.createElement('select');const options=typeof choices==='string'?(lookups[choices]||[]):choices.map(v=>({id:v,name:states[v]||v}));options.forEach(o=>{const el=document.createElement('option');el.value=o.id;el.textContent=o.name;input.append(el);});}
   else if(['description','staffReply'].includes(k)){input=document.createElement('textarea');input.rows=4;}
   else {input=document.createElement('input');input.type=numberKeys.includes(k)?'number':k==='password'?'password':k==='email'?'email':k.endsWith('Date')?'datetime-local':'text';}
   if(['lat','lng'].includes(k)){input.type='number';input.step='any';input.min=k==='lat'?'-90':'-180';input.max=k==='lat'?'90':'180';}
   input.name=k;input.required=module==='travel-places'?['name','address','published'].includes(k):!['password','description','imageUrl','address'].includes(k);if(k==='password'&&!r)input.required=true;
   if(numberKeys.includes(k))input.min='0';input.value=r?.[k]??(numberKeys.includes(k)?'0':input.value);if(r&&k==='username')input.readOnly=true;
   if(k==='motorbikeId'&&r)input.disabled=true;label.append(input);$('#live-fields').append(label);
  });
  if(module==='motorbikes'){
   const category=$('#live-fields select[name="categoryId"]'),deposit=$('#live-fields input[name="depositAmount"]');
   const note=document.createElement('small');deposit.parentElement.append(note);
   const syncDeposit=()=>{
    const rate=(lookups.categories||[]).find(c=>String(c.id)===category.value)?.securityDeposit;
    deposit.readOnly=rate!=null;
    if(rate!=null)deposit.value=rate;
    note.textContent=rate!=null?'Mức cọc 1 CCCD theo loại xe: '+money(rate)+'. Phương án 2 CCCD không thu cọc tài sản.':'Loại xe này chưa có mức cọc chung; nhập mức cọc riêng của xe.';
   };
   category.addEventListener('change',syncDeposit);syncDeposit();
  }
  $('#live-dialog').showModal();
 }
 async function action(action,id){
  if(action==='during-change')return window.DuringRentalChanges.open(id);
  if(action==='vehicle-change')return window.VehicleChanges.open(id);
  if(action==='edit')return edit(rows.find(r=>String(r.id)===String(id)));
  if(action==='settlement')return settlement(id);
  if(action==='hand-over')return handover(id);
  if(action==='complete')return completeModal(id);
  if(action==='approve'||action==='deny'){if(!confirm('Xác nhận xử lý yêu cầu gia hạn?'))return;await api('/api/operations/extensions/'+id+'?approve='+(action==='approve'),{method:'POST'});}
  else if(action==='refund'){const reference=prompt('Mã biên nhận sau khi bạn đã hoàn tiền thực tế:');if(!reference)return;await api('/api/operations/payments/'+id+'/refund',{method:'POST',...json({reference})});}
  else {let url='/api/rental-orders/'+id+'/'+action;if(action==='reject'){const reason=prompt('Nhập lý do từ chối:');if(!reason)return;url+='?reason='+encodeURIComponent(reason);}else if(!confirm('Xác nhận thực hiện thao tác này?'))return;await api(url,{method:'POST'});}
  await load();
 }
 async function settlement(id){
  const inspectionData=await api('/api/operations/orders/'+id+'/inspection');
  const panel=document.createElement('dialog');panel.style.cssText='max-width:680px;width:95%;padding:28px;border-radius:20px;background:#102331;color:white';
  panel.innerHTML='<h2 style="font-size:22px">Thu chi và biên bản</h2><p class="cw-muted">Chỉ ghi nhận khoản tiền đã nhận hoặc đã hoàn thực tế.</p><p class="cw-message" role="status"></p><form class="cw-grid"><label class="cw-field cw-full">Khoản tiền<select name="purpose"><option value="BALANCE">Thu đủ tiền thuê còn lại</option><option value="SECURITY">Thu bảo đảm tài sản</option><option value="DEPOSIT_REFUND">Hoàn cọc đơn hủy hoặc từ chối</option><option value="SECURITY_REFUND">Hoàn bảo đảm tài sản</option></select></label><label class="cw-field cw-full">Mã biên nhận<input name="reference" required maxlength="100"></label><label class="cw-field">Phương thức<select name="method"><option value="CASH">Tiền mặt</option><option value="BANK_TRANSFER">Chuyển khoản</option><option value="OTHER">Khác</option></select></label><label class="cw-field cw-full">Ghi chú<textarea name="note" maxlength="2000"></textarea></label><button class="cw-btn primary">Ghi nhận thu / hoàn tiền</button></form><hr style="margin:24px 0;border-color:#344a5b"><form class="cw-grid" data-inspection><label class="cw-field cw-full">Tình trạng lúc giao<textarea name="before" maxlength="4000"></textarea></label><label class="cw-field">Km lúc giao<input type="number" min="0" name="odoBefore"></label><label class="cw-field">Km lúc trả<input type="number" min="0" name="odoAfter"></label><label class="cw-field cw-full">Tình trạng lúc trả<textarea name="after" maxlength="4000"></textarea></label><button class="cw-btn">Lưu biên bản</button></form><button class="cw-btn" data-close style="margin-top:20px">Đóng</button>';
  const inspectionForm=panel.querySelector('[data-inspection]');
  const beforeHandover=['CONFIRMED','READY_FOR_HANDOVER'].includes(inspectionData.status);
  const afterHandover=['HANDED_OVER','EXTENDED','OVERDUE','RETURN_REQUESTED','RETURNED'].includes(inspectionData.status);
  ['before','after','odoBefore','odoAfter'].forEach(k=>{const field=inspectionForm.elements.namedItem(k);field.value=inspectionData[k]??'';field.disabled=['before','odoBefore'].includes(k)?!beforeHandover:!afterHandover;});
  inspectionForm.querySelector('button').disabled=!beforeHandover&&!afterHandover;
  document.body.append(panel);panel.showModal();panel.querySelector('[data-close]').onclick=()=>{panel.close();panel.remove();};
  panel.querySelectorAll('form').forEach(form=>form.onsubmit=async e=>{e.preventDefault();const button=form.querySelector('button');button.disabled=true;try{const d=Object.fromEntries(new FormData(form));const inspection=form.hasAttribute('data-inspection');if(inspection){for(const k of ['odoBefore','odoAfter']){if(k in d)d[k]=d[k]===''?null:Number(d[k]);}}await api('/api/operations/orders/'+id+(inspection?'/inspection':'/cash'),{method:inspection?'PUT':'POST',...json(d)});panel.querySelector('.cw-message').textContent='Đã lưu thành công.';if(module)await load();}catch(e){panel.querySelector('.cw-message').textContent=e.message;}finally{button.disabled=false;}});
 }
 async function handover(id){
  const order=await api('/api/rental-orders/'+id);
  const remaining=Math.max(0,(order.totalPrice||0)-(order.depositPaid||0)-(order.balancePaid||0));
  const deposit=Math.max(0,order.securityDeposit||0);
  const panel=document.createElement('dialog');panel.style.cssText='max-width:540px;width:95%;padding:28px;border-radius:20px;background:#102331;color:white';
  panel.innerHTML='<h2 style="font-size:22px">Bàn giao xe & Ghi nhận tiền</h2><p class="cw-muted">'+safe(order.orderCode)+' · Khách: '+safe(order.customerName)+'</p><p class="cw-message" role="status"></p><form class="cw-grid"><label class="cw-field cw-full">Tiền thuê còn lại thực thu<input type="number" min="0" step="1000" name="remainingAmount" value="'+remaining+'" required></label><label class="cw-field cw-full">Cọc tài sản thực thu<input type="number" min="0" step="1000" name="securityDeposit" value="'+deposit+'" required></label><label class="cw-field">Phương thức<select name="paymentMethod"><option value="CASH">Tiền mặt</option><option value="BANK_TRANSFER">Chuyển khoản</option><option value="OTHER">Khác</option></select></label><label class="cw-field">Phí nhận sớm<input type="number" min="0" step="1000" name="earlyPickupFee" value="0"></label><label class="cw-field cw-full">Thời điểm giao nhận thực tế<input type="datetime-local" name="actualPickupTime"></label><label class="cw-field cw-full">Ghi chú<textarea name="note" maxlength="2000"></textarea></label><button class="cw-btn primary" style="margin-top:12px">Xác nhận đã giao xe</button></form><button class="cw-btn" data-close style="margin-top:16px">Đóng</button>';
  document.body.append(panel);panel.showModal();panel.querySelector('[data-close]').onclick=()=>{panel.close();panel.remove();};
  const form=panel.querySelector('form');
  form.onsubmit=async e=>{e.preventDefault();const button=form.querySelector('button');button.disabled=true;try{const fd=new FormData(form);const payload={remainingAmount:Number(fd.get('remainingAmount')),securityDeposit:Number(fd.get('securityDeposit')),paymentMethod:fd.get('paymentMethod'),earlyPickupFee:Number(fd.get('earlyPickupFee')||0),actualPickupTime:fd.get('actualPickupTime')||null,note:fd.get('note')?.trim()||null};await api('/api/rental-orders/'+id+'/hand-over',{method:'POST',...json(payload)});panel.close();panel.remove();if(module)await load();}catch(err){panel.querySelector('.cw-message').textContent=err.message;}finally{button.disabled=false;}};
 }
 async function completeModal(id){
  const order=await api('/api/rental-orders/'+id);
  let held=0;
  (order.payments||[]).forEach(p=>{
   if(p.status==='PAID'||p.status==='REFUNDED'){
    const t=p.transactionType||p.purpose;
    if(['ASSET_DEPOSIT','ASSET_DEPOSIT_ADDITIONAL','SECURITY'].includes(t))held+=Math.abs(p.amount);
    else if(['ASSET_DEPOSIT_REFUND','DEPOSIT_DEDUCTION','SECURITY_REFUND'].includes(t))held-=Math.abs(p.amount);
   }
  });
  if(held<0)held=0;
  const isOverdue=new Date()>new Date(order.returnDate);
  const isReturnReq=order.status==='RETURN_REQUESTED';
  const panel=document.createElement('dialog');
  panel.style.cssText='max-width:560px;width:95%;padding:24px;border-radius:18px;background:#102331;color:#f8fafc;border:1px solid #334155;box-shadow:0 25px 50px -12px rgba(0,0,0,0.5)';
  panel.innerHTML='<h2 style="font-size:20px;font-weight:700;margin-bottom:6px">Hoàn tất đơn thuê xe</h2>'+
  '<p class="cw-muted" style="margin-bottom:12px;font-size:14px">'+safe(order.orderCode)+' · Khách: '+safe(order.customerName||'')+'</p>'+
  (isOverdue?'<div style="color:#ef4444;background:#ef444422;padding:8px 12px;border-radius:8px;margin-bottom:12px;font-size:13px;font-weight:600">⚠️ Trả xe quá hạn! Hạn trả: '+safe(order.returnDate)+'</div>':'')+
  (isReturnReq?'<div style="color:#f59e0b;background:#f59e0b22;padding:8px 12px;border-radius:8px;margin-bottom:12px;font-size:13px">ℹ️ Khách có yêu cầu trả xe sớm</div>':'')+
  '<p class="cw-message" role="status" style="margin-bottom:8px"></p>'+
  '<form class="cw-grid">'+
  '<div class="cw-full" style="background:#1e293b;padding:12px;border-radius:10px;margin-bottom:6px">'+
  '<div style="font-size:12px;color:#94a3b8">Tiền cọc tài sản đang giữ</div>'+
  '<strong style="font-size:18px;color:#38bdf8">'+held.toLocaleString('vi-VN')+' đ</strong>'+
  '</div>'+
  '<label class="cw-field">Số tiền hoàn cọc (VNĐ)<input type="number" min="0" max="'+held+'" step="1000" name="assetDepositRefundAmount" value="'+held+'" required></label>'+
  '<label class="cw-field">Khấu trừ cọc (VNĐ)<input type="number" min="0" max="'+held+'" step="1000" name="assetDepositDeductionAmount" value="0" required></label>'+
  '<label class="cw-field cw-full">Lý do khấu trừ cọc (nếu có)<input type="text" name="depositDeductionReason" placeholder="Ví dụ: bồi thường trầy xước, thiếu phụ kiện..."></label>'+
  '<label class="cw-field">Phương thức hoàn cọc<select name="depositRefundMethod"><option value="CASH">Tiền mặt</option><option value="BANK_TRANSFER">Chuyển khoản</option><option value="OTHER">Khác</option></select></label>'+
  '<label class="cw-field">Phí trả trễ (VNĐ)<input type="number" min="0" step="1000" name="lateFee" value="0"></label>'+
  '<label class="cw-field cw-full">Lý do thu phí trễ<input type="text" name="lateFeeReason" placeholder="Ví dụ: Khách trả trễ 3 giờ"></label>'+
  '<label class="cw-field">Phương thức thu phí trễ<select name="lateFeePaymentMethod"><option value="CASH">Tiền mặt</option><option value="BANK_TRANSFER">Chuyển khoản</option><option value="OTHER">Khác</option></select></label>'+
  (isReturnReq?'<div class="cw-full" style="border-top:1px solid #334155;padding-top:10px;margin-top:6px">'+
  '<div style="font-weight:600;font-size:13px;margin-bottom:6px">Quyết toán trả xe sớm:</div>'+
  '<label class="cw-field">Hoàn tiền thuê trả sớm (VNĐ)<input type="number" min="0" step="1000" name="refundAmount" value="0"></label>'+
  '<label class="cw-field">Lý do hoàn tiền thuê<input type="text" name="refundReason" value="Khách trả xe sớm"></label>'+
  '<label class="cw-field">Hình thức hoàn tiền thuê<select name="paymentMethod"><option value="CASH">Tiền mặt</option><option value="BANK_TRANSFER">Chuyển khoản</option><option value="OTHER">Khác</option></select></label>'+
  '</div>':'')+
  '<button class="cw-btn primary" style="margin-top:14px;width:100%">Xác nhận hoàn tất đơn</button>'+
  '</form>'+
  '<button class="cw-btn" data-close style="margin-top:10px;width:100%">Đóng</button>';
  document.body.append(panel);
  panel.showModal();
  panel.querySelector('[data-close]').onclick=()=>{panel.close();panel.remove();};
  const form=panel.querySelector('form');
  const refIn=form.elements.namedItem('assetDepositRefundAmount');
  const dedIn=form.elements.namedItem('assetDepositDeductionAmount');
  if(refIn&&dedIn&&held>0){
   refIn.oninput=()=>{const v=Math.min(held,Math.max(0,Number(refIn.value)||0));dedIn.value=held-v;};
   dedIn.oninput=()=>{const v=Math.min(held,Math.max(0,Number(dedIn.value)||0));refIn.value=held-v;};
  }
  form.onsubmit=async e=>{
   e.preventDefault();
   const btn=form.querySelector('button.primary');btn.disabled=true;
   try{
    const fd=new FormData(form);
    const payload={
     assetDepositRefundAmount:Number(fd.get('assetDepositRefundAmount')||0),
     assetDepositDeductionAmount:Number(fd.get('assetDepositDeductionAmount')||0),
     depositDeductionReason:fd.get('depositDeductionReason')?.trim()||null,
     depositRefundMethod:fd.get('depositRefundMethod')||'CASH',
     lateFee:Number(fd.get('lateFee')||0),
     lateFeeReason:fd.get('lateFeeReason')?.trim()||null,
     lateFeePaymentMethod:fd.get('lateFeePaymentMethod')||'CASH',
     refundAmount:Number(fd.get('refundAmount')||0),
     refundReason:fd.get('refundReason')?.trim()||null,
     paymentMethod:fd.get('paymentMethod')||'CASH'
    };
    await api('/api/rental-orders/'+id+'/complete',{method:'POST',...json(payload)});
    panel.close();panel.remove();
    if(typeof load==='function')await load();
    else location.reload();
   }catch(err){panel.querySelector('.cw-message').textContent=err.message;btn.disabled=false;}
  };
 }
 if(module){
  Promise.all([api('/api/operations/lookups').then(d=>lookups=d),load()]).catch(show);
  let searchTimer;$('#live-search').oninput=()=>{if(module!=='travel-places'){render();return;}clearTimeout(searchTimer);searchTimer=setTimeout(()=>{placePage=0;load().catch(show);},300);};if(module==='travel-places'){$('#place-prev').onclick=()=>{placePage--;load().catch(show);};$('#place-next').onclick=()=>{placePage++;load().catch(show);};}$('#live-reload').onclick=()=>load().catch(show);$('#live-export').onclick=()=>download('motorent-'+module+'.csv',configs[module].columns,visible());
  if(configs[module].fields&&!configs[module].noCreate){$('#live-add').hidden=false;$('#live-add').onclick=()=>edit(null);}
  $('#live-close').onclick=()=>$('#live-dialog').close();
  $('#live-body').onclick=async e=>{const b=e.target.closest('[data-live-action]');if(!b)return;b.disabled=true;try{await action(b.dataset.liveAction,b.dataset.id);}catch(e){show(e);}finally{b.disabled=false;}};
  $('#live-form').onsubmit=async e=>{e.preventDefault();const b=e.target.querySelector('button');b.disabled=true;try{const d=Object.fromEntries(new FormData(e.target));await api('/api/operations/data/'+module+(editing?'/'+encodeURIComponent(editing.id):''),{method:editing?'PUT':'POST',...json(d)});$('#live-dialog').close();await load();}catch(e){$('#live-form-error').textContent=e.message;}finally{b.disabled=false;}};
 }
 if($('#live-settings')){api('/api/shop').then(d=>{Object.entries(d).forEach(([k,v])=>{const f=$('#live-settings').elements.namedItem(k);if(f)f.value=v??'';});}).catch(show);$('#live-settings').onsubmit=async e=>{e.preventDefault();const b=e.target.querySelector('button');b.disabled=true;try{await api('/api/admin/settings',{method:'PUT',...json(Object.fromEntries(new FormData(e.target)))});show('Đã lưu cấu hình cửa hàng.');}catch(e){show(e);}finally{b.disabled=false;}};}
 if($('#live-report')){
  const form=$('#report-period'),today=new Date(),local=d=>{const x=new Date(d);x.setMinutes(x.getMinutes()-x.getTimezoneOffset());return x.toISOString().slice(0,10);};form.elements.from.value=local(new Date(today.getFullYear(),today.getMonth(),1));form.elements.to.value=local(today);let result;
  async function report(){result=await api('/api/admin/reports?'+new URLSearchParams(new FormData(form)));const metrics=[['rentalNetRevenue','Doanh thu thuần thuê xe',true],['operatingProfit','Lợi nhuận hoạt động',true],['payosIn','Thu qua PayOS',true],['cashIn','Thu tiền mặt (Staff)',true],['bankTransferIn','Thu chuyển khoản (Staff)',true],['rentalRefunded','Tiền hoàn khách thuê',true],['lateFeeCollected','Phí trễ hạn thu được',true],['totalAssetDepositHeld','Quỹ cọc tài sản đang giữ',true],['assetDepositRefunded','Cọc tài sản đã hoàn',true],['assetDepositDeducted','Cọc tài sản đã khấu trừ',true],['maintenanceCost','Chi phí bảo dưỡng xe',true],['completedOrders','Số đơn hoàn tất'],['paymentsToReview','Giao dịch cần đối soát']];$('#report-stats').innerHTML=metrics.map(([k,label,isMoney])=>'<div class="ops-stat"><div class="ops-stat-label">'+label+'</div><strong>'+safe(isMoney?money(result[k]):result[k])+'</strong></div>').join('');const daily=Object.entries(result.daily);$('#report-daily').innerHTML=daily.length?daily.map(([day,total])=>'<div class="cw-row" style="padding:10px"><span>'+safe(day)+'</span><strong>'+money(total)+'</strong></div>').join(''):'Chưa có đơn hoàn tất trong khoảng thời gian này.';}
  form.onsubmit=e=>{e.preventDefault();report().catch(show);};$('#report-export').onclick=()=>{if(result)download('motorent-doanh-thu.csv',['date','revenue'],Object.entries(result.daily).map(([date,revenue])=>({date,revenue})));};report().catch(show);
 }
})();
