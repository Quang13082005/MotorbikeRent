from pathlib import Path
import re
R=Path('src/main/java/com/motorrent/motorbikerental');T=Path('src/main/resources/templates')
def edit(p,a,b):
 s=p.read_text('utf-8');assert a in s,str(p);p.write_text(s.replace(a,b),encoding='utf-8')
edit(R/'entity/RentalOrder.java','String note;','String note;\n    @Column(columnDefinition="TEXT") String identitySnapshot;\n    Boolean termsAccepted;')
edit(R/'dto/request/RentalOrderRequest.java','String promotionCode;','''String promotionCode;
    Boolean termsAccepted;
    java.util.List<Person> people;
    public record Person(String name,String phone,String idCard,String birthDate,String address) {}''')
edit(R/'service/RentalOrderService.java','var q=pricing.quote(request,motorbike);','''var q=pricing.quote(request,motorbike);
        int required=Integer.valueOf(2).equals(request.getSecurityOption())?2:1;
        if(!Boolean.TRUE.equals(request.getTermsAccepted()) || request.getPeople()==null || request.getPeople().size()!=required) throw new IllegalArgumentException("Cần đồng ý điều khoản và nhập đủ thông tin giấy tờ");
        for(var person:request.getPeople()) {
            if(person.name()==null || person.name().isBlank() || person.phone()==null || !person.phone().matches("[+0-9 ()-]{9,20}") || person.idCard()==null || !person.idCard().matches("[a-zA-Z0-9]{6,20}") || person.address()==null || person.address().isBlank()) throw new IllegalArgumentException("Thông tin người thuê chưa đầy đủ hoặc không hợp lệ");
            try { if(!java.time.LocalDate.parse(person.birthDate()).isBefore(java.time.LocalDate.now())) throw new IllegalArgumentException(); } catch(Exception ex) {throw new IllegalArgumentException("Ngày sinh không hợp lệ");}
        }
        if(required==2 && request.getPeople().get(0).idCard().equals(request.getPeople().get(1).idCard())) throw new IllegalArgumentException("Hai giấy tờ phải thuộc hai người khác nhau");
        String identity;
        try {identity=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request.getPeople());}catch(Exception ex){throw new IllegalArgumentException("Không lưu được thông tin hợp đồng");}''')
edit(R/'service/RentalOrderService.java','.note(request.getNote())','.note(request.getNote()).identitySnapshot(identity).termsAccepted(true)')
edit(R/'controller/web/CustomerPageController.java','model.addAttribute("motorbikes", motorbikeService.getAllMotorbikes());','model.addAttribute("motorbikes", motorbikeService.getAllMotorbikes());\n        model.addAttribute("recentReviews",reviews.findAll().stream().sorted(java.util.Comparator.comparing(com.motorrent.motorbikerental.entity.Review::getCreatedAt).reversed()).limit(6).toList());')
# Catalog replaces inert extra cards and fake pagination while retaining all filter capabilities.
exec(Path('docs/implementation-20260925/customer_pages.py').read_text('utf-8').split("profile=")[0])
filters='''<p class="cw-muted">Chọn lịch thuê để kiểm tra xe còn trống.</p><form method="get" class="cw-card cw-grid"><label class="cw-field">Ngày nhận<input type="datetime-local" name="pickup" th:value="${pickup}"></label><label class="cw-field">Ngày trả<input type="datetime-local" name="returnAt" th:value="${returnAt}"></label><label class="cw-field">Tên xe<input name="keyword" th:value="${keyword}" placeholder="Nhập tên xe"></label><label class="cw-field">Loại xe<select name="category"><option value="all">Tất cả</option><option value="xe-ga" th:selected="${category == 'xe-ga'}">Xe ga</option><option value="xe-so" th:selected="${category == 'xe-so'}">Xe số</option><option value="xe-con-tay" th:selected="${category == 'xe-con-tay'}">Xe côn tay</option></select></label><label class="cw-field">Giá thuê<select name="priceFilter"><option value="all">Tất cả</option><option value="under120" th:selected="${priceFilter == 'under120'}">Dưới 120.000 đ</option><option value="120to180" th:selected="${priceFilter == '120to180'}">120.000 đến 180.000 đ</option><option value="above180" th:selected="${priceFilter == 'above180'}">Trên 180.000 đ</option></select></label><label class="cw-field">Dung tích<select name="ccFilter"><option value="all">Tất cả</option><option th:each="cc : ${ccOptions}" th:value="${cc}" th:text="${cc+' cc'}" th:selected="${ccFilter == #strings.toString(cc)}"></option></select></label><label class="cw-field">Sắp xếp<select name="sortBy"><option value="popular">Đánh giá cao</option><option value="price-asc" th:selected="${sortBy == 'price-asc'}">Giá tăng dần</option><option value="price-desc" th:selected="${sortBy == 'price-desc'}">Giá giảm dần</option><option value="newest" th:selected="${sortBy == 'newest'}">Đời xe mới</option></select></label><label class="cw-field">Khả dụng<select name="statusFilter"><option value="all">Tất cả</option><option value="available" th:selected="${statusFilter == 'available'}">Sẵn sàng</option></select></label><input type="hidden" name="location" th:value="${location}"><input type="hidden" name="deliveryAddress" th:value="${deliveryAddress}"><input type="hidden" name="deliveryLat" th:value="${deliveryLat}"><input type="hidden" name="deliveryLng" th:value="${deliveryLng}"><button class="cw-btn primary">Tìm xe</button></form>'''
cards='''<p class="cw-muted" th:text="${#lists.size(motorbikes)+' xe phù hợp'}"></p><p class="cw-card" th:if="${#lists.isEmpty(motorbikes)}">Không có xe phù hợp. Hãy thử lịch hoặc bộ lọc khác.</p><div class="cw-grid"><article class="cw-card" th:each="m : ${motorbikes}"><img class="cw-hero-image" style="height:210px" th:src="${@display.image(m.imagePrimary)}" th:alt="${m.name}"><p class="cw-muted" th:text="${m.brand+' · '+m.category}"></p><h2 th:text="${m.name}"></h2><div class="cw-row"><span class="cw-price" th:text="${@display.money(m.pricePerDay)+' / ngày'}"></span><span class="cw-tag" th:text="${@display.status(m.status)}"></span></div><div class="cw-facts"><span th:text="${m.cc+' cc · '+m.year}"></span><span th:text="${m.rating+' / 5 · '+m.reviewCount+' đánh giá'}"></span></div><a class="cw-btn primary" data-preserve-query th:href="@{/customer/motorbike-detail/{id}(id=${m.id})}">Xem chi tiết</a></article></div>'''
page('motorbike-list','Chọn xe cho chuyến đi',filters+cards)
edit(R/'controller/web/CustomerPageController.java','model.addAttribute("keyword", keyword);','model.addAttribute("ccOptions",motorbikeService.getAllMotorbikes().stream().map(com.motorrent.motorbikerental.dto.response.MotorbikeResponse::getCc).filter(java.util.Objects::nonNull).distinct().sorted().toList());\n        model.addAttribute("keyword", keyword);')
edit(R/'service/MotorbikeService.java','case "price-asc":','case "popular":\n                    motorbikes.sort(java.util.Comparator.comparing(Motorbike::getRating,java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));\n                    break;\n                case "price-asc":')
p=T/'customer/home.html';s=p.read_text('utf-8')
s=re.sub(r'onsubmit="event.preventDefault\(\); const start=.*?"','id="home-search" method="get" action="/customer/motorbike-list"',s)
s=s.replace('name="pickup-datetime"','name="pickup"').replace('name="return-datetime"','name="returnAt"').replace('name="vehicle-type"','name="category"').replace('name="pickup-location"','name="location"')
s=re.sub(r' value="2026-[^"]+"','',s)
for a,b in [('ALL','all'),('SCOOTER','xe-ga'),('SEMI_AUTOMATIC','xe-so'),('MANUAL','xe-con-tay')]:s=s.replace('value="'+a+'"','value="'+b+'"')
s=re.sub(r'<!-- BEGIN: FleetShowcase -->.*?<!-- END: FleetShowcase -->','<section id="bang-gia-xe" class="customer-workspace"><h2>Đội xe của cửa hàng</h2>'+cards+'</section>',s,flags=re.S)
s=re.sub(r'<!-- BEGIN: CustomerTestimonials -->.*?<!-- END: CustomerTestimonials -->','''<section class="customer-workspace"><h2>Khách hàng chia sẻ</h2><p th:if="${#lists.isEmpty(recentReviews)}">Chưa có đánh giá.</p><article class="cw-card" th:each="r : ${recentReviews}"><strong th:text="${r.rating+' / 5'}"></strong><p th:text="${r.comment}"></p></article></section>''',s,flags=re.S)
s=re.sub(r'<!-- BEGIN: AIAssistantSection -->.*?<!-- END: AIAssistantSection -->','''<section class="customer-workspace" id="ai-assistant"><h2>Tìm xe theo nhu cầu</h2><p class="cw-muted">Xem các xe trong danh mục hiện tại, rồi chọn lịch thuê để kiểm tra khả dụng.</p><div class="cw-actions"><a class="cw-btn" href="/customer/motorbike-list?priceFilter=under120">Tiết kiệm chi phí</a><a class="cw-btn" href="/customer/motorbike-list?category=xe-ga">Xe ga đi phố</a><a class="cw-btn" href="/customer/motorbike-list?category=xe-con-tay">Xe côn tay</a></div></section>''',s,flags=re.S)
s=re.sub(r'<script data-purpose="ai-simulator">.*?</script>','',s,flags=re.S)
s=s.replace('</body>','''<script>document.getElementById('home-search').addEventListener('submit',function(){const q=new URLSearchParams(location.search);for(const k of ['deliveryAddress','deliveryLat','deliveryLng']){if(q.has(k)){let i=document.createElement('input');i.type='hidden';i.name=k;i.value=q.get(k);this.append(i);}}});</script><script src="/js/customer.js" defer></script></body>''')
p.write_text(s,encoding='utf-8')
# Keep the existing contract and OCR/map interactions; connect its fields and pricing to backend.
p=T/'customer/rental-contract.html';s=p.read_text('utf-8')
s=s.replace('<span class="text-on-surface">Honda Air Blade 160cc ABS (2024)</span>','<span class="text-on-surface" th:text="${motorbike.name}"></span>')
s=s.replace('<h1 class="font-headline-md text-headline-md text-on-surface">Honda Air Blade 160cc ABS</h1>','<h1 class="font-headline-md text-headline-md text-on-surface" th:text="${motorbike.name}"></h1>')
s=s.replace('<span class="font-headline-sm text-headline-sm text-primary-container">160.000đ</span>','<span class="font-headline-sm text-headline-sm text-primary-container" th:text="${@display.money(motorbike.pricePerDay)}"></span>')
s=re.sub(r'(<img\b[^>]*?)src="https://lh3.googleusercontent.com/[^"]+"',r'\1th:src="${@display.image(motorbike.imagePrimary)}"',s)
s=s.replace('let securityOption = 1;','''let securityOption = 1;
    const bookingBikeId=/*[[${motorbike.id}]]*/ null;
    const shopConfig=/*[[${shop}]]*/ {};
    const coordinates={pickup:null,return:null};
    let quoteVersion=0,quoteReady=false;
    function bookingRequest(){
      const value=id=>document.getElementById(id).value;
      const people=[1,2].slice(0,securityOption===2?2:1).map(i=>{let birth=value('p'+i+'_dob');if(birth.includes('/'))birth=birth.split('/').reverse().join('-');return {name:value('p'+i+'_name'),phone:value('p'+i+'_phone'),idCard:value('p'+i+'_cccd'),birthDate:birth,address:value('p'+i+'_address')};});
      return {motorbikeId:bookingBikeId,pickupDate:value('pickupTimeInput'),returnDate:value('returnTimeInput'),pickupType:value('pickupLocationSelect')==='store'?'STORE':'DELIVERY',returnType:value('returnLocationSelect')==='store'?'STORE':'DELIVERY',pickupAddress:value('pickupAddressInput'),returnAddress:value('returnAddressInput'),pickupLat:coordinates.pickup?.lat,pickupLng:coordinates.pickup?.lng,returnLat:coordinates.return?.lat,returnLng:coordinates.return?.lng,securityOption,people,termsAccepted:document.getElementById('termsAccepted').checked};
    }''')
s=s.replace('<input checked="" class="mt-1 w-4 h-4 rounded accent-primary-container" type="checkbox">','<input id="termsAccepted" class="mt-1 w-4 h-4 rounded accent-primary-container" type="checkbox">')
a=s.index('        const requestBody = {');b=s.index('        const btn =',a)
s=s[:a]+'''        const requestBody=bookingRequest();
        if(!quoteReady){alert('Vui lòng chờ tính giá hoặc kiểm tra thông tin giao nhận.');return;}
        if(!requestBody.termsAccepted){alert('Vui lòng đọc và đồng ý điều khoản thuê xe.');return;}
'''+s[b:]
s=s.replace('`/api/payment/create/${orderId}?amount=${amount}`','`/api/payment/create/${orderId}`')
s=s.replace('const motorbikeId = /*[[${motorbike.id}]]*/ 1;','const motorbikeId = bookingBikeId;')
s=s.replace('const STORE_LAT = 16.0597, STORE_LNG = 108.1717;','const STORE_LAT = shopConfig.lat, STORE_LNG = shopConfig.lng;')
s=s.replace('let fee = 15000;','let fee = shopConfig.deliveryBaseFee;').replace('fee = 15000;','fee = shopConfig.deliveryBaseFee;').replace('if (distKm > 3) fee += Math.ceil(distKm - 3) * 4000;','if (distKm > shopConfig.deliveryBaseKm) fee += Math.ceil(distKm - shopConfig.deliveryBaseKm) * shopConfig.deliveryPerKm;').replace(' * 1.3;',';')
s=s.replace('const deliveryLng = parseFloat(urlParams.get(\'deliveryLng\'));','const deliveryLng = parseFloat(urlParams.get(\'deliveryLng\'));\n            coordinates.pickup={lat:deliveryLat,lng:deliveryLng};coordinates.return={...coordinates.pickup};')
s=s.replace("document.getElementById('pickupTimeInput').value = nowStr;","document.getElementById('pickupTimeInput').value = urlParams.get('pickup') || nowStr;")
s=s.replace("document.getElementById('returnTimeInput').value = tomorrowStr;","document.getElementById('returnTimeInput').value = urlParams.get('returnAt') || tomorrowStr;")
s=s.replace('function onMapLocationConfirmed(targetId, addr, lat, lng, fee, dist) {','function onMapLocationConfirmed(targetId, addr, lat, lng, fee, dist) {\n        coordinates[targetId === "pickupAddressInput" ? "pickup" : "return"]={lat,lng};')
s=s.replace("returnSelect.value = 'maps_delivery';","returnSelect.value = 'maps_delivery';\n                coordinates.return={lat,lng};")
a=s.index('    function updateTotalCost() {');b=s.index('  </script>',a)
s=s[:a]+'''    async function updateTotalCost() {
        const seq=++quoteVersion;quoteReady=false;
        const button=document.getElementById('submitBookingBtn');button.disabled=true;
        const req=bookingRequest();if(!req.pickupDate || !req.returnDate)return;
        try {
            const response=await fetch('/api/rental-orders/quote',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(req)});
            const q=await response.json();if(seq!==quoteVersion)return;if(!response.ok)throw new Error(q.message || 'Không tính được giá');
            const money=n=>new Intl.NumberFormat('vi-VN').format(n)+' VNĐ';
            document.getElementById('sumRentalLabel').textContent='Giá thuê xe ('+q.days+' ngày):';
            document.getElementById('sumRentalCost').textContent=money(q.rentalFee);
            document.getElementById('sumDeliveryCost').textContent=money(q.deliveryFee);
            document.getElementById('sumSecurityDeposit').textContent=money(q.securityDeposit)+' (đối chiếu giấy tờ khi nhận xe)';
            document.getElementById('sumTotalCost').textContent=money(q.totalPrice+q.securityDeposit);
            document.getElementById('sumRemainingCost').textContent=money(q.totalPrice+q.securityDeposit-q.depositDue);
            document.getElementById('sumDepositAmount').textContent=money(q.depositDue);
            document.getElementById('sumDepositDesc').textContent='Khoản giữ xe và phí giao nhận theo cấu hình cửa hàng';
            document.getElementById('btnSubmitText').textContent='XÁC NHẬN & THANH TOÁN '+money(q.depositDue);
            window.currentDepositAmount=q.depositDue;quoteReady=true;button.disabled=false;
            document.getElementById('timeErrorMsg').classList.add('hidden');
        } catch(e) { if(seq===quoteVersion){const error=document.getElementById('timeErrorMsg');error.textContent=e.message;error.classList.remove('hidden');}}
    }
'''+s[b:]
# Populate the primary contact from current account; OCR can still fill these fields.
for key,expr in [('name',"customer.lastName + ' ' + customer.firstName"),('phone','customer.phone'),('address','customer.address'),('cccd','customer.idCard'),('dob',"customer.birthDate != null ? #temporals.format(customer.birthDate, 'dd/MM/yyyy') : ''")]:
 s=s.replace(f'id="p1_{key}"',f'id="p1_{key}" th:value="${{{expr}}}"')
s=s.replace('Đã xác thực','Đã đọc thông tin')
p.write_text(s,encoding='utf-8')
