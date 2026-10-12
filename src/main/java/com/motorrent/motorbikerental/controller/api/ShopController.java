package com.motorrent.motorbikerental.controller.api;
@org.springframework.web.bind.annotation.RestController @lombok.RequiredArgsConstructor
public class ShopController {
 private final com.motorrent.motorbikerental.service.PricingService pricing;
 @org.springframework.web.bind.annotation.GetMapping("/api/shop") public Object get() {return pricing.settings();}
}
