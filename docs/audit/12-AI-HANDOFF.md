# 12 - TÀI LIỆU BÀN GIAO TIẾP QUẢN DỰ ÁN (AI HANDOFF GUIDE)

> **DÀNH CHO KỸ SƯ AI TIẾP THEO (FOR THE NEXT AGENTIC AI):**  
> Tài liệu này được biên soạn để bạn có thể tiếp nhận và bắt tay vào làm việc ngay lập tức trên dự án MotoRent mà không phải đoán mò hay quét lại từ đầu. Đọc kỹ và tuân thủ tuyệt đối các nguyên tắc an toàn dưới đây.

---

## 1. Bối Cảnh Dự Án & Bản Đồ Kiến Trúc Đã Được Kiểm Định
- **Tên dự án:** MotoRent (`motorbike-rental`) — Hệ thống quản lý cho thuê xe máy tại Đà Nẵng.
- **Vị trí thư mục:** `d:/Motorbike-rental` (Không có `.git`, không dùng các lệnh `git reset` hay `git clean`).
- **Stack công nghệ thực tế:**
  - Java 21 LTS (`jdk-21`), Spring Boot 4.1.1, Spring Framework 7.0.9.
  - Maven 3.9.8 (đường dẫn: `C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.2.4\plugins\maven\lib\maven3\bin\mvn.cmd`).
  - MySQL 9.0.1 (Community Server) đang chạy trên cổng `3306`.
  - Frontend: Thymeleaf SSR kết hợp REST API, Tailwind CDN, Vanilla JavaScript (không có Node/React/Vue build step).
- **Cơ sở dữ liệu:**
  - Database chính: `motorent_db` (đang có 4 đơn, 4 payments).
  - Database kiểm thử cách ly: `motorent_verify_20260926` (bảo toàn baseline: đúng 2 đơn, 2 payments = 110.000 VNĐ).

---

## 2. Trạng Thái Hiện Tại (Exact Current State)
- **Tiến độ nghiệp vụ lõi (Core Rental & Financial Lifecycle):** Đã hoàn tất 100% 65 ca kiểm thử chấp nhận (Business Cases 1–65).
- **Kiểm thử tự động:**
  - 122/122 unit/integration tests PASS trên H2 (`mvn test -f .phase9-verification-pom.xml`).
  - 20/20 MySQL concurrency tests PASS trên database clone `motorent_verify_20260926` (`-Dphase1.mysql=true`).
  - Kịch bản PowerShell E2E (`verify-phase9-full.ps1`) chạy trên live runtime port 8087 PASS 100%.
- **Trạng thái quy trình:** Đã hoàn thành đợt kiểm toán toàn diện (Full Project Audit). Không có tiến trình chạy ngầm nào bị bỏ quên.

---

## 3. Các Lỗi Đã Xác Nhận Cần Xử Lý Ngay (Confirmed Bugs)
1. **MBR-001 (High): Lỗi CSRF tại tính năng quét CCCD OCR.**
   - File: `src/main/resources/templates/customer/rental-contract.html` (dòng 472).
   - Nguyên nhân: Hàm `handleScanImage()` gọi `fetch('/api/ocr/cccd')` thiếu header `X-XSRF-TOKEN`, bị chặn `403 Forbidden`.
   - Cách sửa: Lấy cookie `XSRF-TOKEN` và đính kèm vào headers của fetch request.
2. **MBR-002 (Medium): Thuật toán tính ngày thuê xe.**
   - File: `src/main/java/com/motorrent/motorbikerental/service/PricingService.java` (dòng 9-14).
   - Hiện trạng: Tính trần số ngày theo chu kỳ 24h (`(seconds + 86399) / 86400`) thay vì mốc 12:00/20:00. Đang chờ chủ dự án quyết định giữ nguyên hay sửa.
3. **MBR-005 (Critical): Thông tin bí mật hardcoded trong `application.properties`.**
   - Cần chuyển mật khẩu DB, Gmail password, Google Vision API Key, PayOS keys sang biến môi trường.

---

## 4. Các Tệp Tin Then Chốt Phải Biết (Key Source Files)

| Vai trò / Lĩnh vực | Đường dẫn tệp tin | Ghi chú quan trọng |
|---|---|---|
| **Bảo mật & Phân quyền** | `src/main/java/com/motorrent/motorbikerental/config/SecurityConfig.java` | JWT Stateless + Cookie CSRF rotation, cấu hình quyền các URL |
| **Ngữ cảnh người dùng** | `src/main/java/com/motorrent/motorbikerental/security/CurrentUser.java` | Chứa `customer()`, `operator()`, `canRead()`, `owns()` chống IDOR |
| **Khóa bi quan & Xe trống**| `src/main/java/com/motorrent/motorbikerental/service/AvailabilityService.java` | Khóa `SELECT FOR UPDATE` xe theo thứ tự ID tăng dần |
| **Tính giá & Giao nhận** | `src/main/java/com/motorrent/motorbikerental/service/PricingService.java` | Tính số ngày, tính phí giao nhận theo công thức Haversine |
| **Cổng PayOS & Sổ cái** | `src/main/java/com/motorrent/motorbikerental/service/PaymentService.java` | Tạo link, webhook SHA-256, đối soát reconcile, xử lý tiền trễ |
| **Đổi xe trước giao** | `src/main/java/com/motorrent/motorbikerental/service/VehicleChangeService.java` | Đổi xe trước khi giao, tính lại toàn bộ tiền thuê xe mới |
| **Đổi xe khi đang thuê**| `src/main/java/com/motorrent/motorbikerental/service/DuringRentalChangeService.java`| Đổi xe giữa chặng, chỉ tính phí chặng mới theo xe mới |
| **Gia hạn hợp đồng** | `src/main/java/com/motorrent/motorbikerental/service/ExtensionService.java` | Gia hạn ngày trả xe kèm thanh toán phí gia hạn |
| **Báo cáo tài chính Admin**| `src/main/java/com/motorrent/motorbikerental/service/OperationsService.java` | Tính doanh thu thuần, quỹ cọc tài sản giữ hộ, lợi nhuận |
| **Hợp đồng & OCR CCCD** | `src/main/resources/templates/customer/rental-contract.html` | Trang điền thông tin hợp đồng và quét CCCD |

---

## 5. Các Lệnh Vận Hành & Kiểm Thử Chuẩn (Standard Commands)

### 5.1. Chạy bài kiểm thử tự động (122 tests):
```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" test -f .phase9-verification-pom.xml
```

### 5.2. Chạy bài kiểm thử Concurrency trên MySQL (20 tests):
```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" test -f .phase9-verification-pom.xml "-Dtest=Phase1ConcurrencyTests" "-Dphase1.mysql=true"
```

### 5.3. Khởi chạy server kiểm thử độc lập trên cổng 8087 (Daemon):
```powershell
java -jar target/phase9/motorbike-rental-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1 --server.port=8087 --spring.jpa.hibernate.ddl-auto=validate --app.seed-demo=false --app.base-url=http://127.0.0.1:8087 --spring.datasource.url=jdbc:mysql://localhost:3306/motorent_verify_20260926?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh --app.payments-enabled=false --payos.client-id=test --payos.api-key=test --payos.checksum-key=test --spring.ai.openai.api-key=test --google.cloud.vision.api-key=test --google.maps.api-key= --spring.mail.host=127.0.0.1 --spring.mail.port=1 --logging.file.name=target/runtime/application-8087.log
```

### 5.4. Chạy kịch bản kiểm thử hồi quy 65 ca:
```powershell
& .\docs\phase9-20260929\verify-phase9-full.ps1
```

---

## 6. Quy Tắc Phát Triển Bắt Buộc (Safe Development Rules)
1. **Tuyệt đối không sửa code mò:** Đọc kỹ `AGENTS.md` và `PROJECT_CONTEXT.md` trước khi sửa bất kỳ dòng code nào.
2. **Không tự ý đổi chính sách tiền tệ:** Tiền cọc tài sản là Nợ phải trả (`ASSET_DEPOSIT`), không bao giờ được tính là Doanh thu (`REVENUE`).
3. **Bảo toàn dữ liệu Baseline:** Khi thực hiện bất kỳ bài test ghi dữ liệu nào, phải dọn dẹp sạch sẽ sau test, đảm bảo database clone luôn giữ đúng: **2 đơn thuê, 2 thanh toán = 110.000 VNĐ**.
4. **Không can thiệp cổng 8080 của người dùng:** Người dùng có thể đang mở dự án trên IntelliJ IDEA chạy cổng 8080. Khi test local, ưu tiên sử dụng các cổng cách ly (như 8082, 8087).
5. **Không in thông tin nhạy cảm:** Không bao giờ print mật khẩu hoặc private keys vào báo cáo hoặc terminal log.
6. **Báo cáo bằng tiếng Việt:** Luôn trình bày tóm tắt công việc bằng tiếng Việt cho chủ dự án theo phong cách chuyên nghiệp.

---

## 7. Nhiệm Vụ Tiếp Theo Được Đề Xuất (Next Recommended Task)
- **Nhiệm vụ 1:** Sửa lỗi **MBR-001** (Thêm CSRF token vào `fetch('/api/ocr/cccd')` trong file `rental-contract.html`).
- **Nhiệm vụ 2:** Chuyển dịch toàn bộ secret keys trong `application.properties` sang Environment Variables.
- **Nhiệm vụ 3:** Khởi tạo Git repository (`git init`) và commit toàn bộ source code sạch.
