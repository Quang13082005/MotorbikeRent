package com.motorrent.motorbikerental.controller.api;

import com.motorrent.motorbikerental.dto.request.DuringRentalQuoteRequest;
import com.motorrent.motorbikerental.service.DuringRentalQuoteService;
import com.motorrent.motorbikerental.service.DuringRentalChangeService;
import com.motorrent.motorbikerental.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class DuringRentalQuoteController {
    private final DuringRentalQuoteService service;
    private final DuringRentalChangeService changes;
    private final PaymentService payments;
    @PostMapping("/api/operations/orders/{id}/during-rental-changes")
    public Object propose(@PathVariable Long id,@RequestBody DuringRentalQuoteRequest request){return java.util.Map.of("id",changes.propose(id,request));}
    @PostMapping("/api/operations/during-rental-changes/{id}/confirm")
    public Object confirm(@PathVariable Long id,@RequestBody DuringRentalChangeService.Confirmation request){changes.confirm(id,request);return java.util.Map.of("message","Đã xác nhận đổi xe thực tế");}
    @DeleteMapping("/api/operations/during-rental-changes/{id}")
    public Object cancel(@PathVariable Long id){changes.cancel(id);return java.util.Map.of("message","Đã hủy phương án");}
    @PostMapping("/api/payment/during-rental-change/{id}")
    public Object pay(@PathVariable Long id){return java.util.Map.of("checkoutUrl",payments.createDuringChangePaymentLink(id));}

    @GetMapping("/api/operations/orders/{id}/during-rental-replacement-options")
    public Object options(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime changeTime,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime returnTime) {
        return service.options(id, changeTime, returnTime);
    }

    @PostMapping("/api/operations/orders/{id}/during-rental-changes/quote")
    public Object quote(@PathVariable Long id, @RequestBody DuringRentalQuoteRequest request) {
        return service.quote(id, request);
    }
}
