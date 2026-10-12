# Cập nhật theo yêu cầu ngày 27/09/2026

## Đã thực hiện

- Bỏ bảng stations và cột/khóa ngoại motorbikes.station_id. Địa chỉ cửa hàng duy nhất tiếp tục dùng shop_settings. Không xóa xe hoặc đơn.
- Bỏ bảng promotions, entity/repository/mapper, chức năng quản trị, mục menu và mã khuyến mãi trên form hợp đồng. Các trường số tiền giảm/mã cũ trong rental_orders chỉ là dấu vết lịch sử; đơn mới không áp dụng giảm giá.
- Đối chiếu hai file Downloads/places_VN_Đà Nẵng.json: giống nhau hoàn toàn, mỗi file 32.740 mã địa điểm riêng. 28 bản ghi trước đã có external_id trùng nguồn; nhập thêm 32.712. Database chính có đúng 32.740 địa điểm, 31.978 bản ghi công bố. Các bản ghi đóng cửa, chưa rõ trạng thái hoặc thiếu thông tin và lựa chọn ẩn trước đó không được tự công bố.
- Tìm kiếm có/không dấu và phân trang tại database: khách 24 địa điểm/trang, admin 50/trang, bản đồ tối đa 12 gợi ý. Xuất CSV ở admin áp dụng trang đang xem, nút đã ghi rõ.
- Khôi phục bố cục tìm kiếm gọn: lịch nhận/trả + điểm giao + nút kiểm tra; tab loại xe và hàng bộ lọc bên dưới. Giữ bộ lọc thật, lịch trống, bản đồ và thông tin giao nhận.
- Trang chủ có tab Xe ga / Xe số / Xe côn tay, mỗi loại tối đa 6 xe theo đánh giá lưu trong database. Màn hình rộng hiển thị 3 cột, tablet 2 cột, điện thoại nhỏ 1 cột. Dữ liệu hiện có 3 xe ga, 3 xe số, 4 xe côn tay; không nhân bản card cho đủ 6.
- Bỏ mục Tìm xe theo nhu cầu; liên kết menu chuyển đến mục xe đề cử. Sửa màu chữ trên nền sáng của trang chủ.
- Sửa bộ lọc để hiểu cả tên loại tiếng Việt và mã URL, ví dụ Xe tay ga / xe-ga. Escape tên địa điểm trước khi đưa vào gợi ý bản đồ.

## Kiểm tra

- 26 bài kiểm thử đạt: tests.log. Có các trường hợp phân trang, tìm không dấu, ký tự wildcard, giới hạn 6 xe, đúng loại tiếng Việt, trang khuyến mãi không còn truy cập và hợp đồng không còn ô mã khuyến mãi.
- MySQL clone: CRUD địa điểm, công bố/ẩn, tab xe và bộ lọc trên trình duyệt hoạt động. Bản ghi kiểm thử được ẩn lại.
- Database chính: giữ 11 xe, 2 đơn và 2 khoản thu; nhập địa điểm thành công. migration-original.log ghi kết quả.
- Ứng dụng chính cổng 8080 đã khởi động với ddl-auto=validate, không tự tạo lại hai bảng đã bỏ. HTTP các trang chủ/tab/danh sách/địa điểm trả 200.

## Sao lưu và vận hành

- Sao lưu trước thay đổi: ../implementation-20260925/database-before-stations-promotions-20260927.sql và SHA256 đi kèm. Chứa dữ liệu tài khoản, chỉ giữ nội bộ.
- RefactorDatabase.java chỉ nhận database chính hoặc clone đã chỉ định; kiểm tra phụ thuộc trước khi ghi. Phần DDL của MySQL không rollback bằng transaction; dùng bản sao lưu nếu cần phục hồi schema. Import địa điểm dùng transaction và external_id để tránh trùng khi chạy lại.
- Mã đã bỏ được giữ ở removed/ để đối chiếu, không còn trong src/main/java hay class chạy ứng dụng.
- Xem kết quả thật tại http://localhost:8080/. Cổng 8082 là bản sao kiểm thử dùng khóa thanh toán/email giả.
- File SQL cũ src/main/resources/motorent_db.sql là bản dump lịch sử, không phải migration cho phiên bản mới; không nhập đè lên database hiện tại.

## Giới hạn còn lại của dự án

Đợt yêu cầu giao diện/database này đã triển khai. Chưa xác nhận toàn dự án sẵn sàng vận hành thật: PayOS/webhook qua mạng, email và OCR vẫn cần kiểm thử tích hợp thực tế. Chưa phát sinh chuyển tiền thật hoặc gửi email ngoài hệ thống trong đợt này. Các ảnh xe placeholder cần được thay bằng ảnh xe thực của cửa hàng khi có dữ liệu.
