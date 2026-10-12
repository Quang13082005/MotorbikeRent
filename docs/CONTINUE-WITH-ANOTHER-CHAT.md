> QUY TẮC MỚI NHẤT: Đã bỏ checkbox và bước chuẩn bị giao. Staff chỉ bấm Đã giao xe / Hoàn tất đơn; không tự ghi tiền, không chặn hoàn tất vì số tiền chưa ghi nhận. Xem đầu [KET-QUA](continuation-20260928/KET-QUA.md). Mô tả cũ bên dưới đã bị thay thế.

> CẬP NHẬT MỚI NHẤT 28/09/2026: đọc [kết quả tiếp tục](continuation-20260928/KET-QUA.md) trước. Dự án hiện tại D:/Motorbike-rental. Yêu cầu mới đã thay thế đề xuất xe điện và bàn giao bắt buộc biên bản: bỏ xe điện; bàn giao bằng hai checkbox, không giao sớm. Nội dung cũ phía dưới là lịch sử, không phải yêu cầu mới của người dùng.

# Tiếp tục MotoRent bằng Gemini hoặc chat khác

Tài liệu bàn giao ngày 2026-09-25. Không cần đọc hết lịch sử chat để nắm hướng chính, nhưng phải kiểm tra source và trạng thái công việc thực tế trước sửa.

## Cách người dùng chuyển công việc

1. Cho công cụ mới quyền đọc thư mục dự án D:/DuAnCuaToi/motorbike-rental. Nếu dùng chat không truy cập máy, gửi bản source đã loại secret, build/cache, log và dữ liệu cá nhân không cần thiết; gửi ba tài liệu trong danh sách dưới. Không gửi nguyên application.properties nếu chứa khóa bí mật.
2. Cho công cụ truy cập JSON C:/Users/THINKPAD789/Downloads/places_VN_Đà Nẵng.json khi làm phần import. Nếu chuyển máy, cung cấp đường dẫn mới; không mặc định đường dẫn máy cũ tồn tại. Không cần tải 25MB dữ liệu lên frontend.
3. Dán prompt dưới đây khi muốn bắt đầu/tiếp tục triển khai. Nếu chỉ muốn góp ý kế hoạch, đổi câu đầu thành “Chỉ đọc và góp ý, chưa sửa code”.

## Prompt sao chép

> Hãy tiếp tục triển khai dự án MotoRent từ source hiện tại. Đọc docs/HANDOFF.md, docs/07-LEAN-RENTAL-PLAN.md và docs/CONTINUE-WITH-ANOTHER-CHAT.md trước, rồi kiểm tra mã nguồn, schema và các thay đổi đang có để xác định bước chưa hoàn thành.
>
> Mục tiêu của tôi là website đơn giản để tìm khách thuê xe tại Đà Nẵng. Phải có phần giới thiệu du lịch/vui chơi, khách sạn/chỗ ở và ăn uống, có liên kết hợp lý đến danh sách xe/form thuê; khách sạn có thể điền sẵn nơi giao xe. Không xây hệ thống đặt phòng/tour.
>
> Khách không cần đăng ký/đăng nhập. GIỮ Google Maps, quét CCCD và thanh toán cọc PayOS. Maps chỉ mở khi cần chọn nơi giao; OCR tùy chọn để điền nhanh và có nhập tay; staff xác nhận xe/giá trước khi khách trả cọc. Khách có đường dẫn riêng để xem đơn/thanh toán. Staff/admin vẫn cần đăng nhập và phân quyền. Không bỏ các chức năng này theo kế hoạch v1 cũ.
>
> Ưu tiên một luồng chạy thật từ xem xe đến gửi yêu cầu, staff xác nhận, cọc và giao/nhận. Giữ nhận diện MotoRent và phần đang dùng được; dọn chức năng thừa theo kế hoạch, không chỉ ẩn menu và để dữ liệu mẫu giả làm chức năng thật. Sao lưu trước thay đổi lớn, không xóa dữ liệu đơn cũ hay JSON nguồn. Không lấy khách/nhân viên đầu tiên trong database làm chủ đơn. Không ghi đã thanh toán từ query URL, không tin số tiền phía trình duyệt.
>
> Chỉ chọn lọc và biên tập địa điểm từ JSON, không đăng hết 32.740 bản ghi. File có địa điểm ngoài vùng, nơi đóng cửa và thiếu dữ liệu; photo_count không phải ảnh. Không bịa ảnh/giá/đánh giá/mô tả. Không tự gửi tin cho khách, tạo giao dịch thật hoặc xuất bản website chỉ vì đang làm bản phát triển.
>
> Sau mỗi mốc và trước khi hết ngữ cảnh, cập nhật docs/HANDOFF.md bằng tiến độ thật: file sửa, quyết định, migration, lệnh test/kết quả, lỗi còn lại và bước kế tiếp cụ thể. Đọc lại source thay vì tin tuyệt đối báo cáo cũ; không làm lại việc đã xong, không báo test đạt khi chưa chạy. Nếu thiếu source hoặc tài nguyên thiết yếu, nói rõ thiếu gì thay vì tự suy đoán.

## Quy trình cho công cụ tiếp nhận

1. HANDOFF là điểm vào; plan v2 là phạm vi hiện hành. Yêu cầu mới của người dùng có ưu tiên hơn tài liệu. Các docs cũ mô tả hệ thống trước đây không được tự khôi phục phạm vi đã bỏ.
2. Kiểm tra AGENTS.md/hướng dẫn repo nếu có; kiểm tra git/backup thật. Chưa có bằng chứng repo root hiện có .git. Không reset hoặc restore đè thay đổi đang có; snapshot target/ui-before không phải checkpoint hiện tại.
3. Đọc các phần liên quan trước sửa: CustomerPageController, RentalOrderApiController, RentalOrderService, PaymentController/Service, OcrApiController, security, entities/repositories, staff.js, template customer và map-modal. Đọc operations.js để nhận diện prototype admin. Không in cấu hình bí mật.
4. Kiểm kê module giữ/bỏ theo phụ thuộc. Giữ Maps/OCR/PayOS và bảo mật nội bộ. Lập migration dữ liệu có thể phục hồi; không chạy DROP hoặc ddl-auto create trên database thật.
5. Thực hiện mốc chưa xong nhỏ nhất theo plan; sau mỗi mốc build/test thích hợp, kiểm tra UI khi đổi giao diện và cập nhật bàn giao. Khi dịch vụ ngoài thiếu khóa/test sandbox, tiếp tục phần độc lập với mock, ghi rõ phần chưa kiểm trực tiếp.
6. Giữ lời hứa giao diện khớp backend: nút lưu phải lưu thật, quét phải điền được dữ liệu, đơn không được thành đã cọc từ URL, khách mới gửi yêu cầu chưa có cam kết giữ xe.

## Mẫu cập nhật HANDOFF sau mỗi mốc

```text
Thời điểm / mốc:
Phạm vi và quyết định mới được người dùng chốt:
Đã hoàn thành (kèm file chính):
Đang làm (file/hàm, trạng thái chạy được hay chưa):
Thay đổi schema/dữ liệu + checkpoint/khôi phục:
Lệnh kiểm tra đã chạy + kết quả thực tế:
Kiểm tra dịch vụ ngoài: thật / sandbox / mock / chưa kiểm:
Lỗi hoặc phụ thuộc còn thiếu:
Bước tiếp theo cụ thể (1–3 bước):
Tiến trình preview đang chạy nếu đã kiểm tra:
```

Không ghi token, khóa API, thông tin CCCD, số điện thoại khách thật hoặc đường dẫn truy cập riêng của đơn vào bàn giao. Khi phiên sắp hết ngữ cảnh, ưu tiên lưu trạng thái đang dở và bước tiếp theo hơn bắt đầu thay đổi lớn chưa thể kiểm tra.

## Tình trạng khi tạo tài liệu

Chỉ hoàn thành việc viết lại kế hoạch và hướng dẫn tiếp tục. Chưa có luồng khách vãng lai, chưa chuyển schema, chưa import địa điểm, chưa sửa thanh toán/OCR/Maps theo thiết kế mới. Đọc HANDOFF mới nhất để biết các lượt sau đã làm thêm gì.
