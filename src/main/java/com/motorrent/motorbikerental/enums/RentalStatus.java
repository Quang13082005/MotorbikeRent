package com.motorrent.motorbikerental.enums;

public enum RentalStatus {
    PENDING,           // Chờ xác nhận
    CONFIRMED,         // Đã xác nhận
    READY_FOR_HANDOVER, // Sẵn sàng bàn giao
    HANDED_OVER,       // Đã bàn giao (đang thuê)
    RETURN_REQUESTED,  // Yêu cầu trả xe
    RETURNED,          // Đã trả xe
    COMPLETED,         // Hoàn tất
    CANCELLED,         // Đã hủy
    REJECTED,          // Từ chối
    OVERDUE,           // Quá hạn
    EXTENDED           // Đã gia hạn
}
