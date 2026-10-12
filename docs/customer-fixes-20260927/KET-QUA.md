# Kết quả sửa luồng customer — 27/09/2026

## Đã áp dụng

- Chặn ngày nhận trong quá khứ và ngày trả không sau ngày nhận ở giao diện và máy chủ. Mốc giờ dùng Asia/Ho_Chi_Minh.
- Tìm kiếm/lọc xe không tự mở bản đồ; bản đồ mở khi người dùng chọn điểm giao nhận.
- Giữ địa chỉ và tọa độ từ landing page sang danh sách xe, chi tiết xe và hợp đồng. Điểm trả ban đầu theo điểm giao; sau khi thay đổi riêng thì được giữ riêng, kể cả tải lại trang.
- Mức cọc tài sản lưu trong motorbike_categories.security_deposit: xe số 1.000.000, xe ga 2.000.000, xe côn tay 1.500.000 đồng. Một CCCD áp dụng mức này, hai CCCD bằng 0. Khoản giữ xe và phí giao nhận vẫn tính theo shop_settings; cọc tài sản hiển thị riêng để thu khi nhận xe.
- Mã giao dịch PayOS mới không phụ thuộc ID tuần tự của database, giảm va chạm giữa các bản sao. Giữ giao dịch chờ khi gặp lỗi để thử lại; không tự đánh dấu đã trả tiền. Giao diện hiển thị thông báo lỗi máy chủ.
- Hợp đồng lấy đại diện, hotline, địa chỉ duy nhất, giờ mở cửa và điều khoản từ shop_settings. Bỏ thông tin cơ sở thứ hai và tuyên bố chữ ký số chưa có cơ sở xác nhận; bỏ nội dung quảng bá tích điểm/ưu đãi còn sót trên các trang tài khoản.

## Database và bản sao lưu

- Bản sao lưu trước thay đổi: `../implementation-20260925/database-before-customer-fixes-20260927.sql`, kèm SHA-256. Đây là dữ liệu riêng tư, không đưa lên kho công khai.
- Đã áp dụng trên `motorent_verify_20260926` trước, rồi `motorent_db`.
- Phạm vi theo xác nhận của chủ dự án: Đà Nẵng mới, gồm Quảng Nam cũ.
- Xóa 156 bản ghi có địa chỉ nguồn thuộc tỉnh khác; tạm ẩn 30 bản ghi không đủ địa chỉ rõ ràng. Việc lọc dựa trên địa chỉ nguồn, chưa phải đối chiếu ranh giới GIS.
- Database chính còn 32.584 địa điểm; 31.822 công khai. Các bản ghi vốn ẩn vẫn được giữ ẩn. Không sửa số tiền hoặc trạng thái của đơn thuê/thanh toán lịch sử.
- Bổ sung ảnh có nguồn và giấy phép cho Chùa Cầu và Sun World Bà Nà Hills. File nguồn không chứa URL ảnh nên chưa có ảnh cho toàn bộ danh mục. Admin có thể thêm URL, tác giả, nguồn và giấy phép trong trang địa điểm.
- Tài khoản `customer_ui_verify@example.test` chỉ được tạo trên bản sao để kiểm thử, do customer_demo đang bị khóa. Không mở khóa tài khoản cũ hoặc tạo tài khoản này trên database chính.

## Bằng chứng kiểm tra

- `tests.log`: 29 kiểm thử tự động đạt; bao gồm bảo vệ dữ liệu người dùng, thanh toán, giá thuê và quy trình admin/staff.
- `mysql-customer-check.log`: sáu tổ hợp cọc trên MySQL đạt; API từ chối ngày quá khứ; hồ sơ, lịch sử, đơn thuê và hợp đồng render thành công.
- Kiểm tra trình duyệt: ngày 14/09 bị chặn; tìm kiếm và lọc xe ga không bật bản đồ; Cầu Rồng Hotel được giữ xuyên các trang; cọc xe ga đổi 2 triệu → 0 → 2 triệu; đổi riêng điểm trả về cửa hàng rồi tải lại vẫn giữ và phí trả bằng 0.
- Ảnh Chùa Cầu hiển thị thực tế, kèm tác giả và liên kết giấy phép.
- `payos-readonly.log`: PayOS thật trả “Mã thanh toán không tồn tại” cho mã kiểm thử chỉ đọc, thay vì lỗi xác thực.
- `payos-link-check.log`: SDK với cấu hình chính tạo link thử thành công, hủy ngay thành công, `AMOUNT_PAID=0`. Không chuyển tiền và không tạo đơn thuê khách hàng để thực hiện phép thử này.

## Cách mở và phần còn cần xác nhận

- Ứng dụng chính: http://127.0.0.1:8080/customer/home — database chính, cấu hình PayOS chính.
- Bản sao: http://127.0.0.1:8082/customer/home — chỉ kiểm thử, chủ động tắt thanh toán thật. Thông báo 503 ở bản sao là có chủ đích; không dùng bản này để kiểm tra chuyển tiền.
- Mục điều khoản của shop_settings đang trống ở bản sao kiểm thử. Trang hợp đồng thông báo rõ, không tự đặt nội dung pháp lý mới. Chủ cửa hàng cần kiểm tra và nhập điều khoản chính thức ở Admin → Cài đặt cửa hàng trước khi vận hành.
- Chưa thực hiện chuyển tiền thật, callback/webhook qua mạng công khai hoặc hoàn tiền ngân hàng; việc tạo và hủy link thành công không thay thế kiểm tra toàn bộ luồng tiền thật.
- Chưa chứng nhận toàn bộ dự án hoàn tất hoặc toàn bộ trang admin/staff đã được kiểm tra bằng tay. Các quy trình chung có kiểm thử tự động; OCR/email thực tế và chất lượng toàn bộ danh mục địa điểm cần xác minh tiếp.
- Build kiểm chứng dùng `.verification-pom.xml` (cùng dependency/source, chỉ đổi thư mục đầu ra sang target/verified) để tránh xung đột class với IDE đang biên dịch target/classes.

Nguồn ảnh: https://commons.wikimedia.org/wiki/File:2024-12-20_Japanese_Bridge_in_Hoi_An.jpg và https://commons.wikimedia.org/wiki/File:Golden_Bridge_at_Ba_Na_Hills_20250718.jpg.
Tài liệu PayOS đối chiếu: https://payos.vn/docs/api/.
