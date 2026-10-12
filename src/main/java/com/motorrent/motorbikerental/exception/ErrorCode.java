package com.motorrent.motorbikerental.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ===== GENERAL =====
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(1002, "Không tìm thấy tài nguyên", HttpStatus.NOT_FOUND),
    PERMISSION_DENIED(1003, "Không có quyền truy cập", HttpStatus.FORBIDDEN),

    // ===== AUTH =====
    UNAUTHENTICATED(2001, "Chưa đăng nhập hoặc phiên đã hết hạn", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(2002, "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_EXISTS(2003, "Email đã được đăng ký", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS(2004, "Số điện thoại đã được đăng ký", HttpStatus.CONFLICT),
    ACCOUNT_LOCKED(2005, "Tài khoản đã bị khóa", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(2006, "Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),

    // ===== USER =====
    USER_NOT_FOUND(3001, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    USER_BLOCKED(3002, "Tài khoản bị khóa", HttpStatus.FORBIDDEN),
    USER_EXISTED(3003, "Username đã tồn tại", HttpStatus.CONFLICT),
    EMAIL_EXISTED(3004, "Email đã tồn tại", HttpStatus.CONFLICT),
    PASSWORD_CONFIRMATION_MISMATCH(3005, "Mật khẩu không khớp", HttpStatus.BAD_REQUEST),
    SESSION_EXPIRED(3006, "Phiên bản đã hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(3007, "Token đã hết hạn", HttpStatus.UNAUTHORIZED),

    // ===== MOTORBIKE =====
    MOTORBIKE_NOT_FOUND(4001, "Không tìm thấy xe máy", HttpStatus.NOT_FOUND),
    MOTORBIKE_NOT_AVAILABLE(4002, "Xe không còn khả dụng trong thời gian yêu cầu", HttpStatus.CONFLICT),

    // ===== RENTAL =====
    RENTAL_NOT_FOUND(5001, "Không tìm thấy đơn thuê", HttpStatus.NOT_FOUND),
    RENTAL_ORDER_NOT_FOUND(5001, "Không tìm thấy đơn thuê", HttpStatus.NOT_FOUND),
    RENTAL_CANNOT_CANCEL(5002, "Đơn thuê không thể hủy ở trạng thái hiện tại", HttpStatus.BAD_REQUEST),
    INVALID_RENTAL_STATUS(5002, "Trạng thái đơn không hợp lệ để thực hiện thao tác này", HttpStatus.BAD_REQUEST),
    RENTAL_DATE_INVALID(5003, "Ngày thuê không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE(5003, "Khoảng thời gian không hợp lệ", HttpStatus.BAD_REQUEST),
    RENTAL_ALREADY_RETURNED(5004, "Xe đã được trả", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
