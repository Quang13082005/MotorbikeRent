# BÁO CÁO KẾT QUẢ NGHIỆM THU PHASE 6 (2026-09-29)

## Trả sớm và hoàn tiền thuê xe (Early return and partial refund)

### 1. Phạm vi đã hoàn thành
- **Database Migration:** Đã áp dụng `V006__early_return_refund.sql` lên schema clone `motorent_verify_20260926` sau khi sao lưu an toàn tại `before-motorent_verify_20260926.sql`. Bổ sung các cột audit trên `rental_contracts`:
  - `early_return_requested_at` (DATETIME): thời điểm khách gửi yêu cầu.
  - `early_return_reason` (TEXT): lý do khách muốn trả sớm.
  - `early_return_refund_amount` (INT): số tiền Staff quyết định hoàn (kể cả 0 đồng).
  - `early_return_decision_reason` (TEXT): lý do/ghi chú quyết định hoàn tiền của Staff.
  - `early_return_payment_method` (VARCHAR): phương thức hoàn tiền (`CASH`, `BANK_TRANSFER`, `OTHER`).
- **Backend Implementation:**
  - `EarlyReturnRequest.java`, `CompleteOrderRequest.java`.
  - `RentalOrderService.java`:
    - `requestEarlyReturn`: Khách gửi yêu cầu khi đang thuê (`HANDED_OVER`, `EXTENDED`), chuyển trạng thái `RETURN_REQUESTED`. Giữ nguyên xe `RENTED`, đơn không tự `COMPLETED`, không tự động hoàn tiền.
    - `completeOrder`: Xử lý hoàn tất đơn kèm quyết toán trả sớm. Chặn số tiền hoàn âm hoặc vượt quá số tiền thuê ròng khách đã trả (`netRentalPaid`). Nếu `refundAmount > 0`, ghi nhận đúng 1 bản ghi `Payment` loại `EARLY_RETURN_REFUND`, direction `OUT`, status `PAID`, idempotent. Nếu `refundAmount = 0`, vẫn audit đầy đủ vào `RentalContract`. Giải phóng xe thành `AVAILABLE`, hủy các đề xuất đổi xe/gia hạn chờ thanh toán còn dang dở.
  - `RentalOrderApiController.java`: Cung cấp `POST /api/rental-orders/{id}/early-return` và mở rộng `POST /api/rental-orders/{id}/complete`.
- **Frontend Implementation:**
  - `rental-detail.html`, `customer.js`: Nút "Yêu cầu trả xe sớm", banner hiển thị hotline khi đơn ở trạng thái `RETURN_REQUESTED`.
  - `staff/rental-orders.html`, `staff.js`, `operations-live.js`: Modal/prompt nhập số tiền hoàn tiền thuê, lý do, phương thức khi bấm "Hoàn tất đơn".

---

### 2. Bằng chứng kiểm thử thực tế

1. **Automated Unit & Integration Tests:**
   - Chạy qua Maven với `.phase6-verification-pom.xml`: **101/101 tests PASS** (0 failures, 0 errors, 0 skipped).
   - Log: [docs/phase6-20260929/package.log](file:///d:/Motorbike-rental/docs/phase6-20260929/package.log).
2. **MySQL Concurrency Tests:**
   - Chạy trên clone `motorent_verify_20260926`: **20/20 test cases PASS** (0 failures, 0 errors).
   - Log: [docs/phase6-20260929/tests-mysql.log](file:///d:/Motorbike-rental/docs/phase6-20260929/tests-mysql.log).
3. **Đóng gói Artifact:**
   - File JAR hoàn chỉnh: `target/phase6/motorbike-rental-0.0.1-SNAPSHOT.jar`.
4. **Kiểm thử HTTP trên môi trường Clone (Cổng 8085, PID 11912):**
   - **Operations HTTP:** **36/36 checks PASS** ([docs/phase6-20260929/operations-http.log](file:///d:/Motorbike-rental/docs/phase6-20260929/operations-http.log)).
   - **Customer HTTP:** **12/12 checks PASS** ([docs/phase6-20260929/customer-http.log](file:///d:/Motorbike-rental/docs/phase6-20260929/customer-http.log)).
   - **Phase 6 HTTP Flow (Cases 43–47):** **ALL CHECKS PASS** ([docs/phase6-20260929/phase6-http.log](file:///d:/Motorbike-rental/docs/phase6-20260929/phase6-http.log)):
     - Case 43: Customer gửi yêu cầu trả sớm thành công (đơn chuyển `RETURN_REQUESTED`).
     - Case 44: Đơn không tự `COMPLETED`, xe không tự giải phóng (`RENTED`), không tự refund; giao diện khách hàng hiển thị hotline cửa hàng.
     - Case 45: Chặn hoàn tiền quá hạn mức đã thanh toán (HTTP 403), chặn số tiền hoàn âm.
     - Case 46: Staff hoàn tất đơn và ghi nhận giao dịch `EARLY_RETURN_REFUND`, direction `OUT`, status `PAID`, số tiền 100.000 VNĐ.
     - Case 47: Xe trở lại `AVAILABLE`, audit đầy đủ nhân viên hoàn tất và hợp đồng, chống gọi lặp (idempotent).

---

### 3. Đối soát Baseline Dữ liệu
- **`motorent_verify_20260926`:** 2 orders, 2 payments, tổng thu 110.000 VNĐ.
- **`motorent_db`:** 2 orders, 2 payments, tổng thu 110.000 VNĐ.
- Dọn dẹp sạch sẽ fixture test `PHASE6-UI-20260929`, không để lại bất kỳ dữ liệu rác nào.
