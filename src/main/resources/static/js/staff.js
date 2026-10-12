/* Staff UI only; existing rental API routes and state transitions are preserved. */
(() => {
  'use strict';
  const app = document.querySelector('.staff-app');
  if (!app) return;
  const $ = (s) => app.querySelector(s);
  const $$ = (s) => [...app.querySelectorAll(s)];
  const labels = {PENDING_PAYMENT:'Chờ thanh toán cọc', PENDING:'Chờ duyệt', CONFIRMED:'Đã xác nhận', READY_FOR_HANDOVER:'Sẵn sàng giao', HANDED_OVER:'Đang thuê', RETURN_REQUESTED:'Yêu cầu trả xe', RETURNED:'Đã trả xe', COMPLETED:'Đã hoàn thành', CANCELLED:'Đã hủy', REJECTED:'Đã từ chối', OVERDUE:'Quá hạn', EXTENDED:'Đã gia hạn'};
  const money = (v) => v == null ? 'Chưa có' : new Intl.NumberFormat('vi-VN').format(v) + ' ₫';
  const date = (v) => v ? new Intl.DateTimeFormat('vi-VN', {dateStyle:'short', timeStyle:'short'}).format(new Date(v)) : 'Chưa có';
  function token() {
    // Login stores the same token in a cookie and localStorage. Prefer the cookie used by page navigation.
    const cookie = document.cookie.split('; ').find(v => v.startsWith('access_token='));
    if (cookie) return decodeURIComponent(cookie.substring(13));
    try { return localStorage.getItem('token'); } catch (_) { return null; }
  }
  async function api(url, options = {}) {
    const auth = token();
    const response = await fetch(url, {...options, credentials:'same-origin', headers:{Accept:'application/json', ...(auth ? {Authorization:'Bearer ' + auth} : {}), ...options.headers}});
    if (response.redirected || response.status === 401) throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    const isJson = (response.headers.get('content-type') || '').includes('json');
    const data = isJson ? await response.json() : null;
    if (!response.ok) throw new Error(data?.message || (response.status === 403 ? 'Tài khoản không có quyền thực hiện thao tác này.' : 'Không thể xử lý yêu cầu. Vui lòng thử lại.'));
    if (!isJson) throw new Error('Không nhận được dữ liệu hợp lệ. Vui lòng kiểm tra phiên đăng nhập.');
    return data;
  }
  function report(element, error) { element.textContent = error instanceof TypeError ? 'Không kết nối được máy chủ. Kiểm tra mạng và thử lại.' : error.message; element.hidden = false; }
  function fields(container, values) {
    const dl = document.createElement('dl');
    values.forEach(([name, value]) => {
      const item = document.createElement('div'), dt = document.createElement('dt'), dd = document.createElement('dd');
      dt.textContent = name; dd.textContent = value == null || value === '' ? 'Chưa có thông tin' : value;
      item.append(dt, dd); dl.append(item);
    });
    container.replaceChildren(dl);
  }
  $$('.staff-badge[data-status]').forEach(b => b.textContent = labels[b.dataset.status] || b.dataset.status);
  $$('[data-refresh]').forEach(b => b.addEventListener('click', () => location.reload()));
  $$('[data-close]').forEach(b => b.addEventListener('click', () => b.closest('dialog').close()));
  $$('dialog').forEach(d => d.addEventListener('click', e => { if (e.target === d && !d.dataset.busy) { const r = d.getBoundingClientRect(); if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom) d.close(); } }));

  const cards = $$('.staff-order'), search = $('#staff-search');
  if (search) {
    const params = new URLSearchParams(location.search);
    let selected = params.get('status') || 'ALL';
    if (!$$('[data-filter]').some(b => b.dataset.filter === selected)) selected = 'ALL';
    search.value = params.get('order') || params.get('q') || '';
    const normalize = value => value.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').toLowerCase();
    const group = status => ({PENDING:'PENDING',CONFIRMED:'PREPARING',READY_FOR_HANDOVER:'PREPARING',HANDED_OVER:'RENTING',EXTENDED:'RENTING',OVERDUE:'OVERDUE'}[status] || 'OTHER');
    const haystacks = new Map(cards.map(c => [c, normalize(c.querySelector('.staff-order-top').textContent + ' ' + c.querySelector('.staff-order-body').textContent)]));
    function filter(updateUrl = true) {
      const query = normalize(search.value.trim());
      let visible = 0;
      cards.forEach(c => { c.hidden = !((selected === 'ALL' || group(c.dataset.status) === selected) && haystacks.get(c).includes(query)); if (!c.hidden) visible++; });
      $$('[data-filter]').forEach(b => b.setAttribute('aria-pressed', String(b.dataset.filter === selected)));
      $$('[data-count]').forEach(b => b.textContent = cards.filter(c => (b.dataset.count === 'ALL' || group(c.dataset.status) === b.dataset.count) && haystacks.get(c).includes(query)).length);
      $('#staff-result-count').textContent = visible + ' / ' + cards.length + ' đơn đang xử lý';
      $('#staff-empty').hidden = visible !== 0;
      if (!cards.length) { $('#staff-empty-title').textContent = 'Chưa có đơn đang xử lý'; $('#staff-empty-description').textContent = 'Đơn mới sẽ xuất hiện khi khách gửi yêu cầu thuê xe.'; $('#staff-reset').hidden = true; }
      if (updateUrl) { const url = new URL(location.href); ['order','q','status'].forEach(k => url.searchParams.delete(k)); if (selected !== 'ALL') url.searchParams.set('status', selected); if (search.value.trim()) url.searchParams.set('q', search.value.trim()); history.replaceState(null, '', url); }
    }
    search.addEventListener('input', () => filter());
    $$('[data-filter]').forEach(b => b.addEventListener('click', () => { selected = b.dataset.filter; filter(); }));
    $('#staff-reset').addEventListener('click', () => { selected = 'ALL'; search.value = ''; filter(); search.focus(); });
    filter(false);
  }

  let detailSequence = 0;
  $$('[data-detail]').forEach(b => b.addEventListener('click', async () => {
    const sequence = ++detailSequence, dialog = $('#staff-detail'), body = $('#staff-detail-body');
    body.textContent = 'Đang tải chi tiết đơn…'; dialog.showModal();
    try {
      const d = await api('/api/rental-orders/' + encodeURIComponent(b.dataset.id));
      if (sequence !== detailSequence || !dialog.open) return;
      fields(body, [['Mã đơn',d.orderCode],['Trạng thái',labels[d.status] || d.status],['Khách hàng',d.customerName],['Số điện thoại',d.customer?.phone],['Xe thuê',d.motorbikeName],['Số ngày thuê',d.days],['Nhận xe',date(d.pickupDate)],['Trả xe dự kiến',date(d.returnDate)],['Tổng tiền thuê',money(d.totalPrice)],['Hình thức nhận',d.pickupType === 'DELIVERY' ? 'Giao tận nơi' : 'Tại cửa hàng'],...(d.rejectionReason ? [['Lý do từ chối',d.rejectionReason]] : [])]);
    } catch (error) {
      if (sequence !== detailSequence || !dialog.open) return;
      body.replaceChildren(); const p = document.createElement('p'); p.setAttribute('role','alert'); report(p,error); body.append(p);
      const retry = document.createElement('button'); retry.type = 'button'; retry.className = 'ops-btn'; retry.textContent = 'Thử lại'; retry.style.marginTop = '16px'; retry.addEventListener('click', () => { dialog.close(); b.click(); }); body.append(retry);
    }
  }));

  const descriptions = {confirm:['Xác nhận đơn thuê','Duyệt đơn này sau khi đã kiểm tra xe và lịch thuê.'],reject:['Từ chối đơn thuê','Lý do sẽ được gửi cùng thao tác từ chối đơn.']};
  let pendingAction = null, busy = false;
  $$('[data-action]').forEach(b => b.addEventListener('click', async () => {
    if (b.dataset.action === 'hand-over') {
      const dialog = $('#staff-handover');
      if (dialog) {
        let remaining = parseInt(b.dataset.remaining || '0', 10);
        let deposit = parseInt(b.dataset.deposit || '0', 10);
        $('#staff-handover-info').textContent = (b.dataset.code || '') + (b.dataset.customer ? ' · Khách: ' + b.dataset.customer : '') + (b.dataset.bike ? ' · Xe: ' + b.dataset.bike : '');
        $('#handover-remaining').value = Math.max(0, remaining);
        $('#handover-deposit').value = Math.max(0, deposit);
        $('#handover-method').value = 'CASH';
        $('#handover-early-fee').value = 0;
        $('#handover-time').value = '';
        $('#handover-note').value = '';
        $('#staff-handover-error').hidden = true;
        dialog.dataset.orderId = b.dataset.id;
        dialog.showModal();
        return;
      }
    }
    if (b.dataset.action === 'complete') {
      if (b.disabled) return;
      completeStaffModal(b.dataset.id);
      return;
    }
    pendingAction = {id:b.dataset.id, action:b.dataset.action};
    const [title,description] = descriptions[pendingAction.action];
    $('#staff-confirm-title').textContent = title;
    $('#staff-confirm-text').textContent = b.dataset.code + ' · ' + description;
    $('#staff-reason-field').hidden = pendingAction.action !== 'reject';
    $('#staff-reason').required = pendingAction.action === 'reject'; $('#staff-reason').value = '';
    $('#staff-action-error').hidden = true; $('#staff-action-submit').textContent = title;
    $('#staff-confirm').showModal();
    if (pendingAction.action === 'reject') $('#staff-reason').focus();
  }));
  $('#staff-confirm')?.addEventListener('cancel', e => { if (busy) e.preventDefault(); });
  $('#staff-action-form')?.addEventListener('submit', async e => {
    e.preventDefault(); if (busy || !pendingAction) return;
    const reason = $('#staff-reason').value.trim();
    if (pendingAction.action === 'reject' && !reason) { report($('#staff-action-error'),new Error('Vui lòng nhập lý do từ chối.')); $('#staff-reason').focus(); return; }
    busy = true; const dialog = $('#staff-confirm'), submit = $('#staff-action-submit'), original = submit.textContent;
    dialog.dataset.busy = 'true'; dialog.querySelectorAll('button').forEach(b => b.disabled = true); submit.textContent = 'Đang xử lý…'; $('#staff-action-error').hidden = true;
    try {
      await api('/api/rental-orders/' + encodeURIComponent(pendingAction.id) + '/' + pendingAction.action + (pendingAction.action === 'reject' ? '?reason=' + encodeURIComponent(reason) : ''), {method:'POST', headers:{'Content-Type':'application/json'}});
      // Keep the server as source of truth for status transitions and filtered counts.
      location.reload();
    } catch (error) { report($('#staff-action-error'), error); }
    finally { busy = false; delete dialog.dataset.busy; dialog.querySelectorAll('button').forEach(b => b.disabled = false); submit.textContent = original; }
  });

  $('#staff-handover')?.addEventListener('cancel', e => { if (busy) e.preventDefault(); });
  $('#staff-handover-form')?.addEventListener('submit', async e => {
    e.preventDefault(); if (busy) return;
    const dialog = $('#staff-handover'), orderId = dialog.dataset.orderId;
    if (!orderId) return;
    const remainingAmount = parseInt($('#handover-remaining').value || '0', 10);
    const securityDeposit = parseInt($('#handover-deposit').value || '0', 10);
    const paymentMethod = $('#handover-method').value;
    const actualPickupTime = $('#handover-time').value ? $('#handover-time').value : null;
    const earlyPickupFee = parseInt($('#handover-early-fee').value || '0', 10);
    const note = $('#handover-note').value.trim() || null;

    const payload = { remainingAmount, securityDeposit, paymentMethod, actualPickupTime, earlyPickupFee, note };
    busy = true;
    const submit = $('#staff-handover-submit'), original = submit.textContent;
    dialog.dataset.busy = 'true';
    dialog.querySelectorAll('button').forEach(b => b.disabled = true);
    submit.textContent = 'Đang xử lý…';
    $('#staff-handover-error').hidden = true;
    try {
      await api('/api/rental-orders/' + encodeURIComponent(orderId) + '/hand-over', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      location.reload();
    } catch (error) {
      report($('#staff-handover-error'), error);
    } finally {
      busy = false;
      delete dialog.dataset.busy;
      dialog.querySelectorAll('button').forEach(b => b.disabled = false);
      submit.textContent = original;
    }
  });

  async function loadProfile() {
    const state = $('#staff-profile-state'), panel = $('#staff-profile-fields');
    state.textContent = 'Đang tải thông tin…'; panel.hidden = true;
    try {
      if (!token()) throw new Error('Chưa có phiên đăng nhập. Vui lòng đăng nhập để xem thông tin.');
      const response = await api('/api/auth/me'), user = response.result;
      if (!user) throw new Error('Không tìm thấy thông tin phiên đăng nhập.');
      $('#staff-profile-name').textContent = [user.firstName,user.lastName].filter(Boolean).join(' ') || user.username;
      state.textContent = user.role === 'ADMIN' ? 'Quản trị viên' : 'Nhân viên';
      fields(panel,[['Tên đăng nhập',user.username],['Email',user.email],['Số điện thoại',user.phone],['Vai trò',user.role === 'ADMIN' ? 'Quản trị viên' : 'Nhân viên']]); panel.hidden = false;
    } catch (error) { report(state,error); }
  }
  if ($('#staff-profile-state')) { loadProfile(); $('#staff-profile-retry').addEventListener('click',loadProfile); }
  $('#staff-logout')?.addEventListener('click', async e => {
    const button = e.currentTarget; button.disabled = true;
    try {
      if (token()) await api('/api/auth/logout',{method:'POST'});
      try { localStorage.removeItem('token'); } catch (_) { /* Storage can be disabled. */ }
      document.cookie = 'access_token=; path=/; max-age=0';
      location.assign('/auth/login?logout');
    } catch (error) { report($('#staff-profile-state'),error); button.disabled = false; }
  });
  async function completeStaffModal(id){
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
    '<p class="cw-muted" style="margin-bottom:12px;font-size:14px">'+(order.orderCode||'')+' · Khách: '+(order.customerName||'')+'</p>'+
    (isOverdue?'<div style="color:#ef4444;background:#ef444422;padding:8px 12px;border-radius:8px;margin-bottom:12px;font-size:13px;font-weight:600">⚠️ Trả xe quá hạn! Hạn trả: '+(order.returnDate||'')+'</div>':'')+
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
        await api('/api/rental-orders/'+id+'/complete',{
          method:'POST',
          headers:{'Content-Type':'application/json'},
          body:JSON.stringify(payload)
        });
        panel.close();panel.remove();
        location.reload();
      }catch(err){panel.querySelector('.cw-message').textContent=err.message;btn.disabled=false;}
    };
  }

})();

