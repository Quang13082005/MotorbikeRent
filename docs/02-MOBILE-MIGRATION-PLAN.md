# Kế Hoạch Chuyển Đổi Mobile (MOBILE MIGRATION PLAN)

Kế hoạch này vạch ra chiến lược để biến đổi từng màn hình từ giao diện Desktop sang Mobile mà không làm hỏng trải nghiệm Desktop hiện có.

## Chiến Lược Mobile-First
*   Thiết kế giao diện từ kích thước nhỏ nhất (`360x800`), sau đó mới mở rộng lên Tablet (`768x1024`) và Desktop (`1920x1080`).
*   Sử dụng Bottom Navigation cho Customer trên Mobile.
*   Sử dụng Drawer (Sidebar trượt) cho Staff và Admin trên Mobile.
*   Chuyển các bảng (`<table>`) trên màn hình quản lý thành các thẻ Card danh sách (`<ul> <li>`) trên Mobile.

## Bảng Tiến Độ Chuyển Đổi

| Màn hình hiện tại | Role | Desktop status | Mobile status | Phase thực hiện | Cách chuyển đổi |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Login / Register | Khách | Đã có | Chưa chuẩn | Phase 2 | Full-width input, form thân thiện mobile |
| Trang Chủ (Landing) | Customer | Tốt | Tạm Ổn | Phase 1 | Tối ưu ảnh, card xe, thêm Bottom Navigation |
| Danh sách xe | Customer | Đã có | Vỡ layout bộ lọc | Phase 3 | Bộ lọc bằng Bottom Sheet, Card xếp dọc |
| Chi tiết xe | Customer | Đã có | Chưa cân đối | Phase 1 | Gắn sticky action bar "Đặt Xe" dưới đáy màn hình |
| Form Tạo Đơn / Ký HĐ | Customer | Đã có | Form dài | Phase 1 & 4 | Chia section rõ ràng, step indicator |
| Danh sách đơn (Pending) | Staff | Đã có (Table) | Vỡ layout | Phase 1 | Đổi table thành danh sách Card |
| Chi tiết đơn / Duyệt đơn | Staff | Đã có | Popup lớn | Phase 1 | Dùng Bottom Sheet hoặc Full-screen dialog |
| Bàn giao / Nhận xe | Staff | Đã có | Form Desktop | Phase 5 | Checklist dễ bấm 1 tay, upload ảnh |
| Quản lý (Dashboard) | Admin | Đã có | Vỡ layout | Phase 8 | Xếp lại Card dạng 1 cột, Sidebar -> Drawer |
