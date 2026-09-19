package com.motorrent.motorbikerental.dto.response;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RentalDetailResponse {
    Long id;
    String orderCode;
    String motorbikeName;
    String customerName;
    LocalDateTime pickupDate;
    LocalDateTime returnDate;
    Integer days;
    Integer totalPrice;
    String status;
    String pickupType;
    String rejectionReason;
    LocalDateTime createdAt;
    
    UserResponse customer;
    MotorbikeResponse motorbike;
    List<PaymentResponse> payments;
    List<Object> extensions;
}