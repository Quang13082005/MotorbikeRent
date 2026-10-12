package com.motorrent.motorbikerental;
import com.motorrent.motorbikerental.service.Financials;
import com.motorrent.motorbikerental.entity.Payment;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
class FinancialsTests {
 @Test void rejectMismatchedOrNegativeComponents(){
  assertThatThrownBy(()->Financials.components(Map.of("BOOKING_PAYMENT",300000,"DELIVERY_FEE",100000),500000)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->Financials.components(Map.of("BOOKING_PAYMENT",-1),-1)).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void legacyLateRefundIsNotLateFeeAndIsIdempotentlyNormalized(){
  var p=Payment.builder().amount(-60000).receivedAmount(-60000).purpose("LATE_REFUND").status("PAID").build();
  Financials.normalize(p);Financials.normalize(p);
  assertThat(p.getTransactionType()).isEqualTo("BOOKING_REFUND");assertThat(p.getAmount()).isEqualTo(60000);assertThat(p.getDirection()).isEqualTo("OUT");assertThat(Financials.cash(p)).isEqualTo(-60000);
 }
}
