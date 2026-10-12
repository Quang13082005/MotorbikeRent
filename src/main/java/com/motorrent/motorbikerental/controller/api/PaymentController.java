package com.motorrent.motorbikerental.controller.api;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import java.util.Map;
import com.motorrent.motorbikerental.service.PaymentService;
@RestController @RequestMapping("/api/payment") @RequiredArgsConstructor
public class PaymentController {
 private final PaymentService payments;
 @PostMapping("/extension/{id}") public Map<String,String> extension(@PathVariable Long id){return Map.of("checkoutUrl",payments.createExtensionPaymentLink(id));}
 private final com.motorrent.motorbikerental.service.FinancialSummaryService summaries;
 @GetMapping("/order/{orderId}/summary") public Object summary(@PathVariable Long orderId){return summaries.forOrder(orderId);}
 @PostMapping("/create/{orderId}") public Map<String,String> create(@PathVariable Long orderId) { return Map.of("checkoutUrl",payments.createPaymentLink(orderId)); }
 @PostMapping("/payos-webhook") public Map<String,String> webhook(@RequestBody vn.payos.type.Webhook body) { payments.webhook(body);return Map.of("message","OK"); }
 @PostMapping("/reconcile/{orderId}") public Object reconcile(@PathVariable Long orderId) { payments.reconcile(orderId);return payments.list(orderId); }
 @GetMapping("/order/{orderId}") public Object list(@PathVariable Long orderId) { return payments.list(orderId); }
}
