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
 '<label class="cw-field cw-full">Lý do khấu trừ cọc (bắt buộc nếu có khấu trừ)<input type="text" name="depositDeductionReason" placeholder="Ví dụ: bồi thường trầy xước, thiếu phụ kiện..."></label>'+
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
