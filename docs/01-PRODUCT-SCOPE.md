# Phạm Vi Sản Phẩm (PRODUCT SCOPE)

## Mục Tiêu Chuyển Đổi
Chuyển đổi dự án MotoRent từ **Desktop-first Website** sang **Mobile-first Responsive Web**, và sau đó tiến tới **Progressive Web App (PWA)**, nhằm đáp ứng xu hướng đặt xe trực tiếp trên thiết bị di động của khách du lịch.

## Vấn Đề Cốt Lõi
Khách hàng sử dụng điện thoại để tìm xe, kiểm tra xe còn trống, chọn thời gian thuê và gửi yêu cầu; nhân viên sử dụng điện thoại hoặc máy tính để tiếp nhận và xác nhận; khách hàng xem được trạng thái mới của đơn.

## Core User Flow (Luồng Người Dùng Chính)
1. Customer mở MotoRent trên điện thoại.
2. Xem danh sách xe.
3. Chọn xe.
4. Chọn thời gian nhận và trả.
5. Hệ thống kiểm tra lịch trống.
6. Hệ thống tính giá dự kiến.
7. Customer tạo đơn.
8. Staff nhận được đơn.
9. Staff xác nhận hoặc từ chối.
10. Customer xem trạng thái mới.

## Phạm Vi Công Nghệ Giữ Lại
*   Spring Boot, Spring Security.
*   Thymeleaf (Server-Side Rendering).
*   MySQL Database.
*   Design System MotoRent (Tailwind CSS, Plus Jakarta Sans, màu sắc đặc trưng).
*   **Không** tách làm backend riêng cho app native, dùng chung một codebase duy nhất cho cả Desktop và Mobile web.

## Phạm Vi Không Nằm Trong Lần Chuyển Đổi Này
*   Ứng dụng Native (React Native, Flutter, Swift).
*   Chuyển đổi toàn bộ màn hình cùng lúc (Sẽ làm theo Phase).
