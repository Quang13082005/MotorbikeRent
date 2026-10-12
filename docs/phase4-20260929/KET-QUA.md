# Phase 4 — Gia hạn và thanh toán PayOS

Đã triển khai và kiểm chứng cục bộ Phase 4 ngày 29/09/2026. Toàn bộ dự án chưa hoàn tất; chưa triển khai Phase 5.

## Luồng hiện tại

1. Customer gửi ngày trả mới và lý do. Hệ thống kiểm tra thời gian, quyền sở hữu và lịch xe.
2. Staff duyệt: yêu cầu chuyển sang **Chờ thanh toán**, giữ chỗ phần gia hạn trong thời hạn cấu hình. Ngày trả, tổng tiền trên đơn chưa đổi.
3. Customer bấm **Thanh toán phí gia hạn**. Hệ thống tạo giao dịch PayOS cho toàn bộ phí đã duyệt; thử lại dùng cùng giao dịch đã lưu.
4. Chỉ xác nhận thanh toán hợp lệ từ máy chủ mới ghi **EXTENSION_PAYMENT / IN**, tăng ngày trả, tổng tiền thuê và tiền đã trả đúng một lần.
5. Tiền sai, đến muộn hoặc yêu cầu đã mất hiệu lực được ghi **Cần đối soát**, không tự gia hạn. Giữ chỗ hết hiệu lực không chặn đơn mới. Khi không đủ lịch, thông báo có hotline lấy từ cấu hình cửa hàng.

Không thêm checkbox công việc thực tế hoặc trạng thái Rental trung gian. Cọc tài sản không bị cộng thành phí gia hạn. Giữ nguyên đơn và thanh toán cũ; kiểm tra lại lịch dưới khóa trước khi ghi dữ liệu.

## Thay đổi

15 file source/test, danh sách tại [changed-files.txt](changed-files.txt), bản so sánh tại [source.diff](source.diff). Các phần chính: ExtensionService, AvailabilityService, PaymentService, RentalExtension/Repository, controller, giao diện Customer/Staff/Admin và test.

- API mới: POST `/api/payment/extension/{id}`; GET `/api/rental-orders/{id}/extensions`.
- API duyệt cũ giữ trường `message`, thêm trạng thái và thông tin yêu cầu.
- Migration [V004__extension_payment.sql](V004__extension_payment.sql) thêm 6 cột theo dõi/snapshot, không xóa lịch sử. Đã áp dụng trên cả main và clone sau khi sao lưu riêng.
- Sửa lỗi phát hiện khi thử trình duyệt: `datetime-local` phải cho phép nhập giây khi giá trị `min` có giây; trước đó một số thời gian hợp lệ bị browser chặn.
- Sửa thứ tự lịch sử khi hai yêu cầu có cùng giây tạo: dùng ID giảm dần làm điều kiện sắp xếp bổ sung.

## Kiểm thử đã chạy

- **82/82 automated tests đạt**, không lỗi hoặc bỏ qua: [package-final.log](package-final.log). Bước đóng gói sau test bị khóa JAR, nên log này kết thúc BUILD FAILURE dù test đạt.
- Sau khi xác định đúng tiến trình con cổng 8083, đóng gói lại thành công: [package-retry.log](package-retry.log). Lần này bỏ qua chạy lại test; không có thay đổi source giữa hai lần.
- **15/15 ca MySQL đạt** trên clone: [tests-mysql-final.log](tests-mysql-final.log). Bao gồm callback đồng thời/trùng, hai yêu cầu hoặc lần tạo payment đồng thời, duyệt gia hạn cạnh tranh với đặt xe mới, hết hạn, sai tiền/link, lịch thay đổi, đơn hoàn thành/đổi xe, quyền và ngày sai; giữ 7 ca hồi quy trước.
- **12/12 HTTP Customer**: [customer-http.log](customer-http.log).
- **36/36 HTTP Staff/Admin**: [operations-http.log](operations-http.log).
- Kiểm tra cú pháp hai file JavaScript và UTF-8 của source đã sửa đạt.
- Thử trình duyệt: gửi yêu cầu, Staff duyệt, Customer thấy hạn/nút thanh toán, ngày trả và tổng tiền giữ nguyên, click nhận đúng thông báo clone tắt PayOS. Chi tiết tại [browser-verification.md](browser-verification.md).

Các ca 24–30 đã được đối chiếu với bằng chứng trong [ACCEPTANCE_CASES.md](../gemini-handoff/ACCEPTANCE_CASES.md); tổng số test không đồng nghĩa cả 65 ca nghiệm thu đã đạt.

## Database và server sau kiểm tra

Fixture Phase 4 đã dọn, các phiên test đã đăng xuất. Cả hai database đều trở về **2 đơn, 2 thanh toán; tổng amount và received_amount 110.000 đồng**: [baseline-final.log](baseline-final.log).

- **8083**: bản Phase 4 trên clone, PayOS thật tắt; PID listener 34472, launcher 31832 ở lần kiểm tra cuối. Mở [bản kiểm thử Phase 4](http://127.0.0.1:8083/).
- **8082**: vẫn giữ nguyên server Phase 3 PID 12416 theo yêu cầu; không dùng cổng này để kết luận chức năng Phase 4.
- **8080**: không có listener ở lần kiểm tra cuối. Schema chính đã nâng cấp nhưng chưa chuyển runtime chính sang JAR Phase 4.

PID có thể đổi; luôn xác minh lại trước thao tác. Không đóng gói đè JAR đang chạy hoặc dừng tất cả Java.

## Giới hạn và bàn giao

Chưa chuyển tiền thật hoặc kiểm chứng callback PayOS đi qua mạng bên ngoài. Automated tests dùng SDK giả lập cho timeout/thử lại; browser clone chỉ kiểm chứng nút và thông báo tắt thanh toán. Chưa nghiệm thu mọi kích thước màn hình hay toàn bộ dự án.

Backup nằm ngay thư mục này, gồm SQL trước migration cho hai database và `source-before-phase4-paths.zip`; hash tại `backup-sha256.txt`. Không tải backup/config chứa dữ liệu nhạy cảm lên dịch vụ AI. Các thay đổi schema là bổ sung; không cần xóa cột để chạy lại code cũ, nhưng code cũ vẫn có nghiệp vụ duyệt gia hạn sai nên không dùng song song cho đơn thật.

Đã chuẩn bị [prompt dán vào Gemini 3.8 Flash](../gemini-handoff/PROMPT-GEMINI-TIEP-TUC.md) và [checkpoint chính xác](../gemini-handoff/CURRENT_TASK.md). Nếu tiếp tục Phase 5, cần chốt quy tắc chia tiền khi đổi xe giữa ngày trước khi triển khai; không tự suy đoán.
