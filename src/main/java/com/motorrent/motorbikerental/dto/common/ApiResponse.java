package com.motorrent.motorbikerental.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    @Builder.Default
    int code = 1000;

    String message;
    T result;

    public static <T> ApiResponse<T> success(T result) {
        return ApiResponse.<T>builder()
                .code(1000)
                .message("Thành công")
                .result(result)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T result) {
        return ApiResponse.<T>builder()
                .code(1000)
                .message(message)
                .result(result)
                .build();
    }

    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Thành công")
                .build();
    }
}
