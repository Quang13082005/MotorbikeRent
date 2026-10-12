# 05 - KIỂM TOÁN CƠ SỞ DỮ LIỆU (DATABASE AUDIT)

Báo cáo kiểm toán chi tiết lược đồ dữ liệu, ràng buộc toàn vẹn, tính nhất quán thực thể JPA và hiệu năng truy vấn của hệ thống cơ sở dữ liệu MySQL 9.0.1.

---

## 1. Tổng Quan Lược Đồ Cơ Sở Dữ Liệu (Database Schemas Overview)
Hệ thống sử dụng hai lược đồ cơ sở dữ liệu:
1. `motorent_db`: Cơ sở dữ liệu chính (Production/Default schema).
2. `motorent_verify_20260926`: Cơ sở dữ liệu bản sao cách ly chuyên dùng cho kiểm thử tự động, kiểm thử tranh chấp đồng thời (concurrency) và xác minh hồi quy.

### Danh sách các bảng dữ liệu (19 Tables):
1. `users`: Tài khoản người dùng (Khách hàng, Nhân viên, Quản trị viên).
2. `token_authentication`: Quản lý vòng đời và thu hồi token JWT.
3. `otp_tokens`: Lưu trữ mã OTP xác thực email khi quên mật khẩu.
4. `user_documents`: Quản lý tài liệu tùy thân (CCCD, GPLX) của người dùng.
5. `motorbike_brands`: Thương hiệu xe máy (Honda, Yamaha, Vespa...).
6. `motorbike_categories`: Phân loại xe (Xe số, Xe ga, Xe côn) kèm cấu hình tiền cọc mặc định.
7. `motorbikes`: Quản lý thông tin từng chiếc xe (Biển số, số khung, phân khối, số km, trạng thái xe).
8. `motorbike_images`: Hình ảnh thư viện xe (ảnh chính, ảnh chi tiết).
9. `maintenance_records`: Lịch sử bảo dưỡng và chi phí sửa chữa xe.
10. `shop_settings`: Cấu hình thông tin cửa hàng, tọa độ định vị, đơn giá giao xe theo km, % cọc.
11. `rental_orders`: Đơn thuê xe, giá tiền, cọc giữ xe, địa chỉ và tọa độ giao nhận.
12. `rental_contracts`: Hợp đồng điện tử, chữ ký, tình trạng xe trước/sau, số km trước/sau, quyết toán hoàn cọc/khấu trừ.
13. `rental_vehicle_changes`: Lịch sử phương án đổi xe (trước và trong khi thuê).
14. `rental_extensions`: Lịch sử các yêu cầu và quyết định gia hạn thời gian thuê xe.
15. `payments`: Sổ cái ghi nhận dòng tiền hợp nhất (`IN`/`OUT`, PayOS/Tiền mặt/Chuyển khoản, chi tiết components).
16. `reviews`: Đánh giá của khách hàng về xe và dịch vụ sau khi hoàn tất đơn.
17. `notifications`: Thông báo trong hệ thống gửi tới người dùng.
18. `travel_places`: Danh mục các địa điểm du lịch tại Đà Nẵng và lân cận phục vụ gợi ý hành trình.
19. `guest_bookings`: Bảng lưu tạm thông tin đặt xe của khách vãng lai trước khi đăng ký tài khoản.

---

## 2. Đối Chiếu Thực Thể JPA và Lược Đồ MySQL (Entity-to-Schema Mapping)

| Entity Java | Bảng MySQL tương ứng | Khóa chính (PK) | Ràng buộc duy nhất (Unique) | Đánh giá tính toàn vẹn |
|---|---|---|---|:---:|
| `User` | `users` | `user_id` (varchar 36 UUID) | `email`, `username` | **TỐT** |
| `Motorbike` | `motorbikes` | `motorbike_id` (bigint auto) | `license_plate` | **TỐT** |
| `RentalOrder` | `rental_orders` | `order_id` (bigint auto) | `order_code` | **TỐT** |
| `RentalContract` | `rental_contracts` | `contract_id` (bigint auto) | `order_id`, `contract_code` | **TỐT** |
| `RentalExtension` | `rental_extensions` | `id` (bigint auto) | Không | **TỐT** |
| `RentalVehicleChange` | `rental_vehicle_changes` | `id` (bigint auto) | Không | **TỐT** |
| `Payment` | `payments` | `payment_id` (bigint auto) | `provider_code` (PayOS) | **TỐT** |
| `TokenAuthentication` | `token_authentication` | `token_id` (bigint auto) | `token_value` | **TỐT** |
| `ShopSettings` | `shop_settings` | `id` (bigint auto) | Bản ghi duy nhất (id=1) | **TỐT** |

---

## 3. Kiểm Tra Toàn Vẹn Dữ Liệu & Rủi Ro Tiềm Ẩn (Data Integrity & Concurrency Risks)

### 3.1. Chống đặt trùng lịch xe (Double Booking Prevention)
- **Cơ chế triển khai:**
  - `AvailabilityService` sử dụng câu lệnh `SELECT ... FOR UPDATE` (Pessimistic Write Lock) trên bảng `motorbikes` theo thứ tự ID xe tăng dần (`lockVehiclesAscending`).
  - Kiểm tra giao thoa khoảng thời gian thuê: `[plannedPickup, plannedReturn)` giao nhau với bất kỳ đơn thuê nào đang hoạt động (`PENDING_PAYMENT` có giữ chỗ, `PENDING`, `CONFIRMED`, `HANDED_OVER`, `EXTENDED`, `OVERDUE`).
  - Đã được chứng minh bằng 20 kịch bản kiểm thử đa luồng chạy song song trên MySQL thật (`Phase1ConcurrencyTests`).

### 3.2. Toàn vẹn sổ cái thanh toán (Payment Ledger Integrity)
- Bảng `payments` không cho phép sửa đổi dòng tiền đã ghi nhận. Khi có phát sinh hoàn tiền hoặc đổi xe:
  - Hệ thống tạo bản ghi mới với chiều `OUT` và `transaction_type` rõ ràng (`BOOKING_REFUND`, `ASSET_DEPOSIT_REFUND`, `EARLY_RETURN_REFUND`...).
  - Mọi bản ghi tiền mặt hoặc chuyển khoản đều lưu trữ thông tin kiểm toán: `processed_by_staff_id`, `processed_at`, `note`, `payment_method`.

### 3.3. Dữ liệu mồ côi và Ràng buộc khóa ngoại (Foreign Key Constraints)
- Tất cả các quan hệ cốt lõi đều có khóa ngoại chặt chẽ:
  - `payments.order_id` -> `rental_orders.order_id`
  - `rental_contracts.order_id` -> `rental_orders.order_id`
  - `rental_vehicle_changes.order_id` -> `rental_orders.order_id`
  - `rental_orders.motorbike_id` -> `motorbikes.motorbike_id`
  - `rental_orders.customer_id` -> `users.user_id`
- Không có hiện tượng dữ liệu mồ côi (Orphan records) giữa các bảng.

---

## 4. Đánh Giá Chỉ Mục (Index Audit) & Kiến Nghị Tối Ưu

### 4.1. Các chỉ mục hiện có:
- Primary Key indexes trên tất cả các bảng.
- Unique Index trên `users.email`, `users.username`, `motorbikes.license_plate`, `rental_orders.order_code`, `rental_contracts.order_id`.
- Foreign Key indexes tự động tạo bởi MySQL trên các cột tham chiếu.

### 4.2. Kiến nghị bổ sung Composite Index để tối ưu truy vấn:
1. **Bảng `rental_orders` (Tìm kiếm xe trống theo khoảng thời gian):**
   ```sql
   CREATE INDEX idx_orders_bike_dates_status 
   ON rental_orders(motorbike_id, pickup_date, return_date, status);
   ```
   *Mục đích:* Tăng tốc độ truy vấn kiểm tra trùng lịch xe (`AvailabilityService.available()`) khi bảng đơn hàng tăng lên hàng trăm nghìn dòng.

2. **Bảng `payments` (Báo cáo doanh thu & Dòng tiền theo ngày):**
   ```sql
   CREATE INDEX idx_payments_paidat_status_direction 
   ON payments(paid_at, status, direction);
   ```
   *Mục đích:* Tối ưu hóa truy vấn tính toán báo cáo dòng tiền theo khoảng ngày trong `OperationsService.reports()`.

3. **Bảng `travel_places` (Dung lượng lớn 31,992 dòng):**
   - Bảng này hiện chứa gần 32,000 địa điểm du lịch, chiếm phần lớn dung lượng database (khoảng 27MB).
   - Kiến nghị bổ sung Fulltext Index trên cột tên địa điểm và danh mục nếu có chức năng tìm kiếm du lịch:
   ```sql
   CREATE FULLTEXT INDEX idx_travel_places_search ON travel_places(title, address);
   ```
