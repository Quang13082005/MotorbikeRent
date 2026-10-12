package com.motorrent.motorbikerental.dto.response;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionResponse {
    Long id;
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
    Integer usedCount;
    String status;
}