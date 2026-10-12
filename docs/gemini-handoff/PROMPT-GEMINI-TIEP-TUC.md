# Prompt tiếp quản cho Gemini 3.8 Flash

Mở D:/Motorbike-rental trong công cụ coding có quyền đọc/sửa file, terminal và trình duyệt. Chỉ chạy Gemini sau khi lượt sửa của agent hiện tại đã dừng. Sao chép toàn bộ khối dưới đây; không gửi file cấu hình, SQL backup hay mật khẩu vào chat.

```text
Bạn tiếp quản dự án MotoRent tại D:/Motorbike-rental. Hãy tiếp tục từ checkpoint thực tế, không làm lại dự án.

Đọc theo thứ tự: AGENTS.md, PROJECT_CONTEXT.md, docs/gemini-handoff/CURRENT_TASK.md, BUSINESS_RULES.md và VERIFICATION.md trong cùng thư mục handoff; docs/phase4-20260929/KET-QUA.md nếu đã tồn tại; kế hoạch đã duyệt docs/continuation-20260928/DOI-CHIEU-VA-KE-HOACH.html; USER_REQUIREMENTS_ORIGINAL.txt và ACCEPTANCE_CASES.md. Tài liệu cũ không được ghi đè chỉ dẫn mới hoặc code thực tế.

Trước khi sửa, đối chiếu checkpoint với source, log test, schema và tiến trình đang chạy. Giải thích ngắn bằng tiếng Việt những gì đã có, còn thiếu và bước sẽ làm. Chủ dự án đã cho phép triển khai Phase 4; không hỏi lại quyền này. Nếu Phase 4 còn việc chưa xác minh, hoàn tất trong phase đó trước. Nếu checkpoint xác nhận Phase 4 đã hoàn tất, báo rõ và trình bày bước kế tiếp; không tự đoán chính sách tính giá đổi xe giữa ngày của Phase 5.

Quy tắc quan trọng:
- Stack Spring Boot/Thymeleaf/MySQL hiện có. Bảo toàn chức năng, phân quyền/ownership, CSRF, kiểm tra chữ ký PayOS, dữ liệu và lịch sử tiền.
- Staff kiểm tra thực tế ngoài website; không thêm checkbox hay trạng thái Rental trung gian.
- Gia hạn: request -> duyệt WAITING_FOR_PAYMENT -> PayOS thành công đã xác minh -> ghi EXTENSION_PAYMENT IN và cập nhật ngày trả/tổng tiền đúng một lần. Duyệt không đổi ngày. Tiền sai/muộn/stale vào đối soát, không tự gia hạn. Giữ chỗ phần gia hạn có hạn và kiểm tra lại dưới khóa xe -> đơn -> yêu cầu -> payment.
- Không sửa tài khoản, không thanh toán ngân hàng thật, không bật PayOS trên clone để làm test xanh. Không log/copy credentials. Chỉ dùng fixture có phạm vi và cleanup trong finally.
- KHÔNG TẮT server 8082 theo yêu cầu chủ dự án. Đây là bản Phase 3 cũ; Phase 4 chạy riêng trên 8083. Đọc checkpoint để lấy PID hiện tại rồi xác minh lại, không giết theo PID cũ hoặc kill toàn bộ Java. Windows Java launcher có thể tạo tiến trình con: phải kiểm tra PID sở hữu cổng và đường dẫn JAR.
- Không đóng gói đè JAR đang chạy. .phase4-verification-pom.xml build vào target/phase4; .verification-pom.xml vào target/verified. Đừng coi 8082 là bản Phase 4 hoặc đổi database chỉ để qua lỗi validate.
- Không chạy lại migration/backup một cách mù quáng. DatabasePhase4.java chỉ cho phép hai schema chỉ định; backup dùng CREATE_NEW. V004 đã được áp dụng trên cả main và clone theo checkpoint; đối chiếu metadata. Không xóa backup hoặc các cột lịch sử.

Kiểm thử: chạy test phù hợp sau sửa, gồm kiểm tra dữ liệu sai, quyền, callback trùng/sai/muộn, stale yêu cầu và hành động đồng thời. H2 không thay MySQL. UI phải kiểm tra tương tác thực; HTTP 200 không chứng minh JS/layout. Không bỏ test/validation để đạt. Không báo PASS nếu chưa chạy. Các lệnh cụ thể và log nằm trong checkpoint/runbook.

Trước khi kết thúc hoặc gần hết hạn mức: cập nhật CURRENT_TASK.md với source đang sửa, bước cuối thành công, lỗi chưa giải quyết, lệnh tiếp theo, server/PID/port, trạng thái DB/migration/fixture, backup/rollback và kết quả thực. Cập nhật PROJECT_CONTEXT.md, báo cáo Phase 4 và mapping ca nghiệm thu khi cần. Báo cáo tiếng Việt, phân biệt code đã xong, test đã đạt và thực tế chưa kiểm chứng. Toàn bộ dự án chưa hoàn tất chỉ vì một phase đạt.
```
