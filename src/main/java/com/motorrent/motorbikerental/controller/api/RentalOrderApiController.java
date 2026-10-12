package com.motorrent.motorbikerental.controller.api;
import com.motorrent.motorbikerental.dto.request.*;
import com.motorrent.motorbikerental.dto.response.*;
import com.motorrent.motorbikerental.service.RentalOrderService;
import com.motorrent.motorbikerental.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/rental-orders") @RequiredArgsConstructor
public class RentalOrderApiController {
 private final RentalOrderService service; private final CurrentUser current;
 @PostMapping public RentalOrderResponse create(@Valid @RequestBody RentalOrderRequest r) { return service.createOrder(r,current.customer().getId()); }
 @PostMapping("/quote") public java.util.Map<String,Object> quote(@Valid @RequestBody RentalOrderRequest r) { current.customer(); return service.quote(r); }
 @GetMapping("/my-orders") public List<RentalOrderResponse> mine() { return service.getCustomerOrders(current.customer().getId()); }
 @GetMapping("/{id}") public RentalDetailResponse detail(@PathVariable Long id) { service.requireReadable(id); return service.getOrderDetail(id); }
 @GetMapping("/pending") public List<RentalOrderResponse> pending() { current.operator(); return service.getPendingOrders(); }
 @PostMapping("/{id}/confirm") public RentalOrderResponse confirm(@PathVariable Long id) { return service.confirmOrder(id,current.operator().getId()); }
 @PostMapping("/{id}/reject") public RentalOrderResponse reject(@PathVariable Long id,@RequestParam String reason) { return service.rejectOrder(id,current.operator().getId(),reason); }
 @PostMapping("/{id}/hand-over") public RentalOrderResponse handover(@PathVariable Long id, @RequestBody(required = false) HandoverRequest request) { return service.handOverOrder(id, current.operator().getId(), request); }
 @PostMapping("/{id}/complete") public RentalOrderResponse complete(@PathVariable Long id, @RequestBody(required = false) CompleteOrderRequest request) { return service.completeOrder(id, request, current.operator().getId()); }
 @PostMapping("/{id}/early-return") public RentalOrderResponse earlyReturn(@PathVariable Long id, @RequestBody(required = false) EarlyReturnRequest request) { return service.requestEarlyReturn(id, request, current.customer().getId()); }
 @PostMapping("/{id}/cancel") public RentalOrderResponse cancel(@PathVariable Long id) { service.requireOwned(id); return service.cancelOrder(id); }
}
