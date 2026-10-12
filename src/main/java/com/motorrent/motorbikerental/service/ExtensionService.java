package com.motorrent.motorbikerental.service;

import com.motorrent.motorbikerental.entity.*;
import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.enums.RentalStatus;
import com.motorrent.motorbikerental.security.CurrentUser;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class ExtensionService {
 private final NotificationService notifications;
 private final AvailabilityService availability;
 private final RentalOrderRepository orders;
 private final RentalExtensionRepository extensions;
 private final PaymentRepository payments;
 private final PricingService pricing;
 private final CurrentUser current;
 private final RentalVehicleChangeRepository vehicleChanges;
 @PersistenceContext private EntityManager em;
 private static final java.security.SecureRandom CODES=new java.security.SecureRandom();
 private static boolean active(RentalOrder o){return List.of(RentalStatus.HANDED_OVER,RentalStatus.EXTENDED,RentalStatus.OVERDUE).contains(o.getStatus());}
 private boolean matches(RentalOrder o,RentalExtension e){return active(o)&&vehicleChanges.openDuring(o.getId(),AvailabilityService.now())==0&&Objects.equals(o.getMotorbike().getId(),e.getMotorbikeId())&&Objects.equals(o.getReturnDate(),e.getOriginalReturnDate())&&Objects.equals(o.getTotalPrice(),e.getOriginalTotal())&&Objects.equals(o.getDailyRate(),e.getDailyRate());}
 public String effectiveStatus(RentalExtension e){
  if(List.of("PENDING","WAITING_FOR_PAYMENT").contains(e.getStatus())&&!matches(e.getRentalOrder(),e))return "CANCELLED";
  if("WAITING_FOR_PAYMENT".equals(e.getStatus())&&(e.getExpiresAt()==null||!e.getExpiresAt().isAfter(AvailabilityService.now())))return "EXPIRED";
  return e.getStatus();
 }
 public Map<String,Object> view(RentalExtension e){return OperationsService.row("id",e.getId(),"newReturnDate",e.getNewReturnDate(),"originalReturnDate",e.getOriginalReturnDate(),"extraAmount",e.getExtraAmount(),"status",effectiveStatus(e),"reason",e.getReason(),"expiresAt",e.getExpiresAt(),"approvedAt",e.getApprovedAt(),"processedAt",e.getProcessedAt());}
 @Transactional(readOnly=true) public List<Map<String,Object>> forOrder(Long id){current.canRead(orders.findById(id).orElseThrow());return extensions.findByRentalOrderIdOrderByCreatedAtDesc(id).stream().map(this::view).toList();}
 private RentalExtension locked(Long id){var e=extensions.lockById(id).orElseThrow();em.refresh(e,LockModeType.PESSIMISTIC_WRITE);return e;}
 private void requireSchedule(RentalOrder o,RentalExtension e){
  if(!e.getNewReturnDate().isAfter(AvailabilityService.now()))throw new IllegalArgumentException("Ngày trả mới phải ở tương lai");
  if(!availability.availableLocked(o.getMotorbike(),o.getId(),e.getOriginalReturnDate(),e.getNewReturnDate()))throw new IllegalArgumentException("Xe hiện tại không còn trống trong khoảng gia hạn. Vui lòng liên hệ cửa hàng theo hotline "+Objects.toString(pricing.settings().getPhone(),"")+" để được hỗ trợ đổi xe.");
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) public void request(Long id,LocalDateTime end,String reason){
  var o=availability.lockOrder(id);current.owns(o);
  if(vehicleChanges.openDuring(id,AvailabilityService.now())>0)throw new IllegalArgumentException("Đang có phương án đổi xe; liên hệ cửa hàng để xử lý trước khi gia hạn");
  if(!active(o))throw new IllegalArgumentException("Chỉ được gia hạn khi đang thuê xe");
  if(reason==null||reason.isBlank()||reason.length()>1000)throw new IllegalArgumentException("Nhập lý do, tối đa 1000 ký tự");
  int days=PricingService.days(o.getReturnDate(),end);
  for(var old:extensions.lockOpen(id)){em.refresh(old,LockModeType.PESSIMISTIC_WRITE);String state=effectiveStatus(old);if(List.of("PENDING","WAITING_FOR_PAYMENT").contains(state))throw new IllegalArgumentException("Bạn đã có yêu cầu gia hạn đang xử lý");old.setStatus(state);old.setProcessedAt(AvailabilityService.now());}
  int rate=Objects.requireNonNullElse(o.getDailyRate(),o.getMotorbike().getPricePerDay());
  // Snapshot the nullable original rate too, so legacy orders remain comparable.
  var e=RentalExtension.builder().rentalOrder(o).requestedBy(current.get()).motorbikeId(o.getMotorbike().getId()).dailyRate(o.getDailyRate()).originalTotal(o.getTotalPrice()).originalReturnDate(o.getReturnDate()).newReturnDate(end).extraDays(days).extraAmount(Math.multiplyExact(days,rate)).reason(reason.trim()).build();
  if(e.getExtraAmount()<=0)throw new IllegalArgumentException("Phí gia hạn chưa hợp lệ; liên hệ cửa hàng");
  requireSchedule(o,e);extensions.save(e);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) public Map<String,Object> decide(Long id,boolean approve){
  var actor=current.operator();var o=availability.lockOrder(extensions.orderIdFor(id).orElseThrow());var e=locked(id);
  if(!"PENDING".equals(e.getStatus()))throw new IllegalArgumentException("Yêu cầu đã được xử lý");
  if(approve){
   if(!matches(o,e))throw new IllegalArgumentException("Đơn thuê đã thay đổi, cần tạo yêu cầu mới");
   requireSchedule(o,e);e.setStatus("WAITING_FOR_PAYMENT");e.setExpiresAt(AvailabilityService.now().plusMinutes(pricing.settings().getHoldMinutes()));
  }else{e.setStatus("REJECTED");e.setProcessedAt(AvailabilityService.now());}
  e.setApprovedBy(actor);e.setApprovedAt(AvailabilityService.now());
  notifications.send(o,"Yêu cầu gia hạn "+o.getOrderCode(),approve?"Yêu cầu đã được chấp nhận. Vui lòng thanh toán phí gia hạn trước hạn. Ngày trả chỉ thay đổi sau khi thanh toán thành công.":"Yêu cầu gia hạn bị từ chối. Vui lòng trả xe theo lịch đã xác nhận.");
  return view(e);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) public Payment preparePayment(Long id){
  var o=availability.lockOrder(extensions.orderIdFor(id).orElseThrow());current.owns(o);var e=locked(id);
  if(!"WAITING_FOR_PAYMENT".equals(effectiveStatus(e)))throw new IllegalArgumentException("Yêu cầu gia hạn không còn trong thời gian thanh toán");
  requireSchedule(o,e);
  var existing=payments.lockForOrder(o.getId()).stream().filter(p->"EXTENSION".equals(p.getSourceType())&&id.equals(p.getSourceId())).findFirst();
  if(existing.isPresent()){if(!"PENDING".equals(existing.get().getStatus()))throw new IllegalArgumentException("Giao dịch đã được xử lý");return existing.get();}
  var p=Payment.builder().rentalOrder(o).amount(e.getExtraAmount()).transactionType("EXTENSION_PAYMENT").purpose("EXTENSION_PAYMENT").direction("IN").paymentMethod("PAYOS").sourceType("EXTENSION").sourceId(id).expiresAt(e.getExpiresAt()).idempotencyKey("EXTENSION:"+id).componentsJson(Financials.components(Map.of("EXTENSION_PAYMENT",e.getExtraAmount()),e.getExtraAmount())).build();
  long code;do{code=1+CODES.nextLong(4_503_599_627_370_495L);}while(payments.findByProviderCode(code).isPresent());p.setProviderCode(code);return payments.saveAndFlush(p);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) public void applyPayment(Long code,Integer amount,String link,String reference){
  Long id=payments.sourceIdForCode(code).orElseThrow();var o=availability.lockOrder(extensions.orderIdFor(id).orElseThrow());var e=locked(id);var p=payments.lockByProviderCode(code).orElseThrow();em.refresh(p,LockModeType.PESSIMISTIC_WRITE);
  if(!"PENDING".equals(p.getStatus()))return;
  if(!"EXTENSION".equals(p.getSourceType())||!Objects.equals(p.getSourceId(),id)||!Objects.equals(p.getRentalOrder().getId(),o.getId()))throw new IllegalArgumentException("Nguồn thanh toán không khớp");
  if(link==null||link.isBlank()||p.getProviderLinkId()!=null&&!Objects.equals(p.getProviderLinkId(),link))throw new IllegalArgumentException("Mã giao dịch không khớp");
  if(amount==null||amount<0)throw new IllegalArgumentException("Số tiền nhận không hợp lệ");
  var now=AvailabilityService.now();p.setReceivedAmount(amount);p.setPaidAt(now);p.setProcessedAt(now);p.setProviderLinkId(link);p.setTransactionCode(reference==null||reference.isBlank()?"PAYOS-"+code:reference);
  if(!Objects.equals(amount,p.getAmount())||!Objects.equals(amount,e.getExtraAmount())||!"WAITING_FOR_PAYMENT".equals(effectiveStatus(e))||p.getExpiresAt()==null||!p.getExpiresAt().isAfter(now)||!e.getNewReturnDate().isAfter(now)||!availability.availableLocked(o.getMotorbike(),o.getId(),e.getOriginalReturnDate(),e.getNewReturnDate())){
   p.setStatus("REVIEW_REQUIRED");e.setStatus("REVIEW_REQUIRED");e.setProcessedAt(now);return;
  }
  // One transaction posts the receipt and advances only the approved segment.
  int oldTotal=o.getTotalPrice();o.setReturnDate(e.getNewReturnDate());o.setDays(Math.addExact(o.getDays(),e.getExtraDays()));o.setTotalPrice(Math.addExact(oldTotal,amount));o.setRentalFee(Math.addExact(Objects.requireNonNullElse(o.getRentalFee(),oldTotal-Objects.requireNonNullElse(o.getDeliveryFee(),0)),amount));o.setBalancePaid(Math.addExact(Objects.requireNonNullElse(o.getBalancePaid(),0),amount));o.setStatus(RentalStatus.EXTENDED);
  p.setStatus("PAID");e.setStatus("PAID");e.setProcessedAt(now);orders.save(o);
  notifications.send(o,"Đã thanh toán gia hạn "+o.getOrderCode(),"Ngày trả mới đã được xác nhận: "+e.getNewReturnDate()+". Khoản thanh toán được lưu trong chi tiết đơn.");
 }
}
