package com.motorrent.motorbikerental.controller.api;
@org.springframework.web.bind.annotation.RestController @lombok.RequiredArgsConstructor
public class ExtensionController {
 private final com.motorrent.motorbikerental.service.ExtensionService service;
 public record Request(java.time.LocalDateTime newReturnDate,String reason) {}
 @org.springframework.web.bind.annotation.PostMapping("/api/rental-orders/{id}/extensions")
 public Object request(@org.springframework.web.bind.annotation.PathVariable Long id,@org.springframework.web.bind.annotation.RequestBody Request request) {service.request(id,request.newReturnDate(),request.reason());return java.util.Map.of("message","Đã gửi yêu cầu gia hạn");}
 @org.springframework.web.bind.annotation.PostMapping("/api/operations/extensions/{id}")
 public Object decide(@org.springframework.web.bind.annotation.PathVariable Long id,@org.springframework.web.bind.annotation.RequestParam boolean approve) {var result=new java.util.LinkedHashMap<>(service.decide(id,approve));result.put("message",approve?"Đã duyệt; chờ khách thanh toán phí gia hạn. Ngày trả chưa thay đổi.":"Đã từ chối yêu cầu");return result;}
 @org.springframework.web.bind.annotation.GetMapping("/api/rental-orders/{id}/extensions")
 public Object history(@org.springframework.web.bind.annotation.PathVariable Long id){return service.forOrder(id);}
}
