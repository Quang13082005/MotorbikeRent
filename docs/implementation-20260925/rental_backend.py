exec(Path('docs/implementation-20260925/foundation.py').read_text('utf-8').split("edit('config/DatabaseSeeder.java'")[0]) if False else None
from pathlib import Path
R=Path('src/main/java/com/motorrent/motorbikerental')
def put(p,s): (R/p).write_text(s,encoding='utf-8')
def edit(p,a,b):
 f=R/p; s=f.read_text('utf-8'); assert a in s,(p,a); f.write_text(s.replace(a,b),encoding='utf-8')
edit('entity/User.java','String address;','String address;\n    java.time.LocalDate birthDate;')
edit('dto/response/UserResponse.java','String phone;','String phone;\n    String address;\n    Boolean gender;\n    java.time.LocalDate birthDate;')
fields='''Integer rentalFee;
    Integer deliveryFee;
    Integer securityDeposit;
    Integer depositDue;
    Integer dailyRate;
    Integer balancePaid;
    String returnType;
    String returnAddress;
    LocalDateTime paymentExpiresAt;'''
edit('entity/RentalOrder.java','Integer totalPrice;','Integer totalPrice;\n    '+fields)
edit('dto/response/RentalOrderResponse.java','Integer depositPaid;','Integer depositPaid;\n    '+fields)
edit('dto/response/RentalDetailResponse.java','Integer totalPrice;','Integer totalPrice;\n    Integer depositPaid;\n    '+fields)
edit('dto/request/RentalOrderRequest.java','Long motorbikeId;','@jakarta.validation.constraints.NotNull\n    Long motorbikeId;')
for a in ['pickupDate','returnDate']:
 edit('dto/request/RentalOrderRequest.java',f'LocalDateTime {a};',f'@jakarta.validation.constraints.NotNull\n    LocalDateTime {a};')
edit('dto/request/RentalOrderRequest.java','Integer depositAmount;','''Integer depositAmount;
    String returnType;
    String returnAddress;
    Double pickupLat; Double pickupLng;
    Double returnLat; Double returnLng;
    Integer securityOption;
    String promotionCode;''')
edit('repository/MotorbikeRepository.java','public interface MotorbikeRepository extends JpaRepository<Motorbike, Long> {','''public interface MotorbikeRepository extends JpaRepository<Motorbike, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Motorbike m where m.id=:id")
    java.util.Optional<Motorbike> lockById(@Param("id") Long id);''')
edit('repository/RentalOrderRepository.java','public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {','''public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RentalOrder r where r.id=:id")
    java.util.Optional<RentalOrder> lockById(@Param("id") Long id);
    @Query("select count(r) from RentalOrder r where r.motorbike.id=:bike and r.id<>:excluded and r.status not in ('CANCELLED','REJECTED','COMPLETED') and r.pickupDate<:end and r.returnDate>:start")
    long overlapping(@Param("bike") Long bike,@Param("excluded") Long excluded,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);
''')
put('entity/ShopSettings.java','''package com.motorrent.motorbikerental.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="shop_settings") @Data @NoArgsConstructor
public class ShopSettings {
 @Id private Long id=1L;
 private String name,address,phone,hours,zalo,email;
 @Column(length=4000) private String terms;
 private Double lat,lng;
 private Integer deliveryBaseFee,deliveryPerKm,deliveryBaseKm,deliveryMaxKm,holdMinutes,depositPercent;
}
''')
put('repository/ShopSettingsRepository.java','''package com.motorrent.motorbikerental.repository;
public interface ShopSettingsRepository extends org.springframework.data.jpa.repository.JpaRepository<com.motorrent.motorbikerental.entity.ShopSettings,Long> {}
''')
put('service/PricingService.java','''package com.motorrent.motorbikerental.service;
import com.motorrent.motorbikerental.entity.*; import com.motorrent.motorbikerental.repository.*; import com.motorrent.motorbikerental.dto.request.RentalOrderRequest;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import java.time.*; import java.util.*;
@Service @RequiredArgsConstructor
public class PricingService {
 private final ShopSettingsRepository settings;
 public ShopSettings settings() { return settings.findById(1L).orElseThrow(()->new IllegalArgumentException("Cửa hàng chưa cấu hình giá và giao nhận")); }
 public static int days(LocalDateTime start,LocalDateTime end) {
  if(start==null || end==null || !end.isAfter(start)) throw new IllegalArgumentException("Ngày trả phải sau ngày nhận");
  long seconds=Duration.between(start,end).getSeconds();
  if(seconds>366L*86400) throw new IllegalArgumentException("Thời gian thuê tối đa 366 ngày");
  return Math.toIntExact((seconds+86399)/86400);
 }
 public int delivery(String type,String address,Double lat,Double lng) {
  if(type==null || "STORE".equals(type)) return 0;
  if(!"DELIVERY".equals(type) || address==null || address.isBlank() || lat==null || lng==null || !Double.isFinite(lat) || !Double.isFinite(lng) || Math.abs(lat)>90 || Math.abs(lng)>180) throw new IllegalArgumentException("Chọn địa chỉ và tọa độ giao nhận hợp lệ");
  var s=settings(); if(s.getLat()==null || s.getLng()==null) throw new IllegalArgumentException("Chưa cấu hình tọa độ cửa hàng");
  double a=Math.pow(Math.sin(Math.toRadians(lat-s.getLat())/2),2)+Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(s.getLat()))*Math.pow(Math.sin(Math.toRadians(lng-s.getLng())/2),2);
  double km=6371*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
  if(km>s.getDeliveryMaxKm()) throw new IllegalArgumentException("Địa chỉ ngoài phạm vi giao nhận của cửa hàng");
  return Math.addExact(s.getDeliveryBaseFee(),Math.multiplyExact((int)Math.ceil(Math.max(0,km-s.getDeliveryBaseKm())),s.getDeliveryPerKm()));
 }
 public Map<String,Object> quote(RentalOrderRequest r,Motorbike bike) {
  if(r.getPickupDate()==null || r.getPickupDate().isBefore(LocalDateTime.now().minusMinutes(1))) throw new IllegalArgumentException("Ngày nhận không được ở quá khứ");
  int days=days(r.getPickupDate(),r.getReturnDate());
  int rental=Math.multiplyExact(days,bike.getPricePerDay());
  int delivery=Math.addExact(delivery(r.getPickupType(),r.getPickupAddress(),r.getPickupLat(),r.getPickupLng()),delivery(r.getReturnType(),r.getReturnAddress(),r.getReturnLat(),r.getReturnLng()));
  int security=Integer.valueOf(2).equals(r.getSecurityOption())?0:Objects.requireNonNullElse(bike.getDepositAmount(),0);
  int due=Math.addExact(Math.toIntExact((long)rental*settings().getDepositPercent()/100),delivery);
  return Map.of("days",days,"dailyRate",bike.getPricePerDay(),"rentalFee",rental,"deliveryFee",delivery,"securityDeposit",security,"depositDue",due,"totalPrice",Math.addExact(rental,delivery),"holdMinutes",settings().getHoldMinutes());
 }
}
''')
f=R/'service/RentalOrderService.java'; t=f.read_text('utf-8')
t=t.replace('private final UserRepository userRepository;','''private final UserRepository userRepository;
    private final com.motorrent.motorbikerental.security.CurrentUser current;
    private final PricingService pricing;
    private final com.motorrent.motorbikerental.repository.RentalContractRepository contracts;
    public RentalOrder requireReadable(Long id) { var o=rentalOrderRepository.findById(id).orElseThrow(()->new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND)); current.canRead(o); return o; }
    public RentalOrder requireOwned(Long id) { var o=requireReadable(id); current.owns(o); return o; }
    public java.util.Map<String,Object> quote(RentalOrderRequest r) { return pricing.quote(r,motorbikeRepository.findById(r.getMotorbikeId()).orElseThrow(()->new AppException(ErrorCode.MOTORBIKE_NOT_FOUND))); }
''')
a=t.index('        if (request.getPickupDate().isAfter'); b=t.index('        RentalOrder order = RentalOrder.builder()',a)
t=t[:a]+'''        Motorbike motorbike = motorbikeRepository.lockById(request.getMotorbikeId()).orElseThrow(() -> new AppException(ErrorCode.MOTORBIKE_NOT_FOUND));
        if (motorbike.getStatus() == com.motorrent.motorbikerental.enums.MotorbikeStatus.MAINTENANCE || motorbike.getStatus() == com.motorrent.motorbikerental.enums.MotorbikeStatus.UNAVAILABLE || rentalOrderRepository.existsOverlappingOrder(motorbike.getId(),request.getPickupDate(),request.getReturnDate())) throw new AppException(ErrorCode.MOTORBIKE_NOT_AVAILABLE);
        var q=pricing.quote(request,motorbike);
        User customer=userRepository.findById(customerId).orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        int days=(Integer)q.get("days"), totalPrice=(Integer)q.get("totalPrice");
''' +t[b:]
t=t.replace('.totalPrice(totalPrice)','.totalPrice(totalPrice)\n                .rentalFee((Integer)q.get("rentalFee")).deliveryFee((Integer)q.get("deliveryFee")).securityDeposit((Integer)q.get("securityDeposit"))\n                .dailyRate(motorbike.getPricePerDay()).depositDue((Integer)q.get("depositDue")).balancePaid(0)\n                .returnType(request.getReturnType()==null?"STORE":request.getReturnType()).returnAddress(request.getReturnAddress())\n                .paymentExpiresAt(LocalDateTime.now().plusMinutes((Integer)q.get("holdMinutes")))')
t=t.replace('rentalOrderRepository.findById(orderId)','rentalOrderRepository.lockById(orderId)')
# detail is read-only, no write lock
t=t.replace('public RentalDetailResponse getOrderDetail(Long orderId) {\n        RentalOrder order = rentalOrderRepository.lockById','public RentalDetailResponse getOrderDetail(Long orderId) {\n        RentalOrder order = rentalOrderRepository.findById')
t=t.replace('order.setStatus(RentalStatus.HANDED_OVER);','''order.setStatus(RentalStatus.HANDED_OVER);
        order.getMotorbike().setStatus(com.motorrent.motorbikerental.enums.MotorbikeStatus.RENTED);
        if(contracts.findByRentalOrderId(orderId).isEmpty()) contracts.save(com.motorrent.motorbikerental.entity.RentalContract.builder().rentalOrder(order).contractCode("HD-"+order.getOrderCode()).odoBefore(order.getMotorbike().getOdoKm()).build());''')
t=t.replace('if (order.getStatus() != RentalStatus.HANDED_OVER) {','if (!java.util.List.of(RentalStatus.HANDED_OVER,RentalStatus.EXTENDED,RentalStatus.OVERDUE,RentalStatus.RETURN_REQUESTED,RentalStatus.RETURNED).contains(order.getStatus())) {')
t=t.replace('order.setStatus(RentalStatus.COMPLETED);','''if (java.util.Objects.requireNonNullElse(order.getDepositPaid(),0)+java.util.Objects.requireNonNullElse(order.getBalancePaid(),0)<order.getTotalPrice()) throw new IllegalArgumentException("Cần ghi nhận đủ tiền thuê trước khi hoàn tất");
        order.setActualReturnDate(LocalDateTime.now());
        order.getMotorbike().setStatus(com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE);
        order.setStatus(RentalStatus.COMPLETED);''')
a=t.index('    @Transactional\n    public RentalOrderResponse rejectOrder(Long orderId, String staffId, String reason)'); b=t.index('    @org.springframework.scheduling',a)
t=t[:a]+'''    @Transactional
    public RentalOrderResponse rejectOrder(Long orderId, String staffId, String reason) {
        if(reason==null || reason.isBlank()) throw new IllegalArgumentException("Nhập lý do từ chối");
        var request=new RejectRentalOrderRequest(); request.setReason(reason.trim());
        return rejectOrder(orderId,request,staffId);
    }

'''+t[b:]
t=t.replace('o.getCreatedAt() != null && o.getCreatedAt().isBefore(expiryTime)','o.getPaymentExpiresAt() != null && o.getPaymentExpiresAt().isBefore(LocalDateTime.now())')
t=t.replace('for (RentalOrder order : expiredOrders) {','for (RentalOrder candidate : expiredOrders) {\n            RentalOrder order=rentalOrderRepository.lockById(candidate.getId()).orElseThrow();\n            if(order.getStatus()!=RentalStatus.PENDING_PAYMENT) continue;')
f.write_text(t,encoding='utf-8')
edit('repository/RentalContractRepository.java','public interface RentalContractRepository extends JpaRepository<RentalContract, Long> {','public interface RentalContractRepository extends JpaRepository<RentalContract, Long> {\n    java.util.Optional<RentalContract> findByRentalOrderId(Long id);')
edit('repository/MotorbikeImageRepository.java','public interface MotorbikeImageRepository extends JpaRepository<MotorbikeImage, Long> {','public interface MotorbikeImageRepository extends JpaRepository<MotorbikeImage, Long> {\n    java.util.List<MotorbikeImage> findByMotorbikeIdOrderBySortOrderAsc(Long id);')
edit('entity/Motorbike.java','String licensePlate;','String licensePlate;\n\n    @OneToMany(mappedBy="motorbike")\n    java.util.List<MotorbikeImage> images;\n\n    public String getImagePrimary() {\n        if(images==null || images.isEmpty()) return "/images/motorbike-placeholder.svg";\n        String url=images.stream().filter(i->Boolean.TRUE.equals(i.getIsPrimary())).findFirst().orElse(images.get(0)).getImageUrl();\n        if(url==null || url.isBlank()) return "/images/motorbike-placeholder.svg";\n        return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/") ? url : "/"+url;\n    }')
edit('service/MotorbikeService.java','return motorbikeMapper.toDetailResponse(motorbike);','var result=motorbikeMapper.toDetailResponse(motorbike);\n        result.setAllImages(motorbike.getImages()==null?java.util.List.of():motorbike.getImages().stream().map(com.motorrent.motorbikerental.entity.MotorbikeImage::getImageUrl).toList());\n        return result;')
