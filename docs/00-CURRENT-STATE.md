> QUY TẮC MỚI NHẤT: Đã bỏ checkbox và bước chuẩn bị giao. Staff chỉ bấm Đã giao xe / Hoàn tất đơn; không tự ghi tiền, không chặn hoàn tất vì số tiền chưa ghi nhận. Xem đầu [KET-QUA](continuation-20260928/KET-QUA.md). Mô tả cũ bên dưới đã bị thay thế.

> CẬP NHẬT MỚI NHẤT 28/09/2026: đọc [kết quả tiếp tục](continuation-20260928/KET-QUA.md) trước. Dự án hiện tại D:/Motorbike-rental. Yêu cầu mới đã thay thế đề xuất xe điện và bàn giao bắt buộc biên bản: bỏ xe điện; bàn giao bằng hai checkbox, không giao sớm. Nội dung cũ phía dưới là lịch sử, không phải yêu cầu mới của người dùng.

# Tình Trạng Hiện Tại của Dự Án (CURRENT STATE)

## Tổng Quan
*   **Tên dự án:** MotoRent (motorbike-rental)
*   **Kho lưu trữ (Git):** Đã được khởi tạo (commit ban đầu `chore: initial commit before mobile migration`).
*   **Java Version:** 17
*   **Spring Boot Version:** 4.1.1
*   **Build Tool:** Maven (`pom.xml`)
*   **Database:** MySQL (File script `motorent_db.sql` hiện có)

## Tình Trạng Hiện Tại (Web UI)
*   Website hiện tại được thiết kế theo hướng **Desktop-first**.
*   **Responsive:** Khá hạn chế trên thiết bị di động. Một số component (như Header) vừa được tôi sửa lỗi responsive cơ bản, nhưng toàn bộ trải nghiệm chưa phải là Mobile-first (ví dụ form đặt xe, các bảng quản lý...).
*   **Role phân quyền:** Có sẵn CUSTOMER, STAFF, ADMIN thông qua Spring Security (sử dụng kết hợp Cookie session cho web).
*   **Các trang đã có (Dạng HTML tĩnh / Thymeleaf):**
    *   **Customer:** Home, Motorbike List, Motorbike Detail, Rental Contract, Payment, Review, Profile, v.v.
    *   **Staff:** Dashboard, Pending Rentals (Rental Orders), Handover, Return, Maintenance, Profile.
    *   **Admin:** Customers, Staffs, Motorbikes, Stations, Promotions, Reports, Settings.
    *   **Auth:** Login, Register, Forgot Password.

## Kiến Trúc (Architecture)
*   Dự án sử dụng kiến trúc chuẩn Spring Boot: `Controller -> Service -> Repository -> Database`.
*   Có sử dụng `MapStruct` để ánh xạ giữa `Entity` và `DTO`.
*   Giao diện người dùng sử dụng **Thymeleaf**, được render từ phía server (SSR).
*   Design System dựa trên Tailwind CSS, sử dụng cấu hình chung tại `tailwind-config.js` và `motorent.css`.

## Phát Hiện Lỗi & Tồn Đọng
*   Lỗi Session văng đăng nhập đã được xử lý bằng cách cấu hình lại Spring Security (`STATELESS` -> Web Session).
*   Nhiều màn hình Admin và Staff hiện tại dùng các thẻ `<table>` truyền thống, sẽ bị vỡ layout trên thiết bị di động nhỏ.
*   Chưa có manifest chuẩn để hoạt động như một PWA.
