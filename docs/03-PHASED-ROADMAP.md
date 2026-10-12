# Lộ Trình Triển Khai (PHASED ROADMAP)

Dự án được chia thành các Phase nhỏ nhằm đảo bảo việc kiểm thử và nghiệm thu dễ dàng sau mỗi bước. **Chỉ làm phase tiếp theo khi đã nhận được lệnh phê duyệt.**

## Danh Sách Các Phase
1.  **Phase 0:** Discovery và Mobile Architecture (Hoàn thành)
2.  **Phase 1:** Mobile Core Rental MVP (Customer đặt xe, Staff duyệt đơn)
3.  **Phase 2:** Mobile Authentication (Đăng nhập, đăng ký, phân quyền)
4.  **Phase 3:** Fleet và Availability (Quản lý đội xe, lịch trống, bộ lọc)
5.  **Phase 4:** Rental và Contract (Chi tiết hợp đồng, hạch toán)
6.  **Phase 5:** Handover, Return và Extension (Quy trình bàn giao, trả xe, gia hạn)
7.  **Phase 6:** Payment và Deposit (Thanh toán giả lập -> Thanh toán thật)
8.  **Phase 7:** Mobile Experience và PWA (Manifest, offline fallback, cache)
9.  **Phase 8:** Admin và Reporting (Báo cáo, thống kê, quản lý tổng thể)
10. **Phase 9:** AI và OCR (Nhận diện CCCD, gợi ý xe)

## Backlog Phase 1 (Mobile Core Rental MVP)

| ID | User Story | Task | Role | Priority | Estimate | Dependency | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1.1 | Là Customer, tôi muốn tìm và xem thông tin xe trên điện thoại | Tối ưu UI Mobile Home, Motorbike List, Detail | Customer | P0 | 4h | Không | TODO |
| 1.2 | Là Customer, tôi muốn chọn ngày giờ và tạo yêu cầu thuê xe | Làm form đặt xe, logic tính giá dự kiến, API lưu đơn | Customer | P0 | 6h | 1.1 | TODO |
| 1.3 | Là Staff, tôi muốn thấy đơn mới để tiếp nhận | Làm UI Pending Rentals dạng Card cho Mobile, API lấy danh sách | Staff | P0 | 4h | 1.2 | TODO |
| 1.4 | Là Staff, tôi muốn xác nhận hoặc từ chối đơn thuê | Làm Bottom Sheet duyệt đơn, API cập nhật trạng thái | Staff | P0 | 4h | 1.3 | TODO |
| 1.5 | Là Customer, tôi muốn xem trạng thái mới của đơn | Làm màn hình My Rental Detail cập nhật trạng thái | Customer | P0 | 2h | 1.4 | TODO |
