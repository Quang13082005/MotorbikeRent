# 10 - ĐÁNH GIÁ TRẢI NGHIỆM NGƯỜI DÙNG & SẢN PHẨM (UX & PRODUCT REVIEW)

Dưới góc nhìn của **Senior Technical Product Manager** và **UI/UX Architect**, tài liệu này phân tích hành trình người dùng (User Journey), những điểm nghẽn thao tác và đề xuất các giải pháp nâng tầm sản phẩm MotoRent.

---

## 1. Trải Nghiệm Khách Hàng (Customer UX)

### 1.1. Điểm Tích Cực (Positive Highlights)
- **Giao diện hiện đại & Thân thiện:** Thiết kế gam màu xanh ngọc / than chì tạo cảm giác chuyên nghiệp, năng động cho dịch vụ du lịch Đà Nẵng.
- **Minh bạch tài chính:** Báo giá chi tiết ngay từ màn hình chọn xe, hiển thị rõ số tiền thuê, phí giao nhận theo km, tiền cọc giữ xe (50%), và cọc tài sản theo từng phương án bảo lãnh CCCD.
- **Thanh toán VietQR nhanh chóng:** Tích hợp PayOS cho phép quét mã QR thanh toán trên ứng dụng ngân hàng mà không cần nhập số thẻ thủ công.

### 1.2. Điểm Nghẽn Thao Tác (Pain Points & Friction)
1. **Quá trình điền thông tin hợp đồng còn dài:**
   - Khách phải nhập họ tên, số CCCD, ngày sinh, địa chỉ. Mặc dù có tính năng "Quét ảnh CCCD" bằng AI, tính năng này đang bị lỗi kỹ thuật (MBR-001) khiến khách phải gõ tay 100%.
2. **Thời gian giữ chỗ thanh toán 3 phút (`holdMinutes=3`):**
   - 3 phút có thể là hơi ngắn đối với khách du lịch khi mạng 4G chập chờn hoặc mở ứng dụng ngân hàng chậm.
   - **Đề xuất:** Tăng thời gian giữ chỗ lên **5 - 10 phút** để giảm tỷ lệ đơn bị hủy oan uổng (`CANCELLED`).
3. **Thiếu tính năng lưu lịch sử vị trí thường dùng:**
   - Khách thường nhận xe tại sân bay hoặc khách sạn. Việc phải tìm lại tọa độ trên bản đồ mỗi lần đặt xe mới làm tốn thao tác.

---

## 2. Trải Nghiệm Nhân Viên Vận Hành (Staff UX)

### 2.1. Điểm Tích Cực (Positive Highlights)
- **Giao diện điều hành tập trung (All-in-One Dashboard):** Nhân viên có thể quan sát nhanh số lượng đơn chờ duyệt, đơn đang thuê, đơn cần bảo dưỡng xe.
- **Biểu mẫu tác vụ gọn gàng (Lean Action Dialogs):** Các tác vụ bàn giao, đổi xe, hoàn tất đơn đều được đóng gói trong một pop-up/dialog nhỏ gọn, prefill sẵn số tiền chuẩn, nhân viên chỉ cần kiểm tra đối chiếu thực tế và bấm nút.

### 2.2. Điểm Nghẽn Thao Tác (Pain Points)
1. **Chưa có tính năng chụp ảnh kiểm tra xe (Inspection Checklist with Camera):**
   - Hiện nhân viên chỉ nhập ghi chú text (`odo_before`, `vehicle_condition_before`).
   - Trong thực tế kinh doanh cho thuê xe máy, tranh chấp phổ biến nhất là vết xước xe có từ trước hay sau chuyến đi.
   - **Đề xuất:** Bổ sung tính năng cho phép nhân viên chụp 4 góc xe (trước, sau, trái, phải) và công tơ mét bằng camera điện thoại khi giao/nhận xe và lưu trực tiếp vào hợp đồng.
2. **Khó xử lý các đơn thanh toán trễ (`REVIEW_REQUIRED`):**
   - Khi có đơn thanh toán trễ, hệ thống chỉ gắn cờ `REVIEW_REQUIRED`. Nhân viên chưa có nút "Hoàn tiền cho khách" hoặc "Gán tiền này sang xe khác" trực tiếp trên màn hình điều hành.

---

## 3. Trải Nghiệm Quản Trị Viên (Admin UX)

### 3.1. Điểm Tích Cực
- Báo cáo dòng tiền tách bạch rõ ràng giữa Doanh thu thuần thuê xe (`rentalNetRevenue`) và Quỹ cọc tài sản giữ hộ (`totalAssetDepositHeld`), bảo vệ chủ doanh nghiệp khỏi việc ngộ nhận tiền cọc là lợi nhuận.
- 12 phân hệ quản trị bao phủ toàn diện hoạt động của cửa hàng.

### 3.2. Điểm Cần Cải Thiện
- Báo cáo tài chính hiện tại chủ yếu là bảng số liệu và thẻ tóm tắt.
- **Đề xuất:** Bổ sung biểu đồ trực quan (Biểu đồ cột doanh thu theo ngày, Biểu đồ tròn tỷ trọng thanh toán PayOS / Tiền mặt, Biểu đồ đường tần suất thuê xe theo thương hiệu).

---

## 4. Đề Xuất Tối Ưu Hóa Theo Mức Độ Ưu Tiên (Prioritized Recommendations)

### Nhóm MUST FIX (Cần làm ngay):
1. **Sửa lỗi quét CCCD (MBR-001):** Thêm header CSRF vào fetch request trên trang hợp đồng.
2. **Chuyển dịch Secrets:** Đưa toàn bộ mật khẩu và API keys vào biến môi trường hệ thống.
3. **Tăng thời hạn thanh toán giữ chỗ:** Nâng cấu hình `holdMinutes` từ 3 phút lên 10 phút.

### Nhóm SHOULD IMPROVE (Nên làm sớm):
1. **Thêm module Chụp ảnh xe bàn giao (Vehicle Photo Inspection):** Tải lên 4 ảnh hiện trạng khi giao xe và nhận xe.
2. **Tối ưu hiển thị bảng trên di động:** Thêm card-view cho bảng lịch sử đơn trên màn hình nhỏ.
3. **Thêm biểu đồ Chart.js cho Admin Reports:** Giúp chủ cửa hàng nắm bắt xu hướng kinh doanh trực quan.

### Nhóm NICE TO HAVE / FUTURE PHASE (Giai đoạn tiếp theo):
1. **Triển khai PWA (Progressive Web App):** Cho phép cài đặt ứng dụng vào màn hình chính của điện thoại và nạp trang nhanh khi mạng yếu.
2. **AI Trợ Lý Hành Trình Du Lịch Đà Nẵng:** Tận dụng 31,992 địa điểm du lịch có sẵn trong database kết hợp với Spring AI để gợi ý lịch trình phượt xe máy cho du khách.
3. **Gửi tin nhắn Zalo ZNS / SMS:** Tự động gửi thông báo xác nhận đơn và mã đơn qua tin nhắn Zalo khi khách hoàn tất thanh toán.
