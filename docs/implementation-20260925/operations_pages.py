from pathlib import Path
T=Path('src/main/resources/templates')
titles={'motorbikes':'Đội xe','customers':'Khách hàng','staffs':'Nhân viên','rental-orders':'Đơn thuê xe','maintenance':'Bảo dưỡng','promotions':'Khuyến mãi','payments':'Thu chi và đối soát','extensions':'Yêu cầu gia hạn','reviews':'Đánh giá của khách'}
def shell(role,title,active,body):
 return f'''<!DOCTYPE html><html xmlns:th="http://www.thymeleaf.org" lang="vi"><head th:replace="~{{fragments/head :: head(title='{title}')}}"></head><body><div th:replace="~{{fragments/{role}-layout :: layout(activeNav='{active}', content=~{{::#mainContent}})}}"><div id="mainContent"><div class="ops-heading"><div><p class="ops-eyebrow">MOTORENT / QUẢN LÝ</p><h1>{title}</h1></div></div><div id="ops-error" class="cw-message" role="status" aria-live="polite"></div>{body}</div></div></body></html>'''
for role in ['admin','staff']:
 for module,title in titles.items():
  if role=='staff' and module not in ['maintenance','payments','extensions','reviews']:continue
  body=f'''<section data-live-module="{module}"><div class="cw-filter"><input id="live-search" placeholder="Tìm trong danh sách" aria-label="Tìm kiếm"><button class="ops-btn" id="live-reload">Tải lại</button><button class="ops-btn" id="live-export">Xuất CSV</button><button class="ops-btn ops-primary" id="live-add" hidden>Thêm mới</button></div><p id="live-count" class="cw-muted" style="margin:16px 0">Đang tải dữ liệu…</p><div class="ops-panel ops-table-scroll"><table class="ops-table"><thead id="live-head"></thead><tbody id="live-body"></tbody></table></div><dialog id="live-dialog" style="max-width:760px;width:95%;background:#102331;color:#e6edf5;border:1px solid #344a5b;border-radius:20px;padding:28px"><h2 id="live-title" style="font-size:22px;font-weight:700"></h2><p id="live-form-error" class="cw-message" role="alert"></p><form id="live-form"><div id="live-fields" class="cw-grid" style="margin-top:20px"></div><div class="cw-actions"><button class="cw-btn primary">Lưu dữ liệu</button><button type="button" class="cw-btn" id="live-close">Đóng</button></div></form></dialog></section>'''
  (T/role/(module+'.html')).write_text(shell(role,title,module,body),encoding='utf-8')
report='''<section id="live-report"><form id="report-period" class="cw-filter"><label>Từ ngày<input type="date" name="from" required></label><label>Đến ngày<input type="date" name="to" required></label><button class="ops-btn ops-primary">Xem báo cáo</button><button class="ops-btn" type="button" id="report-export">Xuất CSV</button></form><div id="report-stats" class="ops-stats" style="margin-top:24px"></div><section class="ops-panel" style="padding:24px"><h2>Doanh thu đơn hoàn tất theo ngày</h2><p class="cw-muted">Ghi nhận theo ngày thực trả xe. Khoản bảo đảm tài sản không tính vào doanh thu.</p><div id="report-daily" style="margin-top:20px"></div></section></section>'''
for page,title in [('dashboard','Tổng quan kinh doanh'),('reports','Báo cáo kinh doanh')]: (T/'admin'/(page+'.html')).write_text(shell('admin',title,page,report),encoding='utf-8')
fields=[('name','Tên cửa hàng','text'),('representative','Người đại diện','text'),('address','Địa chỉ cửa hàng','text'),('phone','Điện thoại','tel'),('email','Email','email'),('zalo','Zalo','text'),('hours','Giờ mở cửa','text'),('lat','Vĩ độ','number'),('lng','Kinh độ','number'),('deliveryBaseFee','Phí giao nhận cơ bản','number'),('deliveryBaseKm','Khoảng cách cơ bản km','number'),('deliveryPerKm','Phí mỗi km phát sinh','number'),('deliveryMaxKm','Bán kính phục vụ km','number'),('holdMinutes','Thời gian giữ chỗ phút','number'),('depositPercent','Phần trăm cọc thuê xe','number')]
form='<form id="live-settings" class="cw-card cw-grid">'+''.join(f'<label class="cw-field">{label}<input name="{name}" type="{kind}" step="any" required></label>' for name,label,kind in fields)+'<label class="cw-field cw-full">Điều khoản bổ sung<textarea name="terms" rows="6" maxlength="4000"></textarea></label><button class="cw-btn primary">Lưu cấu hình</button></form>'
(T/'admin/settings.html').write_text(shell('admin','Cài đặt cửa hàng','settings',form),encoding='utf-8')
for role in ['admin','staff']:
 p=T/'fragments'/(role+'-layout.html');s=p.read_text('utf-8')
 if role=='admin':
  a=s.index('<nav aria-label=');b=s.index('</nav>',a)+6
  nav={'dashboard':'Tổng quan',**titles,'reports':'Báo cáo','settings':'Cài đặt cửa hàng'}
  s=s[:a]+'<nav aria-label="Điều hướng quản trị">'+''.join(f'<a href="/admin/{k}" th:attr="aria-current=${{activeNav == \'{k}\' ? \'page\' : null}}"><span class="material-symbols-outlined">{("settings" if k=="settings" else "list_alt")}</span><span>{v}</span></a>' for k,v in nav.items())+'</nav>'+s[b:]
 else:
  s=s.replace('</nav>',''.join(f'<a href="/staff/{k}"><span class="material-symbols-outlined">list_alt</span><span>{titles[k]}</span></a>' for k in ['payments','extensions','reviews'])+'</nav>')
 s=s.replace('</div></html>','</div></html>')
 s=s.replace('<script th:src="@{/js/operations.js}" defer></script>','<script th:src="@{/js/operations.js}" defer></script><script src="/js/operations-live.js" defer></script>')
 p.write_text(s,encoding='utf-8')
# Preserve responsive navigation; remove the mock record editor and sample report generator.
p=Path('src/main/resources/static/js/operations.js');s=p.read_text('utf-8');a=s.index('  let toastTimer;');p.write_text(s[:a]+'})();\n',encoding='utf-8')
p=Path('src/main/java/com/motorrent/motorbikerental/controller/web/AdminPageController.java');p.write_text('''package com.motorrent.motorbikerental.controller.web;
import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.*;import org.springframework.ui.Model;import java.util.Set;
@Controller @RequestMapping("/admin")
public class AdminPageController {
 @GetMapping("/{page}") public String page(@PathVariable String page,Model model){
  if(page.equals("stations"))return "redirect:/admin/settings";
  if(!Set.of("dashboard","reports","settings","motorbikes","customers","staffs","rental-orders","maintenance","promotions","payments","extensions","reviews").contains(page))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
  model.addAttribute("activeNav",page);return "admin/"+page;
 }
}
''',encoding='utf-8')
p=Path('src/main/java/com/motorrent/motorbikerental/controller/web/StaffPageController.java');s=p.read_text('utf-8');i=s.rfind('}');s=s[:i]+'''    @GetMapping({"/payments","/extensions","/reviews"})
    public String tools(jakarta.servlet.http.HttpServletRequest request,Model model){String page=request.getRequestURI().substring("/staff/".length());model.addAttribute("activeNav",page);return "staff/"+page;}
'''+s[i:];p.write_text(s,encoding='utf-8')
