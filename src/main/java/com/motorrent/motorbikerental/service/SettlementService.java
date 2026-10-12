package com.motorrent.motorbikerental.service;
import com.motorrent.motorbikerental.entity.*;import com.motorrent.motorbikerental.repository.*;import com.motorrent.motorbikerental.enums.RentalStatus;import com.motorrent.motorbikerental.security.CurrentUser;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.time.LocalDateTime;import java.util.*;
@Service @RequiredArgsConstructor
public class SettlementService {
 private final CurrentUser current;private final RentalOrderRepository orders;private final PaymentRepository payments;private final RentalContractRepository contracts;private final MotorbikeRepository bikes;
 @Transactional public void cash(Long id,String purpose,String reference){ cash(id,purpose,reference,"CASH",null); }
 @Transactional public void cash(Long id,String purpose,String reference,String method,String note){
  var actor=current.operator();var o=orders.lockById(id).orElseThrow();
  if(reference==null || reference.isBlank() || reference.length()>100)throw new IllegalArgumentException("Cần mã biên nhận thu / hoàn tiền");
  String paymentMethod=method==null?"CASH":method;
  if(!List.of("CASH","BANK_TRANSFER","OTHER").contains(paymentMethod))throw new IllegalArgumentException("Phương thức thu chi không hợp lệ");
  if(note!=null && note.length()>2000)throw new IllegalArgumentException("Ghi chú tối đa 2000 ký tự");
  int amount;String status="PAID";
  if("BALANCE".equals(purpose)){
   if(!List.of(RentalStatus.CONFIRMED,RentalStatus.READY_FOR_HANDOVER,RentalStatus.HANDED_OVER,RentalStatus.EXTENDED,RentalStatus.OVERDUE,RentalStatus.RETURNED,RentalStatus.RETURN_REQUESTED).contains(o.getStatus()))throw new IllegalArgumentException("Đơn chưa đủ điều kiện thu tiền còn lại");
   amount=o.getTotalPrice()-Objects.requireNonNullElse(o.getDepositPaid(),0)-Objects.requireNonNullElse(o.getBalancePaid(),0);if(amount<=0)throw new IllegalArgumentException("Đơn đã thu đủ tiền thuê");o.setBalancePaid(Objects.requireNonNullElse(o.getBalancePaid(),0)+amount);
  } else if("DEPOSIT_REFUND".equals(purpose)) {
   if(!List.of(RentalStatus.CANCELLED,RentalStatus.REJECTED).contains(o.getStatus()))throw new IllegalArgumentException("Chỉ hoàn cọc cho đơn đã hủy hoặc từ chối");
   var prior=payments.lockForOrder(id);int refunded=prior.stream().filter(p->Financials.posted(p) && "BOOKING_REFUND".equals(Financials.type(p)) && !"LATE_REFUND".equals(p.getPurpose())).mapToInt(Financials::signedAmount).sum();amount=Objects.requireNonNullElse(o.getDepositPaid(),0)+refunded;if(amount<=0)throw new IllegalArgumentException("Không còn khoản cọc cần hoàn");amount=-amount;
  } else if("SECURITY".equals(purpose)){
   if(!List.of(RentalStatus.CONFIRMED,RentalStatus.READY_FOR_HANDOVER).contains(o.getStatus()))throw new IllegalArgumentException("Chỉ nhận bảo đảm trước bàn giao");
   amount=Objects.requireNonNullElse(o.getSecurityDeposit(),0);if(amount<=0 || payments.lockForOrder(id).stream().anyMatch(p->Financials.posted(p) && "ASSET_DEPOSIT".equals(Financials.type(p))))throw new IllegalArgumentException("Khoản bảo đảm đã ghi nhận hoặc không cần thu");
  } else if("SECURITY_REFUND".equals(purpose)){
   if(!List.of(RentalStatus.COMPLETED,RentalStatus.CANCELLED,RentalStatus.REJECTED).contains(o.getStatus()))throw new IllegalArgumentException("Hoàn bảo đảm sau khi kết thúc đơn");
   amount=payments.lockForOrder(id).stream().filter(p->Financials.posted(p) && List.of("ASSET_DEPOSIT","ASSET_DEPOSIT_ADDITIONAL","ASSET_DEPOSIT_REFUND").contains(Financials.type(p))).mapToInt(Financials::signedAmount).sum();if(amount<=0)throw new IllegalArgumentException("Không còn khoản bảo đảm cần hoàn");amount=-amount;
  } else throw new IllegalArgumentException("Loại khoản thu không hợp lệ");
  var recordedAt=AvailabilityService.now();
  payments.save(Payment.builder().rentalOrder(o).amount(Math.absExact(amount)).receivedAmount(Math.absExact(amount)).direction(amount<0?"OUT":"IN").purpose(purpose).paymentMethod(paymentMethod).status(status).transactionCode(paymentMethod+"-"+reference.trim()).idempotencyKey("OFFLINE:"+paymentMethod+":"+reference.trim()).paidAt(recordedAt).processedAt(recordedAt).processedByStaffId(actor.getId()).note(note).sourceType("STAFF_COLLECTION").sourceId(id).componentsJson(Financials.components(Map.of(Financials.type(purpose),Math.absExact(amount)),Math.absExact(amount))).build());o.setStaff(actor);orders.save(o);
 }
 @Transactional(readOnly=true) public Map<String,Object> inspectionDetails(Long id){
  current.operator();var o=orders.findById(id).orElseThrow();var c=contracts.findByRentalOrderId(id).orElse(null);
  return OperationsService.row("status",o.getStatus().name(),"before",c==null?null:c.getVehicleConditionBefore(),"after",c==null?null:c.getVehicleConditionAfter(),"odoBefore",c==null?null:c.getOdoBefore(),"odoAfter",c==null?null:c.getOdoAfter());
 }
 @Transactional public void inspection(Long id,String before,String after,Integer odoBefore,Integer odoAfter){
  current.operator();bikes.lockById(orders.motorbikeIdFor(id).orElseThrow()).orElseThrow();var o=orders.lockById(id).orElseThrow();if(List.of(RentalStatus.CANCELLED,RentalStatus.REJECTED,RentalStatus.COMPLETED).contains(o.getStatus()))throw new IllegalArgumentException("Đơn đã đóng");
  boolean beforeHandover=List.of(RentalStatus.CONFIRMED,RentalStatus.READY_FOR_HANDOVER).contains(o.getStatus());
  boolean afterHandover=List.of(RentalStatus.HANDED_OVER,RentalStatus.EXTENDED,RentalStatus.OVERDUE,RentalStatus.RETURN_REQUESTED,RentalStatus.RETURNED).contains(o.getStatus());
  if(!beforeHandover && !afterHandover)throw new IllegalArgumentException("Đơn chưa được xác nhận để lập biên bản");
  var c=contracts.findByRentalOrderId(id).orElseGet(()->RentalContract.builder().rentalOrder(o).contractCode("HD-"+o.getOrderCode()).build());
  if(before!=null && before.length()>4000 || after!=null && after.length()>4000)throw new IllegalArgumentException("Biên bản tối đa 4000 ký tự");
  if(afterHandover && ((before!=null && !Objects.equals(before,Objects.toString(c.getVehicleConditionBefore(),""))) || (odoBefore!=null && !Objects.equals(odoBefore,c.getOdoBefore()))))throw new IllegalArgumentException("Không sửa biên bản lúc giao sau khi đã bàn giao xe");
  if(beforeHandover && ((after!=null && !after.isBlank()) || odoAfter!=null))throw new IllegalArgumentException("Chỉ ghi nhận tình trạng trả sau khi đã bàn giao xe");
  if(odoBefore!=null && odoBefore<0 || odoAfter!=null && odoAfter<Objects.requireNonNullElse(odoBefore,Objects.requireNonNullElse(c.getOdoBefore(),0)))throw new IllegalArgumentException("Công tơ mét không hợp lệ");
  if(beforeHandover && before!=null && !before.isBlank()){c.setVehicleConditionBefore(before);c.setOdoBefore(odoBefore);}
  if(after!=null && !after.isBlank()){c.setVehicleConditionAfter(after);c.setOdoAfter(odoAfter);if(odoAfter!=null)o.getMotorbike().setOdoKm(odoAfter);}
  contracts.save(c);
 }
 @Transactional public void refundReview(Long paymentId,String receipt){ refundReview(paymentId,receipt,"CASH",null); }
 @Transactional public void refundReview(Long paymentId,String receipt,String method,String note){
  var actor=current.operator();
  if(method==null)method="CASH";
  if(!List.of("CASH","BANK_TRANSFER","OTHER").contains(method) || note!=null && note.length()>2000)throw new IllegalArgumentException("Phương thức hoặc ghi chú không hợp lệ");orders.lockById(payments.orderIdForPayment(paymentId).orElseThrow()).orElseThrow();var p=payments.lockById(paymentId).orElseThrow();
  if(!"REVIEW_REQUIRED".equals(p.getStatus()) || p.getReceivedAmount()==null || p.getReceivedAmount()<=0)throw new IllegalArgumentException("Giao dịch không có khoản cần hoàn");
  if(receipt==null || receipt.isBlank() || receipt.length()>100)throw new IllegalArgumentException("Nhập mã biên nhận hoàn tiền");
  payments.save(Payment.builder().rentalOrder(p.getRentalOrder()).amount(p.getReceivedAmount()).receivedAmount(p.getReceivedAmount()).direction("OUT").paymentMethod(method).processedByStaffId(actor.getId()).processedAt(AvailabilityService.now()).originalPaymentId(p.getId()).sourceType("PAYMENT_REVIEW_REFUND").sourceId(p.getId()).idempotencyKey("REVIEW_REFUND:"+p.getId()).note(note).componentsJson(Financials.components(Map.of("BOOKING_REFUND",p.getReceivedAmount()),p.getReceivedAmount())).purpose("LATE_REFUND").status("PAID").paidAt(LocalDateTime.now()).transactionCode("REFUND-"+receipt).build());p.setStatus("REFUNDED");payments.save(p);
 }
}
