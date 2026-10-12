package com.motorrent.motorbikerental.controller.api;
import com.motorrent.motorbikerental.service.*;
import com.motorrent.motorbikerental.dto.request.VehicleChangeRequest;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.Map;

@RestController @RequiredArgsConstructor
public class VehicleChangeController {
 private final VehicleChangeService service;
 private final PaymentService payments;
 @GetMapping("/api/operations/orders/{id}/replacement-options") public Object options(@PathVariable Long id){return service.options(id);}
 @PostMapping("/api/operations/orders/{id}/vehicle-changes/quote") public Object quote(@PathVariable Long id,@RequestBody VehicleChangeRequest r){return service.preview(id,r);}
 @PostMapping("/api/operations/orders/{id}/vehicle-changes") public Object propose(@PathVariable Long id,@RequestBody VehicleChangeRequest r){return service.propose(id,r);}
 @GetMapping("/api/rental-orders/{id}/vehicle-changes") public Object history(@PathVariable Long id){return service.history(id);}
 @DeleteMapping("/api/operations/vehicle-changes/{id}") public Object cancel(@PathVariable Long id){service.cancel(id);return Map.of("message","Đã hủy đề nghị");}
 @PostMapping("/api/payment/vehicle-change/{id}") public Object pay(@PathVariable Long id){return Map.of("checkoutUrl",payments.createChangePaymentLink(id));}
 @PostMapping("/api/operations/orders/{id}/reject-with-refund") public Object reject(@PathVariable Long id,@RequestBody VehicleChangeService.Refund r){service.refund(id,r,true);return Map.of("message","Đã ghi nhận hoàn tiền và từ chối đơn");}
 @PostMapping("/api/operations/orders/{id}/adjustment-refund") public Object refund(@PathVariable Long id,@RequestBody VehicleChangeService.Refund r){service.refund(id,r,false);return Map.of("message","Đã ghi nhận hoàn tiền chênh lệch");}
}
