package com.motorrent.motorbikerental.controller.api;
import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import com.motorrent.motorbikerental.service.SettlementService;import java.util.Map;
@RestController @RequestMapping("/api/operations") @RequiredArgsConstructor
public class SettlementController {
 private final SettlementService service;
 public record Cash(String purpose,String reference,String method,String note){}
 public record Inspection(String before,String after,Integer odoBefore,Integer odoAfter){}
 @GetMapping("/orders/{id}/inspection") public Object inspection(@PathVariable Long id){return service.inspectionDetails(id);}
 @PostMapping("/orders/{id}/cash") public Object cash(@PathVariable Long id,@RequestBody Cash r){service.cash(id,r.purpose(),r.reference(),r.method(),r.note());return Map.of("message","Đã ghi nhận");}
 @PutMapping("/orders/{id}/inspection") public Object inspection(@PathVariable Long id,@RequestBody Inspection r){service.inspection(id,r.before(),r.after(),r.odoBefore(),r.odoAfter());return Map.of("message","Đã lưu biên bản");}
 @PostMapping("/payments/{id}/refund") public Object refund(@PathVariable Long id,@RequestBody Cash r){service.refundReview(id,r.reference(),r.method(),r.note());return Map.of("message","Đã ghi nhận hoàn tiền");}
}
