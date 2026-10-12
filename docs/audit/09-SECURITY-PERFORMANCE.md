# 09 - BÁO CÁO BẢO MẬT VÀ HIỆU NĂNG (SECURITY & PERFORMANCE AUDIT)

Báo cáo phân tích chuyên sâu về an ninh thông tin, phòng chống các lỗ hổng OWASP Top 10, quản lý thông tin định danh người dùng (PII), hiệu năng truy vấn cơ sở dữ liệu và tải tài nguyên giao diện.

---

## 1. Kiểm Toán An Ninh Thông Tin (Security Audit)

### 1.1. Xác thực & Quản lý Phiên (Authentication & Session Management)
- **Cơ chế:** Sử dụng JWT (JSON Web Token) ký bằng thuật toán HMAC-SHA256 (`jjwt 0.11.5`).
- **Lưu trữ Token:** Token được lưu trong Cookie trình duyệt với cờ `HttpOnly`, ngăn chặn hiệu quả việc mã độc JavaScript đánh cắp token qua XSS.
- **Vòng đời & Thu hồi (Revocation):** Bảng `token_authentication` lưu trạng thái `is_used` của từng token. Khi người dùng bấm Đăng xuất hoặc Đổi mật khẩu, toàn bộ token của tài khoản đó đều bị đánh dấu vô hiệu (`is_used = false`), đảm bảo token cũ không thể tái sử dụng.
- **Mã hóa mật khẩu:** Sử dụng `BCryptPasswordEncoder` với độ dài muối an toàn.

### 1.2. Phân Quyền & Phòng Chống IDOR (Authorization & Insecure Direct Object Reference)
- **Kiểm soát truy cập:** Mọi controller API đều kiểm tra quyền sở hữu bản ghi qua lớp [CurrentUser.java](file:///d:/Motorbike-rental/src/main/java/com/motorrent/motorbikerental/security/CurrentUser.java).
- **Chống IDOR:** Khách hàng truyền `orderId` trên URL (ví dụ: `/api/rental-orders/38`) sẽ được kiểm tra:
  ```java
  public void canRead(RentalOrder order) {
      User user = get();
      if (user.getRole() == UserRole.CUSTOMER && !user.getId().equals(order.getCustomer().getId()))
          throw new AccessDeniedException("Bạn không có quyền truy cập đơn này");
  }
  ```
  Nếu không phải chủ đơn, backend lập tức chặn với mã `403 Forbidden`. Đã có 34 bài test kiểm chứng trong `CustomerSecurityIntegrationTests`.
- **Định danh Nhân viên an toàn:** Khi Staff thực hiện duyệt đơn, giao xe, đổi xe hay hoàn tất đơn, mã nhân viên (`staffId`) luôn được lấy từ token đã xác thực, không bao giờ nhận `staffId` từ request payload của client.

### 1.3. Phòng Chống CSRF (Cross-Site Request Forgery)
- Hệ thống áp dụng `CookieCsrfTokenRepository.withHttpOnlyFalse()` kết hợp cơ chế quay vòng token CSRF (`XSRF-TOKEN`).
- Mọi request thay đổi dữ liệu (`POST`, `PUT`, `DELETE`) đều bắt buộc phải gửi kèm header `X-XSRF-TOKEN` khớp với cookie CSRF hiện tại.
- Ngoại lệ duy nhất là `/api/payment/payos-webhook`, vì webhook được gửi từ server-to-server của PayOS và được bảo vệ bằng chữ ký số riêng.

### 1.4. Bảo Vệ Dữ Liệu Cá Nhân Nhạy Cảm (PII & CCCD Protection)
- Trong các báo cáo tài chính và danh sách công khai, thông tin số CCCD, hình ảnh mặt trước/sau của khách hàng không bị phơi bày tùy tiện.
- Ảnh CCCD sau khi tải lên quét OCR được xử lý trực tiếp trên bộ nhớ (RAM/byte array), không bị lưu vĩnh viễn vào các thư mục web tĩnh công khai.
- Ảnh đại diện người dùng (`avatar`) được giới hạn định dạng JPG/PNG, kích thước tối đa 5MB, được re-encode bằng Java `Graphics2D` thành ảnh 512x512 mới trước khi lưu, loại trừ hoàn toàn nguy cơ chèn mã độc (Polyglot / Shell upload).

### 1.5. Lỗ Hổng Hardcoded Credentials (CRITICAL FINDING)
- **Thực trạng:** File `application.properties` đang chứa trực tiếp mật khẩu cơ sở dữ liệu MySQL, mật khẩu ứng dụng Gmail SMTP, Google Cloud Vision API key, và bộ ba khóa PayOS.
- **Rủi ro:** Nếu mã nguồn này vô tình được đưa lên các dịch vụ Git công khai (GitHub, GitLab), toàn bộ cơ sở dữ liệu và tài khoản thanh toán sẽ bị xâm nhập.
- **Khắc phục:** Thay thế bằng cấu hình đọc từ biến môi trường hệ thống.

---

## 2. Kiểm Toán Hiệu Năng Hệ Thống (Performance Audit)

### 2.1. Cảnh Báo Open-in-View (OSIV Warning)
- Trong log khởi động hệ thống ghi nhận:
  ```
  WARN JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering.
  ```
- **Tác động:** Khi bật OSIV, kết nối cơ sở dữ liệu sẽ bị giữ mở trong suốt quá trình render Thymeleaf HTML. Nếu trang web phức tạp hoặc mạng người dùng chậm, kết nối HikariCP pool sẽ nhanh chóng bị cạn kiệt (Connection Pool Exhaustion).
- **Khuyến nghị:** Thêm dòng sau vào `application.properties`:
  ```properties
  spring.jpa.open-in-view=false
  ```
  Đồng thời đảm bảo các truy vấn lazy loading cần thiết được nạp bằng `JOIN FETCH` hoặc DTO projection trong tầng Service.

### 2.2. Kiểm Tra Vấn Đề N+1 Queries
- Trong `OperationsService.java`, một số hàm liệt kê dữ liệu (như `list("rental-orders")`) đang thực hiện `orders.findAll()` rồi map từng đơn sang DTO. Nếu entity `RentalOrder` nạp `Customer` và `Motorbike` ở chế độ EAGER hoặc gọi getter trong vòng lặp, số lượng query sinh ra sẽ tăng tuyến tính với số lượng đơn hàng (N+1 queries).
- **Khuyến nghị:** Tối ưu các câu lệnh Repository bằng `EntityGraph` hoặc `@Query("SELECT o FROM RentalOrder o JOIN FETCH o.customer JOIN FETCH o.motorbike")`.

### 2.3. Bảng Dữ Liệu Dung Lượng Lớn `travel_places`
- Bảng `travel_places` hiện chứa 31,992 dòng dữ liệu địa điểm du lịch, chiếm tới hơn 27MB trong cơ sở dữ liệu.
- Việc tải toàn bộ hoặc tìm kiếm không có phân trang (pagination) trên bảng này có thể làm chậm đáng kể thời gian phản hồi API.
- **Khuyến nghị:** Luôn áp dụng phân trang `Pageable` với `PageRequest.of(page, size)` khi truy vấn danh sách địa điểm du lịch.

### 2.4. Tải Tài Nguyên Giao Diện (Frontend Asset Loading)
- Hiện tại dự án sử dụng Tailwind CSS qua CDN script:
  `<script src="https://cdn.tailwindcss.com"></script>`
- **Đánh giá:**
  - Tailwind CDN rất tiện lợi cho quá trình phát triển (development/preview).
  - Tuy nhiên, trong môi trường sản xuất (production), việc tải CDN này làm tăng thời gian render trang (FCP / LCP) vì trình duyệt phải tải bộ script JIT của Tailwind và biên dịch CSS runtime trên máy client.
  - **Khuyến nghị:** Cho tương lai, nên biên dịch sẵn một file CSS tĩnh đã được purge (PostCSS) để tối ưu hóa tốc độ tải trang.
