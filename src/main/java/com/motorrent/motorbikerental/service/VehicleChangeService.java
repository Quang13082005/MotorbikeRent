package com.motorrent.motorbikerental.service;

import com.motorrent.motorbikerental.entity.*;
import com.motorrent.motorbikerental.enums.*;
import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.dto.request.VehicleChangeRequest;
import com.motorrent.motorbikerental.security.CurrentUser;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Service @RequiredArgsConstructor
public class VehicleChangeService {
 private final RentalOrderRepository orders;
 private final MotorbikeRepository bikes;
 private final RentalVehicleChangeRepository changes;
 private final PaymentRepository payments;
 private final PricingService pricing;
 private final AvailabilityService availability;
 private final CurrentUser current;
 private final NotificationService notifications;
 @PersistenceContext private EntityManager em;
 private static final java.security.SecureRandom CODES=new java.security.SecureRandom();
 private static int n(Integer v){return Objects.requireNonNullElse(v,0);}
 private static boolean before(RentalOrder o){return List.of(RentalStatus.PENDING,RentalStatus.CONFIRMED,RentalStatus.READY_FOR_HANDOVER).contains(o.getStatus());}
 private void requireBefore(RentalOrder o){if(!before(o))throw new IllegalArgumentException("Chỉ đổi xe trước khi bàn giao đơn đã thanh toán booking");if(!o.getReturnDate().isAfter(AvailabilityService.now()))throw new IllegalArgumentException("Lịch thuê đã kết thúc, cần xử lý lại lịch với khách");}
 private void reason(String reason){if(reason==null||reason.isBlank()||reason.length()>1000)throw new IllegalArgumentException("Nhập lý do, tối đa 1000 ký tự");}
 // Vehicle mutexes serialize changes; READ_COMMITTED prevents stale snapshots after waiting.
 private RentalOrder lock(Long orderId,Long target){
  Long original=orders.motorbikeIdFor(orderId).orElseThrow();
  var ids=new TreeSet<Long>();ids.add(original);if(target!=null)ids.add(target);
  for(Long id:ids){var b=bikes.lockById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy xe"));em.refresh(b,LockModeType.PESSIMISTIC_WRITE);}
  var o=orders.lockById(orderId).orElseThrow();em.refresh(o,LockModeType.PESSIMISTIC_WRITE);
  if(!original.equals(o.getMotorbike().getId())&&!Objects.equals(target,o.getMotorbike().getId()))throw new IllegalArgumentException("Xe của đơn vừa thay đổi, vui lòng tải lại");
  return o;
 }
 private RentalVehicleChange lockedChange(Long id){var c=changes.lockById(id).orElseThrow();em.refresh(c,LockModeType.PESSIMISTIC_WRITE);return c;}
 private void clearExpired(RentalOrder o){
  for(var c:changes.lockPending(o.getId())){em.refresh(c,LockModeType.PESSIMISTIC_WRITE);if(!c.getExpiresAt().isAfter(AvailabilityService.now())){c.setStatus("EXPIRED");c.setProcessedAt(AvailabilityService.now());}else throw new IllegalArgumentException("Đơn đang có đề nghị đổi xe chờ thanh toán; hãy hủy đề nghị đó trước");}
 }
 private boolean twoDocuments(RentalOrder o){
  try{var value=new com.fasterxml.jackson.databind.ObjectMapper().readTree(o.getIdentitySnapshot());if(value!=null&&value.isArray())return value.size()==2;}catch(Exception ignored){}
  return o.getSecurityDeposit()!=null && o.getSecurityDeposit()==0;
 }
 private RentalVehicleChange quote(RentalOrder o,Motorbike b,VehicleChangeRequest r){
  if(o.getMotorbike().getId().equals(b.getId()))throw new IllegalArgumentException("Chọn xe khác xe hiện tại");
  var c=new RentalVehicleChange();c.setRentalOrder(o);c.setOldVehicle(o.getMotorbike());c.setNewVehicle(b);
  c.setOldVehicleName(o.getMotorbike().getName());c.setNewVehicleName(b.getName());c.setOldPlate(o.getMotorbike().getLicensePlate());c.setNewPlate(b.getLicensePlate());
  c.setOldDailyRate(o.getDailyRate());c.setNewDailyRate(b.getPricePerDay());c.setOldRentalFee(o.getRentalFee());c.setOldDeliveryFee(o.getDeliveryFee());c.setOldTotal(o.getTotalPrice());c.setOldSecurityDeposit(o.getSecurityDeposit());
  c.setPlannedPickup(o.getPickupDate());c.setPlannedReturn(o.getReturnDate());
  c.setNewRentalFee(Math.multiplyExact(PricingService.days(o.getPickupDate(),o.getReturnDate()),b.getPricePerDay()));
  c.setNewPickupType(o.getPickupType());c.setNewPickupAddress(o.getPickupAddress());c.setNewPickupLat(o.getPickupLat());c.setNewPickupLng(o.getPickupLng());
  c.setNewReturnType(o.getReturnType());c.setNewReturnAddress(o.getReturnAddress());c.setNewReturnLat(o.getReturnLat());c.setNewReturnLng(o.getReturnLng());
  c.setNewPickupFee(o.getPickupFee());c.setNewReturnFee(o.getReturnFee());c.setNewDeliveryFee(n(o.getDeliveryFee()));
  if(r.pickupType()!=null||r.returnType()!=null){
   if(r.pickupType()!=null){c.setNewPickupType(r.pickupType());c.setNewPickupAddress(r.pickupAddress());c.setNewPickupLat(r.pickupLat());c.setNewPickupLng(r.pickupLng());c.setNewPickupFee(pricing.delivery(r.pickupType(),r.pickupAddress(),r.pickupLat(),r.pickupLng()));}
   if(r.returnType()!=null){c.setNewReturnType(r.returnType());c.setNewReturnAddress(r.returnAddress());c.setNewReturnLat(r.returnLat());c.setNewReturnLng(r.returnLng());c.setNewReturnFee(pricing.delivery(r.returnType(),r.returnAddress(),r.returnLat(),r.returnLng()));}
   if(c.getNewPickupFee()==null){if("STORE".equals(c.getNewPickupType()))c.setNewPickupFee(0);else throw new IllegalArgumentException("Đơn cũ thiếu phí từng chặng; chọn lại cả hai địa điểm để báo giá chính xác");}
   if(c.getNewReturnFee()==null){if("STORE".equals(c.getNewReturnType()))c.setNewReturnFee(0);else throw new IllegalArgumentException("Đơn cũ thiếu phí từng chặng; chọn lại cả hai địa điểm để báo giá chính xác");}
   c.setNewDeliveryFee(Math.addExact(c.getNewPickupFee(),c.getNewReturnFee()));
  }
  c.setNewTotal(Math.addExact(c.getNewRentalFee(),c.getNewDeliveryFee()));
  c.setNewSecurityDeposit(twoDocuments(o)?0:PricingService.securityDeposit(b));
  c.setNewDepositDue(Math.addExact(Math.toIntExact((long)c.getNewRentalFee()*pricing.settings().getDepositPercent()/100),c.getNewDeliveryFee()));
  c.setPaidBefore(n(o.getDepositPaid()));c.setBalancePaidBefore(n(o.getBalancePaid()));
  int paid=Math.addExact(c.getPaidBefore(),c.getBalancePaidBefore());
  c.setAdditionalPayment(Math.max(0,c.getNewDepositDue()-paid));c.setCreditAmount(Math.max(0,paid-c.getNewTotal()));
  return c;
 }
 public Map<String,Object> view(RentalVehicleChange c){
  var result=OperationsService.row("id",c.getId(),"orderId",c.getRentalOrder().getId(),"type",c.getType(),"status",c.getStatus(),"oldVehicleId",c.getOldVehicle().getId(),"newVehicleId",c.getNewVehicle().getId(),"oldVehicle",c.getOldVehicleName(),"newVehicle",c.getNewVehicleName(),"oldPlate",c.getOldPlate(),"newPlate",c.getNewPlate(),"oldTotal",c.getOldTotal(),"newTotal",c.getNewTotal(),"newRentalFee",c.getNewRentalFee(),"newDeliveryFee",c.getNewDeliveryFee(),"newDailyRate",c.getNewDailyRate(),"newSecurityDeposit",c.getNewSecurityDeposit(),"paidBefore",n(c.getPaidBefore())+n(c.getBalancePaidBefore()),"additionalPayment",c.getAdditionalPayment(),"creditAmount",c.getCreditAmount(),"remaining",Math.max(0,c.getNewTotal()-n(c.getPaidBefore())-n(c.getBalancePaidBefore())-n(c.getAdditionalPayment())),"reason",c.getReason(),"staffId",c.getProcessedByStaffId(),"createdAt",c.getCreatedAt(),"processedAt",c.getProcessedAt(),"expiresAt",c.getExpiresAt(),"cancelledByStaffId",c.getCancelledByStaffId(),"refundedAmount",c.getRefundedAmount(),"refundedAt",c.getRefundedAt(),"refundedByStaffId",c.getRefundedByStaffId());
  boolean during="DURING_RENTAL".equals(c.getType());
  if(during){
   int held=AssetDepositAmounts.held(payments.findByRentalOrderIdOrderByCreatedAtDesc(c.getRentalOrder().getId()));
   result.putAll(OperationsService.row("extensionStartTime",c.getExtensionStartTime(),"newPlannedReturnTime",c.getNewPlannedReturnTime(),"extraRentalAmount",c.getExtraRentalAmount(),"vehicleChangeFee",c.getVehicleChangeFee(),"exchangeAddress",c.getExchangeAddress(),"actualVehicleChangeTime",c.getActualVehicleChangeTime(),"changedByStaffId",c.getChangedByStaffId(),"additionalDepositAmount",c.getAdditionalDepositAmount(),"heldDeposit",held,"additionalDeposit",Math.max(0,n(c.getNewSecurityDeposit())-held)));
  }
  if("WAITING_FOR_PAYMENT".equals(c.getStatus())&&!(during?List.of(RentalStatus.HANDED_OVER,RentalStatus.EXTENDED,RentalStatus.OVERDUE).contains(c.getRentalOrder().getStatus()):before(c.getRentalOrder())))result.put("status","CANCELLED");
  else if("WAITING_FOR_PAYMENT".equals(c.getStatus())&&c.getExpiresAt()!=null&&!c.getExpiresAt().isAfter(AvailabilityService.now()))result.put("status","EXPIRED");
  return result;
 }
 @Transactional(readOnly=true) public List<Map<String,Object>> history(Long id){current.canRead(orders.findById(id).orElseThrow());return changes.findByRentalOrderIdOrderByIdDesc(id).stream().map(this::view).toList();}
 @Transactional(readOnly=true) public List<Map<String,Object>> options(Long id){
  current.operator();var o=orders.findById(id).orElseThrow();if(!before(o))throw new IllegalArgumentException("Chỉ xử lý trước bàn giao");if(!o.getReturnDate().isAfter(AvailabilityService.now()))return List.of();var result=new ArrayList<Map<String,Object>>();
  for(var b:bikes.findAll())if(!b.getId().equals(o.getMotorbike().getId())&&availability.available(b,id,o.getPickupDate(),o.getReturnDate())){
   var c=quote(o,b,new VehicleChangeRequest(b.getId(),null,null,null,null,null,null,null,null,null));var item=view(c);
   item.put("image",b.getImagePrimary());item.put("category",b.getCategory()==null?"":b.getCategory().getName());item.put("available",true);result.add(item);
  }return result;
 }
 @Transactional(readOnly=true) public Map<String,Object> preview(Long id,VehicleChangeRequest r){current.operator();var o=orders.findById(id).orElseThrow();requireBefore(o);var b=bikes.findById(r.motorbikeId()).orElseThrow();if(!availability.available(b,id,o.getPickupDate(),o.getReturnDate()))throw new IllegalArgumentException("Xe không còn trống");return view(quote(o,b,r));}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> propose(Long id,VehicleChangeRequest r){
  var actor=current.operator();reason(r.reason());if(r.motorbikeId()==null)throw new IllegalArgumentException("Chọn xe thay thế");
  var o=lock(id,r.motorbikeId());requireBefore(o);clearExpired(o);var b=bikes.findById(r.motorbikeId()).orElseThrow();availability.requireAvailable(b,id,o.getPickupDate(),o.getReturnDate());
  var c=quote(o,b,r);c.setReason(r.reason().trim());c.setProcessedByStaffId(actor.getId());c.setCreatedAt(AvailabilityService.now());c.setExpiresAt(AvailabilityService.now().plusMinutes(pricing.settings().getHoldMinutes()));
  changes.saveAndFlush(c);
  if(c.getAdditionalPayment()==0)applyChange(o,c);else notifications.send(o,"Đề nghị đổi xe "+o.getOrderCode(),"Mở chi tiết đơn để thanh toán phần bổ sung và xác nhận xe thay thế. Xe và giá hiện tại chưa thay đổi.");
  return view(c);
 }
 private boolean matches(RentalOrder o,RentalVehicleChange c){return before(o)&&o.getMotorbike().getId().equals(c.getOldVehicle().getId())&&o.getPickupDate().equals(c.getPlannedPickup())&&o.getReturnDate().equals(c.getPlannedReturn())&&Objects.equals(o.getTotalPrice(),c.getOldTotal())&&n(o.getDepositPaid())==n(c.getPaidBefore())&&n(o.getBalancePaid())==n(c.getBalancePaidBefore());}
 private void applyChange(RentalOrder o,RentalVehicleChange c){
  o.setMotorbike(c.getNewVehicle());o.setDailyRate(c.getNewDailyRate());o.setRentalFee(c.getNewRentalFee());o.setDeliveryFee(c.getNewDeliveryFee());o.setTotalPrice(c.getNewTotal());o.setSecurityDeposit(c.getNewSecurityDeposit());o.setDepositDue(c.getNewDepositDue());
  o.setPickupFee(c.getNewPickupFee());o.setReturnFee(c.getNewReturnFee());o.setPickupType(c.getNewPickupType());o.setPickupAddress(c.getNewPickupAddress());o.setPickupLat(c.getNewPickupLat());o.setPickupLng(c.getNewPickupLng());
  o.setReturnType(c.getNewReturnType());o.setReturnAddress(c.getNewReturnAddress());o.setReturnLat(c.getNewReturnLat());o.setReturnLng(c.getNewReturnLng());
  // Preserve booking records and amount paid; the top-up is recorded separately by applyPayment.
  c.setStatus("APPLIED");c.setProcessedAt(AvailabilityService.now());orders.save(o);changes.save(c);
  notifications.send(o,"Đã đổi xe "+o.getOrderCode(),"Xe thay thế: "+c.getNewVehicleName()+". Mã đơn và các khoản đã thanh toán được giữ lại.");
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Payment preparePayment(Long id){
  Long orderId=changes.orderId(id).orElseThrow();var o=lock(orderId,changes.vehicleId(id).orElseThrow());current.owns(o);var c=lockedChange(id);
  if(!"WAITING_FOR_PAYMENT".equals(c.getStatus())||!c.getExpiresAt().isAfter(AvailabilityService.now())||!matches(o,c))throw new IllegalArgumentException("Đề nghị đổi xe không còn hiệu lực");
  availability.requireAvailable(c.getNewVehicle(),orderId,c.getPlannedPickup(),c.getPlannedReturn());
  var existing=payments.lockForOrder(orderId).stream().filter(p->"VEHICLE_CHANGE".equals(p.getSourceType())&&id.equals(p.getSourceId())).findFirst();if(existing.isPresent())return existing.get();
  var allocation=new LinkedHashMap<String,Integer>();
  int remaining=c.getAdditionalPayment();
  int pickup=Math.min(remaining,Math.max(0,n(c.getNewPickupFee())-n(o.getPickupFee())));remaining-=pickup;
  int returns=Math.min(remaining,Math.max(0,n(c.getNewReturnFee())-n(o.getReturnFee())));remaining-=returns;
  allocation.put("DELIVERY_FEE",pickup);allocation.put("RETURN_FEE",returns);allocation.put("BOOKING_PAYMENT",remaining);
  var p=Payment.builder().rentalOrder(o).amount(c.getAdditionalPayment()).purpose("BOOKING_PAYMENT").transactionType("BOOKING_PAYMENT").paymentMethod("PAYOS").sourceType("VEHICLE_CHANGE").sourceId(id).idempotencyKey("VEHICLE_CHANGE:"+id).expiresAt(c.getExpiresAt()).componentsJson(Financials.components(allocation,c.getAdditionalPayment())).build();
  long code;do{code=1+CODES.nextLong(4_503_599_627_370_495L);}while(payments.findByProviderCode(code).isPresent());p.setProviderCode(code);return payments.saveAndFlush(p);
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void applyPayment(Long code,Integer amount,String link,String reference){
  Long id=payments.sourceIdForCode(code).orElseThrow();Long orderId=changes.orderId(id).orElseThrow();var o=lock(orderId,changes.vehicleId(id).orElseThrow());var c=lockedChange(id);var p=payments.lockByProviderCode(code).orElseThrow();em.refresh(p,LockModeType.PESSIMISTIC_WRITE);
  if(!"PENDING".equals(p.getStatus()))return;
  if(p.getProviderLinkId()!=null&&!Objects.equals(link,p.getProviderLinkId()))throw new IllegalArgumentException("Mã giao dịch không khớp");if(amount==null||amount<0)throw new IllegalArgumentException("Số tiền không hợp lệ");
  p.setReceivedAmount(amount);p.setProviderLinkId(link);p.setTransactionCode(reference==null?"PAYOS-"+code:reference);p.setPaidAt(AvailabilityService.now());p.setProcessedAt(AvailabilityService.now());
  if(!Objects.equals(amount,p.getAmount())||!"WAITING_FOR_PAYMENT".equals(c.getStatus())||!c.getExpiresAt().isAfter(AvailabilityService.now())||!matches(o,c)||!availability.availableLocked(c.getNewVehicle(),orderId,c.getPlannedPickup(),c.getPlannedReturn())){
   p.setStatus("REVIEW_REQUIRED");if("WAITING_FOR_PAYMENT".equals(c.getStatus())){c.setStatus("EXPIRED");c.setProcessedAt(AvailabilityService.now());}return;
  }
  p.setStatus("PAID");applyChange(o,c);o.setDepositPaid(Math.addExact(n(o.getDepositPaid()),amount));
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void cancel(Long id){
  var actor=current.operator();var o=lock(changes.orderId(id).orElseThrow(),changes.vehicleId(id).orElseThrow());var c=lockedChange(id);
  if(!"WAITING_FOR_PAYMENT".equals(c.getStatus()))throw new IllegalArgumentException("Đề nghị đã được xử lý");
  c.setStatus("CANCELLED");c.setCancelledByStaffId(actor.getId());c.setProcessedAt(AvailabilityService.now());notifications.send(o,"Đã hủy đề nghị đổi xe", "Xe và giá trên đơn hiện tại được giữ nguyên.");
 }
 public record Refund(Integer amount,String method,String reference,String reason,String note) {}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void refund(Long id,Refund r,boolean reject){
  var actor=current.operator();reason(r.reason());if(r.amount()==null||r.amount()<0)throw new IllegalArgumentException("Số tiền không hợp lệ");
  if(!List.of("CASH","BANK_TRANSFER","OTHER").contains(Objects.toString(r.method(),""))||r.reference()==null||r.reference().isBlank()||r.reference().length()>100||r.note()!=null&&r.note().length()>900)throw new IllegalArgumentException("Nhập phương thức, mã biên nhận và ghi chú hợp lệ");
  var o=lock(id,null);if(!before(o))throw new IllegalArgumentException("Chỉ xử lý hoàn tiền trước bàn giao");
  int paid=Math.addExact(n(o.getDepositPaid()),n(o.getBalancePaid()));int expected=reject?paid:Math.max(0,paid-o.getTotalPrice());
  if(r.amount()!=expected||(!reject&&expected==0))throw new IllegalArgumentException("Số tiền cần hoàn hiện tại: "+expected);
  for(var c:changes.lockPending(id)){if(!reject&&c.getExpiresAt().isAfter(AvailabilityService.now()))throw new IllegalArgumentException("Hủy đề nghị chờ thanh toán trước khi hoàn tiền");c.setStatus("CANCELLED");c.setCancelledByStaffId(actor.getId());c.setProcessedAt(AvailabilityService.now());}
  if(expected>0){
   String type=reject?"BOOKING_REFUND":"BOOKING_ADJUSTMENT_REFUND";var now=AvailabilityService.now();
   var change=reject?null:changes.findByRentalOrderIdOrderByIdDesc(id).stream().filter(c->"APPLIED".equals(c.getStatus())).findFirst().orElse(null);
   if(change!=null){change.setRefundedAmount(n(change.getRefundedAmount())+expected);change.setRefundedAt(now);change.setRefundedByStaffId(actor.getId());}
   payments.saveAndFlush(Payment.builder().rentalOrder(o).amount(expected).receivedAmount(expected).direction("OUT").purpose(type).transactionType(type).paymentMethod(r.method()).status("PAID").paidAt(now).processedAt(now).processedByStaffId(actor.getId()).note(r.reason()+ (r.note()==null?"":" · "+r.note())).transactionCode(r.method()+"-"+r.reference().trim()).idempotencyKey("OFFLINE:"+r.method()+":"+r.reference().trim()).sourceType(change==null?"BEFORE_DELIVERY_REFUND":"VEHICLE_CHANGE_REFUND").sourceId(change==null?id:change.getId()).componentsJson(Financials.components(Map.of(type,expected),expected)).build());
   int fromBalance=Math.min(n(o.getBalancePaid()),expected);o.setBalancePaid(n(o.getBalancePaid())-fromBalance);o.setDepositPaid(n(o.getDepositPaid())-(expected-fromBalance));
  }
  if(reject){o.setStatus(RentalStatus.REJECTED);o.setRejectionReason(r.reason());o.setStaff(actor);}
  orders.save(o);notifications.send(o,reject?"Đã hoàn tiền và từ chối đơn":"Đã hoàn tiền chênh lệch", "Số tiền cửa hàng đã xác nhận hoàn: "+expected+" đồng.");
 }
}
