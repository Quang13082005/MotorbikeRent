package com.motorrent.motorbikerental.service;
import com.motorrent.motorbikerental.entity.*; import com.motorrent.motorbikerental.repository.*; import com.motorrent.motorbikerental.dto.request.RentalOrderRequest;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import java.time.*; import java.util.*;
@Service @RequiredArgsConstructor
public class PricingService {
 private final ShopSettingsRepository settings;
 public ShopSettings settings() { return settings.findById(1L).orElseThrow(()->new IllegalArgumentException("Cửa hàng chưa cấu hình giá và giao nhận")); }
 public static void validateStart(LocalDateTime start){if(start==null || start.isBefore(LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).withSecond(0).withNano(0)))throw new IllegalArgumentException("Ngày nhận xe không được ở quá khứ");}
 public static int days(LocalDateTime start,LocalDateTime end) {
  if(start==null || end==null || !end.isAfter(start)) throw new IllegalArgumentException("Ngày trả phải sau ngày nhận");
  long seconds=Duration.between(start,end).getSeconds();
  if(seconds>366L*86400) throw new IllegalArgumentException("Thời gian thuê tối đa 366 ngày");
  return Math.toIntExact((seconds+86399)/86400);
 }
 public int delivery(String type,String address,Double lat,Double lng) {
  if(type==null || "STORE".equals(type)) return 0;
  if(!"DELIVERY".equals(type) || address==null || address.isBlank() || lat==null || lng==null || !Double.isFinite(lat) || !Double.isFinite(lng) || Math.abs(lat)>90 || Math.abs(lng)>180) throw new IllegalArgumentException("Chọn địa chỉ và tọa độ giao nhận hợp lệ");
  var s=settings(); if(s.getLat()==null || s.getLng()==null) throw new IllegalArgumentException("Chưa cấu hình tọa độ cửa hàng");
  double a=Math.pow(Math.sin(Math.toRadians(lat-s.getLat())/2),2)+Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(s.getLat()))*Math.pow(Math.sin(Math.toRadians(lng-s.getLng())/2),2);
  double km=6371*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
  if(km>s.getDeliveryMaxKm()) throw new IllegalArgumentException("Địa chỉ ngoài phạm vi giao nhận của cửa hàng");
  return Math.addExact(s.getDeliveryBaseFee(),Math.multiplyExact((int)Math.ceil(Math.max(0,km-s.getDeliveryBaseKm())),s.getDeliveryPerKm()));
 }
 public static int securityDeposit(Motorbike bike){return bike.getCategory()!=null && bike.getCategory().getSecurityDeposit()!=null?bike.getCategory().getSecurityDeposit():Objects.requireNonNullElse(bike.getDepositAmount(),0);}
 public Map<String,Object> quote(RentalOrderRequest r,Motorbike bike) {
  validateStart(r.getPickupDate());
  if(r.getSecurityOption()!=null && r.getSecurityOption()!=1 && r.getSecurityOption()!=2)throw new IllegalArgumentException("Lựa chọn giấy tờ không hợp lệ");
  int days=days(r.getPickupDate(),r.getReturnDate());
  int rental=Math.multiplyExact(days,bike.getPricePerDay());
  int pickupFee=delivery(r.getPickupType(),r.getPickupAddress(),r.getPickupLat(),r.getPickupLng());
  int returnFee=delivery(r.getReturnType(),r.getReturnAddress(),r.getReturnLat(),r.getReturnLng());
  int delivery=Math.addExact(pickupFee,returnFee);
  int discount=0;
  int security=Integer.valueOf(2).equals(r.getSecurityOption())?0:securityDeposit(bike);
  int due=Math.addExact(Math.toIntExact((long)(rental-discount)*settings().getDepositPercent()/100),delivery);
  var result=new LinkedHashMap<String,Object>(); result.put("pickupFee",pickupFee);result.put("returnFee",returnFee);
  result.putAll(Map.of("days",days,"dailyRate",bike.getPricePerDay(),"rentalFee",rental,"deliveryFee",delivery,"securityDeposit",security,"depositDue",due,"totalPrice",Math.addExact(rental-discount,delivery),"discountAmount",discount,"holdMinutes",settings().getHoldMinutes()));return result;
 }
}
