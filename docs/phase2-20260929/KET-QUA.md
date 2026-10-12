# Kết quả Phase 2 — đổi xe trước bàn giao và hoàn tiền

Ngày 29/09/2026. Triển khai theo kế hoạch đã duyệt; đây không phải tuyên bố hoàn thành toàn dự án.

## Chức năng

- Staff/Admin có nút **Đổi xe / hoàn tiền** cho đơn chưa giao. Danh sách thay thế kiểm tra toàn khoảng dự kiến, hiển thị ảnh/loại/biển số/giá/tổng/cọc và chênh lệch.
- Đổi cùng giá hoặc rẻ hơn được tính lại toàn kỳ theo xe mới; giữ mã đơn và payment booking gốc. Số còn lại không âm; phần dư được hiển thị riêng.
- Xe đắt hơn/phí thay đổi: tạo đề nghị giữ xe có hạn; Customer xem và thanh toán phần booking còn thiếu. Chỉ xác nhận PayOS đúng tiền/đề nghị/lịch còn hiệu lực mới đổi xe và cập nhật số đã trả.
- Callback lặp không nhân đôi tiền; sai/trễ/hủy/xung đột chuyển tiền nhận sang REVIEW_REQUIRED. Đề nghị hết hạn/hủy hoặc đơn đóng không giữ xe tiếp.
- Staff nhập khoản đã hoàn ngoài thực tế; ghi BOOKING_REFUND hoặc BOOKING_ADJUSTMENT_REFUND theo chiều OUT, phương thức, lý do, mã biên nhận, nhân viên và thời điểm. Hoàn toàn bộ và REJECTED trong cùng transaction.
- Lịch sử lưu xe/giá/phí trước và sau, số đã trả, khoản bổ sung, tiền dư/thực hoàn, người đề nghị/hủy/hoàn và thời gian. Không thêm trạng thái đơn thuê trung gian hoặc checkbox giấy tờ.
- Bản đồ địa điểm nhận/trả mở bằng nút; server tính lại phí từng chặng. Hộp thoại bản đồ trên trang vận hành được bổ sung cuộn cho cửa sổ thấp.

## Mã nguồn / API

Danh sách 21 file nguồn thay đổi: [changed-files.txt](changed-files.txt). Các file chính: RentalVehicleChange, repository, VehicleChangeService, VehicleChangeController, VehicleChangeRequest, AvailabilityService, PaymentService, RentalOrderService, vehicle-changes.js và templates Staff/Admin/Customer.

API mới:
- GET /api/operations/orders/{id}/replacement-options
- POST /api/operations/orders/{id}/vehicle-changes/quote
- POST /api/operations/orders/{id}/vehicle-changes
- GET /api/rental-orders/{id}/vehicle-changes
- DELETE /api/operations/vehicle-changes/{id}
- POST /api/payment/vehicle-change/{id}
- POST /api/operations/orders/{id}/reject-with-refund
- POST /api/operations/orders/{id}/adjustment-refund

## Dữ liệu và bản chạy

V002__before_delivery_change.sql thêm bảng lịch sử/đề nghị, liên kết các bảng hiện có. Đã chạy trên clone trước, sau đó main. Hai schema vẫn giữ hai đơn và hai payment gốc, tổng amount/received 110.000 đồng. Dữ liệu UI thử được đánh dấu riêng và dọn sau kiểm tra; xem checkpoint mới nhất để biết trạng thái dọn cuối cùng.

Sao lưu trước sửa: source-before-phase2.zip, before-motorent_db.sql, before-motorent_verify_20260926.sql. SHA-256 ở backup-sha256.txt. Không nhập backup trực tiếp lên schema đang có dữ liệu; hướng dẫn phục hồi ở ../gemini-handoff/VERIFICATION.md.

Bản chính http://127.0.0.1:8080/ và clone http://127.0.0.1:8082/ đã khởi động với JAR Phase 2 và schema validate; log start-main.log/start-clone.log. Clone tắt thanh toán thật.

## Kiểm thử thực hiện

- package-tests.log: **70 tests, 0 failures, 0 errors**, build/package thành công.
- tests-mysql.log: **6 tests đồng thời trên MySQL clone**, đạt. Bao gồm hai đơn tranh một xe thay thế, callback đổi xe lặp, hai Staff cùng hoàn tiền và ba race của nền tảng cũ.
- operations-http.log: **36 kiểm tra HTTP/API** trang vận hành và phân quyền, đạt.
- customer-http.log: **12 kiểm tra** giá cọc/giấy tờ/ngày quá khứ/trang khách/PayOS disabled, đạt.
- phase2-http.log: quote/đề nghị, không đổi trước trả tiền, Customer đọc lịch sử và render chi tiết/payment-result, chặn Customer thao tác Staff, Admin đọc phương án, hủy có audit, đổi cùng giá giữ payment, hoàn/từ chối — đạt trên fixture clone.
- node --check cho vehicle-changes.js và operations-live.js: đạt. Sau lần 70 test, chỉ bổ sung CSS/JS hiển thị; package-final-assets.log đóng gói thành công với skipTests, không tính là một lần chạy lại test.
- Browser đã quan sát form Staff, danh sách xe thực, giá 300.000/cọc 2.000.000/PayOS bổ sung 50.000 và bản đồ mở theo nút. Không đánh đồng các kiểm tra HTTP với bấm toàn bộ luồng trên trình duyệt. Chi tiết quan sát cuối cùng ở browser-verification.md.

Hai lỗi được test đồng thời phát hiện và sửa: callback lặp sau đổi xe gặp liên kết xe cũ; hai thao tác hoàn tiền đọc snapshot MySQL cũ. VehicleChangeService dùng khóa cố định và READ_COMMITTED cho giao dịch ghi, kiểm chứng lại bằng test MySQL.

## Giới hạn / tiếp theo

Chưa thực hiện giao dịch ngân hàng thật hoặc kiểm chứng toàn bộ đường callback mạng của tài khoản PayOS thật. API tạo link dùng SDK hiện có; clone trả 503 theo thiết kế, kiểm thử service mô phỏng sự kiện thanh toán hợp lệ/sai/trễ. Cần kiểm thử tích hợp provider trong môi trường được phép trước đưa vào sử dụng thực tế.

Phase 3–9 chưa hoàn tất: ghi thu tiền tại bàn giao/nhận sớm; gia hạn PayOS; đổi xe khi đang thuê; trả sớm; quyết toán cọc/khấu trừ/late fee; báo cáo net revenue và nghiệm thu đầy đủ 65 case. Không suy ra chúng đã có chỉ vì mã loại giao dịch được khai báo.

Bộ bàn giao Gemini: [README](../gemini-handoff/README.md), [checkpoint](../gemini-handoff/CURRENT_TASK.md). Các file AGENTS.md/GEMINI.md/PROJECT_CONTEXT.md và tài liệu nghiệp vụ/prompt/runbook đã được tạo theo yêu cầu mới.
