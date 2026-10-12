/* Shared navigation for administration and staff pages. Data actions are handled by operations-live.js. */
(() => {
  'use strict';
  const app = document.querySelector('.ops-app');
  if (!app) return;
  const $ = s => app.querySelector(s);
  const icon = name => `<span class="material-symbols-outlined" aria-hidden="true">${name}</span>`;
  const escape = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const money = value => new Intl.NumberFormat('vi-VN').format(Number(value)) + ' ₫';
  const normalize = value => String(value).normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').toLowerCase();
  const pageLabel = $('[data-page-label]');
  if (pageLabel) pageLabel.textContent = $('nav [aria-current="page"] span:last-child')?.textContent || 'MotoRent';
  const menu = $('.ops-menu');
  const mobile = matchMedia('(max-width: 760px)'), navigation=$('.ops-sidebar');
  function syncNavigation() { navigation.inert = mobile.matches && !app.classList.contains('ops-menu-open'); }
  function closeMenu() { app.classList.remove('ops-menu-open'); menu?.setAttribute('aria-expanded','false'); menu?.setAttribute('aria-label','Mở menu'); syncNavigation(); }
  menu?.addEventListener('click', () => { const open = app.classList.toggle('ops-menu-open'); menu.setAttribute('aria-expanded',String(open)); menu.setAttribute('aria-label',open ? 'Đóng menu' : 'Mở menu'); syncNavigation(); });
  mobile.addEventListener('change',closeMenu); syncNavigation();
  app.addEventListener('keydown', e => { if (e.key === 'Escape' && app.classList.contains('ops-menu-open')) { closeMenu(); menu.focus(); } });
  $('.ops-main')?.addEventListener('click', closeMenu);
})();
