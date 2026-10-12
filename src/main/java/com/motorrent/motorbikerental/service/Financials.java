package com.motorrent.motorbikerental.service;

import com.motorrent.motorbikerental.entity.Payment;
import java.util.*;

/** One cash movement per Payment. Components explain the amount, never add cash again. */
public final class Financials {
 private Financials() {}
 public static final Set<String> TYPES=Set.of("BOOKING_PAYMENT","BOOKING_REFUND","BOOKING_ADJUSTMENT_REFUND","RENTAL_REMAINING","EXTENSION_PAYMENT","DELIVERY_FEE","RETURN_FEE","VEHICLE_CHANGE_FEE","LATE_FEE","EARLY_RETURN_REFUND","ASSET_DEPOSIT","ASSET_DEPOSIT_ADDITIONAL","ASSET_DEPOSIT_REFUND","DEPOSIT_DEDUCTION","EARLY_PICKUP_FEE","OTHER");
 public static String type(String purpose){return switch(Objects.toString(purpose,"OTHER")){
  case "DEPOSIT"->"BOOKING_PAYMENT";case "BALANCE"->"RENTAL_REMAINING";case "SECURITY"->"ASSET_DEPOSIT";
  case "SECURITY_REFUND"->"ASSET_DEPOSIT_REFUND";case "DEPOSIT_REFUND","LATE_REFUND"->"BOOKING_REFUND";default->Objects.toString(purpose,"OTHER");};}
 public static String type(Payment p){return p.getTransactionType()==null?type(p.getPurpose()):p.getTransactionType();}
 public static boolean posted(Payment p){return List.of("PAID","REFUNDED").contains(Objects.toString(p.getStatus(),""));}
 public static boolean asset(Payment p){return Set.of("ASSET_DEPOSIT","ASSET_DEPOSIT_ADDITIONAL","ASSET_DEPOSIT_REFUND","DEPOSIT_DEDUCTION").contains(type(p));}
 public static int signedAmount(Payment p){return signed(p,p.getAmount());}
 public static Integer signedReceived(Payment p){return p.getReceivedAmount()==null?null:signed(p,p.getReceivedAmount());}
 private static int signed(Payment p,int amount){return "OUT".equals(p.getDirection())?-Math.absExact(amount):amount;}
 public static long cash(Payment p){return posted(p)?Objects.requireNonNullElse(signedReceived(p),signedAmount(p)):0;}
 public static String transactionStatus(Payment p){return posted(p)?"SUCCESS":p.getStatus();}
 public static void normalize(Payment p){
  if(p.getAmount()==null)throw new IllegalArgumentException("Thiếu số tiền");
  if(p.getTransactionType()==null)p.setTransactionType(type(p.getPurpose()));
  if(!TYPES.contains(p.getTransactionType()))throw new IllegalArgumentException("Loại giao dịch không hợp lệ");
  if(p.getDirection()==null)p.setDirection(p.getAmount()<0?"OUT":"IN");
  if(!Set.of("IN","OUT").contains(p.getDirection()))throw new IllegalArgumentException("Chiều tiền không hợp lệ");
  p.setAmount(Math.absExact(p.getAmount()));
  if(p.getReceivedAmount()!=null)p.setReceivedAmount(Math.absExact(p.getReceivedAmount()));
 }
 public static String components(Map<String,Integer> entries,int expected){
  long total=0;for(var entry:entries.entrySet()){
   if(!TYPES.contains(entry.getKey())||entry.getValue()==null||entry.getValue()<0)throw new IllegalArgumentException("Chi tiết khoản tiền không hợp lệ");
   total+=entry.getValue();
  }
  if(total!=expected)throw new IllegalArgumentException("Tổng chi tiết không bằng số tiền giao dịch");
  try{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(entries);}catch(Exception ex){throw new IllegalArgumentException("Không lưu được chi tiết khoản tiền");}
 }
}
