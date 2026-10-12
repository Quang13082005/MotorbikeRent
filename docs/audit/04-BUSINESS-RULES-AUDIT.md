# 04 - ĐỐI CHIẾU QUY TẮC NGHIỆP VỤ (BUSINESS RULES AUDIT)

Tài liệu này đối chiếu chi tiết giữa **Kỳ Vọng Nghiệp Vụ Ban Đầu (Expected Business Baseline)** và **Mã Nguồn Thực Tế (Actual Implementation & Behavior)** của dự án MotoRent.

---

## 1. Vai Trò & Phân Quyền (Roles & Access Control)

### Kỳ vọng (Expected Baseline):
- 3 vai trò: `CUSTOMER`, `STAFF`, `ADMIN`.
- Khách hàng không thể đọc/sửa đơn thuê của khách hàng khác.
- Nhân viên không thể thực hiện chức năng Admin.
- Không thể bypass authorization bằng cách gọi trực tiếp API.
- Không tin tưởng role, userId, rentalId hoặc payment status do client gửi lên.

### Thực tế trong mã nguồn (Actual Code):
- **Đối chiếu: HOÀN TOÀN KHỚP (100% MATCH).**
- File [SecurityConfig.java](file:///d:/Motorbike-rental/src/main/java/com/motorrent/motorbikerental/config/SecurityConfig.java) phân chia rõ ràng các tiền tố URL:
  - `/api/admin/**` và `/admin/**` yêu cầu `hasRole('ADMIN')`.
  - `/api/operations/**` và `/staff/**` yêu cầu `hasAnyRole('STAFF', 'ADMIN')`.
  - `/api/rental-orders/**` yêu cầu `authenticated()`.
- File [CurrentUser.java](file:///d:/Motorbike-rental/src/main/java/com/motorrent/motorbikerental/security/CurrentUser.java) lấy định danh trực tiếp từ `SecurityContextHolder.getContext().getAuthentication()`:
  - `current.customer().getId()` đảm bảo các thao tác tạo đơn hoặc truy vấn chỉ lấy dữ liệu của chính người dùng.
  - `current.operator().getId()` đảm bảo chỉ nhân viên hoặc admin mới được thao tác duyệt, giao xe, hoàn tất đơn.
  - `canRead(RentalOrder)` và `owns(RentalOrder)` kiểm tra quyền sở hữu bản ghi đơn thuê, chặn đứng hoàn toàn lỗ hổng IDOR.

---

## 2. Vòng Đời Đơn Thuê (Rental Order Lifecycle)

### Kỳ vọng (Expected Baseline):
- Trình tự: `Đặt xe` -> `Thanh toán cọc giữ xe` -> `Nhân viên xác nhận` -> `Giao xe` -> `Đang thuê` -> `Trả xe / Kiểm tra` -> `Hoàn cọc tài sản / Khấu trừ` -> `Hoàn tất`.

### Tên Enum & Trạng thái Thực tế trong Source Code:
Enum [RentalStatus.java](file:///d:/Motorbike-rental/src/main/java/com/motorrent/motorbikerental/enums/RentalStatus.java):
- `PENDING_PAYMENT`: Chờ thanh toán đặt cọc (có thời hạn giữ chỗ `holdMinutes`, mặc định 3 phút).
- `PENDING`: Đã thanh toán cọc giữ chỗ, chờ nhân viên xác nhận đơn.
- `CONFIRMED`: Nhân viên đã xác nhận đơn thuê.
- `READY_FOR_HANDOVER`: Xe đã được chuẩn bị sẵn sàng bàn giao.
- `HANDED_OVER`: Xe đã được bàn giao cho khách (Tương đương "Đang thuê").
- `RETURN_REQUESTED`: Khách hàng gửi yêu cầu trả xe sớm.
- `RETURNED`: Xe đã được khách hàng trả lại tại cửa hàng.
- `COMPLETED`: Đơn thuê đã hoàn tất (đã quyết toán cọc và các chi phí phát sinh).
- `CANCELLED`: Đơn bị hủy (do khách hàng chủ động hủy khi chưa thanh toán hoặc hết hạn giữ chỗ).
- `REJECTED`: Đơn bị nhân viên từ chối tiếp nhận (kèm lý do và hoàn cọc nếu đã thu).
- `OVERDUE`: Đơn thuê quá hạn trả xe dự kiến.
- `EXTENDED`: Đơn thuê đã gia hạn thời gian trả xe thành công.

### Đánh giá tính nhất quán:
- Hệ thống duy trì tính nhất quán cao. Trạng thái xe (`MotorbikeStatus`) tự động chuyển đổi đồng bộ với trạng thái đơn:
  - Khi xe đang được thuê (`HANDED_OVER`), trạng thái xe là `RENTED`.
  - Khi đơn hoàn tất (`COMPLETED`) hoặc hủy (`CANCELLED`/`REJECTED`), trạng thái xe tự động trở về `AVAILABLE`.

---

## 3. Quy Tắc Hai Loại Tiền Cọc (Two-Tier Deposit System)

### Kỳ vọng (Expected Baseline):
- **Cọc giữ xe (Booking Deposit):** Thanh toán online qua PayOS (50% tiền thuê + 100% phí giao nhận xe). Được cấn trừ vào tiền thuê khi nhận xe.
- **Cọc tài sản (Asset Deposit):** Thu khi bàn giao xe (tiền mặt / chuyển khoản). Hoàn trả khi trả xe sau khi kiểm tra không có hư hại.
- Mức cọc tài sản:
  - 1 CCCD, Xe số: 1.000.000 VND
  - 1 CCCD, Xe ga: 2.000.000 VND
  - 1 CCCD, Xe côn: 1.500.000 VND
  - 2 CCCD của 2 người khác nhau: 0 VND (Bảo lãnh trách nhiệm)

### Thực tế trong mã nguồn (Actual Code):
- **Đối chiếu: HOÀN TOÀN KHỚP (100% MATCH).**
- File [PricingService.java](file:///d:/Motorbike-rental/src/main/java/com/motorrent/motorbikerental/service/PricingService.java) dòng 24-35:
  ```java
  int security = Integer.valueOf(2).equals(r.getSecurityOption()) ? 0 : securityDeposit(bike);
  int due = Math.addExact(Math.toIntExact((long)(rental - discount) * settings().getDepositPercent() / 100), delivery);
  ```
- File [AssetDepositAmounts.java](file:///d:/Motorbike-rental/src/main/java/com/motorrent/motorbikerental/service/AssetDepositAmounts.java) phân biệt rõ ràng số dư cọc tài sản đang giữ (`held`) và không bao giờ tính cọc tài sản vào doanh thu kinh doanh.

---

## 4. Quy Tắc Tính Thời Gian Thuê (Rental Duration & Pricing Calculation)

### Kỳ vọng (Expected Baseline):
- Nhận xe trước 12:00: ngày thuê đầu tiên kết thúc lúc 20:00 cùng ngày.
- Nhận xe từ 12:00 trở đi: ngày thuê đầu tiên kết thúc lúc 12:00 hôm sau.
- Các ngày tiếp theo tính theo mốc giờ kết thúc tương ứng.

### Thực tế trong mã nguồn (Actual Code):
- **Đối chiếu: KHÁC BIỆT LỚN (DISCREPANCY DETECTED - MBR-002).**
- File [PricingService.java](file:///d:/Motorbike-rental/src/main/java/com/motorrent/motorbikerental/service/PricingService.java) dòng 9-14:
  ```java
  public static int days(LocalDateTime start, LocalDateTime end) {
      if (start == null || end == null || !end.isAfter(start)) 
          throw new IllegalArgumentException("Ngày trả phải sau ngày nhận");
      long seconds = Duration.between(start, end).getSeconds();
      if (seconds > 366L * 86400) 
          throw new IllegalArgumentException("Thời gian thuê tối đa 366 ngày");
      return Math.toIntExact((seconds + 86399) / 86400);
  }
  ```
- **Phân tích:** Mã nguồn hiện tại tính số ngày bằng cách làm tròn lên theo chu kỳ 24 giờ (`ceil(duration_in_seconds / 86400)`).
  - Ví dụ: Thuê từ 09:00 ngày 1 đến 18:00 ngày 1 (9 tiếng) -> được tính là 1 ngày (86400s ceiling).
  - Thuê từ 09:00 ngày 1 đến 08:59 ngày 2 (23 tiếng 59 phút) -> được tính là 1 ngày.
  - Thuê từ 09:00 ngày 1 đến 09:01 ngày 2 (24 tiếng 1 phút) -> được tính là 2 ngày.
- **Kết luận:** Quy tắc mốc 12h/20h chưa từng được triển khai trong source code. Mã nguồn đang chạy theo tiêu chuẩn tính tròn block 24h phổ biến trong ngành du lịch thuê xe.

---

## 5. Cổng Thanh Toán PayOS (PayOS Integration Rules)

### Kỳ vọng (Expected Baseline):
- Tạo payment link với số tiền cọc hợp lệ.
- Webhook xác thực chữ ký SHA-256 từ PayOS.
- Chống duplicate callback (Idempotency).
- Đối soát chủ động (Reconcile) khi webhook bị chậm.
- Không tin tưởng redirect URL của client.

### Thực tế trong mã nguồn (Actual Code):
- **Đối chiếu: HOÀN TOÀN KHỚP (100% MATCH).**
- File [PaymentService.java](file:///d:/Motorbike-rental/src/main/java/com/motorrent/motorbikerental/service/PaymentService.java):
  - `webhook(Webhook body)`: Gọi `payOS.verifyPaymentWebhookData(body)` để xác thực chữ ký số hợp lệ trước khi phân tích payload.
  - `apply(...)`: Xử lý giao dịch idempotent. Nếu giao dịch đã ở trạng thái `PAID`, các lần gọi lại duplicate sẽ bỏ qua không cập nhật hai lần.
  - Trường hợp thanh toán sai số tiền, thanh toán sau khi hết hạn giữ chỗ hoặc xe đã bị đơn khác đặt trước sẽ được ghi nhận trạng thái `REVIEW_REQUIRED` kèm ghi chú cụ thể để nhân viên can thiệp thủ công, tuyệt đối không tạo đơn ảo hoặc ghi đè lịch xe.
  - Redirect URL `/customer/payment-result` chỉ hiển thị thông tin hướng dẫn, không bao giờ dùng tham số URL để xác nhận trạng thái thanh toán.

---

## 6. Đổi Xe (Vehicle Change Rules)

### 6.1. Trước Khi Giao Xe (BEFORE_DELIVERY)
- **Quy tắc:** Khách hàng liên hệ nhân viên thỏa thuận. Nhân viên là người tạo đề xuất đổi xe trên hệ thống.
- **Hạch toán:** Tính lại toàn bộ tiền thuê xe mới từ đầu. Giữ nguyên mã đơn và lịch sử thanh toán cũ.
  - Xe mới đắt hơn: Yêu cầu nộp cọc bổ sung (`additionalPayment`), tạo link PayOS.
  - Xe mới rẻ hơn / bằng giá: Cập nhật xe mới ngay lập tức, tiền thừa ghi nhận là khoản ghi có (`creditAmount`) hoặc hoàn tiền trực tiếp (`BOOKING_ADJUSTMENT_REFUND`).

### 6.2. Trong Khi Thuê (DURING_RENTAL)
- **Quy tắc:** Nhân viên lập phương án đổi xe tại điểm hẹn với khách.
- **Hạch toán:** Giữ nguyên giá chặng đã sử dụng của xe cũ. Chặng tương lai được tính theo đơn giá xe mới.
- **Bàn giao vật lý:** Sau khi khách hàng thanh toán bổ sung (nếu có), xe cũ vẫn được giữ ở trạng thái `RENTED` cho đến khi nhân viên bấm "Xác nhận đã đổi xe", lúc đó xe cũ mới được trả về `AVAILABLE` và xe mới chuyển sang `RENTED`.

---

## 7. Gia Hạn, Trả Xe Sớm & Hoàn Tất Đơn (Extension, Early Return & Completion)

### 7.1. Gia Hạn (Rental Extension)
- Khách hàng yêu cầu gia hạn thời gian trả xe mới.
- Hệ thống kiểm tra xe có bị trùng lịch với đơn sau hay không.
- Nhân viên duyệt đơn -> Đơn chuyển sang `WAITING_FOR_PAYMENT` với thời hạn giữ chỗ.
- Khách hàng thanh toán phí gia hạn thành công -> Hệ thống tự động cập nhật ngày trả mới (`returnDate`) và trạng thái `EXTENDED`.

### 7.2. Trả Xe Sớm (Early Return)
- Khách hàng bấm yêu cầu trả sớm -> Trạng thái đơn đổi thành `RETURN_REQUESTED`.
- Xe chưa được tự động giải phóng, không tự động hoàn tiền để tránh gian lận.
- Nhân viên kiểm tra xe thực tế, thỏa thuận số tiền hoàn hợp lý (có thể bằng 0đ), ghi nhận lý do và phương thức hoàn tiền (`EARLY_RETURN_REFUND` chiều `OUT`).

### 7.3. Hoàn Tất Đơn (Final Completion)
- Nhân viên kiểm tra xe thực tế, nhập số km cuối (`odo_after`), hiện trạng xe (`vehicle_condition_after`).
- Hoàn cọc tài sản (`ASSET_DEPOSIT_REFUND`), khấu trừ chi phí sửa chữa hư hỏng (`DEPOSIT_DEDUCTION`), thu phí quá hạn (`LATE_FEE`) trong cùng một giao dịch nguyên tử (Atomic Transaction).
- Xe tự động được giải phóng về trạng thái `AVAILABLE`.
