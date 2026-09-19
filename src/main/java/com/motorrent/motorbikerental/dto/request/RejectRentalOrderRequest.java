package com.motorrent.motorbikerental.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RejectRentalOrderRequest {
    @NotBlank(message = "Lý do từ chối không được để trống")
    String reason;
}
