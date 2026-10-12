# MOTORBIKE RENTAL — GITHUB HANDOFF REPORT (PHASE 0)
**Ngày thực hiện:** 12/10/2026  
**Người thực hiện:** Technical Project Maintainer / Senior Software & Security Engineer  
**Đối tượng bàn giao:** Đội ngũ phát triển / Đánh giá độc lập (Independent AI Auditor / ChatGPT)  
**Tình trạng:** Sẵn sàng bàn giao qua GitHub baseline  

---

## 1. GITHUB REPOSITORY INFORMATION

- **Repository Remote:** `https://github.com/Quang13082005/MotorbikeRent.git`
- **Target Handoff Branch:** `handoff/motorbike-rental-audit-20261012`
- **Baseline Commit SHA:** `3c06df914c7629c20743f544c4403f3ee844b20e`
- **Push Status:** Đang thực hiện push lên remote origin
- **Repository Visibility:** Private / Authenticated
- **Remote Verification:** Đã kết nối với remote `origin`. Branch `phase-1` cũ vẫn được giữ nguyên vẹn trên remote; branch `handoff/motorbike-rental-audit-20261012` được tạo mới độc lập để chứa toàn bộ baseline hoàn chỉnh sau Phase 9 và Audit.
- **Quy tắc Git tuân thủ:**
  - Không force push (`--force`).
  - Không ghi đè hoặc can thiệp branch `main` hoặc `phase-1`.
  - Không xóa branch cũ của dự án.
  - Tách biệt hoàn toàn credentials cục bộ khỏi Git tracking.

---

## 2. CURRENT SOURCE STRUCTURE & TECH STACK

- **Ngôn ngữ:** Java 21 (LTS) - Temurin / Oracle OpenJDK 21
- **Framework:** Spring Boot 4.1.1 (Spring Framework 6.x)
- **Cơ sở dữ liệu:** MySQL 9.0.1 (hỗ trợ kiểm thử trên in-memory H2 Database 2.4.240)
- **Kiến trúc:** Layered MVC + Service Oriented + Transaction Script kết hợp Domain Validation
  - DDL Auto: `validate` trong kiểm thử MySQL / production; `update` trong development cục bộ
  - Transaction isolation: `READ_COMMITTED` / Pessimistic Locking với thứ tự khóa cố định (Vehicle -> Order -> Extension/Change -> Payment)
- **Giao diện người dùng:** 
  - Server-side rendering: Thymeleaf + HTML5 semantic
  - Styling: Tailwind CSS v3 (JIT qua CDN)
  - JavaScript: Vanilla ES6+ modules (`customer.js`, `operations-live.js`, `request-security.js`, `vehicle-changes.js`)
- **Tích hợp bên thứ ba:**
  - **PayOS:** Cổng thanh toán trực tuyến qua QR code với xác thực webhook signature HMAC-SHA256 (`PayOSService.java`).
  - **Google Cloud Vision API:** Trích xuất thông tin Căn cước công dân (CCCD) tự động qua OCR (`OcrService.java`).
  - **Spring Mail:** Gửi email xác nhận đặt cọc và hóa đơn qua Gmail SMTP.
- **Các Package Java cốt lõi:**
  - `com.motorrent.motorbikerental.config`: Cấu hình Security (`SecurityConfig.java`), CORS, Thymeleaf, Async.
  - `com.motorrent.motorbikerental.controller`: Controllers cho Web MVC và REST APIs (Auth, Customer, Staff/Operations, Admin, Payment, OCR, Vehicle).
  - `com.motorrent.motorbikerental.model`: 18 JPA Entities (`RentalOrder`, `Motorbike`, `User`, `Payment`, `RentalContract`, `RentalExtension`, `RentalVehicleChange`, `Settlement`, v.v.).
  - `com.motorrent.motorbikerental.repository`: 18 Spring Data JPA Repositories.
  - `com.motorrent.motorbikerental.service`: Business logic nghiệp vụ chính (`AvailabilityService`, `RentalOrderService`, `PaymentService`, `ExtensionService`, `DuringRentalChangeService`, `VehicleChangeService`, `SettlementService`, `OcrService`).
  - `com.motorrent.motorbikerental.dto`: Các DTO request/response chuẩn hóa nghiệp vụ.
- **Lệnh thực thi dự án:**
  - Build & test H2: `mvn test` (122 automated tests)
  - Test MySQL Concurrency: `mvn test -Dtest=Phase1ConcurrencyTests -Dphase1.mysql=true` (20 concurrency tests)
  - Khởi chạy ứng dụng: `mvn spring-boot:run` (hoặc PowerShell: `.\Start-MotoRent.ps1`)

---

## 3. CLEANUP REPORT (SAFE REPOSITORY CLEANUP)

Trước khi thực hiện dọn dẹp, toàn bộ thư mục dự án đã được sao lưu đầy đủ ra ngoài repository tại `D:\backup-motorent-20261012` (gồm 3.731 files, 510MB dữ liệu).

### A. Files Kept (Giữ lại trong source)
1. **Source code chính:** Toàn bộ thư mục `src/main/java`, `src/main/resources`, `src/test/java`, `src/test/resources`.
2. **Cấu hình chuẩn hóa:** `pom.xml`, `.phase*-verification-pom.xml`, `.env.example`, `.gitignore`, `Start-MotoRent.ps1`.
3. **Tài liệu nghiệp vụ & kỹ thuật quan trọng:**
   - `AGENTS.md`: Quy tắc kỹ thuật và nguyên tắc vận hành AI.
   - `PROJECT_CONTEXT.md`: Bối cảnh hệ thống MotoRent.
   - `docs/audit/`: 12 báo cáo audit toàn diện (Phase A đến Phase J).
   - `docs/business-rules/`: Quy tắc giao xe (`DELIVERY-RULE.docx`), tài liệu nghiệp vụ chi tiết (`Nghiep-vu-cho-thue-xe-may.txt`).
   - `docs/roadmap/`: Lộ trình phát triển (`Nghiep-vu-cho-thue-xe-may-roadmap.md`).
   - `docs/gemini-handoff/`: Tài liệu bàn giao kỹ thuật (`BUSINESS_RULES.md`, `CURRENT_TASK.md`, `VERIFICATION.md`).
   - `docs/phase1-20260928/` đến `docs/phase9-20260929/`: Tài liệu kiểm thử, fixtures và migration scripts (`V001` đến `V007`).

### B. Files Archived (Lưu trữ ngoài Git)
- 14 file snapshot database dump MySQL lớn (~27MB mỗi file, tổng cộng ~380MB) nằm rải rác trong `docs/` (`before-*.sql`, `database-before-*.sql`).
- File `places-import.tsv` (16MB).
- Các file zip snapshot source code cũ (`source-before-*.zip`).
- Toàn bộ được bảo tồn an toàn trong `D:\backup-motorent-20261012`.

### C. Files Deleted (Đã dọn dẹp khỏi thư mục làm việc)
1. **Log và Crash Dumps:**
   - `cookie.txt` (session cookie curl tạm).
   - `hs_err_pid49268.log`, `replay_pid49268.log` (JVM crash dumps cũ).
   - `spring.log`, `spring.log.*.gz` (runtime logs phát sinh cục bộ).
2. **Template Slices trùng lặp:**
   - 9 file `*-content.txt` nằm trong `src/main/resources/templates/` (`login-content.txt`, `register-content.txt`, `forgot-password-content.txt`, `motorbike-detail-content.txt`, `motorbike-list-content.txt`, `my-rentals-content.txt`, `profile-content.txt`, `rental-contract-content.txt`, `rental-history-content.txt`).
   - **Lý do xóa:** Đây là các đoạn HTML thô sinh ra trong quá trình thiết kế UI prototype, không được bất kỳ Controller, template HTML hay test nào tham chiếu. Các view chính (`.html`) đã tích hợp đầy đủ.

---

## 4. SECURITY & CREDENTIALS REPORT

1. **Khử cứng mật khẩu trong `application.properties`:**
   - Tất cả thông tin nhạy cảm đã được chuyển đổi thành biến môi trường với cú pháp chuẩn Spring:
     - Database: `${DB_URL:...}`, `${DB_USERNAME:...}`, `${DB_PASSWORD:}`
     - PayOS: `${PAYOS_CLIENT_ID:}`, `${PAYOS_API_KEY:}`, `${PAYOS_CHECKSUM_KEY:}`
     - Google Cloud Vision: `${GOOGLE_VISION_API_KEY:}`
     - Gmail SMTP: `${MAIL_HOST:...}`, `${MAIL_PORT:...}`, `${MAIL_USERNAME:}`, `${MAIL_PASSWORD:}`
     - JWT Secret: `${JWT_SECRET:...}`
2. **Tách biệt cấu hình cục bộ:**
   - Tạo file `application-local.properties` cho môi trường lập trình local. File này chứa thông tin kết nối máy cá nhân và đã được đưa vào `.gitignore`.
   - Cung cấp file mẫu `.env.example` với các placeholder rõ ràng, tuyệt đối không chứa credentials thật.
3. **Cấu hình `.gitignore` toàn diện:**
   - Chặn toàn bộ: `application-local.properties`, `*.local.properties`, `.env`, `.env.local`, `target/`, `*.log`, `cookie.txt`, `.idea/`, `uploads/*` (giữ lại cấu trúc thư mục bằng `.gitkeep`), các file dump `*.sql.gz`, `before-*.sql`, `database-before*.sql`, `*.tsv`, `*.zip`.
4. **Kiểm tra lịch sử Git (Git History Scan):**
   - Lịch sử commit trên remote `origin/phase-1` trước đây chưa từng commit file `application.properties` chứa secret thật (chỉ commit `.properties.example`). Do đó, không có lịch sử secret leak cần force-rewrite.
5. **Rủi ro bảo mật còn lại cần xử lý trong tương lai:**
   - Xoay vòng (Rotation) các API keys của Google Cloud Vision và PayOS trên hệ thống thực tế của chủ dự án để đảm bảo tiêu chuẩn an ninh cao nhất.
   - Thêm CSRF token vào form upload OCR CCCD (`src/main/resources/templates/customer/profile.html`).

---

## 5. BUILD & TEST VALIDATION (TRƯỚC VÀ SAU CLEANUP)

- **Trạng thái Build:** `BUILD SUCCESS` (Java 21, Apache Maven 3.9.8)
- **Bộ kiểm thử tự động tiêu chuẩn (`mvn test`):**
  - **Số lượng tests:** **122 / 122 tests PASSED** (0 Failures, 0 Errors, 0 Skipped).
  - Bao gồm:
    - Auth & Security Controller Tests
    - Booking & Catalog Service Tests
    - Extension & During-Rental Exchange Tests
    - Early Return & Refund Settlement Tests
    - Phase 7 Completion & Lateness Settlement Tests
    - Phase 8 Financial Report Ledger Invariant Tests
    - Phase 9 End-to-End Lifecycle Regression Tests
- **Bộ kiểm thử Concurrency trên clone MySQL 9.0.1 (`Phase1ConcurrencyTests`):**
  - **Số lượng tests:** **20 / 20 tests PASSED** (0 Failures, 0 Errors, 0 Skipped).
  - Kiểm tra race condition, pessimistic locking (thứ tự xe -> đơn -> extension -> payment), idempotency của callback PayOS và tính toàn vẹn số dư tài chính.
- **Kiểm tra hồi quy (Regression):** Không phát hiện bất kỳ lỗi hồi quy nào sau khi dọn dẹp file và chuẩn hóa cấu hình.

---

## 6. CURRENT PROJECT STATE (PHASE 1 - 9)

Hệ thống MotoRent hiện tại bao gồm:
1. **Khách hàng (Customer):**
   - Đăng ký, đăng nhập, quên mật khẩu, cập nhật profile và CCCD (tích hợp OCR Google Vision).
   - Tra cứu xe theo ngày nhận/trả, bộ lọc loại xe/hãng/giá.
   - Tạo đơn thuê (Booking), thanh toán cọc qua PayOS QR code.
   - Yêu cầu đổi xe trước khi nhận (Before Delivery) và trong thời gian thuê (During Rental Exchange).
   - Yêu cầu gia hạn hợp đồng (Rental Extension) và thanh toán phụ phí gia hạn.
   - Xem lịch sử đơn thuê và theo dõi trạng thái hợp đồng.
2. **Nhân viên / Vận hành (Staff / Operations):**
   - Dashboard quản lý đơn thuê trực tiếp (Live Operations).
   - Bàn giao xe (Handover): kiểm tra xe vật lý ngoài đời, xác nhận trên hệ thống.
   - Duyệt gia hạn và đổi xe; kiểm tra xung đột lịch xe tự động.
   - Tiếp nhận trả xe sớm (Early Return) và hoàn tất hợp đồng (Completion) với bảng quyết toán (Settlement).
3. **Quản trị viên (Admin):**
   - Quản lý danh mục xe, trạng thái xe, bảng giá theo ngày.
   - Quản lý tài khoản người dùng và phân quyền (CUSTOMER, STAFF, ADMIN).
   - Báo cáo tài chính, đối soát thanh toán và dòng tiền (IN/OUT).

---

## 7. ĐỀ XUẤT CÔNG VIỆC TIẾP THEO CHO AI ĐỘC LẬP (CHATGPT / DEVELOPER)

Dựa trên kết quả Full Project Audit, các hạng mục cần ưu tiên tiếp theo:
1. **Security & Credentials Rotation:** Cấp mới và cấu hình bí mật qua biến môi trường trên server production/staging. Thêm CSRF token cho endpoint OCR upload CCCD.
2. **Chính sách giá (Pricing Policy Finalization):** Thống nhất chính sách tính giá hoàn trả khi trả xe sớm và tính phạt trễ giờ (đặc biệt các mốc quá 24h hoặc qua đêm).
3. **Nâng cấp đối soát PayOS (Payment Hardening):** Bổ sung scheduled job tự động polling trạng thái thanh toán từ PayOS đối với các giao dịch khách hàng bị gián đoạn mạng.
4. **Quy trình nghiệm thu bàn giao xe vật lý:** Hỗ trợ nhân viên tải lên ảnh biên bản bàn giao xe và biên bản kiểm tra hư hại lúc trả xe.
5. **Tối ưu hóa Mobile UX & PWA:** Cải thiện trải nghiệm responsive trên thiết bị di động, chuyển đổi thành Progressive Web App (PWA) để nhân viên vận hành thao tác mượt mà trên điện thoại.
6. **Containerization & CI/CD Pipeline:** Xây dựng `Dockerfile` và `docker-compose.yml` chuẩn hóa, thiết lập GitHub Actions workflow tự động chạy kiểm thử trên mỗi pull request.
