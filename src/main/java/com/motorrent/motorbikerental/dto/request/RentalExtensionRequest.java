package com.motorrent.motorbikerental.dto.request;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RentalExtensionRequest {
    Long orderId;
    LocalDateTime newReturnDate;
    String reason;
}