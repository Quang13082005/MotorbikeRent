# 02 - HIỆN TRẠNG HỆ THỐNG (CURRENT SYSTEM STATUS)

## 1. Trạng Thái Phát Triển Tổng Thể (Overall Readiness)
- **Tình trạng mã nguồn:** Mã nguồn Backend (Spring Boot 4.1.1) và Database (MySQL 9.0.1) đã hoàn thành xuất sắc chu kỳ kiểm định 9 giai đoạn về mặt logic nghiệp vụ tài chính và xử lý tranh chấp dữ liệu (concurrency locking).
- **Khả năng chạy runtime:** Ứng dụng khởi động ổn định, kết nối cơ sở dữ liệu bình thường, phục vụ đầy đủ cả giao diện Thymeleaf và hệ thống REST API.
- **Mức độ sẵn sàng cho khách hàng thật (Production Readiness):** **CHƯA SẴN SÀNG CHO KHÁCH HÀNG THẬT (NOT PRODUCTION-READY)**.
  - *Lý do:* Hệ thống đang chứa các thông tin bảo mật nhạy cảm hardcoded trong file cấu hình, giao diện người dùng trên thiết bị di động cần hoàn thiện trải nghiệm biểu mẫu/ảnh, tính năng OCR CCCD bị chặn bởi thiếu CSRF token trong request, tính năng PWA chưa được thiết lập, và việc tính giờ thuê xe hiện dùng phương pháp làm tròn 24h thay vì mốc 12h/20h theo kỳ vọng nghiệp vụ ban đầu.

---

## 2. Các Chức Năng Hoạt Động Tốt & Đã Xác Minh (Working Functions - Verified)

### 2.1. Phân quyền và Bảo mật (Authentication & Authorization)
- Đăng nhập, đăng xuất, cấp phát JWT qua Cookie HttpOnly (`access_token`) và Header Bearer.
- Thu hồi token hợp lệ khi người dùng chủ động đăng xuất hoặc đổi mật khẩu.
- Chống tấn công CSRF thông qua Cookie-based CSRF token rotation (`XSRF-TOKEN`).
- Phân quyền nghiêm ngặt 3 cấp (`CUSTOMER`, `STAFF`, `ADMIN`):
  - Khách hàng không thể đọc hoặc thao tác trên đơn thuê của khách hàng khác (Ngăn chặn IDOR).
  - Nhân viên bị chặn 403 Forbidden khi truy cập vào cấu hình hệ thống hoặc báo cáo tài chính của Admin.
  - Định danh nhân viên (`staffId`) luôn được lấy từ ngữ cảnh đã xác thực (SecurityContext), loại bỏ hoàn toàn việc tin cậy tham số do client gửi lên.

### 2.2. Vòng đời đơn thuê & Xử lý đồng thời (Rental Order Lifecycle & Concurrency)
- Tạo đơn thuê, tính cọc giữ xe và phí giao nhận chính xác.
- Khóa bi quan (Pessimistic Locking `SELECT ... FOR UPDATE` theo thứ tự ID xe tăng dần) đảm bảo loại trừ triệt để tình trạng Double Booking khi nhiều khách hàng cùng đặt một xe trong cùng khoảng thời gian.
- Bàn giao xe (`HANDED_OVER`): Thu tiền thuê còn lại và cọc tài sản (`ASSET_DEPOSIT`), cập nhật trạng thái xe thành `RENTED`.
- Đổi xe trước khi giao (`BEFORE_DELIVERY`): Tính lại giá toàn bộ thời gian thuê, giữ nguyên mã đơn và lịch sử thanh toán, tạo link thanh toán bổ sung nếu xe mới giá cao hơn hoặc ghi nhận hoàn cọc nếu xe mới rẻ hơn.
- Đổi xe trong khi thuê (`DURING_RENTAL`): Giữ nguyên chi phí của chặng đã sử dụng theo giá cũ, chỉ tính chặng mới theo giá xe mới, đảm bảo xe cũ chỉ được thu hồi khi nhân viên xác nhận thực tế.
- Gia hạn thuê xe (`EXTENSION`): Khóa xe tạm thời trong thời gian giữ chỗ, chỉ mở rộng thời gian trả khi đã xác thực thanh toán tiền gia hạn thành công.
- Trả xe sớm (`EARLY_RETURN`): Khách hàng gửi yêu cầu (trạng thái `RETURN_REQUESTED`), nhân viên đánh giá thực tế và ghi nhận hoàn tiền thuê (`EARLY_RETURN_REFUND`) có kiểm soát.
- Hoàn tất đơn (`COMPLETED`): Trả cọc tài sản (`ASSET_DEPOSIT_REFUND`), khấu trừ thiệt hại (`DEPOSIT_DEDUCTION`), thu phí quá hạn (`LATE_FEE`), tự động trả xe về trạng thái `AVAILABLE`.

### 2.3. Báo cáo tài chính theo dòng tiền (Admin Cash Flow Reports)
- API `/api/admin/reports` phân loại chính xác các dòng tiền:
  - Phân tách kênh thu: PayOS, Tiền mặt (`CASH`), Chuyển khoản (`BANK_TRANSFER`).
  - Doanh thu thuần thuê xe: `rentalNetRevenue = rentalCollected - rentalRefunded`.
  - Quỹ cọc tài sản đang giữ: `totalAssetDepositHeld = assetDepositIn - assetDepositRefunded - assetDepositDeducted`.
  - Lợi nhuận hoạt động: `operatingProfit = rentalNetRevenue + lateFeeCollected - maintenanceCost`.
  - Tuân thủ 100% các bất biến kế toán.

---

## 3. Các Chức Năng Bị Lỗi Hoặc Chưa Đồng Bộ (Broken Functions & Inconsistencies)

| ID | Chức năng bị ảnh hưởng | Hiện trạng thực tế | Nguyên nhân kỹ thuật | Mức độ |
|---|---|---|---|:---:|
| **MBR-001** | OCR quét CCCD tại hợp đồng thuê | Quét CCCD báo lỗi "Lỗi không xác định" hoặc không phản hồi JSON | Hàm `handleScanImage()` trong `rental-contract.html` dùng raw `fetch('/api/ocr/cccd')` mà không đính kèm header `X-XSRF-TOKEN`, dẫn đến bị Spring Security chặn 403 Forbidden | **HIGH** |
| **MBR-002** | Quy tắc tính ngày thuê xe | Tính trần số ngày theo chu kỳ 24h: `(seconds + 86399) / 86400` thay vì áp dụng mốc 12:00/20:00 | Hàm `PricingService.days()` chưa cài đặt logic mốc 12h và 20h như mô tả nghiệp vụ ban đầu | **MEDIUM** |
| **MBR-003** | Khôi phục đơn thanh toán trễ | Khi đơn bị tự động hủy, nếu khách hàng thanh toán PayOS hợp lệ thì đơn được chuyển về `PENDING`, nhưng nếu xe đã bị đơn khác đặt thì chuyển về `REVIEW_REQUIRED` | Chưa có giao diện hỗ trợ Staff xử lý hoàn tiền hoặc đổi xe nhanh từ trạng thái `REVIEW_REQUIRED` của đơn bị hủy | **MEDIUM** |
| **MBR-004** | Responsive bảng dữ liệu di động | Bảng lịch sử thuê xe và đơn hàng bị tràn ngang trên màn hình điện thoại (<400px) | Thiếu thẻ bọc `overflow-x-auto` và cấu hình bảng responsive cho viewport nhỏ | **LOW** |

---

## 4. Các Chức Năng Còn Thiếu (Missing Functions)
1. **PWA & Offline Capability (Phase 7):**
   - Chưa có file `manifest.json`.
   - Chưa đăng ký Service Worker (`sw.js`) để cache tài nguyên tĩnh hoặc hỗ trợ cài đặt Add to Home Screen.
2. **AI Gợi Ý Xe & Chatbot Thông Minh (Phase 9):**
   - Dependency `spring-ai-starter-model-openai` đã có nhưng cấu hình `spring.ai.openai.chat.enabled=false`.
   - Chưa có Controller/Service xử lý gợi ý xe theo sở thích hoặc hỗ trợ tự động hỏi đáp.
3. **Cơ chế tải lên ảnh kiểm tra xe thực tế (Vehicle Inspection Photos):**
   - Bảng `rental_contracts` có các trường text `vehicle_condition_before`, `vehicle_condition_after`, `odo_before`, `odo_after` nhưng chưa có tính năng upload ảnh chụp hiện trạng xe lúc giao và nhận.
4. **Push Notification (Web Push / SMS):**
   - Hiện chỉ có bảng `notifications` lưu thông báo trong ứng dụng, chưa tích hợp Web Push Notification hoặc gửi tin nhắn SMS/Zalo ZNS cho khách hàng.

---

## 5. Các Rào Cản & Điểm Nghẽn Hiện Tại (Blockers)
1. **Bảo mật file cấu hình (Security Config Blocker):**
   - File `src/main/resources/application.properties` đang lưu trực tiếp mật khẩu database, app password email, API key Google Cloud Vision và PayOS Secret Keys. Đây là rủi ro bảo mật nghiêm trọng cần chuyển sang Environment Variables trước khi đưa ra internet.
2. **Kiểm thử PayOS với dòng tiền thật (Real PayOS Payment Blocker):**
   - Mọi kiểm thử hiện tại chạy trên môi trường giả lập hoặc tắt thanh toán (`--app.payments-enabled=false`). Cần môi trường tài khoản PayOS doanh nghiệp có webhook công khai qua HTTPS (ngrok/Cloudflare Tunnel) để kiểm thử luồng thực tế.
3. **Quyết định nghiệp vụ mốc thời gian (Business Policy Decision Blocker):**
   - Cần chủ dự án xác nhận dứt khoát: Giữ nguyên cách tính tròn 24h hiện tại (chuẩn ngành cho thuê xe du lịch) hay bắt buộc chuyển sang quy tắc nhận trước 12:00 trả 20:00.
