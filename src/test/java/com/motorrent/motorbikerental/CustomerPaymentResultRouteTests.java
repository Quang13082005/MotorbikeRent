package com.motorrent.motorbikerental;
import org.junit.jupiter.api.Test;
import com.motorrent.motorbikerental.service.PricingService;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;
class CustomerPaymentResultRouteTests {
 @Test void rentalDaysKeepPartialHours() {
  var start=LocalDateTime.of(2026,9,26,10,0);
  assertThat(PricingService.days(start,start.plusHours(24))).isEqualTo(1);
  assertThat(PricingService.days(start,start.plusHours(24).plusMinutes(1))).isEqualTo(2);
  assertThatThrownBy(()->PricingService.days(start,start)).isInstanceOf(IllegalArgumentException.class);
 }
}
