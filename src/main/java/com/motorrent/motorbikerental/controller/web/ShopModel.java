package com.motorrent.motorbikerental.controller.web;
@org.springframework.web.bind.annotation.ControllerAdvice(basePackages="com.motorrent.motorbikerental.controller.web")
@lombok.RequiredArgsConstructor
public class ShopModel {
 private final com.motorrent.motorbikerental.repository.ShopSettingsRepository settings;
 @org.springframework.web.bind.annotation.ModelAttribute("display")
 public com.motorrent.motorbikerental.service.Display display(){return new com.motorrent.motorbikerental.service.Display();}
 @org.springframework.web.bind.annotation.ModelAttribute("shop")
 public com.motorrent.motorbikerental.entity.ShopSettings shop() {return settings.findById(1L).orElseGet(com.motorrent.motorbikerental.entity.ShopSettings::new);}
}
