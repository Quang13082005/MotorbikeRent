from pathlib import Path
R=Path('src/main/java/com/motorrent/motorbikerental')
def edit(path,a,b):
 p=R/path;s=p.read_text('utf-8');assert a in s,path;p.write_text(s.replace(a,b),encoding='utf-8')
edit('repository/PromotionRepository.java','public interface PromotionRepository extends JpaRepository<Promotion, Long> {','''public interface PromotionRepository extends JpaRepository<Promotion, Long> {
 java.util.Optional<Promotion> findByCode(String code);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select p from Promotion p where p.code=:code")
 java.util.Optional<Promotion> lockByCode(@org.springframework.data.repository.query.Param("code") String code);''')
edit('entity/RentalOrder.java','Integer rentalFee;','Integer rentalFee;\n    String promotionCode;\n    Integer discountAmount;')
edit('service/PricingService.java','private final ShopSettingsRepository settings;','private final ShopSettingsRepository settings;\n private final PromotionRepository promotions;')
edit('service/PricingService.java','int security=Integer.valueOf(2)', '''int discount=0;
  if(r.getPromotionCode()!=null && !r.getPromotionCode().isBlank()) {
   var p=promotions.findByCode(r.getPromotionCode().trim().toUpperCase(Locale.ROOT)).orElseThrow(()->new IllegalArgumentException("Mã ưu đãi không tồn tại"));
   var now=LocalDateTime.now();if(!"ACTIVE".equals(p.getStatus()) || p.getStartDate()==null || p.getEndDate()==null || now.isBefore(p.getStartDate()) || now.isAfter(p.getEndDate()) || rental<Objects.requireNonNullElse(p.getMinOrderAmount(),0) || p.getUsageLimit()!=null && Objects.requireNonNullElse(p.getUsedCount(),0)>=p.getUsageLimit())throw new IllegalArgumentException("Mã ưu đãi chưa áp dụng, đã hết hạn hoặc hết lượt");
   discount="PERCENTAGE".equals(p.getDiscountType())?Math.toIntExact((long)rental*p.getDiscountValue()/100):p.getDiscountValue();
   discount=Math.min(Math.max(0,rental-1000),Math.min(discount,Objects.requireNonNullElse(p.getMaxDiscount(),rental)));
  }
  int security=Integer.valueOf(2)''')
edit('service/PricingService.java','(long)rental*settings().getDepositPercent()/100','(long)(rental-discount)*settings().getDepositPercent()/100')
edit('service/PricingService.java','"totalPrice",Math.addExact(rental,delivery),','"totalPrice",Math.addExact(rental-discount,delivery),"discountAmount",discount,')
edit('service/RentalOrderService.java','private final PricingService pricing;','private final PricingService pricing;\n    private final com.motorrent.motorbikerental.repository.PromotionRepository promotions;')
edit('service/RentalOrderService.java','var q=pricing.quote(request,motorbike);','''com.motorrent.motorbikerental.entity.Promotion promotion=null;
        if(request.getPromotionCode()!=null && !request.getPromotionCode().isBlank()) promotion=promotions.lockByCode(request.getPromotionCode().trim().toUpperCase(java.util.Locale.ROOT)).orElseThrow(()->new IllegalArgumentException("Mã ưu đãi không tồn tại"));
        var q=pricing.quote(request,motorbike);
        if(promotion!=null){promotion.setUsedCount(java.util.Objects.requireNonNullElse(promotion.getUsedCount(),0)+1);promotions.save(promotion);}''')
edit('service/RentalOrderService.java','.rentalFee((Integer)q.get("rentalFee"))','.promotionCode(promotion==null?null:promotion.getCode()).discountAmount((Integer)q.get("discountAmount"))\n                .rentalFee((Integer)q.get("rentalFee"))')
p=Path('src/main/resources/templates/customer/rental-contract.html');s=p.read_text('utf-8');s=s.replace('<!-- Main Grid','<!-- Main Grid')
s=s.replace('id="sumRentalLabel"','id="sumRentalLabel"',1)
i=s.index('<button id="submitBookingBtn"');s=s[:i]+'''<label class="cw-field">Mã khuyến mãi<input id="promotionCode" maxlength="40" onchange="updateTotalCost()" placeholder="Nhập mã nếu có"></label><p id="promotionSaving" class="cw-muted"></p>'''+s[i:]
s=s.replace('securityOption,people,termsAccepted:','securityOption,people,promotionCode:document.getElementById("promotionCode").value,termsAccepted:')
s=s.replace("const money=n=>new Intl.NumberFormat('vi-VN').format(n)+' VNĐ';","const money=n=>new Intl.NumberFormat('vi-VN').format(n)+' VNĐ';\n            document.getElementById('promotionSaving').textContent=q.discountAmount ? 'Đã giảm '+money(q.discountAmount) : '';")
p.write_text(s,encoding='utf-8')
