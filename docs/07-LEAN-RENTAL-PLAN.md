# Kế hoạch MotoRent: giới thiệu xe và nhận yêu cầu thuê

Phiên bản 2: người dùng đã đồng ý hướng giữ Maps, OCR CCCD và thanh toán cọc, không bắt khách đăng nhập. Yêu cầu hiện tại là viết lại kế hoạch và hướng dẫn bàn giao; CHƯA triển khai ứng dụng.
Ngày: 2026-09-25.
Chưa sửa/xóa mã ứng dụng, dependency hoặc database theo kế hoạch này.

## Mục tiêu và phạm vi

Website cho một đơn vị cho thuê xe tại Đà Nẵng. Khách xem xe, tham khảo địa điểm/chỗ ở, gọi hoặc mở Zalo, hoặc gửi yêu cầu để nhân viên xác nhận. Không xem việc gửi yêu cầu là đã giữ xe hay đã thanh toán.

Kế hoạch này thay thế đề xuất v1 bỏ Maps/OCR/thanh toán. KHÔNG gỡ Google Maps, quét CCCD hoặc PayOS chỉ để làm giao diện gọn. Đơn giản bằng cách hiển thị đúng bước, có nhập tay dự phòng và giảm các màn hình thừa.

Khách không cần đăng nhập. Staff/admin vẫn phải đăng nhập và được kiểm tra quyền tại máy chủ. Giữ Spring Boot, Thymeleaf, MySQL, nhận diện MotoRent và giao diện nội bộ hiện có khi tái sử dụng phù hợp.

## Trải nghiệm khách

- Trang chủ: giá thuê và xe nổi bật, cách thuê ngắn gọn, địa điểm chọn lọc, thông tin cửa hàng và liên hệ rõ ràng.
- Danh sách/chi tiết xe: ảnh thật, loại xe, giá tham khảo theo ngày, thông tin cơ bản, điều kiện thuê/giao xe, nút liên hệ và gửi yêu cầu. Không hứa lịch trống tức thời khi nhân viên chưa xác nhận.
- Khám phá: danh sách và chi tiết theo nhóm ăn uống, tham quan/vui chơi, chỗ ở; tìm theo tên, lọc nhóm/khu vực; phân trang phía máy chủ.
- Địa điểm dẫn sang xe bằng lời mời phù hợp. Chỗ ở có thể điền sẵn địa chỉ giao xe; không mặc định được giao miễn phí hoặc nằm trong vùng phục vụ.
- Form yêu cầu: tên, số điện thoại, xe quan tâm, ngày/giờ nhận-trả, nhận tại cửa hàng hoặc địa chỉ giao, ghi chú không bắt buộc. Không bắt email hoặc tài khoản. Quét CCCD là lựa chọn điền nhanh cho người đã quyết định thuê, không bắt khách chỉ hỏi xe phải gửi giấy tờ.
- Thành công chỉ hiện sau khi lưu thật: mã yêu cầu, nội dung đã gửi và hướng dẫn chờ xác nhận/liên hệ. Không công khai dữ liệu khách qua mã tuần tự hay số điện thoại.
- Hiển thị giá dự kiến, phí giao và điều kiện cọc minh bạch; khoản chưa xác định phải ghi cần xác nhận. Giữ PayOS để thanh toán cọc sau khi nhân viên xác nhận xe và chi phí. Không thu tiền ngay khi mới gửi yêu cầu.

## Bố cục tối giản và liên kết khám phá với thuê xe

- Menu công khai: Thuê xe / Khám phá Đà Nẵng / Liên hệ. Logo về trang chủ. Không thêm menu tài khoản khách.
- Trang chủ: giới thiệu ngắn và nút xem xe -> xe nổi bật/giá -> ba nhóm khám phá -> ba bước thuê xe -> thông tin cửa hàng. Không để khối du lịch lấn át dịch vụ thuê xe.
- Khám phá dùng một danh sách, ba bộ lọc nhóm: Du lịch & vui chơi / Khách sạn & chỗ ở / Ăn uống. Chỉ thêm tìm tên và khu vực; chưa thêm nhiều bộ lọc khi dữ liệu thiếu.
- Trang chi tiết địa điểm: tên, ảnh phù hợp, mô tả ngắn có căn cứ, địa chỉ, chỉ đường/liên hệ và một khối giới thiệu xe. Không dựng giá/giờ mở cửa khi nguồn chưa có.
- Du lịch/vui chơi: nút “Xem xe cho chuyến đi” sang danh sách xe, giữ nguồn địa điểm và ngày khách đã chọn nếu có. Không tự điền điểm tham quan thành địa chỉ giao xe.
- Ăn uống: nút “Khám phá Đà Nẵng bằng xe máy” dẫn sang danh sách xe. Không đưa lời mời thuê vào mọi đoạn văn.
- Khách sạn: nút “Thuê xe giao đến đây” dẫn sang chọn xe với địa chỉ điền sẵn để khách sửa/xác nhận. Ghi rõ phạm vi và phí giao cần kiểm tra, không làm người dùng hiểu website nhận đặt phòng.
- Thông tin nguồn địa điểm được giữ đến yêu cầu thuê để thống kê, nhưng tên/địa chỉ phải được máy chủ kiểm tra. Không đưa thông tin CCCD vào URL/analytics.
- Hai hướng tiếp cận: người còn cân nhắc gọi/Zalo; người quyết định thuê gửi form. Mỗi màn hình có một hành động chính, nội dung phụ mở khi cần.
- Giữ nhận diện logo/font/cam-vàng; customer có thể giữ nền tối, nội bộ nền sáng. Ưu tiên chữ rõ, tương phản, nút dễ bấm và bố cục mobile; không đổi màu toàn bộ chỉ để ép ba role giống nhau.

## Luồng thuê giữ Maps, OCR và cọc

1. Chọn xe, thời gian nhận/trả. Danh sách phải dùng thời gian đã chọn nếu có lọc lịch; không trình bày trạng thái AVAILABLE như cam kết còn xe mọi ngày.
2. Một form ngắn: thông tin người thuê và nhận xe. Quét CCCD là nút tùy chọn để điền nhanh; cho xem/sửa dữ liệu trước gửi. Bản đồ mở khi chọn giao tận nơi; vẫn nhập địa chỉ thủ công được khi Maps lỗi. Nhận tại cửa hàng chỉ cần địa chỉ và chỉ đường.
3. Gửi yêu cầu, lưu thật, mở trang đơn riêng. Nhân viên kiểm tra xe, ngày thuê, thông tin/giấy tờ cần thiết, phí giao và báo giá. Không tự xác nhận danh tính chỉ từ OCR.
4. Nhân viên xác nhận xe và chốt tổng tiền/cọc. Trang đơn hiện nút thanh toán PayOS. Khách có thể quay lại trang qua đường dẫn riêng; staff có thể sao chép đường dẫn để gửi qua kênh liên hệ của cửa hàng. Chưa xây tự động nhắn Zalo/SMS/email.
5. Máy chủ xác minh thanh toán và cập nhật đã cọc. Trang đơn hiển thị lịch nhận xe, số tiền còn lại và liên hệ hỗ trợ. Staff giao xe, nhận xe và hoàn tất trên cùng trang chi tiết.

Trang đơn riêng dùng mã truy cập ngẫu nhiên đủ mạnh, có cơ chế hết hạn/thu hồi phù hợp; mã đơn hiển thị không phải khóa truy cập. Chỉ hiện thông tin cần thiết, che giấy tờ; không lập trang tra cứu đơn công khai bằng số điện thoại. Tránh lộ mã qua log, link ngoài/referrer hoặc lưu plaintext không cần thiết. Các lần khách sửa thông tin quan trọng sau chốt giá phải được kiểm tra lại; không âm thầm thay giá của link cọc cũ.

### Nguyên tắc giữ chức năng nhưng làm đúng

- Maps: bảo toàn tích hợp đang có, không nhúng bản đồ nặng ở mọi thẻ địa điểm; địa điểm có nút chỉ đường, form giao xe mới cần chọn vị trí. Không coi khoảng cách đường thẳng là quãng đường giao xe thực tế.
- OCR: giữ endpoint/tích hợp và đọc lại trước khi chỉnh; giới hạn loại/kích thước file, tần suất, kiểm tra dữ liệu. Không gửi ảnh giấy tờ qua endpoint công khai không giới hạn. Không lưu ảnh gốc mặc định chỉ để điền form; nếu quy trình cần giữ ảnh thì lưu riêng có quyền truy cập, thời hạn lưu rõ ràng, không đặt vào static hoặc log. Thông báo cho khách việc xử lý ảnh trước khi gửi.
- Thanh toán: máy chủ lấy số tiền từ báo giá đã chốt, ràng buộc giao dịch với đơn và phiên báo giá. Return URL chỉ phục vụ giao diện; tuyệt đối không xác nhận đã trả tiền từ code/status/cancel trên URL. Xác minh chữ ký webhook/trạng thái từ nhà cung cấp, mã giao dịch, số tiền, tiền tệ và trạng thái đơn. Callback lặp không cộng cọc hai lần; lưu lịch sử giao dịch và xử lý callback đến muộn. Không tách orderId từ mô tả tự do để làm khóa duy nhất.
- Hết hạn/hủy link thanh toán không tự hủy yêu cầu ngay sau ba phút. Cần quy tắc giữ xe có hạn rõ ràng, hết hạn giải phóng xe nhất quán; khách có thể liên hệ xin xác nhận lại và tạo link mới. Thanh toán đến sau khi đơn hết hạn phải chuyển nhân viên xử lý, không âm thầm hồi sinh đơn hoặc bỏ qua tiền đã nhận.
- Kiểm tra trùng lịch và trạng thái xe khi staff xác nhận, có xử lý cạnh tranh hai yêu cầu cùng xe. Yêu cầu mới chưa được xác nhận không tự khóa toàn bộ lịch xe vô thời hạn.

## Nội bộ tối thiểu

- Staff: danh sách yêu cầu, tìm/lọc, ghi chú và một trang chi tiết gồm khách/xe/giao nhận/giấy tờ cần thiết/thanh toán. Trạng thái luồng thuê: Mới / Đã xác nhận / Đang thuê / Hoàn tất / Hủy; “Đã liên hệ” là dấu việc đã làm. Cọc là nhãn riêng Chưa đến bước cọc / Chờ cọc / Đã cọc / Cần xử lý, không nhân số trạng thái thuê với mọi trạng thái thanh toán. Enum và dữ liệu cũ cần ánh xạ trước khi chuyển.
- Admin: các chức năng staff và quản lý xe/ảnh/giá/ẩn-hiện, địa điểm xuất bản, tài khoản nhân viên, thông tin liên hệ cửa hàng. Cần lưu thật vào database, bỏ dữ liệu demo khỏi luồng sử dụng thật.
- Tình trạng xe chỉ cần cho thuê/tạm ngừng (ví dụ bảo dưỡng), chưa cần hệ thống bảo dưỡng riêng. Xác nhận xe thủ công; nếu có dữ liệu xe cụ thể và lịch nhận-trả thì kiểm tra xung đột trước xác nhận, không hứa giữ xe tự động khi mới gửi yêu cầu.
- Số liệu tối thiểu: yêu cầu mới, đã liên hệ, đã xác nhận, cọc thực nhận; không gọi tổng giá yêu cầu là doanh thu. Cọc được hoàn/hủy cần theo dõi xử lý rõ ràng; không báo đã hoàn tiền khi chưa có giao dịch/thao tác được xác nhận.

## Bỏ khỏi phiên bản mục tiêu

- Đăng ký/đăng nhập khách bắt buộc, hồ sơ khách và lịch sử nhiều màn hình, OTP/2FA khách và đổi mật khẩu khách. Thay bằng trang đơn riêng, không bỏ đăng nhập/phân quyền staff/admin.
- Hợp đồng điện tử dài trong bước gửi yêu cầu, gia hạn tự động, đánh giá chưa hoàn thiện. Giữ điều kiện thuê/cọc dễ đọc và có thể mở chi tiết.
- Cơ chế tự hủy yêu cầu ba phút kể từ khi tạo. Thay bằng thời hạn giữ xe/thanh toán sau xác nhận như mô tả trên; KHÔNG bỏ PayOS, webhook hoặc OCR.
- AI/chatbot mô phỏng, điểm/voucher, nhiều trạm, báo cáo và bảo dưỡng nâng cao.
- Trang, endpoint, DTO, entity/repository và thư viện chỉ phục vụ phần đã bỏ; xử lý theo phụ thuộc, không xóa nhầm thành phần đang dùng cho đăng nhập nội bộ.
- Số liệu quảng cáo, đánh giá và ảnh minh họa không phản ánh hoạt động thật.

Không xóa dữ liệu nghiệp vụ cũ hoặc DROP bảng trong bước dọn code. Cần bản sao lưu và phương án chuyển dữ liệu; dữ liệu cũ có thể giữ ngoài luồng mới. Không dựa vào ddl-auto để thực hiện chuyển đổi phá hủy.

## Dữ liệu địa điểm đã kiểm tra

Nguồn người dùng: C:/Users/THINKPAD789/Downloads/places_VN_Đà Nẵng.json (24.931.314 byte). Đây là JSON danh mục, chưa phải schema database ứng dụng.

- 32.740 bản ghi; không có nhóm trùng place_id.
- food 22.431; stay 6.822; attraction 124; các nhóm khác và 2.597 bản ghi thiếu nhóm.
- 465 đóng cửa vĩnh viễn; 146 đóng cửa tạm thời; còn có trạng thái thiếu/chưa rõ/chưa mở.
- 13.344 thiếu điện thoại; 7 thiếu tọa độ; 8.513 thiếu rating.
- Có bản ghi gắn admin1 Quảng Ngãi, Huế và nơi khác; province không đủ để lọc khu vực phục vụ.
- Có tên, địa chỉ, điện thoại/website tùy bản ghi, link Google Maps, tọa độ, phân loại, rating/số lượt đánh giá và ngày crawl/cập nhật.
- Chỉ có photo_count, không có URL ảnh, mô tả du lịch, giá phòng hay giờ mở cửa. Không tự suy ra dữ liệu này. Ngày crawl không đảm bảo thông tin còn đúng.

Đề xuất import có kiểm tra, chạy lại không nhân bản theo place_id; tách trạng thái hoạt động nguồn và trạng thái xuất bản nội bộ. Mặc định là bản nháp, loại các bản ghi không phù hợp khỏi danh sách đề cử và ghi lý do. Chọn khoảng 30–50 nơi để biên tập trước, không đăng tự động toàn bộ. Nơi chưa có ảnh dùng hình thay thế trung tính; bổ sung ảnh thật được phép sử dụng, không gán ảnh địa điểm khác. Xác định khu vực phục vụ bằng vùng/tọa độ và kiểm tra địa chỉ; không chỉ dựa tên tỉnh.

Không ưu tiên nhập/hiển thị rating ở bản đầu. Nếu sử dụng nội dung từ nguồn ngoài, phải kiểm tra điều kiện sử dụng và ghi nguồn phù hợp trước xuất bản. Không xây đặt phòng hoặc đồng bộ phòng trống.

## Mô hình dữ liệu mục tiêu

- Tài khoản nội bộ: tận dụng User với STAFF/ADMIN, không mở đăng ký công khai.
- Xe và ảnh xe: tái sử dụng dữ liệu hiện có; khách chọn mẫu, nhân viên xác nhận xe cụ thể.
- Yêu cầu thuê: thông tin khách nhập, xe, thời gian/địa chỉ, trạng thái, ghi chú, người xử lý và thời điểm; ghi nguồn giới thiệu nếu có.
- Không lấy customer/staff đầu tiên trong database. Staff lấy từ phiên đăng nhập. Đơn khách vãng lai chứa thông tin liên hệ riêng; kiểm tra khóa ngoại customer hiện tại để chuyển có kiểm soát, không tạo một tài khoản CUSTOMER chung cho mọi khách.
- Báo giá/cọc/giao dịch: tổng tiền, phí giao, tiền cọc phải thu, cọc đã nhận, số dư còn lại, hạn giữ xe, mã giao dịch nhà cung cấp, trạng thái xử lý. Lưu giá đã chốt thay vì tính lại bằng giá xe có thể đổi sau này.
- Quyền truy cập đơn khách vãng lai và dữ liệu giấy tờ: thiết kế tối thiểu nhưng riêng tư, không dùng ID tuần tự làm quyền xem hoặc quyền trả tiền.
- Địa điểm và ảnh biên tập: thông tin nguồn, nhóm/khu vực, slug, trạng thái xuất bản, mô tả và ảnh do quản trị bổ sung.
- Cấu hình cửa hàng: liên hệ, địa chỉ, giờ hỗ trợ, điều kiện thuê/giao xe.

Không cần quan hệ địa điểm-xe phức tạp. Yêu cầu thuê chỉ cần tham chiếu địa điểm nguồn tùy chọn và chụp lại địa chỉ giao để không bị thay đổi khi nội dung địa điểm sửa.

## Thứ tự triển khai khi bắt đầu làm ứng dụng

1. Lập bản sao có thể khôi phục của source và dữ liệu cần giữ; kiểm tra các thay đổi hiện có. Viết inventory phụ thuộc và chiến lược chuyển dữ liệu. Cập nhật tài liệu bàn giao ngay từ đầu.
2. Làm luồng khách vãng lai xem xe -> form Maps/OCR có dự phòng -> lưu yêu cầu -> trang đơn riêng -> staff nhận/xử lý. Kiểm tra dữ liệu đầu vào, ngày thuê, quyền, chống gửi trùng và spam cơ bản. Giữ đường chạy cũ cho tới khi đường mới hoạt động, tránh xóa trước rồi mới nối lại.
3. Nối xác nhận xe/báo giá -> cọc PayOS -> webhook được kiểm chứng -> giao/nhận xe. Kiểm tra cạnh tranh giữ xe, callback lặp/sai/đến muộn, hủy/thử thanh toán lại. Không tạo thanh toán thật để thử nghiệm khi chưa có yêu cầu phù hợp; dùng môi trường thử hoặc mock và ghi rõ giới hạn.
4. Rút gọn UI khách/nội bộ theo luồng mới, quản trị xe/giá và địa điểm phải lưu thật. Khách không cần nhìn thấy kiến trúc/backend hoặc các nhãn kỹ thuật.
5. Xây importer địa điểm có xem trước và báo cáo; làm trang khám phá/chi tiết và liên kết sang thuê xe cho đủ ba nhóm du lịch, khách sạn, ăn uống. Không tự xuất bản danh mục thô. Nội dung chưa xác minh giữ bản nháp.
6. Gỡ module thừa theo phụ thuộc; dọn menu/link, cấu hình, dependency, dữ liệu demo trong UI và tài liệu lỗi thời. Giữ thư viện/API Maps, OCR, PayOS và bảo mật nội bộ còn dùng. Không tự xóa dữ liệu cũ hoặc file cá nhân.
7. Kiểm thử xuyên suốt, build, mobile và quyền; cập nhật hướng dẫn chạy/chuyển dữ liệu. Nếu không có credentials hay sandbox ngoài, ghi rõ phần đã kiểm bằng mock và phần chưa kiểm trực tiếp, không báo hoàn tất tích hợp thực tế.

## Điều kiện hoàn thành

- Khách xem xe/địa điểm và gửi yêu cầu trên điện thoại không cần tài khoản; thanh toán cọc có ở bước sau xác nhận, không bắt trả tiền khi chỉ hỏi xe.
- Maps chọn vị trí và địa chỉ nhập tay đều dùng được; quét CCCD điền được dữ liệu, có sửa và nhập tay; thông tin cần thiết được lưu đúng, không công khai giấy tờ.
- Khách mở trang đơn bằng quyền truy cập hợp lệ, không thể xem đơn khác bằng đổi ID. Staff/admin có kiểm tra quyền cả trang và API.
- Link thanh toán dùng giá server; sửa query return URL không đổi trạng thái cọc; webhook sai/lặp/đến muộn được kiểm chứng. Hết hạn, hủy và thanh toán lại có trạng thái rõ, không thu trùng hoặc tự mất đơn.
- Lỗi lưu không hiện thông báo thành công; yêu cầu hợp lệ xuất hiện thật ở staff và giữ sau tải lại.
- Khách vô danh không truy cập API/nội dung nội bộ; staff không sửa các phần chỉ dành admin.
- Xe/địa điểm ẩn không công khai; importer chạy lại không trùng; địa chỉ chỗ ở điền sẵn đúng nhưng cho phép chỉnh.
- Du lịch, khách sạn và ăn uống đều có trang giới thiệu và đường dẫn hợp lý sang thuê xe. Nguồn địa điểm theo được đến yêu cầu; CTA điểm tham quan không tự chọn sai nơi giao.
- Không tải JSON 25MB xuống trình duyệt; danh sách tìm kiếm/phân trang ở server.
- Không xuất bản số liệu mẫu, nội dung/ảnh sai địa điểm hoặc khẳng định đã giữ xe khi chưa xác nhận.
- Ghi nhận nguồn yêu cầu để xem khám phá có tạo liên hệ/đơn xác nhận hay không; không hứa đảm bảo thu hút khách.
- Có tài liệu chạy, chuyển dữ liệu và HANDOFF cập nhật, phân biệt rõ đã làm/chưa làm/đã kiểm thử.

## Bàn giao liên tục

docs/HANDOFF.md được cập nhật sau mỗi mốc, trước khi kết thúc phiên hoặc gần giới hạn ngữ cảnh, không chờ phút cuối. Ghi yêu cầu đã duyệt, file thay đổi, dữ liệu chuyển, lệnh chạy/test và kết quả thật, việc dở dang, lỗi còn lại, bước tiếp theo và giới hạn cần giữ. Không ghi secret vào tài liệu.

Hướng dẫn dùng với Gemini/chat khác và mẫu cập nhật: docs/CONTINUE-WITH-ANOTHER-CHAT.md. Việc đang nằm trong kế hoạch không phải bằng chứng đã triển khai.
