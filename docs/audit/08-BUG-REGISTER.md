# 08 - SỔ ĐĂNG KÝ VÀ THEO DÕI LỖI (BUG REGISTER)

Bảng phân loại và theo dõi toàn bộ các lỗi kỹ thuật, vấn đề nghiệp vụ, quan ngại kiến trúc và rào cản môi trường được phát hiện trong cuộc kiểm toán hệ thống MotoRent.

---

## 1. Danh Sách Lỗi Kỹ Thuật Đã Xác Nhận (Confirmed Bugs)

### MBR-001: Thiếu CSRF Header trong hàm quét CCCD tại trang Hợp đồng thuê xe
- **Trạng thái:** **CONFIRMED BUG**
- **Mức độ nghiêm trọng (Severity):** **HIGH**
- **Vai trò bị ảnh hưởng:** `CUSTOMER`
- **Chức năng bị ảnh hưởng:** Quét tự động ảnh CCCD (OCR) trên trang Hợp đồng thuê xe (`rental-contract.html`).
- **Tập tin liên quan:** `src/main/resources/templates/customer/rental-contract.html` (Dòng 472–475)
- **Các bước tái hiện (Reproduction steps):**
  1. Đăng nhập tài khoản Customer.
  2. Chọn xe và tiến hành tạo hợp đồng thuê tại `/customer/rental-contract/{orderId}`.
  3. Bấm nút "Quét ảnh CCCD" và chọn file ảnh CCCD hợp lệ.
  4. Trình duyệt gửi request `POST /api/ocr/cccd`.
- **Kết quả kỳ vọng (Expected result):** Backend tiếp nhận ảnh, gửi sang Google Cloud Vision OCR và trả về JSON chứa số CCCD, họ tên, ngày sinh, địa chỉ để tự động điền vào form.
- **Kết quả thực tế (Actual result):** Trình duyệt nhận phản hồi HTTP `403 Forbidden` với nội dung JSON: `{"message":"Phiên biểu mẫu đã thay đổi. Vui lòng tải lại trang rồi thử lại."}`. Giao diện hiển thị thông báo lỗi "Lỗi không xác định".
- **Bằng chứng kỹ thuật (Log/Evidence):**
  Trong `rental-contract.html`:
  ```javascript
  const response = await fetch(apiUrl, {
      method: 'POST',
      body: formData // Thiếu header X-XSRF-TOKEN
  });
  ```
  Trong khi [SecurityConfig.java](file:///d:/Motorbike-rental/src/main/java/com/motorrent/motorbikerental/config/SecurityConfig.java) áp dụng bộ lọc CSRF token rotation cho tất cả các endpoint mutating trừ webhook PayOS:
  ```java
  .csrf(csrf -> csrf.sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
      .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
      ...ignoringRequestMatchers("/api/payment/payos-webhook"))
  ```
- **Nguyên nhân gốc rễ (Root cause):** Hàm `handleScanImage()` sử dụng trực tiếp hàm `fetch()` của trình duyệt mà không trích xuất cookie `XSRF-TOKEN` để gán vào header `X-XSRF-TOKEN` của HTTP Request.
- **Giải pháp đề xuất (Suggested fix):**
  Lấy token CSRF từ cookie hoặc từ meta tag và truyền vào header của request fetch:
  ```javascript
  const csrfToken = document.cookie.split('; ')
      .find(row => row.startsWith('XSRF-TOKEN='))?.split('=')[1];
  const response = await fetch(apiUrl, {
      method: 'POST',
      headers: {
          'X-XSRF-TOKEN': csrfToken || ''
      },
      body: formData
  });
  ```
- **Phụ thuộc:** Không.

---

### MBR-002: Bảng lịch sử và quản lý đơn bị tràn ngang trên màn hình di động hẹp
- **Trạng thái:** **CONFIRMED BUG**
- **Mức độ nghiêm trọng (Severity):** **LOW**
- **Vai trò bị ảnh hưởng:** `CUSTOMER`, `STAFF`
- **Chức năng bị ảnh hưởng:** Màn hình Lịch sử thuê xe (`rental-history.html`) và Quản lý đơn của nhân viên (`rental-orders.html`).
- **Tập tin liên quan:** `src/main/resources/templates/customer/rental-history.html`, `src/main/resources/templates/staff/rental-orders.html`
- **Các bước tái hiện:** Mở trang lịch sử thuê xe trên viewport điện thoại (chiều rộng 375px - 393px).
- **Kết quả thực tế:** Bảng dữ liệu có nhiều cột (Mã đơn, Tên xe, Ngày nhận, Ngày trả, Tổng tiền, Trạng thái) bị tràn ra ngoài màn hình, gây thanh cuộn ngang toàn trang (Horizontal page scroll) làm giảm trải nghiệm di động.
- **Giải pháp đề xuất:** 
  1. Thêm thẻ bọc `<div class="overflow-x-auto w-full">` quanh thẻ `<table>`.
  2. Hoặc chuyển đổi hiển thị trên mobile sang dạng Thẻ thông tin (Card view) thay vì Bảng (Table view).

---

## 2. Các Vấn Đề Chờ Quyết Định Nghiệp Vụ (Business Decision Pending)

### MBR-003: Thuật toán tính số ngày thuê xe (Làm tròn 24h vs Mốc 12h/20h)
- **Trạng thái:** **BUSINESS DECISION PENDING**
- **Mức độ nghiêm trọng (Severity):** **MEDIUM**
- **Vai trò bị ảnh hưởng:** `CUSTOMER`, `ADMIN` (Doanh thu)
- **Tập tin liên quan:** [PricingService.java](file:///d:/Motorbike-rental/src/main/java/com/motorrent/motorbikerental/service/PricingService.java) (Dòng 9–14)
- **Mô tả vấn đề:**
  - *Tài liệu yêu cầu ban đầu (Baseline):* Nhận xe trước 12:00 -> tính ngày đầu kết thúc lúc 20:00 cùng ngày; Nhận sau 12:00 -> kết thúc lúc 12:00 hôm sau.
  - *Mã nguồn thực tế hiện tại:* Đang tính theo chu kỳ 24h: `(seconds + 86399) / 86400` (làm tròn lên ngày tiếp theo nếu vượt quá chu kỳ 24h).
- **Đánh giá nghiệp vụ:** 
  - Cách tính hiện tại trong source code rất phổ biến và minh bạch với khách du lịch (thuê 24 tiếng tính 1 ngày).
  - Quy tắc mốc 12h/20h phức tạp hơn, nếu áp dụng cần làm rõ: Nếu khách nhận lúc 11:59 và trả lúc 20:05 thì tính 1 ngày hay 2 ngày?
- **Khuyến nghị:** Chủ dự án xem xét và phê duyệt giữ nguyên cách tính 24h hiện tại hoặc chỉ định thuật toán chi tiết nếu muốn chuyển đổi.

---

### MBR-004: Xử lý đơn bị tự động hủy nhưng khách thanh toán PayOS hợp lệ và xe đã bị người khác đặt
- **Trạng thái:** **BUSINESS DECISION PENDING**
- **Mức độ nghiêm trọng (Severity):** **MEDIUM**
- **Vai trò bị ảnh hưởng:** `CUSTOMER`, `STAFF`
- **Tập tin liên quan:** [PaymentService.java](file:///d:/Motorbike-rental/src/main/java/com/motorrent/motorbikerental/service/PaymentService.java) (Dòng 152–156)
- **Mô tả vấn đề:**
  Khi đơn bị hủy tự động do hết hạn 3 phút, khách vẫn thanh toán thành công qua mã QR, hệ thống kiểm tra nếu xe đã bị đơn khác đặt thì chuyển khoản thanh toán thành `REVIEW_REQUIRED` để bảo vệ chống double booking.
- **Thực tế:** Hiện tại chưa có nút bấm nghiệp vụ chuyên biệt trên giao diện Staff để:
  1. Hoàn tiền cọc tự động qua ngân hàng cho khách.
  2. Hoặc cho phép Staff chuyển khoản tiền cọc này sang một đơn xe khác mà khách chọn lại.
- **Khuyến nghị:** Cần xây dựng luồng hỗ trợ Staff giải quyết các giao dịch `REVIEW_REQUIRED` mượt mà hơn.

---

## 3. Rào Cản Môi Trường & Bảo Mật (Environment & Security Blockers)

### MBR-005: Hardcoded Credentials trong file application.properties
- **Trạng thái:** **ENVIRONMENT BLOCKER / CRITICAL SECURITY CONCERN**
- **Mức độ nghiêm trọng (Severity):** **HIGH**
- **Tập tin liên quan:** `src/main/resources/application.properties`
- **Mô tả:** Mật khẩu MySQL, mật khẩu ứng dụng Gmail, API key Google Cloud Vision, và PayOS API Key/Checksum Key đang được lưu bằng plain-text trong file cấu hình.
- **Khuyến nghị:** Chuyển toàn bộ các giá trị này sang biến môi trường (`${DB_PASSWORD}`, `${MAIL_PASSWORD}`, `${PAYOS_API_KEY}`...).

---

### MBR-006: Thư mục dự án chưa khởi tạo Git Repository
- **Trạng thái:** **ENVIRONMENT BLOCKER**
- **Mức độ nghiêm trọng (Severity):** **MEDIUM**
- **Mô tả:** Dự án tại `d:/Motorbike-rental` hiện tại không có thư mục `.git`. Việc thiếu Git Version Control gây rủi ro mất lịch sử thay đổi, khó đối soát diff khi nhiều lập trình viên hoặc AI cùng can thiệp.
- **Khuyến nghị:** Khởi tạo Git repository (`git init`), tạo file `.gitignore` loại bỏ `target/`, file `.log`, và các file chứa thông tin nhạy cảm trước khi commit lần đầu.
