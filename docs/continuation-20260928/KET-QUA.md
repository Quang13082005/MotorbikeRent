# CẬP NHẬT KẾ HOẠCH — Phase 1 ngày 28/09/2026

Người dùng đã duyệt bắt đầu kế hoạch trong DOI-CHIEU-VA-KE-HOACH.html. Đã hoàn thành Phase 1: nền tảng ghi nhận thu/chi, audit và availability; 50 test hồi quy và 3 test đồng thời trên MySQL đạt. Hai ứng dụng đã khởi động lại. Theo dõi kết quả mới tại ../phase1-20260928/KET-QUA.md; các mô tả bên dưới là lịch sử, không phải yêu cầu mới nhất. Luồng đổi xe, tiền khi giao/hoàn tất, PayOS gia hạn và nhận sớm thuộc các phase sau, chưa triển khai ở Phase 1.

---

# YÊU CẦU HIỆN HÀNH — Staff đơn giản, 28/09/2026

Nội dung này thay thế mọi mô tả checkbox/thu tiền bắt buộc/bước chuẩn bị giao ở bên dưới.
- Người dùng yêu cầu website chỉ ghi kết quả cuối cùng của công việc ngoài thực tế. Đã bỏ checkbox, endpoint checklist và bước prepare-handover; không thêm trạng thái mới.
- Đơn đã xác nhận → bấm Đã giao xe → HANDED_OVER/xe RENTED. Không có hộp thoại xác nhận bổ sung. Giữ quy định không giao trước giờ nhận đã duyệt trước đây.
- Đang thuê (kể cả gia hạn và trạng thái cũ được hỗ trợ) → bấm Hoàn tất đơn → COMPLETED/xe AVAILABLE; vào lịch sử và được đánh giá. Không bắt buộc biên bản, ghi thu tiền hoặc hoàn cọc.
- Hai thao tác không tự tạo Payment, không cập nhật depositPaid/balancePaid và không tự hoàn tiền. Số liệu giao dịch chỉ phản ánh khoản thực sự được ghi nhận trên website; không suy diễn tiền ngoài thực tế từ trạng thái hoàn thành.
- Giữ dấu vết riêng: rental_contracts.handover_staff_id/handed_over_at và completion_staff_id/completed_at; rental_orders.actual_return_date lưu thời điểm hoàn tất. Không ghi đè người giao khi người khác hoàn tất.
- Cột checkbox cũ còn trong DB để bảo toàn lịch sử, đã bỏ khỏi entity và không đọc/ghi trong luồng hiện tại. Trạng thái READY_FOR_HANDOVER chỉ tương thích đơn cũ, không có thao tác tạo mới trạng thái này.
- Sao lưu cả hai DB: database-before-simple-staff-*.sql trước khi thêm hai cột audit hoàn tất; không chuẩn hóa trạng thái hoặc thanh toán lịch sử.
- 37 kiểm thử đạt: tests-simple-staff.log. Gói build: package-simple-staff.log. Đã chạy giao diện staff trên clone: mỗi nút đúng một lần bấm, không modal/checkbox; DB xác nhận COMPLETED, đủ hai audit, balance=0 và paymentRecords=0 (simple-staff-mysql.log). Đã dọn đơn thử.
- Các công cụ thu chi/biên bản cũ vẫn ở khu vực thu chi, nhưng bỏ khỏi thẻ đơn của Staff/admin và hoàn toàn không phải bước bắt buộc.

---

# Tiến độ cập nhật ngày 28/09/2026

## Yêu cầu mới nhất đã áp dụng
- Người dùng thay thế đề xuất xe điện 0 cc bằng yêu cầu bỏ hoàn toàn xe điện.
- Người dùng thay thế điều kiện biên bản bắt buộc bằng hai xác nhận của nhân viên: đã kiểm tra và giữ đủ CCCD theo hợp đồng; đã nhận đủ tiền thuê còn lại và cọc tài sản qua ngân hàng cửa hàng.
- Vẫn giữ yêu cầu đã duyệt: không bàn giao trước giờ nhận xe. Chặn số giấy tờ trùng kể cả khác chữ hoa/thường.

## Đã hoàn thành trong lượt này
- Xóa tab xe điện ở danh sách xe. Mã nguồn đang sử dụng không còn tham chiếu loại xe điện / VinFast / Feliz / Theon. Backend vẫn yêu cầu xe có dung tích 40–2000 cc; không triển khai đề xuất xe điện 0 cc cũ.
- Sao lưu đầy đủ cả motorent_db và motorent_verify_20260926 vào database-before-handover-*.sql, kèm backup-sha256.txt.
- Xóa xe mẫu VinFast Feliz S (id 16), ảnh liên quan, danh mục xe điện (id 4) và hãng VinFast không còn sử dụng trên cả hai DB. Trước khi xóa xác minh không có đơn, đánh giá, bảo dưỡng liên kết. Còn 10 xe và 3 loại xe.
- Chuyển bản SQL cũ không còn khớp schema khỏi src/main/resources sang legacy-motorent-db-do-not-import.sql để tránh đưa dữ liệu mẫu đã bỏ vào bản build. File này và các backup chỉ phục vụ phục hồi/lịch sử; không import vào DB đang dùng.
- Admin và staff dùng cùng hộp thoại hai checkbox, có số tiền còn thiếu lấy từ DB. Backend bắt buộc cả hai giá trị true và đến giờ nhận xe theo Asia/Ho_Chi_Minh.
- Xác nhận bàn giao ghi các khoản còn thiếu thành BANK_TRANSFER/BALANCE và BANK_TRANSFER/SECURITY, cập nhật tiền thuê đã thu, trạng thái đơn và xe trong một transaction. Tiền đã thu trước đó được trừ; thao tác lặp bị chặn. Không gọi PayOS cho bước nhận tiền này.
- Lưu identity_checked, payment_received, handed_over_at, handover_staff_id trong rental_contracts. Các dòng lịch sử cũ để NULL; không giả lập xác nhận cho đơn cũ.
- Không bắt nhân viên nhập biên bản riêng. Chức năng biên bản tự nguyện và thu/hoàn tiền hiện có vẫn giữ.
- Nhãn giao dịch chuyển khoản hiển thị đúng cho customer và admin/staff.

## Xác minh
- 34 kiểm thử Java đạt, 0 lỗi: tests-handover.log. Bao gồm xác nhận thiếu checkbox, bàn giao sớm, cọc đã nhận một phần, chống thu trùng, hai giấy tờ trùng/khác nhau và các luồng cũ.
- Hai file JavaScript thay đổi qua kiểm tra cú pháp Node.
- Gói JAR thành công: package-handover.log; bản chính 8080 và bản kiểm thử 8082 đã khởi động với schema validate.
- Các trang/API admin, staff và phân quyền kiểm thử lại thành công: operations-after-handover.log.
- Báo giá 3 loại xe/2 phương án CCCD, ngày quá khứ, hồ sơ/lịch sử/đơn/hợp đồng và khóa thanh toán thật trên clone đều đạt: customer-after-handover.log.
- Kiểm thử trình duyệt bằng đơn giả lập UI-HANDOVER-20260928 trên clone: thiếu checkbox bị chặn, đủ checkbox chuyển sang Đang thuê. DB lưu 50.000 tiền thuê còn lại + 1.000.000 cọc, hai xác nhận, người và giờ bàn giao: handover-mysql.log. Đã dọn đơn, giao dịch, thông báo thử và trả xe về trạng thái khả dụng; không tạo giao dịch ngân hàng thật.
- Trình duyệt danh sách xe hiển thị 10 xe, chỉ có tab xe ga, xe số, xe côn tay.

## Tiến độ các lượt trước và giới hạn còn lại
Các sửa ngày giờ, bản đồ chỉ mở theo thao tác, lưu điểm giao/trả xuyên trang, mức cọc theo loại xe, UTF-8 hồ sơ/lịch sử, bỏ stations/promotions, bố cục xe và nhập địa điểm đã thực hiện trước đó; xem docs/customer-fixes-20260927/KET-QUA.md.
Dữ liệu địa điểm chính còn 32.584 dòng, 31.822 hiển thị, phạm vi Đà Nẵng mới gồm Quảng Nam cũ; có 2 ảnh đã xác minh nguồn. Chưa có ảnh thật cho toàn bộ địa điểm vì nguồn gốc không cung cấp URL ảnh.
PayOS đã kiểm tra tạo và hủy link chưa thanh toán ở lượt trước; chưa kiểm thử chuyển tiền thật và toàn bộ webhook ngân hàng. Không được ghi rằng dự án đã hoàn tất 100% hoặc đã nhận tiền ngân hàng thật.
Không thực hiện lại các kế hoạch cũ về khách không tài khoản trong HANDOFF cũ nếu người dùng chưa yêu cầu.

## Cách kiểm tra tiếp
Mở http://127.0.0.1:8080/ để kiểm tra bản chính; http://127.0.0.1:8082/ là bản kiểm thử tắt PayOS.
Trong staff → Đơn thuê xe, chuẩn bị đơn đã đến giờ nhận → Xác nhận giao xe → kiểm tra số tiền → tích hai checkbox sau khi kiểm tra CCCD và nhận tiền thực tế → Xác nhận bàn giao.
QR ngân hàng do cửa hàng cung cấp bên ngoài như yêu cầu; ứng dụng không tự xác minh giao dịch ở tài khoản ngân hàng riêng.
Khởi động lại bằng Start-MotoRent.ps1 (chính) hoặc Start-MotoRent.ps1 -Verify (clone) khi cổng chưa chạy.

## Tiếp tục kiểm tra chu trình trả xe (28/09/2026)
- Đã nối nút Hoàn tất đơn trên trang staff cho các trạng thái gia hạn/quá hạn/trả xe vốn được API hỗ trợ.
- Đã bổ sung Thu chi của đơn trong trang payments của staff/admin để mở phần hoàn cọc cho đơn đã hoàn tất hoặc hủy, không cần đơn còn trong hàng đợi.
- 37 kiểm thử đạt (tests-lifecycle.log), gồm chu trình PayOS giả lập → duyệt → bàn giao → gia hạn → thu bổ sung → hoàn tất → hoàn cọc → đánh giá → đối chiếu báo cáo doanh thu. Kiểm tra chặn hoàn trùng và phân quyền customer.
- Đã build thành công package-lifecycle.log. Trình duyệt đã xác minh nút Thu chi của đơn mở được lựa chọn Hoàn bảo đảm tài sản cho đơn đã hoàn tất. Lần bấm ghi nhận trên UI bị Failed to fetch sau khi tiến trình localhost dừng qua lượt mới; không ghi nhận là UI hoàn tiền thành công. API hoàn cọc được kiểm thử tích hợp thành công. Đơn thử trên clone đã được dọn.
- ReadinessCheck.java chỉ đọc: điều khoản trong DB chính đang trống; cấu hình SMTP/OCR/PayOS có giá trị nhưng chưa chứng minh hoạt động thực tế; app.base-url hiện là localhost. Cần chủ cửa hàng nhập điều khoản thực tế và phối hợp kiểm thử dịch vụ thật. Không gửi email hoặc chuyển tiền thật trong lượt này.
