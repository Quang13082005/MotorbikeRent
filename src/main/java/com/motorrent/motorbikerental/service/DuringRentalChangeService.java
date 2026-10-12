package com.motorrent.motorbikerental.service;

import com.motorrent.motorbikerental.dto.request.DuringRentalQuoteRequest;
import com.motorrent.motorbikerental.entity.*;
import com.motorrent.motorbikerental.enums.*;
import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.security.CurrentUser;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class DuringRentalChangeService {
 private final DuringRentalQuoteService quotes;
 private final CurrentUser current;
 private final RentalOrderRepository orders;
 private final MotorbikeRepository bikes;
 private final RentalVehicleChangeRepository changes;
 private final RentalExtensionRepository extensions;
 private final PaymentRepository payments;
 private final AvailabilityService availability;
 private final PricingService pricing;
 private final NotificationService notifications;
 @PersistenceContext private EntityManager em;
 private static final java.security.SecureRandom CODES=new java.security.SecureRandom();
 private static int n(Integer amount){return Objects.requireNonNullElse(amount,0);}
 private static boolean active(RentalOrder o){return List.of(RentalStatus.HANDED_OVER,RentalStatus.EXTENDED,RentalStatus.OVERDUE).contains(o.getStatus());}
 private RentalOrder lock(Long id,Long target){
  Long original=orders.motorbikeIdFor(id).orElseThrow();
  var ids=new TreeSet<Long>();ids.add(original);ids.add(target);
  for(Long bike:ids){var b=bikes.lockById(bike).orElseThrow();em.refresh(b,LockModeType.PESSIMISTIC_WRITE);}
  var o=orders.lockById(id).orElseThrow();em.refresh(o,LockModeType.PESSIMISTIC_WRITE);
  if(!original.equals(o.getMotorbike().getId())&&!target.equals(o.getMotorbike().getId()))throw new IllegalArgumentException("Xe của đơn vừa thay đổi, vui lòng tải lại");
  return o;
 }
 private RentalVehicleChange locked(Long id){var c=changes.lockById(id).orElseThrow();em.refresh(c,LockModeType.PESSIMISTIC_WRITE);if(!"DURING_RENTAL".equals(c.getType()))throw new IllegalArgumentException("Sai loại đề nghị đổi xe");return c;}
 private boolean matches(RentalOrder o,RentalVehicleChange c){return active(o)&&o.getMotorbike().getId().equals(c.getOldVehicle().getId())&&Objects.equals(o.getReturnDate(),c.getPlannedReturn())&&Objects.equals(o.getTotalPrice(),c.getOldTotal())&&n(o.getDepositPaid())==n(c.getPaidBefore())&&n(o.getBalancePaid())==n(c.getBalancePaidBefore());}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public Long propose(Long id,DuringRentalQuoteRequest r){
  var actor=current.operator();if(r==null||r.motorbikeId()==null)throw new IllegalArgumentException("Chọn xe thay thế");
  var o=lock(id,r.motorbikeId());
  if(changes.openDuring(id,AvailabilityService.now())>0)throw new IllegalArgumentException("Đang có phương án đổi xe chưa hoàn tất");
  for(var e:extensions.lockOpen(id)){
   em.refresh(e,LockModeType.PESSIMISTIC_WRITE);
   if("PENDING".equals(e.getStatus())||e.getExpiresAt()!=null&&e.getExpiresAt().isAfter(AvailabilityService.now()))throw new IllegalArgumentException("Xử lý hoặc từ chối yêu cầu gia hạn cũ trước khi lập phương án đổi xe");
  }
  var q=quotes.quote(id,r);var b=bikes.findById(r.motorbikeId()).orElseThrow();
  availability.requireAvailable(b,id,r.plannedChangeTime(),r.newReturnTime());
  var c=new RentalVehicleChange();c.setType("DURING_RENTAL");c.setRentalOrder(o);c.setOldVehicle(o.getMotorbike());c.setNewVehicle(b);
  c.setOldVehicleName(o.getMotorbike().getName());c.setNewVehicleName(b.getName());c.setOldPlate(o.getMotorbike().getLicensePlate());c.setNewPlate(b.getLicensePlate());
  c.setOldDailyRate(o.getDailyRate());c.setNewDailyRate(b.getPricePerDay());c.setOldRentalFee(o.getRentalFee());c.setNewRentalFee((Integer)q.get("newRentalFee"));
  c.setOldDeliveryFee(o.getDeliveryFee());c.setNewDeliveryFee(o.getDeliveryFee());c.setOldTotal(o.getTotalPrice());c.setNewTotal((Integer)q.get("newTotal"));
  c.setOldSecurityDeposit(o.getSecurityDeposit());c.setNewSecurityDeposit((Integer)q.get("requiredDeposit"));
  c.setPaidBefore(n(o.getDepositPaid()));c.setBalancePaidBefore(n(o.getBalancePaid()));c.setAdditionalPayment((Integer)q.get("additionalPayment"));c.setCreditAmount(0);
  c.setPlannedPickup(o.getPickupDate());c.setPlannedReturn(o.getReturnDate());c.setExtensionStartTime(r.plannedChangeTime());c.setNewPlannedReturnTime(r.newReturnTime());
  c.setExtraRentalAmount((Integer)q.get("futureRentalAmount"));c.setVehicleChangeFee(r.vehicleChangeFee());c.setExchangeAddress(r.exchangeAddress().trim());c.setReason(r.reason().trim());
  c.setProcessedByStaffId(actor.getId());var now=AvailabilityService.now();c.setCreatedAt(now);c.setExpiresAt(now.plusMinutes(pricing.settings().getHoldMinutes()));
  // A payment cannot authorize an exchange after its planned boundary has passed.
  if(c.getExpiresAt().isAfter(c.getExtensionStartTime()))c.setExpiresAt(c.getExtensionStartTime());
  if(!c.getExpiresAt().isAfter(now))throw new IllegalArgumentException("Chọn thời điểm đổi xe còn đủ thời gian thanh toán");
  changes.saveAndFlush(c);notifications.send(o,"Phương án tiếp tục thuê bằng xe khác","Vui lòng xem và thanh toán phương án đổi xe trong chi tiết đơn. Xe đang sử dụng chưa thay đổi.");return c.getId();
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public Payment preparePayment(Long id){
  var o=lock(changes.orderId(id).orElseThrow(),changes.vehicleId(id).orElseThrow());current.owns(o);var c=locked(id);
  if(!"WAITING_FOR_PAYMENT".equals(c.getStatus())||!c.getExpiresAt().isAfter(AvailabilityService.now())||!matches(o,c))throw new IllegalArgumentException("Phương án đổi xe hết hiệu lực");
  availability.requireAvailable(c.getNewVehicle(),o.getId(),c.getExtensionStartTime(),c.getNewPlannedReturnTime());
  var prior=payments.lockForOrder(o.getId()).stream().filter(p->"DURING_RENTAL".equals(p.getSourceType())&&id.equals(p.getSourceId())).findFirst();
  if(prior.isPresent()){if(!"PENDING".equals(prior.get().getStatus()))throw new IllegalArgumentException("Giao dịch đã xử lý");return prior.get();}
  var p=Payment.builder().rentalOrder(o).amount(c.getAdditionalPayment()).purpose("EXTENSION_PAYMENT").transactionType("EXTENSION_PAYMENT").direction("IN").paymentMethod("PAYOS").sourceType("DURING_RENTAL").sourceId(id).idempotencyKey("DURING_RENTAL:"+id).expiresAt(c.getExpiresAt()).componentsJson(Financials.components(Map.of("EXTENSION_PAYMENT",c.getExtraRentalAmount(),"VEHICLE_CHANGE_FEE",c.getVehicleChangeFee()),c.getAdditionalPayment())).build();
  long code;do{code=1+CODES.nextLong(4_503_599_627_370_495L);}while(payments.findByProviderCode(code).isPresent());p.setProviderCode(code);return payments.saveAndFlush(p);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void applyPayment(Long code,Integer amount,String link,String reference){
  Long id=payments.sourceIdForCode(code).orElseThrow();var o=lock(changes.orderId(id).orElseThrow(),changes.vehicleId(id).orElseThrow());var c=locked(id);var p=payments.lockByProviderCode(code).orElseThrow();em.refresh(p,LockModeType.PESSIMISTIC_WRITE);
  if(!"PENDING".equals(p.getStatus()))return;
  if(!"DURING_RENTAL".equals(p.getSourceType())||!Objects.equals(p.getSourceId(),id)||!Objects.equals(p.getRentalOrder().getId(),o.getId()))throw new IllegalArgumentException("Nguồn thanh toán không khớp");
  if(link==null||link.isBlank()||p.getProviderLinkId()!=null&&!link.equals(p.getProviderLinkId()))throw new IllegalArgumentException("Mã giao dịch không khớp");
  if(amount==null||amount<0)throw new IllegalArgumentException("Số tiền không hợp lệ");
  var now=AvailabilityService.now();p.setReceivedAmount(amount);p.setProviderLinkId(link);p.setPaidAt(now);p.setProcessedAt(now);p.setTransactionCode(reference==null||reference.isBlank()?"PAYOS-"+code:reference);
  if(!Objects.equals(amount,p.getAmount())||!Objects.equals(amount,c.getAdditionalPayment())||!"WAITING_FOR_PAYMENT".equals(c.getStatus())||!c.getExpiresAt().isAfter(now)||!matches(o,c)||!availability.availableLocked(c.getNewVehicle(),o.getId(),c.getExtensionStartTime(),c.getNewPlannedReturnTime())){
   p.setStatus("REVIEW_REQUIRED");c.setStatus("REVIEW_REQUIRED");c.setProcessedAt(now);return;
  }
  p.setStatus("PAID");c.setStatus("READY_FOR_EXCHANGE");c.setFinancialApprovedAt(now);
  o.setReturnDate(c.getNewPlannedReturnTime());o.setDays(Math.addExact(o.getDays(),PricingService.days(c.getExtensionStartTime(),c.getNewPlannedReturnTime())));
  o.setTotalPrice(c.getNewTotal());o.setRentalFee(c.getNewRentalFee());o.setBalancePaid(Math.addExact(n(o.getBalancePaid()),amount));o.setStatus(RentalStatus.EXTENDED);
  // Keep the physical vehicle association and status until Staff returns the old bike.
  notifications.send(o,"Đã thanh toán phương án đổi xe","Lịch thuê tiếp đã được xác nhận. Cửa hàng sẽ thực hiện đổi xe theo phương án đã thống nhất.");
 }
 public record Confirmation(Integer additionalDeposit,String method,String reference) {}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void confirm(Long id,Confirmation request){
  var actor=current.operator();var o=lock(changes.orderId(id).orElseThrow(),changes.vehicleId(id).orElseThrow());var c=locked(id);
  if("APPLIED".equals(c.getStatus()))return;
  if(!"READY_FOR_EXCHANGE".equals(c.getStatus())||!active(o)||!o.getMotorbike().getId().equals(c.getOldVehicle().getId())||!Objects.equals(o.getReturnDate(),c.getNewPlannedReturnTime())||!Objects.equals(o.getTotalPrice(),c.getNewTotal()))throw new IllegalArgumentException("Phương án chưa thanh toán hoặc đơn đã thay đổi");
  var now=AvailabilityService.now();if(now.isBefore(c.getExtensionStartTime()))throw new IllegalArgumentException("Chưa đến giờ đổi xe đã thanh toán");
  if(!now.isBefore(o.getReturnDate()))throw new IllegalArgumentException("Đã hết lịch thuê tiếp; cần xử lý với khách trước khi đổi xe");
  var target=c.getNewVehicle();em.refresh(target,LockModeType.PESSIMISTIC_WRITE);
  availability.requireAvailable(target,o.getId(),now,o.getReturnDate());
  if(target.getStatus()!=MotorbikeStatus.AVAILABLE)throw new IllegalArgumentException("Xe thay thế chưa sẵn sàng bàn giao thực tế");
  var ledger=payments.lockForOrder(o.getId());for(var p:ledger)em.refresh(p,LockModeType.PESSIMISTIC_WRITE);
  var deposit=AssetDepositAmounts.quote(c.getNewSecurityDeposit(),AssetDepositAmounts.held(ledger));
  if(request==null)throw new IllegalArgumentException("Nhập khoản cọc bổ sung thực thu");deposit.validateActualCollection(request.additionalDeposit());
  if(deposit.additionalDeposit()>0){
   if(request.method()==null||!Set.of("CASH","BANK_TRANSFER","OTHER").contains(request.method())||request.reference()==null||request.reference().isBlank()||request.reference().length()>100)throw new IllegalArgumentException("Nhập phương thức và mã biên nhận cọc bổ sung");
   int amount=deposit.additionalDeposit();payments.saveAndFlush(Payment.builder().rentalOrder(o).amount(amount).receivedAmount(amount).direction("IN").status("PAID").purpose("ASSET_DEPOSIT_ADDITIONAL").transactionType("ASSET_DEPOSIT_ADDITIONAL").paymentMethod(request.method()).transactionCode(request.method()+"-"+request.reference().trim()).idempotencyKey("EXCHANGE_ASSET:"+id).sourceType("DURING_RENTAL_ASSET").sourceId(id).paidAt(now).processedAt(now).processedByStaffId(actor.getId()).componentsJson(Financials.components(Map.of("ASSET_DEPOSIT_ADDITIONAL",amount),amount)).build());
  }
  // Do not erase a maintenance/unavailable flag established while the bike was away.
  if(c.getOldVehicle().getStatus()==MotorbikeStatus.RENTED)c.getOldVehicle().setStatus(MotorbikeStatus.AVAILABLE);
  target.setStatus(MotorbikeStatus.RENTED);o.setMotorbike(target);o.setDailyRate(c.getNewDailyRate());o.setSecurityDeposit(c.getNewSecurityDeposit());
  c.setAdditionalDepositAmount(deposit.additionalDeposit());c.setChangedByStaffId(actor.getId());c.setActualVehicleChangeTime(now);c.setProcessedAt(now);c.setStatus("APPLIED");
  notifications.send(o,"Đã đổi xe thực tế","Xe đang thuê: "+c.getNewVehicleName()+". Lịch sử và cọc tài sản được giữ trong đơn hiện tại.");
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void cancel(Long id){
  var actor=current.operator();var o=lock(changes.orderId(id).orElseThrow(),changes.vehicleId(id).orElseThrow());var c=locked(id);
  if(!"WAITING_FOR_PAYMENT".equals(c.getStatus()))throw new IllegalArgumentException("Chỉ hủy phương án chưa thanh toán");
  c.setStatus("CANCELLED");c.setCancelledByStaffId(actor.getId());c.setProcessedAt(AvailabilityService.now());
 }
}
