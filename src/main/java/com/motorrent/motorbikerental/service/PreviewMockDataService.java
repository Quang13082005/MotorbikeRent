package com.motorrent.motorbikerental.service;

import com.motorrent.motorbikerental.enums.MotorbikeStatus;
import com.motorrent.motorbikerental.enums.RentalStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock data service cho Preview mode.
 * Tất cả dữ liệu mẫu được tập trung tại đây — không hardcode trong template.
 * Dữ liệu này chỉ dùng cho việc xem giao diện, chưa phải nghiệp vụ backend thật.
 */
@Service
public class PreviewMockDataService {

    // =============================================
    // MOTORBIKE DATA
    // =============================================
    public List<Map<String, Object>> getMockMotorbikes() {
        List<Map<String, Object>> list = new ArrayList<>();

        list.add(createMotorbike(1L, "Honda Air Blade 160 ABS", "Honda", "Xe tay ga",
                "2023", 300000, 350000, 150, "Tự động", "2.1L/100km",
                "ABS 2 kênh | Phanh đĩa trước sau | Smartkey", "AVAILABLE",
                4.9, 128, "images/mock/airblade160.jpg"));

        list.add(createMotorbike(2L, "Yamaha Exciter 155 VVA RC", "Yamaha", "Xe côn tay",
                "2024", 350000, 420000, 155, "Côn tay 6 số", "2.6L/100km",
                "VVA Engine | Phanh đĩa ABS | Odo 3.200km", "AVAILABLE",
                4.8, 95, "images/mock/exciter155.jpg"));

        list.add(createMotorbike(3L, "Honda SH Mode 125 ABS", "Honda", "Xe tay ga",
                "2023", 280000, 320000, 125, "Tự động", "1.9L/100km",
                "Khóa thông minh | Cốp rộng 20L | ABS", "AVAILABLE",
                4.7, 73, "images/mock/shmode125.jpg"));

        list.add(createMotorbike(4L, "Yamaha NVX 155 VVA", "Yamaha", "Xe tay ga",
                "2022", 270000, 300000, 155, "Tự động", "2.2L/100km",
                "Phanh đĩa cả 2 bánh | Cốp 25L | Kết nối Y-Connect", "RENTED",
                4.6, 61, "images/mock/nvx155.jpg"));

        list.add(createMotorbike(5L, "Honda Winner X 150 ABS", "Honda", "Xe số",
                "2023", 260000, 290000, 150, "Số 6 cấp", "2.0L/100km",
                "Phanh ABS | Đèn LED | Odo 8.500km", "AVAILABLE",
                4.5, 44, "images/mock/winner150.jpg"));

        list.add(createMotorbike(6L, "Suzuki GSX S150", "Suzuki", "Xe côn tay",
                "2023", 320000, 380000, 150, "Côn tay 6 số", "2.3L/100km",
                "Phanh đĩa 2 bánh | Full fairing | Odo 5.100km", "AVAILABLE",
                4.8, 38, "images/mock/gsxs150.jpg"));

        list.add(createMotorbike(7L, "Honda Vision 110 ABS", "Honda", "Xe tay ga",
                "2024", 220000, 250000, 110, "Tự động", "1.8L/100km",
                "Tiết kiệm nhiên liệu | Nhẹ | Phù hợp nội thành", "AVAILABLE",
                4.4, 182, "images/mock/vision110.jpg"));

        list.add(createMotorbike(8L, "VinFast Theon S", "VinFast", "Xe điện",
                "2024", 280000, 340000, 0, "Tự động", "Điện - 120km/sạc",
                "Pin 4.0kWh | Sạc nhanh 3h | App kết nối | Odo mới", "AVAILABLE",
                4.9, 22, "images/mock/theons.jpg"));

        list.add(createMotorbike(9L, "Yamaha Grande 125 Hybrid", "Yamaha", "Xe tay ga",
                "2023", 250000, 280000, 125, "Tự động Hybrid", "1.5L/100km",
                "Hybrid thật | Khởi động nhanh | Cốp 22L", "MAINTENANCE",
                4.6, 57, "images/mock/grande125.jpg"));

        return list;
    }

    private Map<String, Object> createMotorbike(Long id, String name, String brand, String category,
                                                 String year, int pricePerDay, int pricePerWeek,
                                                 int cc, String transmission, String fuel,
                                                 String highlights, String status,
                                                 double rating, int reviews, String image) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("brand", brand);
        m.put("category", category);
        m.put("year", year);
        m.put("pricePerDay", pricePerDay);
        m.put("pricePerDayFormatted", formatPrice(pricePerDay));
        m.put("pricePerWeek", pricePerWeek);
        m.put("cc", cc);
        m.put("transmission", transmission);
        m.put("fuel", fuel);
        m.put("highlights", highlights);
        m.put("status", status);
        m.put("statusLabel", getMotorbikeStatusLabel(status));
        m.put("statusColor", getMotorbikeStatusColor(status));
        m.put("rating", rating);
        m.put("reviews", reviews);
        m.put("image", image);
        return m;
    }

    // =============================================
    // RENTAL ORDERS DATA
    // =============================================
    public List<Map<String, Object>> getMockRentalOrders() {
        List<Map<String, Object>> list = new ArrayList<>();

        list.add(createRental(1L, "MR240001", "Honda Air Blade 160 ABS", "2023",
                "14/09/2024 08:00", "17/09/2024 18:00", 3, 900000,
                "COMPLETED", "Nguyễn Văn An", "0901234567", "Cửa hàng"));

        list.add(createRental(2L, "MR240002", "Yamaha Exciter 155 VVA RC", "2024",
                "18/09/2024 09:00", "20/09/2024 18:00", 2, 700000,
                "HANDED_OVER", "Trần Thị Bình", "0912345678", "Giao tận nơi"));

        list.add(createRental(3L, "MR240003", "Honda Vision 110 ABS", "2024",
                "20/09/2024 10:00", "22/09/2024 18:00", 2, 440000,
                "PENDING", "Lê Văn Cường", "0923456789", "Cửa hàng"));

        list.add(createRental(4L, "MR240004", "Honda SH Mode 125 ABS", "2023",
                "15/09/2024 08:00", "18/09/2024 18:00", 3, 840000,
                "CONFIRMED", "Phạm Thị Dung", "0934567890", "Giao tận nơi"));

        list.add(createRental(5L, "MR240005", "Yamaha NVX 155 VVA", "2022",
                "10/09/2024 08:00", "11/09/2024 18:00", 1, 270000,
                "OVERDUE", "Hoàng Văn Em", "0945678901", "Cửa hàng"));

        list.add(createRental(6L, "MR240006", "VinFast Theon S", "2024",
                "22/09/2024 09:00", "25/09/2024 18:00", 3, 840000,
                "PENDING", "Vũ Thị Fang", "0956789012", "Giao tận nơi"));

        return list;
    }

    private Map<String, Object> createRental(Long id, String orderCode, String motorbikeName,
                                              String motorbikeYear, String pickupDate, String returnDate,
                                              int days, int totalPrice, String status,
                                              String customerName, String customerPhone, String pickupType) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("orderCode", orderCode);
        m.put("motorbikeName", motorbikeName);
        m.put("motorbikeYear", motorbikeYear);
        m.put("pickupDate", pickupDate);
        m.put("returnDate", returnDate);
        m.put("days", days);
        m.put("totalPrice", totalPrice);
        m.put("totalPriceFormatted", formatPrice(totalPrice));
        m.put("status", status);
        m.put("statusLabel", getRentalStatusLabel(status));
        m.put("statusColor", getRentalStatusColor(status));
        m.put("statusBg", getRentalStatusBg(status));
        m.put("customerName", customerName);
        m.put("customerPhone", customerPhone);
        m.put("pickupType", pickupType);
        return m;
    }

    // =============================================
    // ADMIN STATISTICS DATA
    // =============================================
    public Map<String, Object> getAdminDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("revenueToday", "4.250.000");
        stats.put("revenueMonth", "87.600.000");
        stats.put("totalOrders", 342);
        stats.put("activeRentals", 18);
        stats.put("overdueRentals", 3);
        stats.put("availableMotorbikes", 67);
        stats.put("maintenanceMotorbikes", 4);
        stats.put("utilizationRate", 72);
        stats.put("totalCustomers", 1284);
        stats.put("totalStaff", 6);
        stats.put("cancelRate", 4.2);
        stats.put("avgRating", 4.8);
        return stats;
    }

    // =============================================
    // STAFF DASHBOARD DATA
    // =============================================
    public Map<String, Object> getStaffDashboardData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pendingOrders", 5);
        data.put("handoverToday", 3);
        data.put("returnToday", 4);
        data.put("overdueOrders", 2);
        data.put("extensionRequests", 1);
        data.put("maintenanceVehicles", 3);
        data.put("staffName", "Nguyễn Văn Nhân");
        data.put("shift", "Ca sáng: 07:00 - 15:00");
        data.put("stationName", "Chi nhánh Trung tâm - 15 Lê Lợi, Đà Nẵng");
        return data;
    }

    // =============================================
    // CUSTOMER PROFILE DATA
    // =============================================
    public Map<String, Object> getMockCustomerProfile() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", 1001L);
        p.put("fullName", "Nguyễn Văn An");
        p.put("email", "nguyenvanan@email.com");
        p.put("phone", "0901234567");
        p.put("idCard", "036095001234");
        p.put("driverLicense", "B2-036095001234");
        p.put("address", "123 Nguyễn Văn Linh, Đà Nẵng");
        p.put("memberSince", "03/2024");
        p.put("totalRentals", 8);
        p.put("totalSpent", "3.200.000");
        p.put("loyaltyPoints", 320);
        p.put("avatarUrl", "");
        p.put("verified", true);
        return p;
    }

    // =============================================
    // HELPERS
    // =============================================
    private String formatPrice(int price) {
        return String.format("%,d", price).replace(",", ".") + "đ";
    }

    private String getMotorbikeStatusLabel(String status) {
        return switch (status) {
            case "AVAILABLE" -> "Còn xe";
            case "RENTED" -> "Đang thuê";
            case "MAINTENANCE" -> "Bảo trì";
            case "RESERVED" -> "Đã đặt";
            default -> "Không khả dụng";
        };
    }

    private String getMotorbikeStatusColor(String status) {
        return switch (status) {
            case "AVAILABLE" -> "text-emerald-400";
            case "RENTED" -> "text-sky-400";
            case "MAINTENANCE" -> "text-amber-400";
            case "RESERVED" -> "text-purple-400";
            default -> "text-slate-400";
        };
    }

    public String getRentalStatusLabel(String status) {
        return switch (status) {
            case "PENDING" -> "Chờ xác nhận";
            case "CONFIRMED" -> "Đã xác nhận";
            case "READY_FOR_HANDOVER" -> "Sẵn sàng bàn giao";
            case "HANDED_OVER" -> "Đang thuê";
            case "RETURNED" -> "Đã trả xe";
            case "COMPLETED" -> "Hoàn tất";
            case "CANCELLED" -> "Đã hủy";
            case "OVERDUE" -> "Quá hạn";
            case "EXTENDED" -> "Đã gia hạn";
            default -> status;
        };
    }

    public String getRentalStatusColor(String status) {
        return switch (status) {
            case "PENDING" -> "text-amber-400";
            case "CONFIRMED" -> "text-sky-400";
            case "READY_FOR_HANDOVER" -> "text-blue-400";
            case "HANDED_OVER" -> "text-emerald-400";
            case "RETURNED", "COMPLETED" -> "text-slate-300";
            case "CANCELLED" -> "text-red-400";
            case "OVERDUE" -> "text-red-500";
            case "EXTENDED" -> "text-purple-400";
            default -> "text-slate-400";
        };
    }

    public String getRentalStatusBg(String status) {
        return switch (status) {
            case "PENDING" -> "bg-amber-500/10 border-amber-500/30";
            case "CONFIRMED" -> "bg-sky-500/10 border-sky-500/30";
            case "READY_FOR_HANDOVER" -> "bg-blue-500/10 border-blue-500/30";
            case "HANDED_OVER" -> "bg-emerald-500/10 border-emerald-500/30";
            case "RETURNED", "COMPLETED" -> "bg-slate-500/10 border-slate-500/30";
            case "CANCELLED" -> "bg-red-500/10 border-red-500/30";
            case "OVERDUE" -> "bg-red-600/15 border-red-600/40";
            case "EXTENDED" -> "bg-purple-500/10 border-purple-500/30";
            default -> "bg-slate-500/10 border-slate-500/20";
        };
    }
}
