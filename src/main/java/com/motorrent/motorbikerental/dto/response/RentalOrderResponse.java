package com.motorrent.motorbikerental.dto.response;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RentalOrderResponse {
    Long id;
    String orderCode;
    String motorbikeName;
    String customerName;
    String customerPhone;
    String customerAvatar;
    LocalDateTime pickupDate;
    LocalDateTime returnDate;
    Integer days;
    Integer totalPrice;
    String status;
    String pickupType;
    String pickupAddress;
    String rejectionReason;
    LocalDateTime createdAt;
    MotorbikeResponse motorbike;
    Integer depositPaid;
}