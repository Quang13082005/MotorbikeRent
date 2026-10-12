# 07 - BÁO CÁO KIỂM THỬ THỰC TẾ (TEST EXECUTION REPORT)

Báo cáo chi tiết kết quả thực hiện các bộ kiểm thử tự động, kiểm thử tranh chấp đồng thời trên cơ sở dữ liệu thật, kiểm thử giao diện End-to-End và kiểm thử bảo mật.

---

## 1. Tóm Tắt Kết Quả Kiểm Thử (Executive Test Summary)

| Loại Kiểm Thử (Test Suite) | Môi Trường Thực Hiện | Số Lượng Tests | PASS | FAIL | BLOCKED | Kết Quả | Thời Gian Thực Thi |
|---|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **Maven Automated Suite** | Java 21, H2 / Mockito | 122 | 122 | 0 | 0 | **100% PASS** | 2 phút 55 giây |
| **MySQL Concurrency Suite** | MySQL 9.0.1 (`motorent_verify_20260926`) | 20 | 20 | 0 | 0 | **100% PASS** | 1 phút 01 giây |
| **E2E PowerShell Suite** | Live Port 8087, MySQL Clone | 65 (Cases 1–65) | 65 | 0 | 0 | **100% PASS** | 18 giây |
| **Browser UI / Mobile Audit** | Chrome DevTools (Port 8087) | 4 Pages / 2 Viewports | 4 | 0 | 0 | **PASS** | 45 giây |
| **TỔNG HỢP KIỂM THỬ** | Toàn bộ môi trường local | **211 Tests** | **211** | **0** | **0** | **100% PASS** | ~4 phút 59 giây |

---

## 2. Chi Tiết Kiểm Thử Tự Động Maven (Maven Surefire 122 Tests)
Lệnh thực thi:
```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" test -f .phase9-verification-pom.xml
```

### Danh sách các lớp kiểm thử (Test Classes):
1. **`AssetDepositAmountsTests` (6 tests):** Xác minh thuật toán tính toán cọc tài sản, quote bổ sung và kiểm toán số dư không tính vào doanh thu.
2. **`BeforeDeliveryChangeTests` (7 tests):** Kiểm thử quy trình đổi xe trước khi bàn giao, hoàn cọc chênh lệch, tạo link thanh toán bù giá.
3. **`CustomerPaymentResultRouteTests` (2 tests):** Đảm bảo trang kết quả thanh toán không xác nhận đơn sai trái từ query parameter.
4. **`CustomerSecurityIntegrationTests` (34 tests):** Kiểm thử phân quyền, phòng chống IDOR, xác thực token JWT, cookie CSRF rotation, chặn truy cập trái phép.
5. **`EarlyReturnTests` (7 tests):** Kiểm thử quy trình trả xe sớm, hoàn tiền một phần có kiểm toán của Staff.
6. **`FinancialsTests` (3 tests):** Kiểm thử tính đúng đắn của hàm tính toán tiền âm/dương (`IN`/`OUT`) và componentsJson.
7. **`JwtCsrfIntegrationTests` (3 tests):** Kiểm thử tích hợp giữa JWT Authentication và Cookie CSRF Filter.
8. **`MotorbikeRentalApplicationTests` (1 test):** Kiểm thử Spring Application Context load thành công.
9. **`OperationsPageControllerTests` (4 tests):** Kiểm thử các trang quản lý điều hành Thymeleaf cho Staff/Admin.
10. **`OperationsWorkflowTests` (10 tests):** Kiểm thử quy trình vận hành từ tiếp nhận, đổi xe đến hoàn tất.
11. **`PayOSReconciliationIntegrationTests` (10 tests):** Kiểm thử xác thực chữ ký Webhook, đối soát reconcile, xử lý thanh toán trễ, sai số tiền.
12. **`Phase1ConcurrencyTests` (20 tests):** Kiểm thử đa luồng với bộ dữ liệu concurrency.
13. **`Phase7CompletionTests` (6 tests):** Kiểm thử quyết toán hoàn cọc tài sản, khấu trừ hư hỏng và thu phí quá hạn.
14. **`Phase8FinancialReportTests` (5 tests):** Kiểm thử các bất biến tài chính trong báo cáo doanh thu Admin.
15. **`Phase9EndToEndRegressionTests` (4 tests):** Kiểm thử hồi quy toàn diện chéo module.

**Kết quả Maven:**
`[INFO] Tests run: 122, Failures: 0, Errors: 0, Skipped: 0`  
`[INFO] BUILD SUCCESS` (Tổng thời gian: 02:55 min).

---

## 3. Chi Tiết Kiểm Thử Tranh Chấp Đồng Thời MySQL (MySQL 20 Concurrency Tests)
Lệnh thực thi:
```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" test -f .phase9-verification-pom.xml "-Dtest=Phase1ConcurrencyTests" "-Dphase1.mysql=true"
```
Kết nối thực tế: `jdbc:mysql://localhost:3306/motorent_verify_20260926?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh` (MySQL Community Server 9.0.1, Isolation Level: `REPEATABLE_READ`).

### Các ca kiểm thử đồng thời tiêu biểu đã PASS:
1. `simultaneousBookingsOnlyOneCommits`: Hai khách hàng cùng lúc gửi request đặt một xe trong cùng khoảng thời gian -> Đúng 1 request thành công (200), request còn lại bị từ chối với thông báo "Xe không còn trống" (400).
2. `simultaneousCallbacksRemainPaidWithoutDuplicateOrReview`: Webhook từ PayOS gửi trùng lặp đồng thời 2 lần -> Hệ thống áp dụng đúng 1 lần, trạng thái giữ nguyên `PAID`, không tạo bản ghi trùng hoặc lỗi review.
3. `simultaneousRefundsOnlyOnePostsAndAudits`: Hai nhân viên cùng bấm hoàn cọc đồng thời -> Đúng 1 giao dịch `OUT` được ghi nhận, hành động thứ hai bị chặn.
4. `twoOrdersCannotHoldTheSameReplacement`: Hai đơn hàng khác nhau cùng lúc chọn đổi sang cùng một chiếc xe thay thế -> Đúng 1 đơn giữ được xe thay thế, đơn kia bị chặn.
5. `duringTwoStaffConfirmOnlyOneAssetMovement`: Hai nhân viên cùng lúc bấm xác nhận đổi xe khi đang thuê -> Chỉ 1 giao dịch bổ sung cọc tài sản được ghi vào sổ cái.
6. `twoStaffHandoverActionsOnlyPostOnce`: Hai nhân viên cùng lúc thao tác bàn giao xe -> Đúng 1 giao dịch thu tiền thuê còn lại và cọc tài sản được ghi nhận.

---

## 4. Chi Tiết Kiểm Thử End-to-End PowerShell (`verify-phase9-full.ps1`)
Chạy trực tiếp trên cổng 8087 (PID 2012 / 15228):
- **Customer Pages (8/8 PASS):** `/`, `/customer/home`, `/customer/motorbike-list`, `/customer/shop-information`, `/customer/travel-places`, `/customer/profile`, `/customer/rental-history`, `/customer/my-rentals`.
- **Báo giá cọc xe Option 1 & Option 2 (PASS):**
  - Xe 9 (Xe số): Option 1 cọc 1.000.000đ | Option 2 cọc 0đ.
  - Xe 7 (Xe ga): Option 1 cọc 2.000.000đ | Option 2 cọc 0đ.
  - Xe 12 (Xe côn): Option 1 cọc 1.500.000đ | Option 2 cọc 0đ.
- **Chặn đặt ngày trong quá khứ (PASS):** Request đặt ngày quá khứ bị từ chối với mã `400 Bad Request`.
- **Staff Operations & Chặn quyền Admin (PASS):** Staff xem được các trang và API vận hành nhưng bị chặn `403 Forbidden` khi cố truy cập `/admin/settings`, `/api/admin/reports`, `/api/operations/data/customers`, `/api/operations/data/staffs`.
- **Admin Management & Invariants (PASS):** 12 trang Admin tải thành công 200 OK. Báo cáo tài chính `/api/admin/reports` thỏa mãn 100% các bất biến kế toán.

---

## 5. Bằng Chứng Trực Quan Từ Browser Subagent (Screenshots & Recordings)
Được chụp và ghi lại trong phiên kiểm thử trên cổng 8087:
- **Video Recording WebP:** `e2e_ui_audit_1791770910991.webp` (Lưu tại thư mục artifacts).
- **Screenshot Desktop (1280x800):** `customer_home_desktop_1791770923488.png`
  - Đánh giá: Header, logo, thanh điều hướng, banner khuyến mãi, bộ lọc tìm kiếm xe hiển thị chuẩn xác, không vỡ layout.
- **Screenshot Mobile (393x852 - iPhone 14/15 Viewport):** `customer_home_mobile_1791770931858.png`
  - Đánh giá: Giao diện tự động co giãn, thanh tìm kiếm xếp dọc thân thiện với ngón tay cái, các thẻ xe hiển thị ảnh rõ nét.
- **Screenshot Login Page (Mobile):** `login_page_mobile_1791770968145.png`
  - Đánh giá: Form đăng nhập căn giữa đẹp mắt, các trường username/password có biểu tượng trực quan, nút đăng nhập to bản dễ bấm.

---

## 6. Bảo Toàn Tuyệt Đối Dữ Liệu Baseline
Sau toàn bộ các bài kiểm thử:
- Database clone `motorent_verify_20260926`: **Đúng 2 đơn thuê, 2 giao dịch thanh toán = 110.000 VNĐ**.
- Database chính `motorent_db`: **Bảo toàn nguyên vẹn**.
