package com.motorrent.motorbikerental.service;

import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class FinancialSummaryService {
 private final PaymentRepository payments;
 private final RentalOrderRepository orders;
 private final CurrentUser current;
 @Transactional(readOnly=true)
 public Map<String,Object> forOrder(Long id){
  current.canRead(orders.findById(id).orElseThrow());
  long rentalNet=0,assetHeld=0,review=0;
  for(var p:payments.findByRentalOrderIdOrderByCreatedAtDesc(id)){
   if("REVIEW_REQUIRED".equals(p.getStatus())){review+=Objects.requireNonNullElse(p.getReceivedAmount(),0);continue;}
   if(!Financials.posted(p))continue;
   if(Financials.asset(p))assetHeld+=Financials.cash(p);else rentalNet+=Financials.cash(p);
  }
  return OperationsService.row("orderId",id,"rentalCashNet",rentalNet,"assetDepositHeld",assetHeld,"unreconciledAmount",review);
 }
}
