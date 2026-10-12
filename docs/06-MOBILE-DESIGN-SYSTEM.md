# Hệ Thống Thiết Kế Mobile (MOBILE DESIGN SYSTEM)

Duy trì bản sắc thương hiệu của MotoRent (từ file Stitch) nhưng tối ưu hóa cho màn hình cảm ứng.

## 1. Màu Sắc (Color Palette)
*   **Nền chính:** Midnight Navy / Xanh đen (tạo cảm giác cao cấp).
*   **CTA (Call to Action):** Màu Cam / Vàng đồng (`amber-500`, `orange-600`) cho các nút bấm quan trọng như "Đặt Xe", "Xác nhận".
*   **Trạng thái:**
    *   Thành công (Xanh lá).
    *   Đang chờ (Vàng).
    *   Thất bại/Hủy (Đỏ).

## 2. Typography
*   **Font chữ:** Plus Jakarta Sans.
*   **Size chữ Mobile:**
    *   Tiêu đề lớn nhất: 24px (không quá to để tránh xuống dòng liên tục).
    *   Nội dung thường (Body): 14px - 16px (chuẩn đọc trên điện thoại).
    *   Thông tin phụ: 12px.

## 3. Tương Tác Cảm Ứng (Touch Targets)
*   Mọi nút bấm phải có vùng cảm ứng tối thiểu `44x44px`.
*   Khoảng cách (gap) giữa các nút bấm tối thiểu `12px` (tương đương `gap-3` trong Tailwind).

## 4. UI Patterns cho Mobile
*   **Danh sách xe:** Hiển thị 1 cột (Card dọc) thay vì dạng Grid nhiều cột.
*   **Sidebar Menu:** Ẩn thành Hamburger menu (Offcanvas/Drawer) vuốt từ cạnh màn hình.
*   **Menu thao tác:** Chuyển các Modal thông báo lớn thành **Bottom Sheet** trượt từ dưới lên (dễ thao tác bằng ngón cái).
*   **Nút hành động chính:** Nút "Đặt xe ngay" nên đặt cố định dưới cùng màn hình (Sticky Bottom Bar).
