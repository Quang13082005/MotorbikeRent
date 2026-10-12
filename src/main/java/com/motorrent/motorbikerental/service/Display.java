package com.motorrent.motorbikerental.service;
@org.springframework.stereotype.Component("display")
public class Display {
 public String money(Number n) {return n==null?"Chưa có":java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("vi-VN")).format(n)+" ₫";}
 public String moneyVnd(Number n) {return n==null?"Chưa có":java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("vi-VN")).format(n)+" VNĐ";}
 public String date(java.time.LocalDateTime d) {return d==null?"Chưa có":d.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));}
 public String image(String u) {if(u==null || u.isBlank())return "/images/motorbike-placeholder.svg";if(u.startsWith("https://") || u.startsWith("http://"))return u;if(u.startsWith("//") || u.contains(":"))return "/images/motorbike-placeholder.svg";return u.startsWith("/")?u:"/"+u;}
 public String status(Object s) {if(s==null)return "Chưa có";return switch(s.toString()) {
  case "PENDING_PAYMENT"->"Chờ thanh toán cọc";case "PENDING"->"Chờ duyệt";case "CONFIRMED"->"Đã xác nhận";case "READY_FOR_HANDOVER"->"Sẵn sàng giao";
  case "HANDED_OVER","RENTED"->"Đang thuê";case "COMPLETED"->"Đã hoàn thành";case "CANCELLED"->"Đã hủy";case "REJECTED"->"Từ chối";
  case "EXTENDED"->"Đã gia hạn";case "OVERDUE"->"Quá hạn";case "RETURN_REQUESTED"->"Yêu cầu trả xe";case "RETURNED"->"Đã trả xe";
  case "AVAILABLE"->"Sẵn sàng";case "UNAVAILABLE"->"Tạm ngừng";case "MAINTENANCE"->"Bảo dưỡng";case "PAID"->"Đã nhận tiền";
  case "WAITING_FOR_PAYMENT"->"Chờ thanh toán gia hạn";case "EXPIRED"->"Hết hạn thanh toán";
  case "REVIEW_REQUIRED"->"Cần đối soát";case "REFUNDED"->"Đã hoàn tiền";case "APPROVED"->"Đã duyệt";case "ACTIVE"->"Đang hoạt động";case "BLOCKED"->"Đã khóa";default->s.toString();};}
}
