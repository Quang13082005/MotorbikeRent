package com.motorrent.motorbikerental.dto.response;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardStatsResponse {
    Map<String, Object> stats;
}