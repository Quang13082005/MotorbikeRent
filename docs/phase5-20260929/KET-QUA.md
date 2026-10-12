# Kết quả kiểm tra nghiệm thu Phase 5 — 29/09/2026

Đã hoàn tất toàn bộ triển khai và kiểm thử nghiệm thu **Phase 5: Đổi xe trong lúc thuê (During-rental vehicle exchange)** theo mục 3B kế hoạch `docs/continuation-20260928/DOI-CHIEU-VA-KE-HOACH.html` và tuân thủ các quy tắc trong `AGENTS.md`.

## 1. Kết quả kiểm thử thực tế

1. **Kiểm thử tự động (Unit & Integration Tests):**
   - Chạy 94/94 automated tests qua Maven wrapper với `.phase5-verification-pom.xml`: **94 passed, 0 failures, 0 errors, 0 skipped**.
   - Bằng chứng lưu tại: `docs/phase5-20260929/package.log`.

2. **Kiểm thử tương tranh trên MySQL Clone (`motorent_verify_20260926`):**
   - Chạy `Phase1ConcurrencyTests` với `-Dphase1.mysql=true`: **20/20 test cases PASS, 0 failures, 0 errors**.
   - Bao gồm các ca kiểm thử tương tranh Phase 5:
     - `duringOnlyStaffCanProposeAndNoMidSegmentPolicyIsInvented`: xác minh chỉ Staff mới có quyền đề xuất đổi xe; Customer gọi bị từ chối `AccessDeniedException` (403); chặn đề xuất khi đang có gia hạn chờ duyệt.
     - `duringConcurrentCallbacksPostOnce`: hai callback thanh toán PayOS gửi đồng thời cho một đề xuất đổi xe $\to$ chỉ ghi nhận dòng tiền 1 lần duy nhất, chuyển trạng thái `READY_FOR_EXCHANGE`.
     - `duringTwoStaffConfirmOnlyOneAssetMovement`: hai Staff bấm nút "Xác nhận đã đổi xe" đồng thời $\to$ cơ chế khóa bi quan và kiểm tra trạng thái chỉ ghi nhận khoản `ASSET_DEPOSIT_ADDITIONAL` một lần duy nhất, tránh thu trùng tiền cọc.
   - Bằng chứng lưu tại: `docs/phase5-20260929/tests-mysql.log`.

3. **Kiểm thử HTTP trên môi trường Clone (Cổng 8084):**
   - **Operations Regression:** 36/36 checks PASS (`docs/phase5-20260929/operations-http.log`).
   - **Customer Regression:** 12/12 checks PASS (`docs/phase5-20260929/customer-http.log`).
   - **Phase 5 During-Rental Flow:** 14/14 checks PASS (`docs/phase5-20260929/phase5-http.log`):
     - Xác thực phân quyền: Khách hàng gọi API đề xuất đổi xe bị chặn 403 Forbidden.
     - Tra cứu xe thay thế khả dụng theo khoảng thời gian đổi.
     - Báo giá đổi xe chặng tiếp theo tính đúng tiền thuê mới, cọc yêu cầu và chênh lệch cọc.
     - Tạo đề xuất đổi xe: trạng thái `WAITING_FOR_PAYMENT`, xe và giá ban đầu trên đơn giữ nguyên cho đến khi thanh toán thành công.
     - Khách hàng xem đề xuất trong chi tiết đơn; gọi thanh toán trên clone trả về 503 (bảo vệ không gọi cổng thật).
     - Mô phỏng thanh toán thành công: đơn chuyển sang `EXTENDED`, đề xuất chuyển sang `READY_FOR_EXCHANGE`. Xe cũ vẫn giữ nguyên (`RENTED`), xe mới chưa bàn giao thực tế.
     - Staff bấm "Xác nhận đã đổi xe thực tế": xe trên đơn cập nhật sang xe mới, xe cũ chuyển về `AVAILABLE`, xe mới chuyển thành `RENTED`, ghi nhận đúng khoản cọc bổ sung `ASSET_DEPOSIT_ADDITIONAL` (IN, PAID, không tính vào doanh thu).
     - Kiểm thử idempotency: gọi lại API confirm lần 2 không tạo thêm bất kỳ khoản tiền nào.
     - Khách hàng xem lại trang chi tiết đơn hiển thị xe mới đầy đủ.

4. **Bảo toàn dữ liệu Baseline:**
   - Fixture tạm `PHASE5-UI-20260929` đã được dọn sạch hoàn toàn qua `Phase5Fixture.java cleanup`.
   - Đối soát baseline: Cả `motorent_db` và `motorent_verify_20260926` đều bảo toàn đúng **2 orders, 2 payments = 110.000 VNĐ**.

---

## 2. Danh sách file thay đổi & bổ sung trong Phase 5

1. **Migration & DB Helpers:**
   - `docs/phase5-20260929/V005__during_rental_exchange.sql` (bổ sung cột audit/snapshot vào `rental_vehicle_changes`).
   - `docs/phase5-20260929/DatabasePhase5.java` (helper sao lưu, migrate và inspect baseline).
   - `docs/phase5-20260929/Phase5Fixture.java` (tạo và dọn dẹp fixture kiểm thử trên clone).
2. **Backend Logic & DTO:**
   - `src/main/java/com/motorrent/motorbikerental/dto/request/DuringRentalQuoteRequest.java`
   - `src/main/java/com/motorrent/motorbikerental/service/AssetDepositAmounts.java`
   - `src/main/java/com/motorrent/motorbikerental/service/DuringRentalQuoteService.java`
   - `src/main/java/com/motorrent/motorbikerental/service/DuringRentalChangeService.java`
   - `src/main/java/com/motorrent/motorbikerental/controller/api/DuringRentalQuoteController.java`
   - `src/main/java/com/motorrent/motorbikerental/entity/RentalVehicleChange.java`
   - `src/main/java/com/motorrent/motorbikerental/service/PaymentService.java` (tích hợp `VEHICLE_CHANGE_DURING`)
   - `src/main/java/com/motorrent/motorbikerental/service/VehicleChangeService.java` (hỗ trợ view cho cả during-rental)
3. **Frontend & Templates:**
   - `src/main/resources/static/js/during-rental-changes.js`
   - `src/main/resources/templates/fragments/staff-layout.html`
   - `src/main/resources/templates/fragments/admin-layout.html`
4. **Kiểm thử tự động:**
   - `src/test/java/com/motorrent/motorbikerental/AssetDepositAmountsTests.java`
   - `src/test/java/com/motorrent/motorbikerental/CustomerSecurityIntegrationTests.java` (thêm 2 test cấm customer gọi API đổi xe)
   - `src/test/java/com/motorrent/motorbikerental/Phase1ConcurrencyTests.java` (thêm 3 bài test tương tranh cho Phase 5)

---

## 3. Giới hạn và các bước kế tiếp

- **Giới hạn môi trường:** Không thực hiện giao dịch ngân hàng thật trên PayOS. Mọi luồng PayOS trên clone đều được ngắt kết nối thực và kiểm thử qua mô phỏng nội bộ có kiểm tra chữ ký/idempotency.
- **Tiến trình đang chạy:**
  - Cổng 8084 (PID 15616): Runtime Phase 5 trên clone `motorent_verify_20260926`.
  - Cổng 8080: Runtime IntelliJ của người dùng (giữ nguyên không can thiệp).
  - Cổng 8082, 8083: Các runtime cũ của Phase 3 và Phase 4.
- **Bước tiếp theo:**
  - Theo kế hoạch 9 giai đoạn đã duyệt, bước tiếp theo là **Phase 6: Trả sớm và hoàn tiền thuê (Early return and partial refund)**.
  - Lưu ý: Không tự ý bắt đầu Phase 6 cho đến khi chủ sở hữu duyệt nghiệm thu Phase 5.
