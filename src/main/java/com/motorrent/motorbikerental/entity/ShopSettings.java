package com.motorrent.motorbikerental.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="shop_settings") @Data @NoArgsConstructor
public class ShopSettings {
 @Id private Long id=1L;
 private String name,address,phone,hours,zalo,email,representative;
 @Column(length=4000) private String terms;
 private Double lat,lng;
 private Integer deliveryBaseFee,deliveryPerKm,deliveryBaseKm,deliveryMaxKm,holdMinutes,depositPercent;
}
