package com.motorrent.motorbikerental.exception;

import com.motorrent.motorbikerental.dto.common.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bắt AppException có mã lỗi cụ thể
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException e) {
        ErrorCode errorCode = e.getErrorCode();
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(response);
    }

    /**
     * Bắt lỗi validate @Valid trong @RequestBody (DTO)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Dữ liệu không hợp lệ";
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(ErrorCode.INVALID_KEY.getCode())
                .message(message)
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Bắt lỗi validate @RequestParam, @PathVariable
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Dữ liệu không hợp lệ");
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(ErrorCode.INVALID_KEY.getCode())
                .message(message)
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 404 - Resource not found
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception e) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(ErrorCode.RESOURCE_NOT_FOUND.getCode())
                .message("Trang hoặc tài nguyên không tồn tại")
                .build();
        return ResponseEntity.status(404).body(response);
    }

    /**
     * Bắt tất cả Exception chưa xác định
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .message("Lỗi hệ thống: " + e.getMessage())
                .build();
        return ResponseEntity.internalServerError().body(response);
    }
}
