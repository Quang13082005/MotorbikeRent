# Nhật Ký Quyết Định (DECISION LOG)

## Quyết định #001: Không tách Backend và Frontend
*   **Ngày:** (Phase 0)
*   **Nội dung:** Giữ nguyên kiến trúc Monolithic với Spring Boot và Thymeleaf.
*   **Lý do:** Tối ưu hóa thời gian phát triển, tận dụng tốt kỹ năng sẵn có, không cần bảo trì hai codebase riêng biệt (Web API và App Mobile). Spring Security tích hợp cực tốt với Thymeleaf.

## Quyết định #002: Tailwind CSS cho Mobile-first
*   **Ngày:** (Phase 0)
*   **Nội dung:** Tiếp tục sử dụng Tailwind CSS thay vì Bootstrap.
*   **Lý do:** Tailwind cung cấp các utility classes (như `sm:`, `md:`) giúp dễ dàng tùy biến giao diện riêng cho điện thoại mà không cần viết quá nhiều custom CSS.

## Quyết định #003: Quản lý State UI bằng Alpine.js / Vanilla JS
*   **Ngày:** (Phase 0)
*   **Nội dung:** Các component động trên di động (Bottom Sheet, Drawer, Modal) sẽ dùng Vanilla JS (như code Header hiện tại) để tránh tải thêm React/Vue phức tạp.
