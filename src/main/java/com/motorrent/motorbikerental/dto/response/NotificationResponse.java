package com.motorrent.motorbikerental.dto.response;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
    Long id;
    String title;
    String message;
    String type;
    Boolean isRead;
    String link;
    LocalDateTime createdAt;
}