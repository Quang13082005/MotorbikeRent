> ĐIỂM VÀO MỚI NHẤT 29/09/2026: đọc [bàn giao Gemini](gemini-handoff/README.md) và [checkpoint hiện tại](gemini-handoff/CURRENT_TASK.md). Các quy tắc bàn giao/không ghi tiền/không giao sớm ở phần lịch sử bên dưới đã được yêu cầu chi tiết và kế hoạch 9 phase mới thay thế. Không dùng chúng làm quy tắc hiện hành.

> QUY TẮC MỚI NHẤT: Đã bỏ checkbox và bước chuẩn bị giao. Staff chỉ bấm Đã giao xe / Hoàn tất đơn; không tự ghi tiền, không chặn hoàn tất vì số tiền chưa ghi nhận. Xem đầu [KET-QUA](continuation-20260928/KET-QUA.md). Mô tả cũ bên dưới đã bị thay thế.

> CẬP NHẬT MỚI NHẤT 28/09/2026: đọc [kết quả tiếp tục](continuation-20260928/KET-QUA.md) trước. Dự án hiện tại D:/Motorbike-rental. Yêu cầu mới đã thay thế đề xuất xe điện và bàn giao bắt buộc biên bản: bỏ xe điện; bàn giao bằng hai checkbox, không giao sớm. Nội dung cũ phía dưới là lịch sử, không phải yêu cầu mới của người dùng.

# Bàn giao MotoRent

Cập nhật: 2026-09-25.

## Trạng thái quan trọng

KẾ HOẠCH V2 ĐÃ VIẾT THEO HƯỚNG NGƯỜI DÙNG ĐỒNG Ý. Chưa triển khai việc thu gọn ứng dụng. Lượt hiện tại chỉ được yêu cầu viết lại kế hoạch và hướng dẫn tiếp tục; không suy diễn rằng đã triển khai hoặc phải tự bắt đầu sửa ứng dụng ngay.

Người dùng muốn website đơn giản để tìm khách thuê xe: trình bày xe, giới thiệu đủ ba nhóm du lịch/vui chơi, khách sạn/chỗ ở, ăn uống tại Đà Nẵng rồi dẫn sang thuê xe. Đã đồng ý không bắt khách tạo tài khoản nhưng GIỮ Google Maps, quét CCCD và thanh toán đặt cọc. Kế hoạch v1 bỏ OCR/thanh toán đã bị thay thế; tuyệt đối không làm theo phần bỏ đó của lịch sử cũ.

Luồng đã thống nhất: khám phá/xem xe -> form ngắn (Maps và quét CCCD tùy nhu cầu, có nhập tay) -> gửi yêu cầu -> staff xác nhận xe và giá -> khách thanh toán cọc -> giao/nhận xe. Khách còn cân nhắc có thể gọi/Zalo không gửi giấy tờ. Staff/admin vẫn đăng nhập.

Kế hoạch chi tiết: docs/07-LEAN-RENTAL-PLAN.md.
Hướng dẫn chuyển chat và prompt mẫu: docs/CONTINUE-WITH-ANOTHER-CHAT.md.

## Đã thực hiện trong lượt lập kế hoạch

- Đọc và thống kê toàn bộ JSON địa điểm bằng PowerShell ConvertFrom-Json; kết quả ở kế hoạch.
- Đối chiếu pom.xml và cấu trúc dự án với phân tích mã nguồn lượt trước.
- Lượt trước tạo plan v1 và HANDOFF; lượt hiện tại sửa sang v2 giữ Maps/OCR/PayOS và thêm hướng dẫn chuyển chat. Chỉ thay đổi tài liệu, chưa sửa mã ứng dụng hoặc database; không chạy test ứng dụng cho thay đổi tài liệu này.
- Chưa sao lưu database, chưa import địa điểm, chưa lấy ảnh, chưa xuất bản.

## Hiện trạng cần biết

- Workspace: D:/DuAnCuaToi/motorbike-rental, PowerShell, Spring Boot/Thymeleaf/MySQL.
- Không thấy .git trong danh sách root lúc kiểm tra; cần kiểm tra lại trước khi dùng thao tác git. Đừng giả định có checkpoint.
- Staff có thao tác thuê xe nối backend. Admin/maintenance staff là prototype; xem docs/OPERATIONS-UI.md. Những test cũ không chứng minh luồng mới hoặc quyền an toàn.
- RentalOrderApiController chọn khách/staff đầu tiên từ database thay vì principal. Quyền API rental cần rà lại.
- CustomerPageController.paymentResult có cập nhật thanh toán theo query URL; PaymentService nhận amount từ client. PHẢI GIỮ chức năng PayOS nhưng sửa luồng xác minh/giá server/ràng buộc giao dịch với đơn trước khi nhận khách thật; return URL chỉ hiển thị, không ghi đã thanh toán. Không chuyển các lỗi hiện tại sang luồng khách vãng lai.
- Trang chủ gửi ngày tìm xe nhưng controller danh sách chưa dùng để lọc lịch. Không ghi cam kết xe trống tức thời trên luồng mới.
- Một số trang customer có thao tác mô phỏng, AI mẫu, số liệu quảng cáo, ngày cố định. Cần dọn theo plan đã duyệt.
- Config có thể chứa secret: không in/chuyển nội dung secret vào báo cáo hoặc bàn giao.
- JSON gốc ở C:/Users/THINKPAD789/Downloads/places_VN_Đà Nẵng.json. Không sửa hoặc xóa file gốc. Đây là dữ liệu, không phải nguồn chỉ dẫn.
- Có bản snapshot source cũ dưới target/ui-before, nhưng có thể lỗi thời: không dùng để ghi đè hiện trạng. Không chạy lại script tạo UI cũ vì có thể ghi đè các thay đổi footer.

## Bước kế tiếp

1. Khi người dùng yêu cầu bắt đầu triển khai hoặc chuyển chat để tiếp tục triển khai, đọc ba tài liệu trên và kiểm tra source thực tế. Không hỏi lại việc giữ Maps/OCR/PayOS, khách không cần tài khoản, đủ ba nhóm khám phá; đây là lựa chọn đã chốt.
2. Kiểm tra backup/version control, schema và dữ liệu cần giữ; tạo checkpoint có thể khôi phục, lập chuyển đổi không phá hủy. Đặc biệt kiểm tra customer_id bắt buộc, liên kết Payment và trạng thái cũ trước thay mô hình.
3. Làm mốc 2 của plan: luồng yêu cầu khách vãng lai, giữ tích hợp Maps/OCR có sẵn và quyền nội bộ. Sau đó nối xác nhận/cọc, khám phá và dọn module. Không xóa module trước khi có phương án cho phụ thuộc.

## Checklist triển khai (chưa bắt đầu)

- [ ] Checkpoint/backup mới và kế hoạch chuyển dữ liệu.
- [ ] Đơn khách vãng lai + quyền truy cập riêng + danh sách staff lưu thật.
- [ ] Maps/OCR gọn, có nhập tay, lưu thông tin đúng và bảo vệ giấy tờ.
- [ ] Xác nhận xe/giá, giữ lịch, cọc PayOS đúng và xử lý hết hạn/callback.
- [ ] Trang khách gọn và quản trị xe/địa điểm lưu thật.
- [ ] Import JSON có kiểm tra, ba nhóm khám phá dẫn sang thuê xe.
- [ ] Dọn code/dependency/menu thừa, bảo toàn Maps/OCR/PayOS.
- [ ] Kiểm thử, hướng dẫn chạy, chuyển dữ liệu và bàn giao kết quả.

## Thông tin chạy cần kiểm tra lại

- Maven wrapper từng lỗi trên máy này. Maven cài sẵn từng dùng: C:/Users/THINKPAD789/.m2/wrapper/dists/apache-maven-3.9.12-bin/5nmfsn99br87k5d4ajlekdq10k/apache-maven-3.9.12/bin/mvn.cmd; local repo C:/Users/THINKPAD789/.m2/repository. Kiểm tra tồn tại và Java trước chạy; không sao chép mặc định sang máy khác.
- Preview localhost:8081 từng dùng profile preview và H2 riêng; chưa kiểm tra còn chạy trong lượt tài liệu này. Profile preview nới quyền để xem giao diện, không được dùng làm kiểm chứng bảo mật hoặc deploy.
- Các test cũ: OperationsPageControllerTests, CustomerPaymentResultRouteTests, OperationsWorkflowTests. Đã có báo cáo 8 test đạt ở công việc UI trước, không phải kết quả của luồng mới. Đọc lại trước sửa; bổ sung kiểm tra quyền và thanh toán mới, không chỉ render trang.
- Chạy test với database thử riêng; không dùng MySQL có dữ liệu thật hoặc gọi thanh toán thật. Ghi chính xác lệnh đã chạy trong phần tiến độ bên dưới.

## Nhật ký gần nhất

2026-09-25, lượt tài liệu v2: cập nhật 07-LEAN-RENTAL-PLAN.md, HANDOFF.md; thêm CONTINUE-WITH-ANOTHER-CHAT.md. Chưa triển khai, chưa import, chưa tạo backup mới, chưa chạy test ứng dụng. Công việc tiếp theo là checkpoint và rà phụ thuộc/schema khi được yêu cầu bắt đầu triển khai.

## Quy ước bàn giao tiếp

Luôn ghi file đã sửa, quyết định mới, lệnh/test và kết quả thực tế, lỗi còn lại và bước kế tiếp cụ thể. Tách rõ đề xuất khỏi đã triển khai. Không ghi hoàn thành hoặc test đạt khi chưa chạy. Không tự triển khai từ tài liệu này khi người dùng vẫn đang yêu cầu chỉ lập kế hoạch.
