# ARCHITECTURE ANALYSIS — Motorbike Rental Project

> **Ngày:** 2026-09-15 | **Trạng thái:** Phân tích hoàn tất — Chờ phê duyệt

---

## 1. Kiến Trúc Thực Tế Của TourGo

TourGo là dự án Spring Boot monolith MVC tích hợp REST API lẫn Thymeleaf SSR.

**Stack:** Spring Boot 3.5.3 / Java 21 / MySQL

```
com.example.tourify_system_be/
├── TourifySystemBeApplication.java
├── config/         (AiConfig, CloudinaryConfig, PayOSConfig, WebMvcConfig, WebSocketConfig)
├── controller/     (Tất cả controller — KHÔNG phân web/api)
├── dto/
│   ├── request/   (APIResponse<T> + tất cả request DTO)  ← APIResponse đặt sai chỗ
│   └── response/  (tất cả response DTO)
├── entity/         (41 entity)
├── exception/      (AppException, ErrorCode enum, GlobalExceptionHandler)
├── mapper/         (MapStruct interfaces)
├── repository/     (JPA repository, prefix I: IUserRepository)
├── security/       (JwtUtil, JwtAuthenticationFilter, SecurityConfig, CustomUserDetails)
├── service/        (Service classes — KHÔNG tách interface/impl)
├── specification/  (JPA Specification — TourSpecification)
└── utils/
```

### Patterns chính của TourGo

| Pattern | Chi tiết |
|---------|----------|
| API Response | `APIResponse<T>` generic, code + message + result |
| Exception | `AppException(ErrorCode)` + `@ControllerAdvice GlobalExceptionHandler` |
| JWT | Stateless, token nhúng userId + role, `JwtAuthenticationFilter extends OncePerRequestFilter` |
| Mapper | MapStruct `@Mapper(componentModel = "spring")` — interface only |
| Pagination | `PageResponse<T>` với static `fromPage(Page<T>)` |
| Repository | `JpaRepository` — naming `IXxxRepository` |
| Security | `CustomUserDetails implements UserDetails`, role = plain String |
| CORS | `CorsConfigurationSource` bean trong `SecurityConfig` |

---

## 2. Công Nghệ Và Dependency TourGo

| Dependency | Version | Ghi chú |
|-----------|---------|---------|
| Spring Boot Parent | 3.5.3 | |
| Java | 21 | Target dự án motorbike: 17 |
| spring-boot-starter-data-jpa | BOM | |
| spring-boot-starter-web | BOM | |
| spring-boot-starter-security | BOM | |
| spring-boot-starter-oauth2-client | BOM | Google OAuth2 |
| spring-boot-starter-thymeleaf | BOM | |
| spring-boot-starter-freemarker | BOM | Email templates |
| spring-boot-starter-mail | BOM | |
| spring-boot-starter-validation | BOM | Jakarta Validation |
| spring-boot-starter-websocket | BOM | Chat real-time |
| mysql-connector-j | 8.4.0 | |
| lombok | 1.18.30 | |
| mapstruct + processor | 1.5.5.Final | |
| jjwt-api/impl/jackson | 0.11.5 | JWT |
| payos-java | 1.0.1 | Thanh toán |
| cloudinary | 1.37.0 (+ 1.32.2 duplicate!) | Image upload |
| spring-ai | 1.0.0 | AI — không dùng |
| poi-ooxml | 5.2.5 | Excel |
| google-api-services-docs | - | Không dùng |

**⚠️ Vấn đề TourGo cần lưu ý:**
- JWT secret hardcode: `"your-very-strong-secret-key-1234567890"` — KHÔNG copy
- Database/email/OAuth2/PayOS credentials viết thẳng trong yaml — KHÔNG copy
- Cloudinary khai báo 2 lần (version conflict)
- `APIResponse` đặt trong `dto/request/` — thiết kế sai, sẽ sửa trong motorbike-rental

---

## 3. Thành Phần Có Thể Áp Dụng

| Thành phần | Mức tái sử dụng | Ghi chú |
|-----------|----------------|---------|
| `AppException` + `ErrorCode` enum | ✅ Hoàn toàn | Đổi tên domain + thêm error codes thuê xe |
| `GlobalExceptionHandler` | ✅ Hoàn toàn | Giữ nguyên logic |
| `APIResponse<T>` wrapper | ✅ Hoàn toàn | Chuyển sang `dto/common/` |
| `PageResponse<T>` | ✅ Hoàn toàn | |
| `JwtAuthenticationFilter` | ✅ Cần cải tiến | Bỏ hardcode secret, dùng `@Value` |
| `JwtUtil` / `JwtService` | ✅ Cần cải tiến | Secret từ env var |
| `SecurityConfig` (cấu trúc) | ✅ Cần điều chỉnh | Cấu hình lại endpoint permissions |
| `CustomUserDetails` | ✅ Hoàn toàn | |
| MapStruct mapper pattern | ✅ Hoàn toàn | |
| Repository pattern `IXxxRepository` | ✅ Hoàn toàn | |
| JPA Specification pattern | ✅ Áp dụng | Filter xe, filter đơn thuê |
| `WebMvcConfig` (resource handler) | ✅ Áp dụng | Static assets |

---

## 4. Thành Phần Không Nên Sao Chép

| Thành phần | Lý do |
|-----------|-------|
| `Tour`, `BookingTour`, `TourActivity`... | Nghiệp vụ tour — không liên quan |
| `PayOSService` (payment logic cụ thể) | Chỉ tham khảo cách tổ chức |
| `AIAnalyticsService`, `SuggestTourService` | AI tour — không liên quan |
| `ChatService`, `WebSocketConfig` | Chat real-time — không cần giai đoạn đầu |
| `GoogleDocsService` | Không liên quan |
| `FollowSubCompanyService` | Khái niệm Sub-Company chỉ có ở TourGo |
| Hardcoded secrets bất kỳ | **Tuyệt đối không copy** |

---

## 5. Cấu Trúc Hiện Tại Của `motorbike-rental`

```
motorbike-rental/
├── .gitattributes / .gitignore
├── .mvn/wrapper/
├── HELP.md
├── mvnw / mvnw.cmd
├── pom.xml                   ← Spring Boot 4.1.1, Java 17
└── src/
    └── main/
        ├── java/com/motorrent/motorbikerental/
        │   └── MotorbikeRentalApplication.java   ← DUY NHẤT
        └── resources/
            ├── application.properties            ← Chỉ có app name
            ├── static/                           ← TRỐNG
            └── templates/                        ← TRỐNG
```

**Nhận xét:**
- Dự án hoàn toàn mới — không có code nghiệp vụ
- Spring Boot **4.1.1** (phiên bản rất mới)
- Package root: `com.motorrent.motorbikerental`
- Java version: 17

### Vấn đề nghiêm trọng trong pom.xml

Các dependency test sau **KHÔNG TỒN TẠI** trên Maven Central:

```
spring-boot-starter-data-jpa-test        ← KHÔNG TỒN TẠI
spring-boot-starter-mail-test            ← KHÔNG TỒN TẠI
spring-boot-starter-security-oauth2-client-test ← KHÔNG TỒN TẠI
spring-boot-starter-security-test        ← KHÔNG TỒN TẠI
spring-boot-starter-thymeleaf-test       ← KHÔNG TỒN TẠI
spring-boot-starter-validation-test      ← KHÔNG TỒN TẠI
spring-boot-starter-webmvc-test          ← KHÔNG TỒN TẠI
spring-boot-starter-websocket-test       ← KHÔNG TỒN TẠI
```

→ **Phải sửa pom.xml trước khi compile** (xóa 8 dependency sai, thêm `spring-boot-starter-test`)

**Dependency THIẾU cần bổ sung:**
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` → JWT (version 0.12.6 — mới nhất)
- `mapstruct` + `mapstruct-processor` → DTO mapping

---

## 6. Danh Sách Front-end Customer Hiện Có

### Design System: "Coastal Midnight Velocity"

| Yếu tố | Giá trị |
|--------|---------|
| Font | Plus Jakarta Sans (Google Fonts) |
| Icons | Material Symbols Outlined (Google) |
| CSS Framework | TailwindCSS v3 (CDN inline mỗi trang) |
| Background | `#051424` (midnight navy) |
| Primary/CTA | `#f97316` → `#ea580c` (sunset orange gradient) |
| Cards | `#141d33` (dark slate) |
| Text | `#d4e4fa` |
| Success | `#10b981` (emerald) |
| Error | `#f43f5e` (rose) |
| Style | Glassmorphism + dark mode + micro-animations |

### Các trang đã thiết kế

| Thư mục | Trang | Có HTML |
|---------|-------|---------|
| `motorent_n_ng_xe_cho_thu` | Danh sách xe | ✅ ~67KB |
| `motorent_n_ng_chi_ti_t_xe_honda_air_blade_160_abs` | Chi tiết xe + Form thuê | ✅ |
| `motorent_n_ng_h_p_ng_thu_xe_i_n_t` | Hợp đồng thuê xe điện tử | ✅ ~41KB |
| `motorent_n_ng_h_s_c_nh_n` | Hồ sơ cá nhân | ✅ |
| `motorent_n_ng_kh_i_ph_c_t_i_kho_n` | Quên/Khôi phục mật khẩu | ✅ |
| `motorent_n_ng_l_ch_s_thu_xe` | Lịch sử thuê xe | ✅ |
| `motorent_n_ng_n_thu_c_a_t_i_ng_b_ti_n_tr_nh` | Đơn thuê của tôi | ✅ |
| `motorent_n_ng_ng_k_t_i_kho_n` | Đăng ký | ✅ ~24KB |
| `motorent_n_ng_ng_nh_p_t_i_kho_n` | Đăng nhập | ✅ ~28KB |
| `coastal_midnight_velocity` | Design System Docs | ✅ DESIGN.md |

---

## 7. Các Trang Customer Còn Thiếu

| Trang | Trạng thái | Hành động |
|-------|-----------|-----------|
| **Trang chủ (landing page)** | ❌ THIẾU | Tạo mới theo design system |
| Đăng nhập | ✅ Có | Chuyển → Thymeleaf |
| Đăng ký | ✅ Có | Chuyển → Thymeleaf |
| Quên mật khẩu | ✅ Có | Chuyển → Thymeleaf |
| **Đặt lại mật khẩu** | ❌ THIẾU | Tạo mới |
| Danh sách xe | ✅ Có | Chuyển → Thymeleaf |
| Chi tiết xe + Form thuê | ✅ Có | Chuyển → Thymeleaf |
| Hợp đồng thuê xe | ✅ Có | Chuyển → Thymeleaf |
| Đơn thuê của tôi | ✅ Có | Chuyển → Thymeleaf |
| **Chi tiết đơn thuê** | ❌ THIẾU | Tạo mới hoặc tách từ danh sách |
| **Gia hạn thuê xe** | ❌ THIẾU | Tạo mới |
| Lịch sử thuê xe | ✅ Có | Chuyển → Thymeleaf |
| Hồ sơ cá nhân | ✅ Có | Chuyển → Thymeleaf |
| **Chỉnh sửa hồ sơ** | ❌ THIẾU | Tích hợp vào hồ sơ hoặc tạo mới |
| **Đổi mật khẩu** | ❌ THIẾU | Tạo mới |
| **Trang thanh toán** | ❌ THIẾU | Tạo mới |
| **Kết quả thanh toán** | ❌ THIẾU | Tạo mới |
| **Đánh giá xe** | ❌ THIẾU | Tạo mới |
| **Lỗi 403 / 404 / 500** | ❌ THIẾU | Tạo mới |

**Tổng:** 9/19 trang đã có frontend, **10 trang cần tạo mới** theo design system.

---

## 8. Files Có Nguy Cơ Trùng Hoặc Ghi Đè

| Rủi ro | Mức độ | Xử lý |
|--------|--------|-------|
| `application.properties` | Thấp | Thay bằng `application.yml` |
| `static/` (đang trống) | Không có | An toàn tuyệt đối |
| `templates/` (đang trống) | Không có | An toàn tuyệt đối |
| Tailwind CDN trong HTML | Không phải file | Giữ nguyên CDN |

**Dự án còn trống — không có rủi ro ghi đè nghiêm trọng.**

---

## 9. Kế Hoạch Tích Hợp Front-end Vào Spring Boot

### Chiến lược chuyển đổi

```
HTML gốc (Tailwind CDN) → Thymeleaf Template (giữ TailwindCSS CDN)
Mock data trong HTML     → Thymeleaf model attributes từ Controller
JavaScript fetch         → REST API /api/v1/**
<link href="...">        → <link th:href="@{/css/...}">
<script src="...">       → <script th:src="@{/js/...}">
```

### Cấu trúc templates mục tiêu

```
src/main/resources/
├── templates/
│   ├── fragments/
│   │   ├── _head.html           (meta, font, CSS links chung)
│   │   ├── _navbar.html         (header + navigation)
│   │   ├── _footer.html
│   │   ├── _scripts.html        (JS ở cuối body)
│   │   └── _pagination.html
│   ├── customer/
│   │   ├── home.html
│   │   ├── motorbikes.html      (Danh sách xe)
│   │   ├── motorbike-detail.html
│   │   ├── my-rentals.html
│   │   ├── rental-detail.html
│   │   ├── rental-contract.html
│   │   ├── rental-extend.html
│   │   ├── rental-history.html
│   │   ├── payment.html
│   │   ├── payment-result.html
│   │   └── review.html
│   ├── auth/
│   │   ├── login.html
│   │   ├── register.html
│   │   ├── forgot-password.html
│   │   └── reset-password.html
│   ├── profile/
│   │   ├── view.html
│   │   ├── edit.html
│   │   └── change-password.html
│   ├── staff/          ← placeholder only
│   ├── admin/          ← placeholder only
│   └── error/
│       ├── 403.html
│       ├── 404.html
│       └── 500.html
└── static/
    ├── css/
    │   └── custom.css   (override styles)
    ├── js/
    │   ├── api.js       (fetch wrapper + token)
    │   ├── auth.js
    │   ├── motorbikes.js
    │   ├── rental.js
    │   └── profile.js
    └── images/
```

---

## 10. Dependencies: Thiếu, Thừa, Xung Đột

### Cần BỔ SUNG

```xml
<!-- JWT (dùng version 0.12.6 thay vì 0.11.5 của TourGo) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
</dependency>

<!-- Test (chuẩn) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Cần XÓA (8 dependency không tồn tại)

```
spring-boot-starter-data-jpa-test
spring-boot-starter-mail-test
spring-boot-starter-security-oauth2-client-test
spring-boot-starter-security-test
spring-boot-starter-thymeleaf-test
spring-boot-starter-validation-test
spring-boot-starter-webmvc-test
spring-boot-starter-websocket-test
```

### Giữ Nguyên

```
spring-boot-starter-data-jpa, mail, security, oauth2-client,
thymeleaf, validation, webmvc, websocket,
thymeleaf-extras-springsecurity6, mysql-connector-j, lombok,
spring-ai, spring-boot-starter-webmvc
```

---

## 11. Giả Định Nghiệp Vụ Cần Xác Nhận

> **Lưu ý:** Những điểm dưới đây ảnh hưởng đến thiết kế database và API. Cần xác nhận trước khi tạo Entity.

### ❓ Q1: Một đơn thuê = bao nhiêu xe?

- **Phương án A:** 1 đơn = 1 xe → `rental_orders` có `motorbike_id` trực tiếp
- **Phương án B:** 1 đơn = nhiều xe → cần bảng `rental_order_items`

*Giả định tạm thời: **Phương án A** — phổ biến hơn cho thuê xe máy nhỏ lẻ.*

### ❓ Q2: Quan hệ đơn thuê ↔ hợp đồng?

- **A:** 1-1: xác nhận đơn → tự động tạo hợp đồng
- **B:** Hợp đồng là document riêng hoàn toàn

*Giả định tạm thời: **Phương án A** — hợp đồng là view của đơn đã confirmed.*

### ❓ Q3: Đơn vị tính giá thuê?

- **A:** Theo ngày (1 ngày = 24h tròn)
- **B:** Theo giờ
- **C:** Ngày + giờ lẻ

*Giả định tạm thời: **Phương án A** — `pricePerDay` × số ngày.*

### ❓ Q4: Tỷ lệ tiền cọc?

- **A:** Số tiền cố định mỗi xe
- **B:** % giá trị xe
- **C:** % tổng giá thuê

*Giả định tạm thời: **Phương án A** — `depositAmount` là trường trong bảng `motorbikes`.*

### ❓ Q5: Phí giao/nhận xe?

- **A:** Theo khoảng cách (km)
- **B:** Nhập thủ công khi tạo đơn
- **C:** Theo vùng/trạm

*Giả định tạm thời: **Phương án B** — nhập thủ công, lưu trong `rental_orders`.*

### ❓ Q6: Cần soft delete không?

*Giả định tạm thời: **Có** — trường `deletedAt` (nullable) cho `User` và `Motorbike`.*

### ❓ Q7: Lưu lịch sử thay đổi trạng thái đơn không?

*Giả định tạm thời: **Chưa cần** — chỉ lưu trạng thái hiện tại. Thêm `StatusHistory` sau nếu cần.*

### ❓ Q8: Cổng thanh toán sẽ dùng?

*Giả định tạm thời: Không tích hợp cổng cụ thể giai đoạn đầu. `Payment` entity lưu: `amount`, `transactionCode`, `status`, `paymentMethod` (CASH/ONLINE), `gateway` (enum: NONE/PAYOS/VNPAY).*

---

## 12. Cấu Trúc Package Mục Tiêu

```
com.motorrent.motorbikerental/
├── MotorbikeRentalApplication.java
├── config/
│   ├── AppConfig.java
│   └── WebMvcConfig.java
├── security/
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   ├── SecurityConfig.java
│   ├── CustomUserDetails.java
│   └── CustomUserDetailsService.java
├── controller/
│   ├── web/        ← Thymeleaf controllers
│   └── api/        ← REST controllers
├── dto/
│   ├── common/     ← ApiResponse, PageResponse
│   ├── request/
│   └── response/
├── entity/
├── enums/
├── exception/
├── mapper/
├── repository/
├── service/
├── specification/
├── util/
└── validation/
```

---

## 13. Mapping Frontend → Thymeleaf Template

| Thư mục gốc | Template đích |
|-------------|--------------|
| `motorent_n_ng_ng_nh_p_t_i_kho_n/code.html` | `templates/auth/login.html` |
| `motorent_n_ng_ng_k_t_i_kho_n/code.html` | `templates/auth/register.html` |
| `motorent_n_ng_kh_i_ph_c_t_i_kho_n/code.html` | `templates/auth/forgot-password.html` |
| `motorent_n_ng_xe_cho_thu/code.html` | `templates/customer/motorbikes.html` |
| `motorent_n_ng_chi_ti_t_xe_.../code.html` | `templates/customer/motorbike-detail.html` |
| `motorent_n_ng_h_p_ng_thu_xe_.../code.html` | `templates/customer/rental-contract.html` |
| `motorent_n_ng_n_thu_c_a_t_i_.../code.html` | `templates/customer/my-rentals.html` |
| `motorent_n_ng_l_ch_s_thu_xe/code.html` | `templates/customer/rental-history.html` |
| `motorent_n_ng_h_s_c_nh_n/code.html` | `templates/profile/view.html` |

---

*Tài liệu hoàn tất. Cần xác nhận các giả định tại Mục 11 trước khi thiết kế entity và database.*
