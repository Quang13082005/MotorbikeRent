package com.motorrent.motorbikerental.dto.request;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RentalOrderRequest {
    @jakarta.validation.constraints.NotNull
    Long motorbikeId;
    @jakarta.validation.constraints.NotNull
    LocalDateTime pickupDate;
    @jakarta.validation.constraints.NotNull
    LocalDateTime returnDate;
    String pickupType;
    String pickupAddress;
    String note;
    Integer depositAmount;
    String returnType;
    String returnAddress;
    Double pickupLat; Double pickupLng;
    Double returnLat; Double returnLng;
    Integer securityOption;
    Boolean termsAccepted;
    java.util.List<Person> people;
    public record Person(String name,String phone,String idCard,String birthDate,String address) {}
}