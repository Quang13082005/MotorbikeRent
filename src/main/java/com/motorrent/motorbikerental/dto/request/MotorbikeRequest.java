package com.motorrent.motorbikerental.dto.request;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MotorbikeRequest {
    String name;
    Long brandId;
    Long categoryId;
    Long stationId;
    String year;
    Integer cc;
    String transmission;
    String fuelConsumption;
    Integer pricePerDay;
    Integer pricePerWeek;
    Integer depositAmount;
    String highlights;
    String description;
    String licensePlate;
}