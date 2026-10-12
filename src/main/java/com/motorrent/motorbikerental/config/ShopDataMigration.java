package com.motorrent.motorbikerental.config;
import com.motorrent.motorbikerental.repository.ShopSettingsRepository;
import com.motorrent.motorbikerental.entity.ShopSettings;
import org.springframework.context.annotation.*;
@Configuration
public class ShopDataMigration {
 // Initial values are migrated from the existing storefront. Subsequent edits come from the database.
 @Bean org.springframework.boot.CommandLineRunner initializeShop(ShopSettingsRepository repository) {
  return args->{
   var s=repository.findById(1L).orElseGet(ShopSettings::new);
   if(s.getName()==null)s.setName("MotoRent Đà Nẵng");
   if(s.getAddress()==null)s.setAddress("11 Nguyễn Văn Tạo, P. An Khê, TP. Đà Nẵng");
   if(s.getRepresentative()==null)s.setRepresentative("Nguyễn Tấn Đạt");
   if(s.getPhone()==null)s.setPhone("0986780177");
   if(s.getEmail()==null)s.setEmail("Datlien789@gmail.com");
   if(s.getHours()==null)s.setHours("05:00 - 20:00 hàng ngày");
   if(s.getLat()==null)s.setLat(16.0526);if(s.getLng()==null)s.setLng(108.1718);
   if(s.getDeliveryBaseFee()==null)s.setDeliveryBaseFee(15000);
   if(s.getDeliveryPerKm()==null)s.setDeliveryPerKm(4000);
   if(s.getDeliveryBaseKm()==null)s.setDeliveryBaseKm(3);
   if(s.getDeliveryMaxKm()==null)s.setDeliveryMaxKm(30);
   if(s.getHoldMinutes()==null || s.getHoldMinutes()<15)s.setHoldMinutes(15);
   if(s.getDepositPercent()==null)s.setDepositPercent(50);
   repository.save(s);
  };
 }
}
