from pathlib import Path
R=Path('src/main/java/com/motorrent/motorbikerental')
def put(p,s): (R/p).write_text(s,encoding='utf-8')
def edit(p,a,b):
 f=R/p;s=f.read_text('utf-8');assert a in s,(p,a);f.write_text(s.replace(a,b),encoding='utf-8')
edit('entity/Payment.java','String transactionCode;','''@Column(unique=true) String transactionCode;
    @Column(unique=true) Long providerCode;
    String providerLinkId;
    @Column(length=2000) String checkoutUrl;
    String purpose;
    Integer receivedAmount;
    LocalDateTime expiresAt;''')
edit('repository/PaymentRepository.java','public interface PaymentRepository extends JpaRepository<Payment, Long> {','''public interface PaymentRepository extends JpaRepository<Payment, Long> {
    java.util.List<Payment> findByRentalOrderIdOrderByCreatedAtDesc(Long id);
    java.util.Optional<Payment> findByProviderCode(Long code);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Payment p where p.id=:id")
    java.util.Optional<Payment> lockById(@org.springframework.data.repository.query.Param("id") Long id);
''')
put('service/PaymentService.java','''package com.motorrent.motorbikerental.service;
import com.motorrent.motorbikerental.entity.*; import com.motorrent.motorbikerental.repository.*; import com.motorrent.motorbikerental.enums.RentalStatus;
import com.motorrent.motorbikerental.security.CurrentUser;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Value; import org.springframework.web.server.ResponseStatusException; import org.springframework.http.HttpStatus;
import java.time.*; import java.util.*; import vn.payos.PayOS; import vn.payos.type.*;
@Service @RequiredArgsConstructor
public class PaymentService {
 private final PayOS payOS; private final RentalOrderRepository orders; private final PaymentRepository payments;
 private final CurrentUser current; private final TransactionTemplate tx;
 @Value("${app.base-url:http://localhost:8080}") private String baseUrl;
 public String createPaymentLink(Long orderId) {
  Payment payment=tx.execute(s->{
   var order=orders.lockById(orderId).orElseThrow(()->new IllegalArgumentException("Không tìm thấy đơn")); current.owns(order);
   if(order.getStatus()!=RentalStatus.PENDING_PAYMENT || order.getPaymentExpiresAt()==null || !order.getPaymentExpiresAt().isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Đơn không còn trong thời gian thanh toán");
   var existing=payments.findByRentalOrderIdOrderByCreatedAtDesc(orderId).stream().filter(p->"PAYOS".equals(p.getPaymentMethod()) && "DEPOSIT".equals(p.getPurpose())).findFirst();
   if(existing.isPresent()) return existing.get();
   if(order.getDepositDue()==null || order.getDepositDue()<=0) throw new IllegalArgumentException("Đơn chưa có khoản cọc hợp lệ");
   var p=Payment.builder().rentalOrder(order).amount(order.getDepositDue()).paymentMethod("PAYOS").purpose("DEPOSIT").status("PENDING").expiresAt(order.getPaymentExpiresAt()).build();
   payments.saveAndFlush(p); p.setProviderCode(p.getId()); return payments.save(p);
  });
  if(payment.getCheckoutUrl()!=null) return payment.getCheckoutUrl();
  try {
   var data=PaymentData.builder().orderCode(payment.getProviderCode()).amount(payment.getAmount()).description("Coc don "+orderId)
    .returnUrl(baseUrl+"/customer/payment-result?orderId="+orderId).cancelUrl(baseUrl+"/customer/payment-result?orderId="+orderId)
    .expiredAt(payment.getExpiresAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toEpochSecond())
    .item(ItemData.builder().name("Dat coc thue xe").price(payment.getAmount()).quantity(1).build()).build();
   var response=payOS.createPaymentLink(data);
   tx.executeWithoutResult(s->{var p=payments.lockById(payment.getId()).orElseThrow();p.setCheckoutUrl(response.getCheckoutUrl());p.setProviderLinkId(response.getPaymentLinkId());payments.save(p);});
   return response.getCheckoutUrl();
  } catch(Exception e) {
   // Keep the persisted provider code for reconciliation after a timeout.
   throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Chưa tạo được liên kết PayOS. Đơn vẫn được lưu; vui lòng thử lại hoặc liên hệ cửa hàng.");
  }
 }
 public void webhook(Webhook body) {
  WebhookData data;
  try { data=payOS.verifyPaymentWebhookData(body); }
  catch(Exception e) { throw new IllegalArgumentException("Chữ ký thanh toán không hợp lệ"); }
  if(!"00".equals(data.getCode())) return;
  apply(data.getOrderCode(),data.getAmount(),data.getPaymentLinkId(),data.getReference());
 }
 public void reconcile(Long orderId) {
  var order=orders.findById(orderId).orElseThrow(); current.canRead(order);
  for(var p:payments.findByRentalOrderIdOrderByCreatedAtDesc(orderId)) {
   if(p.getProviderCode()==null || !"PENDING".equals(p.getStatus())) continue;
   try { var remote=payOS.getPaymentLinkInformation(p.getProviderCode());
    if("PAID".equals(remote.getStatus())) apply(remote.getOrderCode(),remote.getAmountPaid(),remote.getId(),"PAYOS-"+remote.getId());
   } catch(Exception e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Chưa đối soát được với PayOS. Vui lòng thử lại."); }
  }
 }
 public void apply(Long code,Integer received,String link,String reference) {
  var initial=payments.findByProviderCode(code).orElse(null); if(initial==null) return;
  tx.executeWithoutResult(s->{
   var order=orders.lockById(initial.getRentalOrder().getId()).orElseThrow();
   var p=payments.lockById(initial.getId()).orElseThrow();
   if(!"PENDING".equals(p.getStatus())) return;
   if(p.getProviderLinkId()!=null && !Objects.equals(p.getProviderLinkId(),link)) throw new IllegalArgumentException("Mã giao dịch không khớp");
   p.setProviderLinkId(link); p.setReceivedAmount(received); p.setPaidAt(LocalDateTime.now());
   p.setTransactionCode(reference==null?"PAYOS-"+code:reference);
   if(!Objects.equals(received,p.getAmount()) || order.getStatus()!=RentalStatus.PENDING_PAYMENT || p.getExpiresAt().isBefore(LocalDateTime.now())) {
    p.setStatus("REVIEW_REQUIRED"); // Money received after expiry or with a mismatch must be reconciled by staff.
   } else { p.setStatus("PAID"); order.setDepositPaid(p.getAmount()); order.setStatus(RentalStatus.PENDING); orders.save(order); }
   payments.save(p);
  });
 }
 public java.util.List<java.util.Map<String,Object>> list(Long id) {
  current.canRead(orders.findById(id).orElseThrow());
  return payments.findByRentalOrderIdOrderByCreatedAtDesc(id).stream().map(p->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",p.getId());m.put("amount",p.getAmount());m.put("receivedAmount",p.getReceivedAmount());m.put("status",p.getStatus());m.put("method",p.getPaymentMethod());m.put("purpose",p.getPurpose());m.put("paidAt",p.getPaidAt());return m;}).toList();
 }
}
''')
put('controller/api/PaymentController.java','''package com.motorrent.motorbikerental.controller.api;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import java.util.Map;
import com.motorrent.motorbikerental.service.PaymentService;
@RestController @RequestMapping("/api/payment") @RequiredArgsConstructor
public class PaymentController {
 private final PaymentService payments;
 @PostMapping("/create/{orderId}") public Map<String,String> create(@PathVariable Long orderId) { return Map.of("checkoutUrl",payments.createPaymentLink(orderId)); }
 @PostMapping("/payos-webhook") public Map<String,String> webhook(@RequestBody vn.payos.type.Webhook body) { payments.webhook(body);return Map.of("message","OK"); }
 @PostMapping("/reconcile/{orderId}") public Object reconcile(@PathVariable Long orderId) { payments.reconcile(orderId);return payments.list(orderId); }
 @GetMapping("/order/{orderId}") public Object list(@PathVariable Long orderId) { return payments.list(orderId); }
}
''')
