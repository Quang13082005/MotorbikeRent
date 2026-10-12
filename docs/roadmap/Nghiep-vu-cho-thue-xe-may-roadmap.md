# Nghiệp vụ cho thuê xe máy

```jsx
╔══════════════════════════════════════════════════════════════════════╗
║              ROADMAP TOÀN BỘ QUY TRÌNH THUÊ XE MÁY                 ║
╚══════════════════════════════════════════════════════════════════════╝

CUSTOMER
   │
   ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 1A. NHẬP NHU CẦU THUÊ XE                                            │
│                                                                     │
│ Khi Customer vào hệ thống, trước tiên chọn:                         │
│                                                                     │
│ - Ngày/giờ nhận xe                                                  │
│ - Ngày/giờ trả xe                                                   │
│ - Vị trí / địa điểm giao xe                                         │
│ - Địa điểm trả xe nếu có                                            │
│                                                                     │
│ Sau đó bấm:                                                         │
│                                                                     │
│ [ TÌM XE ]                                                          │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 1B. BACKEND KIỂM TRA AVAILABILITY                                   │
│                                                                     │
│ Backend lấy khoảng thời gian Customer yêu cầu:                      │
│                                                                     │
│ requestedPickupTime                                                 │
│          ↓                                                          │
│ requestedReturnTime                                                 │
│                                                                     │
│ Sau đó kiểm tra tất cả Rental / Booking hiện có để xác định         │
│ những xe KHÔNG bị overlap trong toàn bộ khoảng thời gian thuê.      │
│                                                                     │
│ Không được chỉ dựa vào:                                             │
│                                                                     │
│ vehicle.status = AVAILABLE                                          │
│                                                                     │
│ Vì một xe có thể AVAILABLE ở hiện tại nhưng đã có người đặt         │
│ trong khoảng thời gian Customer đang tìm kiếm.                      │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 1C. HỆ THỐNG LỌC DANH SÁCH XE CÒN TRỐNG                            │
│                                                                     │
│ XE BỊ TRÙNG LỊCH:                                                   │
│ → Không hiển thị cho Customer trong kết quả tìm kiếm.               │
│                                                                     │
│ XE KHÔNG BỊ TRÙNG LỊCH:                                            │
│ → Hiển thị để Customer lựa chọn.                                    │
│                                                                     │
│ Sau bước availability, Customer có thể tiếp tục lọc theo:           │
│                                                                     │
│ - xe số                                                             │
│ - xe ga                                                             │
│ - xe côn                                                            │
│ - hãng xe                                                           │
│ - mức giá                                                           │
│ - các tiêu chí khác nếu project đang hỗ trợ                         │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 2A. CUSTOMER CHỌN XE                                                │
│                                                                     │
│ Customer lúc này chỉ nhìn thấy các xe thực sự còn trống trong      │
│ khoảng thời gian đã chọn ở bước đầu.                                │
│                                                                     │
│ Customer:                                                          │
│                                                                     │
│ Chọn xe                                                             │
│    ↓                                                                │
│ Xem chi tiết xe                                                     │
│    ↓                                                                │
│ Chọn [LÀM HỢP ĐỒNG / THUÊ XE]                                      │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 2B. CUSTOMER NHẬP / XÁC NHẬN THÔNG TIN ĐẶT XE                      │
│                                                                     │
│ - Thông tin Customer                                                │
│ - Thông tin hợp đồng                                                │
│ - Phương án CCCD                                                    │
│ - Địa điểm giao xe                                                  │
│ - Địa điểm trả xe                                                   │
│ - Các thông tin cần thiết khác                                      │
│                                                                     │
│ Ngày/giờ nhận và trả xe được giữ từ bước tìm kiếm ban đầu.          │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 2C. BACKEND KIỂM TRA AVAILABILITY LẦN CUỐI                          │
│                                                                     │
│ Trước khi tạo Rental, Backend PHẢI kiểm tra lại chiếc xe            │
│ Customer vừa chọn trong khoảng:                                     │
│                                                                     │
│ plannedPickupTime                                                   │
│        →                                                            │
│ plannedReturnTime                                                   │
│                                                                     │
│ Mục đích:                                                           │
│ tránh trường hợp trong lúc Customer đang xem xe / điền hợp đồng,    │
│ một Customer khác đã đặt chiếc xe đó trước.                         │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                       ┌────────┴─────────┐
                       │                  │
                 ĐÃ BỊ ĐẶT           VẪN CÒN TRỐNG
                       │                  │
                       ▼                  ▼
              Thông báo Customer      Cho phép tiếp tục
              xe vừa bị đặt               │
                       │                  │
                       ▼                  │
              Quay lại danh sách          │
              xe còn trống                │
                                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 3. TẠO RENTAL / ĐƠN THUÊ                                           │
│                                                                     │
│ Hệ thống lưu:                                                       │
│ - customerId                                                        │
│ - vehicleId                                                         │
│ - plannedPickupTime                                                 │
│ - plannedReturnTime                                                 │
│ - địa điểm giao/trả                                                 │
│ - giá thuê                                                          │
│ - phí giao/trả                                                      │
│ - cọc tài sản dự kiến                                               │
│ - thông tin hợp đồng                                                │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 4. TÍNH TIỀN ĐẶT TRƯỚC / GIỮ XE                                   │
│                                                                     │
│ Booking Payment =                                                   │
│ % tiền thuê + phí giao/trả nếu có                                  │
│                                                                     │
│ LƯU Ý:                                                              │
│ Cọc tài sản KHÔNG nằm trong Booking Payment.                        │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 5. CUSTOMER THANH TOÁN PAYOS                                       │
│                                                                     │
│ Customer → PayOS                                                    │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                       ┌────────┴──────────┐
                       │                   │
                    FAILED             SUCCESS
                       │                   │
                       ▼                   ▼
               Đơn chưa hợp lệ      Webhook xác nhận
                                           │
                                           ▼
                              Ghi Transaction:
                              BOOKING_PAYMENT
                              direction = IN
                              paymentMethod = PAYOS
                                           │
                                           ▼
                                  CHỜ STAFF XỬ LÝ
                                           │
                                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 6. STAFF KIỂM TRA ĐƠN                                              │
│                                                                     │
│ Staff kiểm tra thực tế xe có thể phục vụ hay không.                 │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                      ┌─────────┴─────────┐
                      │                   │
                 XE PHỤC VỤ ĐƯỢC     XE KHÔNG PHỤC VỤ
                      │                   │
                      ▼                   ▼
                Chuẩn bị giao xe     Staff liên hệ Customer
                                          │
                                 ┌────────┴────────┐
                                 │                 │
                          Không thuê nữa      Muốn xe khác
                                 │                 │
                                 ▼                 ▼
                           HOÀN TIỀN       ĐỔI XE TRƯỚC GIAO
                                             BEFORE_DELIVERY

══════════════════════════ NHÁNH HOÀN TIỀN ══════════════════════════

Staff thực tế hoàn tiền cho Customer
            │
            ▼
Staff nhập:
- số tiền hoàn
- paymentMethod
- lý do
- ghi chú
            │
            ▼
Staff bấm:
[XÁC NHẬN ĐÃ HOÀN TIỀN]
            │
            ▼
Backend:
- Rental = REJECTED / CANCELLED
- ghi BOOKING_REFUND
- direction = OUT
- lưu staffId
- processedAt
            │
            ▼
ĐƠN KẾT THÚC

══════════════════════ NHÁNH ĐỔI XE TRƯỚC GIAO ═════════════════════

BEFORE_DELIVERY
            │
            ▼
Hệ thống giữ nguyên Rental hiện tại
KHÔNG tạo Rental mới
KHÔNG xóa BOOKING_PAYMENT cũ
            │
            ▼
Backend lấy:
plannedPickupTime
→
plannedReturnTime
            │
            ▼
Tìm danh sách xe không overlap toàn bộ khoảng thuê
            │
            ▼
Staff thấy:
- tên xe
- loại xe
- biển số
- giá/ngày
- tổng tiền
- mức cọc
- lịch trống
- chênh lệch
            │
            ▼
Staff gọi Customer và thống nhất xe mới
            │
            ▼
Backend CHECK AVAILABILITY LẠI
            │
            ▼
       ┌────┴─────────────────────────────────────┐
       │                                          │
 XE MỚI CÙNG GIÁ                             XE MỚI KHÁC GIÁ
       │                                          │
       ▼                                          ▼

Cùng giá                                    Tính lại TOÀN BỘ
→ giữ tiền đã trả                           thời gian thuê theo xe mới
       │                                          │
       │                               ┌──────────┴──────────┐
       │                               │                     │
       │                          XE RẺ HƠN            XE ĐẮT HƠN
       │                               │                     │
       │                               ▼                     ▼
       │                      Giảm remaining          Tính Booking Payment
       │                      hoặc có credit         mới cần phải trả
       │                               │                     │
       │                    Nếu đã trả dư              Nếu thiếu tiền:
       │                               │                     │
       │                               ▼                     ▼
       │                      Staff hoàn phần dư       Tạo PayOS phần chênh
       │                      và ghi transaction              │
       │                                                       ▼
       │                                                 PAYOS SUCCESS?
       │                                                  │          │
       │                                                NO          YES
       │                                                  │          │
       │                                          Chưa đổi xe       │
       │                                                             ▼
       └──────────────────────────────────────────────────────► UPDATE
                                                               vehicleId
                                                               giá Rental
                                                               amountPaid
                                                               history
                                                                    │
                                                                    ▼
                                                           Chuẩn bị giao xe

══════════════════════════ LUỒNG GIAO XE ════════════════════════════

Staff chuẩn bị giao xe
        │
        ▼
Staff xử lý ngoài thực tế:
- kiểm tra CCCD
- đối chiếu Customer
- kiểm tra xe
- thu tiền thuê còn lại
- thu cọc tài sản
        │
        ▼
Staff nhập vào hệ thống:
- remaining rental amount
- asset deposit amount
- paymentMethod
        │
        ▼
Staff bấm:
[ĐÃ GIAO XE]
        │
        ▼
Backend ghi:
- actualPickupTime
- deliveredByStaffId
- deliveredAt
        │
        ▼
Rental = RENTING
Vehicle = RENTED / IN_USE
        │
        ▼
Ghi Transaction:
- RENTAL_REMAINING
- ASSET_DEPOSIT
        │
        ▼
LƯU Ý:
ASSET_DEPOSIT KHÔNG PHẢI DOANH THU
        │
        ▼
══════════════════════════ CUSTOMER ĐANG THUÊ ═══════════════════════
        │
        ├────────────────────────────────────────────────────────────┐
        │                                                            │
        ▼                                                            ▼
   THUÊ BÌNH THƯỜNG                                             CÓ PHÁT SINH
                                                                     │
                    ┌────────────────────────────────────────────────┼──────────────┐
                    │                         │                      │              │
                    ▼                         ▼                      ▼              ▼
                 GIA HẠN                  ĐỔI XE               TRẢ SỚM       NHẬN SỚM

══════════════════════════ NHÁNH GIA HẠN ════════════════════════════

Customer:
[Gia hạn]
    │
    ▼
Chọn newPlannedReturnTime
    │
    ▼
Backend kiểm tra:
currentVehicle
oldReturnTime → newReturnTime
có overlap?
    │
 ┌──┴────────────┐
 │               │
NO              YES
 │               │
 ▼               ▼
Cho gửi       Không cho gia hạn xe hiện tại
yêu cầu           │
 │                ▼
 ▼         Hiển thị cảnh báo +
Staff review   Hotline liên hệ Staff
 │                │
 ▼                ▼
Staff approve  Có thể xử lý bằng
 │             ĐỔI XE TRONG LÚC THUÊ Hoặc không thuê nữa trả xe luôn 
 ▼
Extension =
WAITING_FOR_PAYMENT
 │
 ▼
Tạo PayOS phí gia hạn
 │
 ▼
PAYOS SUCCESS?
 │
 ├── NO
 │    └→ KHÔNG thay đổi ngày trả
 │
 └── YES
       │
       ▼
 ghi EXTENSION_PAYMENT
       │
       ▼
 update plannedReturnTime
       │
       ▼
 Extension = COMPLETED / PAID
       │
       ▼
 Tiếp tục RENTING

══════════════════ ĐỔI XE TRONG LÚC ĐANG THUÊ ═════════════════════

DURING_RENTAL
       │
       ▼
Staff và Customer liên hệ trực tiếp
       │
       ▼
Thống nhất:
- xe mới
- thời điểm đổi
- ngày trả mới
- nơi đổi
- phí hỗ trợ
- lý do
       │
       ▼
Backend tìm xe trống trong khoảng:

vehicleChangeTime
→
newPlannedReturnTime
       │
       ▼
Danh sách xe trống cho Staff
       │
       ▼
Customer chọn xe
       │
       ▼
Backend CHECK LẠI availability
       │
       ▼
TÍNH TIỀN:

Phần đã dùng xe cũ
→ GIỮ NGUYÊN

Phần thời gian tiếp theo
→ TÍNH THEO GIÁ XE MỚI

+
Vehicle Change Fee nếu có
       │
       ▼
Ví dụ:

Extra Rental   360k
Change Fee     100k
-------------------
PayOS          460k
       │
       ▼
PAYOS SUCCESS?
       │
 ┌─────┴───────┐
 │             │
NO            YES
 │             │
 ▼             ▼
Không cập    cập nhật phần gia hạn /
nhật gì      kế hoạch đổi xe
               │
               ▼
           Staff thực tế:
           - đem xe mới
           - nhận xe cũ
           - kiểm tra xe cũ
           - giao xe mới
               │
               ▼
           Staff bấm:
           [XÁC NHẬN ĐÃ ĐỔI XE]
               │
               ▼
           backend lưu:
           actualVehicleChangeTime
           oldVehicleId
           newVehicleId
           staffId
               │
               ▼
           Xe cũ → AVAILABLE
           Xe mới → RENTED
               │
               ▼
           Tiếp tục RENTING
```

```jsx
══════════════════════════ NHÁNH TRẢ XE SỚM ═════════════════════════

Customer:
[YÊU CẦU TRẢ XE SỚM]
       │
       ▼
Website:
"Vui lòng liên hệ cửa hàng..."
       │
       ▼
Staff + Customer trao đổi
       │
       ▼
Staff quyết định:
Có refund không?
       │
 ┌─────┴─────────┐
 │               │
NO              YES
 │               │
 ▼               ▼
refund = 0    Staff nhập:
             refundAmount
             reason
             paymentMethod
                  │
                  ▼
             Staff thực tế hoàn tiền
                  │
                  ▼
             EARLY_RETURN_REFUND
             direction = OUT
                  │
           ┌──────┘
           │
           ▼
Staff nhận xe thực tế
       │
       ▼
Kiểm tra xe / cọc / phát sinh
       │
       ▼
Hoàn tất đơn
       │
       ▼
actualReturnTime
Rental = COMPLETED
Vehicle = AVAILABLE
```

```jsx
══════════════════════════ NHÁNH NHẬN XE SỚM ════════════════════════

Customer muốn nhận xe sớm
       │
       ▼
Staff nhận yêu cầu
       │
       ▼
Backend kiểm tra:
newPickupTime → plannedReturnTime
có overlap không?
       │
 ┌─────┴──────┐
 │            │
YES          NO
 │            │
 ▼            ▼
Không       Staff đồng ý
cho nhận       │
               ▼
       Có phí nhận sớm?
          │          │
         NO         YES
          │          │
          │     Ghi phí phát sinh
          │          │
          └─────┬────┘
                ▼
         Staff giao xe thực tế
                │
                ▼
         [ĐÃ GIAO XE]
                │
                ▼
         actualPickupTime = thời gian thực tế
                │
                ▼
         RENTING
```

```jsx
══════════════════════════ TRẢ XE CUỐI RENTAL ═══════════════════════

                         CUSTOMER TRẢ XE
                               │
                    ┌──────────┼───────────┐
                    │          │           │
                    ▼          ▼           ▼
                 ĐÚNG HẠN    TRẢ SỚM     TRẢ TRỄ
                    │          │           │
                    │          │           ▼
                    │          │     currentTime >
                    │          │     plannedReturnTime
                    │          │           │
                    │          │           ▼
                    │          │     Có extension hợp lệ?
                    │          │        │          │
                    │          │       YES        NO
                    │          │        │          │
                    │          │        │          ▼
                    │          │        │     LATE RETURN
                    │          │        │          │
                    │          │        │     Staff liên hệ
                    │          │        │          │
                    │          │        │     nhập lateFee
                    │          │        │     reason
                    │          │        │     paymentMethod
                    │          │        │          │
                    │          │        │          ▼
                    │          │        │     LATE_FEE
                    │          │        │     direction = IN
                    │          │        │
                    └──────────┴────────┴──────────────┐
                                                      ▼
                                              STAFF NHẬN XE
                                                      │
                                                      ▼
                                            Kiểm tra xe thực tế
                                                      │
                                                      ▼
                                            Xử lý phí phát sinh
                                                      │
                                                      ▼
                                            XỬ LÝ CỌC TÀI SẢN
```

```jsx
ASSET DEPOSIT
     │
     ▼
Cọc ban đầu
     │
     ▼
Có khấu trừ không?
     │
 ┌───┴────────┐
 │            │
NO           YES
 │            │
 ▼            ▼
Hoàn toàn   Staff nhập:
bộ cọc      - originalDeposit
             - deductionAmount
             - refundAmount
             - reason
                │
                ▼
        Ví dụ:

        Deposit        2.000.000
        Deduction        300.000
        ------------------------
        Refund         1.700.000
                │
                ▼
        Transactions:
        ASSET_DEPOSIT_REFUND
        OUT 1.700.000

        DEPOSIT_DEDUCTION
        300.000
                │
                ▼
```

```jsx
════════════════════════════ HOÀN TẤT RENTAL ════════════════════════

Staff đã:
- nhận xe
- kiểm tra xe
- thu phí phát sinh nếu có
- hoàn cọc nếu có
- xử lý refund nếu có
       │
       ▼
Staff bấm:
[HOÀN TẤT ĐƠN]
       │
       ▼
Backend:
actualReturnTime = now
completedByStaffId = staff
completedAt = now

Rental = COMPLETED

Vehicle = AVAILABLE
       │
       ▼
Customer
       │
       ▼
Đơn chuyển vào LỊCH SỬ
       │
       ▼
Customer được:
[ĐÁNH GIÁ]
       │
       ▼
Rating
       │
       ▼
════════════════════════════ ADMIN ══════════════════════════════════
```

```jsx

                         GIAO DỊCH / DÒNG TIỀN
                                │
           ┌────────────────────┼────────────────────┐
           │                    │                    │
           ▼                    ▼                    ▼
      TIỀN THU VÀO         TIỀN CHI / HOÀN       TIỀN CỌC
           │                    │                    │
           │                    │                    │
           ▼                    ▼                    ▼

   TIỀN ĐẶT TRƯỚC         HOÀN TIỀN ĐẶT TRƯỚC    CỌC TÀI SẢN
   GIỮ XE                  BOOKING_REFUND          ASSET_DEPOSIT

   BOOKING_PAYMENT
           │
           ▼

   TIỀN THUÊ CÒN LẠI      HOÀN PHẦN CHÊNH LỆCH   CỌC THU THÊM
   KHI GIAO XE             KHI ĐỔI XE              KHI ĐỔI XE
   RENTAL_REMAINING        BOOKING_ADJUSTMENT_     ASSET_DEPOSIT_
                          REFUND                   ADDITIONAL

           │                    │                    │
           ▼                    ▼                    ▼

   TIỀN GIA HẠN           HOÀN TIỀN DO            HOÀN CỌC
   EXTENSION_PAYMENT      TRẢ XE SỚM              ASSET_DEPOSIT_
                          EARLY_RETURN_REFUND      REFUND

           │
           ▼

   PHÍ GIAO XE
   DELIVERY_FEE

           │
           ▼

   PHÍ NHẬN / TRẢ XE
   RETURN_FEE

           │
           ▼

   PHÍ ĐỔI XE
   VEHICLE_CHANGE_FEE

           │
           ▼

   PHÍ TRẢ XE TRỄ
   LATE_FEE

           │
           ▼

   KHOẢN KHẤU TRỪ TỪ CỌC(có thể là gây hư hại xe, trả trễ)
   DEPOSIT_DEDUCTION

           └────────────────────┬────────────────────┘
                                │
                                ▼
                              ADMIN
                                │
                                ▼
                       BÁO CÁO TÀI CHÍNH
                                │
              ┌─────────────────┼─────────────────┐
              │                 │                 │
              ▼                 ▼                 ▼
          DOANH THU         TIỀN HOÀN          TIỀN CỌC
              │                 │                 │
              │                 │                 │
              ▼                 ▼                 ▼

        Tiền thuê xe       Hoàn tiền đặt xe     Cọc đã thu

        Phí giao xe        Hoàn do trả sớm      Cọc đang giữ

        Phí nhận/trả xe    Hoàn chênh lệch      Cọc đã hoàn
                           khi đổi xe
        Tiền gia hạn

        Phí đổi xe

        Phí trả trễ

        Khoản khấu trừ
        hợp lệ từ cọc

              │
              ▼

                  DOANH THU THỰC TẾ
                        / NET REVENUE
                             =
                      Tiền thuê xe
                             +
                      Phí giao / trả xe
                             +
                      Tiền gia hạn
                             +
                      Phí đổi xe
                             +
                      Phí trả xe trễ
                             +
                      Các khoản thu hợp lệ
                             -
                      Các khoản hoàn tiền

        CỌC TÀI SẢN KHÔNG ĐƯỢC TÍNH VÀO DOANH THU
```

```

```
