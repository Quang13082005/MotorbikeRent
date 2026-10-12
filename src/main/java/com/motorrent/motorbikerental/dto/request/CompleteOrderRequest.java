package com.motorrent.motorbikerental.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompleteOrderRequest {
    Integer refundAmount;
    String refundReason;
    String paymentMethod;
    String note;

    Integer assetDepositRefundAmount;
    Integer assetDepositDeductionAmount;
    String depositDeductionReason;
    String depositRefundMethod;

    Integer lateFee;
    String lateFeeReason;
    String lateFeePaymentMethod;
}
