package com.motorrent.motorbikerental.dto.response;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MotorbikeResponse {
    Long id;
    String name;
    String brand;
    String category;
    String year;
    Integer cc;
    Integer pricePerDay;
    String status;
    Double rating;
    Integer reviewCount;
    String imagePrimary;
    Integer odoKm;
}