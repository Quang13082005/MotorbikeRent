package com.motorrent.motorbikerental.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HandoverRequest {
    Integer remainingAmount;
    Integer securityDeposit;
    String paymentMethod;
    LocalDateTime actualPickupTime;
    Integer earlyPickupFee;
    String note;
}
