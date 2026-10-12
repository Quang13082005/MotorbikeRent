# Giao diện Staff & Admin

## Phong cách

Thiết kế dựa trên customer hiện tại: nền sáng, thẻ trắng, Plus Jakarta Sans,
điểm nhấn cam/vàng, bo góc mềm. Hai role có sidebar, điều hướng mobile và CSS
riêng trong `operations.css`, được giới hạn bằng `.ops-app`.

Giữ nền sáng cho công việc nội bộ; các trang customer nền tối giữ nguyên.
Nhận diện chung đến từ logo, font chữ và tông cam/vàng. Footer dùng chung
`fragments/operations-footer.html` cho toàn bộ hai role: nhận diện MotoRent,
bản quyền, nhãn không gian làm việc, liên kết tài khoản (staff) / cài đặt
(admin) và về đầu trang. Footer gọn một hàng trên desktop, xếp dọc trên
điện thoại; không chứa quảng bá hoặc cam kết dành cho khách hàng.

## Staff: kết nối chức năng hiện có

- `/staff/dashboard`: số liệu thật và các đơn chờ duyệt từ `RentalOrderService`.
- `/staff/rental-orders`: tìm tên/mã đơn/điện thoại/xe, lọc trạng thái, chi tiết,
  duyệt, từ chối có lý do, chuẩn bị giao, giao xe và hoàn tất đơn.
- `/staff/handover`: lọc đơn CONFIRMED / READY_FOR_HANDOVER, dùng lại API staff.
- `/staff/return`: lọc đơn đang thuê và các trạng thái trả xe. Chỉ hiển thị nút
  hoàn tất cho HANDED_OVER, đúng điều kiện mà service hiện tại chấp nhận.
- `/staff/profile`: thông tin phiên đăng nhập, tải lại và đăng xuất bằng API cũ.
- `/staff/maintenance`: giao diện mẫu, chưa ghi dữ liệu bảo dưỡng vào backend.

Checklist giao/nhận chỉ nhắc việc trong trang; không được xem là biên bản đã lưu.
Không tự động thay dữ liệu thật bằng dữ liệu giả khi danh sách staff trống.

## Admin: bản mẫu frontend

Các trang tổng quan, đơn thuê, đội xe, bảo dưỡng, khách hàng, nhân viên, trạm xe,
khuyến mãi, báo cáo và cài đặt dùng dữ liệu minh họa, có nhãn rõ ràng.

Danh sách hỗ trợ tìm kiếm không dấu, lọc trạng thái, xem/sửa/thêm bản mẫu,
kiểm tra trường bắt buộc, mã trùng, khoảng ngày và xuất CSV theo kết quả lọc.
Thay đổi chỉ nằm trong bộ nhớ trang và mất khi tải lại. Cài đặt chưa áp dụng
cho hệ thống. Tổng quan/báo cáo là bộ dữ liệu minh họa riêng, không đồng bộ với
các chỉnh sửa mẫu trên trang danh sách.

## Phạm vi bảo toàn

Không sửa template customer/auth, CSS/JS chung, API, service, repository,
entity, security, cấu hình thanh toán hoặc cấu hình cơ sở dữ liệu.
Controller staff chỉ bổ sung dữ liệu cho hai màn hình giao/nhận trước đây trống;
controller admin bổ sung các route còn thiếu.

Ngoại lệ nhỏ cần thiết để ứng dụng khởi động: `CustomerPageController` có hai
mapping GET `/customer/payment-result` trùng nhau từ trước. Đã gộp thành một
method và giữ cả `orderId`, `cancel`, `status`; không sửa template hay xử lý
thanh toán. `CustomerPaymentResultRouteTests` bảo vệ tính duy nhất của route
và dữ liệu model này.

Không nới quyền truy cập: STAFF/ADMIN tiếp tục dùng cấu hình security có sẵn.

## Kiểm tra

- `OperationsPageControllerTests`: kiểm tra các hàng đợi staff không lẫn đơn
  đã hoàn tất/hủy, bảo toàn các trạng thái ngoại lệ và số liệu tổng quan.
- `OperationsWorkflowTests`: render 16 trang với Thymeleaf và kiểm tra API
  duyệt → chuẩn bị giao → giao → hoàn tất, cùng thao tác từ chối có lý do.
  Test dùng H2 riêng và transaction rollback, không dùng MySQL của ứng dụng.
- Chạy: `mvn -Dtest=OperationsPageControllerTests,CustomerPaymentResultRouteTests,OperationsWorkflowTests test`.
- Kết quả: 8 tests đạt. HTTP smoke test: 16 trang staff/admin và 5 URL
  customer/auth trả về 200. Template customer/auth và backend nghiệp vụ
  không thay đổi so với bản trước khi thiết kế.
- Đã kiểm tra trình duyệt: tìm không dấu, bộ lọc rỗng, sửa/thêm bản mẫu,
  chặn mã trùng, khôi phục dữ liệu mẫu khi tải lại; menu mobile và footer
  tại 390px/1440px không tràn ngang.
- Kiểm tra cú pháp: `node --check src/main/resources/static/js/operations.js`.
- Xem giao diện bằng các route `/staff/dashboard` và `/admin/dashboard` sau
  khi đăng nhập đúng role. Profile `preview` hiện có của dự án chỉ dùng cục bộ.
