package com.motorrent.motorbikerental.dto.request;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionRequest {
    String code;
    String name;
    String description;
    String discountType;
    Integer discountValue;
    Integer minOrderAmount;
    Integer maxDiscount;
    LocalDateTime startDate;
    LocalDateTime endDate;
    Integer usageLimit;
}