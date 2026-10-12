package com.motorrent.motorbikerental.service;
import com.motorrent.motorbikerental.entity.*; import com.motorrent.motorbikerental.repository.*; import com.motorrent.motorbikerental.enums.RentalStatus;
import com.motorrent.motorbikerental.security.CurrentUser;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Value; import org.springframework.web.server.ResponseStatusException; import org.springframework.http.HttpStatus;
import java.time.*; import java.util.*; import vn.payos.PayOS; import vn.payos.type.*;
@Service @RequiredArgsConstructor @lombok.extern.slf4j.Slf4j
public class PaymentService {
 private final PayOS payOS; private final RentalOrderRepository orders; private final PaymentRepository payments;
 private final NotificationService notifications;
 private final AvailabilityService availability;
 private final VehicleChangeService vehicleChanges;
 private final ExtensionService extensions;
 private final DuringRentalChangeService duringChanges;
 public String createDuringChangePaymentLink(Long id){
  if(!paymentsEnabled)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"PayOS đang tắt trên bản kiểm thử");
  Payment p=duringChanges.preparePayment(id);return checkout(p,p.getRentalOrder().getId());
 }
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
 private final CurrentUser current; private final TransactionTemplate tx;
 @Value("${app.base-url:http://localhost:8080}") private String baseUrl;
 @Value("${app.payments-enabled:true}") private boolean paymentsEnabled;
 private static final java.security.SecureRandom PAYMENT_CODES=new java.security.SecureRandom();
 public String createPaymentLink(Long orderId) {
  if(!paymentsEnabled)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Thanh toán PayOS đang tắt trong môi trường kiểm thử này. Vui lòng kiểm tra thanh toán trên bản ứng dụng chính.");
  Payment payment=tx.execute(s->{
   var order=availability.lockOrder(orderId); current.owns(order);
   if(order.getStatus()!=RentalStatus.PENDING_PAYMENT || order.getPaymentExpiresAt()==null || !order.getPaymentExpiresAt().isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Đơn không còn trong thời gian thanh toán");
   var existing=payments.lockForOrder(orderId).stream().filter(p->"PAYOS".equals(p.getPaymentMethod()) && "DEPOSIT".equals(p.getPurpose())).findFirst();
   if(existing.isPresent()) return existing.get();
   availability.requireAvailable(order.getMotorbike(),orderId,order.getPickupDate(),order.getReturnDate());
   if(order.getDepositDue()==null || order.getDepositDue()<=0) throw new IllegalArgumentException("Đơn chưa có khoản cọc hợp lệ");
   var p=Payment.builder().rentalOrder(order).amount(order.getDepositDue()).paymentMethod("PAYOS").purpose("DEPOSIT").status("PENDING").expiresAt(order.getPaymentExpiresAt()).sourceType("BOOKING").sourceId(orderId).idempotencyKey("BOOKING:"+orderId).build();
   var components=new LinkedHashMap<String,Integer>();
   if(order.getPickupFee()!=null && order.getReturnFee()!=null){
    components.put("DELIVERY_FEE",order.getPickupFee());components.put("RETURN_FEE",order.getReturnFee());
    components.put("BOOKING_PAYMENT",Math.subtractExact(order.getDepositDue(),Math.addExact(order.getPickupFee(),order.getReturnFee())));
   }else components.put("BOOKING_PAYMENT",order.getDepositDue());
   p.setComponentsJson(Financials.components(components,p.getAmount()));
   long code;do{code=1+PAYMENT_CODES.nextLong(4_503_599_627_370_495L);}while(payments.findByProviderCode(code).isPresent());
   p.setProviderCode(code);return payments.saveAndFlush(p);
  });
  return checkout(payment,orderId);
 }
 public String createChangePaymentLink(Long id){
  if(!paymentsEnabled)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"PayOS đang tắt trên bản kiểm thử");
  Payment p=vehicleChanges.preparePayment(id);return checkout(p,p.getRentalOrder().getId());
 }
 private String checkout(Payment payment,Long orderId){
  if(payment.getCheckoutUrl()!=null) return payment.getCheckoutUrl();
  try {
   var data=PaymentData.builder().orderCode(payment.getProviderCode()).amount(payment.getAmount()).description(("EXTENSION".equals(payment.getSourceType())?"Gia han ":List.of("VEHICLE_CHANGE","DURING_RENTAL").contains(Objects.toString(payment.getSourceType(),""))?"Doi xe ":"Coc don ")+orderId)
    .returnUrl(baseUrl+"/customer/payment-result?orderId="+orderId).cancelUrl(baseUrl+"/customer/payment-result?orderId="+orderId)
    .expiredAt(payment.getExpiresAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toEpochSecond())
    .item(ItemData.builder().name("EXTENSION".equals(payment.getSourceType())?"Thanh toan gia han":List.of("VEHICLE_CHANGE","DURING_RENTAL").contains(Objects.toString(payment.getSourceType(),""))?"Thanh toan bo sung doi xe":"Dat coc thue xe").price(payment.getAmount()).quantity(1).build()).build();
   var response=payOS.createPaymentLink(data);
   tx.executeWithoutResult(s->{var p=payments.lockById(payment.getId()).orElseThrow();p.setCheckoutUrl(response.getCheckoutUrl());p.setProviderLinkId(response.getPaymentLinkId());payments.save(p);});
   return response.getCheckoutUrl();
  } catch(Exception e) {
   String providerError=e instanceof vn.payos.exception.PayOSException pe?Objects.toString(pe.getCode(),"unknown").replaceAll("[^A-Za-z0-9_-]",""):"network";
   log.warn("PayOS link failed: order={}, errorType={}, providerCode={}",orderId,e.getClass().getSimpleName(),providerError);
   // Keep the persisted provider code for reconciliation after a timeout.
   throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Chưa tạo được liên kết PayOS (mã "+providerError+"). Đơn vẫn được lưu; vui lòng thử lại hoặc liên hệ cửa hàng.");
  }
 }
 public void webhook(Webhook body) {
  WebhookData data;
  try { data=payOS.verifyPaymentWebhookData(body); }
  catch(Exception e) { throw new IllegalArgumentException("Chữ ký thanh toán không hợp lệ"); }
  if(!"00".equals(data.getCode())) return;
  LocalDateTime txnTime = null;
  if(data.getTransactionDateTime() != null && !data.getTransactionDateTime().isBlank()){
   try { txnTime = java.time.OffsetDateTime.parse(data.getTransactionDateTime()).toLocalDateTime(); }
   catch(Exception ex1){
    try { txnTime = LocalDateTime.parse(data.getTransactionDateTime()); } catch(Exception ignored){}
   }
  }
  apply(data.getOrderCode(),data.getAmount(),data.getPaymentLinkId(),data.getReference(),txnTime);
 }
 public void reconcile(Long orderId) {
  var order=orders.findById(orderId).orElseThrow(); current.canRead(order);
  for(var p:payments.findByRentalOrderIdOrderByCreatedAtDesc(orderId)) {
   if(p.getProviderCode()==null || !"PENDING".equals(p.getStatus())) continue;
   try { 
    var remote=payOS.getPaymentLinkInformation(p.getProviderCode());
    if("PAID".equals(remote.getStatus())) {
     LocalDateTime txnTime = null;
     if(remote.getTransactions() != null && !remote.getTransactions().isEmpty()){
      String dt = remote.getTransactions().get(0).getTransactionDateTime();
      if(dt != null && !dt.isBlank()){
       try { txnTime = java.time.OffsetDateTime.parse(dt).toLocalDateTime(); }
       catch(Exception ex1){
        try { txnTime = LocalDateTime.parse(dt); } catch(Exception ignored){}
       }
      }
     }
     apply(remote.getOrderCode(),remote.getAmountPaid(),remote.getId(),"PAYOS-"+remote.getId(),txnTime);
    }
   } catch(Exception e) { 
    log.warn("Reconcile error for order {}: {}", orderId, e.getMessage());
    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Chưa đối soát được với PayOS. Vui lòng thử lại."); 
   }
  }
 }
 public void apply(Long code,Integer received,String link,String reference) {
  apply(code, received, link, reference, null);
 }
 public void apply(Long code,Integer received,String link,String reference,LocalDateTime paymentTime) {
  if("DURING_RENTAL".equals(payments.sourceTypeForCode(code).orElse(null))){duringChanges.applyPayment(code,received,link,reference);return;}
  if("EXTENSION".equals(payments.sourceTypeForCode(code).orElse(null))){extensions.applyPayment(code,received,link,reference);return;}
  if("VEHICLE_CHANGE".equals(payments.sourceTypeForCode(code).orElse(null))){vehicleChanges.applyPayment(code,received,link,reference);return;}
  var orderId=payments.orderIdForCode(code).orElse(null); if(orderId==null) return;
  tx.executeWithoutResult(s->{
   var order=availability.lockOrder(orderId);
   var p=payments.lockByProviderCode(code).orElseThrow();
   entityManager.refresh(p,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
   if(!"PENDING".equals(p.getStatus())) return;
   if(p.getProviderLinkId()!=null && !Objects.equals(p.getProviderLinkId(),link)) throw new IllegalArgumentException("Mã giao dịch không khớp");
   if(received==null || received<0)throw new IllegalArgumentException("Số tiền nhận không hợp lệ");
   LocalDateTime actualPaidAt = paymentTime != null ? paymentTime : AvailabilityService.now();
   p.setSourceType("BOOKING");p.setSourceId(order.getId());p.setProcessedAt(AvailabilityService.now());
   p.setProviderLinkId(link); p.setReceivedAmount(received); p.setPaidAt(actualPaidAt);
   p.setTransactionCode(reference==null?"PAYOS-"+code:reference);

   boolean amountMatched = Objects.equals(received, p.getAmount());
   boolean paidBeforeDeadline = p.getExpiresAt() == null || !actualPaidAt.isAfter(p.getExpiresAt());
   boolean vehicleAvailable = availability.availableLocked(order.getMotorbike(), order.getId(), order.getPickupDate(), order.getReturnDate());

   if (!amountMatched) {
    p.setStatus("REVIEW_REQUIRED");
    p.setNote("Số tiền nhận được không khớp với tiền cọc.");
   } else if (order.getStatus() == RentalStatus.PENDING_PAYMENT) {
    if (paidBeforeDeadline && vehicleAvailable) {
     p.setStatus("PAID");
     order.setDepositPaid(p.getAmount());
     order.setStatus(RentalStatus.PENDING);
     orders.save(order);
     notifications.orderChanged(order);
    } else {
     p.setStatus("REVIEW_REQUIRED");
     p.setNote(!paidBeforeDeadline ? "Thanh toán nhận được sau khi hết hạn giữ chỗ." : "Xe không còn trống khi xác nhận thanh toán.");
    }
   } else if (order.getStatus() == RentalStatus.CANCELLED) {
    if (paidBeforeDeadline && vehicleAvailable) {
     // Safe restore: customer paid within deadline and vehicle is still free
     p.setStatus("PAID");
     order.setDepositPaid(p.getAmount());
     order.setStatus(RentalStatus.PENDING);
     order.setRejectionReason(null);
     orders.save(order);
     notifications.orderChanged(order);
    } else {
     // Vehicle occupied or paid after deadline: do NOT restore, prevent double-booking
     p.setStatus("REVIEW_REQUIRED");
     p.setNote(!paidBeforeDeadline ? "Thanh toán sau khi hết hạn giữ chỗ và đơn đã hủy." : "Khách đã thanh toán đúng hạn nhưng xe đã bị đơn khác đặt sau khi đơn bị hủy tự động. Cần Staff hỗ trợ đổi xe hoặc hoàn tiền.");
    }
   } else {
    p.setStatus("REVIEW_REQUIRED");
   }
   payments.save(p);
  });
 }
 public java.util.List<java.util.Map<String,Object>> list(Long id) {
  current.canRead(orders.findById(id).orElseThrow());
  return payments.findByRentalOrderIdOrderByCreatedAtDesc(id).stream().map(p->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",p.getId());m.put("amount",Financials.signedAmount(p));m.put("receivedAmount",Financials.signedReceived(p));m.put("absoluteAmount",p.getAmount());m.put("transactionType",Financials.type(p));m.put("direction",p.getDirection());m.put("transactionStatus",Financials.transactionStatus(p));m.put("status",p.getStatus());m.put("method",p.getPaymentMethod());m.put("purpose",p.getPurpose());m.put("paidAt",p.getPaidAt());return m;}).toList();
 }
 public String createExtensionPaymentLink(Long id){
  if(!paymentsEnabled)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"PayOS đang tắt trên bản kiểm thử");
  Payment p=extensions.preparePayment(id);return checkout(p,p.getRentalOrder().getId());
 }
}
