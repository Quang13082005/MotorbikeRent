package com.motorrent.motorbikerental.dto.response;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MotorbikeDetailResponse {
    Long id;
    String name;
    String licensePlate;
    String color;
    Long assetValue;
    String brand;
    String category;
    String year;
    Integer cc;
    Integer pricePerDay;
    String status;
    Double rating;
    Integer reviewCount;
    String imagePrimary;
    
    List<String> allImages;
    String description;
    String highlights;
    Integer odoKm;
    String transmission;
    String fuelConsumption;
    Integer pricePerWeek;
    Integer depositAmount;
}