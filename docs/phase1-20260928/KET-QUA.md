# Phase 1 — Nền tảng thu/chi, audit và lịch xe

Ngày 28/09/2026. Thực hiện sau khi người dùng duyệt kế hoạch trong `docs/continuation-20260928/DOI-CHIEU-VA-KE-HOACH.html`.

## Đã thực hiện

- Tái sử dụng bảng `payments`, không tạo module Transaction hay bảng tài chính song song. Bổ sung `transactionType`, `direction`, nhân viên/thời điểm ghi nhận, ghi chú, nguồn nghiệp vụ, liên kết giao dịch hoàn và khóa chống ghi trùng.
- Lưu số tiền không âm + IN/OUT trong DB. Giữ `purpose`/`status` cũ để tương thích; API bổ sung loại chuẩn, `transactionStatus=SUCCESS` cho khoản đã ghi nhận. Trường `amount`/`receivedAmount` trả về giao diện cũ vẫn có dấu, thêm `absoluteAmount` để không làm khoản hoàn hiển thị như khoản thu.
- Booking vẫn tính `% tiền thuê + toàn bộ phí giao/nhận`, tách cọc tài sản. Đơn mới lưu phí từng chặng và tọa độ; payment mới lưu snapshot phân bổ tiền thuê/giao/trả. Tổng phân bổ phải đúng bằng giao dịch, không cộng các thành phần thêm một lần vào dòng tiền.
- Thu/hoàn hiện có hỗ trợ Tiền mặt / Chuyển khoản / Khác và ghi chú. Người thực hiện lấy từ tài khoản đăng nhập, không lấy từ dữ liệu client. Trang thu chi hiển thị chiều tiền, người và thời điểm ghi nhận. Dữ liệu cũ thiếu người thực hiện ghi “Chưa ghi nhận”, không gán cho staff hiện tại của đơn.
- Thêm API xem số tiền thuê thu ròng, cọc đang giữ và khoản cần đối soát theo đơn. API kiểm tra quyền đọc của chủ đơn/Staff/Admin. Đây là số liệu dòng tiền đã ghi nhận, chưa thay thế báo cáo doanh thu Phase 8.
- Tạo `AvailabilityService` dùng chung ở tìm xe theo thời gian, tạo đơn, kiểm tra gia hạn và giao xe. Lịch giáp ranh không trùng; đơn giữ chỗ chưa trả đã hết hạn không tiếp tục chặn xe. Xe đã quá hạn mà chưa được nhận lại không được coi là trống chỉ vì giờ trả dự kiến đã qua.
- Kiểm tra availability dưới khóa xe/đơn, đọc dữ liệu mới đã commit trong MySQL REPEATABLE READ. Khi đã chọn ngày, bộ lọc lịch không loại xe chỉ vì hiện tại đang RENTED nhưng lịch tương lai hợp lệ.
- Giữ cơ chế hết hạn booking làm nền tảng giữ chỗ. Giữ chỗ riêng cho đề nghị đổi xe/gia hạn sẽ được nối ở Phase 2/4/5 khi các đề nghị đó tồn tại; chưa tạo bảng hoặc luồng giả cho chúng.
- Sửa lỗi hoàn tiền đồng thời phát hiện trên MySQL: hai yêu cầu có thể cùng thấy số dư cũ. Đã chuyển sang đọc payment dưới khóa; chỉ một khoản hoàn được ghi. Bổ sung kiểm tra hai callback PayOS đồng thời và làm mới bản ghi dưới khóa để giao dịch PAID không bị callback lặp ghi sai thành REVIEW_REQUIRED.
- Tiền đến muộn/sai số tiền hoặc lịch không còn khả dụng vẫn vào đối soát, không tự xác nhận đơn và không tự chuyển tiền hoàn.

## API thêm / thay đổi

- Thêm `GET /api/payment/order/{orderId}/summary`: `rentalCashNet`, `assetDepositHeld`, `unreconciledAmount`.
- `GET /api/payment/order/{orderId}`: giữ trường cũ, bổ sung `absoluteAmount`, `transactionType`, `direction`, `transactionStatus`.
- `GET /api/operations/data/payments`: bổ sung audit, chiều tiền, loại chuẩn và phân bổ; giao diện hiển thị thông tin người ghi nhận.
- `POST /api/operations/orders/{id}/cash`: giữ `purpose`, `reference`; thêm `method`, `note`. Client cũ không gửi method vẫn mặc định CASH.
- `POST /api/operations/payments/{id}/refund`: thêm method/note, lưu giao dịch gốc và nhân viên.
- Không đổi endpoint hay thêm trạng thái/checkbox ở thao tác giao/hoàn tất. Không mở API cho Customer tự ghi nhận đã trả tiền.

## Database và sao lưu

- Sao lưu mã nguồn: `source-before-phase1.zip`.
- Sao lưu đầy đủ hai DB trước migration: `before-motorent_db.sql`, `before-motorent_verify_20260926.sql`; SHA-256 trong `backup-sha256.txt`.
- Migration có phiên bản: `V001__financial_foundation.sql`; công cụ `DatabasePhase1.java` kiểm tra backup bắt buộc, bỏ qua cột/index đã có khi chạy lại và kiểm tra tổng tiền có dấu trước/sau.
- Bổ sung 10 cột ở payments, 6 cột ở rental_orders và unique index idempotency. Không xóa bảng hay đơn lịch sử. Không suy đoán tọa độ, phân bổ từng chặng hoặc staff cho bản ghi cũ.
- Đã thử trên `motorent_verify_20260926` trước, sau đó mới áp dụng `motorent_db`. Cả hai giữ nguyên 2 payment, 2 đơn và tổng số tiền có dấu 110.000 đồng tại thời điểm xác minh. Chạy lại migration trên clone vẫn giữ nguyên kết quả.
- Các fixture kiểm thử đồng thời trên MySQL được dọn sau mỗi test; không tạo giao dịch ngân hàng thật.

## Kết quả kiểm thử

- `package-tests.log`: **50 tests, 0 failures, 0 errors; BUILD SUCCESS**. Gồm bộ hồi quy cũ và các ca mới về giá, phân bổ, phương thức/audit, quyền truy cập summary, tách cọc, tiền cần đối soát, hoàn khoản chưa thu, lịch giáp ranh, giữ chỗ hết hạn và xe quá hạn.
- `tests-mysql.log`: **3 tests, 0 failures, 0 errors** trên MySQL 9, REPEATABLE READ: đặt xe đồng thời chỉ một đơn; hoàn tiền đồng thời chỉ một khoản; callback lặp đồng thời chỉ một giao dịch PAID và đơn chờ duyệt đúng.
- `operations-live.js`: qua kiểm tra cú pháp Node.
- Sau khi khởi động lại, `operations-http.log`: **36 kiểm tra HTTP/API** Admin/Staff và chặn quyền trái phép đạt. `customer-http.log`: **12 kiểm tra** cọc 3 loại xe/1–2 giấy tờ, ngày quá khứ, trang Customer và chặn PayOS thật trên clone đạt.
- Kiểm tra HTTP bổ sung trên clone: 2 payment lịch sử có direction/type/audit label; API summary trả đủ 3 số dư cho 2 đơn. Đây là kiểm tra response và render server, không phải tuyên bố đã bấm thử trực quan mọi thao tác trên trình duyệt.
- Bản chính **http://127.0.0.1:8080/** và bản kiểm thử **http://127.0.0.1:8082/** đã khởi động lại thành công bằng JAR Phase 1 với schema validate. Log: `start-main.log`, `start-clone.log`.
- Không dùng số lượng 50 test để tuyên bố đã hoàn thành 65 tình huống của toàn bộ kế hoạch. Chưa kiểm thử giao dịch ngân hàng thật.

## Các file liên quan

Danh sách chính xác: `changed-files.txt` (so sánh với source backup).

- Entity: Payment, RentalOrder; repository: PaymentRepository, RentalOrderRepository.
- Service mới: AvailabilityService, Financials, FinancialSummaryService.
- Service mở rộng: PricingService, PaymentService, SettlementService, RentalOrderService, ExtensionService, OperationsService.
- API/DTO/mapper: PaymentController, SettlementController, PaymentResponse, PaymentMapper; lọc lịch ở CustomerPageController.
- Giao diện: operations-live.js và customer/rental-detail.html.
- Test: CustomerSecurityIntegrationTests, FinancialsTests, Phase1ConcurrencyTests.

## Giới hạn và bước tiếp theo

Phase 1 chỉ hoàn thành nền tảng. Đổi xe trước giao thuộc Phase 2; ghi tiền ngay khi giao/nhận sớm thuộc Phase 3; PayOS gia hạn thuộc Phase 4; đổi xe lúc thuê thuộc Phase 5; trả sớm/hoàn cọc/late fee/báo cáo đầy đủ ở Phase 6–8.

Do đó hiện tại nút bàn giao chưa tự tạo payment, quy tắc cấm nhận sớm và gia hạn đổi ngày ngay khi Staff duyệt vẫn còn theo hành vi trước Phase 3/4. Không coi các điểm này đã được sửa trong lượt Phase 1. Chưa thêm điều kiện hay quy trình chưa được yêu cầu. Báo cáo doanh thu tổng hợp hiện tại chưa phải net revenue mới.

Các quy ước chia giá khi đổi giữa ngày và phân loại khấu trừ cọc vẫn cần chốt trước Phase 5/7 theo kế hoạch đã duyệt; không ngăn cản nền tảng Phase 1.

## Lưu ý phục hồi cho người tiếp tục

Không nhập backup SQL vào DB đang chạy: backup chứa CREATE TABLE + INSERT và dùng cho khôi phục vào schema trống. Nếu cần rollback, dừng đúng ứng dụng, giữ thêm backup dữ liệu mới phát sinh, phục hồi bản trước Phase 1 vào schema riêng rồi đối chiếu; khôi phục source và build tương ứng. Không chạy JAR cũ với dữ liệu hoàn đã chuyển thành amount dương + OUT, vì code cũ chỉ hiểu amount âm. Không tự xóa các cột mới để rollback.

Môi trường build hiện dùng JDK 21, Maven đã cài và `.verification-pom.xml`. Sandbox từng chặn đọc real path của JAR trong `.m2`; lần build được cấp quyền đọc đã thành công. Không thay phiên bản dependency của dự án.
