# 06 - KIỂM TOÁN DANH MỤC API (API ENDPOINTS AUDIT)

Báo cáo kiểm toán toàn diện danh mục các REST API trong hệ thống MotoRent, bao gồm phương thức HTTP, đường dẫn endpoint, cơ chế xác thực, vai trò yêu cầu, cấu trúc dữ liệu và kết quả kiểm thử runtime.

---

## 1. Nhóm API Xác Thực & Tài Khoản (Authentication & Account APIs)

| Phương thức | Endpoint | Xác thực | Quyền hạn | Request Body / Params | Kết quả Runtime | Đánh giá |
|:---:|---|:---:|:---:|---|:---:|:---:|
| `POST` | `/api/auth/login` | Public | None | `{"username": "...", "password": "..."}` | `200 OK` (trả về JWT token + set cookie) | **PASS** |
| `POST` | `/api/auth/register` | Public | None | `{"username", "email", "password", "phone", ...}` | `200 OK` (tạo user, role CUSTOMER) | **PASS** |
| `POST` | `/api/auth/forgot-password` | Public | None | `{"email": "..."}` | `200 OK` (gửi mã OTP qua email) | **PASS** |
| `POST` | `/api/auth/verify-otp` | Public | None | `{"email": "...", "otp": "..."}` | `200 OK` | **PASS** |
| `POST` | `/api/auth/reset-password` | Public | None | `{"email": "...", "otp": "...", "newPassword": "..."}` | `200 OK` | **PASS** |
| `GET` | `/api/account` | Yêu cầu | Authenticated | None | `200 OK` (trả về thông tin cá nhân) | **PASS** |
| `PUT` | `/api/account` | Yêu cầu | Authenticated | `Profile` DTO (họ tên, email, sđt, địa chỉ, ngày sinh) | `200 OK` | **PASS** |
| `POST` | `/api/account/password` | Yêu cầu | Authenticated | `Password` DTO (current, new, confirm) | `200 OK` (thu hồi token cũ) | **PASS** |
| `POST` | `/api/account/avatar` | Yêu cầu | Authenticated | `MultipartFile file` (ảnh <= 5MB) | `200 OK` (lưu ảnh vào `/uploads/avatars/`) | **PASS** |

---

## 2. Nhóm API Thuê Xe & Đơn Hàng (Rental Orders APIs)

| Phương thức | Endpoint | Xác thực | Quyền hạn | Mô tả chức năng & Dữ liệu | Kết quả Runtime | Đánh giá |
|:---:|---|:---:|:---:|---|:---:|:---:|
| `POST` | `/api/rental-orders/quote` | Yêu cầu | `CUSTOMER` | Tính toán tiền thuê, cọc giữ xe, phí giao nhận | `200 OK` (400 nếu ngày quá khứ) | **PASS** |
| `POST` | `/api/rental-orders` | Yêu cầu | `CUSTOMER` | Tạo đơn thuê xe mới kèm thông tin giấy tờ | `200 OK` (chuyển sang `PENDING_PAYMENT`) | **PASS** |
| `GET` | `/api/rental-orders/my-orders`| Yêu cầu | `CUSTOMER` | Lấy danh sách đơn thuê của chính khách hàng | `200 OK` | **PASS** |
| `GET` | `/api/rental-orders/{id}` | Yêu cầu | `CUSTOMER`/`OPERATOR` | Chi tiết đơn thuê (chỉ chủ đơn hoặc Staff xem được) | `200 OK` (403 nếu IDOR) | **PASS** |
| `GET` | `/api/rental-orders/pending` | Yêu cầu | `STAFF`/`ADMIN` | Lấy danh sách đơn chờ duyệt (`PENDING`) | `200 OK` | **PASS** |
| `POST` | `/api/rental-orders/{id}/confirm` | Yêu cầu | `STAFF`/`ADMIN` | Nhân viên xác nhận tiếp nhận đơn | `200 OK` (chuyển sang `CONFIRMED`) | **PASS** |
| `POST` | `/api/rental-orders/{id}/reject` | Yêu cầu | `STAFF`/`ADMIN` | Từ chối đơn kèm lý do (`reason`) | `200 OK` (chuyển sang `REJECTED`) | **PASS** |
| `POST` | `/api/rental-orders/{id}/hand-over`| Yêu cầu | `STAFF`/`ADMIN` | Bàn giao xe cho khách kèm thu cọc tài sản | `200 OK` (chuyển sang `HANDED_OVER`) | **PASS** |
| `POST` | `/api/rental-orders/{id}/complete` | Yêu cầu | `STAFF`/`ADMIN` | Hoàn tất đơn, hoàn cọc tài sản, trừ hư hỏng | `200 OK` (chuyển sang `COMPLETED`) | **PASS** |
| `POST` | `/api/rental-orders/{id}/early-return`| Yêu cầu | `CUSTOMER` | Khách yêu cầu trả sớm | `200 OK` (chuyển `RETURN_REQUESTED`) | **PASS** |
| `POST` | `/api/rental-orders/{id}/cancel` | Yêu cầu | `CUSTOMER` | Khách hàng hủy đơn (khi chưa thanh toán) | `200 OK` (chuyển sang `CANCELLED`) | **PASS** |

---

## 3. Nhóm API Thanh Toán & PayOS (Payment APIs)

| Phương thức | Endpoint | Xác thực | Quyền hạn | Mô tả chức năng | Kết quả Runtime | Đánh giá |
|:---:|---|:---:|:---:|---|:---:|:---:|
| `POST` | `/api/payment/create/{id}` | Yêu cầu | `CUSTOMER` | Tạo link thanh toán PayOS cọc giữ xe | `200 OK` (trả về URL checkout PayOS) | **PASS** |
| `POST` | `/api/payment/vehicle-change/{id}`| Yêu cầu | `CUSTOMER` | Tạo link thanh toán PayOS đổi xe trước giao | `200 OK` | **PASS** |
| `POST` | `/api/payment/during-change/{id}` | Yêu cầu | `CUSTOMER` | Tạo link thanh toán PayOS đổi xe khi đang thuê | `200 OK` | **PASS** |
| `POST` | `/api/payment/extension/{id}` | Yêu cầu | `CUSTOMER` | Tạo link thanh toán PayOS gia hạn hợp đồng | `200 OK` | **PASS** |
| `POST` | `/api/payment/reconcile/{id}` | Yêu cầu | `CUSTOMER`/`STAFF` | Đối soát thủ công giao dịch với PayOS | `200 OK` (cập nhật nếu đã thanh toán) | **PASS** |
| `POST` | `/api/payment/payos-webhook` | Public | None (Bảo vệ bằng chữ ký SHA-256) | Nhận webhook thông báo thanh toán từ PayOS | `200 OK` (400 nếu sai chữ ký số) | **PASS** |
| `GET` | `/api/payment/order/{id}` | Yêu cầu | `CUSTOMER`/`OPERATOR` | Danh sách lịch sử giao dịch của đơn hàng | `200 OK` | **PASS** |

---

## 4. Nhóm API Điều Hành & Quản Trị (Operations & Admin APIs)

| Phương thức | Endpoint | Xác thực | Quyền hạn | Mô tả chức năng | Kết quả Runtime | Đánh giá |
|:---:|---|:---:|:---:|---|:---:|:---:|
| `GET` | `/api/operations/data/{module}` | Yêu cầu | `STAFF`/`ADMIN` | Lấy dữ liệu module (`rental-orders`, `maintenance`, `payments`, `extensions`, `reviews`) | `200 OK` | **PASS** |
| `GET` | `/api/operations/data/{module}` | Yêu cầu | `STAFF` | Truy cập module nhạy cảm (`customers`, `staffs`, `travel-places`) | `403 Forbidden` (Được bảo vệ) | **PASS** |
| `GET` | `/api/admin/reports` | Yêu cầu | `ADMIN` | Báo cáo dòng tiền, doanh thu thuần, cọc tài sản | `200 OK` (`403` nếu là Staff) | **PASS** |
| `PUT` | `/api/admin/settings` | Yêu cầu | `ADMIN` | Cập nhật cấu hình cửa hàng, định vị, đơn giá km | `200 OK` (`403` nếu là Staff) | **PASS** |
| `GET` | `/api/shop` | Public | None | Lấy thông tin công khai của cửa hàng | `200 OK` | **PASS** |

---

## 5. Nhóm API OCR & Trí Tuệ Nhân Tạo (OCR & AI APIs)

| Phương thức | Endpoint | Xác thực | Quyền hạn | Request Payload | Kết quả Runtime | Đánh giá & Vấn đề |
|:---:|---|:---:|:---:|---|:---:|---|
| `POST` | `/api/ocr/cccd` | Yêu cầu | Authenticated | `MultipartFile file` (ảnh CCCD) | `200 OK` (gọi trực tiếp có CSRF) / `403 Forbidden` (trên web hợp đồng) | **BUG MBR-001:** Web `rental-contract.html` không đính kèm header `X-XSRF-TOKEN` khi gửi fetch |

---

## 6. Đánh Giá Kiến Trúc API (API Architecture Assessment)
- **Chuẩn hóa REST:** Các endpoint thiết kế rõ ràng, chia tách theo domain context.
- **Envelope Response:** Đa số API trả về DTO trực tiếp hoặc danh sách đối tượng JSON thuần túy, riêng `/api/auth/login` bọc trong `result.token`. Frontend đã xử lý tương thích tốt.
- **Mã lỗi HTTP:** Sử dụng chuẩn mã trạng thái (`200 OK`, `400 Bad Request` khi input không hợp lệ, `401 Unauthorized` khi chưa đăng nhập, `403 Forbidden` khi sai quyền hoặc vi phạm CSRF, `404 Not Found`, `502 Bad Gateway` khi lỗi kết nối cổng thanh toán).
