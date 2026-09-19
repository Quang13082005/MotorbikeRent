package com.motorrent.motorbikerental.dto.response;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceResponse {
    Long id;
    String type;
    String description;
    Integer cost;
    String status;
    LocalDateTime startedAt;
    LocalDateTime completedAt;
}