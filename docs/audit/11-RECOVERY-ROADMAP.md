# 11 - LỘ TRÌNH KHÔI PHỤC VÀ HOÀN THIỆN (RECOVERY & COMPLETION ROADMAP)

Lộ trình từng bước có thứ tự ưu tiên cụ thể, phân rã công việc, tiêu chí nghiệm thu (Acceptance Criteria) và yêu cầu kiểm thử để đưa dự án MotoRent từ trạng thái kết thúc Audit sang trạng thái sẵn sàng vận hành thương mại.

---

## 1. Giai Đoạn 1: Sửa Lỗi Cấp Bách & Bảo Mật Hệ Thống (Priority 1: Hardening & Bugfixes)
*Mục tiêu: Đưa toàn bộ các chức năng hiện có vào trạng thái hoạt động hoàn hảo và an toàn tuyệt đối.*

### Nhóm việc 1.1: Bảo vệ thông tin bí mật (Secrets Protection)
- **Công việc:** Chuyển toàn bộ credentials trong `application.properties` sang biến môi trường (`${DB_PASSWORD}`, `${PAYOS_API_KEY}`...). Tạo file `application-local.properties.example` chuẩn cho môi trường dev.
- **Tiêu chí nghiệm thu (Acceptance Criteria):**
  - Không còn mật khẩu hoặc private key dạng văn bản rõ trong repository.
  - Ứng dụng đọc được credentials từ biến môi trường khi khởi chạy.

### Nhóm việc 1.2: Sửa lỗi tính năng quét CCCD OCR (Fix Bug MBR-001)
- **Công việc:** Cập nhật hàm `handleScanImage()` trong `rental-contract.html` để tự động đính kèm header `X-XSRF-TOKEN` từ cookie CSRF.
- **Tiêu chí nghiệm thu:**
  - Khách hàng bấm quét CCCD -> Nhận phản hồi `200 OK` từ `/api/ocr/cccd`.
  - Tự động điền đầy đủ số CCCD, họ tên, ngày sinh, địa chỉ vào các ô nhập liệu.

### Nhóm việc 1.3: Khởi tạo Git Version Control & `.gitignore`
- **Công việc:** Chạy `git init`, cấu hình file `.gitignore` loại trừ `target/`, các file `*.log`, file backup `.sql`, file nén `.zip`, thư mục `uploads/`. Thực hiện Initial Commit an toàn.
- **Tiêu chí nghiệm thu:** Git repository được quản lý chặt chẽ, không commit các file nhạy cảm.

---

## 2. Giai Đoạn 2: Tối Ưu Hóa Trải Nghiệm Khách Hàng & Vận Hành (Priority 2: Operations & UX)
*Mục tiêu: Nâng cao sự thuận tiện cho cả khách hàng và nhân viên khi thực hiện giao dịch thực tế.*

### Nhóm việc 2.1: Tăng thời hạn thanh toán giữ chỗ đơn hàng
- **Công việc:** Thay đổi cấu hình `holdMinutes` trong `shop_settings` từ 3 phút thành **10 phút**.
- **Tiêu chí nghiệm thu:** Đơn hàng tạo mới có thời hạn thanh toán là 10 phút, khách hàng có đủ thời gian mở app ngân hàng quét mã VietQR.

### Nhóm việc 2.2: Bổ sung module Ảnh kiểm tra xe (Vehicle Photo Inspection)
- **Công việc:**
  - Bổ sung 4 trường ảnh vào bảng `rental_contracts` (hoặc bảng phụ `rental_inspection_photos`): `photo_front`, `photo_rear`, `photo_left`, `photo_right`, `photo_odo`.
  - Trên màn hình Staff Handover và Staff Return, bổ sung nút mở camera điện thoại để chụp ảnh xe thực tế.
- **Tiêu chí nghiệm thu:**
  - Ảnh xe lúc giao và nhận được lưu trữ an toàn, hiển thị trong chi tiết hợp đồng để đối chiếu khi có tranh chấp trầy xước.

### Nhóm việc 2.3: Tối ưu hiển thị bảng di động (Mobile Card View)
- **Công việc:** Cải tiến `rental-history.html` và `rental-orders.html` để tự động chuyển từ dạng bảng sang dạng danh sách thẻ (Card list) khi màn hình có độ rộng nhỏ hơn 640px.
- **Tiêu chí nghiệm thu:** Không còn hiện tượng tràn ngang màn hình trên thiết bị di động.

---

## 3. Giai Đoạn 3: Triển Khai PWA & Trực Quan Hóa Quản Trị (Priority 3: PWA & Admin Charts)
*Mục tiêu: Hoàn thiện Phase 7 (PWA) và nâng cấp Phase 8 (Admin Dashboard).*

### Nhóm việc 3.1: Hoàn thiện Phase 7 - PWA (Progressive Web App)
- **Công việc:**
  - Tạo file `src/main/resources/static/manifest.json` định nghĩa tên app, màu theme, icon các kích thước (192x192, 512x512).
  - Đăng ký Service Worker `sw.js` để cache các tài nguyên tĩnh (`css/`, `js/`, font chữ, logo) và cung cấp offline fallback page.
- **Tiêu chí nghiệm thu:**
  - Trình duyệt Chrome / Safari trên điện thoại hiển thị banner cài đặt "Thêm vào màn hình chính" (Add to Home Screen).
  - Tốc độ tải trang lặp lại (repeat visit) dưới 500ms nhờ cache tài nguyên tĩnh.

### Nhóm việc 3.2: Tích hợp Biểu đồ Doanh thu Admin
- **Công việc:** Tích hợp thư viện Chart.js vào trang `admin/reports.html` để trực quan hóa dữ liệu từ `/api/admin/reports`.
- **Tiêu chí nghiệm thu:** Hiển thị biểu đồ cột doanh thu thuần theo ngày, biểu đồ tròn cơ cấu thanh toán (PayOS vs Tiền mặt).

---

## 4. Giai Đoạn 4: Trợ Lý AI Du Lịch & Chuẩn Bị Triển Khai (Priority 4: AI & Deployment)
*Mục tiêu: Tận dụng dữ liệu địa điểm du lịch và chuẩn bị vận hành môi trường thương mại.*

### Nhóm việc 4.1: Kích hoạt Trợ lý Du lịch Thông Minh (Spring AI)
- **Công việc:** Sử dụng dữ liệu từ bảng `travel_places` kết hợp với OpenAI / Spring AI để cung cấp tính năng "Gợi ý lịch trình phượt Đà Nẵng bằng xe máy".
- **Tiêu chí nghiệm thu:** Khách hàng có thể chọn số ngày du lịch (1 ngày, 2 ngày, 3 ngày) và nhận được gợi ý lộ trình kèm dòng xe máy phù hợp.

### Nhóm việc 4.2: Đóng gói Container & Triển khai Production
- **Công việc:** Tạo `Dockerfile` đa tầng (Multi-stage build với Eclipse Temurin 21) và `docker-compose.yml` gồm MySQL 9.0 và MotoRent Spring Boot.
- **Tiêu chí nghiệm thu:** Khởi chạy toàn bộ hệ thống bằng một lệnh duy nhất: `docker compose up -d`.
